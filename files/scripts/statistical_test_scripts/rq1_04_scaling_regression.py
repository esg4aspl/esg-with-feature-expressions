#!/usr/bin/env python3
"""
rq1_04_scaling_regression.py

Step 4 of the RQ1 pipeline — scaling analysis.

Reproduces the regression and Spearman results reported in the manuscript
sections 'Per-product model complexity as cost driver' and 'Computational
Complexity':

  - Per-product linear regression of median T_gen on edge count,
    across all 4455+ products in the experimental sample, per coverage level:
        R^2 at L = 2, L = 3, L = 4
        Spearman rho at L = 2, L = 3, L = 4

  - SPL-level linear regression of per-product median T_gen on average
    edge count across all 8 SPLs x 3 coverage levels (24 data points):
        R^2 (SPL-level aggregation)

  - Per-SPL per-level Spearman rho (diagnostic: shows when per-product
    edge count has little effect, e.g. on the structural baseline).

Paths (relative to this script's location):
    Script: files/scripts/statistical_test_scripts/rq1_04_scaling_regression.py
    Data  : files/Cases/<SPL>/RQ1_<SPL>_perProduct.xlsx  (produced by rq1_02)
    Out   : files/scripts/statistical_test_scripts/rq1_result/
            rq1_scaling_regression.xlsx

Usage (from any directory):
    python rq1_04_scaling_regression.py

Dependencies: pandas, numpy, scipy, openpyxl
"""
from __future__ import annotations

import sys
from pathlib import Path

import numpy as np
import pandas as pd
from scipy import stats


# ─── Paths (match rq3_* script convention) ─────────────────────────────────
SCRIPT_DIR = Path(__file__).resolve().parent
SCRIPTS_DIR = SCRIPT_DIR.parent
FILES_DIR = SCRIPTS_DIR.parent
DATA_DIR = FILES_DIR / "Cases"
OUTPUT_DIR = SCRIPT_DIR / "rq1_result"


# ─── Configuration ──────────────────────────────────────────────────────────
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

# Column-name helpers (approach names differ in raw CSVs)
APPROACH_COLUMNS = {
    "ESG-Fx":     {"time": "TotalTestGenTime(ms)", "edges": "NumberOfESGFxEdges",
                   "vertices": "NumberOfESGFxVertices"},
    "EFG":        {"time": "GuitarGenTime(ms)",    "edges": "NumberOfEFGEdges",
                   "vertices": "NumberOfEFGVertices"},
    "RandomWalk": {"time": "TestGenTime(ms)",      "edges": "Edges",
                   "vertices": "Vertices"},
}


def find_column(df, candidates):
    for c in candidates:
        if c in df.columns:
            return c
    for c in candidates:
        for col in df.columns:
            if col.strip().lower() == c.lower():
                return col
    return None


def parse_euro(val):
    if pd.isna(val):
        return np.nan
    if isinstance(val, (int, float)):
        return float(val)
    s = str(val).strip()
    if s == '' or s.lower() == 'none':
        return np.nan
    if ',' in s and '.' in s:
        if s.rfind(',') > s.rfind('.'):
            s = s.replace('.', '').replace(',', '.')
    elif ',' in s:
        s = s.replace(',', '.')
    try:
        return float(s)
    except ValueError:
        return np.nan


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


# ─── Data loading ──────────────────────────────────────────────────────────
def load_all_per_product_data():
    """Load all per-product median data across all SPLs, approaches, levels."""
    all_rows = []

    for spl_folder, spl_short in SPL_MAPPING.items():
        excel_path = DATA_DIR / spl_folder / f"RQ1_{spl_folder}_perProduct.xlsx"
        if not excel_path.exists():
            print(f"  [SKIP] {spl_folder}: perProduct Excel missing")
            continue

        xls = pd.ExcelFile(excel_path)
        for sheet in xls.sheet_names:
            approach = get_approach(sheet)
            level = get_level(sheet)
            if approach is None or level is None:
                continue

            cols = APPROACH_COLUMNS[approach]
            df = pd.read_excel(xls, sheet_name=sheet)

            pid_col = find_column(df, ["ProductID", "Product ID", "productId"])
            time_col = find_column(df, [cols["time"]])
            edge_col = find_column(df, [cols["edges"]])
            vert_col = find_column(df, [cols["vertices"]])

            if pid_col is None or time_col is None or edge_col is None:
                continue

            sub = df[[pid_col, time_col, edge_col]].copy()
            if vert_col:
                sub[vert_col] = df[vert_col]
            sub[time_col] = sub[time_col].apply(parse_euro)
            sub[edge_col] = sub[edge_col].apply(parse_euro)
            if vert_col:
                sub[vert_col] = sub[vert_col].apply(parse_euro)

            agg = {time_col: 'median', edge_col: 'median'}
            if vert_col:
                agg[vert_col] = 'median'
            medians = sub.groupby(pid_col).agg(agg).reset_index()

            rename = {pid_col: 'ProductID', time_col: 'Tgen_ms', edge_col: 'Edges'}
            if vert_col:
                rename[vert_col] = 'Vertices'
            medians = medians.rename(columns=rename)

            medians['SPL'] = spl_short
            medians['Approach'] = approach
            medians['Level'] = level
            all_rows.append(medians)

    return pd.concat(all_rows, ignore_index=True) if all_rows else pd.DataFrame()


# ─── Regression helpers ────────────────────────────────────────────────────
def run_linreg(x, y):
    x = np.asarray(x, dtype=float)
    y = np.asarray(y, dtype=float)
    mask = ~(np.isnan(x) | np.isnan(y))
    x, y = x[mask], y[mask]
    if len(x) < 3 or np.std(x) == 0:
        return {"N": int(len(x)), "slope": np.nan, "intercept": np.nan,
                "R2": np.nan, "p_value": np.nan, "std_err": np.nan}
    slope, intercept, r_value, p_value, std_err = stats.linregress(x, y)
    return {"N": int(len(x)), "slope": float(slope), "intercept": float(intercept),
            "R2": float(r_value ** 2), "p_value": float(p_value), "std_err": float(std_err)}


def run_spearman(x, y):
    x = np.asarray(x, dtype=float)
    y = np.asarray(y, dtype=float)
    mask = ~(np.isnan(x) | np.isnan(y))
    x, y = x[mask], y[mask]
    if len(x) < 3 or np.std(x) == 0 or np.std(y) == 0:
        return {"N": int(len(x)), "rho": np.nan, "p_value": np.nan}
    rho, p = stats.spearmanr(x, y)
    return {"N": int(len(x)), "rho": float(rho), "p_value": float(p)}


# ─── Main analysis ─────────────────────────────────────────────────────────
def main():
    print("=" * 70)
    print("rq1_04: Scaling Regression Analysis")
    print("=" * 70)
    print(f"Data directory   : {DATA_DIR}")
    print(f"Output directory : {OUTPUT_DIR}")

    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

    print("\nLoading per-product data...")
    data = load_all_per_product_data()
    if data.empty:
        print("ERROR: no per-product data loaded. Run rq1_02 first.")
        sys.exit(1)
    print(f"  Loaded {len(data)} product-level rows.")

    output_file = OUTPUT_DIR / "rq1_scaling_regression.xlsx"
    sheets: dict[str, pd.DataFrame] = {}

    # ── Sheet 1: Pooled per-product regression (all SPLs together) ──
    print("\n[1] Pooled per-product regression (all SPLs, per approach x level)")
    rows = []
    for approach in ["ESG-Fx", "EFG", "RandomWalk"]:
        for level in sorted(data[data['Approach'] == approach]['Level'].unique()):
            sub = data[(data['Approach'] == approach) & (data['Level'] == level)]
            lr = run_linreg(sub['Edges'], sub['Tgen_ms'])
            sp = run_spearman(sub['Edges'], sub['Tgen_ms'])
            rows.append({
                "Approach": approach, "Level": level,
                "N_products": lr["N"],
                "Spearman_rho": round(sp["rho"], 4) if not pd.isna(sp["rho"]) else np.nan,
                "Spearman_p": sp["p_value"],
                "Linreg_slope": round(lr["slope"], 4) if not pd.isna(lr["slope"]) else np.nan,
                "Linreg_intercept": round(lr["intercept"], 4) if not pd.isna(lr["intercept"]) else np.nan,
                "R2": round(lr["R2"], 4) if not pd.isna(lr["R2"]) else np.nan,
                "Linreg_p": lr["p_value"],
            })
            if approach == "ESG-Fx":
                print(f"  ESG-Fx  {level}: N={lr['N']:<5}  "
                      f"rho={sp['rho']:.3f}  R2={lr['R2']:.3f}  p={lr['p_value']:.2e}")
    sheets["pooled_regression"] = pd.DataFrame(rows)

    # ── Sheet 2: SPL-level regression (one point per SPL-level aggregate) ──
    print("\n[2] SPL-level regression (one point per SPL x level)")
    rows = []
    for approach in ["ESG-Fx", "EFG", "RandomWalk"]:
        # Aggregate per SPL-level: avg edges & median of per-product medians
        sub = data[data['Approach'] == approach]
        agg_spl_level = sub.groupby(['SPL', 'Level']).agg({
            'Edges': 'mean',
            'Tgen_ms': 'median',
        }).reset_index()
        # Regression across all SPL x level points
        lr = run_linreg(agg_spl_level['Edges'], agg_spl_level['Tgen_ms'])
        sp = run_spearman(agg_spl_level['Edges'], agg_spl_level['Tgen_ms'])
        rows.append({
            "Approach": approach,
            "N_SPL_level_points": lr["N"],
            "Spearman_rho": round(sp["rho"], 4) if not pd.isna(sp["rho"]) else np.nan,
            "Spearman_p": sp["p_value"],
            "Linreg_slope": round(lr["slope"], 4) if not pd.isna(lr["slope"]) else np.nan,
            "Linreg_intercept": round(lr["intercept"], 4) if not pd.isna(lr["intercept"]) else np.nan,
            "R2": round(lr["R2"], 4) if not pd.isna(lr["R2"]) else np.nan,
            "Linreg_p": lr["p_value"],
        })
        print(f"  {approach:<10s}: N={lr['N']:<3}  R2={lr['R2']:.3f}  p={lr['p_value']:.2e}")
    sheets["spl_level_regression"] = pd.DataFrame(rows)

    # ── Sheet 3: Per-SPL per-level Spearman (diagnostic) ──
    print("\n[3] Per-SPL per-level Spearman (diagnostic)")
    rows = []
    for approach in ["ESG-Fx", "EFG", "RandomWalk"]:
        for spl in SPL_MAPPING.values():
            for level in sorted(data[(data['Approach'] == approach) &
                                     (data['SPL'] == spl)]['Level'].unique()):
                sub = data[(data['Approach'] == approach) & (data['SPL'] == spl)
                           & (data['Level'] == level)]
                if len(sub) < 10:
                    continue
                sp = run_spearman(sub['Edges'], sub['Tgen_ms'])
                lr = run_linreg(sub['Edges'], sub['Tgen_ms'])
                rows.append({
                    "Approach": approach, "SPL": spl, "Level": level,
                    "N_products": sp["N"],
                    "rho": round(sp["rho"], 4) if not pd.isna(sp["rho"]) else np.nan,
                    "Spearman_p": sp["p_value"],
                    "R2": round(lr["R2"], 4) if not pd.isna(lr["R2"]) else np.nan,
                    "Linreg_p": lr["p_value"],
                })
    sheets["per_spl_per_level"] = pd.DataFrame(rows)

    # Write output
    print(f"\nWriting {output_file.name}...")
    with pd.ExcelWriter(output_file, engine="openpyxl") as writer:
        for name, df in sheets.items():
            df.to_excel(writer, sheet_name=name, index=False)
            print(f"  sheet '{name}' ({len(df)} rows)")

    print(f"\nSaved: {output_file}")
    print("\nrq1_04 DONE.")


if __name__ == "__main__":
    main()