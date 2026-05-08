#!/usr/bin/env python3
"""
rq3_10_make_supplementary.py

Step 10 of the RQ3 analysis pipeline.

Generates the complete set of LaTeX supplementary tables for the RQ3
fault-detection results. Every value is computed directly from the
per-SPL raw-data Excel files produced by rq3_01 and from the analysis
outputs produced by rq3_05/06/07/08; nothing is hard-coded.

Output (single self-contained file):
    rq3_result/supplementary/rq3_supplementary.tex

The file produces 12 tables, S1--S12, each cross-referenced from the
main manuscript text:

    S1  Mutation Score, edge omission, all approaches and levels.
    S2  Mutation Score, event omission, all approaches and levels.
    S3  Penalized detection cost, edge omission, all approaches and levels.
    S4  Penalized detection cost, event omission, all approaches and levels.
    S5  Unpenalized detection cost, edge omission (excludes unkilled mutants).
    S6  Unpenalized detection cost, event omission.
    S7  Pairwise Wilcoxon results on edge omission (per SPL x pair x metric).
    S8  Pairwise Wilcoxon results on event omission.
    S9  Edge--Event paired Wilcoxon results, per (SPL, approach) detail.
    S10 Damping-factor sensitivity: Friedman omnibus + post-hoc Wilcoxon.
    S11 Multi-seed stability: per-SPL coefficient-of-variation summary.
    S12 Single-seed RandomWalk (RQ3^a) fault detection results.

Numerical precision
-------------------
All percentage values are reported to one decimal place using ROUND_HALF_UP
(school rounding). Float-arithmetic noise is scrubbed by rounding the raw
value to six decimals first, then quantising to one decimal with ROUND_HALF_UP
via the `decimal` module. Integer-valued cells are written without trailing
".0" except where doing so would harm visual cohesion in mixed rows
(see `always_decimal` parameter on individual table builders). The
syngo.via structural baseline at L=2 is shown to two decimal places (3.57)
to match the precision used throughout the thesis CSVs.

Paths (relative to this script's location):
    Script  : files/scripts/statistical_test_scripts/rq3_10_make_supplementary.py
    Inputs  :
        files/Cases/<SPL>/RQ3_<SPL>_perProduct_rawData.xlsx       (rq3_01)
        rq3_result/rq3_pairwise_wilcoxon_<edge|event>_bh.xlsx     (rq3_05)
        rq3_result/rq3_paired_edge_vs_event.xlsx                   (rq3_06)
        rq3_result/rq3_damping_sensitivity.xlsx                    (rq3_07)
        rq3_result/rq3_multiseed_stability.xlsx                    (rq3_08)
    Output  :
        rq3_result/supplementary/rq3_supplementary.tex

Usage:
    python rq3_10_make_supplementary.py
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
SCRIPT_DIR  = Path(__file__).resolve().parent
SCRIPTS_DIR = SCRIPT_DIR.parent
FILES_DIR   = SCRIPTS_DIR.parent
DATA_DIR    = FILES_DIR / "Cases"
RESULT_DIR  = SCRIPT_DIR / "rq3_result"
SUPP_DIR    = RESULT_DIR / "supplementary"


# =============================================================================
# Configuration
# =============================================================================
SPL_FOLDER_TO_SHORT = {
    "SodaVendingMachine":      "SVM",
    "eMail":                   "eM",
    "Elevator":                "El",
    "BankAccountv2":           "BAv2",
    "StudentAttendanceSystem": "SAS",
    "syngovia":                "Svia",
    "Tesla":                   "Te",
    "HockertyShirts":          "HS",
}
COL_ORDER = ["SVM", "eM", "El", "BAv2", "SAS", "Svia", "Te", "HS"]

ESGFX_LEVELS = ["ESG-Fx_L2", "ESG-Fx_L3", "ESG-Fx_L4"]
EFG_LEVELS = ["EFG_L2", "EFG_L3", "EFG_L4"]
RW_LABEL = "RandomWalk"

# Special-case: 2-decimal precision for syngo.via structural baseline at L=2
# (matches the precision shown in the manuscript Table 7 and CSVs).
TWO_DECIMAL_CELLS = {("EFG_L2", "Svia")}


# =============================================================================
# Numerical helpers
# =============================================================================
def round_half_up(x: float, ndigits: int = 1, scrub: int = 6) -> float:
    """ROUND_HALF_UP with float-noise scrubbing."""
    if pd.isna(x):
        return float("nan")
    scrubbed = round(float(x), scrub)
    q = Decimal(repr(scrubbed)).quantize(
        Decimal(10) ** -ndigits, rounding=ROUND_HALF_UP
    )
    return float(q)


def fmt_value(v: float, ndigits: int = 1, always_decimal: bool = False) -> str:
    """Format a percentage with optional trailing-decimal preservation."""
    if pd.isna(v):
        return "---"
    r = round_half_up(float(v), ndigits)
    if not always_decimal and r == int(r):
        return f"{int(r)}"
    fmt = f"{{:.{ndigits}f}}"
    return fmt.format(r)


def fmt_pvalue_sci(p: float) -> str:
    """Format a small p-value as $a\\times 10^{-b}$."""
    if pd.isna(p):
        return "---"
    if p >= 0.05:
        return r"n.s."
    if p == 0.0:
        return r"$<\!10^{-300}$"
    exp = int(np.floor(np.log10(p)))
    mantissa = p / (10 ** exp)
    m_rounded = round_half_up(mantissa, 1)
    if m_rounded >= 10:
        m_rounded /= 10
        exp += 1
    if m_rounded == int(m_rounded):
        m_str = f"{int(m_rounded)}"
    else:
        m_str = f"{m_rounded:.1f}"
    return rf"${m_str}\!\times\!10^{{{exp}}}$"


def fmt_pvalue_inline(p: float) -> str:
    """Format a p-value plainly for tabular cells: 'n.s.' or '0.0123' or '<0.001'."""
    if pd.isna(p):
        return "---"
    if p >= 0.05:
        return "n.s."
    if p < 0.001:
        return r"$<\!0.001$"
    return f"{round_half_up(p, 3):.3f}"


def fmt_a12(a: float) -> str:
    if pd.isna(a):
        return "---"
    return f"{round_half_up(a, 2):.2f}"


def latex_escape_text(s: str) -> str:
    """
    Escape LaTeX special characters that may appear in raw column or metric
    names lifted directly from CSV/Excel headers (e.g. '%', '_', '&', '#').

    Only meant for short literal strings going into table cells — not for
    formatted numeric values or already-LaTeX content.
    """
    if s is None:
        return ""
    s = str(s)
    # Backslash first so we don't double-escape later replacements
    s = s.replace("\\", r"\textbackslash{}")
    s = s.replace("&", r"\&")
    s = s.replace("%", r"\%")
    s = s.replace("$", r"\$")
    s = s.replace("#", r"\#")
    s = s.replace("_", r"\_")
    s = s.replace("{", r"\{")
    s = s.replace("}", r"\}")
    return s


def pretty_approach(name: str) -> str:
    """
    Convert internal approach identifiers to human-readable, LaTeX-safe
    text-mode labels:

        ESG-Fx_L2  -> "MO L=2"
        EFG_L4     -> "Struct L=4"
        RandomWalk -> "Stoch"

    The output contains no LaTeX special characters that would need escaping.
    """
    return (str(name)
            .replace("ESG-Fx_L", "MO L=")
            .replace("EFG_L",    "Struct L=")
            .replace("RandomWalk", "Stoch"))


def pretty_metric(name: str) -> str:
    """
    Shorten the verbose CSV metric identifiers used by the analysis pipeline
    to compact, LaTeX-safe table labels. Anything not in the short-name map
    is escaped via :func:`latex_escape_text` as a fallback so that no raw
    underscore or percent sign reaches the output.
    """
    short = {
        "MutationScore(%)":                            r"Mutation score (\%)",
        "PenalizedPercentageOfSuiteToDetect(%)":       r"Penalized DC (\%)",
        "PenalizedMedianPercentageOfSuiteToDetect(%)": r"Penalized DC (\%)",
        "PercentageOfSuiteToDetect(%)":                r"Unpenalized DC (\%)",
        "MedianPercentageOfSuiteToDetect(%)":          r"Unpenalized DC (\%)",
        "AchievedEdgeCoverage(%)":                     r"Edge coverage (\%)",
        "NumTestCases":                                r"\# test cases",
        "NumTestEvents":                               r"\# test events",
        "StepsTaken":                                  r"Walk steps",
    }
    if name in short:
        return short[name]
    return latex_escape_text(name)


# =============================================================================
# Data access helpers
# =============================================================================
_EXCEL_CACHE: dict[tuple[str, str], pd.DataFrame] = {}


def _read_sheet_cached(spl_folder: str, sheet: str) -> pd.DataFrame:
    """Read a sheet once per (folder, sheet) tuple and reuse."""
    key = (spl_folder, sheet)
    if key not in _EXCEL_CACHE:
        _EXCEL_CACHE[key] = pd.read_excel(_raw_file(spl_folder), sheet_name=sheet)
    return _EXCEL_CACHE[key]


def _raw_file(spl_folder: str) -> Path:
    """Locate the per-SPL RQ3 raw data Excel file.

    The same file can live in several layouts depending on how the
    repository is checked out. Try the legacy `files/Cases/<SPL>/` path
    first, then the flatter layouts where the file sits next to or
    under the script directory.
    """
    name = f"RQ3_{spl_folder}_perProduct_rawData.xlsx"
    candidates = [
        DATA_DIR / spl_folder / name,            # legacy: files/Cases/<SPL>/
        RESULT_DIR / name,                       # flat: rq3_result/
        SCRIPT_DIR / name,                       # next to the script
        SCRIPT_DIR / spl_folder / name,          # script_dir/<SPL>/
    ]
    for c in candidates:
        if c.exists():
            return c
    # Return the legacy path so the downstream `path.exists()` check fails
    # cleanly (and the warning message points at the conventional location).
    return DATA_DIR / spl_folder / name


def median_deterministic(spl_folder: str, approach: str, sheet: str, col: str) -> float:
    df = _read_sheet_cached(spl_folder, sheet)
    sub = df[df["TestingApproach"] == approach]
    arr = sub[col].dropna().to_numpy(dtype=float)
    return float(np.median(arr)) if len(arr) else float("nan")


def median_stochastic(spl_folder: str, ms_sheet: str, col: str) -> float:
    """Per-product median across the 10 seeds, then median across products."""
    df = _read_sheet_cached(spl_folder, ms_sheet)
    pp = df.groupby("ProductID")[col].median()
    arr = pp.dropna().to_numpy(dtype=float)
    return float(np.median(arr)) if len(arr) else float("nan")


def find_project_root() -> Path | None:
    """Locate a directory that looks like a usable project root.

    Accepts both the legacy `files/Cases` layout and the newer layout
    where the script lives in `statistical_test_scripts/` next to a
    `rq3_result/` directory.
    """
    candidates = [Path.cwd(), *Path.cwd().parents]
    try:
        here = Path(__file__).resolve()
        candidates.extend([here.parent, *here.parents])
    except NameError:
        pass
    seen = set()
    for probe in candidates:
        if probe in seen:
            continue
        seen.add(probe)
        if (probe / "files" / "Cases").exists():
            return probe
    seen.clear()
    for probe in candidates:
        if probe in seen:
            continue
        seen.add(probe)
        if (probe / "rq3_result").exists():
            return probe
    return None


# =============================================================================
# Block builders
# =============================================================================
def _descriptive_table(label: str, caption: str,
                       det_sheet: str, det_col: str,
                       ms_sheet: str | None, ms_col: str | None,
                       single_seed_only: bool = False) -> str:
    """
    Build a descriptive table block (S1-S6, S12).

    Article scope only: L = 2, 3, 4 for Model Once, Generate Any (the L = 1
    configuration exists in the raw data but is not part of the manuscript
    scope and is therefore not reported here either).

    For S1-S6 the stochastic baseline row is the per-product 10-seed median
    (ms_sheet, ms_col).
    For S12 (single_seed_only=True), the stochastic row is the single-seed
    median (RandomWalk approach in the deterministic sheet).
    """
    rows: list[tuple[str, str, str]] = []  # (approach_pretty, key, L)

    rows.append((r"\multirow{3}{*}{\textit{Model Once, Generate Any}}",
                 "ESG-Fx_L2", "2"))
    for L in ("3", "4"):
        rows.append(("", f"ESG-Fx_L{L}", L))

    rows.append((r"\multirow{3}{*}{Structural baseline}", "EFG_L2", "2"))
    rows.append(("", "EFG_L3", "3"))
    rows.append(("", "EFG_L4", "4"))
    rows.append(("Stochastic baseline", "STOCH", "---"))

    body_lines = []
    for approach_pretty, key, L in rows:
        if key == "STOCH":
            cells = []
            for spl_short in COL_ORDER:
                folder = next(f for f, s in SPL_FOLDER_TO_SHORT.items() if s == spl_short)
                if single_seed_only:
                    v = median_deterministic(folder, RW_LABEL, det_sheet, det_col)
                else:
                    v = median_stochastic(folder, ms_sheet, ms_col)
                cells.append(fmt_value(v))
            body_lines.append(rf"{approach_pretty} & {L} & " + " & ".join(cells) + r" \\")
        else:
            cells = []
            for spl_short in COL_ORDER:
                folder = next(f for f, s in SPL_FOLDER_TO_SHORT.items() if s == spl_short)
                v = median_deterministic(folder, key, det_sheet, det_col)
                if (key, spl_short) in TWO_DECIMAL_CELLS:
                    cells.append(fmt_value(v, ndigits=2, always_decimal=True))
                else:
                    cells.append(fmt_value(v))
            body_lines.append(rf"{approach_pretty} & {L} & " + " & ".join(cells) + r" \\")

    # Insert midrules after the 3 Model Once rows and before the Stochastic row.
    body_lines.insert(3, r"\midrule")
    body_lines.insert(7, r"\midrule")  # +1 for inserted midrule + 3 EFG rows

    out = []
    out.append(r"\begin{table*}[width=\textwidth,pos=htbp]")
    out.append(rf"\caption{{{caption}}}")
    out.append(rf"\label{{{label}}}")
    out.append(r"\setlength{\tabcolsep}{4pt}")
    out.append(r"\begin{tabularx}{\textwidth}{@{} l c *{8}{Y} @{}}")
    out.append(r"\toprule")
    out.append(r"\textbf{Approach} & \textbf{$L$} & "
               + " & ".join(rf"\textbf{{{c}}}" for c in COL_ORDER) + r" \\")
    out.append(r"\midrule")
    out.extend(body_lines)
    out.append(r"\bottomrule")
    out.append(r"\end{tabularx}")
    out.append(r"\end{table*}")
    return "\n".join(out)


def _pairwise_wilcoxon_table(label: str, caption: str,
                              src: Path, fault_label: str) -> str:
    """
    Build S7/S8 pairwise Wilcoxon table.

    Reads the rq3_05 output (rq3_pairwise_wilcoxon_<edge|event>_bh.xlsx).
    Lists each (SPL, ApproachA, ApproachB, metric) row with W, p_BH, A12,
    magnitude, winner.
    """
    if not src.exists():
        return f"% S7/S8 source missing: {src}\n"

    sheets = {
        "MutationScore":         "MS",
        "DetectionCost_Penalized":"PenDC",
    }
    out = []
    out.append(r"\begin{table*}[width=\textwidth,pos=htbp]")
    out.append(rf"\caption{{{caption}}}")
    out.append(rf"\label{{{label}}}")
    out.append(r"\setlength{\tabcolsep}{3.5pt}")
    out.append(r"\footnotesize")
    out.append(r"\begin{tabular*}{\textwidth}{@{\extracolsep{\fill}} l l l l c r r r l @{}}")
    out.append(r"\toprule")
    out.append(r"\textbf{Metric} & \textbf{SPL} & \textbf{Approach A} & "
               r"\textbf{Approach B} & \textbf{$n$} & "
               r"\textbf{$W$} & \textbf{$p_{\mathit{BH}}$} & "
               r"\textbf{$A_{12}$} & \textbf{Winner} \\")
    out.append(r"\midrule")

    for sheet, metric_pretty in sheets.items():
        try:
            df = pd.read_excel(src, sheet_name=sheet)
        except ValueError:
            continue
        # Sort by SPL (manuscript order) then by pair
        spl_rank = {s: i for i, s in enumerate(COL_ORDER)}
        df = df.assign(_rank=df["SPL"].map(spl_rank).fillna(99))
        df = df.sort_values(["_rank", "ApproachA", "ApproachB"])
        first_in_metric = True
        for _, r in df.iterrows():
            metric_cell = metric_pretty if first_in_metric else ""
            first_in_metric = False
            spl = str(r["SPL"])
            a = pretty_approach(r["ApproachA"])
            b = pretty_approach(r["ApproachB"])
            n = int(r["n_pairs"]) if "n_pairs" in r and not pd.isna(r["n_pairs"]) else "---"
            W = r.get("W", float("nan"))
            W_str = f"{int(W):,}".replace(",", r"{,}") if not pd.isna(W) else "---"
            p_bh = float(r.get("p_BH", float("nan")))
            a12 = float(r.get("A12", float("nan")))
            winner = str(r.get("winner", "")).replace("ApproachA", "A").replace("ApproachB", "B")
            out.append(rf"{metric_cell} & {spl} & {a} & {b} & {n} & "
                       rf"{W_str} & {fmt_pvalue_inline(p_bh)} & "
                       rf"{fmt_a12(a12)} & {winner} \\")
        out.append(r"\midrule")

    # Remove the trailing \midrule by replacing with \bottomrule
    if out[-1] == r"\midrule":
        out[-1] = r"\bottomrule"
    else:
        out.append(r"\bottomrule")
    out.append(r"\end{tabular*}")
    out.append(r"\end{table*}")
    return "\n".join(out)


def _edge_event_paired_full_table(label: str, caption: str) -> str:
    """
    S9: Per-(SPL x approach) detail of the Edge-Event paired tests.

    Article scope (L=1 excluded). Manuscript Table~9 reports SPL-level
    aggregate counts; this table breaks every (SPL, approach) combination
    out individually with median values, delta, p_BH, A12, and winner for
    both the subsumption (mutation score) and earliness (penalized DC) tests.
    """
    src = RESULT_DIR / "rq3_paired_edge_vs_event.xlsx"
    if not src.exists():
        return f"% S9 source missing: {src}\n"

    def _load(sheet: str) -> pd.DataFrame:
        df = pd.read_excel(src, sheet_name=sheet)
        df = df[df["TestingApproach"] != "ESG-Fx_L1"]  # article scope
        # Sort by SPL (manuscript order) then by approach
        spl_rank = {s: i for i, s in enumerate(COL_ORDER)}
        approach_rank = {
            "ESG-Fx_L2": 0, "ESG-Fx_L3": 1, "ESG-Fx_L4": 2,
            "EFG_L2": 3,    "EFG_L3": 4,    "EFG_L4": 5,
            "RandomWalk": 6,
        }
        df = df.assign(
            _spl_rank=df["SPL"].map(spl_rank).fillna(99),
            _ap_rank=df["TestingApproach"].map(approach_rank).fillna(99),
        )
        return df.sort_values(["_spl_rank", "_ap_rank"]).reset_index(drop=True)

    df_sub = _load("paired_score")
    df_earl = _load("paired_cost_penalized")

    out = []
    out.append(r"\begin{table*}[width=\textwidth,pos=htbp]")
    out.append(rf"\caption{{{caption}}}")
    out.append(rf"\label{{{label}}}")
    out.append(r"\setlength{\tabcolsep}{3pt}")
    out.append(r"\footnotesize")
    out.append(r"\begin{tabular*}{\textwidth}"
               r"{@{\extracolsep{\fill}} l l c r r r r l c r r r r l @{}}")
    out.append(r"\toprule")
    out.append(
        r"& & & \multicolumn{5}{c}{\textbf{Subsumption (mutation score, \%)}} "
        r"& & \multicolumn{5}{c}{\textbf{Earliness (penalized DC, \%)}} \\"
    )
    out.append(r"\cmidrule(lr){4-8} \cmidrule(lr){10-14}")
    out.append(
        r"\textbf{SPL} & \textbf{Approach} & \textbf{$n$} & "
        r"\textbf{Edge} & \textbf{Event} & \textbf{$\Delta$} & "
        r"\textbf{$p_{\mathit{BH}}$} & \textbf{$A_{12}$} & "
        r"& \textbf{Edge} & \textbf{Event} & \textbf{$\Delta$} & "
        r"\textbf{$p_{\mathit{BH}}$} & \textbf{$A_{12}$} \\"
    )
    out.append(r"\midrule")

    # Index by (SPL, approach) for cross-lookup
    df_sub_idx = df_sub.set_index(["SPL", "TestingApproach"])
    df_earl_idx = df_earl.set_index(["SPL", "TestingApproach"])

    prev_spl = None
    for _, r in df_sub.iterrows():
        spl = str(r["SPL"])
        spl_cell = spl if spl != prev_spl else ""
        prev_spl = spl
        approach = str(r["TestingApproach"])
        n = int(r["N_pairs"])
        # Subsumption side (mutation score)
        s_edge = fmt_value(float(r["median_edge"]))
        s_evt  = fmt_value(float(r["median_event"]))
        s_dlt  = fmt_value(float(r["delta_event_minus_edge"]), always_decimal=True)
        s_p    = fmt_pvalue_inline(float(r["p_BH"]))
        s_a12  = fmt_a12(float(r["A12_event_vs_edge"]))
        # Earliness side (penalized DC)
        try:
            er = df_earl_idx.loc[(spl, approach)]
            e_edge = fmt_value(float(er["median_edge"]))
            e_evt  = fmt_value(float(er["median_event"]))
            e_dlt  = fmt_value(float(er["delta_event_minus_edge"]), always_decimal=True)
            e_p    = fmt_pvalue_inline(float(er["p_BH"]))
            e_a12  = fmt_a12(float(er["A12_event_vs_edge"]))
        except KeyError:
            e_edge = e_evt = e_dlt = e_p = e_a12 = "---"

        out.append(
            f"{spl_cell} & {pretty_approach(approach)} & {n} & "
            f"{s_edge} & {s_evt} & {s_dlt} & {s_p} & {s_a12} & & "
            f"{e_edge} & {e_evt} & {e_dlt} & {e_p} & {e_a12} \\\\"
        )
        # Cosmetic: visual separator between SPLs
        if approach == "RandomWalk":
            out.append(r"\addlinespace[2pt]")

    out.append(r"\bottomrule")
    out.append(r"\end{tabular*}")
    out.append(r"\end{table*}")
    return "\n".join(out)


def _damping_full_table(label: str, caption: str) -> str:
    """S10: Damping sensitivity Friedman + post-hoc results."""
    src = RESULT_DIR / "rq3_damping_sensitivity.xlsx"
    if not src.exists():
        return f"% S10 source missing: {src}\n"

    out = []
    out.append(r"\begin{table*}[width=\textwidth,pos=htbp]")
    out.append(rf"\caption{{{caption}}}")
    out.append(rf"\label{{{label}}}")
    out.append(r"\setlength{\tabcolsep}{3.5pt}")
    out.append(r"\footnotesize")

    # Sub-table 1: Friedman omnibus across all metrics
    sheet_labels = [
        ("Friedman_Sens_TestGen",     "Test generation"),
        ("Friedman_Sens_EdgeOmission","Edge omission fault detection"),
        ("Friedman_Sens_EventOmission","Event omission fault detection"),
    ]

    out.append(r"\begin{tabular*}{\textwidth}{@{\extracolsep{\fill}} l l c r r r r r @{}}")
    out.append(r"\toprule")
    out.append(r"\textbf{Block} & \textbf{Metric} & \textbf{SPL} & "
               r"\textbf{$n$} & \textbf{$d{=}0.8$} & "
               r"\textbf{$d{=}0.85$} & \textbf{$d{=}0.9$} & "
               r"\textbf{$p_{\mathit{BH}}$} \\")
    out.append(r"\midrule")
    for sheet, block_pretty in sheet_labels:
        try:
            df = pd.read_excel(src, sheet_name=sheet)
        except ValueError:
            continue
        spl_rank = {s: i for i, s in enumerate(COL_ORDER)}
        df = df.assign(_rank=df["SPL"].map(spl_rank).fillna(99))
        df = df.sort_values(["Metric", "_rank"])
        first_in_block = True
        prev_metric = None
        for _, r in df.iterrows():
            block_cell = block_pretty if first_in_block else ""
            first_in_block = False
            metric_now_raw = str(r["Metric"])
            metric_now = pretty_metric(metric_now_raw)
            metric_cell = metric_now if metric_now_raw != prev_metric else ""
            prev_metric = metric_now_raw
            spl = str(r["SPL"])
            n = int(r["n_products"])
            m08 = fmt_value(float(r["median_d=0.8"]), always_decimal=True)
            m085 = fmt_value(float(r["median_d=0.85"]), always_decimal=True)
            m09 = fmt_value(float(r["median_d=0.9"]), always_decimal=True)
            p_bh_str = fmt_pvalue_sci(float(r["Friedman_p_BH"]))
            out.append(rf"{block_cell} & {metric_cell} & {spl} & {n} & "
                       rf"{m08} & {m085} & {m09} & {p_bh_str} \\")
        out.append(r"\midrule")
    if out[-1] == r"\midrule":
        out[-1] = r"\bottomrule"
    else:
        out.append(r"\bottomrule")
    out.append(r"\end{tabular*}")

    # ----------------------------------------------------------------
    # Sub-table 2: Post-hoc Vargha--Delaney A_12 effect sizes for the
    # significant Friedman cells. Listed pairwise (DampingA, DampingB)
    # with the magnitude classification used in the manuscript.
    # ----------------------------------------------------------------
    posthoc_sheets = [
        ("PostHoc_Sens_TestGen",     "Test generation"),
        ("PostHoc_Sens_EdgeOmission","Edge omission fault detection"),
        ("PostHoc_Sens_EventOmission","Event omission fault detection"),
    ]

    out.append("")
    out.append(r"\vspace{0.6em}")
    out.append("")
    out.append(r"\begin{tabular*}{\textwidth}{@{\extracolsep{\fill}} l l c c c c c c @{}}")
    out.append(r"\toprule")
    out.append(r"\textbf{Block} & \textbf{Metric} & \textbf{SPL} & "
               r"\textbf{Pair} & \textbf{$A_{12}$} & "
               r"\textbf{Magnitude} & \textbf{$p_{\mathit{BH}}$} & "
               r"\textbf{Winner} \\")
    out.append(r"\midrule")

    for sheet, block_pretty in posthoc_sheets:
        try:
            df = pd.read_excel(src, sheet_name=sheet)
        except ValueError:
            continue
        if df.empty:
            continue
        # Keep only significant pairs to match the manuscript wording
        # ("the corresponding post-hoc Vargha--Delaney A_12 effect sizes
        # are 'small'") and to keep the table compact.
        df = df[df["significant_BH"].astype(bool)]
        if df.empty:
            continue
        spl_rank = {s: i for i, s in enumerate(COL_ORDER)}
        df = df.assign(_rank=df["SPL"].map(spl_rank).fillna(99))
        df = df.sort_values(["Metric", "_rank", "DampingA", "DampingB"])
        first_in_block = True
        prev_metric = None
        prev_spl    = None
        for _, r in df.iterrows():
            block_cell = block_pretty if first_in_block else ""
            first_in_block = False
            metric_now_raw = str(r["Metric"])
            metric_now = pretty_metric(metric_now_raw)
            metric_cell = metric_now if metric_now_raw != prev_metric else ""
            prev_metric = metric_now_raw
            spl_now = str(r["SPL"])
            spl_cell = spl_now if spl_now != prev_spl else ""
            prev_spl = spl_now
            pair = rf"$d{{=}}{float(r['DampingA']):.2f}$ vs.\ $d{{=}}{float(r['DampingB']):.2f}$"
            a12 = fmt_a12(float(r["A12"]))
            mag = str(r.get("A12_magnitude", "")).strip() or "---"
            p_bh = fmt_pvalue_sci(float(r["p_BH"]))
            winner = str(r.get("winner", "")).strip() or "---"
            out.append(rf"{block_cell} & {metric_cell} & {spl_cell} & "
                       rf"{pair} & {a12} & {mag} & {p_bh} & {winner} \\")
        out.append(r"\midrule")

    if out[-1] == r"\midrule":
        out[-1] = r"\bottomrule"
    else:
        out.append(r"\bottomrule")
    out.append(r"\end{tabular*}")
    out.append(r"\end{table*}")
    return "\n".join(out)


def _stability_full_table(label: str, caption: str) -> str:
    """S11: Multi-seed stability summary, edge & event."""
    src = RESULT_DIR / "rq3_multiseed_stability.xlsx"
    if not src.exists():
        return f"% S11 source missing: {src}\n"

    out = []
    out.append(r"\begin{table*}[width=\textwidth,pos=htbp]")
    out.append(rf"\caption{{{caption}}}")
    out.append(rf"\label{{{label}}}")
    out.append(r"\setlength{\tabcolsep}{4pt}")
    out.append(r"\footnotesize")
    out.append(r"\begin{tabular*}{\textwidth}{@{\extracolsep{\fill}} l l l c r r r r r l @{}}")
    out.append(r"\toprule")
    out.append(r"\textbf{Operator} & \textbf{SPL} & \textbf{Metric} & \textbf{$n$} & "
               r"\textbf{Median CV (\%)} & \textbf{P25 (\%)} & "
               r"\textbf{P75 (\%)} & \textbf{IQR (\%)} & "
               r"\textbf{Max (\%)} & \textbf{Band} \\")
    out.append(r"\midrule")
    for op_label, sheet in [("Edge omission",  "Stability_edge_summary"),
                             ("Event omission", "Stability_event_summary")]:
        try:
            df = pd.read_excel(src, sheet_name=sheet)
        except ValueError:
            continue
        spl_rank = {s: i for i, s in enumerate(COL_ORDER)}
        df = df.assign(_rank=df["SPL"].map(spl_rank).fillna(99))
        df = df.sort_values(["_rank", "Metric"])
        first_in_block = True
        prev_spl = None
        for _, r in df.iterrows():
            block_cell = op_label if first_in_block else ""
            first_in_block = False
            spl_now = str(r["SPL"])
            spl_cell = spl_now if spl_now != prev_spl else ""
            prev_spl = spl_now
            metric = pretty_metric(str(r["Metric"]))
            n = int(r["n_products"])
            med = fmt_value(float(r["median_CV_percent"]), always_decimal=True)
            p25 = fmt_value(float(r["P25_CV_percent"]), always_decimal=True)
            p75 = fmt_value(float(r["P75_CV_percent"]), always_decimal=True)
            iqr = fmt_value(float(r["IQR_CV_percent"]), always_decimal=True)
            mx  = fmt_value(float(r["max_CV_percent"]), always_decimal=True)
            band = str(r["stability_band"]).replace("_", r"\_")
            out.append(rf"{block_cell} & {spl_cell} & {metric} & {n} & "
                       rf"{med} & {p25} & {p75} & {iqr} & {mx} & {band} \\")
        out.append(r"\midrule")
    if out[-1] == r"\midrule":
        out[-1] = r"\bottomrule"
    else:
        out.append(r"\bottomrule")
    out.append(r"\end{tabular*}")
    out.append(r"\end{table*}")
    return "\n".join(out)


# =============================================================================
# Main: assemble a single LaTeX document
# =============================================================================
PREAMBLE = r"""% =========================================================================
% RQ3 Supplementary Material
% Auto-generated by rq3_10_make_supplementary.py
% Every value computed from the raw data; do not edit by hand.
% =========================================================================
%
% This file can be either compiled standalone (uncomment the documentclass
% block below) or \input into the main supplementary tex file. Tables are
% labelled tab:rq3-supp-S1 ... tab:rq3-supp-S12 and counted independently
% of the manuscript table counter (use \renewcommand{\thetable}{S\arabic{table}}
% in the parent document).
%
% \documentclass[a4paper,fleqn]{cas-sc}
% \usepackage{booktabs,multirow,tabularx}
% \newcolumntype{Y}{>{\centering\arraybackslash}X}
% \newcolumntype{L}{>{\raggedright\arraybackslash}X}
% \begin{document}

\section*{S-RQ3. Supplementary Material for RQ3 (Fault Detection)}
\label{sec:supp_rq3}

This appendix collects the supplementary tables referenced in the RQ3
results section (Section~\ref{sec:results_rq3}) of the main manuscript.
Tables S1--S6 give the full descriptive statistics for both mutation
operators at all coverage levels reported in the article. Tables S7
and S8 give the full pairwise Wilcoxon results that back the cross-approach
claims. Table S9 breaks the Edge--Event paired comparisons out by
individual (SPL, approach) combination, with median values, paired
delta, $p_{\mathit{BH}}$, and $A_{12}$ effect size; the manuscript
table reports SPL-level aggregate counts only. Tables S10 and S11
report the damping-factor sensitivity and
the multi-seed stability analyses in full. Table S12 gives the
fault-detection results obtained from the original RQ1 single-seed
Random Walk sequences (the RQ3$^{a}$ test-suite set in
Table~\ref{tab:baseline_config}); the corresponding manuscript
results use the multi-seed median (RQ3$^{b}$).

All percentage values are rounded to one decimal place using
ROUND\_HALF\_UP after a six-decimal float-noise scrub. The syngo.via
structural baseline value at $L = 2$ is shown to two decimal places
for consistency with the precision used elsewhere in the supplementary
data files.
"""


CAPTION_S1 = (
    r"S1: Median mutation score on edge omission mutants (\%), per approach, "
    r"$L$ level, and SPL. Stochastic baseline rows aggregate the per-product "
    r"median across the 10 seeds (RQ3$^{b}$) and then the median across "
    r"products."
)
CAPTION_S2 = (
    r"S2: Median mutation score on event omission mutants (\%). "
    r"Same layout and aggregation as Table~\ref{tab:rq3-supp-S1}."
)
CAPTION_S3 = (
    r"S3: Median penalized percent of suite to detect on edge omission "
    r"mutants (\%). Mutants the suite fails to kill are charged a $100\%$ "
    r"cost (Section~\ref{sec:rq3_methodology}); rows of 100.0 reflect "
    r"pervasive mutant survival rather than late detection."
)
CAPTION_S4 = (
    r"S4: Median penalized percent of suite to detect on event omission "
    r"mutants (\%). Same layout as Table~\ref{tab:rq3-supp-S3}."
)
CAPTION_S5 = (
    r"S5: Median \emph{unpenalized} percent of suite to detect on edge "
    r"omission mutants (\%). Computed only over mutants the suite kills "
    r"and excludes failures from the average. Compare with "
    r"Table~\ref{tab:rq3-supp-S3}: the two coincide wherever the mutation "
    r"score reaches $100\%$, but the unpenalized variant is much lower for "
    r"the structural baseline on eMail, syngo.via, and Hockerty Shirts "
    r"because it discards the unkilled mutants from the average."
)
CAPTION_S6 = (
    r"S6: Median \emph{unpenalized} percent of suite to detect on event "
    r"omission mutants (\%). Same caveat as Table~\ref{tab:rq3-supp-S5}."
)
CAPTION_S7 = (
    r"S7: Pairwise Wilcoxon signed-rank results on edge omission "
    r"(Benjamini--Hochberg corrected, $\alpha = 0.05$). MO denotes "
    r"\textit{Model Once, Generate Any} and Struct denotes the structural "
    r"baseline. The winner column reports which side of each comparison "
    r"is favoured by the Vargha--Delaney $A_{12}$ value."
)
CAPTION_S8 = (
    r"S8: Pairwise Wilcoxon signed-rank results on event omission "
    r"(Benjamini--Hochberg corrected, $\alpha = 0.05$). Same conventions "
    r"as Table~\ref{tab:rq3-supp-S7}."
)
CAPTION_S9 = (
    r"S9: Per-(SPL, approach) detail of the Edge--Event paired Wilcoxon "
    r"tests reported in aggregate in Table~\ref{tab:rq3-paired}. Each row "
    r"corresponds to one of the $56$ SPL$\times$approach combinations: "
    r"$8$ SPLs times the $7$ approaches reported in this article. Median "
    r"edge / event values, paired delta (event minus edge), "
    r"Benjamini--Hochberg corrected $p$-value, and Vargha--Delaney "
    r"$A_{12}$ are listed for both the subsumption (mutation score) and "
    r"earliness (penalized DC) tests. Approach abbreviations: MO = "
    r"\textit{Model Once, Generate Any}, Struct = structural baseline, "
    r"Stoch = stochastic baseline."
)
CAPTION_S10 = (
    r"S10: Damping-factor sensitivity, full results. The upper sub-table "
    r"reports the Friedman omnibus tests: median per-SPL value at each "
    r"damping factor and the Benjamini--Hochberg adjusted $p$-value across "
    r"the three settings. Cells with $p_{\mathit{BH}} > 0.05$ are reported "
    r"as n.s. The number of paired products $n$ is the count of products "
    r"with all three damping settings recorded. The lower sub-table "
    r"reports the post-hoc Vargha--Delaney $A_{12}$ effect sizes for the "
    r"pairwise comparisons within each significant Friedman cell, with the "
    r"magnitude classification (negligible / small / medium / large) used "
    r"in the manuscript and the corresponding adjusted $p$-value."
)
CAPTION_S11 = (
    r"S11: Multi-seed stability of the stochastic baseline across 10 seeds "
    r"(RQ3$^{b}$). The coefficient-of-variation (CV) is computed per "
    r"product across seeds; the table reports the per-SPL distribution "
    r"summary. Bands are very\_stable (median CV $< 5\%$), stable "
    r"(5--10\%), and notable\_variance ($> 10\%$)."
)
CAPTION_S12 = (
    r"S12: RQ3$^{a}$ fault-detection results. Mutation score and penalized "
    r"detection cost on edge and event omission for the original RQ1 "
    r"Random Walk test sequences (single seed $42 + \mathit{productID}$, "
    r"damping $0.85$). Stochastic baseline rows here are single-seed "
    r"medians; the corresponding manuscript tables (Tables~\ref{tab:rq3-ms-edge} "
    r"and~\ref{tab:rq3-dc-edge}) use the multi-seed median (RQ3$^{b}$)."
)


def main() -> None:
    print("=" * 80)
    print("rq3_10: BUILD RQ3 SUPPLEMENTARY MATERIAL")
    print("=" * 80)

    project_root = find_project_root()
    if project_root is None:
        print("WARNING: project root not detected. Falling back to script "
              "directory; per-SPL RQ3 raw data files will be looked up via "
              "the resolver in `_raw_file`. Most outputs in "
              f"{RESULT_DIR} are still usable.")

    print(f"Script  : {SCRIPT_DIR}")
    print(f"Cases   : {DATA_DIR}")
    print(f"Result  : {RESULT_DIR}")
    SUPP_DIR.mkdir(parents=True, exist_ok=True)

    parts: list[str] = [PREAMBLE, ""]

    # ---- S1, S2: Mutation Score (edge, event) ----
    print("\nBuilding S1 (Mutation Score, edge omission) ...")
    parts.append(_descriptive_table(
        "tab:rq3-supp-S1", CAPTION_S1,
        "EdgeOmission", "MutationScore(%)",
        "EdgeOmission_MultiSeed", "MutationScore(%)",
    ))
    parts.append("")
    print("Building S2 (Mutation Score, event omission) ...")
    parts.append(_descriptive_table(
        "tab:rq3-supp-S2", CAPTION_S2,
        "EventOmission", "MutationScore(%)",
        "EventOmission_MultiSeed", "MutationScore(%)",
    ))
    parts.append("")

    # ---- S3, S4: Penalized DC (edge, event) ----
    print("Building S3 (Penalized DC, edge omission) ...")
    parts.append(_descriptive_table(
        "tab:rq3-supp-S3", CAPTION_S3,
        "EdgeOmission", "PenalizedPercentageOfSuiteToDetect(%)",
        "EdgeOmission_MultiSeed", "PenalizedMedianPercentageOfSuiteToDetect(%)",
    ))
    parts.append("")
    print("Building S4 (Penalized DC, event omission) ...")
    parts.append(_descriptive_table(
        "tab:rq3-supp-S4", CAPTION_S4,
        "EventOmission", "PenalizedPercentageOfSuiteToDetect(%)",
        "EventOmission_MultiSeed", "PenalizedMedianPercentageOfSuiteToDetect(%)",
    ))
    parts.append("")

    # ---- S5, S6: Unpenalized DC (edge, event) ----
    print("Building S5 (Unpenalized DC, edge omission) ...")
    parts.append(_descriptive_table(
        "tab:rq3-supp-S5", CAPTION_S5,
        "EdgeOmission", "PercentageOfSuiteToDetect(%)",
        "EdgeOmission_MultiSeed", "MedianPercentageOfSuiteToDetect(%)",
    ))
    parts.append("")
    print("Building S6 (Unpenalized DC, event omission) ...")
    parts.append(_descriptive_table(
        "tab:rq3-supp-S6", CAPTION_S6,
        "EventOmission", "PercentageOfSuiteToDetect(%)",
        "EventOmission_MultiSeed", "MedianPercentageOfSuiteToDetect(%)",
    ))
    parts.append("")

    # ---- S7, S8: Pairwise Wilcoxon ----
    print("Building S7 (Pairwise Wilcoxon, edge omission) ...")
    parts.append(_pairwise_wilcoxon_table(
        "tab:rq3-supp-S7", CAPTION_S7,
        RESULT_DIR / "rq3_pairwise_wilcoxon_edge_bh.xlsx",
        "edge omission",
    ))
    parts.append("")
    print("Building S8 (Pairwise Wilcoxon, event omission) ...")
    parts.append(_pairwise_wilcoxon_table(
        "tab:rq3-supp-S8", CAPTION_S8,
        RESULT_DIR / "rq3_pairwise_wilcoxon_event_bh.xlsx",
        "event omission",
    ))
    parts.append("")

    # ---- S9: Edge-Event paired per-(SPL, approach) detail ----
    print("Building S9 (Edge-Event paired per-combination detail) ...")
    parts.append(_edge_event_paired_full_table("tab:rq3-supp-S9", CAPTION_S9))
    parts.append("")

    # ---- S10: Damping sensitivity full ----
    print("Building S10 (Damping sensitivity, full results) ...")
    parts.append(_damping_full_table("tab:rq3-supp-S10", CAPTION_S10))
    parts.append("")

    # ---- S11: Stability summary ----
    print("Building S11 (Multi-seed stability summary) ...")
    parts.append(_stability_full_table("tab:rq3-supp-S11", CAPTION_S11))
    parts.append("")

    # ---- S12: Single-seed RW (RQ3^a) descriptive ----
    print("Building S12 (RQ3^a single-seed Random Walk fault detection) ...")
    parts.append(_descriptive_table(
        "tab:rq3-supp-S12", CAPTION_S12,
        "EdgeOmission", "MutationScore(%)",
        None, None,
        single_seed_only=True,
    ))
    parts.append("")

    out_path = SUPP_DIR / "rq3_supplementary.tex"
    out_path.write_text("\n".join(parts), encoding="utf-8")

    print()
    print("=" * 80)
    print(f"DONE.  Wrote {out_path}")
    print(f"        ({len(parts)//2 - 1} table blocks)")
    print("=" * 80)


if __name__ == "__main__":
    main()