#!/usr/bin/env python3
"""
rq1&2_make_tables.py

Generates manuscript-ready LaTeX tables for the RQ1 and RQ2 sections
of the Model Once, Generate Any paper, mirroring the structure of
rq3_09_make_tables.py.

Layout
------
This script lives in:
    esg-with-feature-expressions/files/scripts/statistical_test_scripts/
and reads its inputs from sibling result subfolders:
    rq1_result/RQ1_SPLSummary_medians.xlsx
    rq2_result/RQ2_ApproachSummary_medians.xlsx
    rq2_result/rq2_time_breakdown.xlsx
Outputs are written to:
    rq12_result/manuscript_tables/
    (rq1_table_tgen.tex, rq2_table_throughput.tex, rq2_table_breakdown.tex)

The path resolver also accepts a flat layout (all three Excels in one
directory next to the script, or in /mnt/user-data/uploads) for
ad-hoc runs.

Inputs
------
    RQ1_SPLSummary_medians.xlsx
        Sheets: ESG-Fx_L2/L3/L4, EFG_L2/L3/L4, RandomWalk_L0
        Used for: per-(SPL, approach, L) median TotalTestGenTime,
                  GuitarGenTime, EdgeCoverage.
    RQ2_ApproachSummary_medians.xlsx
        Sheets: ESG-Fx_L2/L3/L4, EFG_L2/L3/L4, RandomWalk_L0
        Two sections per sheet:
          rows  3..10  - SERIAL summary  (1-core sequential CPU cost)
          rows 16..23  - WALL-CLOCK summary (80-shard parallel deployment)
        Used for: handled products, T_pipeline (serial + wall-clock),
                  edge coverage, throughput.
    rq2_time_breakdown.xlsx
        Sheets: breakdown, sat_share_pivot, own_cost_share_pivot
        Used for: SAT share and per-product overhead share at L = 2.

L=1 is NEVER read anywhere - the manuscript covers L = 2, 3, 4 only.

Rounding
--------
All values are rounded with ROUND_HALF_UP to the displayed precision,
after scrubbing float-arithmetic noise at 6 decimals (the same scheme
used in rq3_09_make_tables.py). Integer-valued cells are written
without a trailing ".0" by default (e.g., "100" not "100.0"); set
always_decimal=True to keep the decimal (used in EdgeCoverage rows of
the RQ2 throughput table to match the manuscript's "100.0" style).

Bold / bold-italic markings
---------------------------
Cell decorations match the existing manuscript tables exactly. They are
encoded as explicit sets of (approach_label, L_str, SPL_short) keys at
the top of the script. To change which cells are highlighted, edit the
sets - the numbers themselves are computed from the input files.
"""
from __future__ import annotations

import sys
from decimal import Decimal, ROUND_HALF_UP
from pathlib import Path

import numpy as np
import pandas as pd


# =============================================================================
# Paths
# =============================================================================
# The script lives in:
#   esg-with-feature-expressions/files/scripts/statistical_test_scripts/
# and the input Excel files are split across two sibling result folders:
#   rq1_result/RQ1_SPLSummary_medians.xlsx
#   rq2_result/RQ2_ApproachSummary_medians.xlsx
#   rq2_result/rq2_time_breakdown.xlsx
# Outputs land in a separate subfolder so RQ1/RQ2 results stay clean.
SCRIPT_DIR = Path(__file__).resolve().parent
RESULT_DIR = SCRIPT_DIR / "rq12_result"
TEX_DIR    = RESULT_DIR / "manuscript_tables"


# =============================================================================
# SPL ordering (matches the manuscript column order in tab:rq1-tgen)
# =============================================================================
COL_ORDER = ["SVM", "eM", "El", "BAv2", "SAS", "Svia", "Te", "HS"]

# The RQ1_SPLSummary_medians.xlsx file uses the short labels above directly
# in its "SPL Name" column. The RQ2 ApproachSummary file uses the full
# folder names; we map them through the dict below.
SPL_FULL_TO_SHORT = {
    "SodaVendingMachine":      "SVM",
    "eMail":                   "eM",
    "Elevator":                "El",
    "BankAccountv2":           "BAv2",
    "StudentAttendanceSystem": "SAS",
    "syngovia":                "Svia",
    "Tesla":                   "Te",
    "HockertyShirts":          "HS",
}


# =============================================================================
# Bold / bold-italic encodings - copied from the existing manuscript tables.
# Format: (approach_label, L_str, SPL_short).
#   approach_label in {"MO", "Struct", "Stoch"}
#   L_str         in {"2", "3", "4", ""} ("" for stochastic baseline)
# =============================================================================

# --------------------------------------------------------------------------
# tab:rq1-tgen
# --------------------------------------------------------------------------
# T_gen rows: no bold/bold-italic anywhere in the manuscript.
RQ1_TGEN_BOLD: set[tuple[str, str, str]]        = set()
RQ1_TGEN_BOLD_ITALIC: set[tuple[str, str, str]] = set()

# Edge coverage rows.
# NOTE on (Stoch, "", "Te"): the manuscript shows 99.9 (bold) here; the
# updated Excel data has 99.996, which rounds to 100.0 at one decimal.
# Bolding "100" alongside other plain "100" cells looks inconsistent, so
# the marker is dropped. The text in Section 7.1.2 that calls out "drops
# to 99.9% on Tesla" should be revisited once you confirm whether you
# want to keep that observation at higher precision (e.g., "99.99%").
RQ1_EDGECOV_BOLD = {
    ("Struct", "2", "eM"),  ("Struct", "2", "Svia"), ("Struct", "2", "HS"),
    ("Struct", "3", "Svia"),("Struct", "3", "HS"),
    ("Struct", "4", "Svia"),("Struct", "4", "HS"),
    ("Stoch",  "",  "Svia"),                          ("Stoch",  "",  "HS"),
}
RQ1_EDGECOV_BOLD_ITALIC = {
    ("MO", "4", "eM"),
    ("MO", "4", "BAv2"),
}

# Speedup row: separate sets keyed only by (L, SPL) since speedup has no
# approach dimension (it is always Struct/MO).
RQ1_SPEEDUP_BOLD = {
    ("2", "BAv2"), ("2", "SAS"), ("2", "Te"),  ("2", "HS"),
    ("3", "BAv2"), ("3", "SAS"),
    ("4", "BAv2"),
}
RQ1_SPEEDUP_BOLD_ITALIC = {
    ("3", "Svia"),
    ("4", "Svia"),
}

# --------------------------------------------------------------------------
# tab:rq2-throughput  (L = 2 only)
# --------------------------------------------------------------------------
# Stored as (approach_label, SPL_short) since the table is single-L.
RQ2_HP_BOLD = {
    ("MO",     "HS"),
    ("Struct", "HS"),
}
RQ2_HP_BOLD_ITALIC: set[tuple[str, str]] = set()

RQ2_TPIPE_SER_BOLD = {
    ("MO",    "Svia"), ("MO",    "Te"),  ("MO",    "HS"),
    ("Stoch", "Svia"),
}
RQ2_TPIPE_SER_BOLD_ITALIC: set[tuple[str, str]] = set()

RQ2_TPIPE_WALL_BOLD = {
    ("MO",    "Svia"), ("MO",    "Te"),  ("MO",    "HS"),
    ("Stoch", "Svia"),
}
RQ2_TPIPE_WALL_BOLD_ITALIC: set[tuple[str, str]] = set()

RQ2_THROUGHPUT_BOLD = {
    ("MO",     "Svia"), ("MO",     "HS"),
    ("Struct", "HS"),
    ("Stoch",  "Svia"),
}
RQ2_THROUGHPUT_BOLD_ITALIC: set[tuple[str, str]] = set()

RQ2_EDGECOV_BOLD = {
    ("MO", "Svia"), ("MO", "Te"), ("MO", "HS"),
    ("Struct", "eM"), ("Struct", "SAS"), ("Struct", "Svia"), ("Struct", "HS"),
}
RQ2_EDGECOV_BOLD_ITALIC: set[tuple[str, str]] = set()

# --------------------------------------------------------------------------
# tab:rq2-breakdown
# --------------------------------------------------------------------------
RQ2_SAT_BOLD = {
    ("MO",     "BAv2"), ("MO",     "SAS"), ("MO",     "Te"), ("MO",     "HS"),
    ("Stoch",  "Svia"),
}
RQ2_SAT_BOLD_ITALIC: set[tuple[str, str]] = set()

RQ2_OWNCOST_BOLD = {
    ("MO",     "Te"), ("MO",     "HS"),
    ("Struct", "Te"),
    ("Stoch",  "Svia"),
}
RQ2_OWNCOST_BOLD_ITALIC: set[tuple[str, str]] = set()


# =============================================================================
# Numerical helpers (same scheme as rq3_09)
# =============================================================================
def round_half_up(x: float, ndigits: int = 1, scrub: int = 6) -> float:
    """ROUND_HALF_UP at `ndigits` decimals; scrub float noise at `scrub` decimals first."""
    if x is None or pd.isna(x):
        return float("nan")
    scrubbed = round(float(x), scrub)
    q = Decimal(repr(scrubbed)).quantize(
        Decimal(10) ** -ndigits, rounding=ROUND_HALF_UP
    )
    return float(q)


def fmt_value(v: float, ndigits: int = 1, always_decimal: bool = False,
              thousands: bool = True) -> str:
    """
    Format a numeric value to `ndigits` decimals using ROUND_HALF_UP.

    By default integer-valued cells drop the trailing ".0" (e.g., "100"
    rather than "100.0"); set `always_decimal=True` to keep it.

    `thousands=True` inserts the LaTeX thousands separator "{,}" for
    integer parts >= 1000 (matches "1{,}235.6" in the manuscript).
    """
    if v is None or pd.isna(v):
        return "---"
    r = round_half_up(float(v), ndigits)
    is_integer = (r == int(r))
    if not always_decimal and is_integer:
        s = f"{int(r)}"
    else:
        s = f"{r:.{ndigits}f}"
    if thousands and abs(r) >= 1000:
        # Split into integer / fractional parts and re-insert the LaTeX
        # thousands separator. We only need to handle non-negative numbers
        # in this script.
        if "." in s:
            int_part, frac_part = s.split(".")
        else:
            int_part, frac_part = s, ""
        # Use Python's built-in thousands grouping with "," as a temporary
        # placeholder, then swap to LaTeX-safe "{,}". Doing the grouping
        # via slicing-and-reversing breaks the separator into individual
        # characters when reversed, so we avoid that approach.
        grouped = f"{int(int_part):,}".replace(",", "{,}")
        s = grouped + (("." + frac_part) if frac_part else "")
    return s


def fmt_time(ms: float, wall_clock: bool = False) -> str:
    """
    Format a millisecond duration as a human-readable LaTeX string.

    Two rule sets, matching the manuscript's hand-tuned conventions:

      Serial (default, wall_clock=False):
        - t <    100 s     -> "X.Y\\,s"     (1 decimal)
        - t <  6000 s      -> "X.Y\\,min"   (100 s ... 100 min)
        - else             -> "X.Y\\,h"

      Wall-clock (wall_clock=True):
        - t <    100 s     -> "X.Y\\,s"
        - t <    300 s     -> "X.Y\\,min"   (100 s ... 5 min)
        - else             -> "X.Y\\,h"     (industrial SPLs reported in
                                            hours from ~5 min upward, as
                                            in the manuscript)

    Both rules pick the unit per cell rather than per row.
    """
    if ms is None or pd.isna(ms):
        return "---"
    sec = float(ms) / 1000.0
    if sec < 100:
        return rf"{fmt_value(sec, 1, always_decimal=True)}\,s"
    threshold = 300 if wall_clock else 6000  # boundary between min and h
    if sec < threshold:
        return rf"{fmt_value(sec / 60.0, 1, always_decimal=True)}\,min"
    return rf"{fmt_value(sec / 3600.0, 1, always_decimal=True)}\,h"


def fmt_pct_keep_100_no_decimal(v: float) -> str:
    """
    Format a percentage in the manuscript's RQ1 style:
      - exactly 100   -> "100"   (no trailing ".0")
      - everything else -> "X.Y" (1 decimal, even when the fraction is
                                  zero, e.g., "7.0")
    """
    if v is None or pd.isna(v):
        return "---"
    r = round_half_up(float(v), 1)
    if r == 100.0:
        return "100"
    return f"{r:.1f}"


def decorate(s: str, key: tuple, bold_set: set, bold_italic_set: set) -> str:
    """Wrap `s` in \\textbf{} or \\textbf{\\textit{}} based on key membership."""
    if key in bold_italic_set:
        return r"\textbf{\textit{" + s + r"}}"
    if key in bold_set:
        return r"\textbf{" + s + r"}"
    return s


# =============================================================================
# Path discovery
# =============================================================================
def _first_existing(needle: str, candidates: list[Path]) -> Path:
    """Return the first existing `<candidate>/<needle>` path, else raise."""
    for c in candidates:
        full = c / needle
        if full.exists():
            return full
    raise FileNotFoundError(
        f"Could not find {needle!r} in any of:\n"
        + "\n".join(f"  {c}" for c in candidates)
    )


def find_input_paths() -> dict[str, Path]:
    """
    Resolve the locations of the three input Excel files.

    Production layout (the user's repository):
        statistical_test_scripts/rq1_result/RQ1_SPLSummary_medians.xlsx
        statistical_test_scripts/rq2_result/RQ2_ApproachSummary_medians.xlsx
        statistical_test_scripts/rq2_result/rq2_time_breakdown.xlsx

    A few fallback layouts are tried so the script also works when run
    against a flat data directory (e.g., when iterating in a sandbox).
    """
    rq1_candidates = [
        SCRIPT_DIR / "rq1_result",
        SCRIPT_DIR / "data",
        SCRIPT_DIR,
        Path("/mnt/user-data/uploads"),
    ]
    rq2_candidates = [
        SCRIPT_DIR / "rq2_result",
        SCRIPT_DIR / "data",
        SCRIPT_DIR,
        Path("/mnt/user-data/uploads"),
    ]
    return {
        "rq1_summary":   _first_existing("RQ1_SPLSummary_medians.xlsx",     rq1_candidates),
        "rq2_summary":   _first_existing("RQ2_ApproachSummary_medians.xlsx", rq2_candidates),
        "rq2_breakdown": _first_existing("rq2_time_breakdown.xlsx",          rq2_candidates),
    }


# =============================================================================
# RQ1 — load median data
# =============================================================================
def load_rq1_matrices(rq1_summary_path: Path) -> dict:
    """
    Returns four dicts:
        tgen_matrix       : (approach, L, SPL) -> T_gen in ms
        edgecov_matrix    : (approach, L, SPL) -> EdgeCoverage(%)
        peakmem_matrix    : (approach, L, SPL) -> peak heap memory (MB)
                            (for diagnostic; not used in the LaTeX table)
        execmem_matrix    : (approach, L, SPL) -> peak exec memory (MB)
                            (also diagnostic)
    """
    src = rq1_summary_path
    tgen, edgecov, mem, exec_mem = {}, {}, {}, {}

    # MOGA: ESG-Fx_L2, ESG-Fx_L3, ESG-Fx_L4 sheets.
    for L in ("2", "3", "4"):
        sheet = f"ESG-Fx_L{L}"
        df = pd.read_excel(src, sheet_name=sheet)
        for _, row in df.iterrows():
            spl = str(row["SPL Name"])
            tgen[("MO", L, spl)]    = float(row["TotalTestGenTime(ms)"])
            edgecov[("MO", L, spl)] = float(row["EdgeCoverage(%)"])
            mem[("MO", L, spl)]     = float(row["TestGenPeakMemory(MB)"])

    # Structural baseline: EFG_L2/L3/L4. T_gen here is the GUITAR subprocess
    # wall-clock duration, which the manuscript treats as the disk-to-disk
    # T_gen for the structural baseline.
    for L in ("2", "3", "4"):
        sheet = f"EFG_L{L}"
        df = pd.read_excel(src, sheet_name=sheet)
        for _, row in df.iterrows():
            spl = str(row["SPL Name"])
            tgen[("Struct", L, spl)]    = float(row["GuitarGenTime(ms)"])
            edgecov[("Struct", L, spl)] = float(row["EdgeCoverage(%)"])
            mem[("Struct", L, spl)]     = float(row["TestGenPeakMemory(MB)"])

    # Stochastic baseline: single sheet RandomWalk_L0. Stored under L="".
    df = pd.read_excel(src, sheet_name="RandomWalk_L0")
    for _, row in df.iterrows():
        spl = str(row["SPL Name"])
        tgen[("Stoch", "", spl)]    = float(row["TotalTestGenTime(ms)"])
        edgecov[("Stoch", "", spl)] = float(row["EdgeCoverage(%)"])
        mem[("Stoch", "", spl)]     = float(row["TestGenPeakMemory(MB)"])

    return tgen, edgecov, mem


# =============================================================================
# tab:rq1-tgen
# =============================================================================
def make_rq1_tgen_table(tgen: dict, edgecov: dict, out_path: Path) -> None:
    def cell_tgen(approach: str, L: str, spl: str) -> str:
        ms = tgen.get((approach, L, spl), float("nan"))
        sec = ms / 1000.0 if not pd.isna(ms) else float("nan")
        s = fmt_value(sec, 1, always_decimal=True)
        return decorate(s, (approach, L, spl), RQ1_TGEN_BOLD, RQ1_TGEN_BOLD_ITALIC)

    def cell_edge(approach: str, L: str, spl: str) -> str:
        v = edgecov.get((approach, L, spl), float("nan"))
        s = fmt_pct_keep_100_no_decimal(v)  # "100" if exactly 100, else "X.Y"
        return decorate(s, (approach, L, spl), RQ1_EDGECOV_BOLD, RQ1_EDGECOV_BOLD_ITALIC)

    def cell_speedup(L: str, spl: str) -> str:
        struct_ms = tgen.get(("Struct", L, spl), float("nan"))
        mo_ms     = tgen.get(("MO",     L, spl), float("nan"))
        if pd.isna(struct_ms) or pd.isna(mo_ms) or mo_ms == 0:
            return "---"
        ratio = struct_ms / mo_ms
        s = fmt_value(ratio, 1, always_decimal=True)
        return decorate(s, (L, spl), RQ1_SPEEDUP_BOLD, RQ1_SPEEDUP_BOLD_ITALIC)

    caption = (
        r"RQ1: Median test generation time ($T_{\mathit{gen}}$, disk-to-disk, "
        r"in seconds), median edge coverage (in percent), and speedup at "
        r"$L = 2, 3, 4$ across all eight SPLs. Speedup is the ratio of the "
        r"structural baseline's $T_{\mathit{gen}}$ to that of "
        r"\textit{Model Once, Generate Any}. Stochastic-baseline rows are "
        r"reported once per SPL because the random walk targets edge "
        r"coverage only and does not operate at higher $L$ levels. "
        r"\textbf{Bold} cells indicate values referenced as evidence for "
        r"\textit{Model Once, Generate Any}; "
        r"\textbf{\textit{bold-italic}} cells mark cases where it is at a "
        r"disadvantage."
    )

    lines = []
    lines.append(r"\begin{table*}[width=\textwidth,pos=htbp]")
    lines.append(rf"\caption{{{caption}}}")
    lines.append(r"\label{tab:rq1-tgen}")
    lines.append(r"\setlength{\tabcolsep}{4pt}")
    lines.append(r"\begin{tabularx}{\textwidth}{@{} l c *{8}{Y} @{}}")
    lines.append(r"\toprule")
    lines.append(r"& & \multicolumn{8}{c}{\textbf{Software Product Line$^*$}} \\")
    lines.append(r"\cmidrule(lr){3-10}")
    hdr_cells = " & ".join(rf"\textbf{{{c}}}" for c in COL_ORDER)
    lines.append(rf"\textbf{{Approach}} & \textbf{{$L$}} & {hdr_cells} \\")
    lines.append(r"\midrule")

    # ---- Section: T_gen (seconds) ----
    lines.append(r"\multicolumn{10}{@{}l}{\textbf{$T_{\mathit{gen}}$ (seconds)}} \\")
    lines.append(r"\multirow{3}{*}{\textit{Model Once, Generate Any}}")
    for L in ("2", "3", "4"):
        cells = " & ".join(cell_tgen("MO", L, s) for s in COL_ORDER)
        lines.append(rf"& {L} & {cells} \\")
    lines.append(r"\cmidrule(l{0pt}r{0pt}){2-10}")
    lines.append(r"\multirow{3}{*}{Structural baseline}")
    for L in ("2", "3", "4"):
        cells = " & ".join(cell_tgen("Struct", L, s) for s in COL_ORDER)
        lines.append(rf"& {L} & {cells} \\")
    lines.append(r"\cmidrule(l{0pt}r{0pt}){2-10}")
    cells = " & ".join(cell_tgen("Stoch", "", s) for s in COL_ORDER)
    lines.append(rf"Stochastic baseline & --- & {cells} \\")
    lines.append(r"\midrule")

    # ---- Section: Edge coverage (%) ----
    lines.append(r"\multicolumn{10}{@{}l}{\textbf{Edge coverage (\%)}} \\")
    lines.append(r"\multirow{3}{*}{\textit{Model Once, Generate Any}}")
    for L in ("2", "3", "4"):
        cells = " & ".join(cell_edge("MO", L, s) for s in COL_ORDER)
        lines.append(rf"& {L} & {cells} \\")
    lines.append(r"\cmidrule(l{0pt}r{0pt}){2-10}")
    lines.append(r"\multirow{3}{*}{Structural baseline}")
    for L in ("2", "3", "4"):
        cells = " & ".join(cell_edge("Struct", L, s) for s in COL_ORDER)
        lines.append(rf"& {L} & {cells} \\")
    lines.append(r"\cmidrule(l{0pt}r{0pt}){2-10}")
    cells = " & ".join(cell_edge("Stoch", "", s) for s in COL_ORDER)
    lines.append(rf"Stochastic baseline & --- & {cells} \\")
    lines.append(r"\midrule")

    # ---- Section: Speedup ----
    lines.append(r"\multicolumn{10}{@{}l}{\textbf{Speedup ($\times$)}} \\")
    for L in ("2", "3", "4"):
        cells = " & ".join(cell_speedup(L, s) for s in COL_ORDER)
        lines.append(rf"& {L} & {cells} \\")

    lines.append(r"\bottomrule")
    lines.append(
        r"\multicolumn{10}{@{}l}{\parbox[t]{0.95\textwidth}{\footnotesize "
        r"$^*$~SPL abbreviations: SVM = Soda Vending Machine; eM = eMail; "
        r"El = Elevator; BAv2 = Bank Account; SAS = Student Attendance "
        r"System; Svia = syngo.via; Te = Tesla Web Configurator; "
        r"HS = Hockerty Shirts Web Configurator.}} \\"
    )
    lines.append(r"\end{tabularx}")
    lines.append(r"\end{table*}")

    out_path.write_text("\n".join(lines) + "\n", encoding="utf-8")


# =============================================================================
# RQ2 — load multi-section approach summary
# =============================================================================
# We expose the columns the manuscript tables actually consume. The
# RQ2_ApproachSummary_medians.xlsx file uses two stacked sections per
# sheet; we parse them by row position (rows 2..10 = SERIAL,
# rows 15..23 = WALL-CLOCK).
def _parse_section(df_raw: pd.DataFrame, hdr_row: int, n_data_rows: int = 8) -> pd.DataFrame:
    hdr = df_raw.iloc[hdr_row].tolist()
    body = df_raw.iloc[hdr_row + 1 : hdr_row + 1 + n_data_rows].copy()
    body.columns = hdr
    body = body.reset_index(drop=True)
    return body


def load_rq2_summary(rq2_summary_path: Path) -> dict:
    """
    Returns dict[(approach, L, SPL_short)] -> dict of fields:
        HandledProducts          (int)
        T_pipeline_serial(ms)    (float)
        T_pipeline_wall(ms)      (float)
        Throughput_wall          (float, products / wall-clock second)
        EdgeCoverage(%)          (float)
    For the stochastic baseline we use L="" (the sheet RandomWalk_L0).
    """
    src = rq2_summary_path
    out = {}
    sheets = [
        ("MO",     "2", "ESG-Fx_L2"),
        ("MO",     "3", "ESG-Fx_L3"),
        ("MO",     "4", "ESG-Fx_L4"),
        ("Struct", "2", "EFG_L2"),
        ("Struct", "3", "EFG_L3"),
        ("Struct", "4", "EFG_L4"),
        ("Stoch",  "",  "RandomWalk_L0"),
    ]
    for approach, L, sheet in sheets:
        df_raw = pd.read_excel(src, sheet_name=sheet, header=None)
        serial = _parse_section(df_raw, hdr_row=2)
        wall   = _parse_section(df_raw, hdr_row=15)

        for spl_full, spl_short in SPL_FULL_TO_SHORT.items():
            ser_row  = serial[serial["SPL"] == spl_full]
            wall_row = wall[wall["SPL"] == spl_full]
            if len(ser_row) == 0 or len(wall_row) == 0:
                continue
            sr = ser_row.iloc[0]
            wr = wall_row.iloc[0]

            hp = int(sr["HandledProducts"])
            t_ser_ms  = float(sr["T_pipeline(ms)"])
            t_wall_ms = float(wr["T_pipeline(ms)"])
            wall_sec = t_wall_ms / 1000.0
            throughput = (hp / wall_sec) if wall_sec > 0 else float("nan")
            edge = float(sr["EdgeCoverage(%)"])

            out[(approach, L, spl_short)] = {
                "HandledProducts":          hp,
                "T_pipeline_serial(ms)":    t_ser_ms,
                "T_pipeline_wall(ms)":      t_wall_ms,
                "Throughput_wall":          throughput,
                "EdgeCoverage(%)":          edge,
            }
    return out


# =============================================================================
# tab:rq2-throughput  (L = 2)
# =============================================================================
def make_rq2_throughput_table(rq2: dict, out_path: Path) -> None:
    """
    L = 2 throughput overview. Three approaches x five metric rows.
    """
    def get(approach: str, spl: str, key: str):
        L = "" if approach == "Stoch" else "2"
        d = rq2.get((approach, L, spl))
        if d is None:
            return float("nan")
        return d[key]

    def cell_hp(approach: str, spl: str) -> str:
        v = get(approach, spl, "HandledProducts")
        s = fmt_value(v, 0, thousands=True)
        return decorate(s, (approach, spl), RQ2_HP_BOLD, RQ2_HP_BOLD_ITALIC)

    def cell_tpipe_ser(approach: str, spl: str) -> str:
        ms = get(approach, spl, "T_pipeline_serial(ms)")
        s = fmt_time(ms)
        return decorate(s, (approach, spl), RQ2_TPIPE_SER_BOLD,
                        RQ2_TPIPE_SER_BOLD_ITALIC)

    def cell_tpipe_wall(approach: str, spl: str) -> str:
        ms = get(approach, spl, "T_pipeline_wall(ms)")
        s = fmt_time(ms, wall_clock=True)
        return decorate(s, (approach, spl), RQ2_TPIPE_WALL_BOLD,
                        RQ2_TPIPE_WALL_BOLD_ITALIC)

    def cell_throughput(approach: str, spl: str) -> str:
        v = get(approach, spl, "Throughput_wall")
        s = fmt_value(v, 1, always_decimal=True)
        return decorate(s, (approach, spl), RQ2_THROUGHPUT_BOLD,
                        RQ2_THROUGHPUT_BOLD_ITALIC)

    def cell_edge(approach: str, spl: str) -> str:
        v = get(approach, spl, "EdgeCoverage(%)")
        s = fmt_value(v, 1, always_decimal=True)
        return decorate(s, (approach, spl), RQ2_EDGECOV_BOLD,
                        RQ2_EDGECOV_BOLD_ITALIC)

    APPROACHES = [
        (r"\textit{Model Once, Generate Any}", "MO"),
        (r"Structural baseline",                "Struct"),
        (r"Stochastic baseline",                "Stoch"),
    ]

    caption = (
        r"RQ2: End-to-end throughput at $L = 2$ across the eight SPLs."
        r"\\\\"
        r"\emph{HP}: handled products (cumulative across the 80 shards)."
        r"\\\\"
        r"\emph{$T^{\mathrm{ser}}_{\mathit{pipeline}}$}: serial sum across "
        r"shards (single-core sequential CPU cost)."
        r"\\\\"
        r"\emph{$T^{\mathrm{wall}}_{\mathit{pipeline}}$}: maximum across "
        r"shards (80-core deployment latency)."
        r"\\\\"
        r"\emph{Throughput}: handled products per wall-clock second."
        r"\\\\"
        r"\emph{Edge coverage}: weighted average across shards."
        r"\\\\"
        r"\textbf{Bold} cells indicate values referenced as evidence for "
        r"\textit{Model Once, Generate Any}; "
        r"\textbf{\textit{bold-italic}} cells mark cases where it is at a "
        r"disadvantage."
    )
    # The table caption above embeds explicit \\ to keep the multi-line
    # phrasing of the original. We replace \\\\ with the LaTeX line break
    # at emit time. (We use \\\\ in the f-string to avoid Python escapes.)
    caption = caption.replace(r"\\\\", "")  # collapse to a single paragraph

    lines = []
    lines.append(r"\begin{table*}[width=\textwidth,pos=htbp]")
    lines.append(r"\caption{RQ2: End-to-end throughput at $L = 2$ across the eight SPLs.")
    lines.append(r"\emph{HP}: handled products (cumulative across the 80 shards).")
    lines.append(r"\emph{$T^{\mathrm{ser}}_{\mathit{pipeline}}$}: serial sum across shards (single-core sequential CPU cost).")
    lines.append(r"\emph{$T^{\mathrm{wall}}_{\mathit{pipeline}}$}: maximum across shards (80-core deployment latency).")
    lines.append(r"\emph{Throughput}: handled products per wall-clock second.")
    lines.append(r"\emph{Edge coverage}: weighted average across shards.")
    lines.append(r"\textbf{Bold} cells indicate values referenced as evidence for \textit{Model Once, Generate Any}; \textbf{\textit{bold-italic}} cells mark cases where it is at a disadvantage.}")
    lines.append(r"\label{tab:rq2-throughput}")
    lines.append(r"\setlength{\tabcolsep}{4pt}")
    lines.append(r"\begin{tabularx}{\textwidth}{@{} l *{8}{Y} @{}}")
    lines.append(r"\toprule")
    lines.append(r"& \multicolumn{8}{c}{\textbf{Software Product Line$^*$}} \\")
    lines.append(r"\cmidrule(lr){2-9}")
    hdr_cells = " & ".join(rf"\textbf{{{c}}}" for c in COL_ORDER)
    lines.append(rf"\textbf{{Approach}} & {hdr_cells} \\")
    lines.append(r"\midrule")

    # ---- Section: Handled products ----
    lines.append(r"\multicolumn{9}{@{}l}{\textbf{Handled products}} \\")
    for label, key in APPROACHES:
        cells = " & ".join(cell_hp(key, s) for s in COL_ORDER)
        lines.append(rf"{label} & {cells} \\")
    lines.append(r"\midrule")

    # ---- Section: T_pipeline (serial) ----
    lines.append(
        r"\multicolumn{9}{@{}l}{\textbf{$T^{\mathrm{ser}}_{\mathit{pipeline}}$ "
        r"(cumulative serial cost)}} \\"
    )
    for label, key in APPROACHES:
        cells = " & ".join(cell_tpipe_ser(key, s) for s in COL_ORDER)
        lines.append(rf"{label} & {cells} \\")
    lines.append(r"\midrule")

    # ---- Section: T_pipeline (wall-clock) ----
    lines.append(
        r"\multicolumn{9}{@{}l}{\textbf{$T^{\mathrm{wall}}_{\mathit{pipeline}}$ "
        r"(80-core wall-clock latency)}} \\"
    )
    for label, key in APPROACHES:
        cells = " & ".join(cell_tpipe_wall(key, s) for s in COL_ORDER)
        lines.append(rf"{label} & {cells} \\")
    lines.append(r"\midrule")

    # ---- Section: Throughput ----
    lines.append(
        r"\multicolumn{9}{@{}l}{\textbf{Throughput (products / wall-clock second)}} \\"
    )
    for label, key in APPROACHES:
        cells = " & ".join(cell_throughput(key, s) for s in COL_ORDER)
        lines.append(rf"{label} & {cells} \\")
    lines.append(r"\midrule")

    # ---- Section: Edge coverage ----
    lines.append(r"\multicolumn{9}{@{}l}{\textbf{Edge coverage (\%)}} \\")
    for label, key in APPROACHES:
        cells = " & ".join(cell_edge(key, s) for s in COL_ORDER)
        lines.append(rf"{label} & {cells} \\")

    lines.append(r"\bottomrule")
    lines.append(
        r"\multicolumn{9}{@{}l}{\parbox[t]{0.95\textwidth}{\footnotesize "
        r"$^*$~SPL abbreviations as in Table~\ref{tab:rq1-tgen}.}} \\"
    )
    lines.append(r"\end{tabularx}")
    lines.append(r"\end{table*}")

    out_path.write_text("\n".join(lines) + "\n", encoding="utf-8")


# =============================================================================
# tab:rq2-breakdown  (L = 2)
# =============================================================================
def load_rq2_breakdown(rq2_breakdown_path: Path) -> dict:
    """
    Read pre-computed SAT share and per-product overhead share at L = 2
    from rq2_time_breakdown.xlsx. Both pivots are 8 rows (SPLs) x 4 cols
    (SPL + 3 approach columns).

    Returns dict[(approach_label, SPL_short)] -> {"sat": x, "own": y}.
    """
    src = rq2_breakdown_path
    sat_pv  = pd.read_excel(src, sheet_name="sat_share_pivot")
    own_pv  = pd.read_excel(src, sheet_name="own_cost_share_pivot")

    # Column names in these pivots use full approach labels with the L
    # suffix; map them back to our short keys.
    APPROACH_COLS = {
        "MO":     "Model Once, Generate Any (L=2)",
        "Struct": "Structural Baseline (L=2)",
        "Stoch":  "Stochastic Baseline",
    }

    out = {}
    for _, srow in sat_pv.iterrows():
        spl_short = str(srow["SPL"])
        for approach_key, col in APPROACH_COLS.items():
            sat = float(srow[col])
            # Look up matching own row
            orow = own_pv[own_pv["SPL"] == spl_short].iloc[0]
            own = float(orow[col])
            out[(approach_key, spl_short)] = {"sat": sat, "own": own}
    return out


def make_rq2_breakdown_table(bd: dict, out_path: Path) -> None:
    def cell_sat(approach: str, spl: str) -> str:
        v = bd.get((approach, spl), {}).get("sat", float("nan"))
        s = fmt_value(v, 1, always_decimal=True)
        return decorate(s, (approach, spl), RQ2_SAT_BOLD, RQ2_SAT_BOLD_ITALIC)

    def cell_own(approach: str, spl: str) -> str:
        v = bd.get((approach, spl), {}).get("own", float("nan"))
        s = fmt_value(v, 1, always_decimal=True)
        return decorate(s, (approach, spl), RQ2_OWNCOST_BOLD,
                        RQ2_OWNCOST_BOLD_ITALIC)

    APPROACHES = [
        (r"\textit{Model Once, Generate Any}", "MO"),
        (r"Structural baseline",                "Struct"),
        (r"Stochastic baseline",                "Stoch"),
    ]

    lines = []
    lines.append(r"\begin{table*}[width=\textwidth,pos=htbp]")
    lines.append(
        r"\caption{RQ2: SAT share and per-product overhead share of "
        r"$T^{\mathrm{ser}}_{\mathit{pipeline}}$ at $L = 2$. SAT share is "
        r"the fraction of the pipeline spent in SAT enumeration, which all "
        r"three approaches share. Per-product overhead share is the residual "
        r"fraction spent on the approach's own per-product work "
        r"(transformation, test generation, parsing, execution). The two "
        r"shares sum to approximately $100\%$ for each cell; the remainder "
        r"is the product-instantiation phase, always below $3\%$. "
        r"\textbf{Bold} cells indicate values referenced as evidence for "
        r"\textit{Model Once, Generate Any}; \textbf{\textit{bold-italic}} "
        r"cells mark cases where it is at a disadvantage.}"
    )
    lines.append(r"\label{tab:rq2-breakdown}")
    lines.append(r"\setlength{\tabcolsep}{4pt}")
    lines.append(r"\begin{tabularx}{\textwidth}{@{} l *{8}{Y} @{}}")
    lines.append(r"\toprule")
    lines.append(r"& \multicolumn{8}{c}{\textbf{Software Product Line$^*$}} \\")
    lines.append(r"\cmidrule(lr){2-9}")
    hdr_cells = " & ".join(rf"\textbf{{{c}}}" for c in COL_ORDER)
    lines.append(rf"\textbf{{Approach}} & {hdr_cells} \\")
    lines.append(r"\midrule")

    # ---- Section: SAT share ----
    lines.append(r"\multicolumn{9}{@{}l}{\textbf{SAT share (\%)}} \\")
    for label, key in APPROACHES:
        cells = " & ".join(cell_sat(key, s) for s in COL_ORDER)
        lines.append(rf"{label} & {cells} \\")
    lines.append(r"\midrule")

    # ---- Section: Per-product overhead share ----
    lines.append(r"\multicolumn{9}{@{}l}{\textbf{Per-product overhead share (\%)}} \\")
    for label, key in APPROACHES:
        cells = " & ".join(cell_own(key, s) for s in COL_ORDER)
        lines.append(rf"{label} & {cells} \\")

    lines.append(r"\bottomrule")
    lines.append(
        r"\multicolumn{9}{@{}l}{\parbox[t]{0.95\textwidth}{\footnotesize "
        r"$^*$~SPL abbreviations as in Table~\ref{tab:rq1-tgen}.}} \\"
    )
    lines.append(r"\end{tabularx}")
    lines.append(r"\end{table*}")

    out_path.write_text("\n".join(lines) + "\n", encoding="utf-8")


# =============================================================================
# Verification print  (compare to manuscript)
# =============================================================================
def _print_rq1_verification(tgen, edgecov):
    print("\n" + "=" * 90)
    print("VERIFICATION: tab:rq1-tgen  (cells at displayed precision)")
    print("=" * 90)

    def hdr():
        return f"{'Approach/L':<28s} | " + " | ".join(f"{c:>9s}" for c in COL_ORDER)

    print("\n-- T_gen (seconds) --")
    print(hdr())
    print("-" * len(hdr()))
    for L in ("2", "3", "4"):
        row = []
        for s in COL_ORDER:
            ms = tgen.get(("MO", L, s), float("nan"))
            row.append(fmt_value(ms / 1000.0, 1, always_decimal=True) if not pd.isna(ms) else "---")
        print(f"  Model Once L={L:<22s}".ljust(28) + " | " + " | ".join(f"{c:>9s}" for c in row))
    for L in ("2", "3", "4"):
        row = []
        for s in COL_ORDER:
            ms = tgen.get(("Struct", L, s), float("nan"))
            row.append(fmt_value(ms / 1000.0, 1, always_decimal=True) if not pd.isna(ms) else "---")
        print(f"  Structural L={L:<21s}".ljust(28) + " | " + " | ".join(f"{c:>9s}" for c in row))
    row = []
    for s in COL_ORDER:
        ms = tgen.get(("Stoch", "", s), float("nan"))
        row.append(fmt_value(ms / 1000.0, 1, always_decimal=True) if not pd.isna(ms) else "---")
    print(f"  Stochastic baseline".ljust(28) + " | " + " | ".join(f"{c:>9s}" for c in row))

    print("\n-- Edge coverage (%) --")
    print(hdr())
    print("-" * len(hdr()))
    for L in ("2", "3", "4"):
        row = [fmt_pct_keep_100_no_decimal(edgecov.get(("MO", L, s), float("nan"))) for s in COL_ORDER]
        print(f"  Model Once L={L:<22s}".ljust(28) + " | " + " | ".join(f"{c:>9s}" for c in row))
    for L in ("2", "3", "4"):
        row = [fmt_pct_keep_100_no_decimal(edgecov.get(("Struct", L, s), float("nan"))) for s in COL_ORDER]
        print(f"  Structural L={L:<21s}".ljust(28) + " | " + " | ".join(f"{c:>9s}" for c in row))
    row = [fmt_pct_keep_100_no_decimal(edgecov.get(("Stoch", "", s), float("nan"))) for s in COL_ORDER]
    print(f"  Stochastic baseline".ljust(28) + " | " + " | ".join(f"{c:>9s}" for c in row))

    print("\n-- Speedup (x) --")
    print(hdr())
    print("-" * len(hdr()))
    for L in ("2", "3", "4"):
        row = []
        for s in COL_ORDER:
            sm = tgen.get(("Struct", L, s), float("nan"))
            mm = tgen.get(("MO",     L, s), float("nan"))
            ratio = sm / mm if (mm and not pd.isna(mm) and not pd.isna(sm)) else float("nan")
            row.append(fmt_value(ratio, 1, always_decimal=True))
        print(f"  L={L:<27s}".ljust(28) + " | " + " | ".join(f"{c:>9s}" for c in row))


def _print_rq2_throughput_verification(rq2):
    print("\n" + "=" * 90)
    print("VERIFICATION: tab:rq2-throughput  (L = 2)")
    print("=" * 90)

    def hdr():
        return f"{'Approach/Metric':<32s} | " + " | ".join(f"{c:>10s}" for c in COL_ORDER)

    sections = [
        ("HandledProducts",        "HandledProducts",
            lambda v: fmt_value(v, 0, thousands=True)),
        ("T_pipeline_serial",      "T_pipeline_serial(ms)",
            lambda v: fmt_time(v, wall_clock=False)),
        ("T_pipeline_wall",        "T_pipeline_wall(ms)",
            lambda v: fmt_time(v, wall_clock=True)),
        ("Throughput (wall)",      "Throughput_wall",
            lambda v: fmt_value(v, 1, always_decimal=True)),
        ("EdgeCoverage(%)",        "EdgeCoverage(%)",
            lambda v: fmt_value(v, 1, always_decimal=True)),
    ]

    for label, key, formatter in sections:
        print(f"\n-- {label} --")
        print(hdr())
        print("-" * len(hdr()))
        for app_label, app_key in [("Model Once L=2", "MO"),
                                    ("Structural L=2", "Struct"),
                                    ("Stochastic",    "Stoch")]:
            L = "" if app_key == "Stoch" else "2"
            row = []
            for s in COL_ORDER:
                d = rq2.get((app_key, L, s))
                v = d[key] if d else float("nan")
                row.append(formatter(v))
            print(f"  {app_label:<29s}".ljust(32)
                  + " | " + " | ".join(f"{c:>10s}" for c in row))


def _print_rq2_breakdown_verification(bd):
    print("\n" + "=" * 90)
    print("VERIFICATION: tab:rq2-breakdown  (L = 2)")
    print("=" * 90)

    def hdr():
        return f"{'Approach':<32s} | " + " | ".join(f"{c:>9s}" for c in COL_ORDER)

    print("\n-- SAT share (%) --")
    print(hdr())
    print("-" * len(hdr()))
    for label, key in [("Model Once L=2", "MO"),
                        ("Structural L=2", "Struct"),
                        ("Stochastic",    "Stoch")]:
        row = [fmt_value(bd.get((key, s), {}).get("sat", float("nan")), 1, always_decimal=True)
               for s in COL_ORDER]
        print(f"  {label:<29s}".ljust(32) + " | " + " | ".join(f"{c:>9s}" for c in row))

    print("\n-- Per-product overhead share (%) --")
    print(hdr())
    print("-" * len(hdr()))
    for label, key in [("Model Once L=2", "MO"),
                        ("Structural L=2", "Struct"),
                        ("Stochastic",    "Stoch")]:
        row = [fmt_value(bd.get((key, s), {}).get("own", float("nan")), 1, always_decimal=True)
               for s in COL_ORDER]
        print(f"  {label:<29s}".ljust(32) + " | " + " | ".join(f"{c:>9s}" for c in row))


# =============================================================================
# Main
# =============================================================================
def main() -> None:
    print("=" * 80)
    print("rq12_make_tables: BUILD MANUSCRIPT TABLES FOR RQ1 + RQ2")
    print("=" * 80)

    paths = find_input_paths()
    print("\nInput files:")
    for k, v in paths.items():
        print(f"  {k:<14s} : {v}")
    print(f"Output directory : {TEX_DIR}")

    TEX_DIR.mkdir(parents=True, exist_ok=True)

    # ---- RQ1 ----
    print("\nLoading RQ1 medians ...")
    tgen, edgecov, mem = load_rq1_matrices(paths["rq1_summary"])

    print("Writing tab:rq1-tgen ...")
    out = TEX_DIR / "rq1_table_tgen.tex"
    make_rq1_tgen_table(tgen, edgecov, out)
    print(f"  [OK] {out.name}")

    # ---- RQ2 ----
    print("\nLoading RQ2 throughput summary ...")
    rq2 = load_rq2_summary(paths["rq2_summary"])

    print("Writing tab:rq2-throughput ...")
    out = TEX_DIR / "rq2_table_throughput.tex"
    make_rq2_throughput_table(rq2, out)
    print(f"  [OK] {out.name}")

    print("\nLoading RQ2 time breakdown ...")
    bd = load_rq2_breakdown(paths["rq2_breakdown"])

    print("Writing tab:rq2-breakdown ...")
    out = TEX_DIR / "rq2_table_breakdown.tex"
    make_rq2_breakdown_table(bd, out)
    print(f"  [OK] {out.name}")

    # ---- Verification ----
    _print_rq1_verification(tgen, edgecov)
    _print_rq2_throughput_verification(rq2)
    _print_rq2_breakdown_verification(bd)

    print("\n" + "=" * 80)
    print("DONE.")
    print("=" * 80)


if __name__ == "__main__":
    main()