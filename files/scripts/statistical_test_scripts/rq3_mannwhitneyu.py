#!/usr/bin/env python3
"""
rq3_mannwhitneyu.py

Pairwise Mann-Whitney U tests for RQ3 fault detection results.

For each SPL, performs nine pairwise comparisons at the product level on two
metrics:
  (1) Mutation Score (%)               -> "MutationScore"
  (2) Percentage of Suite to Detect (%) -> "DetectionCost"

Comparison pairs per SPL:
    ESG-Fx L2  vs  EFG L2
    ESG-Fx L3  vs  EFG L3
    ESG-Fx L4  vs  EFG L4
    ESG-Fx L2  vs  Stochastic
    ESG-Fx L3  vs  Stochastic
    ESG-Fx L4  vs  Stochastic
    EFG    L2  vs  Stochastic
    EFG    L3  vs  Stochastic
    EFG    L4  vs  Stochastic

For deterministic approaches (ESG-Fx, EFG), each product contributes one value.
For the stochastic baseline, each product contributes the MEDIAN across the
seeds recorded in the <Operator>_MultiSeed sheet.

Paths are resolved RELATIVE to this script's location:
    Script :  files/scripts/statistical_test_scripts/rq3_mannwhitneyu.py
    Data   :  files/Cases/<SPL>/RQ3_<SPL>_perProduct_rawData.xlsx
    Output :  files/scripts/statistical_test_scripts/rq3_result/rq3_mannwhitneyu_<operator>.xlsx

Usage (from any directory):
    python files/scripts/statistical_test_scripts/rq3_mannwhitneyu.py --operator edge
    python files/scripts/statistical_test_scripts/rq3_mannwhitneyu.py --operator event
    python files/scripts/statistical_test_scripts/rq3_mannwhitneyu.py --operator both

Dependencies: pandas, numpy, scipy, openpyxl
"""
from __future__ import annotations

import argparse
import warnings
from pathlib import Path

import numpy as np
import pandas as pd
from scipy.stats import mannwhitneyu


# --------------------------------------------------------------------------
# Paths (all relative to this script's location)
# Layout: <repo>/files/scripts/statistical_test_scripts/<this script>
#         <repo>/files/Cases/<SPL>/RQ3_<SPL>_perProduct_rawData.xlsx
# --------------------------------------------------------------------------
SCRIPT_DIR = Path(__file__).resolve().parent
SCRIPTS_DIR = SCRIPT_DIR.parent
FILES_DIR = SCRIPTS_DIR.parent
DATA_DIR = FILES_DIR / "Cases"
OUTPUT_DIR = SCRIPT_DIR / "rq3_result"


# --------------------------------------------------------------------------
# SPL configuration
# --------------------------------------------------------------------------
SPL_MAPPING = {
    "SodaVendingMachine": "SVM",
    "eMail": "eM",
    "Elevator": "El",
    "BankAccountv2": "BAv2",
    "StudentAttendanceSystem": "SAS",
    "Tesla": "Te",
    "syngovia": "Svia",
    "HockertyShirts": "HS",
}

ESGFX_APPROACHES = ["ESG-Fx_L2", "ESG-Fx_L3", "ESG-Fx_L4"]
EFG_APPROACHES = ["EFG_L2", "EFG_L3", "EFG_L4"]
STOCHASTIC_LABEL = "RandomWalk"

# (deterministic-sheet column, multiseed-sheet column, short label)
METRICS = [
    ("MutationScore(%)", "MutationScore(%)", "MutationScore"),
    ("PercentageOfSuiteToDetect(%)", "MedianPercentageOfSuiteToDetect(%)", "DetectionCost"),
]

# --operator value -> (sheet name in raw file, sheet name in multiseed)
OPERATOR_SHEETS = {
    "edge": ("EdgeOmission", "EdgeOmission_MultiSeed"),
    "event": ("EventOmission", "EventOmission_MultiSeed"),
}


# --------------------------------------------------------------------------
# Data loading
# --------------------------------------------------------------------------
def _find_raw_file(spl_full_name: str) -> Path:
    """Locate per-product raw data file, tolerating trailing-underscore variants."""
    folder = DATA_DIR / spl_full_name
    candidates = [
        folder / f"RQ3_{spl_full_name}_perProduct_rawData.xlsx",
        folder / f"RQ3_{spl_full_name}_perProduct_rawData_.xlsx",
    ]
    for p in candidates:
        if p.exists():
            return p
    raise FileNotFoundError(
        f"No raw data file for {spl_full_name} in {folder}. "
        f"Expected one of: {[c.name for c in candidates]}"
    )


def load_spl_data(spl_full_name: str, det_sheet: str, ms_sheet: str) -> tuple[pd.DataFrame, pd.DataFrame]:
    """Return (deterministic sheet, multi-seed sheet) as DataFrames."""
    path = _find_raw_file(spl_full_name)
    xls = pd.ExcelFile(path)
    if det_sheet not in xls.sheet_names:
        raise ValueError(f"{spl_full_name}: sheet '{det_sheet}' missing in {path.name}")
    if ms_sheet not in xls.sheet_names:
        raise ValueError(f"{spl_full_name}: sheet '{ms_sheet}' missing in {path.name}")
    det = pd.read_excel(xls, sheet_name=det_sheet)
    ms = pd.read_excel(xls, sheet_name=ms_sheet)
    return det, ms


def _values_deterministic(det_df: pd.DataFrame, approach: str, metric_col: str) -> np.ndarray:
    """Vector of per-product values for a deterministic approach (ESG-Fx or EFG)."""
    sub = det_df[det_df["TestingApproach"] == approach]
    return sub[metric_col].to_numpy(dtype=float)


def _values_stochastic(ms_df: pd.DataFrame, multiseed_metric_col: str) -> np.ndarray:
    """
    Vector of per-product values for the stochastic baseline, computed as the
    median across the recorded seeds for each product.
    """
    agg = ms_df.groupby("ProductID")[multiseed_metric_col].median()
    return agg.to_numpy(dtype=float)


# --------------------------------------------------------------------------
# Mann-Whitney U (tie-safe wrapper)
# --------------------------------------------------------------------------
def _mannwhitneyu_safe(x: np.ndarray, y: np.ndarray) -> tuple[float, float, str, str]:
    """
    Run Mann-Whitney U (two-sided). Returns (U, p, direction, note).

    'direction' indicates which group tends to have larger values:
        "A>B"  : values in x are stochastically larger than in y
        "A<B"  : values in y are stochastically larger than in x
        "A=B"  : tied rank sum (rare; only under perfect symmetry)
        ""     : not applicable (ceiling or empty)

    Direction is derived from the U statistic, not from medians, so it is
    still correct when medians are tied but distributions differ.

    Edge cases:
      - Empty input                    -> (NaN, NaN, "",    "empty sample")
      - Both groups constant and equal -> (NaN, 1.0, "",    "both constant and equal")
      - Normal case                    -> scipy result, tie-corrected
    """
    x = np.asarray(x, dtype=float)
    y = np.asarray(y, dtype=float)
    x = x[~np.isnan(x)]
    y = y[~np.isnan(y)]

    if len(x) == 0 or len(y) == 0:
        return np.nan, np.nan, "", "empty sample"

    x_const = np.all(x == x[0])
    y_const = np.all(y == y[0])
    if x_const and y_const and x[0] == y[0]:
        return np.nan, 1.0, "", "both constant and equal"

    try:
        with warnings.catch_warnings():
            warnings.simplefilter("ignore")
            stat, p = mannwhitneyu(x, y, alternative="two-sided")
        expected = len(x) * len(y) / 2.0
        if stat > expected:
            direction = "A>B"
        elif stat < expected:
            direction = "A<B"
        else:
            direction = "A=B"
        return float(stat), float(p), direction, ""
    except Exception as e:  # pragma: no cover
        return np.nan, np.nan, "", f"error: {e}"


# --------------------------------------------------------------------------
# Per-SPL, per-metric comparison builder
# --------------------------------------------------------------------------
def build_comparisons_for_spl(
    spl_full_name: str,
    det_sheet: str,
    ms_sheet: str,
    det_col: str,
    ms_col: str,
) -> pd.DataFrame:
    """Run all nine pairwise comparisons for a given SPL and metric."""
    short = SPL_MAPPING[spl_full_name]
    det_df, ms_df = load_spl_data(spl_full_name, det_sheet, ms_sheet)

    values: dict[str, np.ndarray] = {}
    for a in ESGFX_APPROACHES + EFG_APPROACHES:
        values[a] = _values_deterministic(det_df, a, det_col)
    values[STOCHASTIC_LABEL] = _values_stochastic(ms_df, ms_col)

    pairs: list[tuple[str, str]] = []
    for lvl in ("L2", "L3", "L4"):
        pairs.append((f"ESG-Fx_{lvl}", f"EFG_{lvl}"))
    for lvl in ("L2", "L3", "L4"):
        pairs.append((f"ESG-Fx_{lvl}", STOCHASTIC_LABEL))
    for lvl in ("L2", "L3", "L4"):
        pairs.append((f"EFG_{lvl}", STOCHASTIC_LABEL))

    rows = []
    for a, b in pairs:
        xa, xb = values[a], values[b]
        U, p, direction, note = _mannwhitneyu_safe(xa, xb)
        rows.append({
            "SPL": short,
            "ApproachA": a,
            "ApproachB": b,
            "n_A": int(np.sum(~np.isnan(xa))),
            "n_B": int(np.sum(~np.isnan(xb))),
            "median_A": float(np.nanmedian(xa)) if len(xa) else np.nan,
            "median_B": float(np.nanmedian(xb)) if len(xb) else np.nan,
            "mean_A": float(np.nanmean(xa)) if len(xa) else np.nan,
            "mean_B": float(np.nanmean(xb)) if len(xb) else np.nan,
            "U_statistic": U,
            "p_value": p,
            "direction": direction,
            "note": note,
        })
    return pd.DataFrame(rows)


# --------------------------------------------------------------------------
# Per-operator pipeline
# --------------------------------------------------------------------------
def run_for_operator(operator: str) -> None:
    det_sheet, ms_sheet = OPERATOR_SHEETS[operator]
    output_file = OUTPUT_DIR / f"rq3_mannwhitneyu_{operator}.xlsx"
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

    print(f"\n=== Operator: {operator} ({det_sheet}) ===")
    print(f"Output file: {output_file}")
    print()

    with pd.ExcelWriter(output_file, engine="openpyxl") as writer:
        for det_col, ms_col, label in METRICS:
            print(f"[{label}] Running Mann-Whitney U tests...")
            frames = []
            for spl_full in SPL_MAPPING.keys():
                try:
                    frames.append(
                        build_comparisons_for_spl(spl_full, det_sheet, ms_sheet, det_col, ms_col)
                    )
                    print(f"  {spl_full:<26s} done")
                except (FileNotFoundError, ValueError) as e:
                    print(f"  {spl_full:<26s} SKIPPED ({e})")
            if frames:
                out = pd.concat(frames, ignore_index=True)
                out.to_excel(writer, sheet_name=label, index=False)
                print(f"  -> wrote sheet '{label}' ({len(out)} rows)")
            else:
                print(f"  -> no data for sheet '{label}'")
            print()

    print(f"Saved: {output_file}")


# --------------------------------------------------------------------------
# Main
# --------------------------------------------------------------------------
def main() -> None:
    parser = argparse.ArgumentParser(description="Pairwise Mann-Whitney U tests for RQ3.")
    parser.add_argument(
        "--operator",
        choices=["edge", "event", "both"],
        default="both",
        help="Which operator to run (default: both).",
    )
    args = parser.parse_args()

    print(f"Script location : {SCRIPT_DIR}")
    print(f"Files directory : {FILES_DIR}")
    print(f"Data directory  : {DATA_DIR}")

    operators = ["edge", "event"] if args.operator == "both" else [args.operator]
    for op in operators:
        run_for_operator(op)


if __name__ == "__main__":
    main()