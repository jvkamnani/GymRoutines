#!/usr/bin/env python3
"""Tests for deterministic Jeff Essentials PDF parsing."""

from __future__ import annotations

import importlib.util
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PDF_PATH = ROOT.parent / "imports" / "jeff_nippard_essentials_4x.pdf"
PARSER_PATH = ROOT / "tools" / "parse_jeff_essentials_pdf.py"

parser = None
IMPORT_ERROR: Exception | None = None

spec = importlib.util.spec_from_file_location("parse_jeff_essentials_pdf", PARSER_PATH)
if spec is None or spec.loader is None:
    IMPORT_ERROR = RuntimeError(f"Could not load parser module from {PARSER_PATH}")
else:
    try:
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        parser = module
    except Exception as exc:  # pragma: no cover
        IMPORT_ERROR = exc


@unittest.skipIf(parser is None, f"Missing parser dependencies: {IMPORT_ERROR}")
class ParseJeffEssentialsPdfTest(unittest.TestCase):
    def test_parse_single_workout_page(self) -> None:
        payload = parser.parse_single_page(PDF_PATH, 20)
        page = payload["workout_page"]

        self.assertEqual(20, page["pdf_page"])
        self.assertEqual(1, page["week"])
        self.assertEqual("UPPER", page["split"])

        rows = page["rows"]
        self.assertEqual(7, len(rows))

        first = rows[0]
        self.assertEqual("Flat DB Press (Heavy)", first["exercise"])
        self.assertEqual("2-3", first["warmup_sets"])
        self.assertEqual("1", first["working_sets"])
        self.assertEqual("4-6", first["reps"])
        self.assertEqual("8-9", first["rpe"])
        self.assertEqual("~3 min", first["rest"])
        self.assertEqual("Machine Chest Press", first["substitutions"][0])
        self.assertEqual("Weighted Dip", first["substitutions"][1])
        self.assertTrue(first["last_youtube_video_link"].startswith("https://"))

        last = rows[-1]
        self.assertEqual("A2: EZ Bar Curl", last["exercise"])
        self.assertEqual("https://youtu.be/tw1h5XOD23Y", last["last_youtube_video_link"])

    def test_parse_all_relevant_pages(self) -> None:
        payload = parser.parse_all_relevant_pages(PDF_PATH)
        meta = payload["meta"]
        self.assertEqual(20, meta["workout_page_start"])
        self.assertEqual(67, meta["workout_page_end"])
        self.assertEqual(48, meta["parsed_page_count"])
        self.assertGreater(meta["total_exercise_rows"], 250)

        warmup = payload["warmup_guide"]
        self.assertEqual(60, warmup["1"]["sets"][0]["weight_percent_of_working"])
        self.assertEqual(85, warmup["3"]["sets"][2]["weight_percent_of_working"])


if __name__ == "__main__":
    unittest.main()
