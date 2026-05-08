#!/usr/bin/env python3
"""
rq1_08_make_supplementary.py

Step 8 of the RQ1 analysis pipeline.

Generates the complete set of LaTeX supplementary tables for the RQ1
generation- and execution-efficiency results. Every value is computed
directly from the per-product / per-run Excel files produced by rq1_01
and from the analysis outputs produced by rq1_03/05/06/07; nothing is
hard-coded.

Output (single self-contained file):
    rq1_result/supplementary/rq1_supplementary.tex

The file produces 9 tables, S1--S9, each cross-referenced from the
main manuscript text:

    S1  Pooled Spearman rho between edges and each T_gen component
        for Model Once, Generate Any (mechanism of the pooled trend
        cited in Section 7.1.2).
    S2  Median share of the L-sequence transformation in T_gen.
    S3  Per-SPL Spearman rho between edges and median T_gen.
    S4  Per-SPL median peak heap memory during test generation.
    S5  Per-SPL median test execution time.
    S6  Run-to-run stability statistics for T_gen across the 11
        repetitions of each cell.
    S7  JVM warm-up evidence: per-cell mean/median and max/median
        ratios, with the high-CV-low-IQR fingerprint isolated.
    S8  Pairwise Wilcoxon results, Vargha-Delaney A12, and BH-adjusted
        p-values for every metric and SPL x level.
    S9  Per-SPL edge-count summary (min, median, max, range ratio).

Numerical precision
-------------------
Spearman rho values are reported to three decimal places. Percentage
values use ROUND_HALF_UP to one decimal place after a six-decimal
float-noise scrub. Integer-valued cells are written without trailing
".0". Memory is reported in MB to one decimal place.

Paths (relative to this script's location):
    Script  : files/scripts/statistical_test_scripts/rq1_08_make_supplementary.py
    Inputs  :
        files/Cases/<SPL>/RQ1_<SPL>_perProduct.xlsx                 (rq1_01)
        files/Cases/<SPL>/RQ1_<SPL>_perRun.xlsx                     (rq1_01)
        files/Cases/<SPL>/RQ1_<SPL>_SPLSummary_medians.xlsx          (rq1_01)
        rq1_result/rq1_per_product_summary.xlsx                      (rq1_03)
        rq1_result/rq1_pairwise_wilcoxon.xlsx                        (rq1_05)
        rq1_result/rq1_run_stability.xlsx                            (rq1_06)
        rq1_result/rq1_mechanism_and_warmup.xlsx                     (rq1_07)
    Output  :
        rq1_result/supplementary/rq1_supplementary.tex

Usage:
    python rq1_08_make_supplementary.py
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
RESULT_DIR  = SCRIPT_DIR / "rq1_result"
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
SPL_SHORT_TO_FOLDER = {v: k for k, v in SPL_FOLDER_TO_SHORT.items()}
COL_ORDER = ["SVM", "eM", "El", "BAv2", "SAS", "Svia", "Te", "HS"]

ESGFX_LEVELS = ["L2", "L3", "L4"]
EFG_LEVELS   = ["L2", "L3", "L4"]

APPROACH_PRETTY = {
    "ESG-Fx":     r"\textit{Model Once, Generate Any}",
    "EFG":        "Structural baseline",
    "RandomWalk": "Stochastic baseline",
}

APPROACH_SHORT = {
    "ESG-Fx":     "MO",
    "EFG":        "Struct",
    "RandomWalk": "Stoch",
}


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


def fmt_rho(v: float) -> str:
    """Spearman rho to three decimal places, preserving sign."""
    if pd.isna(v):
        return "---"
    r = round_half_up(float(v), 3)
    return f"{r:.3f}"


def fmt_int(v: float) -> str:
    if pd.isna(v):
        return "---"
    return f"{int(round(float(v)))}"


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
    """Format a p-value plainly for tabular cells."""
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


# =============================================================================
# Data access helpers
# =============================================================================
def find_project_root() -> Path | None:
    """Locate a directory that looks like a usable project root.

    The script no longer requires the legacy `files/Cases` layout. It is
    enough that the rq1_result directory (with the consolidated summary
    Excel and the analysis outputs) is reachable from the current working
    directory or from the script's parent directories.
    """
    candidates = [Path.cwd(), *Path.cwd().parents]
    try:
        here = Path(__file__).resolve()
        candidates.extend([here.parent, *here.parents])
    except NameError:
        pass
    seen = set()
    # First pass: prefer the legacy layout when it actually exists.
    for probe in candidates:
        if probe in seen:
            continue
        seen.add(probe)
        if (probe / "files" / "Cases").exists():
            return probe
    # Second pass: accept any ancestor that contains a usable rq1_result
    # directory. This covers the new repo layout where the script lives in
    # `statistical_test_scripts/` and the inputs live in
    # `statistical_test_scripts/rq1_result/`.
    seen.clear()
    for probe in candidates:
        if probe in seen:
            continue
        seen.add(probe)
        if (probe / "rq1_result").exists():
            return probe
    return None


def _safe_read(path: Path, sheet_name=None):
    if not path.exists():
        print(f"  [WARN] file missing: {path}")
        return None
    try:
        if sheet_name is None:
            return pd.ExcelFile(path)
        return pd.read_excel(path, sheet_name=sheet_name)
    except Exception as e:
        print(f"  [WARN] could not read {path.name}::{sheet_name}: {e}")
        return None


# =============================================================================
# Block builders
# =============================================================================
def _table_s1(label: str, caption: str) -> str:
    """S1: Per-component pooled Spearman rho for Model Once, Generate Any."""
    src = RESULT_DIR / "rq1_mechanism_and_warmup.xlsx"
    df = _safe_read(src, "mechanism_rho_pivot")
    if df is None or df.empty:
        return f"% S1 source missing: {src}\n"

    # The pivot index is the component name; rename for prose readability
    component_label = {
        "ESGFxModelLoadTime":    "Model loading",
        "TransformationTime":    "$L$-sequence transformation",
        "AlgTestGenTime":        "Euler traversal (algorithm)",
        "TestCaseRecordingTime": "Test recording",
        "TotalTestGenTime":      "Total $T_{\\mathit{gen}}$",
    }
    order = ["Model loading", "$L$-sequence transformation",
             "Euler traversal (algorithm)", "Test recording",
             "Total $T_{\\mathit{gen}}$"]

    # Re-shape for predictable rendering
    if df.index.name == "Component":
        df_idx = df
    else:
        df_idx = df.set_index(df.columns[0])
    df_idx = df_idx.rename(index=component_label)

    out = []
    out.append(r"\begin{table}[pos=htbp]")
    out.append(rf"\caption{{{caption}}}")
    out.append(rf"\label{{{label}}}")
    out.append(r"\setlength{\tabcolsep}{6pt}")
    out.append(r"\begin{tabular*}{\columnwidth}{@{\extracolsep{\fill}} l c c c @{}}")
    out.append(r"\toprule")
    out.append(r"\textbf{Component} & \textbf{$L = 2$} & "
               r"\textbf{$L = 3$} & \textbf{$L = 4$} \\")
    out.append(r"\midrule")
    for comp in order:
        if comp not in df_idx.index:
            continue
        row = df_idx.loc[comp]
        out.append(rf"{comp} & {fmt_rho(row.get('L2'))} & "
                   rf"{fmt_rho(row.get('L3'))} & {fmt_rho(row.get('L4'))} \\")
    out.append(r"\bottomrule")
    out.append(r"\end{tabular*}")
    out.append(r"\end{table}")
    return "\n".join(out)


def _table_s2(label: str, caption: str) -> str:
    """S2: Median share of transformation time in T_gen."""
    src = RESULT_DIR / "rq1_mechanism_and_warmup.xlsx"
    df = _safe_read(src, "transformation_share")
    if df is None or df.empty:
        return f"% S2 source missing: {src}\n"

    out = []
    out.append(r"\begin{table}[pos=htbp]")
    out.append(rf"\caption{{{caption}}}")
    out.append(rf"\label{{{label}}}")
    out.append(r"\setlength{\tabcolsep}{6pt}")
    out.append(r"\begin{tabular*}{\columnwidth}{@{\extracolsep{\fill}} c c c @{}}")
    out.append(r"\toprule")
    out.append(r"\textbf{Coverage level} & \textbf{Median share (\%)} & "
               r"\textbf{IQR width (\%)} \\")
    out.append(r"\midrule")
    for _, r in df.iterrows():
        L = str(r["Level"]).replace("L", "$L = ") + "$"
        out.append(rf"{L} & "
                   rf"{fmt_value(float(r['median_share_pct']), always_decimal=True)} & "
                   rf"{fmt_value(float(r['iqr_share_pct']), always_decimal=True)} \\")
    out.append(r"\bottomrule")
    out.append(r"\end{tabular*}")
    out.append(r"\end{table}")
    return "\n".join(out)


def _table_s3(label: str, caption: str) -> str:
    """S3: Per-SPL Spearman rho between edge count and median T_gen."""
    src = RESULT_DIR / "rq1_per_product_summary.xlsx"
    df = _safe_read(src, "spearman_article")
    if df is None or df.empty:
        return f"% S3 source missing: {src}\n"

    df = df[(df["SPL"] != "ALL") & (df["Metric"] == "Edges")].copy()

    # Build column header tuples (Approach, L)
    col_specs = []
    for approach_short in ["ESG-Fx", "EFG", "RandomWalk"]:
        for L in ["L2", "L3", "L4", "L0"]:
            sub = df[(df["Approach"] == approach_short) & (df["Level"] == L)]
            if not sub.empty:
                col_specs.append((approach_short, L))

    out = []
    out.append(r"\begin{table*}[width=\textwidth,pos=htbp]")
    out.append(rf"\caption{{{caption}}}")
    out.append(rf"\label{{{label}}}")
    out.append(r"\setlength{\tabcolsep}{4pt}")
    out.append(r"\footnotesize")

    # Layout: SPL | MO L=2 | MO L=3 | MO L=4 | Struct L=2 | Struct L=3 | Struct L=4 | Stoch
    n_cols = len(col_specs)
    col_spec_str = "@{} l " + " ".join(["c"] * n_cols) + " @{}"
    out.append(rf"\begin{{tabular*}}{{\textwidth}}{{@{{\extracolsep{{\fill}}}} l "
               + " ".join(["c"] * n_cols) + r" @{}}")
    out.append(r"\toprule")

    # Two-row header: approach group, then L
    header_groups = []
    cur = None
    span = 0
    for app, L in col_specs:
        if app != cur:
            if cur is not None:
                header_groups.append((cur, span))
            cur = app
            span = 1
        else:
            span += 1
    header_groups.append((cur, span))

    h1 = ["\\textbf{SPL}"]
    cur_idx = 1
    for app, span in header_groups:
        # Center each approach group
        h1.append(rf"\multicolumn{{{span}}}{{c}}{{\textbf{{{APPROACH_PRETTY[app]}}}}}")
    out.append(" & ".join(h1) + r" \\")

    # Cmidrule
    cmidrules = []
    cur_col = 2
    for app, span in header_groups:
        cmidrules.append(rf"\cmidrule(lr){{{cur_col}-{cur_col + span - 1}}}")
        cur_col += span
    out.append(" ".join(cmidrules))

    # Row 2: L values
    h2 = [""]
    for app, L in col_specs:
        if app == "RandomWalk":
            h2.append(r"\textbf{---}")
        else:
            L_num = L.replace("L", "")
            h2.append(rf"\textbf{{$L = {L_num}$}}")
    out.append(" & ".join(h2) + r" \\")
    out.append(r"\midrule")

    # Body rows
    for spl in COL_ORDER:
        cells = [spl]
        for app, L in col_specs:
            sub = df[(df["SPL"] == spl) & (df["Approach"] == app)
                     & (df["Level"] == L)]
            if sub.empty:
                cells.append("---")
            else:
                cells.append(fmt_rho(float(sub.iloc[0]["rho"])))
        out.append(" & ".join(cells) + r" \\")
    out.append(r"\bottomrule")
    out.append(r"\end{tabular*}")
    out.append(r"\end{table*}")
    return "\n".join(out)


# Lookup of SPL display names used inside the consolidated summary file.
SPL_SHORT_TO_FULL = {
    "SVM":  "Soda Vending Machine",
    "eM":   "eMail",
    "El":   "Elevator",
    "BAv2": "Bank Account",
    "SAS":  "Student Attendance System",
    "Te":   "Tesla Web Configurator",
    "Svia": "syngo.via",
    "HS":   "Hockerty Shirts Web Configurator",
}


def _resolve_spl_summary_path() -> Path | None:
    """Locate the consolidated RQ1 SPL summary workbook. The expected
    location is rq1_result/RQ1_SPLSummary_medians.xlsx, but legacy paths
    are checked too."""
    candidates = [
        RESULT_DIR / "RQ1_SPLSummary_medians.xlsx",
        SCRIPT_DIR / "RQ1_SPLSummary_medians.xlsx",
        DATA_DIR / "RQ1_SPLSummary_medians.xlsx",
    ]
    # Per-SPL fallback (older layout): files/Cases/<SPL>/RQ1_<SPL>_SPLSummary_medians.xlsx
    for path in candidates:
        if path.exists():
            return path
    return None


def _find_spl_column(df: pd.DataFrame) -> str | None:
    """Pick the column that names the SPL within an SPL-summary sheet."""
    for cand in ["SPL", "SPL Name", "SPLName", "SPL_Name", "Name"]:
        if cand in df.columns:
            return cand
    return None


def _read_spl_summary_metric(metric_col: str, sheet_to_label: dict) -> pd.DataFrame:
    """Build a per-SPL x per-(approach,L) table from the consolidated
    RQ1 SPL summary workbook. Tolerates either short codes (SVM, eM, ...)
    or full SPL names (Soda Vending Machine, eMail, ...) as the SPL column,
    and tolerates the column being labelled SPL, SPL Name, or similar."""
    path = _resolve_spl_summary_path()
    if path is None:
        # Fall back to per-SPL files (legacy layout) before giving up.
        rows = []
        for spl_short in COL_ORDER:
            folder = SPL_SHORT_TO_FOLDER[spl_short]
            sub_path = DATA_DIR / folder / f"RQ1_{folder}_SPLSummary_medians.xlsx"
            if not sub_path.exists():
                continue
            try:
                xls = pd.ExcelFile(sub_path)
            except Exception:
                continue
            record = {"SPL": spl_short}
            for sheet, label in sheet_to_label.items():
                if sheet not in xls.sheet_names:
                    record[label] = float("nan")
                    continue
                df = pd.read_excel(xls, sheet_name=sheet)
                if metric_col not in df.columns or df.empty:
                    record[label] = float("nan")
                    continue
                try:
                    record[label] = float(df.iloc[0][metric_col])
                except (TypeError, ValueError):
                    record[label] = float("nan")
            rows.append(record)
        if not rows:
            print(f"  [WARN] no RQ1_SPLSummary_medians.xlsx found")
        return pd.DataFrame(rows)

    # Consolidated workbook path
    try:
        xls = pd.ExcelFile(path)
    except Exception as e:
        print(f"  [WARN] could not open {path}: {e}")
        return pd.DataFrame()

    rows = []
    for spl_short in COL_ORDER:
        record = {"SPL": spl_short}
        for sheet, label in sheet_to_label.items():
            if sheet not in xls.sheet_names:
                record[label] = float("nan")
                continue
            df = pd.read_excel(xls, sheet_name=sheet)
            if metric_col not in df.columns:
                record[label] = float("nan")
                continue

            spl_col = _find_spl_column(df)
            if spl_col is None:
                # No identifying column; cannot match this SPL safely.
                record[label] = float("nan")
                continue

            # Try short code first, then full display name.
            row = df[df[spl_col].astype(str) == spl_short]
            if row.empty:
                full = SPL_SHORT_TO_FULL.get(spl_short, spl_short)
                row = df[df[spl_col].astype(str) == full]
            if row.empty:
                # Last resort: case-insensitive substring on the full name.
                full = SPL_SHORT_TO_FULL.get(spl_short, spl_short)
                mask = df[spl_col].astype(str).str.lower() == full.lower()
                row = df[mask]
            if row.empty:
                record[label] = float("nan")
                continue
            try:
                record[label] = float(row.iloc[0][metric_col])
            except (TypeError, ValueError):
                record[label] = float("nan")
        rows.append(record)
    return pd.DataFrame(rows)


SHEET_LABEL_MAP = {
    "ESG-Fx_L2":     ("ESG-Fx", "L2"),
    "ESG-Fx_L3":     ("ESG-Fx", "L3"),
    "ESG-Fx_L4":     ("ESG-Fx", "L4"),
    "EFG_L2":        ("EFG",    "L2"),
    "EFG_L3":        ("EFG",    "L3"),
    "EFG_L4":        ("EFG",    "L4"),
    "RandomWalk_L0": ("RandomWalk", "---"),
}


def _per_spl_metric_table(label: str, caption: str,
                           metric_col: str, ndigits: int) -> str:
    """Helper for S4 (memory) and S5 (test exec time)."""
    sheet_label_simple = {sh: f"{APPROACH_PRETTY[a]}|{L}"
                           for sh, (a, L) in SHEET_LABEL_MAP.items()}
    df = _read_spl_summary_metric(metric_col, sheet_label_simple)
    if df.empty:
        msg = (f"could not read RQ1_SPLSummary_medians.xlsx for "
               f"metric_col='{metric_col}' (file or column not found)")
        print(f"  [WARN] {label}: {msg}")
        return f"% {label}: {msg}\n"

    # Drop SPLs that have no data at all for this metric
    data_cols = [c for c in df.columns if c != "SPL"]
    df_has_data = df.dropna(subset=data_cols, how="all")
    if df_has_data.empty:
        msg = (f"all values NaN — column '{metric_col}' likely missing "
               f"from the SPL summary sheets")
        print(f"  [WARN] {label}: {msg}")
        return f"% {label}: {msg}\n"

    # Determine column ordering
    col_specs = []
    for sheet, (approach, L) in SHEET_LABEL_MAP.items():
        col_label = f"{APPROACH_PRETTY[approach]}|{L}"
        if col_label in df.columns:
            col_specs.append((approach, L, col_label))

    out = []
    out.append(r"\begin{table*}[width=\textwidth,pos=htbp]")
    out.append(rf"\caption{{{caption}}}")
    out.append(rf"\label{{{label}}}")
    out.append(r"\setlength{\tabcolsep}{4pt}")
    out.append(r"\footnotesize")

    n_cols = len(col_specs)
    out.append(rf"\begin{{tabular*}}{{\textwidth}}{{@{{\extracolsep{{\fill}}}} l "
               + " ".join(["c"] * n_cols) + r" @{}}")
    out.append(r"\toprule")

    # Two-row header
    header_groups = []
    cur = None
    span = 0
    for app, L, _ in col_specs:
        if app != cur:
            if cur is not None:
                header_groups.append((cur, span))
            cur = app
            span = 1
        else:
            span += 1
    header_groups.append((cur, span))

    h1 = ["\\textbf{SPL}"]
    for app, span in header_groups:
        h1.append(rf"\multicolumn{{{span}}}{{c}}{{\textbf{{{APPROACH_PRETTY[app]}}}}}")
    out.append(" & ".join(h1) + r" \\")

    cmidrules = []
    cur_col = 2
    for app, span in header_groups:
        cmidrules.append(rf"\cmidrule(lr){{{cur_col}-{cur_col + span - 1}}}")
        cur_col += span
    out.append(" ".join(cmidrules))

    h2 = [""]
    for app, L, _ in col_specs:
        if app == "RandomWalk":
            h2.append(r"\textbf{---}")
        else:
            L_num = L.replace("L", "")
            h2.append(rf"\textbf{{$L = {L_num}$}}")
    out.append(" & ".join(h2) + r" \\")
    out.append(r"\midrule")

    # Body
    for _, r in df.iterrows():
        cells = [r["SPL"]]
        for app, L, col_label in col_specs:
            v = r.get(col_label, float("nan"))
            cells.append(fmt_value(v, ndigits=ndigits, always_decimal=True))
        out.append(" & ".join(cells) + r" \\")

    out.append(r"\bottomrule")
    out.append(r"\end{tabular*}")
    out.append(r"\end{table*}")
    return "\n".join(out)


def _table_s6(label: str, caption: str) -> str:
    """S6: Run-to-run stability per cell (T_gen across 11 repetitions)."""
    src = RESULT_DIR / "rq1_run_stability.xlsx"
    df = _safe_read(src, "TestGenTime_ms")
    if df is None or df.empty:
        return f"% S6 source missing: {src}\n"

    # Article scope
    df = df[df["Level"] != "L1"].copy()

    out = []
    out.append(r"\begin{table*}[width=\textwidth,pos=htbp]")
    out.append(rf"\caption{{{caption}}}")
    out.append(rf"\label{{{label}}}")
    out.append(r"\setlength{\tabcolsep}{4pt}")
    out.append(r"\footnotesize")
    out.append(r"\begin{tabular*}{\textwidth}{@{\extracolsep{\fill}} l l c r r r r r r @{}}")
    out.append(r"\toprule")
    out.append(r"\textbf{Approach} & \textbf{SPL} & \textbf{$L$} & "
               r"\textbf{$N$} & \textbf{Median (ms)} & \textbf{Min (ms)} & "
               r"\textbf{Max (ms)} & \textbf{CV (\%)} & "
               r"\textbf{IQR \% of median} \\")
    out.append(r"\midrule")

    # Sort by Approach, L, SPL_ORDER
    spl_rank = {SPL_FOLDER_TO_SHORT[SPL_SHORT_TO_FOLDER[s]]: i
                for i, s in enumerate(COL_ORDER)}
    df["_spl_rank"] = df["SPL"].map(spl_rank)
    df["_approach_rank"] = df["Approach"].map(
        {"ESG-Fx": 0, "EFG": 1, "RandomWalk": 2}
    )
    df = df.sort_values(["_approach_rank", "Level", "_spl_rank"])

    prev_app = None
    prev_L = None
    for _, r in df.iterrows():
        app = APPROACH_PRETTY[r["Approach"]]
        L_str = "---" if r["Level"] == "L0" else r["Level"].replace("L", "")
        app_cell = app if (r["Approach"] != prev_app) else ""
        prev_app = r["Approach"]
        out.append(
            rf"{app_cell} & {r['SPL']} & {L_str} & "
            rf"{int(r['N_runs'])} & "
            rf"{fmt_value(float(r['median']), always_decimal=True)} & "
            rf"{fmt_value(float(r['min']), always_decimal=True)} & "
            rf"{fmt_value(float(r['max']), always_decimal=True)} & "
            rf"{fmt_value(float(r['CV_pct']), always_decimal=True)} & "
            rf"{fmt_value(float(r['IQR_pct_of_median']), always_decimal=True)} \\"
        )
    out.append(r"\bottomrule")
    out.append(r"\end{tabular*}")
    out.append(r"\end{table*}")
    return "\n".join(out)


def _table_s7(label: str, caption: str) -> str:
    """S7: JVM warm-up evidence (per-cell mean/median, max/median)."""
    src = RESULT_DIR / "rq1_mechanism_and_warmup.xlsx"
    df = _safe_read(src, "warmup_per_cell")
    if df is None or df.empty:
        return f"% S7 source missing: {src}\n"

    df = df[df["Level"] != "L1"].copy()

    out = []
    out.append(r"\begin{table*}[width=\textwidth,pos=htbp]")
    out.append(rf"\caption{{{caption}}}")
    out.append(rf"\label{{{label}}}")
    out.append(r"\setlength{\tabcolsep}{4pt}")
    out.append(r"\footnotesize")
    out.append(r"\begin{tabular*}{\textwidth}{@{\extracolsep{\fill}} l l c r r r r r r @{}}")
    out.append(r"\toprule")
    out.append(r"\textbf{Approach} & \textbf{SPL} & \textbf{$L$} & "
               r"\textbf{Median (ms)} & \textbf{Max (ms)} & "
               r"\textbf{CV (\%)} & \textbf{IQR \%} & "
               r"\textbf{Mean / Median} & \textbf{Max / Median} \\")
    out.append(r"\midrule")

    spl_rank = {s: i for i, s in enumerate(COL_ORDER)}
    df["_spl_rank"] = df["SPL"].map(spl_rank)
    df["_approach_rank"] = df["Approach"].map(
        {"ESG-Fx": 0, "EFG": 1, "RandomWalk": 2}
    )
    df = df.sort_values(["_approach_rank", "Level", "_spl_rank"])

    prev_app = None
    for _, r in df.iterrows():
        app = APPROACH_PRETTY[r["Approach"]]
        L_str = "---" if r["Level"] == "L0" else r["Level"].replace("L", "")
        app_cell = app if (r["Approach"] != prev_app) else ""
        prev_app = r["Approach"]
        out.append(
            rf"{app_cell} & {r['SPL']} & {L_str} & "
            rf"{fmt_value(float(r['median_ms']), always_decimal=True)} & "
            rf"{fmt_value(float(r['max_ms']), always_decimal=True)} & "
            rf"{fmt_value(float(r['CV_pct']), always_decimal=True)} & "
            rf"{fmt_value(float(r['IQR_pct']), always_decimal=True)} & "
            rf"{fmt_value(float(r['mean_over_median']), ndigits=3, always_decimal=True)} & "
            rf"{fmt_value(float(r['max_over_median']), ndigits=3, always_decimal=True)} \\"
        )
    out.append(r"\bottomrule")
    out.append(r"\end{tabular*}")
    out.append(r"\end{table*}")
    return "\n".join(out)


def _table_s8(label: str, caption: str) -> str:
    """S8: Pairwise Wilcoxon results (full)."""
    src = RESULT_DIR / "rq1_pairwise_wilcoxon.xlsx"
    df = _safe_read(src, "all_comparisons")
    if df is None or df.empty:
        return f"% S8 source missing: {src}\n"

    # Pretty-print maps. The Metric column uses underscored snake_case names
    # ("TestGenTime_ms", "EdgeCoverage_pct", ...) which are not valid in LaTeX
    # text mode. Convert them to human-readable labels here.
    metric_pretty = {
        "TestGenTime_ms":        r"$T_{\mathit{gen}}$ (ms)",
        "TestGenPeakMemory_MB":  "Peak heap memory (MB)",
        "TestExecTime_ms":       r"$T_{\mathit{exec}}$ (ms)",
        "EdgeCoverage_pct":      r"Edge coverage (\%)",
    }
    # Level values may be underscored (e.g. "L2_vs_L0", "L2_vs_RW"); split into
    # textual labels.
    def _pretty_level(L: str) -> str:
        if pd.isna(L) or L in ("", "nan", "None"):
            return "---"
        s = str(L).strip()
        if s in ("L0", "---"):
            return "---"
        if "_vs_" in s:
            left, right = s.split("_vs_", 1)
            left_disp  = "---" if left  in ("L0", "RW") else left.replace("L", "$L = ") + "$"
            right_disp = "---" if right in ("L0", "RW") else right.replace("L", "$L = ") + "$"
            return rf"{left_disp} vs {right_disp}"
        # Plain "L2" / "L3" / "L4"
        if s.startswith("L"):
            return s.replace("L", "$L = ") + "$"
        return s

    out = []
    out.append(r"\begin{table*}[width=\textwidth,pos=htbp]")
    out.append(rf"\caption{{{caption}}}")
    out.append(rf"\label{{{label}}}")
    out.append(r"\setlength{\tabcolsep}{3pt}")
    out.append(r"\footnotesize")
    out.append(r"\begin{tabular*}{\textwidth}{@{\extracolsep{\fill}} l l l l c c r r r l @{}}")
    out.append(r"\toprule")
    out.append(r"\textbf{Metric} & \textbf{SPL} & \textbf{Approach A} & "
               r"\textbf{Approach B} & \textbf{$L$} & \textbf{$N$} & "
               r"\textbf{$W$} & \textbf{$p_{\mathit{BH}}$} & "
               r"\textbf{$A_{12}$} & \textbf{Winner} \\")
    out.append(r"\midrule")

    # Sort by metric, SPL, level, approach pair
    spl_rank = {s: i for i, s in enumerate(COL_ORDER)}
    if "SPL" in df.columns:
        df["_spl_rank"] = df["SPL"].map(spl_rank).fillna(99)
    if "Level" in df.columns:
        df = df.sort_values(["Metric", "_spl_rank", "Level"])
    else:
        df = df.sort_values(["Metric", "_spl_rank"])

    prev_metric = None
    for _, r in df.iterrows():
        metric_raw = str(r.get("Metric", ""))
        metric = metric_pretty.get(metric_raw, metric_raw.replace("_", r"\_"))
        metric_cell = metric if metric_raw != prev_metric else ""
        prev_metric = metric_raw

        spl = str(r.get("SPL", ""))
        a = APPROACH_SHORT.get(str(r.get("ApproachA", "")),
                                str(r.get("ApproachA", "")))
        b = APPROACH_SHORT.get(str(r.get("ApproachB", "")),
                                str(r.get("ApproachB", "")))
        L_disp = _pretty_level(r.get("Level", ""))

        # n_pairs / N_pairs (handle either capitalisation)
        n_val = r.get("n_pairs", r.get("N_pairs", float("nan")))
        n = int(n_val) if not pd.isna(n_val) else "---"

        W = r.get("W", float("nan"))
        W_str = (f"{int(W):,}".replace(",", r"{,}")
                 if not pd.isna(W) else "---")
        p_bh = float(r.get("p_BH", float("nan")))
        # A12 column may be A12 or A12_A_vs_B
        a12 = r.get("A12", r.get("A12_A_vs_B", float("nan")))
        a12 = float(a12) if not pd.isna(a12) else float("nan")
        # Winner column may be winner or winner_lower; map approach codes to
        # short labels and escape any underscores defensively.
        winner_raw = str(r.get("winner_lower", r.get("winner", "")))
        # 'winner_lower' is computed under the lower-is-better convention. For
        # metrics where higher is better (edge coverage, mutation score), we
        # must invert the winner. We do this on the basis of the A12 effect
        # size and the sign of delta_A_minus_B.
        higher_is_better_metrics = {"EdgeCoverage_pct", "MutationScore_pct"}
        if metric_raw in higher_is_better_metrics and not pd.isna(a12):
            if a12 == 0.5:
                winner_raw = "tie"
            elif a12 > 0.5:
                # A consistently higher than B -> A wins for higher-is-better
                winner_raw = str(r.get("ApproachA", ""))
            else:
                winner_raw = str(r.get("ApproachB", ""))
        winner = APPROACH_SHORT.get(winner_raw, winner_raw)
        winner = winner.replace("_", r"\_")

        out.append(rf"{metric_cell} & {spl} & {a} & {b} & {L_disp} & {n} & "
                   rf"{W_str} & {fmt_pvalue_inline(p_bh)} & "
                   rf"{fmt_a12(a12)} & {winner} \\")

    out.append(r"\bottomrule")
    out.append(r"\end{tabular*}")
    out.append(r"\end{table*}")
    return "\n".join(out)


def _table_s9(label: str, caption: str) -> str:
    """S9: Per-SPL edge-count summary."""
    src = RESULT_DIR / "rq1_per_product_summary.xlsx"
    df = _safe_read(src, "all_medians")
    if df is None or df.empty:
        return f"% S9 source missing: {src}\n"

    df = df[df["Level"] != "L1"].copy()

    out = []
    out.append(r"\begin{table}[pos=htbp]")
    out.append(rf"\caption{{{caption}}}")
    out.append(rf"\label{{{label}}}")
    out.append(r"\setlength{\tabcolsep}{5pt}")
    out.append(r"\begin{tabular*}{\columnwidth}{@{\extracolsep{\fill}} l l r r r r r @{}}")
    out.append(r"\toprule")
    out.append(r"\textbf{SPL} & \textbf{Approach} & \textbf{$N$} & "
               r"\textbf{Min} & \textbf{Median} & \textbf{Max} & "
               r"\textbf{Range ratio} \\")
    out.append(r"\midrule")

    for spl in COL_ORDER:
        for approach in ["ESG-Fx", "EFG", "RandomWalk"]:
            level = "L0" if approach == "RandomWalk" else "L2"
            sub = df[(df["SPL"] == spl) & (df["Approach"] == approach)
                     & (df["Level"] == level) & (df["Edges"].notna())]
            if sub.empty:
                continue
            edges = sub["Edges"].values
            mn, mx = int(edges.min()), int(edges.max())
            md = float(np.median(edges))
            ratio = mx / mn if mn > 0 else float("nan")
            out.append(
                rf"{spl} & {APPROACH_SHORT[approach]} & {len(edges)} & "
                rf"{mn} & {fmt_value(md, always_decimal=False)} & {mx} & "
                rf"{fmt_value(ratio, ndigits=2, always_decimal=True)} \\"
            )
        out.append(r"\addlinespace[2pt]")

    out.append(r"\bottomrule")
    out.append(r"\end{tabular*}")
    out.append(r"\end{table}")
    return "\n".join(out)


# =============================================================================
# Main: assemble a single LaTeX document
# =============================================================================
PREAMBLE = r"""% =========================================================================
% RQ1 Supplementary Material
% Auto-generated by rq1_08_make_supplementary.py
% Every value computed from the raw data; do not edit by hand.
% =========================================================================
%
% This file can be either compiled standalone (uncomment the documentclass
% block below) or \input into the main supplementary tex file. Tables are
% labelled tab:rq1-supp-S1 ... tab:rq1-supp-S9 and counted independently
% of the manuscript table counter (use \renewcommand{\thetable}{S\arabic{table}}
% in the parent document).
%
% \documentclass[a4paper,fleqn]{cas-sc}
% \usepackage{booktabs,multirow,tabularx}
% \newcolumntype{Y}{>{\centering\arraybackslash}X}
% \newcolumntype{L}{>{\raggedright\arraybackslash}X}
% \begin{document}

\section*{S-RQ1. Supplementary Material for RQ1 (Generation and Execution Efficiency)}
\label{sec:supp_rq1}

This appendix collects the supplementary tables referenced in the
RQ1 results section (Section~\ref{sec:results_rq1}) of the main
manuscript. Tables S1 and S2 expand on the mechanism paragraph in
Section~\ref{sec:rq1_scaling}: the per-component pooled Spearman
correlation between edge count and each phase of $T_{\mathit{gen}}$
for \textit{Model Once, Generate Any}, and the median share of the
$L$-sequence transformation in $T_{\mathit{gen}}$. Table S3 gives
the per-SPL Spearman correlation referenced inline for Bank
Account, Student Attendance System, and Elevator. Tables S4 and S5
report per-SPL median peak heap memory and median test execution
time for every (approach, $L$) combination; selected values are
cited inline in Section~\ref{sec:rq1_exec_memory}. Table S6 gives
the run-to-run stability statistics that back the
$\mathrm{IQR}\%$ summary in Section~\ref{sec:rq1_stability}. Table
S7 reports the JVM warm-up evidence cited at the end of
Section~\ref{sec:rq1_stability} and again in
Section~\ref{sec:threats}: per-cell mean/median and max/median
ratios across the eleven repetitions. Table S8 is the full pairwise
Wilcoxon table (with Vargha--Delaney $A_{12}$ effect sizes and
Benjamini--Hochberg corrected $p$-values) for every metric and
(SPL, $L$) combination; the manuscript reports only the headline
findings. Table S9 gives the per-SPL edge-count distribution that
contextualises the per-SPL correlations in Table S3 and the
within-SPL discussion in Section~\ref{sec:rq1_scaling}.

Spearman $\rho$ values are reported to three decimal places.
Percentage values use ROUND\_HALF\_UP to one decimal place after a
six-decimal float-noise scrub. Approach abbreviations: MO =
\textit{Model Once, Generate Any}, Struct = structural baseline,
Stoch = stochastic baseline.
"""


CAPTION_S1 = (
    r"S1: Pooled Spearman rank correlation $\rho$ between per-product "
    r"edge count and each component of $T_{\mathit{gen}}$ for "
    r"\textit{Model Once, Generate Any}, computed across all $4{,}455$ "
    r"products. The increase in component-level $\rho$ with $L$ explains "
    r"the increase in the overall $\rho$ reported for "
    r"\textit{Model Once, Generate Any} in Section~\ref{sec:rq1_scaling}."
)
CAPTION_S2 = (
    r"S2: Median and IQR of the share of the $L$-sequence transformation "
    r"in $T_{\mathit{gen}}$ for \textit{Model Once, Generate Any}, "
    r"computed across all $4{,}455$ products."
)
CAPTION_S3 = (
    r"S3: Per-SPL Spearman rank correlation $\rho$ between per-product "
    r"edge count and per-product median $T_{\mathit{gen}}$, for every "
    r"(approach, $L$) combination. The pooled values reported in "
    r"Section~\ref{sec:rq1_scaling} aggregate these per-SPL correlations "
    r"and are dominated by between-SPL variation in model size."
)
CAPTION_S4 = (
    r"S4: Per-SPL median peak heap memory (MB) during test generation, "
    r"per (approach, $L$) combination. Selected values are cited inline "
    r"in Section~\ref{sec:rq1_exec_memory}."
)
CAPTION_S5 = (
    r"S5: Per-SPL median test execution time $T_{\mathit{exec}}$ (ms), "
    r"per (approach, $L$) combination. Across the board, "
    r"$T_{\mathit{exec}}$ is small relative to $T_{\mathit{gen}}$ "
    r"(Section~\ref{sec:rq1_exec_memory})."
)
CAPTION_S6 = (
    r"S6: Run-to-run stability of $T_{\mathit{gen}}$ across the eleven "
    r"repetitions of each (SPL, approach, $L$) cell. Median, min, max, "
    r"coefficient of variation $\mathrm{CV}\% = 100 \cdot s/\bar{x}$, "
    r"and the relative interquartile range "
    r"$\mathrm{IQR}\% = 100 \cdot (Q_{3} - Q_{1}) / \text{median}$ are "
    r"reported for each cell. Section~\ref{sec:rq1_stability} summarises "
    r"these distributions."
)
CAPTION_S7 = (
    r"S7: JVM warm-up evidence per (SPL, approach, $L$) cell. The "
    r"$\mathrm{Mean}/\mathrm{Median}$ and $\mathrm{Max}/\mathrm{Median}$ "
    r"ratios across the eleven repetitions isolate cells in which a "
    r"single high outlier inflates the mean while leaving the median "
    r"and IQR almost unchanged. Cells with high $\mathrm{CV}\%$ but low "
    r"$\mathrm{IQR}\%$ correspond to the warm-up fingerprint discussed "
    r"in Section~\ref{sec:rq1_stability} and in the construct-validity "
    r"threat in Section~\ref{sec:threats}."
)
CAPTION_S8 = (
    r"S8: Pairwise Wilcoxon signed-rank results, Vargha--Delaney $A_{12}$ "
    r"effect sizes, and Benjamini--Hochberg corrected $p$-values for "
    r"every metric and (SPL, $L$) combination. Approach pairs MO~vs~Struct "
    r"and MO~vs~Stoch back the cross-approach claims in "
    r"Section~\ref{sec:rq1_speedup}."
)
CAPTION_S9 = (
    r"S9: Per-SPL edge-count distribution. The range ratio "
    r"$\max / \min$ quantifies the within-SPL variation in product "
    r"complexity. Section~\ref{sec:rq1_scaling} relates this range to "
    r"the per-SPL correlation: SPLs with a narrower edge range "
    r"(e.g., Bank Account, $3.3\times$) typically produce weak per-SPL "
    r"correlations, while SPLs with a wider range produce sharper trends."
)


def main() -> None:
    print("=" * 80)
    print("rq1_08: BUILD RQ1 SUPPLEMENTARY MATERIAL")
    print("=" * 80)

    project_root = find_project_root()
    if project_root is None:
        print("WARNING: project root not detected. Falling back to script "
              "directory; per-SPL files in legacy 'files/Cases/<SPL>/' will "
              "be skipped if absent. The consolidated workbook in "
              f"{RESULT_DIR} is the primary input.")

    print(f"Script  : {SCRIPT_DIR}")
    print(f"Cases   : {DATA_DIR}")
    print(f"Result  : {RESULT_DIR}")
    SUPP_DIR.mkdir(parents=True, exist_ok=True)

    parts: list[str] = [PREAMBLE, ""]

    print("\nBuilding S1 (per-component Spearman rho) ...")
    parts.append(_table_s1("tab:rq1-supp-S1", CAPTION_S1))
    parts.append("")

    print("Building S2 (transformation share) ...")
    parts.append(_table_s2("tab:rq1-supp-S2", CAPTION_S2))
    parts.append("")

    print("Building S3 (per-SPL Spearman rho) ...")
    parts.append(_table_s3("tab:rq1-supp-S3", CAPTION_S3))
    parts.append("")

    print("Building S4 (per-SPL peak heap memory) ...")
    parts.append(_per_spl_metric_table(
        "tab:rq1-supp-S4", CAPTION_S4,
        "TestGenPeakMemory(MB)", ndigits=1,
    ))
    parts.append("")

    print("Building S5 (per-SPL test execution time) ...")
    parts.append(_per_spl_metric_table(
        "tab:rq1-supp-S5", CAPTION_S5,
        "TestExecTime(ms)", ndigits=1,
    ))
    parts.append("")

    print("Building S6 (run stability) ...")
    parts.append(_table_s6("tab:rq1-supp-S6", CAPTION_S6))
    parts.append("")

    print("Building S7 (JVM warm-up evidence) ...")
    parts.append(_table_s7("tab:rq1-supp-S7", CAPTION_S7))
    parts.append("")

    print("Building S8 (pairwise Wilcoxon, full) ...")
    parts.append(_table_s8("tab:rq1-supp-S8", CAPTION_S8))
    parts.append("")

    print("Building S9 (per-SPL edge count ranges) ...")
    parts.append(_table_s9("tab:rq1-supp-S9", CAPTION_S9))
    parts.append("")

    out_path = SUPP_DIR / "rq1_supplementary.tex"
    out_path.write_text("\n".join(parts), encoding="utf-8")

    print()
    print("=" * 80)
    print(f"DONE.  Wrote {out_path}")
    print(f"        ({len([p for p in parts if p.startswith(chr(92) + 'begin{table')])} table blocks)")
    print("=" * 80)


if __name__ == "__main__":
    main()