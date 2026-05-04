#!/usr/bin/env python3
"""
rq3_09_make_tables.py

Step 9 of the RQ3 analysis pipeline.

Generates manuscript-ready LaTeX tables and supplementary Excel files for
the RQ3 fault detection results.

Manuscript outputs (rq3_result/manuscript_tables/):
    rq3_table_07_edge_mutationscore.tex
        Median mutation score on edge omission per (approach, L, SPL).
    rq3_table_08_edge_detectioncost.tex
        Median penalized % of suite to detect on edge omission per
        (approach, L, SPL).
    rq3_table_09_edge_event_paired.tex
        Per-SPL win counts for the paired Edge-vs-Event tests:
        subsumption (MutationScore) and earliness (penalized DC).
    rq3_table_10_damping_highlights.tex
        Damping factor sensitivity highlights for the stochastic baseline:
        edge coverage and penalized DC at d=0.8/0.85/0.9, with Friedman
        p_BH values.

Supplementary outputs (rq3_result/supplementary/):
    rq3_supp_event_omission_descriptive.xlsx
        Tables 7 & 8 equivalents but for event omission.
    rq3_supp_pairwise_full_edge.xlsx
        Full per-(SPL x pair x metric) paired Wilcoxon results, edge.
    rq3_supp_pairwise_full_event.xlsx
        Same, event.
    rq3_supp_paired_edge_vs_event_full.xlsx
        Copy of rq3_06 output (paired Edge vs Event).
    rq3_supp_damping_friedman_full.xlsx
        Full Friedman + post-hoc table for all (sheet x SPL x metric x pair).
    rq3_supp_multiseed_stability_full.xlsx
        Copy of rq3_08 output (per-product CV%; per-cell summaries).

Numerical precision
-------------------
All percentage values are reported to one decimal place using ROUND_HALF_UP
(school rounding). Float-arithmetic noise (e.g., 19.549999... arising from
median computation) is scrubbed by rounding the raw value to six decimals
first, then quantizing to one decimal with ROUND_HALF_UP via the `decimal`
module. Integer-valued cells are written without a trailing ".0".

Paths (relative to this script's location):
    Script  : files/scripts/statistical_test_scripts/rq3_09_make_tables.py
    Inputs  :
        files/Cases/<SPL>/RQ3_<SPL>_perProduct_rawData.xlsx       (rq3_01)
        rq3_result/rq3_pairwise_wilcoxon_<edge|event>_bh.xlsx     (rq3_05)
        rq3_result/rq3_paired_edge_vs_event.xlsx                   (rq3_06)
        rq3_result/rq3_damping_sensitivity.xlsx                    (rq3_07)
        rq3_result/rq3_multiseed_stability.xlsx                    (rq3_08)
    Outputs :
        rq3_result/manuscript_tables/<table>.tex
        rq3_result/supplementary/<file>.xlsx

Usage:
    python rq3_09_make_tables.py
"""
from __future__ import annotations

import shutil
import sys
from decimal import Decimal, ROUND_HALF_UP
from pathlib import Path

import numpy as np
import pandas as pd
from openpyxl.utils import get_column_letter


# =============================================================================
# Paths
# =============================================================================
SCRIPT_DIR  = Path(__file__).resolve().parent
SCRIPTS_DIR = SCRIPT_DIR.parent
FILES_DIR   = SCRIPTS_DIR.parent
DATA_DIR    = FILES_DIR / "Cases"
RESULT_DIR  = SCRIPT_DIR / "rq3_result"
TEX_DIR     = RESULT_DIR / "manuscript_tables"
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
EFG_LEVELS   = ["EFG_L2",    "EFG_L3",    "EFG_L4"]
RW_LABEL     = "RandomWalk"

# Cells in Table 7 (Edge MS) where Model Once, Generate Any is at a disadvantage.
# Format: (approach_label, L, SPL_short).
TABLE7_BOLD_ITALIC = {
    ("MO", "4", "eM"),
    ("MO", "4", "BAv2"),
}
# Cells in Table 7 supporting Model Once (structural baseline failures).
TABLE7_BOLD = {
    ("Struct", "2", "eM"), ("Struct", "2", "SAS"),
    ("Struct", "2", "Svia"), ("Struct", "2", "HS"),
    ("Struct", "3", "eM"), ("Struct", "3", "SAS"),
    ("Struct", "3", "HS"),
    ("Struct", "4", "eM"), ("Struct", "4", "SAS"),
    ("Struct", "4", "HS"),
}
# Cells where we want 2-decimal precision in Table 7 (syngo.via structural
# baseline at L=2 is reported as 3.57 throughout the thesis tables and CSVs;
# 1-decimal would round to 3.6 and create confusion across documents).
TABLE7_TWO_DECIMAL = {
    ("Struct", "2", "Svia"),
}

# Cells in Table 8 (Edge DC) where structural baseline penalty kicks in.
TABLE8_BOLD = {
    ("Struct", "2", "eM"), ("Struct", "2", "Svia"),
    ("Struct", "2", "HS"), ("Struct", "3", "HS"),
    ("Struct", "4", "HS"),
}
TABLE8_BOLD_ITALIC: set[tuple[str, str, str]] = set()


# =============================================================================
# Numerical helpers
# =============================================================================
def round_half_up(x: float, ndigits: int = 1, scrub: int = 6) -> float:
    """
    Round x to `ndigits` decimals using ROUND_HALF_UP. Float-arithmetic noise
    is scrubbed by first rounding to `scrub` decimals (banker's rounding at
    that precision absorbs e.g. 19.549999... -> 19.55).
    """
    if pd.isna(x):
        return float("nan")
    scrubbed = round(float(x), scrub)
    q = Decimal(repr(scrubbed)).quantize(
        Decimal(10) ** -ndigits, rounding=ROUND_HALF_UP
    )
    return float(q)


def fmt_value(v: float, ndigits: int = 1, always_decimal: bool = False) -> str:
    """Format a percentage to `ndigits` decimals.

    By default integer-valued cells are written without a trailing ".0"
    (matching the RQ1 manuscript style: e.g.\\ "100" rather than "100.0").

    Set `always_decimal=True` for tables where preserving the trailing
    decimal aids visual cohesion across a row that mixes integer-valued
    and fractional cells (e.g.\\ Table 10's HS row: "61.3 / 96.0 / 100.0").
    """
    if pd.isna(v):
        return "---"
    r = round_half_up(float(v), ndigits)
    fmt = f"{{:.{ndigits}f}}"
    if not always_decimal and r == int(r):
        return f"{int(r)}"
    return fmt.format(r)


def fmt_pvalue_sci(p: float) -> str:
    """Format a small p-value as $a\\times 10^{-b}$ for tables."""
    if pd.isna(p):
        return "---"
    if p >= 0.05:
        return r"n.s."
    if p == 0.0:
        return r"$<\!10^{-300}$"
    # Round mantissa to one decimal half-up
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


# =============================================================================
# Path discovery
# =============================================================================
def find_project_root() -> Path | None:
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
    return None


# =============================================================================
# Median computations from raw data
# =============================================================================
def _raw_file(spl_folder: str) -> Path:
    folder = DATA_DIR / spl_folder
    candidates = [
        folder / f"RQ3_{spl_folder}_perProduct_rawData.xlsx",
        folder / f"RQ3_{spl_folder}_perProduct_rawData_.xlsx",
    ]
    for p in candidates:
        if p.exists():
            return p
    raise FileNotFoundError(f"No raw data file in {folder}")


def median_deterministic(spl_folder: str, approach: str, sheet: str, col: str) -> float:
    df = pd.read_excel(_raw_file(spl_folder), sheet_name=sheet)
    sub = df[df["TestingApproach"] == approach]
    arr = sub[col].dropna().to_numpy(dtype=float)
    return float(np.median(arr)) if len(arr) else float("nan")


def median_stochastic(spl_folder: str, ms_sheet: str, col: str) -> float:
    """Per-product median across the 10 seeds, then median across products."""
    df = pd.read_excel(_raw_file(spl_folder), sheet_name=ms_sheet)
    pp = df.groupby("ProductID")[col].median()
    arr = pp.dropna().to_numpy(dtype=float)
    return float(np.median(arr)) if len(arr) else float("nan")


# =============================================================================
# Table 7 / Table 8 — Edge omission descriptive
# =============================================================================
def _build_descriptive_matrix(det_sheet: str, det_col: str,
                              ms_sheet: str, ms_col: str) -> dict:
    """
    Return a dict keyed by (approach_label, L_str, SPL_short) -> median value.
    approach_label is one of {"MO", "Struct", "Stoch"}.
    L_str is "2"/"3"/"4" for MO/Struct, "" for Stoch.
    """
    matrix = {}
    for spl_folder, spl_short in SPL_FOLDER_TO_SHORT.items():
        for a in ESGFX_LEVELS:
            v = median_deterministic(spl_folder, a, det_sheet, det_col)
            matrix[("MO", a[-1], spl_short)] = v
        for a in EFG_LEVELS:
            v = median_deterministic(spl_folder, a, det_sheet, det_col)
            matrix[("Struct", a[-1], spl_short)] = v
        v = median_stochastic(spl_folder, ms_sheet, ms_col)
        matrix[("Stoch", "", spl_short)] = v
    return matrix


def _emit_descriptive_tex(
    out_path: Path,
    matrix: dict,
    bold_set: set,
    bold_italic_set: set,
    label: str,
    caption: str,
    two_decimal_set: set | None = None,
) -> None:
    two_decimal_set = two_decimal_set or set()

    def render_cell(approach: str, L: str, spl: str) -> str:
        v = matrix.get((approach, L, spl), float("nan"))
        key = (approach, L, spl)
        if key in two_decimal_set:
            s = fmt_value(v, ndigits=2, always_decimal=True)
        else:
            s = fmt_value(v)
        if key in bold_italic_set:
            return r"\textbf{\textit{" + s + r"}}"
        if key in bold_set:
            return r"\textbf{" + s + r"}"
        return s

    lines = []
    lines.append(r"\begin{table*}[width=\textwidth,pos=htbp]")
    lines.append(rf"\caption{{{caption}}}")
    lines.append(rf"\label{{{label}}}")
    lines.append(r"\setlength{\tabcolsep}{4pt}")
    lines.append(r"\begin{tabularx}{\textwidth}{@{} l c *{8}{Y} @{}}")
    lines.append(r"\toprule")
    lines.append(r"& & \multicolumn{8}{c}{\textbf{Software Product Line$^*$}} \\")
    lines.append(r"\cmidrule(lr){3-10}")
    hdr_cells = " & ".join(rf"\textbf{{{c}}}" for c in COL_ORDER)
    lines.append(rf"\textbf{{Approach}} & \textbf{{$L$}} & {hdr_cells} \\")
    lines.append(r"\midrule")

    # Model Once, Generate Any
    lines.append(r"\multirow{3}{*}{\textit{Model Once, Generate Any}}")
    for L in ("2", "3", "4"):
        cells = " & ".join(render_cell("MO", L, s) for s in COL_ORDER)
        lines.append(rf"& {L} & {cells} \\")
    lines.append(r"\cmidrule(l{0pt}r{0pt}){2-10}")

    # Structural baseline
    lines.append(r"\multirow{3}{*}{Structural baseline}")
    for L in ("2", "3", "4"):
        cells = " & ".join(render_cell("Struct", L, s) for s in COL_ORDER)
        lines.append(rf"& {L} & {cells} \\")
    lines.append(r"\cmidrule(l{0pt}r{0pt}){2-10}")

    # Stochastic baseline
    cells = " & ".join(render_cell("Stoch", "", s) for s in COL_ORDER)
    lines.append(rf"Stochastic baseline & --- & {cells} \\")

    lines.append(r"\bottomrule")
    lines.append(
        r"\multicolumn{10}{@{}l}{\parbox[t]{0.95\textwidth}{\footnotesize "
        r"$^*$~SPL abbreviations as in Table~\ref{tab:rq1-tgen}.}} \\"
    )
    lines.append(r"\end{tabularx}")
    lines.append(r"\end{table*}")
    out_path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def make_table_07(matrix_ms: dict) -> None:
    caption = (
        "RQ3: Median mutation score on edge omission mutants (in percent), "
        "per approach, $L$ level, and SPL, computed across the 4{,}455 product "
        "samples used in RQ1 and reused here. Stochastic baseline rows aggregate "
        "the per-product median across the 10 seeds and then the median across "
        "products. \\textbf{Bold} cells indicate values referenced as evidence "
        "for \\textit{Model Once, Generate Any}; "
        "\\textbf{\\textit{bold-italic}} cells mark cases where it is at a "
        "disadvantage. The syngo.via structural baseline value at $L = 2$ is "
        "reported to two decimal places to match the corresponding entries in "
        "the supplementary material."
    )
    _emit_descriptive_tex(
        TEX_DIR / "rq3_table_07_edge_mutationscore.tex",
        matrix_ms,
        TABLE7_BOLD,
        TABLE7_BOLD_ITALIC,
        "tab:rq3-ms-edge",
        caption,
        two_decimal_set=TABLE7_TWO_DECIMAL,
    )


def make_table_08(matrix_dc: dict) -> None:
    caption = (
        "RQ3: Median penalized percent of suite to detect on edge omission "
        "mutants, per approach, $L$ level, and SPL. The penalized metric "
        "charges a 100\\% cost for each mutant the suite fails to kill, so "
        "a row of 100.0 (e.g.\\ structural baseline on Hockerty Shirts) "
        "reflects pervasive mutant survival rather than a suite that only "
        "detects mutants on its very last test. Lower values indicate "
        "earlier detection. Stochastic baseline rows aggregate the 10-seed "
        "per-product median. \\textbf{Bold} cells indicate values "
        "referenced as evidence for \\textit{Model Once, Generate Any}."
    )
    _emit_descriptive_tex(
        TEX_DIR / "rq3_table_08_edge_detectioncost.tex",
        matrix_dc,
        TABLE8_BOLD,
        TABLE8_BOLD_ITALIC,
        "tab:rq3-dc-edge",
        caption,
    )


# =============================================================================
# Table 9 — Paired Edge vs Event win counts
# =============================================================================
def make_table_09() -> None:
    src = RESULT_DIR / "rq3_paired_edge_vs_event.xlsx"
    if not src.exists():
        print(f"  [!] missing input: {src.name}; skipping Table 9")
        return

    def counts(sheet: str) -> pd.DataFrame:
        df = pd.read_excel(src, sheet_name=sheet)
        # Article scope: exclude the L=1 case for the Model Once, Generate Any
        # approach. The full counts (including L=1) are in the supplementary
        # copy of this file. With L=1 dropped each row sums to 7 approaches:
        # Model Once, Generate Any at L in {2,3,4}, structural baseline at
        # L in {2,3,4}, stochastic baseline.
        df = df[df["TestingApproach"] != "ESG-Fx_L1"]
        pivot = df.groupby(["SPL", "winner"]).size().unstack(fill_value=0)
        for col in ("Event", "Edge", "tie"):
            if col not in pivot.columns:
                pivot[col] = 0
        return pivot.reindex(COL_ORDER)[["Event", "Edge", "tie"]].fillna(0).astype(int)

    sub = counts("paired_score")
    earl = counts("paired_cost_penalized")

    sub_total = sub.sum().to_dict()
    earl_total = earl.sum().to_dict()

    caption = (
        r"RQ3: Per-SPL outcome counts for the paired Edge--Event Wilcoxon "
        r"tests (with Benjamini--Hochberg correction at $\alpha = 0.05$). "
        r"\emph{Subsumption} compares the mutation score (\%): "
        r"\textit{Event} wins means the same suite kills at least as many "
        r"event omission mutants as edge omission mutants on that "
        r"SPL$\times$approach. \emph{Earliness} compares the penalized "
        r"percent of suite to detect (\%): \textit{Event} wins means event "
        r"mutants are detected after running less of the suite than edge "
        r"mutants. Each row sums to the 7 approaches reported in this "
        r"article (\textit{Model Once, Generate Any} at $L \in \{2, 3, 4\}$, "
        r"structural baseline at $L \in \{2, 3, 4\}$, stochastic baseline); "
        r"the supplementary material includes the additional $L = 1$ row. "
        r"\emph{Tie} cells are tests with $A_{12} = 0.5$ or every paired "
        r"difference equal to zero (typically saturation at $100\%$ mutation "
        r"score or near-zero detection cost)."
    )

    lines = []
    lines.append(r"\begin{table}[pos=htbp]")
    lines.append(rf"\caption{{{caption}}}")
    lines.append(r"\label{tab:rq3-paired}")
    lines.append(r"\setlength{\tabcolsep}{6pt}")
    lines.append(r"\begin{tabular*}{\columnwidth}{@{\extracolsep{\fill}} l ccc c ccc @{}}")
    lines.append(r"\toprule")
    lines.append(
        r"& \multicolumn{3}{c}{\textbf{Subsumption (MS)}} & "
        r"& \multicolumn{3}{c}{\textbf{Earliness (DC)}} \\"
    )
    lines.append(r"\cmidrule(lr){2-4} \cmidrule(lr){6-8}")
    lines.append(
        r"\textbf{SPL} & \textbf{Event} & \textbf{Edge} & \textbf{Tie} & "
        r"& \textbf{Event} & \textbf{Edge} & \textbf{Tie} \\"
    )
    lines.append(r"\midrule")
    for spl in COL_ORDER:
        s = sub.loc[spl]
        e = earl.loc[spl]
        lines.append(
            f"{spl} & {int(s['Event'])} & {int(s['Edge'])} & {int(s['tie'])} & & "
            f"{int(e['Event'])} & {int(e['Edge'])} & {int(e['tie'])} \\\\"
        )
    lines.append(r"\midrule")
    lines.append(
        rf"\textbf{{Total}} & "
        rf"\textbf{{{int(sub_total['Event'])}}} & "
        rf"\textbf{{{int(sub_total['Edge'])}}} & "
        rf"\textbf{{{int(sub_total['tie'])}}} & & "
        rf"\textbf{{{int(earl_total['Event'])}}} & "
        rf"\textbf{{{int(earl_total['Edge'])}}} & "
        rf"\textbf{{{int(earl_total['tie'])}}} \\"
    )
    lines.append(r"\bottomrule")
    lines.append(r"\end{tabular*}")
    lines.append(r"\end{table}")

    (TEX_DIR / "rq3_table_09_edge_event_paired.tex").write_text(
        "\n".join(lines) + "\n", encoding="utf-8"
    )


# =============================================================================
# Table 10 — Damping highlights
# =============================================================================
def make_table_10() -> None:
    """
    Table 10 reports per-SPL damping medians side-by-side with Friedman
    p_BH. Important methodological detail: the Friedman test (rq3_07) uses
    a paired subset (only products with all 3 damping values), whereas the
    descriptive medians shown here use the all-available-products subset
    per damping factor. This matches the rq3_02 cross-SPL summary file.

    On every SPL except Hockerty Shirts the two subsets coincide. On
    Hockerty Shirts the d=0.8 PenalizedDC sample is restricted to 279
    products (the rest hit the walk safety limit at low damping); the
    paired subset is also 279, but at d=0.85 and d=0.9 the all-sample
    median is computed on the full 416 products and differs from the
    paired-subset median by < 1 percentage point.
    """
    src = RESULT_DIR / "rq3_damping_sensitivity.xlsx"
    if not src.exists():
        print(f"  [!] missing input: {src.name}; skipping Table 10")
        return

    fri_edge = pd.read_excel(src, sheet_name="Friedman_Sens_EdgeOmission")
    fri_tg   = pd.read_excel(src, sheet_name="Friedman_Sens_TestGen")

    # ---- All-sample medians from raw data ----
    EC_METRIC = "AchievedEdgeCoverage(%)"
    DC_METRIC = "PenalizedMedianPercentageOfSuiteToDetect(%)"

    def all_sample_medians(spl_folder: str, sheet: str, col: str,
                            operator_filter: str | None = None) -> tuple[dict, int]:
        f = _raw_file(spl_folder)
        df = pd.read_excel(f, sheet_name=sheet)
        if operator_filter:
            df = df[df["Operator"] == operator_filter]
        meds = {}
        ns = {}
        for d in (0.80, 0.85, 0.90):
            arr = df[df["DampingFactor"] == d][col].dropna().to_numpy(dtype=float)
            meds[d] = float(np.median(arr)) if len(arr) else float("nan")
            ns[d] = len(arr)
        # n_paired = #products with all three damping factors
        pv = df.pivot_table(index="ProductID", columns="DampingFactor",
                             values=col, aggfunc="median").dropna(how="any")
        return meds, ns, len(pv)

    def friedman_lookup(df: pd.DataFrame, spl: str, metric: str) -> float:
        row = df[(df["SPL"] == spl) & (df["Metric"] == metric)]
        return float(row.iloc[0]["Friedman_p_BH"]) if len(row) else float("nan")

    caption = (
        r"RQ3: Stochastic baseline damping factor sensitivity. Median "
        r"per-SPL values at three damping levels for two metrics: "
        r"AchievedEdgeCoverage (higher is better) and the penalized percent "
        r"of suite to detect on edge omission (lower is better). "
        r"Descriptive medians are computed across all products available at "
        r"each damping factor; on Hockerty Shirts $137$ products timed out "
        r"at $d = 0.8$ on the fault-detection metric and contribute only to "
        r"$d = 0.85$ and $d = 0.9$. $p_{\mathit{BH}}$ is the "
        r"Benjamini--Hochberg adjusted Friedman omnibus $p$-value, computed "
        r"on the paired subset (the same products at every damping factor; "
        r"$n$ in the table). Cells with $p_{\mathit{BH}} > 0.05$ are "
        r"reported as n.s. On the small and medium-sized SPLs no metric "
        r"reaches statistical significance. On Tesla, edge coverage is at "
        r"the $100\%$ saturation ceiling at all three damping levels but "
        r"small consistent rank shifts across the $400$ paired products "
        r"yield a significant Friedman test; the corresponding post-hoc "
        r"Vargha--Delaney $A_{12}$ effect sizes are `small' (supplementary "
        r"material)."
    )

    lines = []
    lines.append(r"\begin{table*}[width=\textwidth,pos=htbp]")
    lines.append(rf"\caption{{{caption}}}")
    lines.append(r"\label{tab:rq3-damping}")
    lines.append(r"\setlength{\tabcolsep}{4pt}")
    lines.append(
        r"\begin{tabular*}{\textwidth}"
        r"{@{\extracolsep{\fill}} l c cccc c cccc @{}}"
    )
    lines.append(r"\toprule")
    lines.append(
        r"& & \multicolumn{4}{c}{\textbf{Edge coverage (\%)}} & "
        r"& \multicolumn{4}{c}{\textbf{Penalized DC, edge omission (\%)}} \\"
    )
    lines.append(r"\cmidrule(lr){3-6} \cmidrule(lr){8-11}")
    lines.append(
        r"\textbf{SPL} & \textbf{$n$} & "
        r"\textbf{$d{=}0.8$} & \textbf{$d{=}0.85$} & \textbf{$d{=}0.9$} & "
        r"\textbf{$p_{\mathit{BH}}$} & "
        r"& \textbf{$d{=}0.8$} & \textbf{$d{=}0.85$} & \textbf{$d{=}0.9$} & "
        r"\textbf{$p_{\mathit{BH}}$} \\"
    )
    lines.append(r"\midrule")

    for spl_short, spl_folder in [("SVM","SodaVendingMachine"), ("eM","eMail"),
                                    ("El","Elevator"), ("BAv2","BankAccountv2"),
                                    ("SAS","StudentAttendanceSystem"),
                                    ("Svia","syngovia"), ("Te","Tesla"),
                                    ("HS","HockertyShirts")]:
        ec_meds, _, n_paired_ec = all_sample_medians(spl_folder, "Sens_TestGen", EC_METRIC)
        dc_meds, dc_ns, n_paired_dc = all_sample_medians(
            spl_folder, "Sens_EdgeOmission", DC_METRIC, operator_filter="EdgeOmission"
        )
        ec_p = friedman_lookup(fri_tg,   spl_short, EC_METRIC)
        dc_p = friedman_lookup(fri_edge, spl_short, DC_METRIC)
        # n shown is the paired Friedman sample for the DC test (the more
        # restrictive of the two metrics on every SPL); for HS this is 279.
        n_show = n_paired_dc

        lines.append(
            f"{spl_short} & {n_show} & "
            f"{fmt_value(ec_meds[0.80], always_decimal=True)} & "
            f"{fmt_value(ec_meds[0.85], always_decimal=True)} & "
            f"{fmt_value(ec_meds[0.90], always_decimal=True)} & "
            f"{fmt_pvalue_sci(ec_p)} & & "
            f"{fmt_value(dc_meds[0.80], always_decimal=True)} & "
            f"{fmt_value(dc_meds[0.85], always_decimal=True)} & "
            f"{fmt_value(dc_meds[0.90], always_decimal=True)} & "
            f"{fmt_pvalue_sci(dc_p)} \\\\"
        )

    lines.append(r"\bottomrule")
    lines.append(r"\end{tabular*}")
    lines.append(r"\end{table*}")

    (TEX_DIR / "rq3_table_10_damping_highlights.tex").write_text(
        "\n".join(lines) + "\n", encoding="utf-8"
    )


# =============================================================================
# Supplementary Excel files
# =============================================================================
def _autosize_columns(worksheet, df: pd.DataFrame) -> None:
    for idx, col_name in enumerate(df.columns, start=1):
        worksheet.column_dimensions[get_column_letter(idx)].width = (
            len(str(col_name)) + 2
        )


def _write_descriptive_excel(out_path: Path, ms_matrix: dict, dc_matrix: dict,
                             title_ms: str, title_dc: str) -> None:
    """Write Tables 7/8-style data into a wide layout (one sheet per metric)."""
    rows = [("Model Once, Generate Any", "MO", L) for L in ("2", "3", "4")] + \
           [("Structural baseline",      "Struct", L) for L in ("2", "3", "4")] + \
           [("Stochastic baseline",      "Stoch", "")]

    def to_df(matrix: dict) -> pd.DataFrame:
        records = []
        for approach_pretty, approach_key, L in rows:
            rec = {"Approach": approach_pretty, "L": L if L else "---"}
            for spl in COL_ORDER:
                v = matrix.get((approach_key, L, spl), float("nan"))
                # Store the high-precision value for downstream re-rounding.
                rec[spl] = round_half_up(v, 6) if not pd.isna(v) else None
            records.append(rec)
        return pd.DataFrame(records)

    with pd.ExcelWriter(out_path, engine="openpyxl") as writer:
        df_ms = to_df(ms_matrix)
        df_ms.to_excel(writer, sheet_name=title_ms, index=False)
        _autosize_columns(writer.sheets[title_ms], df_ms)
        df_dc = to_df(dc_matrix)
        df_dc.to_excel(writer, sheet_name=title_dc, index=False)
        _autosize_columns(writer.sheets[title_dc], df_dc)


def make_supplementary_event_descriptive() -> None:
    """Tables 7 & 8 equivalents but for event omission."""
    matrix_ms = _build_descriptive_matrix(
        "EventOmission", "MutationScore(%)",
        "EventOmission_MultiSeed", "MutationScore(%)",
    )
    matrix_dc = _build_descriptive_matrix(
        "EventOmission", "PenalizedPercentageOfSuiteToDetect(%)",
        "EventOmission_MultiSeed", "PenalizedMedianPercentageOfSuiteToDetect(%)",
    )
    _write_descriptive_excel(
        SUPP_DIR / "rq3_supp_event_omission_descriptive.xlsx",
        matrix_ms, matrix_dc,
        "Event_MutationScore", "Event_PenalizedDC",
    )


def make_supplementary_descriptive_edge_excel(matrix_ms: dict, matrix_dc: dict) -> None:
    """Same numbers as Tables 7 & 8 but in Excel form for the supplementary."""
    _write_descriptive_excel(
        SUPP_DIR / "rq3_supp_edge_omission_descriptive.xlsx",
        matrix_ms, matrix_dc,
        "Edge_MutationScore", "Edge_PenalizedDC",
    )


def copy_existing(src: Path, dst: Path) -> None:
    if not src.exists():
        print(f"  [!] missing source: {src.name}")
        return
    SUPP_DIR.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(src, dst)


def make_supplementary_copies() -> None:
    """Copy outputs from rq3_05/06/07/08 into the supplementary folder."""
    copy_existing(RESULT_DIR / "rq3_pairwise_wilcoxon_edge_bh.xlsx",
                  SUPP_DIR / "rq3_supp_pairwise_full_edge.xlsx")
    copy_existing(RESULT_DIR / "rq3_pairwise_wilcoxon_event_bh.xlsx",
                  SUPP_DIR / "rq3_supp_pairwise_full_event.xlsx")
    copy_existing(RESULT_DIR / "rq3_paired_edge_vs_event.xlsx",
                  SUPP_DIR / "rq3_supp_paired_edge_vs_event_full.xlsx")
    copy_existing(RESULT_DIR / "rq3_damping_sensitivity.xlsx",
                  SUPP_DIR / "rq3_supp_damping_friedman_full.xlsx")
    copy_existing(RESULT_DIR / "rq3_multiseed_stability.xlsx",
                  SUPP_DIR / "rq3_supp_multiseed_stability_full.xlsx")


# =============================================================================
# Verification print: show every table cell that goes into the manuscript.
# =============================================================================
def _print_descriptive_check(label: str, matrix: dict) -> None:
    print()
    print(f"--- {label} (printed at displayed precision) ---")
    hdr = f"{'Approach':<32s} | " + " | ".join(f"{c:>6s}" for c in COL_ORDER)
    print(hdr)
    print("-" * len(hdr))
    for L in ("2", "3", "4"):
        row = [fmt_value(matrix.get(("MO", L, s), float("nan"))) for s in COL_ORDER]
        print(f"  Model Once L={L:<25s}".ljust(32)
              + " | " + " | ".join(f"{c:>6s}" for c in row))
    for L in ("2", "3", "4"):
        row = [fmt_value(matrix.get(("Struct", L, s), float("nan"))) for s in COL_ORDER]
        print(f"  Structural L={L:<24s}".ljust(32)
              + " | " + " | ".join(f"{c:>6s}" for c in row))
    row = [fmt_value(matrix.get(("Stoch", "", s), float("nan"))) for s in COL_ORDER]
    print(f"  Stochastic baseline".ljust(32)
          + " | " + " | ".join(f"{c:>6s}" for c in row))


# =============================================================================
# Main
# =============================================================================
def main() -> None:
    print("=" * 80)
    print("rq3_09: BUILD MANUSCRIPT TABLES + SUPPLEMENTARY")
    print("=" * 80)

    project_root = find_project_root()
    if project_root is None:
        print("ERROR: project root not found (no 'files/Cases' on the path).")
        sys.exit(1)

    print(f"Script  : {SCRIPT_DIR}")
    print(f"Cases   : {DATA_DIR}")
    print(f"Result  : {RESULT_DIR}")

    TEX_DIR.mkdir(parents=True, exist_ok=True)
    SUPP_DIR.mkdir(parents=True, exist_ok=True)

    # --- Build descriptive matrices once (used by both edge tables) ---
    print("\nComputing descriptive medians for edge omission ...")
    edge_ms_matrix = _build_descriptive_matrix(
        "EdgeOmission", "MutationScore(%)",
        "EdgeOmission_MultiSeed", "MutationScore(%)",
    )
    edge_dc_matrix = _build_descriptive_matrix(
        "EdgeOmission", "PenalizedPercentageOfSuiteToDetect(%)",
        "EdgeOmission_MultiSeed", "PenalizedMedianPercentageOfSuiteToDetect(%)",
    )

    # --- Manuscript tables ---
    print("\nWriting manuscript LaTeX tables ...")
    make_table_07(edge_ms_matrix)
    print(f"  [OK] {(TEX_DIR / 'rq3_table_07_edge_mutationscore.tex').name}")
    make_table_08(edge_dc_matrix)
    print(f"  [OK] {(TEX_DIR / 'rq3_table_08_edge_detectioncost.tex').name}")
    make_table_09()
    print(f"  [OK] {(TEX_DIR / 'rq3_table_09_edge_event_paired.tex').name}")
    make_table_10()
    print(f"  [OK] {(TEX_DIR / 'rq3_table_10_damping_highlights.tex').name}")

    # --- Supplementary ---
    print("\nWriting supplementary Excel files ...")
    make_supplementary_descriptive_edge_excel(edge_ms_matrix, edge_dc_matrix)
    print(f"  [OK] rq3_supp_edge_omission_descriptive.xlsx")
    make_supplementary_event_descriptive()
    print(f"  [OK] rq3_supp_event_omission_descriptive.xlsx")
    make_supplementary_copies()
    print(f"  [OK] supplementary copies")

    # --- Verification print ---
    print("\n" + "=" * 80)
    print("VERIFICATION  (compare to manuscript)")
    print("=" * 80)
    _print_descriptive_check("Table 7: Edge MutationScore (median %)", edge_ms_matrix)
    _print_descriptive_check("Table 8: Edge Penalized DC (median %)",  edge_dc_matrix)

    print("\n" + "=" * 80)
    print("rq3_09 DONE.")
    print("=" * 80)


if __name__ == "__main__":
    main()