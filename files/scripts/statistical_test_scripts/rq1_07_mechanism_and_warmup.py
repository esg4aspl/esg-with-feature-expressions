#!/usr/bin/env python3
"""
rq1_07_mechanism_and_warmup.py

Step 7 of the RQ1 pipeline — mechanism analysis + JVM warm-up evidence.

This script produces the per-component analysis that backs the
"complexity-driven mechanism" paragraph in Section 7.1.2 and the JVM
warm-up footnote in Sections 7.1.5 and the construct-validity threat in
Section 8.

What it computes (all from the per-product / per-run data already on disk):

  Part A — Mechanism analysis for Model Once, Generate Any
    For each L in {L2, L3, L4}, pooled across all 4,455 products:
      * Median share of TransformationTime in TotalTestGenTime (in %)
      * Spearman rho between per-product edge count and:
          - TotalTestGenTime
          - ESGFxModelLoadTime
          - TransformationTime
          - AlgTestGenTime
          - TestCaseRecordingTime
      The increase in component-level rho with L is the empirical
      mechanism behind the rise in the overall pooled rho with L.

  Part B — JVM warm-up evidence (structural baseline)
    For each (SPL, approach, L) cell, across the eleven repetitions:
      * mean / median ratio
      * max  / median ratio
      * CV%  vs. IQR%
    A high mean/median or max/median combined with a small IQR%
    indicates a single high outlier per cell, consistent with
    JVM warm-up on the first invocation in the run sequence.

Paths (mirrors rq1_03/04/05/06):
    Script: files/scripts/statistical_test_scripts/
            rq1_07_mechanism_and_warmup.py
    Data  : files/Cases/<SPL>/RQ1_<SPL>_perProduct.xlsx (Part A)
            files/Cases/<SPL>/RQ1_<SPL>_perRun.xlsx     (Part B)
    Out   : files/scripts/statistical_test_scripts/rq1_result/
            rq1_mechanism_and_warmup.xlsx

Usage:
    python rq1_07_mechanism_and_warmup.py
"""
from __future__ import annotations

import sys
import warnings
from pathlib import Path

import numpy as np
import pandas as pd
from scipy import stats

warnings.filterwarnings("ignore")


# ─── Paths ──────────────────────────────────────────────────────────────────
SCRIPT_DIR  = Path(__file__).resolve().parent
SCRIPTS_DIR = SCRIPT_DIR.parent
FILES_DIR   = SCRIPTS_DIR.parent
DATA_DIR    = FILES_DIR / "Cases"
OUTPUT_DIR  = SCRIPT_DIR / "rq1_result"


# ─── Configuration ──────────────────────────────────────────────────────────
SPL_MAPPING = {
    "SodaVendingMachine":      "SVM",
    "eMail":                   "eM",
    "Elevator":                "El",
    "BankAccountv2":           "BAv2",
    "StudentAttendanceSystem": "SAS",
    "Tesla":                   "Te",
    "syngovia":                "Svia",
    "HockertyShirts":          "HS",
}

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

# Mapping for the time column used as the comparable T_gen on the perRun sheet
TGEN_COL_BY_APPROACH = {
    "ESG-Fx":     "TotalTestGenTime(ms)",
    "EFG":        "GuitarGenTime(ms)",
    "RandomWalk": "TotalTestGenTime(ms)",
}


# ─── Utility: parse European decimal numbers (comma as separator) ───────────
def parse_eu_num(v):
    if pd.isna(v):
        return np.nan
    if isinstance(v, (int, float)):
        return float(v)
    s = str(v).strip()
    if s == "" or s.lower() == "none":
        return np.nan
    if "," in s and "." in s:
        if s.rfind(",") > s.rfind("."):
            s = s.replace(".", "").replace(",", ".")
    elif "," in s:
        s = s.replace(",", ".")
    try:
        return float(s)
    except ValueError:
        return np.nan


# ─── Part A: Mechanism analysis ─────────────────────────────────────────────
def part_a_mechanism():
    """For each L (L2, L3, L4), compute pooled-across-4455-products
    Spearman rho between edge count and each component of T_gen for
    Model Once, Generate Any, plus the median share of TransformationTime
    in TotalTestGenTime."""
    print("\n--- Part A: Mechanism analysis (Model Once, Generate Any) ---")

    component_cols = {
        "TotalTestGenTime":      "TotalTestGenTime(ms)",
        "ESGFxModelLoadTime":    "ESGFxModelLoadTime(ms)",
        "TransformationTime":    "TransformationTime(ms)",
        "AlgTestGenTime":        "AlgTestGenTime(ms)",
        "TestCaseRecordingTime": "TestCaseRecordingTime(ms)",
    }

    all_records = []
    for spl_folder, spl_short in SPL_MAPPING.items():
        excel_path = DATA_DIR / spl_folder / f"RQ1_{spl_folder}_perProduct.xlsx"
        if not excel_path.exists():
            print(f"  [SKIP] {spl_folder}: not found")
            continue

        for level in ["L2", "L3", "L4"]:
            try:
                df = pd.read_excel(excel_path, sheet_name=f"ESG-Fx_{level}")
            except Exception:
                continue

            cols_needed = list(component_cols.values()) + ["NumberOfESGFxEdges"]
            for col in cols_needed:
                if col in df.columns:
                    df[col] = df[col].apply(parse_eu_num)

            agg = {col: "median" for col in cols_needed if col in df.columns}
            pp = df.groupby("ProductID").agg(agg).reset_index()
            pp["SPL"] = spl_short
            pp["Level"] = level
            all_records.append(pp)

    if not all_records:
        return None, None
    data = pd.concat(all_records, ignore_index=True)

    # Per-L: spearman of edges with each component (pooled)
    rho_rows = []
    edge_col = "NumberOfESGFxEdges"
    for L in ["L2", "L3", "L4"]:
        sub = data[data["Level"] == L].dropna(subset=[edge_col])
        n_total = len(sub)
        for comp_short, comp_col in component_cols.items():
            if comp_col not in sub.columns:
                continue
            v = sub.dropna(subset=[comp_col])
            v = v[v[comp_col] >= 0]
            if len(v) < 10 or v[comp_col].std() == 0 or v[edge_col].std() == 0:
                rho_rows.append({"Level": L, "Component": comp_short,
                                 "N": len(v), "rho": np.nan, "p_value": np.nan})
                continue
            rho, p = stats.spearmanr(v[edge_col], v[comp_col])
            rho_rows.append({"Level": L, "Component": comp_short,
                             "N": len(v), "rho": round(rho, 4),
                             "p_value": p})
        # Also report a row for transformation share
        sub_t = sub.dropna(subset=["TransformationTime(ms)", "TotalTestGenTime(ms)"])
        sub_t = sub_t[sub_t["TotalTestGenTime(ms)"] > 0]
        if len(sub_t) > 0:
            share = sub_t["TransformationTime(ms)"] / sub_t["TotalTestGenTime(ms)"] * 100
            rho_rows.append({
                "Level": L,
                "Component": "TransformationShare(%)",
                "N": len(sub_t),
                "rho": np.nan,
                "p_value": np.nan,
                "median_share_pct": round(share.median(), 2),
                "iqr_share_pct": round(share.quantile(0.75) - share.quantile(0.25), 2),
            })

    rho_df = pd.DataFrame(rho_rows)

    # Pivot for readability
    pivot_rho = rho_df[rho_df["Component"] != "TransformationShare(%)"].pivot(
        index="Component", columns="Level", values="rho"
    )
    pivot_share = rho_df[rho_df["Component"] == "TransformationShare(%)"][
        ["Level", "median_share_pct", "iqr_share_pct"]
    ]

    print("\n  Pooled Spearman rho between edge count and each component of T_gen:")
    print(pivot_rho.to_string())
    print("\n  Median share of TransformationTime in TotalTestGenTime (%):")
    print(pivot_share.to_string(index=False))

    return rho_df, data


# ─── Part B: JVM warm-up evidence ──────────────────────────────────────────
def part_b_warmup():
    """For each (SPL, approach, L) cell, compute mean/median and max/median
    ratios across the eleven repetitions, alongside CV% and IQR%. A high
    max/median combined with a small IQR% indicates a single high outlier
    per cell — the empirical fingerprint of JVM warm-up."""
    print("\n--- Part B: JVM warm-up evidence (per-cell outlier analysis) ---")

    rows = []
    for spl_folder, spl_short in SPL_MAPPING.items():
        excel_path = DATA_DIR / spl_folder / f"RQ1_{spl_folder}_perRun.xlsx"
        if not excel_path.exists():
            print(f"  [SKIP] {spl_folder}: perRun file not found")
            continue

        xls = pd.ExcelFile(excel_path)
        for sheet in xls.sheet_names:
            # Identify approach + level from sheet name
            if sheet.startswith("ESG-Fx"):
                approach = "ESG-Fx"
            elif sheet.startswith("EFG"):
                approach = "EFG"
            elif sheet.startswith("Random"):
                approach = "RandomWalk"
            else:
                continue
            level = next((lv for lv in ["L0", "L1", "L2", "L3", "L4"]
                          if lv in sheet), None)
            if level is None:
                continue

            df = pd.read_excel(xls, sheet_name=sheet)
            tgen_col = TGEN_COL_BY_APPROACH[approach]
            if tgen_col not in df.columns:
                continue

            vals = df[tgen_col].apply(parse_eu_num).dropna()
            vals = vals[vals > 0]
            if len(vals) < 2:
                continue

            v = vals.values
            median = float(np.median(v))
            mean = float(np.mean(v))
            std = float(np.std(v, ddof=1))
            mn, mx = float(v.min()), float(v.max())
            q1, q3 = np.percentile(v, [25, 75])
            iqr = float(q3 - q1)

            cv_pct = std / mean * 100 if mean != 0 else np.nan
            iqr_pct = iqr / median * 100 if median != 0 else np.nan
            mean_med = mean / median if median != 0 else np.nan
            max_med = mx / median if median != 0 else np.nan

            rows.append({
                "SPL": spl_short,
                "SPLName": SPL_FULL_NAME.get(spl_short, spl_short),
                "Approach": approach,
                "ApproachLabel": APPROACH_LABEL.get(approach, approach),
                "Level": level,
                "N_runs": int(len(v)),
                "median_ms": round(median, 2),
                "mean_ms": round(mean, 2),
                "min_ms": round(mn, 2),
                "max_ms": round(mx, 2),
                "CV_pct": round(cv_pct, 2),
                "IQR_pct": round(iqr_pct, 2),
                "mean_over_median": round(mean_med, 3),
                "max_over_median": round(max_med, 3),
            })

    df_out = pd.DataFrame(rows)

    # Filter to article scope (L != L1, ignored for the article)
    df_a = df_out[df_out["Level"] != "L1"].copy()

    # Summary by approach
    print("\n  TestGenTime CV% summary by approach (article scope):")
    summary = df_a.groupby("Approach").agg(
        median_CV_pct=("CV_pct", "median"),
        max_CV_pct=("CV_pct", "max"),
        median_IQR_pct=("IQR_pct", "median"),
        max_IQR_pct=("IQR_pct", "max"),
        median_max_over_median=("max_over_median", "median"),
        max_max_over_median=("max_over_median", "max"),
    ).round(2).reset_index()
    print(summary.to_string(index=False))

    # Cells with high CV% but low IQR% — the warm-up fingerprint
    fingerprint = df_a[(df_a["CV_pct"] > 15) & (df_a["IQR_pct"] < 10)].copy()
    fingerprint = fingerprint.sort_values("CV_pct", ascending=False)
    print(f"\n  Cells with CV% > 15 AND IQR% < 10 (warm-up fingerprint): "
          f"{len(fingerprint)} cells")
    if len(fingerprint) > 0:
        print(fingerprint[["SPLName", "Approach", "Level", "CV_pct", "IQR_pct",
                           "max_over_median", "median_ms", "max_ms"]].to_string(index=False))

    return df_out, summary, fingerprint


# ─── Main ───────────────────────────────────────────────────────────────────
def main():
    print("=" * 70)
    print("rq1_07: Mechanism analysis + JVM warm-up evidence")
    print("=" * 70)
    print(f"Data directory   : {DATA_DIR}")
    print(f"Output directory : {OUTPUT_DIR}")

    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

    # Part A
    rho_df, mech_data = part_a_mechanism()
    if rho_df is None:
        print("ERROR: Part A produced no data.")
        sys.exit(1)

    # Part B
    warmup_df, warmup_summary, warmup_fp = part_b_warmup()

    # Write output workbook
    output_file = OUTPUT_DIR / "rq1_mechanism_and_warmup.xlsx"
    print(f"\nWriting {output_file.name}...")

    with pd.ExcelWriter(output_file, engine="openpyxl") as writer:
        # Part A — long form
        rho_df.to_excel(writer, sheet_name="mechanism_rho_long", index=False)
        # Part A — pivot for easy reading in manuscript
        pivot = rho_df[rho_df["Component"] != "TransformationShare(%)"].pivot(
            index="Component", columns="Level", values="rho"
        )
        pivot.to_excel(writer, sheet_name="mechanism_rho_pivot")
        # Transformation share
        share = rho_df[rho_df["Component"] == "TransformationShare(%)"][
            ["Level", "median_share_pct", "iqr_share_pct"]
        ]
        share.to_excel(writer, sheet_name="transformation_share", index=False)

        # Part B
        warmup_df.to_excel(writer, sheet_name="warmup_per_cell", index=False)
        warmup_summary.to_excel(writer, sheet_name="warmup_summary", index=False)
        warmup_fp.to_excel(writer, sheet_name="warmup_fingerprint", index=False)

    print(f"\nSaved: {output_file}")
    print("\nrq1_07 DONE.")


if __name__ == "__main__":
    main()