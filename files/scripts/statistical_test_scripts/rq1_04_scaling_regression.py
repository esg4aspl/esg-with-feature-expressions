#!/usr/bin/env python3
"""
rq1_04_scaling_regression.py

Step 4 of the RQ1 pipeline — scaling analysis.

Reproduces the regression and Spearman results that support the manuscript
sections 'Per-product model complexity as cost driver' and 'Computational
Complexity'.

SCOPE NOTE — only L >= 2 is included in this analysis.
    L=1 (event coverage) uses a structurally different generation procedure:
    the algorithm targets event coverage directly via a single Euler cycle
    on the original graph, with no L-sequence transformation step. The
    complexity formula O(|E_L| * |F|) characterises only the higher-order
    pipeline (transformation -> balancing -> traversal) used at L=2,3,4.
    Including L=1 in the regression would mix two algorithmically distinct
    procedures and conflate their cost profiles, so this script restricts
    the analysis to L=2, L=3, L=4 for ESG-Fx and EFG, plus L=0 for the
    stochastic baseline (which has no L parameter).

Two T_gen columns are reported side by side:

  - Tgen_total_ms : scope-symmetric (disk-to-disk). This is what the manuscript
                    uses for the comparison tables and speedup figures, because
                    it equalises the input/output scope across approaches
                    (ESG-Fx and RandomWalk include model load + recording;
                    EFG's GuitarGenTime already covers the same scope inside
                    its Java 8 sub-process).

  - Tgen_alg_ms   : algorithmic-only component. For ESG-Fx and RandomWalk,
                    this is the algorithmic stretch alone (transformation +
                    balancing + Euler traversal for ESG-Fx). For EFG it
                    coincides with GuitarGenTime since GUITAR is opaque.
                    The Computational Complexity section uses this column
                    because the formula O(|E_L| * |F|) characterises the
                    algorithmic steps and not I/O or load.

Each of the three analyses is run twice, once per time variant, producing
a pair of sheets:

  pooled_regression_total / pooled_regression_alg
      Per-product linear regression of T_gen on edge count, pooled across
      all SPLs, separately per approach x level.

  spl_level_regression_total / spl_level_regression_alg
      One-point-per-(SPL, level) linear regression of T_gen on average edge
      count.

  per_spl_per_level_total / per_spl_per_level_alg
      Per-SPL per-level Spearman correlation. Diagnostic; shows when within
      a single SPL the per-product edge count has little effect (e.g. the
      structural baseline whose cost is dominated by fixed overhead).

A README sheet at the top of the workbook explains the suffix convention
and the L=1 exclusion rationale.

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

# Which coverage levels to include in the scaling regression.
#   L1 is excluded because event coverage uses a structurally different
#   generation procedure (no L-sequence transformation). The complexity
#   formula O(|E_L| * |F|) characterises only L >= 2.
#   L0 is the RandomWalk baseline level (no L parameter); it is kept because
#   it is the only level the stochastic baseline produces.
ALLOWED_LEVELS = {"L0", "L2", "L3", "L4"}
EXCLUDED_LEVELS = {"L1"}

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

# Full display names — used in the 'SPLName' / 'ApproachLabel' columns of
# the output Excel so that the workbook is human-readable even though the
# short codes ('SPL', 'Approach') are kept for stable filtering downstream.
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

# Column-name helpers (approach names differ in raw CSVs)
APPROACH_COLUMNS = {
    "ESG-Fx":     {"time_total": "TotalTestGenTime(ms)",
                   "time_alg":   "AlgTestGenTime(ms)",
                   "edges":      "NumberOfESGFxEdges",
                   "vertices":   "NumberOfESGFxVertices"},
    "EFG":        {"time_total": "GuitarGenTime(ms)",
                   "time_alg":   "GuitarGenTime(ms)",  # GUITAR is opaque; same column
                   "edges":      "NumberOfEFGEdges",
                   "vertices":   "NumberOfEFGVertices"},
    "RandomWalk": {"time_total": "TotalTestGenTime(ms)",
                   "time_alg":   "AlgTestGenTime(ms)",
                   "edges":      "NumberOfESGFxEdges",
                   "vertices":   "NumberOfESGFxVertices"},
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
    """Load all per-product median data across all SPLs, approaches, levels.

    Excludes sheets whose level is in EXCLUDED_LEVELS (L=1). The exclusion
    happens at load time so that downstream regressions cannot accidentally
    re-introduce the excluded level.
    """
    all_rows = []
    skipped_l1 = 0

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
            if level in EXCLUDED_LEVELS:
                skipped_l1 += 1
                continue
            if level not in ALLOWED_LEVELS:
                continue

            cols = APPROACH_COLUMNS[approach]
            df = pd.read_excel(xls, sheet_name=sheet)

            pid_col   = find_column(df, ["ProductID", "Product ID", "productId"])
            tot_col   = find_column(df, [cols["time_total"]])
            alg_col   = find_column(df, [cols["time_alg"]])
            edge_col  = find_column(df, [cols["edges"]])
            vert_col  = find_column(df, [cols["vertices"]])

            if pid_col is None or tot_col is None or edge_col is None:
                continue

            wanted = [pid_col, tot_col, edge_col]
            if alg_col and alg_col != tot_col:
                wanted.append(alg_col)
            if vert_col:
                wanted.append(vert_col)

            sub = df[wanted].copy()
            for c in wanted:
                if c == pid_col:
                    continue
                sub[c] = sub[c].apply(parse_euro)

            agg = {tot_col: 'median', edge_col: 'median'}
            if alg_col and alg_col != tot_col:
                agg[alg_col] = 'median'
            if vert_col:
                agg[vert_col] = 'median'
            medians = sub.groupby(pid_col).agg(agg).reset_index()

            rename = {pid_col: 'ProductID',
                      tot_col: 'Tgen_total_ms',
                      edge_col: 'Edges'}
            if alg_col and alg_col != tot_col:
                rename[alg_col] = 'Tgen_alg_ms'
            else:
                # For approaches like EFG where the two are the same, copy the
                # column so downstream code can still ask for Tgen_alg_ms.
                medians['Tgen_alg_ms'] = medians[tot_col]
            if vert_col:
                rename[vert_col] = 'Vertices'
            medians = medians.rename(columns=rename)

            medians['SPL'] = spl_short
            medians['Approach'] = approach
            medians['Level'] = level
            all_rows.append(medians)

    if skipped_l1:
        print(f"  [INFO] Skipped {skipped_l1} L=1 sheet(s) by design "
              f"(excluded from scaling regression — different traversal procedure).")
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

# Two T_gen columns are analysed separately:
#   - Tgen_total_ms : scope-symmetric (disk-to-disk). Used in the manuscript's
#                     comparison tables and speedup numbers (Section ResultsRQ1).
#   - Tgen_alg_ms   : algorithmic-only component (transformation + balancing +
#                     Euler traversal for ESG-Fx; full GuitarGenTime for EFG
#                     since GUITAR is opaque). Used in the manuscript's
#                     Computational Complexity regression because the
#                     theoretical formula characterises only the algorithmic
#                     steps.
TIME_COLUMNS = [
    ("total", "Tgen_total_ms",
     "Scope-symmetric T_gen (disk-to-disk). Used for table comparisons and speedup."),
    ("alg",   "Tgen_alg_ms",
     "Algorithmic T_gen only (transformation + balancing + traversal). "
     "Used for the Computational Complexity regression."),
]


def pooled_regression(data, time_col):
    rows = []
    for approach in ["ESG-Fx", "EFG", "RandomWalk"]:
        for level in sorted(data[data['Approach'] == approach]['Level'].unique()):
            sub = data[(data['Approach'] == approach) & (data['Level'] == level)]
            lr = run_linreg(sub['Edges'], sub[time_col])
            sp = run_spearman(sub['Edges'], sub[time_col])
            rows.append({
                "Approach": approach,
                "ApproachLabel": APPROACH_LABEL.get(approach, approach),
                "Level": level,
                "N_products": lr["N"],
                "Spearman_rho":     round(sp["rho"], 4)      if not pd.isna(sp["rho"])      else np.nan,
                "Spearman_p":       sp["p_value"],
                "Linreg_slope":     round(lr["slope"], 4)    if not pd.isna(lr["slope"])    else np.nan,
                "Linreg_intercept": round(lr["intercept"], 4)if not pd.isna(lr["intercept"])else np.nan,
                "R2":               round(lr["R2"], 4)       if not pd.isna(lr["R2"])       else np.nan,
                "Linreg_p":         lr["p_value"],
            })
    return pd.DataFrame(rows)


def spl_level_regression(data, time_col):
    rows = []
    for approach in ["ESG-Fx", "EFG", "RandomWalk"]:
        sub = data[data['Approach'] == approach]
        agg = sub.groupby(['SPL', 'Level']).agg({
            'Edges': 'mean',
            time_col: 'median',
        }).reset_index()
        lr = run_linreg(agg['Edges'], agg[time_col])
        sp = run_spearman(agg['Edges'], agg[time_col])
        rows.append({
            "Approach": approach,
            "ApproachLabel": APPROACH_LABEL.get(approach, approach),
            "N_SPL_level_points": lr["N"],
            "Spearman_rho":       round(sp["rho"], 4)       if not pd.isna(sp["rho"])       else np.nan,
            "Spearman_p":         sp["p_value"],
            "Linreg_slope":       round(lr["slope"], 4)     if not pd.isna(lr["slope"])     else np.nan,
            "Linreg_intercept":   round(lr["intercept"], 4) if not pd.isna(lr["intercept"]) else np.nan,
            "R2":                 round(lr["R2"], 4)        if not pd.isna(lr["R2"])        else np.nan,
            "Linreg_p":           lr["p_value"],
        })
    return pd.DataFrame(rows)


def per_spl_per_level_regression(data, time_col):
    rows = []
    for approach in ["ESG-Fx", "EFG", "RandomWalk"]:
        for spl in SPL_MAPPING.values():
            spl_levels = sorted(data[(data['Approach'] == approach) &
                                     (data['SPL'] == spl)]['Level'].unique())
            for level in spl_levels:
                sub = data[(data['Approach'] == approach) & (data['SPL'] == spl)
                           & (data['Level'] == level)]
                if len(sub) < 10:
                    continue
                sp = run_spearman(sub['Edges'], sub[time_col])
                lr = run_linreg(sub['Edges'], sub[time_col])
                rows.append({
                    "Approach": approach,
                    "ApproachLabel": APPROACH_LABEL.get(approach, approach),
                    "SPL": spl,
                    "SPLName": SPL_FULL_NAME.get(spl, spl),
                    "Level": level,
                    "N_products":     sp["N"],
                    "rho":            round(sp["rho"], 4)  if not pd.isna(sp["rho"])  else np.nan,
                    "Spearman_p":     sp["p_value"],
                    "R2":             round(lr["R2"], 4)   if not pd.isna(lr["R2"])   else np.nan,
                    "Linreg_p":       lr["p_value"],
                })
    return pd.DataFrame(rows)


def main():
    print("=" * 70)
    print("rq1_04: Scaling Regression Analysis")
    print("=" * 70)
    print(f"Data directory   : {DATA_DIR}")
    print(f"Output directory : {OUTPUT_DIR}")
    print(f"Levels included  : {sorted(ALLOWED_LEVELS)} "
          f"(L=1 excluded — different traversal procedure)")

    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

    print("\nLoading per-product data...")
    data = load_all_per_product_data()
    if data.empty:
        print("ERROR: no per-product data loaded. Run rq1_02 first.")
        sys.exit(1)
    print(f"  Loaded {len(data)} product-level rows.")
    print(f"  Levels present: {sorted(data['Level'].unique())}")

    output_file = OUTPUT_DIR / "rq1_scaling_regression.xlsx"
    sheets: dict = {}

    # README sheet describing what each pair of sheets contains.
    readme_rows = [
        {"Sheet suffix": "(scope note)",
         "Time column": "—",
         "Description": ("L=1 excluded by design. Event coverage at L=1 uses a "
                         "structurally different generation procedure (single Euler "
                         "cycle, no L-sequence transformation). The complexity "
                         "formula O(|E_L| * |F|) characterises only L >= 2.")},
        {"Sheet suffix": "_total",
         "Time column": "Tgen_total_ms",
         "Description": TIME_COLUMNS[0][2]},
        {"Sheet suffix": "_alg",
         "Time column": "Tgen_alg_ms",
         "Description": TIME_COLUMNS[1][2]},
    ]
    sheets["README"] = pd.DataFrame(readme_rows)

    for tag, time_col, desc in TIME_COLUMNS:
        print(f"\n========== Time variant: {tag} ({time_col}) ==========")
        print(f"  {desc}")

        print(f"\n[1] Pooled per-product regression — {tag}")
        pooled = pooled_regression(data, time_col)
        sheets[f"pooled_regression_{tag}"] = pooled
        for _, r in pooled[pooled['Approach'] == 'ESG-Fx'].iterrows():
            rho = r['Spearman_rho']
            r2 = r['R2']
            rho_s = f"{rho:.3f}" if pd.notna(rho) else "  nan"
            r2_s  = f"{r2:.3f}"  if pd.notna(r2)  else "  nan"
            print(f"  ESG-Fx  {r['Level']}: N={r['N_products']:<5}  rho={rho_s}  R2={r2_s}")

        print(f"\n[2] SPL-level regression — {tag}")
        spl_lvl = spl_level_regression(data, time_col)
        sheets[f"spl_level_regression_{tag}"] = spl_lvl
        for _, r in spl_lvl.iterrows():
            r2 = r['R2']
            r2_s = f"{r2:.3f}" if pd.notna(r2) else "  nan"
            print(f"  {r['Approach']:<10s}: N={r['N_SPL_level_points']:<3}  R2={r2_s}")

        print(f"\n[3] Per-SPL per-level Spearman — {tag}")
        per_spl = per_spl_per_level_regression(data, time_col)
        sheets[f"per_spl_per_level_{tag}"] = per_spl

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