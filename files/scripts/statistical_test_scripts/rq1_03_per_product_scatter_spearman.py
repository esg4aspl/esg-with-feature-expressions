#!/usr/bin/env python3
"""
rq1_03_per_product_scatter_spearman.py

Step 3 of the RQ1 pipeline — per-product descriptive + Spearman analysis.

Reads files/Cases/<SPL>/RQ1_<SPL>_perProduct.xlsx (produced by rq1_02),
computes per-product medians across 11 runs, and produces:

  1. Summary table: per-product model complexity by SPL
  2. Scatter plots: edges vs. T_gen per product (per approach x level)
  3. Box plots: per-product T_gen distribution across SPLs
  4. Spearman correlation table (pooled and per-SPL)
  5. Syngo.via vs Tesla deep comparison
  6. Per-SPL scatter plots
  7. Manuscript figure 5 (two-panel Proposed vs Structural at L=2)

Paths (relative to this script, matching rq1_04/05/06 convention):
    Script: files/scripts/statistical_test_scripts/rq1_03_per_product_scatter_spearman.py
    Data  : files/Cases/<SPL>/RQ1_<SPL>_perProduct.xlsx
    Out   : files/scripts/statistical_test_scripts/rq1_result/
            rq1_per_product_summary.xlsx      (all tables in one workbook)
            plots/article/scatter/scatter_<approach>_<level>.png
            plots/article/boxplot/boxplot_<approach>_<level>.png
            plots/thesis/...                   (same but includes L1)
            plots/perSPL/<SPL>/scatter_<approach>_<level>.png
            plots/fig5.png                     (manuscript figure)

Two scopes: Article (L2-L4 only) and ForPhDThesis (includes L1).

Usage:
    python rq1_03_per_product_scatter_spearman.py
"""

import sys
import warnings
import textwrap
import numpy as np
import pandas as pd
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
from scipy import stats
from pathlib import Path

warnings.filterwarnings('ignore')


# ─── Figure save helper (PDF + PNG) ─────────────────────────────────────────
# Embed fonts in PDFs as TrueType (Type 42) instead of Type 3 outlines.
# This is required by virtually all journals and keeps PDF text searchable.
plt.rcParams['pdf.fonttype'] = 42
plt.rcParams['ps.fonttype']  = 42


def save_figure(fig, output_path, png_dpi=300):
    """Save a matplotlib figure as both PDF (vector, primary output) and
    PNG (raster, for preview / non-LaTeX use).

    Parameters
    ----------
    fig : matplotlib.figure.Figure
        The figure to save.
    output_path : str or Path
        The output path with or without an extension. The extension will
        be replaced with ``.pdf`` and ``.png`` respectively.
    png_dpi : int
        DPI for the PNG output. PDF is vector and ignores DPI.
    """
    output_path = Path(output_path)
    stem = output_path.with_suffix('')
    fig.savefig(stem.with_suffix('.pdf'), bbox_inches='tight')
    fig.savefig(stem.with_suffix('.png'), dpi=png_dpi, bbox_inches='tight')


# ─── Paths (match rq1_04/05/06 convention) ─────────────────────────────────
SCRIPT_DIR = Path(__file__).resolve().parent
SCRIPTS_DIR = SCRIPT_DIR.parent
FILES_DIR = SCRIPTS_DIR.parent
DATA_DIR = FILES_DIR / "Cases"
OUTPUT_DIR = SCRIPT_DIR / "rq1_result"
PLOTS_DIR = OUTPUT_DIR / "plots"


# ─── Configuration ──────────────────────────────────────────────────────────
SPL_NAME_MAPPING = {
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

# Full SPL display names — used in per-SPL plot titles where there is room.
# In legends across many SPLs we keep the short codes for compactness.
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
SPL_SCALE = {
    "SVM": "Small", "eM": "Small", "El": "Small",
    "BAv2": "Medium", "SAS": "Medium",
    "Te": "Large", "Svia": "Large", "HS": "Large",
}

SPL_COLORS = {
    "SVM": "#1b9e77", "eM": "#d95f02", "El": "#2115c9",
    "BAv2": "#e7298a", "SAS": "#66a61e",
    "Te": "#e6ab02", "Svia": "#a6761d", "HS": "#666666",
}

APPROACH_LABEL = {
    "ESG-Fx":     "Model Once, Generate Any",
    "EFG":        "Structural Baseline",
    "RandomWalk": "Stochastic Baseline",
}


# Used when the legend/title would be too long; legend goes outside the
# plotting area in this script, so the long labels are kept verbatim.
APPROACH_LABEL_SHORT = {
    "ESG-Fx":     "Model Once, Generate Any",
    "EFG":        "Structural Baseline",
    "RandomWalk": "Stochastic Baseline",
}


def approach_title(approach, level):
    label = APPROACH_LABEL.get(approach, approach)
    # RandomWalk has no coverage-level parameter; show the approach name
    # alone rather than a meaningless "L = 0".
    if approach == "RandomWalk":
        return label
    lv = level.replace("L", "L = ")
    return f"{label} — {lv}"


SHEETS_ESGFX_THESIS = ["ESG-Fx_L1", "ESG-Fx_L2", "ESG-Fx_L3", "ESG-Fx_L4"]
SHEETS_EFG = ["EFG_L2", "EFG_L3", "EFG_L4"]
SHEETS_RW = ["RandomWalk_L0"]


# ─── Utility Functions ──────────────────────────────────────────────────────

def parse_european_number(val):
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


def read_sheet_safe(excel_path, sheet_name):
    try:
        df = pd.read_excel(excel_path, sheet_name=sheet_name, engine='openpyxl')
        skip_cols = {'ProductID', 'Status', 'ErrorReason', 'Approach',
                     'Coverage Type', 'Coverage Level', 'SafetyLimitHit',
                     'RunID', 'Run ID'}
        for col in df.columns:
            if col not in skip_cols:
                df[col] = df[col].apply(parse_european_number)
        return df
    except Exception:
        return None


def get_approach_from_sheet(sheet_name):
    if sheet_name.startswith("ESG-Fx"):
        return "ESG-Fx"
    if sheet_name.startswith("EFG"):
        return "EFG"
    if sheet_name.startswith("Random"):
        return "RandomWalk"
    return None


def get_level_from_sheet(sheet_name):
    for lv in ["L1", "L2", "L3", "L4", "L0"]:
        if lv in sheet_name:
            return lv
    return None


def find_column(df, candidates):
    for c in candidates:
        if c in df.columns:
            return c
    for c in candidates:
        for col in df.columns:
            if col.strip().lower() == c.lower():
                return col
    return None


def get_time_col(df, approach):
    # T_gen scope-symmetric. ESG-Fx and RandomWalk both expose
    # TotalTestGenTime(ms); EFG exposes GuitarGenTime(ms) which is already
    # disk-to-disk inside its Java 8 sub-process.
    mapping = {
        "ESG-Fx":     ["TotalTestGenTime(ms)"],
        "EFG":        ["GuitarGenTime(ms)"],
        "RandomWalk": ["TotalTestGenTime(ms)"],
    }
    return find_column(df, mapping.get(approach, ["TotalTestGenTime(ms)"]))


def get_vertices_col(df, approach):
    mapping = {
        "ESG-Fx":     ["NumberOfESGFxVertices"],
        "EFG":        ["NumberOfEFGVertices"],
        "RandomWalk": ["NumberOfESGFxVertices"],
    }
    return find_column(df, mapping.get(approach, ["NumberOfESGFxVertices"]))


def get_edges_col(df, approach):
    mapping = {
        "ESG-Fx":     ["NumberOfESGFxEdges"],
        "EFG":        ["NumberOfEFGEdges"],
        "RandomWalk": ["NumberOfESGFxEdges"],
    }
    return find_column(df, mapping.get(approach, ["NumberOfESGFxEdges"]))


def get_product_id_col(df):
    return find_column(df, ["ProductID", "Product ID", "productId"])


def compute_per_product_medians(df, approach):
    pid_col = get_product_id_col(df)
    time_col = get_time_col(df, approach)
    vert_col = get_vertices_col(df, approach)
    edge_col = get_edges_col(df, approach)

    if pid_col is None or time_col is None:
        return None

    agg_dict = {time_col: 'median'}
    if vert_col and vert_col in df.columns:
        agg_dict[vert_col] = 'median'
    if edge_col and edge_col in df.columns:
        agg_dict[edge_col] = 'median'

    extra_candidates = [
        (["NumberOfESGFxTestCases", "NumberOfEFGTestCases"], "test_cases"),
        (["NumberOfESGFxTestEvents", "NumberOfEFGTestEvents"], "test_events"),
        (["TestExecTime(ms)"], "exec_time"),
        (["TestGenPeakMemory(MB)"], "memory"),
        (["TransformationTime(ms)"], "transform_time"),
        (["EdgeCoverage(%)"], "edge_cov"),
    ]
    extra_map = {}
    for cands, label in extra_candidates:
        col = find_column(df, cands)
        if col and col in df.columns:
            agg_dict[col] = 'median'
            extra_map[label] = col

    grouped = df.groupby(pid_col).agg(agg_dict).reset_index()

    rename = {pid_col: 'ProductID', time_col: 'TestGenTime_ms'}
    if vert_col and vert_col in grouped.columns:
        rename[vert_col] = 'Vertices'
    if edge_col and edge_col in grouped.columns:
        rename[edge_col] = 'Edges'
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


# ─── Analyses (return DataFrames) ───────────────────────────────────────────

def analysis_complexity_summary(data):
    rows = []
    for approach in ["ESG-Fx", "EFG", "RandomWalk"]:
        levels = data[data['Approach'] == approach]['Level'].unique()
        for level in sorted(levels):
            subset = data[(data['Approach'] == approach) & (data['Level'] == level)]
            for spl in SPL_ORDER:
                s = subset[subset['SPL'] == spl]
                if s.empty:
                    continue
                avg_e = s['Edges'].mean() if 'Edges' in s else np.nan
                rows.append({
                    'SPL': spl, 'Scale': SPL_SCALE[spl],
                    'Approach': approach, 'Level': level,
                    'N_Products': len(s),
                    'Avg_Vertices': round(s['Vertices'].mean(), 1) if 'Vertices' in s else np.nan,
                    'Avg_Edges': round(avg_e, 1) if not np.isnan(avg_e) else np.nan,
                    'Median_Tgen_ms': round(s['TestGenTime_ms'].median(), 2),
                    'Mean_Tgen_ms': round(s['TestGenTime_ms'].mean(), 2),
                    'Total_Tgen_ms': round(s['TestGenTime_ms'].sum(), 2),
                    'Time_Per_Edge_ms': round(s['TestGenTime_ms'].mean() / avg_e, 4) if avg_e and avg_e > 0 else np.nan,
                })
    return pd.DataFrame(rows)


def analysis_correlation(data):
    rows = []
    for approach in ["ESG-Fx", "EFG", "RandomWalk"]:
        levels = sorted(data[data['Approach'] == approach]['Level'].unique())
        for level in levels:
            subset = data[(data['Approach'] == approach) & (data['Level'] == level)]
            if subset.empty:
                continue

            for metric in ['Edges', 'Vertices']:
                if metric not in subset:
                    continue
                v = subset.dropna(subset=[metric, 'TestGenTime_ms'])
                if len(v) > 3:
                    rho, p = stats.spearmanr(v[metric], v['TestGenTime_ms'])
                    rows.append({'Approach': approach, 'Level': level, 'SPL': 'ALL',
                                 'Metric': metric, 'rho': round(rho, 4),
                                 'p_value': p, 'N': len(v), 'sig': p < 0.05})

            for spl in SPL_ORDER:
                s = subset[subset['SPL'] == spl]
                if len(s) < 10 or 'Edges' not in s:
                    continue
                v = s.dropna(subset=['Edges', 'TestGenTime_ms'])
                if len(v) > 3 and v['Edges'].std() > 0 and v['TestGenTime_ms'].std() > 0:
                    rho, p = stats.spearmanr(v['Edges'], v['TestGenTime_ms'])
                    rows.append({'Approach': approach, 'Level': level, 'SPL': spl,
                                 'Metric': 'Edges', 'rho': round(rho, 4),
                                 'p_value': p, 'N': len(v), 'sig': p < 0.05})

    return pd.DataFrame(rows)


def analysis_svia_vs_tesla(data):
    rows = []
    for approach in ["ESG-Fx", "EFG", "RandomWalk"]:
        levels = sorted(data[data['Approach'] == approach]['Level'].unique())
        for level in levels:
            for spl in ["Te", "Svia"]:
                s = data[(data['Approach'] == approach) & (data['Level'] == level) & (data['SPL'] == spl)]
                if s.empty:
                    continue
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
                    'EdgeCov': round(s['EdgeCoverage'].median(), 2)
                        if 'EdgeCoverage' in s and not s['EdgeCoverage'].isna().all() else np.nan,
                })
    return pd.DataFrame(rows)


# ─── Plots ──────────────────────────────────────────────────────────────────

def plot_scatter(data, scope_dir):
    out = scope_dir / "scatter"
    out.mkdir(parents=True, exist_ok=True)
    for approach in ["ESG-Fx", "EFG", "RandomWalk"]:
        levels = sorted(data[data['Approach'] == approach]['Level'].unique())
        for level in levels:
            subset = data[(data['Approach'] == approach) & (data['Level'] == level)]
            if subset.empty or 'Edges' not in subset or subset['Edges'].isna().all():
                continue

            fig, ax = plt.subplots(figsize=(11, 6.5))
            for spl in SPL_ORDER:
                s = subset[subset['SPL'] == spl]
                if s.empty:
                    continue
                
                # Genişliği artırdık ki legend içinde metin alt satıra taşmasın
                full_name = textwrap.fill(SPL_FULL_NAME.get(spl, spl), width=40)
                
                ax.scatter(s['Edges'], s['TestGenTime_ms'],
                           c=SPL_COLORS[spl],
                           label=f"{full_name} ({SPL_SCALE[spl]})",
                           alpha=0.6, s=30, edgecolors='white', linewidth=0.3)

            ax.set_xlabel('Edges per product', fontsize=12)
            ax.set_ylabel('Test Generation Time (ms)', fontsize=12)
            ax.set_title(approach_title(approach, level), fontsize=13)
            
            # Sığması için ncol=3 yapıldı
            ax.legend(fontsize=9, loc='upper center', ncol=3,
                      bbox_to_anchor=(0.5, -0.12),
                      frameon=True, borderaxespad=0.0)
            ax.set_xscale('log')
            ax.set_yscale('log')
            ax.grid(True, alpha=0.3)

            valid = subset.dropna(subset=['Edges', 'TestGenTime_ms'])
            if len(valid) > 3:
                rho, p = stats.spearmanr(valid['Edges'], valid['TestGenTime_ms'])
                ax.text(0.98, 0.02,
                        f"Spearman $\\rho$ = {rho:.3f} (p = {p:.2e})",
                        transform=ax.transAxes, ha='right', va='bottom', fontsize=10,
                        bbox=dict(boxstyle='round', facecolor='wheat', alpha=0.85))

            plt.tight_layout()
            save_figure(fig, out / f"scatter_{approach}_{level}", png_dpi=150)
            plt.close(fig)


def plot_fig5(data, plots_dir):
    fig, axes = plt.subplots(1, 2, figsize=(15, 6.5))
    panels = [("ESG-Fx", "L2"), ("EFG", "L2")]
    panel_labels = ["(a)", "(b)"]

    for idx, ((approach, level), ax) in enumerate(zip(panels, axes)):
        subset = data[(data['Approach'] == approach) & (data['Level'] == level)]

        for spl in SPL_ORDER:
            s = subset[subset['SPL'] == spl]
            if s.empty:
                continue
                
            # Genişliği artırdık ki legend içinde metin alt satıra taşmasın
            full_name = textwrap.fill(SPL_FULL_NAME.get(spl, spl), width=40)
            
            ax.scatter(s['Edges'], s['TestGenTime_ms'],
                       c=SPL_COLORS[spl],
                       label=f"{full_name} ({SPL_SCALE[spl]})",
                       alpha=0.6, s=30, edgecolors='white', linewidth=0.3)

        ax.set_xlabel('Edges per product', fontsize=12)
        ax.set_ylabel('Test Generation Time (ms)', fontsize=12)
        ax.set_title(f'{panel_labels[idx]} {approach_title(approach, "L2")}',
                     fontsize=13)
        ax.set_xscale('log')
        ax.set_yscale('log')
        ax.grid(True, alpha=0.3)

        valid = subset.dropna(subset=['Edges', 'TestGenTime_ms'])
        if len(valid) > 3:
            rho, p = stats.spearmanr(valid['Edges'], valid['TestGenTime_ms'])
            ax.text(0.98, 0.02,
                    f"Spearman $\\rho$ = {rho:.3f} (p = {p:.2e})",
                    transform=ax.transAxes, ha='right', va='bottom', fontsize=10,
                    bbox=dict(boxstyle='round', facecolor='wheat', alpha=0.85))

    handles, labels = axes[0].get_legend_handles_labels()
    # Fig5 geniş olduğu için ncol=4 sorun yaratmaz
    fig.legend(handles, labels, loc='lower center', ncol=4,
               fontsize=10, frameon=True, bbox_to_anchor=(0.5, -0.04))
    plt.tight_layout(rect=[0, 0.10, 1, 1])
    save_figure(fig, plots_dir / "fig5", png_dpi=200)
    plt.close(fig)

def plot_fig_scaling_grid(data, plots_dir):
    """
    Manuscript figure: 7-panel grid showing per-product scaling for all
    three approaches across all coverage levels.

    Layout (3 rows x 3 cols, with last row containing only 1 panel centered):
        Row 1: Model Once L=2, L=3, L=4
        Row 2: Structural  L=2, L=3, L=4
        Row 3: Stochastic (centered, single panel)

    A single legend at the bottom serves all panels.
    """
    from matplotlib.gridspec import GridSpec
    from matplotlib.ticker import LogFormatterMathtext, NullFormatter

    fig = plt.figure(figsize=(17, 14))
    gs = GridSpec(3, 6, figure=fig, hspace=0.42, wspace=0.55,
                  bottom=0.10, top=0.96, left=0.06, right=0.98)

    panel_specs = [
        (0, slice(0, 2), "ESG-Fx",     "L2", "(a) Model Once, Generate Any — L = 2"),
        (0, slice(2, 4), "ESG-Fx",     "L3", "(b) Model Once, Generate Any — L = 3"),
        (0, slice(4, 6), "ESG-Fx",     "L4", "(c) Model Once, Generate Any — L = 4"),
        (1, slice(0, 2), "EFG",        "L2", "(d) Structural Baseline — L = 2"),
        (1, slice(2, 4), "EFG",        "L3", "(e) Structural Baseline — L = 3"),
        (1, slice(4, 6), "EFG",        "L4", "(f) Structural Baseline — L = 4"),
        (2, slice(2, 4), "RandomWalk", "L0", "(g) Stochastic Baseline"),
    ]

    last_legend_handles = None
    last_legend_labels  = None

    for (row, col, approach, level, label) in panel_specs:
        ax = fig.add_subplot(gs[row, col])
        subset = data[(data['Approach'] == approach) & (data['Level'] == level)]

        for spl in SPL_ORDER:
            s = subset[subset['SPL'] == spl]
            if s.empty:
                continue
            full_name = SPL_FULL_NAME.get(spl, spl)
            ax.scatter(s['Edges'], s['TestGenTime_ms'],
                       c=SPL_COLORS[spl],
                       label=f"{full_name} ({SPL_SCALE[spl]})",
                       alpha=0.55, s=22, edgecolors='white', linewidth=0.3)

        ax.set_xlabel('Edges per product', fontsize=10)
        ax.set_ylabel('Test Generation Time (ms)', fontsize=10)
        ax.set_title(label, fontsize=11)
        ax.set_xscale('log')
        ax.set_yscale('log')
        ax.grid(True, alpha=0.3)
        ax.tick_params(axis='both', labelsize=9)

        # Compact tick formatting on log axes.
        # The default LogFormatter renders intermediate ticks like
        # "3 × 10^3" which expands the gutter and pushes the ylabel
        # of the next panel off-axis.  We keep only base-10 ticks
        # ("10^3") on both axes and suppress minor labels.
        ax.yaxis.set_major_formatter(
            LogFormatterMathtext(base=10.0, labelOnlyBase=True))
        ax.xaxis.set_major_formatter(
            LogFormatterMathtext(base=10.0, labelOnlyBase=True))
        ax.yaxis.set_minor_formatter(NullFormatter())
        ax.xaxis.set_minor_formatter(NullFormatter())

        valid = subset.dropna(subset=['Edges', 'TestGenTime_ms'])
        if len(valid) > 3:
            rho, p = stats.spearmanr(valid['Edges'], valid['TestGenTime_ms'])
            ax.text(0.97, 0.03,
                    f"$\\rho$ = {rho:.3f}\n(p = {p:.1e})",
                    transform=ax.transAxes, ha='right', va='bottom', fontsize=9,
                    bbox=dict(boxstyle='round', facecolor='wheat', alpha=0.85))

        h, l = ax.get_legend_handles_labels()
        if h:
            last_legend_handles = h
            last_legend_labels  = l

    if last_legend_handles is not None:
        fig.legend(last_legend_handles, last_legend_labels,
                   loc='lower center', ncol=4, fontsize=10, frameon=True,
                   bbox_to_anchor=(0.5, 0.005))

    save_figure(fig, plots_dir / "fig_scaling_grid", png_dpi=200)
    plt.close(fig)
    
def plot_boxplots(data, scope_dir):
    out = scope_dir / "boxplot"
    out.mkdir(parents=True, exist_ok=True)
    for approach in ["ESG-Fx", "EFG", "RandomWalk"]:
        levels = sorted(data[data['Approach'] == approach]['Level'].unique())
        for level in levels:
            subset = data[(data['Approach'] == approach) & (data['Level'] == level)]
            if subset.empty:
                continue

            fig, ax = plt.subplots(figsize=(12, 6))
            plot_data, labels, colors = [], [], []
            for spl in SPL_ORDER:
                vals = subset[subset['SPL'] == spl]['TestGenTime_ms'].dropna()
                if vals.empty:
                    continue
                plot_data.append(vals.values)
                
                # Wrap long x-axis labels
                full_name = textwrap.fill(SPL_FULL_NAME.get(spl, spl), width=12)
                
                if spl in ["Te", "Svia", "HS"]:
                    labels.append(f"{full_name}\n({len(vals)} samples)")
                else:
                    labels.append(f"{full_name}\n({len(vals)} products)")
                    
                colors.append(SPL_COLORS[spl])

            if not plot_data:
                plt.close(fig)
                continue

            bp = ax.boxplot(plot_data, labels=labels, patch_artist=True,
                            showfliers=True,
                            flierprops=dict(marker='.', markersize=2, alpha=0.3))
            for patch, c in zip(bp['boxes'], colors):
                patch.set_facecolor(c)
                patch.set_alpha(0.6)

            ax.set_ylabel('Test Generation Time (ms)', fontsize=11)
            ax.set_title(approach_title(approach, level), fontsize=13)
            ax.set_yscale('log')
            ax.grid(True, alpha=0.3, axis='y')
            plt.tight_layout()
            save_figure(fig, out / f"boxplot_{approach}_{level}", png_dpi=150)
            plt.close(fig)

def plot_per_spl(data, plots_dir):
    """Per-SPL scatter plots go to plots/perSPL/<SPL>/"""
    per_spl_dir = plots_dir / "perSPL"
    for case_name, short in SPL_NAME_MAPPING.items():
        out = per_spl_dir / short
        out.mkdir(parents=True, exist_ok=True)

        spl_data = data[data['SPL'] == short]
        if spl_data.empty:
            continue

        for approach in ["ESG-Fx", "EFG", "RandomWalk"]:
            levels = sorted(spl_data[spl_data['Approach'] == approach]['Level'].unique())
            for level in levels:
                s = spl_data[(spl_data['Approach'] == approach) & (spl_data['Level'] == level)]
                if s.empty or 'Edges' not in s or s['Edges'].isna().all():
                    continue

                fig, ax = plt.subplots(figsize=(8, 6))
                ax.scatter(s['Edges'], s['TestGenTime_ms'],
                           c=SPL_COLORS[short], alpha=0.5, s=25,
                           edgecolors='white', linewidth=0.3)
                ax.set_xlabel('Edges per product', fontsize=11)
                ax.set_ylabel('Test Generation Time (ms)', fontsize=11)
                
                # Başlık çok uzun olmasın diye \n ile iki satıra böldüm
                full_name = SPL_FULL_NAME.get(short, short)
                ax.set_title(f'{full_name}:\n{approach_title(approach, level)}', fontsize=12)

                v = s.dropna(subset=['Edges', 'TestGenTime_ms'])
                if len(v) > 3 and v['Edges'].std() > 0:
                    rho, p = stats.spearmanr(v['Edges'], v['TestGenTime_ms'])
                    ax.text(0.98, 0.02, f"ρ = {rho:.3f} (p = {p:.2e})",
                            transform=ax.transAxes, ha='right', va='bottom', fontsize=10,
                            bbox=dict(boxstyle='round', facecolor='wheat', alpha=0.8))

                ax.grid(True, alpha=0.3)
                rng = s['Edges'].max() / max(s['Edges'].min(), 1)
                if rng > 10:
                    ax.set_xscale('log')
                rng_t = s['TestGenTime_ms'].max() / max(s['TestGenTime_ms'].min(), 0.01)
                if rng_t > 10:
                    ax.set_yscale('log')

                plt.tight_layout()
                save_figure(fig, out / f"scatter_{approach}_{level}", png_dpi=150)
                plt.close(fig)



# ─── Main ────────────────────────────────────────────────────────────────────

def main():
    print("=" * 70)
    print("rq1_03: Per-Product Scatter + Spearman Analysis")
    print("=" * 70)
    print(f"Data directory   : {DATA_DIR}")
    print(f"Output directory : {OUTPUT_DIR}")

    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    PLOTS_DIR.mkdir(parents=True, exist_ok=True)

    # Load all per-product data
    print("\nLoading per-product data...")
    all_data = []
    for case_name, short in SPL_NAME_MAPPING.items():
        excel_path = DATA_DIR / case_name / f"RQ1_{case_name}_perProduct.xlsx"
        if not excel_path.exists():
            print(f"  [SKIP] {case_name}: not found")
            continue
        print(f"  [LOAD] {case_name} ({short})")

        all_sheets = SHEETS_ESGFX_THESIS + SHEETS_EFG + SHEETS_RW
        for sheet in all_sheets:
            approach = get_approach_from_sheet(sheet)
            level = get_level_from_sheet(sheet)
            df = read_sheet_safe(str(excel_path), sheet)
            if df is None or df.empty:
                continue

            medians = compute_per_product_medians(df, approach)
            if medians is None or medians.empty:
                continue

            medians['SPL'] = short
            medians['Approach'] = approach
            medians['Level'] = level
            medians['Scale'] = SPL_SCALE[short]
            all_data.append(medians)

    if not all_data:
        print("ERROR: No data loaded. Run rq1_02 first.")
        sys.exit(1)

    data = pd.concat(all_data, ignore_index=True)
    print(f"\nTotal per-product records: {len(data)}")

    # Article subset (L2-L4 only)
    article_data = data[data['Level'] != 'L1']

    # ── Build summary workbook ──
    print("\nBuilding summary workbook...")
    output_excel = OUTPUT_DIR / "rq1_per_product_summary.xlsx"

    with pd.ExcelWriter(output_excel, engine="openpyxl") as writer:
        # All per-product medians (long-form data)
        data.to_excel(writer, sheet_name="all_medians", index=False)
        print(f"  sheet 'all_medians': {len(data)} rows")

        # Complexity summary — article scope
        cs_article = analysis_complexity_summary(article_data)
        cs_article.to_excel(writer, sheet_name="complexity_article", index=False)
        print(f"  sheet 'complexity_article': {len(cs_article)} rows")

        # Complexity summary — thesis scope (includes L1)
        cs_thesis = analysis_complexity_summary(data)
        cs_thesis.to_excel(writer, sheet_name="complexity_thesis", index=False)
        print(f"  sheet 'complexity_thesis': {len(cs_thesis)} rows")

        # Spearman correlations — article
        corr_article = analysis_correlation(article_data)
        corr_article.to_excel(writer, sheet_name="spearman_article", index=False)
        print(f"  sheet 'spearman_article': {len(corr_article)} rows")

        # Spearman correlations — thesis
        corr_thesis = analysis_correlation(data)
        corr_thesis.to_excel(writer, sheet_name="spearman_thesis", index=False)
        print(f"  sheet 'spearman_thesis': {len(corr_thesis)} rows")

        # Svia vs Tesla
        svt = analysis_svia_vs_tesla(article_data)
        svt.to_excel(writer, sheet_name="svia_vs_tesla", index=False)
        print(f"  sheet 'svia_vs_tesla': {len(svt)} rows")

    print(f"\nSaved: {output_excel.name}")

    # ── Build plots ──
    print("\nGenerating plots...")
    article_plots_dir = PLOTS_DIR / "article"
    thesis_plots_dir = PLOTS_DIR / "thesis"

    print("  scatter plots (article)...")
    plot_scatter(article_data, article_plots_dir)

    print("  scatter plots (thesis)...")
    plot_scatter(data, thesis_plots_dir)

    print("  box plots (article)...")
    plot_boxplots(article_data, article_plots_dir)

    print("  box plots (thesis)...")
    plot_boxplots(data, thesis_plots_dir)

    print("  per-SPL scatter plots...")
    plot_per_spl(data, PLOTS_DIR)

    print("  manuscript figure 5...")
    plot_fig5(article_data, PLOTS_DIR)

    print("  manuscript figure (scaling grid)...")
    plot_fig_scaling_grid(data, PLOTS_DIR)

    print(f"\nPlots under: {PLOTS_DIR}")
    print("\nrq1_03 DONE.")


if __name__ == "__main__":
    main()