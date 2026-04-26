#!/usr/bin/env python3
"""
rq2_06_time_breakdown.py

Step 6 of the RQ2 pipeline — end-to-end time breakdown and SAT share.

Decomposes the total elapsed time for each (SPL x approach) into its
component phases:

  - SAT Time              : configuration-space solving
  - Product Gen Time      : product instantiation from the feature model
  - (Transformation Time) : ESG-Fx only
  - Test Generation Time  : the core approach-specific cost
  - Coverage / Execution Time
  - Other / overhead

KEY ANALYSIS:  "Own Cost" = Total - SAT - Product Gen
  - Measures how much overhead the approach itself adds on top of the
    shared SAT / product-enumeration infrastructure
  - Reviewer argument: "On large SPLs, SAT dominates; our contribution
    is avoiding the per-product overhead that the Structural Baseline
    adds on top of SAT"

Outputs:
  - rq2_time_breakdown.xlsx  — detailed tables
  - plots/rq2_time_breakdown.pdf  — stacked-bar chart (one bar per
                                    SPL x approach x level)

Paths:
    Script: files/scripts/statistical_test_scripts/rq2_06_time_breakdown.py
    Data  : files/Cases/<SPL>/RQ2_perShard_<SPL>.xlsx
    Out   : files/scripts/statistical_test_scripts/rq2_result/

Usage:
    python rq2_06_time_breakdown.py

Dependencies: pandas, numpy, matplotlib, openpyxl
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

# Level selection per approach for the breakdown display
# (L=2 is the manuscript main column; L1 is thesis-only, L=0 for RW)
APPROACH_CONFIG = {
    "ESG-Fx_L2":     "Model Once, Generate Any (L=2)",
    "EFG_L2":        "Structural Baseline (L=2)",
    "RandomWalk_L0": "Stochastic Baseline",
}

# Phase columns (may not all be present per approach)
PHASE_COLS = {
    "SAT Time(ms)":                    "SAT",
    "Product Gen Time(ms)":            "Product Gen",
    "Transformation Time(ms)":         "Transformation (Model Once)",
    "EFG Transformation Time(ms)":     "Transformation (Structural)",
    "Test Generation Time(ms)":        "Test Generation",
    "Coverage Analysis Time(ms)":      "Coverage Analysis",
    "Event Coverage Analysis Time(ms)": "Coverage Analysis (Event)",
    "Edge Coverage Analysis Time(ms)": "Coverage Analysis (Edge)",
    "Parse Time(ms)":                  "Parse (Structural)",
    "Test Execution Time(ms)":         "Test Execution",
}

# For stacked-bar: coarser 5-phase breakdown
COARSE_PHASES = ["SAT", "Product Gen", "Transformation", "Test Generation", "Other"]
PHASE_COLORS = {
    "SAT":             "#d95f02",
    "Product Gen":     "#1b9e77",
    "Transformation":  "#7570b3",
    "Test Generation": "#e7298a",
    "Other":           "#a6761d",
}


def aggregate_spl_sheet(spl_folder, sheet_name):
    """
    Compute shard-summed totals (median across runs per shard, then sum)
    for one (SPL, approach, level) cell.
    Returns dict with per-phase totals and total_elapsed.
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

    # Sum per-shard medians for relevant phase columns
    result = {"SPL": SPL_MAPPING.get(spl_folder, spl_folder),
              "Approach_Level": sheet_name}

    # Identify products-per-shard column
    prod_col = None
    for cand in ["Processed Products", " Processed Products"]:
        if cand in df.columns:
            prod_col = cand
            break

    # Start per-shard medians
    per_shard = df.groupby("Shard").median(numeric_only=True).reset_index()

    # Exclude empty shards
    if prod_col and prod_col in per_shard.columns:
        per_shard = per_shard[per_shard[prod_col] > 0]
    if per_shard.empty:
        return None

    # Total elapsed (reference)
    result["Total Elapsed Time(ms)"] = float(per_shard["Total Elapsed Time(ms)"].sum()) \
        if "Total Elapsed Time(ms)" in per_shard.columns else np.nan

    # Sum each phase column present
    for col in PHASE_COLS:
        if col in per_shard.columns:
            result[col] = float(per_shard[col].sum())
        else:
            result[col] = np.nan

    # Processed products
    if prod_col and prod_col in per_shard.columns:
        result["Total Processed Products"] = int(per_shard[prod_col].sum())
    else:
        result["Total Processed Products"] = np.nan

    return result


def build_coarse_phases(row, approach):
    """
    Map the fine-grained phase columns to the 5-phase coarse breakdown
    for stacked-bar plotting.
    """
    sat = row.get("SAT Time(ms)", 0.0) or 0.0
    prod = row.get("Product Gen Time(ms)", 0.0) or 0.0

    # Transformation: different column per approach
    if approach == "ESG-Fx":
        trans = row.get("Transformation Time(ms)", 0.0) or 0.0
    elif approach == "EFG":
        trans = row.get("EFG Transformation Time(ms)", 0.0) or 0.0
    else:  # RandomWalk has no transformation
        trans = 0.0

    test_gen = row.get("Test Generation Time(ms)", 0.0) or 0.0

    # "Other" = total - (sat + prod + trans + test_gen)
    total = row.get("Total Elapsed Time(ms)", 0.0) or 0.0
    accounted = sat + prod + trans + test_gen
    other = max(0.0, total - accounted)

    return {
        "SAT":             sat,
        "Product Gen":     prod,
        "Transformation":  trans,
        "Test Generation": test_gen,
        "Other":           other,
        "Total":           total,
    }


def main():
    print("=" * 70)
    print("rq2_06: Time Breakdown and SAT Share Analysis")
    print("=" * 70)
    print(f"Data directory   : {DATA_DIR}")
    print(f"Output directory : {OUTPUT_DIR}")
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    PLOTS_DIR.mkdir(parents=True, exist_ok=True)

    # ── Aggregate across all (SPL, approach, level) cells ──
    all_rows = []
    for spl_folder, spl_short in SPL_MAPPING.items():
        for sheet in APPROACH_CONFIG:
            row = aggregate_spl_sheet(spl_folder, sheet)
            if row is None:
                print(f"  [SKIP] {spl_short} {sheet}")
                continue
            all_rows.append(row)

    if not all_rows:
        print("\nERROR: no data loaded.")
        sys.exit(1)

    # ── Derive coarse-phase breakdown and SAT share ──
    breakdown_rows = []
    for row in all_rows:
        sheet = row["Approach_Level"]
        if sheet.startswith("ESG-Fx"):
            approach = "ESG-Fx"
        elif sheet.startswith("EFG"):
            approach = "EFG"
        else:
            approach = "RandomWalk"

        phases = build_coarse_phases(row, approach)
        total = phases["Total"]
        if total <= 0:
            continue

        breakdown = {
            "SPL": row["SPL"],
            "Approach_Level": sheet,
            "Approach_Label": APPROACH_CONFIG[sheet],
            "Total Elapsed Time (ms)": round(total, 2),
            "Total Elapsed Time (hours)": round(total / 1000 / 3600, 3),
            "SAT (ms)": round(phases["SAT"], 2),
            "SAT (% of total)": round(100.0 * phases["SAT"] / total, 2),
            "Product Gen (ms)": round(phases["Product Gen"], 2),
            "Product Gen (% of total)": round(100.0 * phases["Product Gen"] / total, 2),
            "Transformation (ms)": round(phases["Transformation"], 2),
            "Transformation (% of total)": round(100.0 * phases["Transformation"] / total, 2),
            "Test Generation (ms)": round(phases["Test Generation"], 2),
            "Test Generation (% of total)": round(100.0 * phases["Test Generation"] / total, 2),
            "Other (ms)": round(phases["Other"], 2),
            "Other (% of total)": round(100.0 * phases["Other"] / total, 2),
            "Own Cost (Total - SAT - Product Gen) (ms)":
                round(total - phases["SAT"] - phases["Product Gen"], 2),
            "Own Cost (% of total)":
                round(100.0 * (total - phases["SAT"] - phases["Product Gen"]) / total, 2),
        }
        breakdown_rows.append(breakdown)

    breakdown_df = pd.DataFrame(breakdown_rows)

    # ── Save Excel ──
    output_file = OUTPUT_DIR / "rq2_time_breakdown.xlsx"
    with pd.ExcelWriter(output_file, engine="openpyxl") as writer:
        breakdown_df.to_excel(writer, sheet_name="breakdown", index=False)

        # Separate sheet: SAT share comparison across approaches
        sat_pivot = breakdown_df.pivot_table(
            index="SPL", columns="Approach_Label",
            values="SAT (% of total)", aggfunc="first")
        sat_pivot = sat_pivot.reindex([s for s in SPL_ORDER
                                       if s in sat_pivot.index])
        sat_pivot.to_excel(writer, sheet_name="sat_share_pivot")

        # Own-cost pivot
        own_pivot = breakdown_df.pivot_table(
            index="SPL", columns="Approach_Label",
            values="Own Cost (% of total)", aggfunc="first")
        own_pivot = own_pivot.reindex([s for s in SPL_ORDER
                                       if s in own_pivot.index])
        own_pivot.to_excel(writer, sheet_name="own_cost_share_pivot")

    print(f"\nSaved: {output_file.name}")

    # ── Stacked-bar PDF plot ──
    print("\nGenerating stacked-bar PDF...")

    # Order: SPL (manuscript order) x approach
    plot_df = breakdown_df.copy()
    plot_df["SPL_order"] = plot_df["SPL"].map({s: i for i, s in enumerate(SPL_ORDER)})
    approach_order_map = {a: i for i, a in enumerate(APPROACH_CONFIG.values())}
    plot_df["Approach_order"] = plot_df["Approach_Label"].map(approach_order_map)
    plot_df = plot_df.sort_values(by=["SPL_order", "Approach_order"]).reset_index(drop=True)

    # x labels: "SPL\napproach"
    x_labels = [f"{r['SPL']}\n{r['Approach_Label'].split(' (')[0]}"
                for _, r in plot_df.iterrows()]

    n_bars = len(plot_df)
    x = np.arange(n_bars)

    fig, ax = plt.subplots(figsize=(max(12, 0.6 * n_bars), 7))

    bottoms = np.zeros(n_bars)
    for phase in COARSE_PHASES:
        heights = plot_df[f"{phase} (ms)"].values / 1000.0 / 3600.0  # convert to hours
        ax.bar(x, heights, bottom=bottoms, color=PHASE_COLORS[phase],
               label=phase, edgecolor="white", linewidth=0.4)
        bottoms += heights

    ax.set_xticks(x)
    ax.set_xticklabels(x_labels, rotation=45, ha="right", fontsize=9)
    ax.set_ylabel("Cumulative CPU Time (hours)", fontsize=11)
    ax.set_title("RQ2: End-to-End Time Breakdown per SPL × Approach\n"
                 "(Cumulative across 80 shards, per-shard median over runs)",
                 fontsize=12)
    ax.legend(loc="upper left", fontsize=10)
    ax.grid(True, axis="y", alpha=0.3)
    ax.set_yscale("log")

    plt.tight_layout()
    plot_file = PLOTS_DIR / "rq2_time_breakdown.pdf"
    fig.savefig(plot_file, format="pdf", bbox_inches="tight")
    plt.close(fig)
    print(f"Saved: {plot_file.name}")

    # Console summary
    print("\n=== SAT share summary ===")
    for _, row in breakdown_df.iterrows():
        print(f"  {row['SPL']:5s} {row['Approach_Label']:40s}  "
              f"SAT={row['SAT (% of total)']:5.1f}%  "
              f"OwnCost={row['Own Cost (% of total)']:5.1f}%")

    print("\nrq2_06 DONE.")


if __name__ == "__main__":
    main()