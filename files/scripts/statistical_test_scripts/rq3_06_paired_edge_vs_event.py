#!/usr/bin/env python3
"""
rq3_06_paired_edge_vs_event.py

Step 6 of the RQ3 analysis pipeline.

Tests the advisor's hypothesis: "Event omission faults are detected earlier
(i.e., with smaller detection cost) than Edge omission faults."

For every (SPL, ProductID, TestingApproach) triple there are two paired
measurements:
  - EdgeOmission detection cost
  - EventOmission detection cost
These are paired on the SAME product x SAME test suite, so the correct test
is paired Wilcoxon signed-rank, NOT Mann-Whitney U.

Two metrics are analyzed in separate sheets:
  1. PenalizedPercentageOfSuiteToDetect(%)  (A2: bias-free, preferred)
  2. PercentageOfSuiteToDetect(%)           (legacy: detected-only)

Effect size: Vargha-Delaney A12 (distribution-free, paired data acceptable).
Multiple testing: Benjamini-Hochberg per metric, across (SPL x approach).

Paths (relative to this script's location):
    Input : files/Cases/<SPL>/RQ3_<SPL>_perProduct_rawData.xlsx
            (produced by rq3_01_merge_shards_per_spl.py)
            Sheets read: EdgeOmission, EventOmission
    Output: files/scripts/statistical_test_scripts/rq3_result/
            rq3_paired_edge_vs_event.xlsx

Usage (from any directory):
    python files/scripts/statistical_test_scripts/rq3_06_paired_edge_vs_event.py

Dependencies: pandas, numpy, scipy, openpyxl
"""
from __future__ import annotations

import argparse
from pathlib import Path

import numpy as np
import pandas as pd
from scipy.stats import wilcoxon


# --------------------------------------------------------------------------
# Paths (match rq3_04 / rq3_05 convention)
# --------------------------------------------------------------------------
SCRIPT_DIR = Path(__file__).resolve().parent
SCRIPTS_DIR = SCRIPT_DIR.parent
FILES_DIR = SCRIPTS_DIR.parent
DATA_DIR = FILES_DIR / "Cases"
OUTPUT_DIR = SCRIPT_DIR / "rq3_result"


# --------------------------------------------------------------------------
# Configuration
# --------------------------------------------------------------------------
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

METRICS = [
    ("PenalizedPercentageOfSuiteToDetect(%)", "penalized"),
    ("PercentageOfSuiteToDetect(%)",          "conditional"),
]


# --------------------------------------------------------------------------
# Statistics helpers
# --------------------------------------------------------------------------
def vargha_delaney_a12(x, y):
    """
    A12 = P(X > Y) + 0.5 * P(X = Y).
    Convention here: x = event, y = edge.
    A12 < 0.5  -> event tends to be lower  (event wins on detection cost)
    A12 > 0.5  -> event tends to be higher (edge wins on detection cost)
    A12 = 0.5  -> no effect.
    """
    x = np.asarray(x, dtype=float)
    y = np.asarray(y, dtype=float)
    n_x, n_y = len(x), len(y)
    if n_x == 0 or n_y == 0:
        return np.nan
    diff = x[:, None] - y[None, :]
    gt = int(np.sum(diff > 0))
    eq = int(np.sum(diff == 0))
    return (gt + 0.5 * eq) / (n_x * n_y)


def a12_magnitude(a12):
    """Vargha-Delaney (2000) thresholds on |0.5 - A12|."""
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
    """Linear step-up BH adjusted p-values; NaN inputs pass through."""
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


# --------------------------------------------------------------------------
# Data loading (reads the merger's Excel output)
# --------------------------------------------------------------------------
def _find_raw_file(spl_full_name: str) -> Path:
    """Locate per-product raw data file, tolerating trailing-underscore variant."""
    folder = DATA_DIR / spl_full_name
    candidates = [
        folder / f"RQ3_{spl_full_name}_perProduct_rawData.xlsx",
        folder / f"RQ3_{spl_full_name}_perProduct_rawData_.xlsx",
    ]
    for p in candidates:
        if p.exists():
            return p
    raise FileNotFoundError(
        f"No raw data file for {spl_full_name} in {folder}. "
        f"Expected: RQ3_{spl_full_name}_perProduct_rawData.xlsx"
    )


def load_operator_sheet(spl_full_name: str, operator_sheet: str) -> pd.DataFrame:
    """Load one of {'EdgeOmission', 'EventOmission'} sheets as DataFrame."""
    path = _find_raw_file(spl_full_name)
    xls = pd.ExcelFile(path)
    if operator_sheet not in xls.sheet_names:
        raise ValueError(f"{spl_full_name}: sheet '{operator_sheet}' missing in {path.name}")
    return pd.read_excel(xls, sheet_name=operator_sheet)


# --------------------------------------------------------------------------
# Paired test for a single (SPL, approach) cell
# --------------------------------------------------------------------------
def run_paired_test(merged: pd.DataFrame, metric_col: str, spl: str, approach: str) -> dict:
    edge_col = f"{metric_col}_edge"
    event_col = f"{metric_col}_event"

    pair = merged[[edge_col, event_col]].dropna()
    n = len(pair)

    row = {
        "SPL": spl,
        "TestingApproach": approach,
        "N_pairs": n,
        "median_edge": np.nan,
        "median_event": np.nan,
        "delta_event_minus_edge": np.nan,
        "W": np.nan,
        "p": np.nan,
        "A12_event_vs_edge": np.nan,
        "magnitude": "n/a",
        "winner": "n/a",
    }

    if n < 6:
        return row

    edge_vals = pair[edge_col].to_numpy(dtype=float)
    event_vals = pair[event_col].to_numpy(dtype=float)

    row["median_edge"] = float(np.median(edge_vals))
    row["median_event"] = float(np.median(event_vals))
    row["delta_event_minus_edge"] = float(np.median(event_vals - edge_vals))

    diffs = event_vals - edge_vals
    if np.allclose(diffs, 0):
        row["W"] = 0.0
        row["p"] = 1.0
        row["A12_event_vs_edge"] = 0.5
        row["magnitude"] = "negligible"
        row["winner"] = "tie"
        return row

    try:
        result = wilcoxon(event_vals, edge_vals,
                          zero_method="wilcox", alternative="two-sided")
        W = float(result.statistic)
        p = float(result.pvalue)
    except ValueError:
        W = np.nan
        p = np.nan

    a12 = vargha_delaney_a12(event_vals, edge_vals)
    row["W"] = W
    row["p"] = p
    row["A12_event_vs_edge"] = a12
    row["magnitude"] = a12_magnitude(a12)

    # Lower detection cost = better. A12 < 0.5 -> event is lower -> event wins.
    if not pd.isna(a12):
        if a12 < 0.5:
            row["winner"] = "Event"
        elif a12 > 0.5:
            row["winner"] = "Edge"
        else:
            row["winner"] = "tie"

    return row


# --------------------------------------------------------------------------
# Main pipeline
# --------------------------------------------------------------------------
def main() -> None:
    parser = argparse.ArgumentParser(
        description="Paired Wilcoxon Edge vs Event detection cost (RQ3)."
    )
    parser.add_argument(
        "--alpha",
        type=float,
        default=0.05,
        help="Significance threshold for BH (default 0.05).",
    )
    args = parser.parse_args()

    print(f"Script location : {SCRIPT_DIR}")
    print(f"Data directory  : {DATA_DIR}")
    print(f"Output directory: {OUTPUT_DIR}")
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

    # Load edge and event sheets for every SPL (one-time)
    print("\nLoading per-SPL raw data...")
    edge_data: dict[str, pd.DataFrame] = {}
    event_data: dict[str, pd.DataFrame] = {}
    for spl in SPL_MAPPING.keys():
        try:
            edge_data[spl] = load_operator_sheet(spl, "EdgeOmission")
            event_data[spl] = load_operator_sheet(spl, "EventOmission")
            print(f"  [OK] {spl:<26s} edge={len(edge_data[spl])} event={len(event_data[spl])}")
        except (FileNotFoundError, ValueError) as e:
            print(f"  [SKIP] {spl:<26s} {e}")

    # Per-metric paired analysis
    output_file = OUTPUT_DIR / "rq3_paired_edge_vs_event.xlsx"
    sheets: dict[str, pd.DataFrame] = {}

    for metric_col, metric_short in METRICS:
        print(f"\n=== Metric: {metric_col} ===")
        rows = []

        for spl in SPL_MAPPING.keys():
            ed = edge_data.get(spl)
            ev = event_data.get(spl)
            if ed is None or ev is None:
                continue
            if metric_col not in ed.columns or metric_col not in ev.columns:
                print(f"  [skip] {spl}: column '{metric_col}' missing")
                continue

            ed_sub = ed[["ProductID", "TestingApproach", metric_col]].rename(
                columns={metric_col: f"{metric_col}_edge"}
            )
            ev_sub = ev[["ProductID", "TestingApproach", metric_col]].rename(
                columns={metric_col: f"{metric_col}_event"}
            )
            merged = ed_sub.merge(
                ev_sub, on=["ProductID", "TestingApproach"], how="inner"
            )

            for approach in sorted(merged["TestingApproach"].unique()):
                cell = merged[merged["TestingApproach"] == approach]
                rows.append(run_paired_test(cell, metric_col, spl, approach))

        df = pd.DataFrame(rows)
        if df.empty:
            print(f"  [warn] no rows for metric {metric_col}, sheet skipped")
            continue

        # BH adjust across all testable rows for this metric
        mask = df["p"].notna()
        df["p_BH"] = np.nan
        if mask.any():
            df.loc[mask, "p_BH"] = benjamini_hochberg(df.loc[mask, "p"].values)
        df["significant_BH"] = df["p_BH"] < args.alpha

        col_order = [
            "SPL", "TestingApproach", "N_pairs",
            "median_edge", "median_event", "delta_event_minus_edge",
            "W", "p", "p_BH", "significant_BH",
            "A12_event_vs_edge", "magnitude", "winner",
        ]
        df = df[col_order]
        sheets[f"paired_wilcoxon_{metric_short}"] = df

        # Console summary
        n_total = len(df)
        n_sig = int(df["significant_BH"].sum())
        n_event = int((df["winner"] == "Event").sum())
        n_edge = int((df["winner"] == "Edge").sum())
        n_event_sig = int(((df["winner"] == "Event") & df["significant_BH"]).sum())
        print(f"  tests       : {n_total}")
        print(f"  sig (BH<{args.alpha}): {n_sig}")
        print(f"  event wins  : {n_event}  (sig: {n_event_sig})")
        print(f"  edge wins   : {n_edge}")

    if not sheets:
        print("\nNo output written (no metrics produced any rows).")
        return

    print(f"\nWriting {output_file}...")
    with pd.ExcelWriter(output_file, engine="openpyxl") as writer:
        for sheet_name, df in sheets.items():
            df.to_excel(writer, sheet_name=sheet_name, index=False)
    print(f"Saved: {output_file}")


if __name__ == "__main__":
    main()