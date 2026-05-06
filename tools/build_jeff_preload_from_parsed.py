#!/usr/bin/env python3
"""Build GymRoutines preload JSON from parsed Jeff Essentials PDF JSON.

Input format: output of tools/parse_jeff_essentials_pdf.py (--output ...).
Output format: routines JSON accepted by tools/generate_preloaded_db.py.
"""

from __future__ import annotations

import argparse
import json
import re
from dataclasses import dataclass
from pathlib import Path
from typing import Any

DAY_LABELS = ["Upper A", "Lower A", "Upper B", "Lower B"]
DAY_ORDER = {label: index for index, label in enumerate(DAY_LABELS)}
RANGE_PATTERN = re.compile(r"^\s*(\d+)\s*-\s*(\d+)\s*$")


@dataclass(frozen=True)
class BuiltExercise:
    payload: dict[str, Any]
    signature: str


def _extract_max_count(value: Any, *, default: int = 0) -> int:
    if value is None:
        return default
    text = str(value).strip()
    if not text:
        return default
    if text.isdigit():
        return int(text)
    match = RANGE_PATTERN.fullmatch(text)
    if match:
        return int(match.group(2))
    first_number = re.search(r"\d+", text)
    if first_number:
        return int(first_number.group(0))
    return default


def _extract_top_rep(value: Any) -> int | None:
    if value is None:
        return None
    text = str(value).strip()
    if not text:
        return None
    if text.isdigit():
        return int(text)
    match = RANGE_PATTERN.fullmatch(text)
    if match:
        return int(match.group(2))
    numbers = [int(found) for found in re.findall(r"\d+", text)]
    if numbers:
        return max(numbers)
    return None


def _exercise_from_row(row: dict[str, Any], pdf_page: int) -> BuiltExercise:
    exercise_name = str(row.get("exercise", "")).strip()
    warmup_sets_text = str(row.get("warmup_sets", "")).strip()
    working_set_count = max(1, _extract_max_count(row.get("working_sets"), default=1))
    rep_target = _extract_top_rep(row.get("reps"))

    substitutions = [
        str(item).strip()
        for item in row.get("substitutions", [])
        if str(item).strip()
    ]

    notes_parts: list[str] = []
    row_notes = str(row.get("notes", "")).strip()
    if row_notes:
        notes_parts.append(row_notes)
    rest = str(row.get("rest", "")).strip()
    if rest:
        notes_parts.append(f"Rest: {rest}")
    video = str(row.get("last_youtube_video_link", "")).strip()
    if video:
        notes_parts.append(f"Video: {video}")
    notes_parts.append(f"Source page: {pdf_page}")

    sets: list[dict[str, Any]] = []
    for _ in range(working_set_count):
        set_row: dict[str, Any] = {"kind": "normal"}
        if rep_target is not None:
            set_row["reps"] = rep_target
        rpe_text = str(row.get("rpe", "")).strip()
        if rpe_text:
            set_row["rpe"] = rpe_text
        sets.append(set_row)

    exercise_payload: dict[str, Any] = {
        "name": exercise_name,
        "track": {"reps": True, "weight": True},
        "sets": sets,
    }

    if warmup_sets_text and warmup_sets_text != "0":
        exercise_payload["warmupSets"] = warmup_sets_text
        warmup_reps = rep_target or 8
        exercise_payload["warmupReps"] = warmup_reps

    if substitutions:
        exercise_payload["alternatives"] = substitutions

    if notes_parts:
        exercise_payload["notes"] = "\n".join(notes_parts)

    rpe_text = str(row.get("rpe", "")).strip()
    if rpe_text:
        exercise_payload["rpe"] = rpe_text

    # Signature excludes source-page metadata so contiguous equal weeks can merge.
    signature_payload = dict(exercise_payload)
    if "notes" in signature_payload:
        normalized_notes = [
            line
            for line in str(signature_payload["notes"]).splitlines()
            if not line.startswith("Source page:")
        ]
        signature_payload["notes"] = "\n".join(normalized_notes).strip()

    signature = json.dumps(signature_payload, sort_keys=True, ensure_ascii=False)
    return BuiltExercise(payload=exercise_payload, signature=signature)


def _week_day_pages(parsed: dict[str, Any]) -> dict[int, dict[str, dict[str, Any]]]:
    pages = parsed.get("workout_pages")
    if not isinstance(pages, list) or not pages:
        raise ValueError("Input must contain non-empty 'workout_pages'")

    grouped: dict[int, list[dict[str, Any]]] = {}
    for page in pages:
        week = page.get("week")
        if not isinstance(week, int):
            raise ValueError(f"Invalid week in page: {page!r}")
        grouped.setdefault(week, []).append(page)

    week_map: dict[int, dict[str, dict[str, Any]]] = {}
    for week, week_pages in grouped.items():
        ordered_pages = sorted(week_pages, key=lambda item: int(item.get("pdf_page", 0)))
        if len(ordered_pages) != 4:
            raise ValueError(f"Week {week} has {len(ordered_pages)} pages, expected 4")

        label_map: dict[str, dict[str, Any]] = {}
        for day_index, page in enumerate(ordered_pages):
            day_label = DAY_LABELS[day_index]
            rows = page.get("rows")
            if not isinstance(rows, list) or not rows:
                raise ValueError(
                    f"Week {week} {day_label} has no exercise rows on page {page.get('pdf_page')}"
                )
            label_map[day_label] = page
        week_map[week] = label_map

    return dict(sorted(week_map.items(), key=lambda item: item[0]))


def build_routines_from_parsed(parsed: dict[str, Any]) -> dict[str, Any]:
    week_pages = _week_day_pages(parsed)
    weeks = list(week_pages.keys())
    if weeks != list(range(min(weeks), max(weeks) + 1)):
        raise ValueError("Weeks are not contiguous in parsed input")

    week_day_exercises: dict[tuple[int, str], list[BuiltExercise]] = {}
    signatures_by_week_day: dict[tuple[int, str], str] = {}

    for week in weeks:
        for day_label in DAY_LABELS:
            page = week_pages[week][day_label]
            pdf_page = int(page.get("pdf_page", 0))
            rows = page["rows"]
            built_exercises = [_exercise_from_row(row, pdf_page) for row in rows]
            week_day_exercises[(week, day_label)] = built_exercises
            signatures_by_week_day[(week, day_label)] = json.dumps(
                [exercise.signature for exercise in built_exercises],
                ensure_ascii=False,
            )

    routines: list[dict[str, Any]] = []

    for day_label in DAY_LABELS:
        run_start = weeks[0]
        run_signature = signatures_by_week_day[(weeks[0], day_label)]

        def flush_range(start_week: int, end_week: int, signature_week: int) -> None:
            exercises = week_day_exercises[(signature_week, day_label)]
            week_label = (
                f"Week {start_week}"
                if start_week == end_week
                else f"Weeks {start_week}-{end_week}"
            )
            routines.append(
                {
                    "name": f"Essentials 4x - {day_label} ({week_label})",
                    "exercises": [exercise.payload for exercise in exercises],
                    "_sort": {
                        "start_week": start_week,
                        "day_order": DAY_ORDER[day_label],
                    },
                }
            )

        for index in range(1, len(weeks)):
            week = weeks[index]
            signature = signatures_by_week_day[(week, day_label)]
            if signature == run_signature:
                continue
            flush_range(run_start, weeks[index - 1], run_start)
            run_start = week
            run_signature = signature

        flush_range(run_start, weeks[-1], run_start)

    routines.sort(
        key=lambda item: (
            item["_sort"]["start_week"],
            item["_sort"]["day_order"],
            item["name"],
        )
    )

    for routine in routines:
        routine.pop("_sort", None)

    return {"routines": routines}


def main() -> None:
    parser = argparse.ArgumentParser(
        description=(
            "Convert parsed Jeff Essentials JSON into GymRoutines preload JSON "
            "with deterministic week/day ordering and contiguous week-range grouping."
        )
    )
    parser.add_argument("--input", required=True, type=Path, help="Parsed JSON path")
    parser.add_argument("--output", required=True, type=Path, help="Output preload JSON path")
    args = parser.parse_args()

    parsed = json.loads(args.input.read_text(encoding="utf-8"))
    preload = build_routines_from_parsed(parsed)

    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(
        json.dumps(preload, indent=2, ensure_ascii=False) + "\n",
        encoding="utf-8",
    )
    print(f"Wrote {args.output}")


if __name__ == "__main__":
    main()
