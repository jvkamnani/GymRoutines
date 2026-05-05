#!/usr/bin/env python3
"""Integration tests for preloaded DB generation."""

from __future__ import annotations

import json
import sqlite3
import subprocess
import tempfile
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SCRIPT = ROOT / "tools" / "generate_preloaded_db.py"
SCHEMA = ROOT / "app" / "schemas" / "com.noahjutz.gymroutines.data.AppDatabase" / "44.json"


class GeneratePreloadedDbIntegrationTest(unittest.TestCase):
    def _run_generator(self, payload: dict) -> sqlite3.Connection:
        temp_dir = tempfile.TemporaryDirectory()
        self.addCleanup(temp_dir.cleanup)
        input_path = Path(temp_dir.name) / "workout.json"
        output_db = Path(temp_dir.name) / "workout_routines_database"
        input_path.write_text(json.dumps(payload), encoding="utf-8")
        subprocess.run(
            [
                "python3",
                str(SCRIPT),
                "--input",
                str(input_path),
                "--schema",
                str(SCHEMA),
                "--output",
                str(output_db),
            ],
            check=True,
            capture_output=True,
            text=True,
            cwd=ROOT,
        )
        connection = sqlite3.connect(str(output_db))
        self.addCleanup(connection.close)
        return connection

    def test_set_kind_aliases_and_drop_inference(self) -> None:
        conn = self._run_generator(
            {
                "routines": [
                    {
                        "name": "Day 1",
                        "exercises": [
                            {
                                "name": "Barbell Bench Press",
                                "notes": "Last set is a dropset",
                                "warmupSets": 1,
                                "warmupReps": 12,
                                "sets": [
                                    {"reps": 8, "kind": "working"},
                                    {"reps": 6, "setKind": "warmup"},
                                    {"reps": 5},
                                ],
                            }
                        ],
                    }
                ]
            },
        )

        kinds = [
            row[0]
            for row in conn.execute(
                "SELECT setKind FROM routine_set_table ORDER BY routineSetId",
            ).fetchall()
        ]
        self.assertEqual(["warm_up", "normal", "warm_up", "drop"], kinds)

    def test_alternatives_written_to_exercise_notes(self) -> None:
        conn = self._run_generator(
            {
                "routines": [
                    {
                        "name": "Day 2",
                        "exercises": [
                            {
                                "name": "Incline Bench Press",
                                "notes": "Use a controlled tempo.",
                                "alternatives": [
                                    " Dumbbell Incline Press ",
                                    "Smith Machine Incline Press",
                                ],
                                "sets": [{"reps": 10}],
                            }
                        ],
                    }
                ]
            },
        )

        notes = conn.execute(
            "SELECT notes FROM exercise_table WHERE name = ?",
            ("Incline Bench Press",),
        ).fetchone()
        self.assertIsNotNone(notes)
        self.assertIn(
            "Alternatives: Dumbbell Incline Press, Smith Machine Incline Press",
            notes[0],
        )

    def test_invalid_set_kind_fails_generation(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            input_path = Path(temp_dir) / "workout.json"
            output_db = Path(temp_dir) / "workout_routines_database"
            input_path.write_text(
                json.dumps(
                    {
                        "routines": [
                            {
                                "name": "Invalid",
                                "exercises": [
                                    {
                                        "name": "Squat",
                                        "sets": [{"reps": 5, "kind": "invalid_kind"}],
                                    }
                                ],
                            }
                        ]
                    },
                ),
                encoding="utf-8",
            )

            result = subprocess.run(
                [
                    "python3",
                    str(SCRIPT),
                    "--input",
                    str(input_path),
                    "--schema",
                    str(SCHEMA),
                    "--output",
                    str(output_db),
                ],
                capture_output=True,
                text=True,
                cwd=ROOT,
            )

        self.assertNotEqual(0, result.returncode)
        self.assertIn("Unsupported set kind", result.stderr)


if __name__ == "__main__":
    unittest.main()
