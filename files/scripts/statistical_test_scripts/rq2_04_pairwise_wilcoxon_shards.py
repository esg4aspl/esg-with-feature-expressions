#!/usr/bin/env python3
"""
rq2_04_pairwise_wilcoxon_shards.py

Step 4 of the RQ2 pipeline — MAIN statistical test.

Paired Wilcoxon signed-rank test comparing the three approaches on
the same 80 shards (identical product partitions across approaches):

  Pairings:
    - Model Once, Generate Any  vs  Structural Baseline
    - Model Once, Generate Any  vs  Stochastic Baseline
    - Structural Baseline       vs  Stochastic Baseline

  Metrics (shard-level cumulative CPU time):
    - Total Elapsed Time(ms)            — end-to-end pipeline cost
    - Test Generation Time(ms)          — approach-specific cost
    - Test Generation Peak Memory(MB)   — memory footprint
    - Edge Coverage(%)                  — effectiveness at industrial scale

  Level pairings:
    - ESG-Fx L2 vs EFG L2
    - ESG-Fx L3 vs EFG L3
    - ESG-Fx L4 vs EFG L4
    - ESG-Fx L2 vs RandomWalk L0   (configuration-level match)
    - EFG L2    vs RandomWalk L0

For each (SPL x level-pairing x metric), one paired Wilcoxon test is
applied on the 80 shard medians (median across available runs per shard).

Effect size: Vargha-Delaney A12.
Multiple testing: Benjamini-Hochberg per metric across the full
(SPL x level-pairing) grid.

Paths (relative to this script):
    Script: files/scripts/statistical_test_scripts/rq2_04_pairwise_wilcoxon_shards.py
    Data  : files/Cases/<SPL>/RQ2_perShard_<SPL>.xlsx
    Out   : files/scripts/statistical_test_scripts/rq2_result/
            rq2_pairwise_wilcoxon.xlsx

Manuscript labels for approaches in output:
    ESG-Fx      -> "Model Once, Generate Any"
    EFG         -> "Structural Baseline"
    RandomWalk  -> "Stochastic Baseline"

Usage:
    python rq2_04_pairwise_wilcoxon_shards.py

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
OUTPUT_DIR = SCRIPT_DIR / "rq2_result"


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

APPROACH_LABEL = {
    "ESG-Fx":     "Model Once, Generate Any",
    "EFG":        "Structural Baseline",
    "RandomWalk": "Stochastic Baseline",
}

# Per-approach column names for the four comparison metrics
APPROACH_METRICS = {
    "ESG-Fx": {
        "time_total":    "Total Elapsed Time(ms)",
        "time_testgen":  "Test Generation Time(ms)",
        "memory":        "Test Generation Peak Memory(MB)",
        "coverage":      "Edge Coverage(%)",
    },
    "EFG": {
        "time_total":    "Total Elapsed Time(ms)",
        "time_testgen":  "Test Generation Time(ms)",
        "memory":        "Test Generation Peak Memory(MB)",
        "coverage":      "Edge Coverage(%)",
    },
    "RandomWalk": {
        "time_total":    "Total Elapsed Time(ms)",
        "time_testgen":  "Test Generation Time(ms)",
        "memory":        "Test Generation Peak Memory(MB)",
        "coverage":      "Edge Coverage(%)",
    },
}

METRIC_KEYS = ["time_total", "time_testgen", "memory", "coverage"]
METRIC_LABEL = {
    "time_total":   "TotalElapsedTime_ms",
    "time_testgen": "TestGenTime_ms",
    "memory":       "TestGenPeakMemory_MB",
    "coverage":     "EdgeCoverage_pct",
}

# For time/memory: lower is better. For coverage: higher is better.
LOWER_IS_BETTER = {
    "time_total":   True,
    "time_testgen": True,
    "memory":       True,
    "coverage":     False,
}

# Level pairings: (approach_A, level_A, approach_B, level_B, label)
LEVEL_PAIRINGS = [
    ("ESG-Fx", "L2", "EFG",        "L2", "ESGFx_L2_vs_EFG_L2"),
    ("ESG-Fx", "L3", "EFG",        "L3", "ESGFx_L3_vs_EFG_L3"),
    ("ESG-Fx", "L4", "EFG",        "L4", "ESGFx_L4_vs_EFG_L4"),
    ("ESG-Fx", "L2", "RandomWalk", "L0", "ESGFx_L2_vs_RW_L0"),
    ("EFG",    "L2", "RandomWalk", "L0", "EFG_L2_vs_RW_L0"),
]


# ─── Helpers ───────────────────────────────────────────────────────────────
def vargha_delaney_a12(x, y):
    """A12: probability that a random observation from x exceeds one from y."""
    x = np.asarray(x, dtype=float)
    y = np.asarray(y, dtype=float)
    if len(x) == 0 or len(y) == 0:
        return np.nan
    diff = x[:, None] - y[None, :]
    gt = int(np.sum(diff > 0))
    eq = int(np.sum(diff == 0))
    return (gt + 0.5 * eq) / (len(x) * len(y))


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
def load_shard_medians(spl_folder, sheet_name):
    """
    Load perShard data, compute per-shard median across runs for a single
    (SPL, approach, level) cell.
    Returns dict: {shard_id: {metric_label: median_value}}  or None.
    """
    excel_path = DATA_DIR / spl_folder / f"RQ2_perShard_{spl_folder}.xlsx"
    if not excel_path.exists():
        return None

    try:
        df = pd.read_excel(excel_path, sheet_name=sheet_name)
    except Exception:
        return None

    if df.empty or "Shard" not in df.columns:
        return None

    # Determine approach from sheet name
    if sheet_name.startswith("ESG-Fx"):
        approach = "ESG-Fx"
    elif sheet_name.startswith("EFG"):
        approach = "EFG"
    elif sheet_name.startswith("Random"):
        approach = "RandomWalk"
    else:
        return None

    metric_cols = APPROACH_METRICS[approach]

    # Only keep shards with processed products > 0 (real data)
    prod_col = None
    for cand in ["Processed Products", " Processed Products"]:
        if cand in df.columns:
            prod_col = cand
            break

    if prod_col is not None:
        df = df[df[prod_col] > 0].copy()

    if df.empty:
        return None

    result = {}
    for shard, group in df.groupby("Shard"):
        shard_data = {}
        for key in METRIC_KEYS:
            col = metric_cols.get(key)
            if col and col in group.columns:
                shard_data[METRIC_LABEL[key]] = float(group[col].median())
            else:
                shard_data[METRIC_LABEL[key]] = np.nan
        result[int(shard)] = shard_data

    return result


def paired_vectors(da, db, metric_label):
    """Align two shard->metric dicts on common shards."""
    common = sorted(set(da) & set(db))
    x = np.array([da[s].get(metric_label, np.nan) for s in common], dtype=float)
    y = np.array([db[s].get(metric_label, np.nan) for s in common], dtype=float)
    return x, y, common


# ─── Main comparison ───────────────────────────────────────────────────────
def compare_one_cell(spl_folder, spl_short, pairing):
    """
    One (SPL x level-pairing) cell: 4 metric comparisons.
    """
    app_a, lvl_a, app_b, lvl_b, label = pairing

    sheet_a = f"{app_a}_{lvl_a}"
    sheet_b = f"{app_b}_{lvl_b}"

    da = load_shard_medians(spl_folder, sheet_a)
    db = load_shard_medians(spl_folder, sheet_b)

    out_rows = []
    if da is None or db is None:
        return out_rows

    for key in METRIC_KEYS:
        metric_label = METRIC_LABEL[key]
        x, y, common = paired_vectors(da, db, metric_label)

        mask = ~(np.isnan(x) | np.isnan(y))
        x_valid = x[mask]
        y_valid = y[mask]
        n = len(x_valid)

        row = {
            "SPL": spl_short,
            "Comparison": label,
            "ApproachA": APPROACH_LABEL[app_a],
            "LevelA": lvl_a,
            "ApproachB": APPROACH_LABEL[app_b],
            "LevelB": lvl_b,
            "Metric": metric_label,
            "N_shards": n,
            "median_A": float(np.median(x_valid)) if n else np.nan,
            "median_B": float(np.median(y_valid)) if n else np.nan,
            "median_delta_A_minus_B": float(np.median(x_valid - y_valid)) if n else np.nan,
            "W": np.nan, "p": np.nan,
            "A12_A_vs_B": np.nan, "magnitude": "n/a",
            "winner": "n/a",
            "note": "",
        }

        W, p, note = paired_wilcoxon(x_valid, y_valid)
        row["W"] = W
        row["p"] = p
        row["note"] = note

        a12 = vargha_delaney_a12(x_valid, y_valid)
        row["A12_A_vs_B"] = a12
        row["magnitude"] = a12_magnitude(a12)

        # Winner depends on the metric
        if not pd.isna(a12):
            if LOWER_IS_BETTER[key]:
                # A has lower values -> A12 < 0.5 means A wins
                if a12 < 0.5:
                    row["winner"] = APPROACH_LABEL[app_a]
                elif a12 > 0.5:
                    row["winner"] = APPROACH_LABEL[app_b]
                else:
                    row["winner"] = "tie"
            else:
                # Higher is better -> A12 > 0.5 means A wins
                if a12 > 0.5:
                    row["winner"] = APPROACH_LABEL[app_a]
                elif a12 < 0.5:
                    row["winner"] = APPROACH_LABEL[app_b]
                else:
                    row["winner"] = "tie"

        out_rows.append(row)

    return out_rows


def main():
    print("=" * 70)
    print("rq2_04: Pairwise Wilcoxon (80-shard paired comparison)")
    print("=" * 70)
    print(f"Data directory   : {DATA_DIR}")
    print(f"Output directory : {OUTPUT_DIR}")
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

    all_rows = []
    for spl_folder, spl_short in SPL_MAPPING.items():
        print(f"\n[{spl_short}] {spl_folder}")
        spl_rows = []
        for pairing in LEVEL_PAIRINGS:
            rows = compare_one_cell(spl_folder, spl_short, pairing)
            spl_rows.extend(rows)
        if not spl_rows:
            print(f"  [SKIP] no data loaded")
            continue
        n_cells = len(spl_rows)
        sig_p05 = sum(1 for r in spl_rows if not pd.isna(r["p"]) and r["p"] < 0.05)
        large_eff = sum(1 for r in spl_rows if r["magnitude"] == "large")
        print(f"  {n_cells} tests; {sig_p05} with p<0.05 (pre-BH); {large_eff} large effects")
        all_rows.extend(spl_rows)

    if not all_rows:
        print("\nERROR: no data loaded.")
        sys.exit(1)

    df = pd.DataFrame(all_rows)

    # BH correction per metric
    print("\nApplying Benjamini-Hochberg correction per metric...")
    df["p_BH"] = np.nan
    for metric in df["Metric"].unique():
        idx = df["Metric"] == metric
        mask_p = df.loc[idx, "p"].notna()
        if mask_p.any():
            adjusted = benjamini_hochberg(df.loc[idx & mask_p, "p"].values)
            df.loc[idx & mask_p, "p_BH"] = adjusted
    df["significant_BH"] = df["p_BH"] < 0.05

    col_order = [
        "SPL", "Comparison", "ApproachA", "LevelA", "ApproachB", "LevelB",
        "Metric", "N_shards", "median_A", "median_B", "median_delta_A_minus_B",
        "W", "p", "p_BH", "significant_BH",
        "A12_A_vs_B", "magnitude", "winner", "note",
    ]
    df = df[col_order]

    # Output
    output_file = OUTPUT_DIR / "rq2_pairwise_wilcoxon.xlsx"
    print(f"\nWriting {output_file.name}...")
    with pd.ExcelWriter(output_file, engine="openpyxl") as writer:
        df.to_excel(writer, sheet_name="all_comparisons", index=False)
        for metric_label in ["TotalElapsedTime_ms", "TestGenTime_ms",
                             "TestGenPeakMemory_MB", "EdgeCoverage_pct"]:
            sub = df[df["Metric"] == metric_label]
            if not sub.empty:
                sub.to_excel(writer, sheet_name=metric_label, index=False)

    # Console summary
    print("\n=== Summary ===")
    for metric in ["TotalElapsedTime_ms", "TestGenTime_ms",
                   "TestGenPeakMemory_MB", "EdgeCoverage_pct"]:
        sub = df[df["Metric"] == metric]
        if sub.empty:
            continue
        n = len(sub)
        sig = int(sub["significant_BH"].sum())
        large = int((sub["magnitude"] == "large").sum())
        print(f"  [{metric:24s}] tests={n}  BH-sig={sig}  large-eff={large}")

    print(f"\nSaved: {output_file}")
    print("\nrq2_04 DONE.")


if __name__ == "__main__":
    main()