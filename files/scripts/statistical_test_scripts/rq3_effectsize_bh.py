#!/usr/bin/env python3
"""
rq3_effectsize_bh.py

Extends the Mann-Whitney U results from rq3_mannwhitneyu.py with:
  (1) Vargha-Delaney A12 effect size
  (2) Magnitude label (negligible / small / medium / large)
      using Vargha-Delaney (2000) thresholds on |0.5 - A12|
  (3) Benjamini-Hochberg adjusted p-values (per-metric family)
  (4) Final significance flag at alpha = 0.05 after correction

This closes the two main statistical-rigor gaps reviewers will flag:
  - Raw p-values alone are uninformative at large n.
  - Running 72 tests per metric without FDR control inflates false positives.

Paths (relative to this script's location):
    Input  : ./rq3_result/rq3_mannwhitneyu_<operator>.xlsx
    Output : ./rq3_result/rq3_effectsize_bh_<operator>.xlsx

Usage (from any directory):
    python files/scripts/statistical_test_scripts/rq3_effectsize_bh.py --operator edge
    python files/scripts/statistical_test_scripts/rq3_effectsize_bh.py --operator event
    python files/scripts/statistical_test_scripts/rq3_effectsize_bh.py --operator both

Dependencies: pandas, numpy, openpyxl
"""
from __future__ import annotations

import argparse
from pathlib import Path

import numpy as np
import pandas as pd


# --------------------------------------------------------------------------
# Paths
# --------------------------------------------------------------------------
SCRIPT_DIR = Path(__file__).resolve().parent
RESULT_DIR = SCRIPT_DIR / "rq3_result"


# --------------------------------------------------------------------------
# Vargha-Delaney A12
# --------------------------------------------------------------------------
def a12_from_U(U: float, n_A: int, n_B: int) -> float:
    """
    Vargha-Delaney A12 derived directly from the Mann-Whitney U statistic:
        A12 = U_A / (n_A * n_B)

    Interpretation:
        A12 = 0.5  -> no effect (groups stochastically equal)
        A12 > 0.5  -> group A values tend to be larger
        A12 < 0.5  -> group B values tend to be larger

    For ceiling cases (U is NaN because both groups are constant and equal)
    we return 0.5, which is the correct effect size: the distributions are
    identical, so there is no effect.
    """
    if pd.isna(U):
        return 0.5
    if n_A == 0 or n_B == 0:
        return np.nan
    return float(U) / (n_A * n_B)


def a12_magnitude(a12: float) -> str:
    """
    Vargha-Delaney (2000) magnitude thresholds on the absolute deviation
    from 0.5:
        |0.5 - A12| < 0.06  -> negligible
        |0.5 - A12| < 0.14  -> small
        |0.5 - A12| < 0.21  -> medium
        |0.5 - A12| >= 0.21 -> large
    """
    if pd.isna(a12):
        return ""
    d = abs(0.5 - a12)
    if d < 0.06:
        return "negligible"
    if d < 0.14:
        return "small"
    if d < 0.21:
        return "medium"
    return "large"


# --------------------------------------------------------------------------
# Benjamini-Hochberg FDR correction
# --------------------------------------------------------------------------
def bh_correct(p_values: np.ndarray) -> np.ndarray:
    """
    Benjamini-Hochberg adjusted p-values (linear step-up).

    Implementation steps:
      1. Sort p-values ascending (keep track of original positions).
      2. For rank i (1-indexed), tentative adjusted p = p_(i) * n / i.
      3. Enforce monotonicity: each adjusted p must be <= the next larger
         one. Achieved with a reverse cumulative minimum.
      4. Clip to [0, 1] and map back to original order.

    NaN p-values pass through untouched (ceiling cases without a valid U).
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
# Enhancement pipeline
# --------------------------------------------------------------------------
def enhance(df: pd.DataFrame) -> pd.DataFrame:
    """Add A12, magnitude, BH-adjusted p-values, and significance flag."""
    out = df.copy()

    out["A12"] = [
        a12_from_U(u, na, nb)
        for u, na, nb in zip(out["U_statistic"], out["n_A"], out["n_B"])
    ]
    out["A12_magnitude"] = out["A12"].apply(a12_magnitude)
    out["p_value_BH"] = bh_correct(out["p_value"].values)
    note_is_ceiling = out["note"].fillna("").str.contains("constant and equal")
    out["significant_BH"] = (out["p_value_BH"] < 0.05) & (~note_is_ceiling)

    preferred = [
        "SPL", "ApproachA", "ApproachB",
        "n_A", "n_B",
        "median_A", "median_B",
        "mean_A", "mean_B",
        "U_statistic",
        "A12", "A12_magnitude", "direction",
        "p_value", "p_value_BH", "significant_BH",
        "note",
    ]
    cols = [c for c in preferred if c in out.columns] + \
           [c for c in out.columns if c not in preferred]
    return out[cols]


# --------------------------------------------------------------------------
# Per-operator pipeline
# --------------------------------------------------------------------------
def run_for_operator(operator: str) -> None:
    input_file = RESULT_DIR / f"rq3_mannwhitneyu_{operator}.xlsx"
    output_file = RESULT_DIR / f"rq3_effectsize_bh_{operator}.xlsx"

    print(f"\n=== Operator: {operator} ===")
    print(f"Input  : {input_file}")
    print(f"Output : {output_file}")
    print()

    if not input_file.exists():
        print(
            f"  ERROR: Missing input {input_file.name}\n"
            f"  Run 'rq3_mannwhitneyu.py --operator {operator}' first."
        )
        return

    xls = pd.ExcelFile(input_file)
    with pd.ExcelWriter(output_file, engine="openpyxl") as writer:
        for sheet in xls.sheet_names:
            df = pd.read_excel(xls, sheet_name=sheet)
            enhanced = enhance(df)
            enhanced.to_excel(writer, sheet_name=sheet, index=False)

            n_total = len(enhanced)
            n_ceil = enhanced["note"].fillna("").str.contains(
                "constant and equal"
            ).sum()
            n_testable = n_total - n_ceil
            n_sig = int(enhanced["significant_BH"].sum())
            mag_counts = enhanced["A12_magnitude"].value_counts()

            print(f"[{sheet}]")
            print(f"  rows                    : {n_total}")
            print(f"  ceiling (not testable)  : {n_ceil}")
            print(f"  testable                : {n_testable}")
            print(f"  significant after BH    : {n_sig}")
            print(f"  effect-size distribution:")
            for mag in ("large", "medium", "small", "negligible"):
                cnt = int(mag_counts.get(mag, 0))
                print(f"      {mag:<12s}: {cnt}")
            print()

    print(f"Saved: {output_file}")


# --------------------------------------------------------------------------
# Main
# --------------------------------------------------------------------------
def main() -> None:
    parser = argparse.ArgumentParser(
        description="Add Vargha-Delaney A12 and Benjamini-Hochberg correction to Mann-Whitney results."
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