#!/usr/bin/env python3
"""
rq3_08_multiseed_stability.py

Step 8 of the RQ3 analysis pipeline.

Reports run-to-run stability of the RandomWalk baseline across the 10 seeds
recorded per product in the EdgeOmission_MultiSeed and EventOmission_MultiSeed
sheets, mirroring the stability methodology used for RQ1 and RQ2.

For every (SPL, ProductID, metric), the coefficient of variation
    CV% = 100 * std(values across seeds) / mean(values across seeds)
is computed across the 10 seeds. The (SPL, metric) cell is then summarised
by the median, IQR, and maximum of CV% across products. Cells are labelled
into bands matching the manuscript's terminology:

    very stable      -> median CV% < 5%
    stable           -> 5% <= median CV% < 10%
    notable variance -> median CV% >= 10%

Two tables are produced per operator (Edge / Event):
  Stability_<operator>_perProduct -- one row per (SPL, ProductID, metric)
                                     with CV% and the seed values
  Stability_<operator>_summary    -- one row per (SPL, metric) with the
                                     aggregate stability statistics
                                     (median CV%, IQR, max CV%, band)

Metrics analysed (per MultiSeed sheet):
    MutationScore(%)                                    (higher is better)
    MedianPercentageOfSuiteToDetect(%)                  (lower is better)
    PenalizedMedianPercentageOfSuiteToDetect(%)         (lower is better; primary)

Paths (relative to this script's location):
    Input : files/Cases/<SPL>/RQ3_<SPL>_perProduct_rawData.xlsx
    Output: files/scripts/statistical_test_scripts/rq3_result/
            rq3_multiseed_stability.xlsx

Notes
-----
- CV% is undefined when the mean is 0. We report it as 0.0 in that case
  (since std must also be 0: identical values give zero variation).
- Products with fewer than 2 valid seed observations are dropped from CV%
  computation for that metric.

Usage:
    python rq3_08_multiseed_stability.py

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

# Per operator, the multiseed sheet name and its metrics.
OPERATORS = {
    "edge":  ("EdgeOmission_MultiSeed",  [
        ("MutationScore(%)",                              True),
        ("MedianPercentageOfSuiteToDetect(%)",            False),
        ("PenalizedMedianPercentageOfSuiteToDetect(%)",   False),
    ]),
    "event": ("EventOmission_MultiSeed", [
        ("MutationScore(%)",                              True),
        ("MedianPercentageOfSuiteToDetect(%)",            False),
        ("PenalizedMedianPercentageOfSuiteToDetect(%)",   False),
    ]),
}

# Stability thresholds on median CV%
BAND_VERY_STABLE_BELOW = 5.0
BAND_STABLE_BELOW = 10.0


# --------------------------------------------------------------------------
# Loading helpers
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
        f"No raw data file for {spl_full_name} in {folder}."
    )


def _load_multiseed_sheet(spl: str, sheet_name: str) -> pd.DataFrame | None:
    path = _find_raw_file(spl)
    xls = pd.ExcelFile(path)
    if sheet_name not in xls.sheet_names:
        return None
    return pd.read_excel(xls, sheet_name=sheet_name)


# --------------------------------------------------------------------------
# CV% helpers
# --------------------------------------------------------------------------
def _cv_percent(values: np.ndarray) -> float:
    """CV% = 100 * std / mean. Returns 0 if mean == 0 and std == 0; NaN if undefined."""
    v = values[~np.isnan(values)]
    if len(v) < 2:
        return float("nan")
    mean = float(np.mean(v))
    std = float(np.std(v, ddof=1))  # sample std
    if mean == 0.0:
        return 0.0 if std == 0.0 else float("inf")
    return 100.0 * std / mean


def _iqr(values: np.ndarray) -> float:
    v = values[~np.isnan(values)]
    if len(v) < 2:
        return float("nan")
    q1, q3 = np.percentile(v, [25, 75])
    return float(q3 - q1)


def _band_label(median_cv: float) -> str:
    if pd.isna(median_cv):
        return "n/a"
    if median_cv < BAND_VERY_STABLE_BELOW:
        return "very_stable"
    if median_cv < BAND_STABLE_BELOW:
        return "stable"
    return "notable_variance"


# --------------------------------------------------------------------------
# Per-operator stability pipeline
# --------------------------------------------------------------------------
def compute_stability_for_operator(operator: str):
    """
    Returns (per_product_df, summary_df) for one operator.
    """
    multiseed_sheet, metrics = OPERATORS[operator]
    perprod_rows = []
    summary_rows = []

    print(f"\n=== Operator: {operator} ({multiseed_sheet}) ===")

    for spl_full in SPL_MAPPING.keys():
        try:
            df = _load_multiseed_sheet(spl_full, multiseed_sheet)
        except FileNotFoundError as e:
            print(f"  [SKIP] {spl_full}: {e}")
            continue
        if df is None:
            print(f"  [SKIP] {spl_full}: '{multiseed_sheet}' not present")
            continue
        short = SPL_MAPPING[spl_full]

        for metric_col, higher_is_better in metrics:
            if metric_col not in df.columns:
                continue

            # Per-product CV% across seeds.
            per_product = (
                df[["ProductID", "Seed", metric_col]]
                .dropna(subset=[metric_col])
                .pivot_table(index="ProductID", columns="Seed",
                             values=metric_col, aggfunc="median")
            )
            if per_product.empty:
                continue

            # Compute CV% row-wise (per product).
            cvs = per_product.apply(
                lambda row: _cv_percent(row.to_numpy(dtype=float)), axis=1
            )
            valid = cvs.dropna()

            for product_id, cv in valid.items():
                row = {
                    "SPL": short,
                    "ProductID": product_id,
                    "Metric": metric_col,
                    "Direction": "higher_is_better" if higher_is_better else "lower_is_better",
                    "n_seeds": int((~per_product.loc[product_id].isna()).sum()),
                    "mean_across_seeds": float(np.nanmean(per_product.loc[product_id].to_numpy(dtype=float))),
                    "std_across_seeds":  float(np.nanstd(per_product.loc[product_id].to_numpy(dtype=float), ddof=1)),
                    "min_across_seeds":  float(np.nanmin(per_product.loc[product_id].to_numpy(dtype=float))),
                    "max_across_seeds":  float(np.nanmax(per_product.loc[product_id].to_numpy(dtype=float))),
                    "CV_percent":        float(cv),
                }
                perprod_rows.append(row)

            # Cell-level summary (one row per SPL x metric).
            cv_arr = valid.to_numpy(dtype=float)
            cv_finite = cv_arr[np.isfinite(cv_arr)]
            if len(cv_finite) > 0:
                median_cv = float(np.median(cv_finite))
                p25, p75 = np.percentile(cv_finite, [25, 75])
                max_cv = float(np.max(cv_finite))
            else:
                median_cv = p25 = p75 = max_cv = float("nan")

            summary_rows.append({
                "SPL": short,
                "Metric": metric_col,
                "Direction": "higher_is_better" if higher_is_better else "lower_is_better",
                "n_products": len(valid),
                "n_inf_CV": int(np.sum(~np.isfinite(cv_arr))) if len(cv_arr) else 0,
                "median_CV_percent": median_cv,
                "P25_CV_percent": float(p25) if not pd.isna(p25) else np.nan,
                "P75_CV_percent": float(p75) if not pd.isna(p75) else np.nan,
                "IQR_CV_percent": float(p75 - p25) if not pd.isna(p25) else np.nan,
                "max_CV_percent": max_cv,
                "stability_band": _band_label(median_cv),
            })

        print(f"  [OK] {spl_full}: {len(metrics)} metrics processed")

    return pd.DataFrame(perprod_rows), pd.DataFrame(summary_rows)


# --------------------------------------------------------------------------
# Excel writing
# --------------------------------------------------------------------------
def _autosize_columns(worksheet, df: pd.DataFrame) -> None:
    for idx, col_name in enumerate(df.columns, start=1):
        worksheet.column_dimensions[get_column_letter(idx)].width = (
            len(str(col_name)) + 2
        )


# --------------------------------------------------------------------------
# Main
# --------------------------------------------------------------------------
def main() -> None:
    parser = argparse.ArgumentParser(
        description="Run-to-run stability across RandomWalk seeds for RQ3."
    )
    parser.add_argument(
        "--operator",
        choices=["edge", "event", "both"],
        default="both",
        help="Which operator to process (default: both).",
    )
    args = parser.parse_args()

    print(f"Script location : {SCRIPT_DIR}")
    print(f"Data directory  : {DATA_DIR}")
    print(f"Output directory: {OUTPUT_DIR}")
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

    output_file = OUTPUT_DIR / "rq3_multiseed_stability.xlsx"
    print(f"Output file     : {output_file}")

    operators = ["edge", "event"] if args.operator == "both" else [args.operator]

    sheets: dict[str, pd.DataFrame] = {}
    for op in operators:
        per_prod, summary = compute_stability_for_operator(op)
        if not per_prod.empty:
            sheets[f"Stability_{op}_perProduct"] = per_prod
        if not summary.empty:
            sheets[f"Stability_{op}_summary"] = summary

    if not sheets:
        print("\nNo output written.")
        return

    print(f"\nWriting {output_file}...")
    with pd.ExcelWriter(output_file, engine="openpyxl") as writer:
        for sheet_name, df in sheets.items():
            df.to_excel(writer, sheet_name=sheet_name, index=False)
            _autosize_columns(writer.sheets[sheet_name], df)
            print(f"  sheet '{sheet_name}': {len(df)} rows")
    print(f"Saved: {output_file}")

    # Console summary -- band counts per operator
    print("\n--- Stability band counts (median CV% per cell) ---")
    for op in operators:
        sn = f"Stability_{op}_summary"
        if sn not in sheets:
            continue
        df = sheets[sn]
        bands = df["stability_band"].value_counts().to_dict()
        total = len(df)
        very = bands.get("very_stable", 0)
        stab = bands.get("stable", 0)
        notab = bands.get("notable_variance", 0)
        max_cv = float(df["median_CV_percent"].max()) if not df.empty else float("nan")
        print(f"  {op:<6s} cells={total:>3d}  very_stable={very}  stable={stab}  "
              f"notable={notab}  worst_median_CV={max_cv:.2f}%")


if __name__ == "__main__":
    main()