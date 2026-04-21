#!/usr/bin/env python3
"""
rq1_per_product_analysis.py — Per-Product Analysis for RQ1
============================================================
Reads per-product raw data from each SPL's Excel file, computes per-product
medians across 11 runs, then produces:

  1. Summary table: per-product average model complexity by SPL
  2. Scatter plots: edges vs. T_gen per product, colored by SPL
  3. Box plots: per-product T_gen distribution across SPLs
  4. Spearman correlation analysis
  5. Svia vs Tesla deep comparison
  6. Per-SPL scatter plots saved to each Case folder

Outputs:
  - Cross-SPL → {BASE_DIR}/_CrossSPL_Analysis/RQ1/PerProductAnalysis/
  - Per-SPL   → {BASE_DIR}/{CaseName}/RQ1/PerProductAnalysis/

Two versions: Article (L2-L4) and ForPhDThesis (includes L1).

Usage:
    python rq1_per_product_analysis.py
"""

import os
import sys
import warnings
import numpy as np
import pandas as pd
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
from scipy import stats
from pathlib import Path

warnings.filterwarnings('ignore')

# ─── Configuration ───────────────────────────────────────────────────────────

BASE_DIR = Path("/Users/dilekozturk/git/esg-with-feature-expressions/files/Cases")

SPL_NAME_MAPPING = {
    "SodaVendingMachine": "SVM",
    "eMail": "eM",
    "Elevator": "El",
    "BankAccountv2": "BAv2",
    "StudentAttendanceSystem": "SAS",
    "Tesla": "Te",
    "syngovia": "Svia",
    "HockertyShirts": "HS"
}

SPL_ORDER = ["SVM", "eM", "El", "BAv2", "SAS", "Te", "Svia", "HS"]
SPL_SCALE = {
    "SVM": "Small", "eM": "Small", "El": "Small",
    "BAv2": "Medium", "SAS": "Medium",
    "Te": "Large", "Svia": "Large", "HS": "Large"
}

SPL_COLORS = {
    "SVM": "#1b9e77", "eM": "#d95f02", "El": "#7570b3",
    "BAv2": "#e7298a", "SAS": "#66a61e",
    "Te": "#e6ab02", "Svia": "#a6761d", "HS": "#666666"
}

# Display labels for figures (manuscript terminology)
APPROACH_LABEL = {
    "ESG-Fx":     "Model Once, Generate Any",
    "EFG":        "Structural Baseline",
    "RandomWalk": "Stochastic Baseline",
}

def approach_title(approach, level):
    """Format title: all labels italic, consistent style."""
    label = APPROACH_LABEL.get(approach, approach)
    lv = level.replace("L0", "Edge Coverage").replace("L", "L = ")
    return f'$\\it{{{label.replace(" ", "\\ ")}}}$ — ${lv}$'

# Sheets to read from per-product Excel files
# Article: L2-L4 for ESG-Fx and EFG, L0 for RandomWalk
# Thesis: adds L1 for ESG-Fx
SHEETS_ESGFX_ARTICLE = ["ESG-Fx_L2", "ESG-Fx_L3", "ESG-Fx_L4"]
SHEETS_ESGFX_THESIS  = ["ESG-Fx_L1", "ESG-Fx_L2", "ESG-Fx_L3", "ESG-Fx_L4"]
SHEETS_EFG   = ["EFG_L2", "EFG_L3", "EFG_L4"]
SHEETS_RW    = ["RandomWalk_L0"]


# ─── Utility Functions ──────────────────────────────────────────────────────

def parse_european_number(val):
    if pd.isna(val): return np.nan
    if isinstance(val, (int, float)): return float(val)
    s = str(val).strip()
    if s == '' or s.lower() == 'none': return np.nan
    if ',' in s and '.' in s:
        if s.rfind(',') > s.rfind('.'):
            s = s.replace('.', '').replace(',', '.')
    elif ',' in s:
        s = s.replace(',', '.')
    try: return float(s)
    except ValueError: return np.nan


def read_sheet_safe(excel_path, sheet_name):
    try:
        df = pd.read_excel(excel_path, sheet_name=sheet_name, engine='openpyxl')
        skip_cols = {'ProductID', 'Status', 'ErrorReason', 'Approach',
                     'Coverage Type', 'Coverage Level', 'SafetyLimitHit'}
        for col in df.columns:
            if col not in skip_cols:
                df[col] = df[col].apply(parse_european_number)
        return df
    except Exception:
        return None


def get_approach_from_sheet(sheet_name):
    if sheet_name.startswith("ESG-Fx"):  return "ESG-Fx"
    if sheet_name.startswith("EFG"):     return "EFG"
    if sheet_name.startswith("Random"):  return "RandomWalk"
    return None


def get_level_from_sheet(sheet_name):
    for lv in ["L1", "L2", "L3", "L4", "L0"]:
        if lv in sheet_name: return lv
    return None


def find_column(df, candidates):
    for c in candidates:
        if c in df.columns: return c
    for c in candidates:
        for col in df.columns:
            if col.strip().lower() == c.lower(): return col
    return None


def get_time_col(df, approach):
    mapping = {
        "ESG-Fx":     ["TotalTestGenTime(ms)"],
        "EFG":        ["GuitarGenTime(ms)"],
        "RandomWalk": ["TestGenTime(ms)"],
    }
    return find_column(df, mapping.get(approach, ["TestGenTime(ms)"]))


def get_vertices_col(df, approach):
    mapping = {
        "ESG-Fx":     ["NumberOfESGFxVertices"],
        "EFG":        ["NumberOfEFGVertices"],
        "RandomWalk": ["Vertices"],
    }
    return find_column(df, mapping.get(approach, ["Vertices"]))


def get_edges_col(df, approach):
    mapping = {
        "ESG-Fx":     ["NumberOfESGFxEdges"],
        "EFG":        ["NumberOfEFGEdges"],
        "RandomWalk": ["Edges"],
    }
    return find_column(df, mapping.get(approach, ["Edges"]))


def get_product_id_col(df):
    return find_column(df, ["ProductID", "Product ID", "productId"])


def compute_per_product_medians(df, approach):
    pid_col  = get_product_id_col(df)
    time_col = get_time_col(df, approach)
    vert_col = get_vertices_col(df, approach)
    edge_col = get_edges_col(df, approach)

    if pid_col is None or time_col is None:
        return None

    agg_dict = {time_col: 'median'}
    if vert_col and vert_col in df.columns: agg_dict[vert_col] = 'median'
    if edge_col and edge_col in df.columns: agg_dict[edge_col] = 'median'

    extra_candidates = [
        (["NumberOfESGFxTestCases", "NumberOfEFGTestCases", "TestCases"], "test_cases"),
        (["NumberOfESGFxTestEvents", "NumberOfEFGTestEvents", "TestEvents"], "test_events"),
        (["TestExecTimeMs", "TestExecTime(ms)"], "exec_time"),
        (["TestGenPeakMemory(MB)", "ParentPeakMemory(MB)"], "memory"),
        (["TransformationTime(ms)"], "transform_time"),
        (["EdgeCoverage(%)", "EdgeCoverage"], "edge_cov"),
    ]
    extra_map = {}
    for cands, label in extra_candidates:
        col = find_column(df, cands)
        if col and col in df.columns:
            agg_dict[col] = 'median'
            extra_map[label] = col

    grouped = df.groupby(pid_col).agg(agg_dict).reset_index()

    rename = {pid_col: 'ProductID', time_col: 'TestGenTime_ms'}
    if vert_col and vert_col in grouped.columns: rename[vert_col] = 'Vertices'
    if edge_col and edge_col in grouped.columns: rename[edge_col] = 'Edges'
    if 'exec_time' in extra_map and extra_map['exec_time'] in grouped.columns:
        rename[extra_map['exec_time']] = 'ExecTime_ms'
    if 'edge_cov' in extra_map and extra_map['edge_cov'] in grouped.columns:
        rename[extra_map['edge_cov']] = 'EdgeCoverage'
    if 'test_cases' in extra_map and extra_map['test_cases'] in grouped.columns:
        rename[extra_map['test_cases']] = 'TestCases'
    if 'test_events' in extra_map and extra_map['test_events'] in grouped.columns:
        rename[extra_map['test_events']] = 'TestEvents'
    if 'memory' in extra_map and extra_map['memory'] in grouped.columns:
        rename[extra_map['memory']] = 'PeakMemory_MB'

    grouped = grouped.rename(columns=rename)
    return grouped


# ─── Analysis Functions ──────────────────────────────────────────────────────

def analysis_1_complexity_summary(data, output_dir):
    print("\n=== Analysis 1: Per-Product Model Complexity Summary ===")
    rows = []
    for approach in ["ESG-Fx", "EFG", "RandomWalk"]:
        levels = data[data['Approach'] == approach]['Level'].unique()
        for level in sorted(levels):
            subset = data[(data['Approach'] == approach) & (data['Level'] == level)]
            for spl in SPL_ORDER:
                s = subset[subset['SPL'] == spl]
                if s.empty: continue
                avg_e = s['Edges'].mean() if 'Edges' in s else np.nan
                rows.append({
                    'SPL': spl, 'Scale': SPL_SCALE[spl],
                    'Approach': approach, 'Level': level,
                    'N_Products': len(s),
                    'Avg_Vertices': round(s['Vertices'].mean(), 1) if 'Vertices' in s else '',
                    'Avg_Edges': round(avg_e, 1) if not np.isnan(avg_e) else '',
                    'Median_Tgen_ms': round(s['TestGenTime_ms'].median(), 2),
                    'Mean_Tgen_ms': round(s['TestGenTime_ms'].mean(), 2),
                    'Total_Tgen_ms': round(s['TestGenTime_ms'].sum(), 2),
                    'Time_Per_Edge_ms': round(s['TestGenTime_ms'].mean() / avg_e, 4) if avg_e > 0 else ''
                })

    df = pd.DataFrame(rows)
    df.to_csv(output_dir / "per_product_complexity_summary.csv", index=False)
    print(f"  Saved: per_product_complexity_summary.csv")
    return df


def analysis_2_scatter(data, output_dir):
    print("\n=== Analysis 2: Scatter Plots (Edges vs. T_gen) ===")
    for approach in ["ESG-Fx", "EFG", "RandomWalk"]:
        levels = sorted(data[data['Approach'] == approach]['Level'].unique())
        for level in levels:
            subset = data[(data['Approach'] == approach) & (data['Level'] == level)]
            if subset.empty or 'Edges' not in subset or subset['Edges'].isna().all():
                continue

            fig, ax = plt.subplots(figsize=(10, 7))
            for spl in SPL_ORDER:
                s = subset[subset['SPL'] == spl]
                if s.empty: continue
                ax.scatter(s['Edges'], s['TestGenTime_ms'],
                           c=SPL_COLORS[spl], label=f"{spl} ({SPL_SCALE[spl]})",
                           alpha=0.6, s=30, edgecolors='white', linewidth=0.3)

            ax.set_xlabel('Edges per product', fontsize=12)
            ax.set_ylabel('Test Generation Time (ms)', fontsize=12)
            ax.set_title(approach_title(approach, level),
                         fontsize=13, )
            ax.legend(fontsize=9, loc='upper left')
            ax.set_xscale('log'); ax.set_yscale('log')
            ax.grid(True, alpha=0.3)

            valid = subset.dropna(subset=['Edges', 'TestGenTime_ms'])
            if len(valid) > 3:
                rho, p = stats.spearmanr(valid['Edges'], valid['TestGenTime_ms'])
                ax.text(0.98, 0.02, f"Spearman ρ = {rho:.3f} (p = {p:.2e})",
                        transform=ax.transAxes, ha='right', va='bottom', fontsize=10,
                        bbox=dict(boxstyle='round', facecolor='wheat', alpha=0.8))

            plt.tight_layout()
            fig.savefig(output_dir / f"scatter_edges_vs_tgen_{approach}_{level}.png",
                        dpi=150, bbox_inches='tight')
            plt.close(fig)
            print(f"  Saved: scatter_edges_vs_tgen_{approach}_{level}.png")


def analysis_3_boxplots(data, output_dir):
    print("\n=== Analysis 3: Box Plots ===")
    for approach in ["ESG-Fx", "EFG", "RandomWalk"]:
        levels = sorted(data[data['Approach'] == approach]['Level'].unique())
        for level in levels:
            subset = data[(data['Approach'] == approach) & (data['Level'] == level)]
            if subset.empty: continue

            fig, ax = plt.subplots(figsize=(12, 6))
            plot_data, labels, colors = [], [], []
            for spl in SPL_ORDER:
                vals = subset[subset['SPL'] == spl]['TestGenTime_ms'].dropna()
                if vals.empty: continue
                plot_data.append(vals.values)
                labels.append(f"{spl}\n({len(vals)} prod)")
                colors.append(SPL_COLORS[spl])

            if not plot_data: plt.close(fig); continue

            bp = ax.boxplot(plot_data, labels=labels, patch_artist=True,
                            showfliers=True,
                            flierprops=dict(marker='.', markersize=2, alpha=0.3))
            for patch, c in zip(bp['boxes'], colors):
                patch.set_facecolor(c); patch.set_alpha(0.6)

            ax.set_ylabel('Test Generation Time (ms)', fontsize=11)
            ax.set_title(approach_title(approach, level),
                         fontsize=13, )
            ax.set_yscale('log')
            ax.grid(True, alpha=0.3, axis='y')
            plt.tight_layout()
            fig.savefig(output_dir / f"boxplot_tgen_{approach}_{level}.png",
                        dpi=150, bbox_inches='tight')
            plt.close(fig)
            print(f"  Saved: boxplot_tgen_{approach}_{level}.png")


def analysis_4_correlation(data, output_dir):
    print("\n=== Analysis 4: Correlation Analysis ===")
    rows = []
    for approach in ["ESG-Fx", "EFG", "RandomWalk"]:
        levels = sorted(data[data['Approach'] == approach]['Level'].unique())
        for level in levels:
            subset = data[(data['Approach'] == approach) & (data['Level'] == level)]
            if subset.empty: continue

            for metric in ['Edges', 'Vertices']:
                if metric not in subset: continue
                v = subset.dropna(subset=[metric, 'TestGenTime_ms'])
                if len(v) > 3:
                    rho, p = stats.spearmanr(v[metric], v['TestGenTime_ms'])
                    rows.append({'Approach': approach, 'Level': level, 'SPL': 'ALL',
                                 'Metric': metric, 'rho': round(rho, 4),
                                 'p_value': p, 'N': len(v), 'sig': p < 0.05})

            for spl in SPL_ORDER:
                s = subset[subset['SPL'] == spl]
                if len(s) < 10 or 'Edges' not in s: continue
                v = s.dropna(subset=['Edges', 'TestGenTime_ms'])
                if len(v) > 3 and v['Edges'].std() > 0 and v['TestGenTime_ms'].std() > 0:
                    rho, p = stats.spearmanr(v['Edges'], v['TestGenTime_ms'])
                    rows.append({'Approach': approach, 'Level': level, 'SPL': spl,
                                 'Metric': 'Edges', 'rho': round(rho, 4),
                                 'p_value': p, 'N': len(v), 'sig': p < 0.05})

    cdf = pd.DataFrame(rows)
    cdf.to_csv(output_dir / "spearman_correlations.csv", index=False)
    print(f"  Saved: spearman_correlations.csv")
    return cdf


def analysis_5_svia_vs_tesla(data, output_dir):
    print("\n=== Analysis 5: Syngo.via vs Tesla ===")
    rows = []
    for approach in ["ESG-Fx", "EFG", "RandomWalk"]:
        levels = sorted(data[data['Approach'] == approach]['Level'].unique())
        for level in levels:
            for spl in ["Te", "Svia"]:
                s = data[(data['Approach'] == approach) & (data['Level'] == level) & (data['SPL'] == spl)]
                if s.empty: continue
                avg_e = s['Edges'].mean() if 'Edges' in s else np.nan
                rows.append({
                    'SPL': spl, 'Approach': approach, 'Level': level,
                    'N': len(s),
                    'Med_Vertices': s['Vertices'].median() if 'Vertices' in s else np.nan,
                    'Mean_Edges': round(avg_e, 1) if not np.isnan(avg_e) else np.nan,
                    'Max_Edges': s['Edges'].max() if 'Edges' in s else np.nan,
                    'Median_Tgen': round(s['TestGenTime_ms'].median(), 2),
                    'Mean_Tgen': round(s['TestGenTime_ms'].mean(), 2),
                    'Total_Tgen': round(s['TestGenTime_ms'].sum(), 2),
                    'EdgeCov': round(s['EdgeCoverage'].median(), 2) if 'EdgeCoverage' in s and not s['EdgeCoverage'].isna().all() else np.nan,
                })

    cdf = pd.DataFrame(rows)
    cdf.to_csv(output_dir / "svia_vs_tesla.csv", index=False)
    print(f"  Saved: svia_vs_tesla.csv")


def analysis_6_per_spl(data, base_dir):
    print("\n=== Analysis 6: Per-SPL Plots ===")
    for case_name, short in SPL_NAME_MAPPING.items():
        out = base_dir / case_name / "RQ1" / "PerProductAnalysis"
        out.mkdir(parents=True, exist_ok=True)

        spl_data = data[data['SPL'] == short]
        if spl_data.empty: continue

        for approach in ["ESG-Fx", "EFG", "RandomWalk"]:
            levels = sorted(spl_data[spl_data['Approach'] == approach]['Level'].unique())
            for level in levels:
                s = spl_data[(spl_data['Approach'] == approach) & (spl_data['Level'] == level)]
                if s.empty or 'Edges' not in s or s['Edges'].isna().all(): continue

                fig, ax = plt.subplots(figsize=(8, 6))
                ax.scatter(s['Edges'], s['TestGenTime_ms'],
                           c=SPL_COLORS[short], alpha=0.5, s=25,
                           edgecolors='white', linewidth=0.3)
                ax.set_xlabel('Edges per product', fontsize=11)
                ax.set_ylabel('Test Generation Time (ms)', fontsize=11)
                ax.set_title(f'{short}: {approach_title(approach, level)}',
                             fontsize=12, )

                v = s.dropna(subset=['Edges', 'TestGenTime_ms'])
                if len(v) > 3 and v['Edges'].std() > 0:
                    rho, p = stats.spearmanr(v['Edges'], v['TestGenTime_ms'])
                    ax.text(0.98, 0.02, f"ρ = {rho:.3f} (p = {p:.2e})",
                            transform=ax.transAxes, ha='right', va='bottom', fontsize=10,
                            bbox=dict(boxstyle='round', facecolor='wheat', alpha=0.8))

                ax.grid(True, alpha=0.3)
                rng = s['Edges'].max() / max(s['Edges'].min(), 1)
                if rng > 10: ax.set_xscale('log')
                rng_t = s['TestGenTime_ms'].max() / max(s['TestGenTime_ms'].min(), 0.01)
                if rng_t > 10: ax.set_yscale('log')

                plt.tight_layout()
                fig.savefig(out / f"scatter_{approach}_{level}_edges_vs_tgen.png",
                            dpi=150, bbox_inches='tight')
                plt.close(fig)

        print(f"  {short}: plots saved")


# ─── Manuscript Figures ──────────────────────────────────────────────────────

def generate_fig5(data, output_dir):
    """Fig 5: Two-panel scatter (Proposed L2 + Structural L2) for the manuscript."""
    print("\n=== Generating fig5 (two-panel scatter for manuscript) ===")

    fig, axes = plt.subplots(1, 2, figsize=(16, 7))

    panels = [("ESG-Fx", "L2"), ("EFG", "L2")]
    panel_labels = ["(a)", "(b)"]

    for idx, ((approach, level), ax) in enumerate(zip(panels, axes)):
        subset = data[(data['Approach'] == approach) & (data['Level'] == level)]

        for spl in SPL_ORDER:
            s = subset[subset['SPL'] == spl]
            if s.empty: continue
            ax.scatter(s['Edges'], s['TestGenTime_ms'],
                       c=SPL_COLORS[spl], label=f"{spl} ({SPL_SCALE[spl]})",
                       alpha=0.6, s=30, edgecolors='white', linewidth=0.3)

        ax.set_xlabel('Edges per product', fontsize=12)
        ax.set_ylabel('Test Generation Time (ms)', fontsize=12)
        ax.set_title(f'{panel_labels[idx]} {approach_title(approach, "L2")}', fontsize=13)

        ax.legend(fontsize=8, loc='upper left')
        ax.set_xscale('log'); ax.set_yscale('log')
        ax.grid(True, alpha=0.3)

        valid = subset.dropna(subset=['Edges', 'TestGenTime_ms'])
        if len(valid) > 3:
            rho, p = stats.spearmanr(valid['Edges'], valid['TestGenTime_ms'])
            ax.text(0.98, 0.02, f"Spearman ρ = {rho:.3f} (p = {p:.2e})",
                    transform=ax.transAxes, ha='right', va='bottom', fontsize=10,
                    bbox=dict(boxstyle='round', facecolor='wheat', alpha=0.8))

    plt.tight_layout()
    fig.savefig(output_dir / "fig5.png", dpi=200, bbox_inches='tight')
    plt.close(fig)
    print(f"  Saved: fig5.png")


# ─── Main ────────────────────────────────────────────────────────────────────

def main():
    print("=" * 70)
    print("RQ1 Per-Product Analysis")
    print("=" * 70)

    cross_out = BASE_DIR / "_CrossSPL_Analysis" / "RQ1" / "PerProductAnalysis"
    cross_out.mkdir(parents=True, exist_ok=True)

    # ── Load all data (thesis version = includes L1) ──
    all_data = []
    for case_name, short in SPL_NAME_MAPPING.items():
        excel_path = BASE_DIR / case_name / f"RQ1_{case_name}_perProduct.xlsx"
        if not excel_path.exists():
            print(f"[SKIP] {case_name}: not found")
            continue
        print(f"[LOAD] {case_name} ({short})")

        all_sheets = SHEETS_ESGFX_THESIS + SHEETS_EFG + SHEETS_RW
        for sheet in all_sheets:
            approach = get_approach_from_sheet(sheet)
            level = get_level_from_sheet(sheet)
            df = read_sheet_safe(str(excel_path), sheet)
            if df is None or df.empty: continue

            medians = compute_per_product_medians(df, approach)
            if medians is None or medians.empty: continue

            medians['SPL'] = short
            medians['Approach'] = approach
            medians['Level'] = level
            medians['Scale'] = SPL_SCALE[short]
            all_data.append(medians)

    if not all_data:
        print("ERROR: No data loaded!")
        sys.exit(1)

    data = pd.concat(all_data, ignore_index=True)

    # ── Save all medians CSV ──
    data.to_csv(cross_out / "all_per_product_medians.csv", index=False)
    print(f"\nTotal records: {len(data)}")

    # ── Run analyses on FULL data (thesis, includes L1) ──
    thesis_out = cross_out / "ForPhDThesis"
    thesis_out.mkdir(parents=True, exist_ok=True)
    analysis_1_complexity_summary(data, thesis_out)
    analysis_2_scatter(data, thesis_out)
    analysis_3_boxplots(data, thesis_out)
    analysis_4_correlation(data, thesis_out)
    analysis_5_svia_vs_tesla(data, thesis_out)

    # ── Run analyses on ARTICLE data (L2-L4 only) ──
    article_data = data[data['Level'] != 'L1']
    analysis_1_complexity_summary(article_data, cross_out)
    analysis_2_scatter(article_data, cross_out)
    analysis_3_boxplots(article_data, cross_out)
    analysis_4_correlation(article_data, cross_out)
    analysis_5_svia_vs_tesla(article_data, cross_out)

    # ── Per-SPL plots (full data) ──
    analysis_6_per_spl(data, BASE_DIR)

    # ── Combined manuscript figures ──
    generate_fig5(article_data, cross_out)

    print("\n" + "=" * 70)
    print("DONE!")
    print(f"  Article outputs:  {cross_out}")
    print(f"  Thesis outputs:   {thesis_out}")
    print(f"  Per-SPL outputs:  {{CaseName}}/RQ1/PerProductAnalysis/")
    print("=" * 70)


if __name__ == "__main__":
    main()