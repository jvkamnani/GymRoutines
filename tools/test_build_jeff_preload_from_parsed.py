import unittest
from pathlib import Path
import sys

sys.path.insert(0, str(Path(__file__).resolve().parent))

from build_jeff_preload_from_parsed import build_routines_from_parsed


def _row(exercise: str, reps: str = "8-10", working_sets: str = "2") -> dict:
    return {
        "exercise": exercise,
        "warmup_sets": "1-2",
        "working_sets": working_sets,
        "reps": reps,
        "rpe": "8-9",
        "rest": "~2 min",
        "substitutions": [f"{exercise} Alt 1", f"{exercise} Alt 2"],
        "notes": f"Notes for {exercise}",
        "youtube_links": [],
        "last_youtube_video_link": "https://youtu.be/example",
    }


class BuildJeffPreloadFromParsedTest(unittest.TestCase):
    def test_groups_contiguous_identical_weeks_into_ranges_and_orders_routines(self):
        pages = []
        page_number = 20

        for week in range(1, 5):
            pages.extend(
                [
                    {"pdf_page": page_number, "week": week, "rows": [_row("UA-1") ]},
                    {"pdf_page": page_number + 1, "week": week, "rows": [_row("LA-1") ]},
                    {"pdf_page": page_number + 2, "week": week, "rows": [_row("UB-1") ]},
                    {"pdf_page": page_number + 3, "week": week, "rows": [_row("LB-1") ]},
                ]
            )
            page_number += 4

        for week in range(5, 9):
            pages.extend(
                [
                    {"pdf_page": page_number, "week": week, "rows": [_row("UA-2") ]},
                    {"pdf_page": page_number + 1, "week": week, "rows": [_row("LA-2") ]},
                    {"pdf_page": page_number + 2, "week": week, "rows": [_row("UB-2") ]},
                    {"pdf_page": page_number + 3, "week": week, "rows": [_row("LB-2") ]},
                ]
            )
            page_number += 4

        parsed = {"workout_pages": pages}

        built = build_routines_from_parsed(parsed)

        routine_names = [routine["name"] for routine in built["routines"]]
        self.assertEqual(
            routine_names,
            [
                "Essentials 4x - Upper A (Weeks 1-4)",
                "Essentials 4x - Lower A (Weeks 1-4)",
                "Essentials 4x - Upper B (Weeks 1-4)",
                "Essentials 4x - Lower B (Weeks 1-4)",
                "Essentials 4x - Upper A (Weeks 5-8)",
                "Essentials 4x - Lower A (Weeks 5-8)",
                "Essentials 4x - Upper B (Weeks 5-8)",
                "Essentials 4x - Lower B (Weeks 5-8)",
            ],
        )

    def test_single_week_range_uses_week_label(self):
        parsed = {
            "workout_pages": [
                {"pdf_page": 20, "week": 1, "rows": [_row("UA")]},
                {"pdf_page": 21, "week": 1, "rows": [_row("LA")]},
                {"pdf_page": 22, "week": 1, "rows": [_row("UB")]},
                {"pdf_page": 23, "week": 1, "rows": [_row("LB")]},
            ]
        }

        built = build_routines_from_parsed(parsed)

        self.assertEqual(
            [routine["name"] for routine in built["routines"]],
            [
                "Essentials 4x - Upper A (Week 1)",
                "Essentials 4x - Lower A (Week 1)",
                "Essentials 4x - Upper B (Week 1)",
                "Essentials 4x - Lower B (Week 1)",
            ],
        )


if __name__ == "__main__":
    unittest.main()
