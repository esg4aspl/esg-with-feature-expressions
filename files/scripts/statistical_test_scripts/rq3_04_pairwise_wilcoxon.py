#!/usr/bin/env python3
"""
rq3_04_pairwise_wilcoxon.py

Step 4 of the RQ3 analysis pipeline.

For each SPL, performs nine pairwise comparisons at the product level using
the paired Wilcoxon signed-rank test (matching the methodology used in RQ1
and RQ2). Each pair is computed for two metrics:
  (1) MutationScore(%)                       (higher is better)
  (2) PenalizedPercentageOfSuiteToDetect(%)  (lower is better; primary cost)

Pairing key is ProductID: every comparison is between two values measured on
the SAME product. This is the appropriate test design because:
  - The same test suite is generated for the same product configuration.
  - The mutant set and ground-truth coverage are shared across approaches.
  - Per-product paired observations are NOT independent across approaches,
    so unpaired Mann-Whitney would be the wrong test.

Comparison pairs per (SPL, level L) for L in {L2, L3, L4}:
    ESG-Fx_L  vs  EFG_L
    ESG-Fx_L  vs  RandomWalk
    EFG_L     vs  RandomWalk

For deterministic approaches (ESG-Fx, EFG), each product contributes one
value (read from the EdgeOmission / EventOmission sheet). For RandomWalk
the per-product value is the median across the 10 seeds recorded in the
*_MultiSeed sheet.

Vargha--Delaney A12 is computed directly from the paired vectors:
    A12(x, y) = (#{x > y} + 0.5 * #{x == y}) / (n_x * n_y)

Magnitudes follow Vargha--Delaney (2000): negligible / small / medium / large.

Note on scope: tests are restricted to L in {L2, L3, L4} because that is the
range where ESG-Fx and EFG share comparable algorithmic regimes. ESG-Fx_L1
is structurally different (event coverage, no L-sequence transformation)
and is therefore excluded from approach-vs-approach pairings, in line with
RQ1/RQ2.

Paths (relative to this script's location):
    Script :  files/scripts/statistical_test_scripts/rq3_04_pairwise_wilcoxon.py
    Data   :  files/Cases/<SPL>/RQ3_<SPL>_perProduct_rawData.xlsx
    Output :  files/scripts/statistical_test_scripts/rq3_result/
              rq3_pairwise_wilcoxon_<operator>.xlsx

Usage:
    python rq3_04_pairwise_wilcoxon.py --operator edge      # primary
    python rq3_04_pairwise_wilcoxon.py --operator event     # supporting
    python rq3_04_pairwise_wilcoxon.py --operator both      # default

Dependencies: pandas, numpy, scipy, openpyxl
"""
from __future__ import annotations

import argparse
import warnings
from pathlib import Path

import numpy as np
import pandas as pd
from scipy.stats import wilcoxon


# --------------------------------------------------------------------------
# Paths
# --------------------------------------------------------------------------
SCRIPT_DIR = Path(__file__).resolve().parent
SCRIPTS_DIR = SCRIPT_DIR.parent
FILES_DIR = SCRIPTS_DIR.parent
DATA_DIR = FILES_DIR / "Cases"
OUTPUT_DIR = SCRIPT_DIR / "rq3_result"


# --------------------------------------------------------------------------
# Configuration
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

# Each metric: (column in deterministic sheet, column in *_MultiSeed sheet,
#               short label, "higher_is_better" flag)
METRICS = [
    ("MutationScore(%)",                     "MutationScore(%)",                       "MutationScore",  True),
    ("PenalizedPercentageOfSuiteToDetect(%)", "PenalizedMedianPercentageOfSuiteToDetect(%)", "DetectionCost_Penalized", False),
]

# --operator value -> (deterministic sheet, multiseed sheet)
OPERATOR_SHEETS = {
    "edge":  ("EdgeOmission",  "EdgeOmission_MultiSeed"),
    "event": ("EventOmission", "EventOmission_MultiSeed"),
}


# --------------------------------------------------------------------------
# Statistics helpers (kept inline so the script is self-contained)
# --------------------------------------------------------------------------
def vargha_delaney_a12(x: np.ndarray, y: np.ndarray) -> float:
    """A12 = P(X > Y) + 0.5 * P(X == Y). Distribution-free, paired-data safe."""
    x = np.asarray(x, dtype=float)
    y = np.asarray(y, dtype=float)
    n_x, n_y = len(x), len(y)
    if n_x == 0 or n_y == 0:
        return float("nan")
    diff = x[:, None] - y[None, :]
    gt = int(np.sum(diff > 0))
    eq = int(np.sum(diff == 0))
    return (gt + 0.5 * eq) / (n_x * n_y)


def a12_magnitude(a12: float) -> str:
    """Vargha--Delaney (2000) thresholds on |0.5 - A12|."""
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
# Data loading
# --------------------------------------------------------------------------
def _find_raw_file(spl_full_name: str) -> Path:
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
        f"Expected: RQ3_{spl_full_name}_perProduct_rawData.xlsx"
    )


def load_spl_data(
    spl_full_name: str, det_sheet: str, ms_sheet: str
) -> tuple[pd.DataFrame, pd.DataFrame]:
    path = _find_raw_file(spl_full_name)
    xls = pd.ExcelFile(path)
    if det_sheet not in xls.sheet_names:
        raise ValueError(f"{spl_full_name}: sheet '{det_sheet}' missing in {path.name}")
    if ms_sheet not in xls.sheet_names:
        raise ValueError(f"{spl_full_name}: sheet '{ms_sheet}' missing in {path.name}")
    det = pd.read_excel(xls, sheet_name=det_sheet)
    ms = pd.read_excel(xls, sheet_name=ms_sheet)
    return det, ms


def _series_deterministic(
    det_df: pd.DataFrame, approach: str, metric_col: str
) -> pd.Series:
    """Per-ProductID metric value for a deterministic approach (ESG-Fx or EFG)."""
    sub = det_df[det_df["TestingApproach"] == approach][["ProductID", metric_col]]
    return sub.set_index("ProductID")[metric_col].astype(float)


def _series_stochastic(ms_df: pd.DataFrame, multiseed_metric_col: str) -> pd.Series:
    """Per-ProductID median across the 10 seeds for the stochastic baseline."""
    agg = ms_df.groupby("ProductID")[multiseed_metric_col].median()
    return agg.astype(float)


# --------------------------------------------------------------------------
# Paired Wilcoxon test wrapper
# --------------------------------------------------------------------------
def _wilcoxon_safe(
    x: np.ndarray, y: np.ndarray
) -> tuple[float, float, str]:
    """
    Run paired Wilcoxon signed-rank (two-sided). Returns (W, p, note).

    Edge cases:
      - n < 6                          -> (NaN, NaN, "n<6")
      - all paired diffs == 0          -> (0.0, 1.0, "all ties")
      - scipy raises ValueError        -> (NaN, NaN, "wilcoxon error")
    """
    x = np.asarray(x, dtype=float)
    y = np.asarray(y, dtype=float)
    if len(x) != len(y):
        return np.nan, np.nan, "length mismatch"
    if len(x) < 6:
        return np.nan, np.nan, "n<6"
    diffs = x - y
    if np.allclose(diffs, 0):
        return 0.0, 1.0, "all ties"
    try:
        with warnings.catch_warnings():
            warnings.simplefilter("ignore")
            res = wilcoxon(x, y, zero_method="wilcox", alternative="two-sided")
        return float(res.statistic), float(res.pvalue), ""
    except ValueError as e:  # pragma: no cover
        return np.nan, np.nan, f"wilcoxon error: {e}"


# --------------------------------------------------------------------------
# Per-SPL, per-metric comparison builder
# --------------------------------------------------------------------------
def _build_pair_list() -> list[tuple[str, str]]:
    pairs: list[tuple[str, str]] = []
    for lvl in ("L2", "L3", "L4"):
        pairs.append((f"ESG-Fx_{lvl}", f"EFG_{lvl}"))
    for lvl in ("L2", "L3", "L4"):
        pairs.append((f"ESG-Fx_{lvl}", STOCHASTIC_LABEL))
    for lvl in ("L2", "L3", "L4"):
        pairs.append((f"EFG_{lvl}", STOCHASTIC_LABEL))
    return pairs


def build_comparisons_for_spl(
    spl_full_name: str,
    det_sheet: str,
    ms_sheet: str,
    det_col: str,
    ms_col: str,
    metric_label: str,
) -> pd.DataFrame:
    """Run all nine paired Wilcoxon comparisons for one (SPL, metric)."""
    short = SPL_MAPPING[spl_full_name]
    det_df, ms_df = load_spl_data(spl_full_name, det_sheet, ms_sheet)

    series: dict[str, pd.Series] = {}
    for a in ESGFX_APPROACHES + EFG_APPROACHES:
        series[a] = _series_deterministic(det_df, a, det_col)
    series[STOCHASTIC_LABEL] = _series_stochastic(ms_df, ms_col)

    rows = []
    for a, b in _build_pair_list():
        sa, sb = series[a], series[b]
        # Inner join on ProductID; drop pairs with NaN on either side.
        joined = pd.concat([sa.rename("A"), sb.rename("B")], axis=1, join="inner").dropna()
        xa = joined["A"].to_numpy(dtype=float)
        xb = joined["B"].to_numpy(dtype=float)

        W, p, note = _wilcoxon_safe(xa, xb)
        a12 = vargha_delaney_a12(xa, xb)

        rows.append({
            "SPL": short,
            "Metric": metric_label,
            "ApproachA": a,
            "ApproachB": b,
            "n_pairs": len(xa),
            "median_A": float(np.nanmedian(xa)) if len(xa) else np.nan,
            "median_B": float(np.nanmedian(xb)) if len(xb) else np.nan,
            "median_delta_AminusB": float(np.nanmedian(xa - xb)) if len(xa) else np.nan,
            "W": W,
            "p_value": p,
            "A12": a12,
            "A12_magnitude": a12_magnitude(a12),
            "note": note,
        })
    return pd.DataFrame(rows)


# --------------------------------------------------------------------------
# Per-operator pipeline
# --------------------------------------------------------------------------
def run_for_operator(operator: str) -> None:
    det_sheet, ms_sheet = OPERATOR_SHEETS[operator]
    output_file = OUTPUT_DIR / f"rq3_pairwise_wilcoxon_{operator}.xlsx"
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

    print(f"\n=== Operator: {operator} ({det_sheet}) ===")
    print(f"Output file: {output_file}")
    print()

    with pd.ExcelWriter(output_file, engine="openpyxl") as writer:
        for det_col, ms_col, label, _ in METRICS:
            print(f"[{label}] paired Wilcoxon ({det_col})")
            frames = []
            for spl_full in SPL_MAPPING.keys():
                try:
                    frames.append(
                        build_comparisons_for_spl(
                            spl_full, det_sheet, ms_sheet, det_col, ms_col, label
                        )
                    )
                    print(f"  {spl_full:<26s} done")
                except (FileNotFoundError, ValueError) as e:
                    print(f"  {spl_full:<26s} SKIPPED ({e})")
            if frames:
                out = pd.concat(frames, ignore_index=True)
                out.to_excel(writer, sheet_name=label, index=False)
                _autosize_columns(writer.sheets[label], out)
                print(f"  -> wrote sheet '{label}' ({len(out)} rows)")
            else:
                print(f"  -> no data for sheet '{label}'")
            print()

    print(f"Saved: {output_file}")


def _autosize_columns(worksheet, df: pd.DataFrame) -> None:
    from openpyxl.utils import get_column_letter
    for idx, col_name in enumerate(df.columns, start=1):
        worksheet.column_dimensions[get_column_letter(idx)].width = (
            len(str(col_name)) + 2
        )


# --------------------------------------------------------------------------
# Main
# --------------------------------------------------------------------------
def main() -> None:
    parser = argparse.ArgumentParser(
        description="Pairwise paired Wilcoxon signed-rank tests for RQ3."
    )
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