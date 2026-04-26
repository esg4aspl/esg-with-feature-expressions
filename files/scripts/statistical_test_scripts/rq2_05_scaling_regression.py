#!/usr/bin/env python3
"""
rq2_05_scaling_regression.py

Step 5 of the RQ2 pipeline — scaling analysis.

For each approach (Model Once, Structural Baseline, Stochastic Baseline),
fits a log-log regression of Total Elapsed Time vs Processed Products
across the 8 SPLs. The slope of the fit is the empirical scaling
exponent — slope ~1 indicates linear scaling, slope > 1 super-linear.

Inputs per (SPL, approach):
  - x = total Processed Products across all 80 shards  (a single number
        per SPL × approach × level; we pick one representative level)
  - y = total Elapsed CPU Time across all 80 shards    (sum of shard
        medians; cumulative CPU time, not wall-clock)

Note on level choice:
  - For Model Once we use L=2 (same as manuscript main results)
  - For Structural we use L=2
  - For Stochastic we use L=0 (only option)

Output:
  - rq2_scaling_regression.xlsx  — slopes, R^2, per-SPL aggregates
  - plots/rq2_scaling_regression.pdf  — 3-panel scatter + fitted line

Paths (relative to this script):
    Script: files/scripts/statistical_test_scripts/rq2_05_scaling_regression.py
    Data  : files/Cases/<SPL>/RQ2_perShard_<SPL>.xlsx
    Out   : files/scripts/statistical_test_scripts/rq2_result/

Usage:
    python rq2_05_scaling_regression.py

Dependencies: pandas, numpy, scipy, matplotlib, openpyxl
"""
from __future__ import annotations

import sys
import warnings
from pathlib import Path

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
from scipy import stats

warnings.filterwarnings("ignore")


# ─── Paths ─────────────────────────────────────────────────────────────────
SCRIPT_DIR = Path(__file__).resolve().parent
SCRIPTS_DIR = SCRIPT_DIR.parent
FILES_DIR = SCRIPTS_DIR.parent
DATA_DIR = FILES_DIR / "Cases"
OUTPUT_DIR = SCRIPT_DIR / "rq2_result"
PLOTS_DIR = OUTPUT_DIR / "plots"


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

SPL_ORDER = ["SVM", "eM", "El", "BAv2", "SAS", "Te", "Svia", "HS"]
SPL_COLORS = {
    "SVM": "#1b9e77", "eM": "#d95f02", "El": "#7570b3",
    "BAv2": "#e7298a", "SAS": "#66a61e",
    "Te": "#e6ab02", "Svia": "#a6761d", "HS": "#666666",
}

# Representative level used for each approach
APPROACH_CONFIG = {
    "ESG-Fx":     {"sheet": "ESG-Fx_L2",   "label": "Model Once, Generate Any"},
    "EFG":        {"sheet": "EFG_L2",      "label": "Structural Baseline"},
    "RandomWalk": {"sheet": "RandomWalk_L0", "label": "Stochastic Baseline"},
}

TIME_COL = "Total Elapsed Time(ms)"
PRODUCTS_COL = "Processed Products"


def aggregate_spl_level(spl_folder, sheet_name):
    """
    For one (SPL, approach) cell:
      - load perShard data
      - take per-shard median across runs
      - sum across shards for total time
      - sum across shards for total processed products
    Returns (total_products, total_time_ms)  or (None, None).
    """
    excel_path = DATA_DIR / spl_folder / f"RQ2_perShard_{spl_folder}.xlsx"
    if not excel_path.exists():
        return None, None

    try:
        df = pd.read_excel(excel_path, sheet_name=sheet_name)
    except Exception:
        return None, None

    if df.empty or "Shard" not in df.columns:
        return None, None
    if TIME_COL not in df.columns or PRODUCTS_COL not in df.columns:
        return None, None

    # Per-shard median across runs
    per_shard = df.groupby("Shard").agg({
        TIME_COL: "median",
        PRODUCTS_COL: "median",
    }).reset_index()

    # Skip shards with 0 products
    per_shard = per_shard[per_shard[PRODUCTS_COL] > 0]
    if per_shard.empty:
        return None, None

    total_products = float(per_shard[PRODUCTS_COL].sum())
    total_time_ms = float(per_shard[TIME_COL].sum())
    return total_products, total_time_ms


def fit_loglog(xs, ys):
    """Fit y = a * x^b via log-log linear regression."""
    xs = np.asarray(xs, dtype=float)
    ys = np.asarray(ys, dtype=float)
    mask = (xs > 0) & (ys > 0) & ~np.isnan(xs) & ~np.isnan(ys)
    xs, ys = xs[mask], ys[mask]
    if len(xs) < 3:
        return {"N": int(len(xs)), "slope": np.nan, "intercept": np.nan,
                "R2": np.nan, "p_value": np.nan, "a_coef": np.nan}
    lx = np.log10(xs)
    ly = np.log10(ys)
    slope, intercept, r, p, se = stats.linregress(lx, ly)
    return {"N": int(len(xs)),
            "slope": float(slope),
            "intercept": float(intercept),
            "R2": float(r ** 2),
            "p_value": float(p),
            "a_coef": float(10 ** intercept)}


def main():
    print("=" * 70)
    print("rq2_05: Scaling Regression (log-log)")
    print("=" * 70)
    print(f"Data directory   : {DATA_DIR}")
    print(f"Output directory : {OUTPUT_DIR}")
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    PLOTS_DIR.mkdir(parents=True, exist_ok=True)

    # ── Gather aggregates per approach ──
    print("\nAggregating per (SPL, approach)...")
    data = {}  # approach -> list of (spl_short, total_products, total_time)
    for approach, cfg in APPROACH_CONFIG.items():
        data[approach] = []
        sheet = cfg["sheet"]
        for spl_folder, spl_short in SPL_MAPPING.items():
            products, time_ms = aggregate_spl_level(spl_folder, sheet)
            if products is None or time_ms is None:
                print(f"  [SKIP] {spl_short} {approach}: sheet {sheet} not available")
                continue
            data[approach].append((spl_short, products, time_ms))
            print(f"  {spl_short:6s} {approach:11s} "
                  f"products={products:>10.0f}  time_ms={time_ms:>15.0f}")

    # ── Fit regression per approach ──
    print("\nFitting log-log regression per approach...")
    fit_rows = []
    for approach, cfg in APPROACH_CONFIG.items():
        pts = data[approach]
        if len(pts) < 3:
            continue
        xs = [p for _, p, _ in pts]
        ys = [t for _, _, t in pts]
        fit = fit_loglog(xs, ys)
        fit_rows.append({
            "Approach": cfg["label"],
            "Level":    cfg["sheet"].split("_")[-1],
            "N_SPLs":   fit["N"],
            "Slope":    round(fit["slope"], 4) if not pd.isna(fit["slope"]) else np.nan,
            "Intercept_log10": round(fit["intercept"], 4) if not pd.isna(fit["intercept"]) else np.nan,
            "a_coef":   round(fit["a_coef"], 4) if not pd.isna(fit["a_coef"]) else np.nan,
            "R2":       round(fit["R2"], 4) if not pd.isna(fit["R2"]) else np.nan,
            "p_value":  fit["p_value"],
        })
        print(f"  {cfg['label']:30s}: slope={fit['slope']:.3f}  "
              f"R2={fit['R2']:.3f}  (N={fit['N']})")

    fit_df = pd.DataFrame(fit_rows)

    # ── Per-SPL aggregates table ──
    detail_rows = []
    for approach, cfg in APPROACH_CONFIG.items():
        for spl_short, products, time_ms in data[approach]:
            detail_rows.append({
                "SPL": spl_short,
                "Approach": cfg["label"],
                "Level": cfg["sheet"].split("_")[-1],
                "Total Processed Products": int(products),
                "Total Elapsed Time (ms)": round(time_ms, 2),
                "Total Elapsed Time (hours)": round(time_ms / 1000 / 3600, 3),
            })
    detail_df = pd.DataFrame(detail_rows)

    # ── Save Excel ──
    output_file = OUTPUT_DIR / "rq2_scaling_regression.xlsx"
    with pd.ExcelWriter(output_file, engine="openpyxl") as writer:
        fit_df.to_excel(writer, sheet_name="regression_summary", index=False)
        detail_df.to_excel(writer, sheet_name="per_spl_aggregates", index=False)
    print(f"\nSaved: {output_file.name}")

    # ── Generate PDF plot ──
    print("\nGenerating scaling regression PDF plot...")
    fig, axes = plt.subplots(1, 3, figsize=(18, 6), sharey=False)

    for ax, (approach, cfg) in zip(axes, APPROACH_CONFIG.items()):
        pts = data[approach]
        if not pts:
            ax.set_title(f"{cfg['label']}\n(no data)")
            continue

        for spl_short, x, y in pts:
            ax.scatter(x, y, s=90, c=SPL_COLORS[spl_short],
                       label=spl_short, edgecolors="white", linewidth=0.5)

        # Fit line
        xs = np.array([p for _, p, _ in pts])
        ys = np.array([t for _, _, t in pts])
        fit = fit_loglog(xs, ys)

        if not pd.isna(fit["slope"]):
            xfit = np.logspace(np.log10(min(xs)), np.log10(max(xs)), 100)
            yfit = fit["a_coef"] * xfit ** fit["slope"]
            ax.plot(xfit, yfit, "--", color="black", linewidth=1.2, alpha=0.7)
            ax.text(0.04, 0.95,
                    f"slope = {fit['slope']:.3f}\n$R^2$ = {fit['R2']:.3f}\n"
                    f"N = {fit['N']} SPLs",
                    transform=ax.transAxes, ha="left", va="top", fontsize=11,
                    bbox=dict(boxstyle="round", facecolor="white", alpha=0.9))

        ax.set_xscale("log")
        ax.set_yscale("log")
        ax.set_xlabel("Total Processed Products", fontsize=11)
        ax.set_ylabel("Total Elapsed CPU Time (ms)", fontsize=11)
        ax.set_title(f"{cfg['label']} ({cfg['sheet'].split('_')[-1]})", fontsize=12)
        ax.legend(fontsize=9, loc="lower right")
        ax.grid(True, which="both", alpha=0.3)

    plt.suptitle("RQ2: Scaling of End-to-End Pipeline Cost vs Industrial Scale "
                 "(log–log)", fontsize=13)
    plt.tight_layout()
    plot_file = PLOTS_DIR / "rq2_scaling_regression.pdf"
    fig.savefig(plot_file, format="pdf", bbox_inches="tight")
    plt.close(fig)
    print(f"Saved: {plot_file.name}")

    print("\nrq2_05 DONE.")


if __name__ == "__main__":
    main()