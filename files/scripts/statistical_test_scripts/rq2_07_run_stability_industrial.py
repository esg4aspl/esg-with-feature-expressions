#!/usr/bin/env python3
"""
rq2_07_run_stability_industrial.py

Step 7 of the RQ2 pipeline — run-to-run stability for industrial SPLs.

Reviewer armor: "You ran only 3 repetitions for the industrial SPLs
(Tesla, syngo.via, Hockerty Shirts) instead of 11. Is the variance
across runs acceptable?"

For each (SPL x approach x level x shard), computes dispersion stats
across the available runs:
  - Coefficient of Variation (CV%) = 100 * std / mean
  - Relative IQR (IQR% of median) = 100 * IQR / median

Aggregates:
  - Per-SPL summary: median CV% across all shards (one row per
    SPL × approach × level)
  - Per-shard detail

Interpretation:
  - CV < 5%  : very stable, 3 runs easily sufficient
  - CV 5-15% : stable, 3 runs acceptable
  - CV > 15% : notable variance, more runs preferred

Paths:
    Script: files/scripts/statistical_test_scripts/rq2_07_run_stability_industrial.py
    Data  : files/Cases/<SPL>/RQ2_perShard_<SPL>.xlsx
    Out   : files/scripts/statistical_test_scripts/rq2_result/
            rq2_run_stability.xlsx

Usage:
    python rq2_07_run_stability_industrial.py

Dependencies: pandas, numpy, openpyxl
"""
from __future__ import annotations

import sys
from pathlib import Path

import numpy as np
import pandas as pd


# ─── Paths ─────────────────────────────────────────────────────────────────
SCRIPT_DIR = Path(__file__).resolve().parent
SCRIPTS_DIR = SCRIPT_DIR.parent
FILES_DIR = SCRIPTS_DIR.parent
DATA_DIR = FILES_DIR / "Cases"
OUTPUT_DIR = SCRIPT_DIR / "rq2_result"


# ─── Configuration ─────────────────────────────────────────────────────────
# All 8 SPLs analyzed — manuscript can cite small/medium as "CV < X%
# baseline established from 11 runs" and industrial as "3 runs within
# same dispersion range".
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

LARGE_SPLS = {"Tesla", "syngovia", "HockertyShirts"}

APPROACH_LABEL = {
    "ESG-Fx":     "Model Once, Generate Any",
    "EFG":        "Structural Baseline",
    "RandomWalk": "Stochastic Baseline",
}

# Metrics for stability analysis
METRIC_COLS = [
    "Total Elapsed Time(ms)",
    "Test Generation Time(ms)",
    "Test Generation Peak Memory(MB)",
    "Edge Coverage(%)",
]


def approach_from_sheet(sheet):
    if sheet.startswith("ESG-Fx"):
        return "ESG-Fx"
    if sheet.startswith("EFG"):
        return "EFG"
    if sheet.startswith("Random"):
        return "RandomWalk"
    return None


def level_from_sheet(sheet):
    for lv in ["L0", "L1", "L2", "L3", "L4"]:
        if sheet.endswith(f"_{lv}"):
            return lv
    return ""


def compute_shard_stability(df_sheet, metric_col):
    """
    For one (SPL, approach, level) sheet and one metric, compute
    per-shard CV% and IQR_pct_of_median across available runs.
    Returns list of dicts (one per shard).
    """
    if metric_col not in df_sheet.columns or "Shard" not in df_sheet.columns:
        return []

    # Only include shards with processed products > 0
    prod_col = None
    for cand in ["Processed Products", " Processed Products"]:
        if cand in df_sheet.columns:
            prod_col = cand
            break

    rows = []
    for shard, g in df_sheet.groupby("Shard"):
        if prod_col is not None and g[prod_col].median() == 0:
            continue

        vals = g[metric_col].dropna().values
        n = len(vals)
        if n < 2:
            continue

        mean = float(np.mean(vals))
        std = float(np.std(vals, ddof=1))
        median = float(np.median(vals))
        q1, q3 = np.percentile(vals, [25, 75])
        iqr = float(q3 - q1)

        cv_pct = (std / mean * 100.0) if mean != 0 else np.nan
        iqr_pct = (iqr / median * 100.0) if median != 0 else np.nan

        rows.append({
            "Shard": int(shard),
            "N_runs": n,
            "mean": round(mean, 4),
            "median": round(median, 4),
            "std": round(std, 4),
            "IQR": round(iqr, 4),
            "CV_pct": round(cv_pct, 3) if not np.isnan(cv_pct) else np.nan,
            "IQR_pct_of_median": round(iqr_pct, 3) if not np.isnan(iqr_pct) else np.nan,
        })

    return rows


def main():
    print("=" * 70)
    print("rq2_07: Run-to-Run Stability Analysis")
    print("=" * 70)
    print(f"Data directory   : {DATA_DIR}")
    print(f"Output directory : {OUTPUT_DIR}")
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

    detail_rows = []
    summary_rows = []

    for spl_folder, spl_short in SPL_MAPPING.items():
        excel_path = DATA_DIR / spl_folder / f"RQ2_perShard_{spl_folder}.xlsx"
        if not excel_path.exists():
            print(f"  [SKIP] {spl_folder}: perShard Excel missing")
            continue

        is_large = spl_folder in LARGE_SPLS
        expected_runs = 3 if is_large else 11

        print(f"\n[{spl_short}] {spl_folder}  (expected runs: {expected_runs})")
        xls = pd.ExcelFile(excel_path)
        for sheet in xls.sheet_names:
            approach = approach_from_sheet(sheet)
            level = level_from_sheet(sheet)
            if approach is None:
                continue

            df_sheet = pd.read_excel(xls, sheet_name=sheet)

            for metric_col in METRIC_COLS:
                if metric_col not in df_sheet.columns:
                    continue

                shard_rows = compute_shard_stability(df_sheet, metric_col)
                if not shard_rows:
                    continue

                for r in shard_rows:
                    detail_rows.append({
                        "SPL": spl_short,
                        "Scale": "Large" if is_large else "Small/Medium",
                        "Approach": APPROACH_LABEL[approach],
                        "Level": level,
                        "Metric": metric_col,
                        **r,
                    })

                # Cell summary
                cvs = [r["CV_pct"] for r in shard_rows
                       if r["CV_pct"] is not None and not np.isnan(r["CV_pct"])]
                iqrs = [r["IQR_pct_of_median"] for r in shard_rows
                        if r["IQR_pct_of_median"] is not None
                        and not np.isnan(r["IQR_pct_of_median"])]

                summary_rows.append({
                    "SPL": spl_short,
                    "Scale": "Large" if is_large else "Small/Medium",
                    "Approach": APPROACH_LABEL[approach],
                    "Level": level,
                    "Metric": metric_col,
                    "N_shards": len(shard_rows),
                    "Expected_runs": expected_runs,
                    "Median_CV_pct": round(float(np.median(cvs)), 3) if cvs else np.nan,
                    "Max_CV_pct": round(float(np.max(cvs)), 3) if cvs else np.nan,
                    "Median_IQR_pct": round(float(np.median(iqrs)), 3) if iqrs else np.nan,
                    "Stability": (
                        "very stable" if cvs and np.median(cvs) < 5
                        else "stable" if cvs and np.median(cvs) < 15
                        else "notable variance"
                    ),
                })
        print(f"  processed {len([1 for r in summary_rows if r['SPL'] == spl_short])} cells")

    if not detail_rows:
        print("\nERROR: no data loaded.")
        sys.exit(1)

    detail_df = pd.DataFrame(detail_rows)
    summary_df = pd.DataFrame(summary_rows)

    # Reorder columns
    summary_df = summary_df[[
        "SPL", "Scale", "Approach", "Level", "Metric",
        "N_shards", "Expected_runs",
        "Median_CV_pct", "Max_CV_pct", "Median_IQR_pct", "Stability",
    ]]

    # Save Excel
    output_file = OUTPUT_DIR / "rq2_run_stability.xlsx"
    with pd.ExcelWriter(output_file, engine="openpyxl") as writer:
        summary_df.to_excel(writer, sheet_name="summary", index=False)

        # Separate sheet for industrial SPLs (reviewer focus)
        large_summary = summary_df[summary_df["Scale"] == "Large"].copy()
        if not large_summary.empty:
            large_summary.to_excel(writer, sheet_name="industrial_focus", index=False)

        # Per-metric sheets from detail
        for metric_col in METRIC_COLS:
            sub = detail_df[detail_df["Metric"] == metric_col]
            if not sub.empty:
                # Sanitize sheet name
                sheet_name = metric_col.replace("(ms)", "_ms") \
                                        .replace("(MB)", "_MB") \
                                        .replace("(%)", "_pct") \
                                        .replace(" ", "_")[:31]
                sub.to_excel(writer, sheet_name=sheet_name, index=False)

    print(f"\nSaved: {output_file.name}")

    # Console summary — industrial focus (reviewer question)
    print("\n=== INDUSTRIAL SPL stability (reviewer focus) ===")
    large_summary = summary_df[summary_df["Scale"] == "Large"].copy()
    if not large_summary.empty:
        for _, row in large_summary.iterrows():
            print(f"  {row['SPL']:5s} {row['Approach']:30s} {row['Level']:4s} "
                  f"{row['Metric']:30s} median_CV={row['Median_CV_pct']:6.2f}%  "
                  f"[{row['Stability']}]")
    else:
        print("  (no industrial SPL data yet — waiting for Hockerty rerun)")

    # Overall cells flagged as "notable variance"
    flagged = summary_df[summary_df["Stability"] == "notable variance"]
    if not flagged.empty:
        print(f"\n=== Cells flagged for notable variance (CV >= 15%) ===")
        for _, row in flagged.iterrows():
            print(f"  {row['SPL']:5s} {row['Approach']:30s} {row['Level']:4s} "
                  f"{row['Metric']:28s} median_CV={row['Median_CV_pct']:6.2f}%")

    print("\nrq2_07 DONE.")


if __name__ == "__main__":
    main()