#!/usr/bin/env python3
"""
rq5_plots.py — Publication-quality plots (PNG format)
======================================================
All plots saved as PNG for compatibility and easy preview.
"""

import os
import numpy as np

import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
import matplotlib.ticker as mticker

# =============================================================================
# STYLE CONFIGURATION
# =============================================================================

SINGLE_COL_WIDTH = 3.5
DOUBLE_COL_WIDTH = 7.0
DPI = 300

# Grayscale-friendly palette
COLORS = {
    'ESG-Fx_L0': '#888888',   # gray (Random Walk)
    'RandomWalk': '#888888',   # gray (alias for L0)
    'ESG-Fx_L1': '#1f77b4',   # blue
    'ESG-Fx_L2': '#2ca02c',   # green
    'ESG-Fx_L3': '#d62728',   # red
    'ESG-Fx_L4': '#ff7f0e',   # orange
    'EFG_L2':    '#9467bd',   # purple
    'EFG_L3':    '#8c564b',   # brown
    'EFG_L4':    '#e377c2',   # pink
}

MARKERS = {
    'ESG-Fx_L0': 'x',
    'RandomWalk': 'x',
    'ESG-Fx_L1': 'o',
    'ESG-Fx_L2': 's',
    'ESG-Fx_L3': '^',
    'ESG-Fx_L4': 'D',
    'EFG_L2':    'v',
    'EFG_L3':    '<',
    'EFG_L4':    '>',
}


def _normalize_approach_name(approach):
    """
    Normalize approach names for display.
    ESG-Fx_L0 -> RandomWalk
    ESG-Fx_L2 -> ESG-Fx L2
    EFG_L2 -> EFG L2
    """
    if approach == 'ESG-Fx_L0':
        return 'RandomWalk'
    elif approach.startswith('ESG-Fx_'):
        level = approach.split('_L')[1]
        return f'ESG-Fx L{level}'
    elif approach.startswith('EFG_'):
        level = approach.split('_L')[1]
        return f'EFG L{level}'
    else:
        return approach


def _apply_style():
    """Apply matplotlib style."""
    plt.rcParams.update({
        'font.family': 'serif',
        'font.serif': ['Times New Roman', 'DejaVu Serif', 'Bitstream Vera Serif'],
        'font.size': 9,
        'axes.titlesize': 10,
        'axes.labelsize': 9,
        'xtick.labelsize': 8,
        'ytick.labelsize': 8,
        'legend.fontsize': 7.5,
        'figure.dpi': DPI,
        'savefig.dpi': DPI,
        'savefig.bbox': 'tight',
        'axes.grid': True,
        'grid.alpha': 0.3,
        'grid.linewidth': 0.5,
    })


# =============================================================================
# SCATTER PLOTS: Config Similarity vs Test Suite Similarity
# =============================================================================

def plot_scatter_per_approach(all_results, output_dir, spl_name):
    """
    One PNG per approach: two subplots (sequence-level and edge-level).
    """
    _apply_style()

    for approach, data in all_results.items():
        df = data['dataframe']
        corr = data['correlations']

        fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(DOUBLE_COL_WIDTH, 3.0))

        color = COLORS.get(approach, '#333333')
        display_name = _normalize_approach_name(approach)

        # Sequence-level
        ax1.scatter(df['Config_Jaccard'], df['Sequence_Jaccard'],
                    alpha=0.12, s=5, c=color, edgecolors='none')
        rho_seq = corr['Sequence_Jaccard']['spearman_rho']
        ax1.set_xlabel('Configuration Similarity (Jaccard)')
        ax1.set_ylabel('Sequence-Level Similarity (Jaccard)')
        ax1.set_title(f'(a) Sequence-level (ρ = {rho_seq:.3f})')
        ax1.set_xlim(-0.05, 1.05)
        ax1.set_ylim(-0.05, 1.05)

        # Trend line
        if len(df) > 2:
            z = np.polyfit(df['Config_Jaccard'], df['Sequence_Jaccard'], 1)
            p = np.poly1d(z)
            x_line = np.linspace(0, 1, 100)
            ax1.plot(x_line, p(x_line), 'k--', alpha=0.7, linewidth=1.0)

        # Edge-level
        ax2.scatter(df['Config_Jaccard'], df['Edge_Jaccard'],
                    alpha=0.12, s=5, c=color, edgecolors='none')
        rho_edge = corr['Edge_Jaccard']['spearman_rho']
        ax2.set_xlabel('Configuration Similarity (Jaccard)')
        ax2.set_ylabel('Edge-Level Similarity (Jaccard)')
        ax2.set_title(f'(b) Edge-level (ρ = {rho_edge:.3f})')
        ax2.set_xlim(-0.05, 1.05)
        ax2.set_ylim(-0.05, 1.05)

        if len(df) > 2:
            z = np.polyfit(df['Config_Jaccard'], df['Edge_Jaccard'], 1)
            p = np.poly1d(z)
            ax2.plot(x_line, p(x_line), 'k--', alpha=0.7, linewidth=1.0)

        fig.suptitle(f'{spl_name} — {display_name}', fontsize=10, fontweight='bold', y=1.02)
        plt.tight_layout()

        safe_name = approach.replace('-', '').replace('_', '')
        path = os.path.join(output_dir, f'{spl_name}_RQ5_Scatter_{safe_name}.png')
        fig.savefig(path, format='png')
        plt.close(fig)
        print(f"    Saved: {os.path.basename(path)}")

    # Combined overview
    _plot_scatter_combined_png(all_results, output_dir, spl_name)


def _plot_scatter_combined_png(all_results, output_dir, spl_name):
    """Combined scatter overview as PNG."""
    _apply_style()

    approaches = list(all_results.keys())
    n = len(approaches)
    if n == 0:
        return

    fig, axes = plt.subplots(n, 2, figsize=(DOUBLE_COL_WIDTH, 2.5 * n))
    if n == 1:
        axes = axes.reshape(1, -1)

    for i, approach in enumerate(approaches):
        df = all_results[approach]['dataframe']
        corr = all_results[approach]['correlations']
        color = COLORS.get(approach, '#333333')
        display_name = _normalize_approach_name(approach)

        for j, (sim_col, label) in enumerate([
            ('Sequence_Jaccard', 'Sequence-Level'),
            ('Edge_Jaccard', 'Edge-Level'),
        ]):
            ax = axes[i, j]
            ax.scatter(df['Config_Jaccard'], df[sim_col],
                       alpha=0.12, s=4, c=color, edgecolors='none')
            rho = corr[sim_col]['spearman_rho']
            ax.set_title(f'{display_name} — {label} (ρ={rho:.3f})', fontsize=8)
            ax.set_xlim(-0.05, 1.05)
            ax.set_ylim(-0.05, 1.05)

            if i == n - 1:
                ax.set_xlabel('Config. Similarity')
            if j == 0:
                ax.set_ylabel(f'{label} Sim.')

    plt.suptitle(f'{spl_name} — RQ5 Redundancy Analysis', fontsize=10, fontweight='bold')
    plt.tight_layout()

    path = os.path.join(output_dir, f'{spl_name}_RQ5_ScatterOverview.png')
    fig.savefig(path, format='png')
    plt.close(fig)
    print(f"    Saved: {os.path.basename(path)}")


# =============================================================================
# CUMULATIVE EDGE COVERAGE PLOT
# =============================================================================

def plot_cumulative_edge_coverage(cumulative_all, output_dir, spl_name):
    """Cumulative unique edge coverage - PNG format."""
    _apply_style()

    fig, ax = plt.subplots(figsize=(SINGLE_COL_WIDTH, 2.8))

    for approach, curve in cumulative_all.items():
        if not curve:
            continue
        indices, values = zip(*curve)
        color = COLORS.get(approach, '#333333')
        display_name = _normalize_approach_name(approach)
        ax.plot(indices, values, label=display_name, color=color, linewidth=1.2)

    ax.set_xlabel('Number of Products Added')
    ax.set_ylabel('Cumulative Unique Edges')
    ax.set_title(f'{spl_name} — Cumulative Edge Coverage')
    ax.legend(loc='lower right', framealpha=0.9)

    plt.tight_layout()

    path = os.path.join(output_dir, f'{spl_name}_RQ5_CumulativeEdge.png')
    fig.savefig(path, format='png')
    plt.close(fig)
    print(f"    Saved: {os.path.basename(path)}")


# =============================================================================
# UNIQUE SEQUENCE RATIO BOX PLOT
# =============================================================================

def plot_unique_sequence_ratios(unique_ratios_all, output_dir, spl_name):
    """Unique sequence ratio box plot - PNG format."""
    _apply_style()

    fig, ax = plt.subplots(figsize=(SINGLE_COL_WIDTH, 2.8))

    approaches = sorted(unique_ratios_all.keys())
    data = []
    labels = []

    for approach in approaches:
        ratios = [r[2] for r in unique_ratios_all[approach].values()]
        if ratios:
            data.append(ratios)
            display_name = _normalize_approach_name(approach)
            labels.append(display_name)

    bp = ax.boxplot(data, labels=labels, patch_artist=True, widths=0.5,
                     medianprops=dict(color='black', linewidth=1.5))

    for i, approach in enumerate(approaches):
        color = COLORS.get(approach, '#cccccc')
        bp['boxes'][i].set_facecolor(color)
        bp['boxes'][i].set_alpha(0.5)

    ax.set_ylabel('Unique Sequence Ratio')
    ax.set_title(f'{spl_name} — Unique Sequence Ratio per Approach')
    ax.set_ylim(-0.05, 1.05)

    plt.xticks(rotation=30, ha='right')
    plt.tight_layout()

    path = os.path.join(output_dir, f'{spl_name}_RQ5_UniqueRatio.png')
    fig.savefig(path, format='png')
    plt.close(fig)
    print(f"    Saved: {os.path.basename(path)}")