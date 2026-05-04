#!/usr/bin/env python3
"""
rq1_06_run_stability.py

Step 6 of the RQ1 pipeline — run-to-run stability.

Reviewer armor for the question: "You report medians of 11 runs, but how
stable are those runs? What is the spread?"

For each (SPL, approach, level), computes dispersion statistics across the
11 runs for three key metrics:

  - Total Elapsed Time (ms)
  - Test Generation Peak Memory (MB)
  - Edge Coverage (%)

Output columns per (SPL, Approach, Level, Metric):
  - N_runs, median, IQR, min, max
  - mean, std, CV% (= std / mean * 100)
  - IQR_pct (= IQR / median * 100)

The RandomWalk approach is the primary stability concern (11 different seeds:
42..52 per product). ESG-Fx and EFG are deterministic per input, so their
variance across runs reflects measurement noise (JVM warm-up, OS scheduling).

Paths:
    Script: files/scripts/statistical_test_scripts/rq1_06_run_stability.py
    Data  : files/Cases/<SPL>/RQ1_<SPL>_perRun.xlsx  (produced by rq1_01)
    Out   : files/scripts/statistical_test_scripts/rq1_result/
            rq1_run_stability.xlsx

Usage:
    python rq1_06_run_stability.py
"""
from __future__ import annotations

import sys
from pathlib import Path

import numpy as np
import pandas as pd


SCRIPT_DIR = Path(__file__).resolve().parent
SCRIPTS_DIR = SCRIPT_DIR.parent
FILES_DIR = SCRIPTS_DIR.parent
DATA_DIR = FILES_DIR / "Cases"
OUTPUT_DIR = SCRIPT_DIR / "rq1_result"


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

# Full display names — added as 'SPLName' / 'ApproachLabel' columns alongside
# the short codes 'SPL' / 'Approach'.
SPL_FULL_NAME = {
    "SVM":  "Soda Vending Machine",
    "eM":   "eMail",
    "El":   "Elevator",
    "BAv2": "Bank Account",
    "SAS":  "Student Attendance System",
    "Te":   "Tesla Web Configurator",
    "Svia": "syngo.via",
    "HS":   "Hockerty Shirts Web Configurator",
}

APPROACH_LABEL = {
    "ESG-Fx":     "Model Once, Generate Any",
    "EFG":        "Structural Baseline",
    "RandomWalk": "Stochastic Baseline",
}

APPROACH_METRICS = {
    "EFG": {
        "TestGenTime_ms":        "GuitarGenTime(ms)",
        "TestGenPeakMemory_MB":  "TestGenPeakMemory(MB)",
        "EdgeCoverage_pct":      "EdgeCoverage(%)",
    },
    "ESG-Fx": {
        "TestGenTime_ms":        "TotalTestGenTime(ms)",
        "TestGenPeakMemory_MB":  "TestGenPeakMemory(MB)",
        "EdgeCoverage_pct":      "EdgeCoverage(%)",
    },
    "RandomWalk": {
        "TestGenTime_ms":        "TotalTestGenTime(ms)",
        "TestGenPeakMemory_MB":  "TestGenPeakMemory(MB)",
        "EdgeCoverage_pct":      "EdgeCoverage(%)",
    },
}


def get_approach(sheet_name):
    if sheet_name.startswith("ESG-Fx"):
        return "ESG-Fx"
    if sheet_name.startswith("EFG"):
        return "EFG"
    if sheet_name.startswith("Random"):
        return "RandomWalk"
    return None


def get_level(sheet_name):
    for lv in ["L1", "L2", "L3", "L4", "L0"]:
        if lv in sheet_name:
            return lv
    return None


def summarize_vector(vals):
    vals = np.asarray(vals, dtype=float)
    vals = vals[~np.isnan(vals)]
    if len(vals) == 0:
        return None
    q1, q3 = np.percentile(vals, [25, 75])
    median = float(np.median(vals))
    mean = float(np.mean(vals))
    std = float(np.std(vals, ddof=1)) if len(vals) > 1 else 0.0
    iqr = float(q3 - q1)
    cv_pct = (std / mean * 100.0) if mean != 0 else np.nan
    iqr_pct = (iqr / median * 100.0) if median != 0 else np.nan
    return {
        "N_runs": int(len(vals)),
        "median": median,
        "IQR": iqr,
        "min": float(np.min(vals)),
        "max": float(np.max(vals)),
        "mean": mean,
        "std": std,
        "CV_pct": cv_pct,
        "IQR_pct_of_median": iqr_pct,
    }


def main():
    print("=" * 70)
    print("rq1_06: Run-to-Run Stability Analysis")
    print("=" * 70)
    print(f"Data directory   : {DATA_DIR}")
    print(f"Output directory : {OUTPUT_DIR}")
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

    rows = []

    for spl_folder, spl_short in SPL_MAPPING.items():
        excel_path = DATA_DIR / spl_folder / f"RQ1_{spl_folder}_perRun.xlsx"
        if not excel_path.exists():
            print(f"  [SKIP] {spl_folder}: perRun Excel missing")
            continue

        xls = pd.ExcelFile(excel_path)
        for sheet in xls.sheet_names:
            approach = get_approach(sheet)
            level = get_level(sheet)
            if approach is None or level is None:
                continue

            df = pd.read_excel(xls, sheet_name=sheet)

            for metric_short, metric_col in APPROACH_METRICS[approach].items():
                if metric_col not in df.columns:
                    continue
                stats = summarize_vector(df[metric_col])
                if stats is None:
                    continue

                stats_row = {
                    "SPL": spl_short,
                    "SPLName": SPL_FULL_NAME.get(spl_short, spl_short),
                    "Approach": approach,
                    "ApproachLabel": APPROACH_LABEL.get(approach, approach),
                    "Level": level,
                    "Metric": metric_short,
                }
                stats_row.update(stats)
                rows.append(stats_row)

        print(f"  [OK]  {spl_folder}")

    if not rows:
        print("ERROR: no data loaded.")
        sys.exit(1)

    df_out = pd.DataFrame(rows)

    # Round numeric columns for readability
    num_cols = ["median", "IQR", "min", "max", "mean", "std", "CV_pct", "IQR_pct_of_median"]
    for c in num_cols:
        if c in df_out.columns:
            df_out[c] = df_out[c].round(3)

    output_file = OUTPUT_DIR / "rq1_run_stability.xlsx"
    print(f"\nWriting {output_file.name}...")
    with pd.ExcelWriter(output_file, engine="openpyxl") as writer:
        df_out.to_excel(writer, sheet_name="all_stability", index=False)

        # One sheet per metric for convenience
        for metric in ["TestGenTime_ms", "TestGenPeakMemory_MB", "EdgeCoverage_pct"]:
            sub = df_out[df_out["Metric"] == metric].copy()
            if not sub.empty:
                sub = sub.sort_values(by=["Approach", "Level", "SPL"]).reset_index(drop=True)
                sub.to_excel(writer, sheet_name=metric, index=False)

        # Summary: RandomWalk stability focus (reviewer's most likely concern)
        rw = df_out[df_out["Approach"] == "RandomWalk"].copy()
        if not rw.empty:
            rw = rw.sort_values(by=["Metric", "SPL"]).reset_index(drop=True)
            rw.to_excel(writer, sheet_name="randomwalk_focus", index=False)

    # Console summary: identify unstable cells (CV > 10% or IQR_pct > 10%)
    print("\n=== Unstable cells (CV > 10%) ===")
    unstable = df_out[(df_out["CV_pct"] > 10) & df_out["CV_pct"].notna()]
    if unstable.empty:
        print("  (none — all approaches stable across 11 runs)")
    else:
        for _, row in unstable.iterrows():
            print(f"  {row['Approach']:<11s} {row['SPL']:<6s} {row['Level']}  "
                  f"{row['Metric']:<24s} CV={row['CV_pct']:6.2f}%")

    print(f"\nSaved: {output_file}")
    print("\nrq1_06 DONE.")


if __name__ == "__main__":
    main()