#!/usr/bin/env python3
"""Generate a GymRoutines Room database from a workout JSON file."""

from __future__ import annotations

import argparse
import json
import re
import sqlite3
from pathlib import Path
from typing import Any

DEFAULT_SCHEMA = (
    "app/schemas/com.noahjutz.gymroutines.data.AppDatabase/44.json"
)
DEFAULT_OUTPUT = "app/src/main/assets/preloaded/workout_routines_database"
SET_KINDS = {"normal", "warm_up", "drop"}
SET_KIND_ALIASES = {
    "working": "normal",
    "work": "normal",
    "regular": "normal",
    "warmup": "warm_up",
    "warm-up": "warm_up",
    "dropset": "drop",
    "drop-set": "drop",
    "drop set": "drop",
}
DROPSET_PATTERN = re.compile(r"\bdrop[\s-]*set(s)?\b", re.IGNORECASE)


def _bool(value: Any, default: bool) -> bool:
    if value is None:
        return default
    if isinstance(value, bool):
        return value
    raise ValueError(f"Expected boolean, got {value!r}")


def _num_or_none(value: Any) -> int | float | None:
    if value is None:
        return None
    if isinstance(value, (int, float)):
        return value
    raise ValueError(f"Expected number or null, got {value!r}")


def _int_or_default(value: Any, default: int) -> int:
    if value is None:
        return default
    if isinstance(value, int):
        return value
    raise ValueError(f"Expected integer, got {value!r}")


def _set_kind(value: Any) -> str:
    if value is None:
        return "normal"
    if not isinstance(value, str):
        raise ValueError(f"Expected set kind string, got {value!r}")
    normalized = value.strip().lower()
    normalized = SET_KIND_ALIASES.get(normalized, normalized)
    if normalized not in SET_KINDS:
        raise ValueError(
            f"Unsupported set kind {value!r}. Allowed values: {sorted(SET_KINDS)}"
        )
    return normalized


def load_json(path: Path) -> dict[str, Any]:
    with path.open("r", encoding="utf-8") as f:
        data = json.load(f)
    if not isinstance(data, dict):
        raise ValueError("Top-level JSON must be an object")
    return data


def create_schema(conn: sqlite3.Connection, schema: dict[str, Any]) -> None:
    entities = schema["database"]["entities"]
    for entity in entities:
        sql = entity["createSql"].replace("${TABLE_NAME}", entity["tableName"])
        conn.execute(sql)

    for entity in entities:
        for index in entity.get("indices", []):
            sql = index["createSql"].replace("${TABLE_NAME}", entity["tableName"])
            conn.execute(sql)

    for query in schema["database"]["setupQueries"]:
        conn.execute(query)


def insert_workout_data(conn: sqlite3.Connection, workout: dict[str, Any]) -> None:
    routines = workout.get("routines")
    if not isinstance(routines, list) or not routines:
        raise ValueError("Input JSON must contain non-empty 'routines' array")

    exercise_ids: dict[str, int] = {}
    next_exercise_id = 1
    next_routine_id = 1
    next_group_id = 1
    next_set_id = 1

    for routine in routines:
        if not isinstance(routine, dict):
            raise ValueError("Each routine must be an object")
        routine_name = routine.get("name", "")
        if not isinstance(routine_name, str):
            raise ValueError("Routine name must be a string")

        conn.execute(
            "INSERT INTO routine_table (name, hidden, routineId) VALUES (?, 0, ?)",
            (routine_name, next_routine_id),
        )

        exercises = routine.get("exercises", [])
        if not isinstance(exercises, list):
            raise ValueError("Routine 'exercises' must be an array")

        for position, exercise in enumerate(exercises):
            if not isinstance(exercise, dict):
                raise ValueError("Each exercise must be an object")

            name = exercise.get("name")
            if not isinstance(name, str) or not name.strip():
                raise ValueError("Exercise name must be a non-empty string")
            normalized_name = name.strip()
            key = normalized_name.lower()

            notes = exercise.get("notes", "")
            if not isinstance(notes, str):
                raise ValueError(f"Exercise notes must be a string: {name}")
            notes = notes.strip()

            alternatives = exercise.get("alternatives", [])
            if alternatives is None:
                alternatives = []
            if not isinstance(alternatives, list):
                raise ValueError(f"Exercise alternatives must be an array: {name}")
            if alternatives:
                normalized_alts: list[str] = []
                for alt in alternatives:
                    if not isinstance(alt, str) or not alt.strip():
                        raise ValueError(
                            f"Each alternative must be a non-empty string: {name}"
                        )
                    normalized_alts.append(alt.strip())
                alternatives_line = "Alternatives: " + ", ".join(normalized_alts)
                notes = (
                    f"{notes}\n{alternatives_line}" if notes else alternatives_line
                )

            superset_tag = exercise.get("supersetTag")
            if superset_tag is not None:
                if not isinstance(superset_tag, str) or not superset_tag.strip():
                    raise ValueError(
                        f"supersetTag must be a non-empty string when provided: {name}"
                    )
                superset_tag = superset_tag.strip()

            warmup_sets = _int_or_default(exercise.get("warmupSets"), 0)
            if warmup_sets < 0:
                raise ValueError(f"warmupSets must be >= 0: {name}")
            warmup_reps = _num_or_none(exercise.get("warmupReps"))

            track = exercise.get("track", {})
            if track is None:
                track = {}
            if not isinstance(track, dict):
                raise ValueError(f"Exercise track must be an object: {name}")

            log_reps = _bool(track.get("reps"), True)
            log_weight = _bool(track.get("weight"), False)
            log_time = _bool(track.get("time"), False)
            log_distance = _bool(track.get("distance"), False)

            if key not in exercise_ids:
                conn.execute(
                    """
                    INSERT INTO exercise_table
                    (name, notes, logReps, logWeight, logTime, logDistance, hidden, exerciseId)
                    VALUES (?, ?, ?, ?, ?, ?, 0, ?)
                    """,
                    (
                        normalized_name,
                        notes,
                        int(log_reps),
                        int(log_weight),
                        int(log_time),
                        int(log_distance),
                        next_exercise_id,
                    ),
                )
                exercise_ids[key] = next_exercise_id
                next_exercise_id += 1

            exercise_id = exercise_ids[key]
            conn.execute(
                """
                INSERT INTO routine_set_group_table
                (routineId, exerciseId, position, supersetTag, id)
                VALUES (?, ?, ?, ?, ?)
                """,
                (next_routine_id, exercise_id, position, superset_tag, next_group_id),
            )

            sets = exercise.get("sets", [])
            if not isinstance(sets, list):
                raise ValueError(f"Exercise sets must be an array: {name}")

            for _ in range(warmup_sets):
                conn.execute(
                    """
                    INSERT INTO routine_set_table
                    (groupId, reps, weight, time, distance, setKind, routineSetId)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """,
                    (
                        next_group_id,
                        warmup_reps,
                        None,
                        None,
                        None,
                        "warm_up",
                        next_set_id,
                    ),
                )
                next_set_id += 1

            drop_on_last_set = DROPSET_PATTERN.search(notes) is not None

            for set_index, set_row in enumerate(sets):
                if not isinstance(set_row, dict):
                    raise ValueError(f"Each set must be an object: {name}")

                reps = _num_or_none(set_row.get("reps"))
                weight = _num_or_none(set_row.get("weight"))
                time = _num_or_none(set_row.get("time"))
                distance = _num_or_none(set_row.get("distance"))
                explicit_kind = (
                    set_row.get("kind")
                    or set_row.get("setKind")
                    or set_row.get("type")
                )
                if explicit_kind is None and drop_on_last_set and set_index == len(sets) - 1:
                    set_kind = "drop"
                else:
                    set_kind = _set_kind(explicit_kind)

                conn.execute(
                    """
                    INSERT INTO routine_set_table
                    (groupId, reps, weight, time, distance, setKind, routineSetId)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """,
                    (
                        next_group_id,
                        reps,
                        weight,
                        time,
                        distance,
                        set_kind,
                        next_set_id,
                    ),
                )
                next_set_id += 1

            next_group_id += 1

        next_routine_id += 1


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--input", required=True, help="Path to workout JSON")
    parser.add_argument(
        "--schema",
        default=DEFAULT_SCHEMA,
        help="Room schema JSON path (default: %(default)s)",
    )
    parser.add_argument(
        "--output",
        default=DEFAULT_OUTPUT,
        help="Output SQLite DB path (default: %(default)s)",
    )
    args = parser.parse_args()

    input_path = Path(args.input)
    schema_path = Path(args.schema)
    output_path = Path(args.output)

    if not input_path.exists():
        raise SystemExit(f"Input file not found: {input_path}")
    if not schema_path.exists():
        raise SystemExit(f"Schema file not found: {schema_path}")

    workout_data = load_json(input_path)
    schema_data = load_json(schema_path)

    output_path.parent.mkdir(parents=True, exist_ok=True)
    if output_path.exists():
        output_path.unlink()

    conn = sqlite3.connect(str(output_path))
    try:
        conn.execute("PRAGMA foreign_keys=OFF")
        create_schema(conn, schema_data)
        insert_workout_data(conn, workout_data)
        conn.commit()
    finally:
        conn.close()

    print(f"Created preloaded DB: {output_path}")


if __name__ == "__main__":
    main()
