#!/usr/bin/env python3
"""
rq1_05_pairwise_wilcoxon.py

Step 5 of the RQ1 pipeline — pairwise approach comparison.

Applies the paired Wilcoxon signed-rank test to compare approaches
(ESG-Fx, EFG, RandomWalk) on the 11-run measurements for the three
main efficiency metrics per SPL x level:

  - Total Elapsed Time (ms)
  - Test Generation Peak Memory (MB)
  - Edge Coverage (%)

Paired structure: each of the 11 Run IDs is a paired observation across
approaches (same seed, same infrastructure). For each (SPL, level) cell,
the following comparisons run:
    ESG-Fx vs EFG                  (paired at L = 2, 3, 4)
    ESG-Fx vs RandomWalk           (ESG-Fx at L = 2; RandomWalk has no L)
    EFG    vs RandomWalk           (EFG    at L = 2; RandomWalk has no L)

Effect size: Vargha-Delaney A12 (distribution-free).
Multiple testing: Benjamini-Hochberg per metric across all cells.

Paths (relative to this script):
    Script: files/scripts/statistical_test_scripts/rq1_05_pairwise_wilcoxon.py
    Data  : files/Cases/<SPL>/RQ1_<SPL>_perRun.xlsx  (produced by rq1_01)
    Out   : files/scripts/statistical_test_scripts/rq1_result/
            rq1_pairwise_wilcoxon.xlsx

Note on RandomWalk: RandomWalk targets edge coverage by random traversal
and is not parameterized by an L level. When comparing it to a
deterministic approach at L = k, the same RandomWalk run is paired with
each level. The 'Level' column in the output uses 'L2_vs_RW' to mark
these cross-approach rows.

Usage (from any directory):
    python rq1_05_pairwise_wilcoxon.py

Dependencies: pandas, numpy, scipy, openpyxl
"""
from __future__ import annotations

import sys
from pathlib import Path

import numpy as np
import pandas as pd
from scipy.stats import wilcoxon


# ─── Paths ─────────────────────────────────────────────────────────────────
SCRIPT_DIR = Path(__file__).resolve().parent
SCRIPTS_DIR = SCRIPT_DIR.parent
FILES_DIR = SCRIPTS_DIR.parent
DATA_DIR = FILES_DIR / "Cases"
OUTPUT_DIR = SCRIPT_DIR / "rq1_result"


# ─── Configuration ─────────────────────────────────────────────────────────
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

# Full display names — added as 'SPLName' / 'ApproachLabel' columns in the
# output Excel for human readability. Short codes ('SPL', 'Approach',
# 'ApproachA', 'ApproachB') are retained for stable filtering.
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

# Metric columns differ per approach. (Time, Memory, Coverage)
# T_gen scope-symmetric: ESG-Fx and RandomWalk both use TotalTestGenTime(ms)
# (alg + model load + recording); EFG uses GuitarGenTime(ms) which is already
# the disk-to-disk total inside its Java 8 sub-process.
APPROACH_METRICS = {
    "EFG": {
        "time":     "GuitarGenTime(ms)",
        "memory":   "TestGenPeakMemory(MB)",
        "coverage": "EdgeCoverage(%)",
    },
    "ESG-Fx": {
        "time":     "TotalTestGenTime(ms)",
        "memory":   "TestGenPeakMemory(MB)",
        "coverage": "EdgeCoverage(%)",
    },
    "RandomWalk": {
        "time":     "TotalTestGenTime(ms)",
        "memory":   "TestGenPeakMemory(MB)",
        "coverage": "EdgeCoverage(%)",
    },
}

METRIC_KEYS = ["time", "memory", "coverage"]
METRIC_SHORT = {"time": "TestGenTime_ms", "memory": "TestGenPeakMemory_MB",
                "coverage": "EdgeCoverage_pct"}

# Comparison levels — only where both approaches produce data
# Paired on Run ID within (SPL, Level).
DET_LEVELS = ["L2", "L3", "L4"]   # ESG-Fx vs EFG at each of these


# ─── Helpers ───────────────────────────────────────────────────────────────
def vargha_delaney_a12(x, y):
    x = np.asarray(x, dtype=float)
    y = np.asarray(y, dtype=float)
    n_x, n_y = len(x), len(y)
    if n_x == 0 or n_y == 0:
        return np.nan
    diff = x[:, None] - y[None, :]
    gt = int(np.sum(diff > 0))
    eq = int(np.sum(diff == 0))
    return (gt + 0.5 * eq) / (n_x * n_y)


def a12_magnitude(a12):
    if pd.isna(a12):
        return "n/a"
    d = abs(a12 - 0.5)
    if d < 0.06:
        return "negligible"
    if d < 0.14:
        return "small"
    if d < 0.21:
        return "medium"
    return "large"


def benjamini_hochberg(pvals):
    p = np.asarray(pvals, dtype=float)
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


def paired_wilcoxon(x, y):
    """
    Return (W, p, note). x and y are aligned pairs (same length).
    """
    x = np.asarray(x, dtype=float)
    y = np.asarray(y, dtype=float)
    mask = ~(np.isnan(x) | np.isnan(y))
    x, y = x[mask], y[mask]

    if len(x) < 6:
        return np.nan, np.nan, f"n={len(x)} < 6"

    diffs = x - y
    if np.allclose(diffs, 0):
        return 0.0, 1.0, "all pairs identical"

    try:
        res = wilcoxon(x, y, zero_method="wilcox", alternative="two-sided")
        return float(res.statistic), float(res.pvalue), ""
    except ValueError as e:
        return np.nan, np.nan, f"error: {e}"


# ─── Data loading ──────────────────────────────────────────────────────────
def load_perrun_sheets(spl_folder):
    excel_path = DATA_DIR / spl_folder / f"RQ1_{spl_folder}_perRun.xlsx"
    if not excel_path.exists():
        return None
    xls = pd.ExcelFile(excel_path)
    return {s: pd.read_excel(xls, sheet_name=s) for s in xls.sheet_names}


def extract_run_vector(df, metric_col):
    """
    Return a dict Run ID -> metric value. Drops rows where Run ID is NaN.
    """
    if df is None or metric_col not in df.columns or 'Run ID' not in df.columns:
        return {}
    sub = df[['Run ID', metric_col]].dropna()
    sub['Run ID'] = sub['Run ID'].astype(int, errors='ignore')
    return dict(zip(sub['Run ID'], sub[metric_col]))


def paired_vectors(da, db):
    """
    Given two dicts {Run ID: value} from two approaches, return two aligned
    arrays for the common Run IDs.
    """
    common = sorted(set(da) & set(db))
    x = np.array([da[r] for r in common], dtype=float)
    y = np.array([db[r] for r in common], dtype=float)
    return x, y, common


# ─── Main ──────────────────────────────────────────────────────────────────
def compare_cell(perrun_data, spl_short, level, approach_a, approach_b):
    """
    One (SPL, level, approach_a vs approach_b) comparison across 3 metrics.
    Returns a list of row dicts (one per metric).
    """
    sheet_a = f"{approach_a}_{level}"
    sheet_b = f"{approach_b}_{level}"
    df_a = perrun_data.get(sheet_a)
    df_b = perrun_data.get(sheet_b)

    out_rows = []
    for mk in METRIC_KEYS:
        metric_col_a = APPROACH_METRICS[approach_a][mk]
        metric_col_b = APPROACH_METRICS[approach_b][mk]
        da = extract_run_vector(df_a, metric_col_a)
        db = extract_run_vector(df_b, metric_col_b)
        xa, yb, common = paired_vectors(da, db)

        row = {
            "SPL": spl_short,
            "SPLName": SPL_FULL_NAME.get(spl_short, spl_short),
            "Level": level,
            "Metric": METRIC_SHORT[mk],
            "ApproachA": approach_a,
            "ApproachALabel": APPROACH_LABEL.get(approach_a, approach_a),
            "ApproachB": approach_b,
            "ApproachBLabel": APPROACH_LABEL.get(approach_b, approach_b),
            "N_pairs": len(common),
            "median_A": float(np.median(xa)) if len(xa) else np.nan,
            "median_B": float(np.median(yb)) if len(yb) else np.nan,
            "delta_A_minus_B": float(np.median(xa - yb)) if len(common) else np.nan,
            "W": np.nan, "p": np.nan,
            "A12_A_vs_B": np.nan, "magnitude": "n/a",
            "winner_lower": "n/a",
            "note": "",
        }

        W, p, note = paired_wilcoxon(xa, yb)
        row["W"] = W
        row["p"] = p
        row["note"] = note

        a12 = vargha_delaney_a12(xa, yb)
        row["A12_A_vs_B"] = a12
        row["magnitude"] = a12_magnitude(a12)

        # "Winner (lower is better)" is informative for time/memory;
        # for coverage, lower is worse.
        if not pd.isna(a12):
            if a12 < 0.5:
                row["winner_lower"] = approach_a
            elif a12 > 0.5:
                row["winner_lower"] = approach_b
            else:
                row["winner_lower"] = "tie"

        out_rows.append(row)

    return out_rows


def main():
    print("=" * 70)
    print("rq1_05: Pairwise Wilcoxon (11-run paired comparison)")
    print("=" * 70)
    print(f"Data directory   : {DATA_DIR}")
    print(f"Output directory : {OUTPUT_DIR}")
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

    # Load all SPLs once
    print("\nLoading per-Run Excel files...")
    all_data = {}
    for spl_folder, spl_short in SPL_MAPPING.items():
        data = load_perrun_sheets(spl_folder)
        if data is None:
            print(f"  [SKIP] {spl_folder}: perRun Excel missing")
            continue
        all_data[spl_short] = (spl_folder, data)
        print(f"  [OK]   {spl_folder}: {len(data)} sheets")

    if not all_data:
        print("ERROR: no data loaded. Run rq1_01 first.")
        sys.exit(1)

    # Run all comparisons
    print("\nRunning paired comparisons...")
    all_rows = []

    # (1) ESG-Fx vs EFG at each L2, L3, L4
    for spl_short, (_, data) in all_data.items():
        for level in DET_LEVELS:
            all_rows.extend(compare_cell(data, spl_short, level, "ESG-Fx", "EFG"))

    # (2) ESG-Fx (at L=2) vs RandomWalk (at L=0) — mismatched levels, tagged separately
    #     We treat this as a configuration-level comparison: each approach runs
    #     in its intended mode.
    for spl_short, (_, data) in all_data.items():
        esgfx_l2 = data.get("ESG-Fx_L2")
        rw_l0 = data.get("RandomWalk_L0")
        for mk in METRIC_KEYS:
            if esgfx_l2 is None or rw_l0 is None:
                continue
            da = extract_run_vector(esgfx_l2, APPROACH_METRICS["ESG-Fx"][mk])
            db = extract_run_vector(rw_l0, APPROACH_METRICS["RandomWalk"][mk])
            xa, yb, common = paired_vectors(da, db)
            W, p, note = paired_wilcoxon(xa, yb)
            a12 = vargha_delaney_a12(xa, yb)
            all_rows.append({
                "SPL": spl_short,
                "SPLName": SPL_FULL_NAME.get(spl_short, spl_short),
                "Level": "L2_vs_RW",
                "Metric": METRIC_SHORT[mk],
                "ApproachA": "ESG-Fx",
                "ApproachALabel": APPROACH_LABEL["ESG-Fx"],
                "ApproachB": "RandomWalk",
                "ApproachBLabel": APPROACH_LABEL["RandomWalk"],
                "N_pairs": len(common),
                "median_A": float(np.median(xa)) if len(xa) else np.nan,
                "median_B": float(np.median(yb)) if len(yb) else np.nan,
                "delta_A_minus_B": float(np.median(xa - yb)) if len(common) else np.nan,
                "W": W, "p": p,
                "A12_A_vs_B": a12, "magnitude": a12_magnitude(a12),
                "winner_lower": (
                    "ESG-Fx" if (not pd.isna(a12) and a12 < 0.5)
                    else "RandomWalk" if (not pd.isna(a12) and a12 > 0.5)
                    else ("tie" if not pd.isna(a12) else "n/a")
                ),
                "note": note,
            })

    # (3) EFG (at L=2) vs RandomWalk (at L=0)
    for spl_short, (_, data) in all_data.items():
        efg_l2 = data.get("EFG_L2")
        rw_l0 = data.get("RandomWalk_L0")
        for mk in METRIC_KEYS:
            if efg_l2 is None or rw_l0 is None:
                continue
            da = extract_run_vector(efg_l2, APPROACH_METRICS["EFG"][mk])
            db = extract_run_vector(rw_l0, APPROACH_METRICS["RandomWalk"][mk])
            xa, yb, common = paired_vectors(da, db)
            W, p, note = paired_wilcoxon(xa, yb)
            a12 = vargha_delaney_a12(xa, yb)
            all_rows.append({
                "SPL": spl_short,
                "SPLName": SPL_FULL_NAME.get(spl_short, spl_short),
                "Level": "L2_vs_RW",
                "Metric": METRIC_SHORT[mk],
                "ApproachA": "EFG",
                "ApproachALabel": APPROACH_LABEL["EFG"],
                "ApproachB": "RandomWalk",
                "ApproachBLabel": APPROACH_LABEL["RandomWalk"],
                "N_pairs": len(common),
                "median_A": float(np.median(xa)) if len(xa) else np.nan,
                "median_B": float(np.median(yb)) if len(yb) else np.nan,
                "delta_A_minus_B": float(np.median(xa - yb)) if len(common) else np.nan,
                "W": W, "p": p,
                "A12_A_vs_B": a12, "magnitude": a12_magnitude(a12),
                "winner_lower": (
                    "EFG" if (not pd.isna(a12) and a12 < 0.5)
                    else "RandomWalk" if (not pd.isna(a12) and a12 > 0.5)
                    else ("tie" if not pd.isna(a12) else "n/a")
                ),
                "note": note,
            })

    all_df = pd.DataFrame(all_rows)

    # BH correction per metric
    print("\nApplying Benjamini-Hochberg per metric...")
    all_df["p_BH"] = np.nan
    for metric in all_df["Metric"].unique():
        idx = all_df["Metric"] == metric
        mask = all_df.loc[idx, "p"].notna()
        if mask.any():
            adjusted = benjamini_hochberg(all_df.loc[idx & mask, "p"].values)
            all_df.loc[idx & mask, "p_BH"] = adjusted
    all_df["significant_BH"] = all_df["p_BH"] < 0.05

    # Column order
    col_order = [
        "SPL", "SPLName", "Level", "Metric",
        "ApproachA", "ApproachALabel", "ApproachB", "ApproachBLabel",
        "N_pairs", "median_A", "median_B", "delta_A_minus_B",
        "W", "p", "p_BH", "significant_BH",
        "A12_A_vs_B", "magnitude", "winner_lower", "note",
    ]
    all_df = all_df[col_order]

    # Split per-metric into sheets for readability
    output_file = OUTPUT_DIR / "rq1_pairwise_wilcoxon.xlsx"
    print(f"\nWriting {output_file.name}...")
    with pd.ExcelWriter(output_file, engine="openpyxl") as writer:
        all_df.to_excel(writer, sheet_name="all_comparisons", index=False)
        for metric in ["TestGenTime_ms", "TestGenPeakMemory_MB", "EdgeCoverage_pct"]:
            sub = all_df[all_df["Metric"] == metric]
            if not sub.empty:
                sub.to_excel(writer, sheet_name=metric, index=False)

    # Console summary
    print("\n=== Summary ===")
    for metric in ["TestGenTime_ms", "TestGenPeakMemory_MB", "EdgeCoverage_pct"]:
        sub = all_df[all_df["Metric"] == metric]
        n = len(sub)
        sig = int(sub["significant_BH"].sum())
        large = int((sub["magnitude"] == "large").sum())
        print(f"  [{metric:24s}] tests={n}  sig(BH<0.05)={sig}  large-effect={large}")

    print(f"\nSaved: {output_file}")
    print("\nrq1_05 DONE.")


if __name__ == "__main__":
    main()