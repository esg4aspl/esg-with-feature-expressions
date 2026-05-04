#!/usr/bin/env python3
"""
rq3_05_effectsize_bh.py

Step 5 of the RQ3 analysis pipeline.

Reads the paired-Wilcoxon output from rq3_04 and adds:
  (1) Benjamini--Hochberg adjusted p-values (per-metric family)
  (2) Significance flag at alpha = 0.05 after correction
  (3) Metric-aware winner column

The "winner" column resolves the practical question: in this (SPL, pair),
which approach is better given that this metric is interpreted as
higher-is-better or lower-is-better?

  - MutationScore           is higher-is-better
  - DetectionCost_Penalized is lower-is-better
  - DetectionCost_Conditional is lower-is-better

For higher-is-better metrics:
    A12 > 0.5  -> ApproachA wins
    A12 < 0.5  -> ApproachB wins
For lower-is-better metrics the directions flip.

Pairs that are "all ties" (every paired difference is zero) are reported
as winner = "tie", regardless of metric direction. Pairs with insufficient
sample (n_pairs < 6) get winner = "n/a".

A12 effect-size magnitude is preserved from rq3_04 (computed there directly
from the paired vectors). This script does NOT recompute A12.

Paths (relative to this script's location):
    Input  : ./rq3_result/rq3_pairwise_wilcoxon_<operator>.xlsx
    Output : ./rq3_result/rq3_pairwise_wilcoxon_<operator>_bh.xlsx

Usage:
    python rq3_05_effectsize_bh.py --operator edge       # primary
    python rq3_05_effectsize_bh.py --operator event      # supporting
    python rq3_05_effectsize_bh.py --operator both       # default

Dependencies: pandas, numpy, openpyxl
"""
from __future__ import annotations

import argparse
from pathlib import Path

import numpy as np
import pandas as pd
from openpyxl.utils import get_column_letter


# --------------------------------------------------------------------------
# Paths
# --------------------------------------------------------------------------
SCRIPT_DIR = Path(__file__).resolve().parent
RESULT_DIR = SCRIPT_DIR / "rq3_result"

ALPHA = 0.05

# Metric direction map. Keys must match the "Metric" column written by rq3_04.
METRIC_HIGHER_IS_BETTER = {
    "MutationScore":             True,
    "DetectionCost_Penalized":   False,
    "DetectionCost_Conditional": False,
}


# --------------------------------------------------------------------------
# Benjamini--Hochberg FDR correction (linear step-up)
# --------------------------------------------------------------------------
def bh_correct(p_values: np.ndarray) -> np.ndarray:
    """
    Linear step-up BH adjusted p-values. NaN inputs pass through.
    """
    p = np.asarray(p_values, dtype=float)
    mask = ~np.isnan(p)
    adj = np.full_like(p, np.nan, dtype=float)
    if mask.sum() == 0:
        return adj
    valid = p[mask]
    n = len(valid)
    order = np.argsort(valid)
    ranked = valid[order]
    q = ranked * n / np.arange(1, n + 1)
    q = np.minimum.accumulate(q[::-1])[::-1]
    q = np.minimum(q, 1.0)
    back = np.empty(n)
    back[order] = q
    adj[mask] = back
    return adj


# --------------------------------------------------------------------------
# Winner derivation
# --------------------------------------------------------------------------
def derive_winner(row: pd.Series, higher_is_better: bool) -> str:
    """
    Return one of: "ApproachA", "ApproachB", "tie", "n/a".

    Uses A12 to decide direction. Tie threshold: A12 == 0.5 exactly OR
    explicit "all ties" note from rq3_04.
    """
    note = str(row.get("note", "") or "")
    if "n<6" in note or "length mismatch" in note or note.startswith("wilcoxon error"):
        return "n/a"
    if "all ties" in note:
        return "tie"

    a12 = row.get("A12", np.nan)
    if pd.isna(a12):
        return "n/a"
    if a12 == 0.5:
        return "tie"
    if higher_is_better:
        return "ApproachA" if a12 > 0.5 else "ApproachB"
    else:
        return "ApproachB" if a12 > 0.5 else "ApproachA"


# --------------------------------------------------------------------------
# Per-sheet enhancement
# --------------------------------------------------------------------------
def enhance(df: pd.DataFrame, metric_label: str) -> pd.DataFrame:
    """Add p_value_BH, significant_BH, winner. Preserve column order."""
    if metric_label not in METRIC_HIGHER_IS_BETTER:
        raise ValueError(
            f"Unknown metric '{metric_label}'. "
            f"Add it to METRIC_HIGHER_IS_BETTER in rq3_05."
        )
    higher_is_better = METRIC_HIGHER_IS_BETTER[metric_label]

    out = df.copy()
    out["p_value_BH"] = bh_correct(out["p_value"].values)
    out["significant_BH"] = out["p_value_BH"] < ALPHA
    # significant_BH is False when p_BH is NaN (NaN < alpha = False), which
    # is the desired behaviour for ceiling/error rows.
    out["winner"] = out.apply(lambda r: derive_winner(r, higher_is_better), axis=1)

    preferred = [
        "SPL", "Metric", "ApproachA", "ApproachB",
        "n_pairs",
        "median_A", "median_B", "median_delta_AminusB",
        "W", "p_value", "p_value_BH", "significant_BH",
        "A12", "A12_magnitude", "winner",
        "note",
    ]
    cols = [c for c in preferred if c in out.columns] + \
           [c for c in out.columns if c not in preferred]
    return out[cols]


def _autosize_columns(worksheet, df: pd.DataFrame) -> None:
    for idx, col_name in enumerate(df.columns, start=1):
        worksheet.column_dimensions[get_column_letter(idx)].width = (
            len(str(col_name)) + 2
        )


# --------------------------------------------------------------------------
# Per-operator pipeline
# --------------------------------------------------------------------------
def run_for_operator(operator: str) -> None:
    input_file = RESULT_DIR / f"rq3_pairwise_wilcoxon_{operator}.xlsx"
    output_file = RESULT_DIR / f"rq3_pairwise_wilcoxon_{operator}_bh.xlsx"

    print(f"\n=== Operator: {operator} ===")
    print(f"Input  : {input_file}")
    print(f"Output : {output_file}")
    print()

    if not input_file.exists():
        print(
            f"  ERROR: Missing input {input_file.name}\n"
            f"  Run 'rq3_04_pairwise_wilcoxon.py --operator {operator}' first."
        )
        return

    xls = pd.ExcelFile(input_file)
    with pd.ExcelWriter(output_file, engine="openpyxl") as writer:
        for sheet in xls.sheet_names:
            df = pd.read_excel(xls, sheet_name=sheet)
            enhanced = enhance(df, sheet)
            enhanced.to_excel(writer, sheet_name=sheet, index=False)
            _autosize_columns(writer.sheets[sheet], enhanced)

            # Console summary
            n_total = len(enhanced)
            n_tie = int((enhanced["winner"] == "tie").sum())
            n_na = int((enhanced["winner"] == "n/a").sum())
            n_testable = n_total - n_tie - n_na
            n_sig = int(enhanced["significant_BH"].sum())
            mag_counts = enhanced["A12_magnitude"].value_counts()

            print(f"[{sheet}]")
            print(f"  rows                    : {n_total}")
            print(f"  ties (A12=0.5 / all-eq) : {n_tie}")
            print(f"  not testable            : {n_na}")
            print(f"  testable                : {n_testable}")
            print(f"  significant after BH    : {n_sig}")
            print(f"  effect-size distribution:")
            for mag in ("large", "medium", "small", "negligible"):
                cnt = int(mag_counts.get(mag, 0))
                print(f"      {mag:<12s}: {cnt}")
            # Winner breakdown
            wins_A = int((enhanced["winner"] == "ApproachA").sum())
            wins_B = int((enhanced["winner"] == "ApproachB").sum())
            print(f"  winner breakdown        : A={wins_A}  B={wins_B}  tie={n_tie}")
            print()

    print(f"Saved: {output_file}")


# --------------------------------------------------------------------------
# Main
# --------------------------------------------------------------------------
def main() -> None:
    parser = argparse.ArgumentParser(
        description="Add BH-adjusted p-values and metric-aware winner column to rq3_04 output."
    )
    parser.add_argument(
        "--operator",
        choices=["edge", "event", "both"],
        default="both",
        help="Which operator to process (default: both).",
    )
    args = parser.parse_args()

    print(f"Script location : {SCRIPT_DIR}")
    print(f"Result directory: {RESULT_DIR}")

    operators = ["edge", "event"] if args.operator == "both" else [args.operator]
    for op in operators:
        run_for_operator(op)


if __name__ == "__main__":
    main()