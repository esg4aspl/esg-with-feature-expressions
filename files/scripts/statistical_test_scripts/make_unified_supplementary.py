#!/usr/bin/env python3
"""
make_unified_supplementary.py

Single-script orchestrator that:

  1. Runs the three existing supplementary builders
       rq1_08_make_supplementary.py
       rq2_08_make_supplementary.py
       rq3_10_make_supplementary.py
     in their own directories so they produce their per-RQ .tex outputs
     against the latest data.
  2. Builds the new RQ1 Table S10 ("Linear regression of T_gen and
     T_gen^alg against edge count") from rq1_scaling_regression.xlsx
     (produced by rq1_04_scaling_regression.py).
  3. Converts every emitted table environment from the cas-sc form
     used by the three input scripts ([width=\\textwidth,pos=htbp] /
     [pos=htbp]) to the svjour3 form expected by the manuscript
     ([htbp]).
  4. Splices S10 into the RQ1 block (and patches the RQ1 descriptive
     paragraph to mention it).
  5. Concatenates RQ1 + RQ2 + RQ3 into a single
     supplementary_unified.tex that you can drop into the manuscript
     between, e.g., Conclusion and References (or wherever the journal
     wants supplementary material).

Layout assumption
-----------------
This script lives in the same directory as the three input scripts, e.g.

    files/scripts/statistical_test_scripts/
        make_unified_supplementary.py        <-- this file
        rq1_04_scaling_regression.py
        rq1_08_make_supplementary.py
        rq2_08_make_supplementary.py
        rq3_10_make_supplementary.py
        rq1_result/                          <-- produced by rq1_*
        rq2_result/                          <-- produced by rq2_*
        rq3_result/                          <-- produced by rq3_*

Output
------
    supplementary_unified.tex

To paste into the manuscript:

    % In the preamble (already present except longtable):
    \\usepackage{booktabs,multirow,tabularx,array,longtable}
    \\newcolumntype{Y}{>{\\centering\\arraybackslash}X}

    % Where you want the supplementary material to appear:
    \\renewcommand{\\thetable}{S\\arabic{table}}
    \\setcounter{table}{0}
    \\input{supplementary_unified.tex}

Usage
-----
    python make_unified_supplementary.py
    python make_unified_supplementary.py --skip-rebuild   # use existing .tex outputs

Dependencies: pandas, numpy, openpyxl  (same as the input scripts)
"""

from __future__ import annotations

import argparse
import re
import subprocess
import sys
from decimal import Decimal, ROUND_HALF_UP
from math import floor, log10
from pathlib import Path

import numpy as np
import pandas as pd


# =============================================================================
# Paths
# =============================================================================
HERE = Path(__file__).resolve().parent

RQ1_BUILDER = HERE / "rq1_08_make_supplementary.py"
RQ2_BUILDER = HERE / "rq2_08_make_supplementary.py"
RQ3_BUILDER = HERE / "rq3_10_make_supplementary.py"

RQ1_TEX = HERE / "rq1_result" / "supplementary" / "rq1_supplementary.tex"
RQ2_TEX = HERE / "rq2_result" / "supplementary" / "rq2_supplementary.tex"
RQ3_TEX = HERE / "rq3_result" / "supplementary" / "rq3_supplementary.tex"

# Source for the new S10 table (produced by rq1_04_scaling_regression.py).
RQ1_REGRESSION_XLSX = HERE / "rq1_result" / "rq1_scaling_regression.xlsx"

OUTPUT_TEX = HERE / "supplementary_unified.tex"


# =============================================================================
# Numerical helpers (used only for the S10 table)
# =============================================================================
def _round_half_up(x: float, ndigits: int = 3) -> float:
    if pd.isna(x):
        return float("nan")
    scrubbed = round(float(x), 6)
    q = Decimal(repr(scrubbed)).quantize(
        Decimal(10) ** -ndigits, rounding=ROUND_HALF_UP
    )
    return float(q)


def _fmt_int(v) -> str:
    if pd.isna(v):
        return "---"
    return f"{int(round(float(v))):,}".replace(",", "{,}")


def _fmt_r2(v) -> str:
    if pd.isna(v):
        return "---"
    return f"{_round_half_up(v, 3):.3f}"


def _fmt_sig3(v) -> str:
    """Three significant figures; scientific notation outside [0.01, 9999]."""
    if pd.isna(v):
        return "---"
    x = float(v)
    if x == 0.0:
        return "0"
    abs_x = abs(x)
    if 0.01 <= abs_x < 1e4:
        digits_after = max(0, 2 - int(floor(log10(abs_x))))
        return f"{x:,.{digits_after}f}".replace(",", "{,}")
    exp = int(floor(log10(abs_x)))
    mantissa = x / (10 ** exp)
    return rf"${mantissa:.2f}\!\times\!10^{{{exp}}}$"


def _fmt_pvalue_sci(p) -> str:
    if pd.isna(p):
        return "---"
    if p >= 0.05:
        return r"n.s."
    if p == 0.0:
        return r"$<\!10^{-300}$"
    exp = int(floor(log10(p)))
    mantissa = p / (10 ** exp)
    m_rounded = _round_half_up(mantissa, 1)
    if m_rounded >= 10:
        m_rounded /= 10
        exp += 1
    if m_rounded == int(m_rounded):
        m_str = f"{int(m_rounded)}"
    else:
        m_str = f"{m_rounded:.1f}"
    return rf"${m_str}\!\times\!10^{{{exp}}}$"


# =============================================================================
# Step 1 – run the three input builders
# =============================================================================
def run_input_builders() -> None:
    for path in (RQ1_BUILDER, RQ2_BUILDER, RQ3_BUILDER):
        if not path.exists():
            print(f"ERROR: required input script missing: {path}")
            sys.exit(1)
        print(f"[run] {path.name}")
        result = subprocess.run(
            [sys.executable, str(path)],
            cwd=str(path.parent),
        )
        if result.returncode != 0:
            print(f"ERROR: {path.name} exited with code {result.returncode}")
            sys.exit(result.returncode)


# =============================================================================
# Step 2 – build the new RQ1 Table S10
# =============================================================================
S10_CAPTION = (
    r"S10: Linear regression of per-product median $T_{\mathit{gen}}$ "
    r"and its algorithmic-only component $T_{\mathit{gen}}^{\mathit{alg}}$ "
    r"against per-product edge count $|E|$, for \textit{Model Once, "
    r"Generate Any}. The pooled block reports one regression per coverage "
    r"level across all $4{,}455$ products. The SPL-level block reports a "
    r"single regression per time variant, with each of the $24$ points "
    r"being the (median $T_{\mathit{gen}}$, mean edge count) pair of one "
    r"(SPL, $L$) combination. The disk-to-disk variant ($T_{\mathit{gen}}$) "
    r"is the one used for cross-approach comparison in "
    r"Section~\ref{sec:results_rq1}; the algorithmic variant "
    r"($T_{\mathit{gen}}^{\mathit{alg}}$) excludes model loading and test "
    r"recording and is the variant referenced in the complexity discussion "
    r"in Section~\ref{sec:complexity}. The $R^{2}$ values quoted inline in "
    r"Section~\ref{sec:complexity} ($R^{2} = 0.65$ at $L = 2$ rising to "
    r"$R^{2} = 0.72$ at $L = 4$, and $R^{2} = 0.77$ at the SPL level) "
    r"correspond to the rows of this table."
)


def build_rq1_s10() -> str:
    if not RQ1_REGRESSION_XLSX.exists():
        return (
            f"% Table S10 source missing: {RQ1_REGRESSION_XLSX}\n"
            f"% Run rq1_04_scaling_regression.py before this script.\n"
        )

    pooled_total = pd.read_excel(RQ1_REGRESSION_XLSX, sheet_name="pooled_regression_total")
    pooled_alg   = pd.read_excel(RQ1_REGRESSION_XLSX, sheet_name="pooled_regression_alg")
    spl_total    = pd.read_excel(RQ1_REGRESSION_XLSX, sheet_name="spl_level_regression_total")
    spl_alg      = pd.read_excel(RQ1_REGRESSION_XLSX, sheet_name="spl_level_regression_alg")

    def _filter_mo(df: pd.DataFrame) -> pd.DataFrame:
        return df[df["Approach"] == "ESG-Fx"].copy()

    pooled_total = _filter_mo(pooled_total)
    pooled_alg   = _filter_mo(pooled_alg)
    spl_total    = _filter_mo(spl_total)
    spl_alg      = _filter_mo(spl_alg)

    def _row(level_label: str, time_var_tex: str, df: pd.DataFrame,
             level: str | None) -> str:
        sub = df if level is None else df[df["Level"] == level]
        if sub.empty:
            return (f"{time_var_tex} & {level_label} & --- & --- & "
                    f"--- & --- & --- \\\\")
        r = sub.iloc[0]
        n_col = "N_products" if "N_products" in sub.columns else "N_SPL_level_points"
        return (f"{time_var_tex} & {level_label} & "
                f"{_fmt_int(r[n_col])} & "
                f"{_fmt_sig3(r['Linreg_slope'])} & "
                f"{_fmt_sig3(r['Linreg_intercept'])} & "
                f"{_fmt_r2(r['R2'])} & "
                f"{_fmt_pvalue_sci(r['Linreg_p'])} \\\\")

    out: list[str] = []
    out.append(r"\begin{table*}[htbp]")
    out.append(rf"\caption{{{S10_CAPTION}}}")
    out.append(r"\label{tab:rq1-supp-S10}")
    out.append(r"\setlength{\tabcolsep}{5pt}")
    out.append(r"\footnotesize")
    out.append(r"\begin{tabular*}{\textwidth}{@{\extracolsep{\fill}} "
               r"l c r r r r r @{}}")
    out.append(r"\toprule")
    out.append(r"\textbf{Time variant} & \textbf{$L$} & \textbf{$N$} & "
               r"\textbf{Slope (ms/edge)} & \textbf{Intercept (ms)} & "
               r"\textbf{$R^{2}$} & \textbf{$p$} \\")
    out.append(r"\midrule")
    out.append(r"\multicolumn{7}{@{}l}{\textit{Pooled per-product "
               r"regression (one regression per coverage level)}} \\")
    for L in ("L2", "L3", "L4"):
        L_tex = "$L = " + L[1:] + "$"
        out.append(_row(L_tex, r"$T_{\mathit{gen}}$", pooled_total, L))
    for L in ("L2", "L3", "L4"):
        L_tex = "$L = " + L[1:] + "$"
        out.append(_row(L_tex, r"$T_{\mathit{gen}}^{\mathit{alg}}$",
                        pooled_alg, L))
    out.append(r"\midrule")
    out.append(r"\multicolumn{7}{@{}l}{\textit{SPL-level regression "
               r"(one point per (SPL, $L$) combination)}} \\")
    out.append(_row("---", r"$T_{\mathit{gen}}$", spl_total, None))
    out.append(_row("---", r"$T_{\mathit{gen}}^{\mathit{alg}}$",
                    spl_alg, None))
    out.append(r"\bottomrule")
    out.append(r"\end{tabular*}")
    out.append(r"\end{table*}")
    return "\n".join(out)


# =============================================================================
# Step 3 – svjour3 conversion
# =============================================================================
def to_svjour3(tex: str) -> str:
    """Convert cas-sc table envs to svjour3-compatible ones.

    Replacements:
      \\begin{table*}[width=\\textwidth,pos=htbp]  ->  \\begin{table*}[htbp]
      \\begin{table}[pos=htbp]                     ->  \\begin{table}[htbp]
    """
    tex = tex.replace(r"[width=\textwidth,pos=htbp]", "[htbp]")
    tex = tex.replace(r"[pos=htbp]", "[htbp]")
    return tex


# =============================================================================
# Step 3b – fix the captions and the table widths
# =============================================================================
def strip_caption_prefixes(tex: str) -> str:
    """Remove the 'Sn: ' prefix from each \\caption{Sn: ...} so tables don't
    render as 'Table S1: S1: ...'. With \\renewcommand{\\thetable}{S\\arabic
    {table}}, LaTeX itself prepends 'Table SN:' to every caption, so the
    hardcoded 'S1:' / 'S2:' / ... in the source caption becomes a duplicate.
    """
    return re.sub(r"\\caption\{S\d+:\s*", r"\\caption{", tex)


def reset_counter_per_rq_section(tex: str) -> str:
    """Insert \\setcounter{table}{0} just before each S-RQ section so each
    RQ block starts at Table S1. This matches the per-RQ numbering that the
    introductory paragraphs in the input scripts assume ('Tables S1 and S2
    give the full throughput at L=3 and L=4', etc.)."""
    return re.sub(
        r"(\\section\*\{S-RQ)",
        r"\n\\setcounter{table}{0}\n\1",
        tex,
    )


def abbreviate_stability_bands(tex: str) -> str:
    """Replace 'very stable' / 'stable' / 'notable variance' band labels with
    two-letter abbreviations VS / S / NV in table cells, so the wide
    stability tables (RQ2 S6 longtable and S7 ranking) stop overflowing.
    A short abbreviation key is added to the relevant captions."""

    # Replace inside table cells. Order matters: the longer phrases must be
    # rewritten first, otherwise the bare 'stable' rule would also catch
    # 'very stable'.

    # End-of-row ( ... & X \\ )
    tex = re.sub(r"& very stable \\\\", r"& VS \\\\", tex)
    tex = re.sub(r"& notable variance \\\\", r"& NV \\\\", tex)
    tex = re.sub(r"& stable \\\\", r"& S \\\\", tex)
    # Mid-row ( ... & X & ... )
    tex = re.sub(r"& very stable &", r"& VS &", tex)
    tex = re.sub(r"& notable variance &", r"& NV &", tex)
    tex = re.sub(r"& stable &", r"& S &", tex)

    # Add an abbreviation key to the S6 caption (which already explains the
    # bands themselves).
    tex = tex.replace(
        r"Stability bands: \emph{very stable}",
        r"Stability bands (rendered in the table cells as VS, S, NV "
        r"respectively): \emph{very stable}",
    )

    # Add a similar key to the S7 caption (which uses the same column but
    # does not explain the bands).
    s7_anchor = (
        r"The same effect was discussed for RQ1 in "
        r"Section~\ref{sec:rq1_stability}.}"
    )
    s7_replacement = (
        r"The same effect was discussed for RQ1 in "
        r"Section~\ref{sec:rq1_stability}. Stability column abbreviations: "
        r"VS = very stable, S = stable, NV = notable variance.}"
    )
    tex = tex.replace(s7_anchor, s7_replacement)

    return tex


def abbreviate_approach_names(tex: str) -> str:
    """Replace the long approach names with abbreviations in TABLE CONTEXT.

        'Model Once, Generate Any' -> 'MO'
        'Structural baseline'      -> 'Struct'
        'Stochastic baseline'      -> 'Stoch'

    Several wide tables (notably RQ1 S6 / S7 / S8 and the RQ3 mutation-score
    tables) repeat these long names in the leftmost column, which forces
    that column wide enough to hold 'Model Once, Generate Any' on a single
    line. The first column then dominates the row and the table overflows
    \\textwidth -- and \\resizebox cannot help because the tabular*'s
    declared width is already \\textwidth (it does not see the overflow as
    extra natural width).

    The supplementary preamble already defines the abbreviation key
    ('MO = Model Once, Generate Any, Struct = structural baseline, Stoch
    = stochastic baseline'). Substituting in the cells is therefore safe
    and lossless.

    Replacements happen ONLY in table contexts -- never in captions or
    prose -- by anchoring on the surrounding markup:
        \\textit{...}          (italic cell content / column header)
        \\textbf{...}          (bold column header)
        \\multirow{N}{*}{...}  (multirow cell)
        ^...&                  (start-of-row cell, followed by `&`)
    """
    # ----- Model Once, Generate Any -> MO -----
    # \multirow{N}{*}{\textit{Model Once, Generate Any}}  (RQ3 mutation tables)
    tex = re.sub(
        r"\\multirow\{(\d+)\}\{\*\}\{\\textit\{Model Once, Generate Any\}\}",
        r"\\multirow{\1}{*}{\\textit{MO}}",
        tex,
    )
    # \textit{Model Once, Generate Any} as cell content followed by &
    # (RQ1 S6, S7 etc.). Anchor on the trailing & so the abbreviation key
    # in the introductory paragraph ("MO = \textit{Model Once, Generate
    # Any}, Struct = ...") is left untouched.
    tex = re.sub(
        r"\\textit\{Model Once, Generate Any\}\s*&",
        r"\\textit{MO} &",
        tex,
    )
    # \textbf{\textit{Model Once, Generate Any}} (column headers spanning
    # several columns -- RQ1 S3, S4, S5, S9; RQ3 paired Wilcoxon).
    tex = tex.replace(
        r"\textbf{\textit{Model Once, Generate Any}}",
        r"\textbf{\textit{MO}}",
    )

    # ----- Structural baseline -> Struct -----
    tex = re.sub(
        r"\\multirow\{(\d+)\}\{\*\}\{Structural baseline\}",
        r"\\multirow{\1}{*}{Struct}",
        tex,
    )
    tex = tex.replace(
        r"\textbf{Structural baseline}",
        r"\textbf{Struct}",
    )
    tex = re.sub(
        r"^(\s*)Structural baseline\s*&",
        r"\1Struct &",
        tex,
        flags=re.MULTILINE,
    )

    # ----- Stochastic baseline -> Stoch -----
    tex = re.sub(
        r"\\multirow\{(\d+)\}\{\*\}\{Stochastic baseline\}",
        r"\\multirow{\1}{*}{Stoch}",
        tex,
    )
    tex = tex.replace(
        r"\textbf{Stochastic baseline}",
        r"\textbf{Stoch}",
    )
    tex = re.sub(
        r"^(\s*)Stochastic baseline\s*&",
        r"\1Stoch &",
        tex,
        flags=re.MULTILINE,
    )

    return tex


def fit_tables_to_page(tex: str) -> str:
    """Make wide supplementary tables fit \\textwidth.

    Strategy
    --------
    1. Each `tabular*` / `tabularx` whose nominal width is `\\textwidth` is
       wrapped in `\\resizebox{\\textwidth}{!}{...}`. We use `\\resizebox`
       (from `graphicx`, which the manuscript already loads) instead of
       `\\adjustbox{max width=\\textwidth}{...}`: the latter sees the table's
       declared width (\\textwidth) rather than its real typeset width and
       therefore does not shrink overflowing content. `\\resizebox` always
       scales the rendered box to exactly \\textwidth, which both shrinks
       overflow and gently stretches narrow tables. For supplementary tables
       this trade-off is acceptable.

       Tables declared with `\\columnwidth` (RQ1 S1, S2, S9) are left alone
       since they're already designed for the narrower scope.

    2. Each `longtable` (page-spanning, cannot be put in a box) is wrapped
       in `{\\scriptsize ...}` so its font size shrinks one step.
    """
    # 1. \resizebox the textwidth-sized tabular* / tabularx
    pattern = re.compile(
        r"(\\begin\{(tabular\*|tabularx)\}\{\\textwidth\}\{[^}]*\}.*?\\end\{\2\})",
        flags=re.DOTALL,
    )

    def _wrap(m: re.Match) -> str:
        return r"\resizebox{\textwidth}{!}{%" + "\n" + m.group(1) + "%\n" + "}"

    tex = pattern.sub(_wrap, tex)

    # 2. \scriptsize the longtable
    tex = re.sub(
        r"(\\begin\{longtable\}.*?\\end\{longtable\})",
        r"{\\scriptsize%\n\1%\n}",
        tex,
        flags=re.DOTALL,
    )

    return tex


# =============================================================================
# Step 4 – splice S10 into RQ1 and patch its descriptive paragraph
# =============================================================================
S10_PARAGRAPH_INSERT = (
    " Table S10 reports the linear-regression statistics promised in "
    r"Section~\ref{sec:complexity} and in the construct-validity threat "
    r"in Section~\ref{sec:threats}: slope, intercept, $R^{2}$, and "
    r"$p$-value for the regression of per-product median "
    r"$T_{\mathit{gen}}$ and its algorithmic-only component "
    r"$T_{\mathit{gen}}^{\mathit{alg}}$ against per-product edge count, "
    r"both pooled across all $4{,}455$ products and at the SPL level."
)


def patch_rq1_for_s10(tex: str, s10_block: str) -> str:
    # Update the header comment
    tex = tex.replace(
        "tab:rq1-supp-S1 ... tab:rq1-supp-S9",
        "tab:rq1-supp-S1 ... tab:rq1-supp-S10",
    )
    # Append a descriptive sentence to the introductory paragraph.
    anchor = "within-SPL discussion in Section~\\ref{sec:rq1_scaling}."
    if anchor in tex:
        tex = tex.replace(anchor, anchor + S10_PARAGRAPH_INSERT, 1)
    else:
        print("[warn] RQ1 anchor for S10 paragraph not found; "
              "preamble prose left unmodified.")
    # Append S10 at the end of the RQ1 file.
    tex = tex.rstrip() + "\n\n" + s10_block + "\n"
    return tex


# =============================================================================
# Step 5 – assemble unified .tex
# =============================================================================
MASTER_HEADER = r"""% =========================================================================
% UNIFIED SUPPLEMENTARY MATERIAL
% Auto-generated by make_unified_supplementary.py
% Do not edit by hand -- re-run the script instead.
%
% Compatibility: this file is written for the svjour3 document class.
% Wide tables are wrapped in \resizebox{\textwidth}{!}{...} (graphicx) so
% they shrink to fit the page; longtables are wrapped in {\scriptsize ...}.
% The "very stable" / "stable" / "notable variance" stability bands are
% rendered in cells as VS / S / NV (the abbreviation key is added to the
% relevant captions automatically).
%
% Required packages -- add the ones not already in the preamble:
%
%     \usepackage{amsmath}                 % for \text{...} in math mode
%     \usepackage{booktabs,multirow,tabularx,array,longtable}
%     \newcolumntype{Y}{>{\centering\arraybackslash}X}
%
% (graphicx is already in the manuscript preamble; \resizebox lives there.)
%
% -------------------------------------------------------------------------
% Two ways to use this file
% -------------------------------------------------------------------------
%
% (A) Bundle into the main manuscript (RECOMMENDED -- all cross-references
%     to the main manuscript automatically resolve, no `??' artifacts):
%
%     % at the desired position in the manuscript .tex
%     \renewcommand{\thetable}{S\arabic{table}}
%     \input{supplementary_unified.tex}
%
%     (The orchestrator already inserts \setcounter{table}{0} at the start
%      of each S-RQ section, so RQ1, RQ2 and RQ3 each restart at S1.)
%
% (B) Standalone supplementary PDF (requires the `xr-hyper' package and
%     a separately-compiled main manuscript .aux file):
%
%     \documentclass[smallextended]{svjour3}
%     \usepackage{amsmath,graphicx,booktabs,multirow,tabularx,array,longtable}
%     \usepackage[hidelinks]{hyperref}
%     \usepackage{xr-hyper}
%     \externaldocument{<main_manuscript_filename_without_extension>}
%     \newcolumntype{Y}{>{\centering\arraybackslash}X}
%     \begin{document}
%     \title{Supplementary Material: ...}
%     \maketitle
%     \renewcommand{\thetable}{S\arabic{table}}
%     \input{supplementary_unified.tex}
%     \end{document}
%
%     Compile order: pdflatex main.tex (twice) -> pdflatex supplementary.tex
%     The .aux of the main manuscript must exist before xr-hyper can read it.
% =========================================================================

"""


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__,
                                     formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument(
        "--skip-rebuild", action="store_true",
        help="Skip running the three input builders; use existing "
             "rq{1,2,3}_supplementary.tex files as-is.",
    )
    args = parser.parse_args()

    print("=" * 72)
    print("UNIFIED SUPPLEMENTARY BUILDER")
    print("=" * 72)
    print(f"Working dir : {HERE}")
    print(f"Output      : {OUTPUT_TEX}")

    # --- Step 1 ---
    if args.skip_rebuild:
        print("\n[skip] not re-running the three input builders (--skip-rebuild).")
    else:
        print("\nStep 1 -- running input builders ...")
        run_input_builders()

    # --- Step 2 ---
    print("\nStep 2 -- building RQ1 Table S10 (regression) ...")
    s10_block = build_rq1_s10()

    # --- Step 3 + 4 ---
    print("\nStep 3 -- reading and post-processing per-RQ outputs ...")
    for path in (RQ1_TEX, RQ2_TEX, RQ3_TEX):
        if not path.exists():
            print(f"ERROR: missing intermediate file: {path}")
            print("       Did the input builder fail? "
                  "Try running it manually to see why.")
            sys.exit(1)

    rq1_tex = RQ1_TEX.read_text(encoding="utf-8")
    rq1_tex = to_svjour3(rq1_tex)
    rq1_tex = patch_rq1_for_s10(rq1_tex, s10_block)
    rq1_tex = strip_caption_prefixes(rq1_tex)
    rq1_tex = abbreviate_stability_bands(rq1_tex)
    rq1_tex = abbreviate_approach_names(rq1_tex)
    rq1_tex = fit_tables_to_page(rq1_tex)

    rq2_tex = RQ2_TEX.read_text(encoding="utf-8")
    rq2_tex = to_svjour3(rq2_tex)
    rq2_tex = strip_caption_prefixes(rq2_tex)
    rq2_tex = abbreviate_stability_bands(rq2_tex)
    rq2_tex = abbreviate_approach_names(rq2_tex)
    rq2_tex = fit_tables_to_page(rq2_tex)

    rq3_tex = RQ3_TEX.read_text(encoding="utf-8")
    rq3_tex = to_svjour3(rq3_tex)
    rq3_tex = strip_caption_prefixes(rq3_tex)
    rq3_tex = abbreviate_stability_bands(rq3_tex)
    rq3_tex = abbreviate_approach_names(rq3_tex)
    rq3_tex = fit_tables_to_page(rq3_tex)

    # --- Step 5 ---
    print("\nStep 4 -- writing unified .tex ...")
    combined = rq1_tex + "\n\n" + rq2_tex + "\n\n" + rq3_tex
    combined = reset_counter_per_rq_section(combined)
    OUTPUT_TEX.write_text(
        MASTER_HEADER + combined,
        encoding="utf-8",
    )

    print()
    print("=" * 72)
    print(f"DONE. Wrote {OUTPUT_TEX}")
    print(f"      ({OUTPUT_TEX.stat().st_size:,} bytes)")
    print("=" * 72)
    print()
    print("Next steps:")
    print("  1. Add these to your manuscript preamble (longtable for RQ2 S6,")
    print("     amsmath for \\text{...} in math mode -- both required):")
    print("        \\usepackage{amsmath,longtable}")
    print("     (graphicx, booktabs, multirow, tabularx, array are already")
    print("      in your preamble.)")
    print()
    print("  2A. To bundle into the main manuscript (recommended -- all")
    print("      cross-refs resolve, no '??'):")
    print("        \\renewcommand{\\thetable}{S\\arabic{table}}")
    print(f"        \\input{{{OUTPUT_TEX.name}}}")
    print()
    print("  2B. For a standalone supplementary PDF, see the comment block")
    print(f"      at the top of {OUTPUT_TEX.name} for the xr-hyper template.")


if __name__ == "__main__":
    main()