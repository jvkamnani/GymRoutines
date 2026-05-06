#!/usr/bin/env python3
"""Deterministically parse Jeff Nippard Essentials 4x PDF workout tables.

This parser extracts structured workout data directly from the PDF:
- week, split (upper/lower), page
- exercise name
- warm-up sets
- working sets
- reps
- rpe
- rest
- substitution options
- notes
- last YouTube link on the row

It also embeds warm-up loading guidance from the program warm-up section.
"""

from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path
from typing import Any

try:
    import pdfplumber
except ModuleNotFoundError as exc:  # pragma: no cover - handled at runtime
    raise SystemExit(
        "Missing dependency: pdfplumber\n"
        "Install in a local venv, for example:\n"
        "  python3 -m venv tools/.venv\n"
        "  tools/.venv/bin/pip install pdfplumber\n"
        "Then run:\n"
        "  tools/.venv/bin/python tools/parse_jeff_essentials_pdf.py ..."
    ) from exc


WORKOUT_PAGE_START = 20
WORKOUT_PAGE_END = 67

WARMUP_GUIDE = {
    "0": {
        "sets": [],
        "summary": "No exercise-specific warm-up sets listed.",
    },
    "1": {
        "sets": [
            {
                "set_index": 1,
                "weight_percent_of_working": 60,
                "reps": "same as working reps",
            }
        ],
        "summary": "One warm-up set around 60% for working-set reps.",
    },
    "2": {
        "sets": [
            {
                "set_index": 1,
                "weight_percent_of_working": 50,
                "reps": "same as working reps",
            },
            {
                "set_index": 2,
                "weight_percent_of_working": 70,
                "reps": "a few less than working reps",
            },
        ],
        "summary": "Mini-pyramid warm-up (about 50%, then 70%).",
    },
    "3": {
        "sets": [
            {
                "set_index": 1,
                "weight_percent_of_working": 45,
                "reps": "same as working reps",
            },
            {
                "set_index": 2,
                "weight_percent_of_working": 65,
                "reps": "a few less than working reps",
            },
            {
                "set_index": 3,
                "weight_percent_of_working": 85,
                "reps": "a few less reps still",
            },
        ],
        "summary": "Full warm-up pyramid (about 45%, 65%, 85%).",
    },
}


def _clean_text(value: Any) -> str:
    if value is None:
        return ""
    text = str(value)
    text = text.replace("\u00a0", " ")
    text = re.sub(r"\s+", " ", text).strip()
    return text


def _week_from_text(value: str) -> int | None:
    match = re.search(r"WEEK\s*(\d+)", value.upper())
    if not match:
        return None
    return int(match.group(1))


def _split_from_table_rows(rows: list[list[str | None]]) -> str | None:
    candidates = []
    for row in rows[1:]:
        if row and row[0]:
            candidates.append(_clean_text(row[0]).upper())
    merged = " ".join(candidates)
    compact = re.sub(r"[^A-Z]", "", merged)
    if "UPPER" in compact:
        return "UPPER"
    if "REPPU" in compact:
        return "UPPER"
    if "LOWER" in compact:
        return "LOWER"
    if "REWOL" in compact:
        return "LOWER"
    return None


def _youtube_links_for_row(page_links: list[dict[str, Any]], row_bbox: tuple[float, float, float, float]) -> list[str]:
    row_top = row_bbox[1]
    row_bottom = row_bbox[3]

    in_row = []
    for link in page_links:
        uri = link.get("uri")
        if not uri:
            continue
        center_y = (link["top"] + link["bottom"]) / 2
        if row_top - 1 <= center_y <= row_bottom + 1:
            in_row.append(link)

    # Left-to-right ordering yields exercise link first and substitution links later.
    in_row.sort(key=lambda item: (item.get("x0", 0.0), item.get("top", 0.0)))

    deduped: list[str] = []
    seen: set[str] = set()
    for link in in_row:
        uri = str(link["uri"]).strip()
        if uri in seen:
            continue
        seen.add(uri)
        deduped.append(uri)
    return deduped


def _pick_main_table(page: Any) -> Any | None:
    tables = page.find_tables()
    candidates: list[Any] = []
    for table in tables:
        extracted = table.extract()
        if not extracted:
            continue
        header = [
            _clean_text(cell)
            for cell in extracted[0]
        ]
        header_text = " ".join(header).upper()
        if "EXERCISE" in header_text and "WARM-UP" in header_text and len(header) >= 11:
            candidates.append(table)
    if not candidates:
        return None
    return max(candidates, key=lambda t: len(t.rows))


def parse_workout_page(pdf: Any, page_number: int) -> dict[str, Any]:
    page = pdf.pages[page_number - 1]
    table = _pick_main_table(page)
    if table is None:
        raise ValueError(f"No workout table found on page {page_number}")

    rows = table.extract()
    if len(rows) < 2:
        raise ValueError(f"Workout table on page {page_number} has no data rows")

    header_text = " ".join(_clean_text(x) for x in rows[0])
    week = _week_from_text(header_text)
    if week is None:
        page_text = _clean_text(page.extract_text() or "")
        week = _week_from_text(page_text)
    if week is None:
        raise ValueError(f"Could not detect week number on page {page_number}")

    split = _split_from_table_rows(rows)

    page_links = [
        link
        for link in page.hyperlinks
        if isinstance(link.get("uri"), str)
        and ("youtube.com" in link["uri"].lower() or "youtu.be" in link["uri"].lower())
    ]

    exercise_rows: list[dict[str, Any]] = []
    for index in range(1, len(rows)):
        row = rows[index]
        if not row or len(row) < 11:
            continue

        exercise = _clean_text(row[1])
        if not exercise:
            continue

        # Ignore header spill rows that can appear in malformed extraction.
        if exercise.upper() == "EXERCISE":
            continue

        row_cells = table.rows[index].cells
        # Ignore merged split-label column (index 0), use content columns for y-range.
        content_cells = row_cells[1:]
        ys = [cell[1] for cell in content_cells if cell] + [cell[3] for cell in content_cells if cell]
        if not ys:
            continue
        row_bbox = (0.0, min(ys), 0.0, max(ys))
        video_links = _youtube_links_for_row(page_links, row_bbox)

        row_data = {
            "exercise": exercise,
            "warmup_sets": _clean_text(row[2]),
            "working_sets": _clean_text(row[3]),
            "reps": _clean_text(row[4]),
            "rpe": _clean_text(row[6]),
            "rest": _clean_text(row[7]),
            "substitutions": [
                _clean_text(row[8]),
                _clean_text(row[9]),
            ],
            "notes": _clean_text(row[10]),
            "youtube_links": video_links,
            "last_youtube_video_link": video_links[-1] if video_links else "",
        }
        exercise_rows.append(row_data)

    return {
        "pdf_page": page_number,
        "week": week,
        "split": split,
        "rows": exercise_rows,
    }


def parse_all_relevant_pages(pdf_path: Path) -> dict[str, Any]:
    with pdfplumber.open(str(pdf_path)) as pdf:
        pages: list[dict[str, Any]] = []
        for page_number in range(WORKOUT_PAGE_START, WORKOUT_PAGE_END + 1):
            parsed = parse_workout_page(pdf, page_number)
            pages.append(parsed)

    return {
        "source_pdf": str(pdf_path),
        "warmup_guide": WARMUP_GUIDE,
        "workout_pages": pages,
        "meta": {
            "workout_page_start": WORKOUT_PAGE_START,
            "workout_page_end": WORKOUT_PAGE_END,
            "parsed_page_count": len(pages),
            "total_exercise_rows": sum(len(page["rows"]) for page in pages),
        },
    }


def parse_single_page(pdf_path: Path, page_number: int) -> dict[str, Any]:
    with pdfplumber.open(str(pdf_path)) as pdf:
        parsed = parse_workout_page(pdf, page_number)
    return {
        "source_pdf": str(pdf_path),
        "warmup_guide": WARMUP_GUIDE,
        "workout_page": parsed,
    }


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(
        description="Parse Jeff Nippard Essentials 4x PDF workout pages into JSON.",
    )
    parser.add_argument(
        "--pdf",
        required=True,
        type=Path,
        help="Path to jeff_nippard_essentials_4x.pdf",
    )
    parser.add_argument(
        "--output",
        required=False,
        type=Path,
        help="Optional output JSON file path. If omitted, prints to stdout.",
    )
    parser.add_argument(
        "--page",
        required=False,
        type=int,
        help=(
            "Parse only one page (1-based PDF page number). "
            "Use values 20-67 for workout pages."
        ),
    )
    args = parser.parse_args(argv)

    pdf_path = args.pdf
    if not pdf_path.exists():
        raise SystemExit(f"PDF not found: {pdf_path}")

    if args.page is not None:
        payload = parse_single_page(pdf_path, args.page)
    else:
        payload = parse_all_relevant_pages(pdf_path)

    output_text = json.dumps(payload, indent=2, ensure_ascii=False)
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(output_text + "\n", encoding="utf-8")
        print(f"Wrote {args.output}")
    else:
        print(output_text)

    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
