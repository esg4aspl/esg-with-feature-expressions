#!/usr/bin/env python3
"""
rq5_analysis.py — Core analysis engine for RQ5 Test Suite Redundancy
=====================================================================
All computations are approach-agnostic. They operate on the common
representation produced by rq5_readers.py:
  - configs: dict { product: set_of_features }
  - suites:  dict { product: (sequences_set, edges_set) }
"""

import numpy as np
import pandas as pd
from scipy import stats
from collections import defaultdict


# =============================================================================
# JACCARD SIMILARITY
# =============================================================================

def jaccard(set_a, set_b):
    """Jaccard similarity: |A ∩ B| / |A ∪ B|. Returns 1.0 for two empty sets."""
    if not set_a and not set_b:
        return 1.0
    intersection = len(set_a & set_b)
    union = len(set_a | set_b)
    return intersection / union if union > 0 else 0.0


# =============================================================================
# PAIRWISE SIMILARITY COMPUTATION
# =============================================================================

def compute_pairwise_similarities(configs, suites):
    """
    Computes config, sequence-level, and edge-level Jaccard for all pairs of
    products that exist in both configs and suites.
    
    Returns: pd.DataFrame with columns:
      Product_A, Product_B, Config_Jaccard, Sequence_Jaccard, Edge_Jaccard,
      Config_A_Size, Config_B_Size, Suite_A_Sequences, Suite_B_Sequences,
      Suite_A_Edges, Suite_B_Edges
    """
    common = sorted(set(configs.keys()) & set(suites.keys()))
    n = len(common)

    if n < 2:
        return pd.DataFrame()

    rows = []
    total_pairs = n * (n - 1) // 2
    count = 0

    for i in range(n):
        for j in range(i + 1, n):
            pa, pb = common[i], common[j]
            seq_a, edges_a = suites[pa]
            seq_b, edges_b = suites[pb]

            rows.append({
                'Product_A': pa,
                'Product_B': pb,
                'Config_Jaccard': jaccard(configs[pa], configs[pb]),
                'Sequence_Jaccard': jaccard(seq_a, seq_b),
                'Edge_Jaccard': jaccard(edges_a, edges_b),
                'Config_A_Size': len(configs[pa]),
                'Config_B_Size': len(configs[pb]),
                'Suite_A_Sequences': len(seq_a),
                'Suite_B_Sequences': len(seq_b),
                'Suite_A_Edges': len(edges_a),
                'Suite_B_Edges': len(edges_b),
            })

            count += 1
            if count % 100000 == 0:
                print(f"    {count}/{total_pairs} pairs...")

    return pd.DataFrame(rows)


# =============================================================================
# CORRELATION
# =============================================================================

def compute_correlations(df):
    """
    Computes Spearman and Pearson correlations between Config_Jaccard
    and each of Sequence_Jaccard, Edge_Jaccard.
    
    Returns: dict { 'Sequence_Jaccard': {...}, 'Edge_Jaccard': {...} }
    """
    results = {}
    for sim_col in ['Sequence_Jaccard', 'Edge_Jaccard']:
        if len(df) < 3:
            results[sim_col] = {
                'spearman_rho': np.nan, 'spearman_p': np.nan,
                'pearson_r': np.nan, 'pearson_p': np.nan, 'n': len(df)
            }
            continue

        sp_rho, sp_p = stats.spearmanr(df['Config_Jaccard'], df[sim_col])
        pe_r, pe_p = stats.pearsonr(df['Config_Jaccard'], df[sim_col])

        results[sim_col] = {
            'spearman_rho': round(sp_rho, 4),
            'spearman_p': sp_p,
            'pearson_r': round(pe_r, 4),
            'pearson_p': pe_p,
            'n': len(df),
        }

    return results


# =============================================================================
# UNIQUE SEQUENCE RATIO
# =============================================================================

def compute_unique_sequence_ratios(suites):
    """
    For each product, computes the fraction of test sequences that appear
    in that product's suite and in NO other product's suite.
    
    Returns: dict { product: (unique_count, total_count, ratio) }
    """
    # Inverted index: sequence -> set of products that contain it
    seq_to_products = defaultdict(set)
    for product, (sequences, _) in suites.items():
        for seq in sequences:
            seq_to_products[seq].add(product)

    results = {}
    for product, (sequences, _) in suites.items():
        total = len(sequences)
        unique = sum(1 for seq in sequences if len(seq_to_products[seq]) == 1)
        ratio = unique / total if total > 0 else 0.0
        results[product] = (unique, total, ratio)

    return results


# =============================================================================
# CUMULATIVE NEW EDGE COVERAGE
# =============================================================================

def compute_cumulative_edge_coverage(suites, n_permutations=10, seed=42):
    """
    Adds products one by one in random order and tracks cumulative unique edges.
    Averages over n_permutations random orderings.
    
    Returns: list of (product_index, avg_cumulative_unique_edges)
    """
    products = sorted(suites.keys())
    n = len(products)
    if n == 0:
        return []

    rng = np.random.RandomState(seed)
    all_curves = []

    for _ in range(n_permutations):
        perm = rng.permutation(products)
        cumulative = set()
        curve = []
        for product in perm:
            _, edges = suites[product]
            cumulative |= edges
            curve.append(len(cumulative))
        all_curves.append(curve)

    avg_curve = np.mean(all_curves, axis=0)
    return [(i + 1, val) for i, val in enumerate(avg_curve)]


# =============================================================================
# FULL ANALYSIS PIPELINE FOR ONE APPROACH
# =============================================================================

def analyze_approach(configs, suites, approach_name):
    """
    Runs the full RQ5 analysis for a single approach.
    
    Returns: dict with keys:
      'dataframe', 'correlations', 'unique_ratios', 'cumulative_curve',
      'n_products', 'n_pairs'
    or None if insufficient data.
    """
    common = set(configs.keys()) & set(suites.keys())
    if len(common) < 2:
        print(f"  {approach_name}: Only {len(common)} products with both config and suite. Skipping.")
        return None

    print(f"  {approach_name}: {len(common)} products, {len(common)*(len(common)-1)//2} pairs")

    # Pairwise similarities
    df = compute_pairwise_similarities(configs, suites)
    if df.empty:
        return None

    # Correlations
    corr = compute_correlations(df)
    for metric, vals in corr.items():
        print(f"    {metric}: Spearman ρ={vals['spearman_rho']:.4f} (p={vals['spearman_p']:.2e}), "
              f"Pearson r={vals['pearson_r']:.4f}")

    # Unique sequence ratios
    unique_ratios = compute_unique_sequence_ratios(suites)
    avg_unique = np.mean([r[2] for r in unique_ratios.values()])
    print(f"    Avg Unique Sequence Ratio: {avg_unique:.4f}")

    # Cumulative edge coverage
    cumulative = compute_cumulative_edge_coverage(suites)

    return {
        'dataframe': df,
        'correlations': corr,
        'unique_ratios': unique_ratios,
        'cumulative_curve': cumulative,
        'n_products': len(common),
        'n_pairs': len(df),
    }