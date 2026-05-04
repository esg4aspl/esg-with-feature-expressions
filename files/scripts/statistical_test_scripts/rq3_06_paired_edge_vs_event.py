#!/usr/bin/env python3
"""
rq3_06_paired_edge_vs_event.py

Step 6 of the RQ3 analysis pipeline.

Tests two related claims about the relationship between the EdgeOmission
operator (the manuscript's primary mutant operator) and the EventOmission
operator (supporting evidence).

Claim 1 -- Subsumption of detection effectiveness
  Prior belief: "A test suite that detects EdgeOmission mutants also detects
  EventOmission mutants." The same product-level test suite is scored
  separately against both mutant sets, so we compare per-product
  MutationScore on Edge vs Event using paired Wilcoxon. Higher MutationScore
  on Event than on Edge supports the subsumption claim empirically.
  Sheet : paired_wilcoxon_score

Claim 2 -- Earliness asymmetry
  Hypothesis: "EventOmission mutants are detected earlier than EdgeOmission
  mutants by the same suite." We compare per-product detection cost
  (% of test suite needed to detect all mutants of that operator). Lower
  detection cost on Event than on Edge supports the earliness claim.
  Sheets:
      paired_wilcoxon_cost_penalized   (penalized -- bias-free, primary)
      paired_wilcoxon_cost_conditional (legacy: detected-only)

For every (SPL, ProductID, TestingApproach) triple there are paired
measurements on the two operators. The pairing key is therefore
(ProductID, TestingApproach), and we run one paired Wilcoxon per
(SPL, TestingApproach) cell. Effect size is Vargha--Delaney A12, computed
directly from the paired vectors. Multiple-testing correction is
Benjamini--Hochberg, applied within each metric across (SPL x approach).

Winner labelling is metric-aware:
  MutationScore   higher is better (Event win = subsumption support)
  DetectionCost   lower  is better (Event win = earliness support)

Paths (relative to this script's location):
    Input : files/Cases/<SPL>/RQ3_<SPL>_perProduct_rawData.xlsx
            (produced by rq3_01_merge_shards_per_spl.py)
            Sheets read: EdgeOmission, EventOmission
    Output: files/scripts/statistical_test_scripts/rq3_result/
            rq3_paired_edge_vs_event.xlsx

Usage:
    python rq3_06_paired_edge_vs_event.py

Dependencies: pandas, numpy, scipy, openpyxl
"""
from __future__ import annotations

import argparse
import warnings
from pathlib import Path

import numpy as np
import pandas as pd
from scipy.stats import wilcoxon
from openpyxl.utils import get_column_letter


# --------------------------------------------------------------------------
# Paths
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

# Each metric: (column name, sheet name, higher_is_better, semantic_winner_label)
# The semantic_winner_label tells the reader what "Event wins" *means* for
# that metric.
METRICS = [
    ("MutationScore(%)",
     "paired_score",
     True,
     "EventDetected",            # Event wins => suite catches more events than edges
    ),
    ("PenalizedPercentageOfSuiteToDetect(%)",
     "paired_cost_penalized",
     False,
     "EventEarlier",             # Event wins => events detected earlier
    ),
    ("PercentageOfSuiteToDetect(%)",
     "paired_cost_conditional",
     False,
     "EventEarlier",
    ),
]


# --------------------------------------------------------------------------
# Statistics helpers
# --------------------------------------------------------------------------
def vargha_delaney_a12(x: np.ndarray, y: np.ndarray) -> float:
    """A12(x, y) = P(X > Y) + 0.5 * P(X == Y). Convention here: x=event, y=edge."""
    x = np.asarray(x, dtype=float)
    y = np.asarray(y, dtype=float)
    n_x, n_y = len(x), len(y)
    if n_x == 0 or n_y == 0:
        return float("nan")
    diff = x[:, None] - y[None, :]
    gt = int(np.sum(diff > 0))
    eq = int(np.sum(diff == 0))
    return (gt + 0.5 * eq) / (n_x * n_y)


def a12_magnitude(a12: float) -> str:
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


def benjamini_hochberg(pvals: np.ndarray) -> np.ndarray:
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
# Data loading
# --------------------------------------------------------------------------
def _find_raw_file(spl_full_name: str) -> Path:
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
    path = _find_raw_file(spl_full_name)
    xls = pd.ExcelFile(path)
    if operator_sheet not in xls.sheet_names:
        raise ValueError(f"{spl_full_name}: sheet '{operator_sheet}' missing in {path.name}")
    return pd.read_excel(xls, sheet_name=operator_sheet)


# --------------------------------------------------------------------------
# Paired test for one (SPL, approach) cell
# --------------------------------------------------------------------------
def run_paired_test(
    merged: pd.DataFrame,
    metric_col: str,
    spl: str,
    approach: str,
    higher_is_better: bool,
) -> dict:
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
        "note": "",
    }

    if n < 6:
        row["note"] = "n<6"
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
        row["note"] = "all ties"
        return row

    try:
        with warnings.catch_warnings():
            warnings.simplefilter("ignore")
            res = wilcoxon(event_vals, edge_vals,
                           zero_method="wilcox", alternative="two-sided")
        W = float(res.statistic)
        p = float(res.pvalue)
    except ValueError as e:
        row["note"] = f"wilcoxon error: {e}"
        return row

    a12 = vargha_delaney_a12(event_vals, edge_vals)
    row["W"] = W
    row["p"] = p
    row["A12_event_vs_edge"] = a12
    row["magnitude"] = a12_magnitude(a12)

    if pd.isna(a12):
        row["winner"] = "n/a"
    elif a12 == 0.5:
        row["winner"] = "tie"
    elif higher_is_better:
        row["winner"] = "Event" if a12 > 0.5 else "Edge"
    else:
        row["winner"] = "Event" if a12 < 0.5 else "Edge"

    return row


# --------------------------------------------------------------------------
# Excel writing helper
# --------------------------------------------------------------------------
def _autosize_columns(worksheet, df: pd.DataFrame) -> None:
    for idx, col_name in enumerate(df.columns, start=1):
        worksheet.column_dimensions[get_column_letter(idx)].width = (
            len(str(col_name)) + 2
        )


# --------------------------------------------------------------------------
# Main pipeline
# --------------------------------------------------------------------------
def main() -> None:
    parser = argparse.ArgumentParser(
        description="Paired Wilcoxon Edge vs Event for MutationScore (subsumption) "
                    "and DetectionCost (earliness)."
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

    # Load edge and event sheets for every SPL once
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

    for metric_col, sheet_name, higher_is_better, semantic_label in METRICS:
        direction_str = "higher-is-better" if higher_is_better else "lower-is-better"
        print(f"\n=== Metric: {metric_col} ({direction_str}) ===")
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
                rows.append(run_paired_test(
                    cell, metric_col, SPL_MAPPING[spl], approach, higher_is_better
                ))

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
            "note",
        ]
        df = df[col_order]
        sheets[sheet_name] = df

        # Console summary
        n_total = len(df)
        n_sig = int(df["significant_BH"].sum())
        n_event = int((df["winner"] == "Event").sum())
        n_edge = int((df["winner"] == "Edge").sum())
        n_tie = int((df["winner"] == "tie").sum())
        n_event_sig = int(((df["winner"] == "Event") & df["significant_BH"]).sum())
        n_edge_sig = int(((df["winner"] == "Edge") & df["significant_BH"]).sum())
        print(f"  tests           : {n_total}")
        print(f"  sig (BH<{args.alpha})    : {n_sig}")
        print(f"  event wins      : {n_event:>4d}  (sig: {n_event_sig})  [{semantic_label}]")
        print(f"  edge wins       : {n_edge:>4d}  (sig: {n_edge_sig})")
        print(f"  ties            : {n_tie}")

    if not sheets:
        print("\nNo output written (no metrics produced any rows).")
        return

    print(f"\nWriting {output_file}...")
    with pd.ExcelWriter(output_file, engine="openpyxl") as writer:
        for sheet_name, df in sheets.items():
            df.to_excel(writer, sheet_name=sheet_name, index=False)
            _autosize_columns(writer.sheets[sheet_name], df)
    print(f"Saved: {output_file}")


if __name__ == "__main__":
    main()