#!/usr/bin/env python3
"""
rq3_07_damping_sensitivity.py

Step 7 of the RQ3 analysis pipeline.

Tests how sensitive the RandomWalk baseline is to the damping factor
parameter, on the same UniGen3 product samples used in RQ1 and RQ3.
Three damping factors are recorded per product: 0.8, 0.85, 0.9.

Two-stage analysis (mirrors common SE-empirical practice):

  Stage 1 -- Friedman omnibus test
      Per (SPL, metric), tests whether the three damping levels yield the
      same distribution of per-product values. Friedman is the
      non-parametric paired equivalent of repeated-measures ANOVA, suitable
      for k=3 related groups on the same subjects (products).
      H0 : all three damping levels yield identical rank distributions.
      Reject H0 -> at least one pair differs; proceed to Stage 2.

  Stage 2 -- Post-hoc paired Wilcoxon
      Per (SPL, metric, pair), tests one of the three pairs:
          0.8  vs 0.85
          0.85 vs 0.9
          0.8  vs 0.9
      with the same paired-Wilcoxon + A12 + BH machinery used by rq3_04.
      BH correction is applied within each metric across all
      (SPL x pair) post-hoc tests in that metric's family.

Sheets read (per SPL, from rq3_01 output):
    Sens_EdgeOmission   -- fault detection on edge mutants
    Sens_EventOmission  -- fault detection on event mutants
    Sens_TestGen        -- test generation metrics (operator-independent)

Metrics analysed:
    Sens_EdgeOmission / Sens_EventOmission:
        MutationScore(%)                                (higher is better)
        PenalizedMedianPercentageOfSuiteToDetect(%)     (lower is better)
    Sens_TestGen:
        NumTestCases                  (lower is better)
        NumTestEvents                 (lower is better)
        AchievedEdgeCoverage(%)       (higher is better)
        StepsTaken                    (lower is better)

Paths (relative to this script's location):
    Input : files/Cases/<SPL>/RQ3_<SPL>_perProduct_rawData.xlsx
    Output: files/scripts/statistical_test_scripts/rq3_result/
            rq3_damping_sensitivity.xlsx

Output sheets:
    Friedman_<source>     -- omnibus per (SPL, metric) for each source sheet
    PostHoc_<source>      -- pairwise per (SPL, metric, pair)

Usage:
    python rq3_07_damping_sensitivity.py
    python rq3_07_damping_sensitivity.py --alpha 0.05

Dependencies: pandas, numpy, scipy, openpyxl
"""
from __future__ import annotations

import argparse
import warnings
from pathlib import Path

import numpy as np
import pandas as pd
from scipy.stats import friedmanchisquare, wilcoxon
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

DAMPING_FACTORS = [0.8, 0.85, 0.9]
DAMPING_PAIRS = [(0.8, 0.85), (0.85, 0.9), (0.8, 0.9)]

# Per source sheet, the metrics to analyse together with their direction.
METRIC_REGISTRY: dict[str, list[tuple[str, bool]]] = {
    "Sens_EdgeOmission": [
        ("MutationScore(%)",                              True),
        ("PenalizedMedianPercentageOfSuiteToDetect(%)",   False),
    ],
    "Sens_EventOmission": [
        ("MutationScore(%)",                              True),
        ("PenalizedMedianPercentageOfSuiteToDetect(%)",   False),
    ],
    "Sens_TestGen": [
        ("NumTestCases",            False),
        ("NumTestEvents",           False),
        ("AchievedEdgeCoverage(%)", True),
        ("StepsTaken",              False),
    ],
}


# --------------------------------------------------------------------------
# Statistics helpers
# --------------------------------------------------------------------------
def vargha_delaney_a12(x: np.ndarray, y: np.ndarray) -> float:
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
        f"No raw data file for {spl_full_name} in {folder}."
    )


def _load_sens_sheet(spl: str, sheet_name: str) -> pd.DataFrame | None:
    path = _find_raw_file(spl)
    xls = pd.ExcelFile(path)
    if sheet_name not in xls.sheet_names:
        return None
    return pd.read_excel(xls, sheet_name=sheet_name)


def _pivot_per_product(df: pd.DataFrame, metric_col: str) -> pd.DataFrame:
    """
    Pivot to (ProductID rows) x (DampingFactor columns).
    Drops products that don't have all three damping observations or have
    NaN on any of them.
    """
    if metric_col not in df.columns:
        return pd.DataFrame()
    sub = df[["ProductID", "DampingFactor", metric_col]].copy()
    sub["DampingFactor"] = sub["DampingFactor"].astype(float).round(3)
    pv = sub.pivot_table(
        index="ProductID",
        columns="DampingFactor",
        values=metric_col,
        aggfunc="median",  # safety: should already be 1 row per (Product, Damping)
    )
    # Keep only rows that have all three damping factors
    needed = [round(float(d), 3) for d in DAMPING_FACTORS]
    if not all(c in pv.columns for c in needed):
        return pd.DataFrame()
    pv = pv[needed].dropna(how="any")
    return pv


# --------------------------------------------------------------------------
# Friedman omnibus per (SPL, metric)
# --------------------------------------------------------------------------
def run_friedman(pv: pd.DataFrame) -> tuple[float, float, str]:
    """Returns (chi2, p, note). pv has 3 columns -- one per damping factor."""
    if pv.empty:
        return np.nan, np.nan, "empty pivot"
    if len(pv) < 6:
        return np.nan, np.nan, f"n<6 (n={len(pv)})"
    cols = [pv.iloc[:, i].to_numpy(dtype=float) for i in range(3)]
    if all(np.allclose(cols[0], c) for c in cols[1:]):
        return 0.0, 1.0, "all three damping levels identical"
    try:
        with warnings.catch_warnings():
            warnings.simplefilter("ignore")
            res = friedmanchisquare(*cols)
        return float(res.statistic), float(res.pvalue), ""
    except ValueError as e:
        return np.nan, np.nan, f"friedman error: {e}"


# --------------------------------------------------------------------------
# Post-hoc paired Wilcoxon (one pair)
# --------------------------------------------------------------------------
def run_posthoc_wilcoxon(
    pv: pd.DataFrame, d_a: float, d_b: float, higher_is_better: bool
) -> dict:
    key_a = round(float(d_a), 3)
    key_b = round(float(d_b), 3)
    out = {
        "DampingA": d_a, "DampingB": d_b,
        "n_pairs": 0,
        "median_A": np.nan, "median_B": np.nan,
        "median_delta_AminusB": np.nan,
        "W": np.nan, "p": np.nan,
        "A12": np.nan, "A12_magnitude": "n/a",
        "winner": "n/a",
        "note": "",
    }
    if pv.empty or key_a not in pv.columns or key_b not in pv.columns:
        out["note"] = "missing damping column"
        return out
    a_vals = pv[key_a].to_numpy(dtype=float)
    b_vals = pv[key_b].to_numpy(dtype=float)
    out["n_pairs"] = len(a_vals)
    if len(a_vals) < 6:
        out["note"] = f"n<6 (n={len(a_vals)})"
        return out
    out["median_A"] = float(np.median(a_vals))
    out["median_B"] = float(np.median(b_vals))
    out["median_delta_AminusB"] = float(np.median(a_vals - b_vals))

    diffs = a_vals - b_vals
    if np.allclose(diffs, 0):
        out["W"] = 0.0
        out["p"] = 1.0
        out["A12"] = 0.5
        out["A12_magnitude"] = "negligible"
        out["winner"] = "tie"
        out["note"] = "all ties"
        return out

    try:
        with warnings.catch_warnings():
            warnings.simplefilter("ignore")
            res = wilcoxon(a_vals, b_vals, zero_method="wilcox", alternative="two-sided")
        out["W"] = float(res.statistic)
        out["p"] = float(res.pvalue)
    except ValueError as e:
        out["note"] = f"wilcoxon error: {e}"
        return out

    a12 = vargha_delaney_a12(a_vals, b_vals)
    out["A12"] = a12
    out["A12_magnitude"] = a12_magnitude(a12)

    if pd.isna(a12) or a12 == 0.5:
        out["winner"] = "tie" if a12 == 0.5 else "n/a"
    elif higher_is_better:
        out["winner"] = f"d={d_a}" if a12 > 0.5 else f"d={d_b}"
    else:
        out["winner"] = f"d={d_b}" if a12 > 0.5 else f"d={d_a}"
    return out


# --------------------------------------------------------------------------
# Per-source-sheet pipeline
# --------------------------------------------------------------------------
def run_for_source_sheet(source_sheet: str, alpha: float):
    """
    Returns (friedman_df, posthoc_df) for one of the Sens_* sheets.
    """
    metrics = METRIC_REGISTRY[source_sheet]
    friedman_rows = []
    posthoc_rows = []

    print(f"\n=== {source_sheet} ===")

    for spl_full in SPL_MAPPING.keys():
        try:
            df = _load_sens_sheet(spl_full, source_sheet)
        except FileNotFoundError as e:
            print(f"  [SKIP] {spl_full}: {e}")
            continue
        if df is None:
            print(f"  [SKIP] {spl_full}: sheet '{source_sheet}' not present")
            continue
        short = SPL_MAPPING[spl_full]

        for metric_col, higher_is_better in metrics:
            pv = _pivot_per_product(df, metric_col)

            # --- Friedman ---
            chi2, p_omni, note_omni = run_friedman(pv)
            friedman_rows.append({
                "SPL": short,
                "Metric": metric_col,
                "Direction": "higher_is_better" if higher_is_better else "lower_is_better",
                "n_products": len(pv),
                "median_d=0.8":  float(pv[0.8].median())  if not pv.empty else np.nan,
                "median_d=0.85": float(pv[0.85].median()) if not pv.empty else np.nan,
                "median_d=0.9":  float(pv[0.9].median())  if not pv.empty else np.nan,
                "Friedman_chi2": chi2,
                "Friedman_p":    p_omni,
                "note":          note_omni,
            })

            # --- Post-hoc Wilcoxon for the three pairs (always reported,
            # even if Friedman is non-significant; manuscript can still
            # cite descriptive numbers) ---
            for d_a, d_b in DAMPING_PAIRS:
                ph = run_posthoc_wilcoxon(pv, d_a, d_b, higher_is_better)
                posthoc_rows.append({
                    "SPL": short,
                    "Metric": metric_col,
                    "Direction": "higher_is_better" if higher_is_better else "lower_is_better",
                    **ph,
                })

        print(f"  [OK] {spl_full}: {len(metrics)} metrics processed")

    fri_df = pd.DataFrame(friedman_rows)
    ph_df = pd.DataFrame(posthoc_rows)

    # BH within each Metric across post-hocs
    if not ph_df.empty:
        ph_df["p_BH"] = np.nan
        ph_df["significant_BH"] = False
        for metric in ph_df["Metric"].unique():
            mask = (ph_df["Metric"] == metric) & ph_df["p"].notna()
            if mask.any():
                ph_df.loc[mask, "p_BH"] = benjamini_hochberg(
                    ph_df.loc[mask, "p"].values
                )
        ph_df["significant_BH"] = ph_df["p_BH"] < alpha

    if not fri_df.empty:
        fri_df["Friedman_p_BH"] = np.nan
        for metric in fri_df["Metric"].unique():
            mask = (fri_df["Metric"] == metric) & fri_df["Friedman_p"].notna()
            if mask.any():
                fri_df.loc[mask, "Friedman_p_BH"] = benjamini_hochberg(
                    fri_df.loc[mask, "Friedman_p"].values
                )
        fri_df["Friedman_significant_BH"] = fri_df["Friedman_p_BH"] < alpha

    return fri_df, ph_df


# --------------------------------------------------------------------------
# Excel writing
# --------------------------------------------------------------------------
def _autosize_columns(worksheet, df: pd.DataFrame) -> None:
    for idx, col_name in enumerate(df.columns, start=1):
        worksheet.column_dimensions[get_column_letter(idx)].width = (
            len(str(col_name)) + 2
        )


# --------------------------------------------------------------------------
# Main
# --------------------------------------------------------------------------
def main() -> None:
    parser = argparse.ArgumentParser(
        description="Damping factor sensitivity (Friedman + post-hoc Wilcoxon) for RQ3."
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

    output_file = OUTPUT_DIR / "rq3_damping_sensitivity.xlsx"
    print(f"Output file     : {output_file}")

    sheets: dict[str, pd.DataFrame] = {}
    for source_sheet in METRIC_REGISTRY.keys():
        fri_df, ph_df = run_for_source_sheet(source_sheet, args.alpha)
        if not fri_df.empty:
            sheets[f"Friedman_{source_sheet}"] = fri_df
        if not ph_df.empty:
            ph_col_order = [
                "SPL", "Metric", "Direction",
                "DampingA", "DampingB",
                "n_pairs",
                "median_A", "median_B", "median_delta_AminusB",
                "W", "p", "p_BH", "significant_BH",
                "A12", "A12_magnitude", "winner",
                "note",
            ]
            ph_col_order = [c for c in ph_col_order if c in ph_df.columns]
            ph_df = ph_df[ph_col_order]
            sheets[f"PostHoc_{source_sheet}"] = ph_df

    if not sheets:
        print("\nNo output written.")
        return

    print(f"\nWriting {output_file}...")
    with pd.ExcelWriter(output_file, engine="openpyxl") as writer:
        for sheet_name, df in sheets.items():
            df.to_excel(writer, sheet_name=sheet_name, index=False)
            _autosize_columns(writer.sheets[sheet_name], df)
            print(f"  sheet '{sheet_name}': {len(df)} rows")
    print(f"Saved: {output_file}")

    # Brief console summary across all post-hocs
    print("\n--- Cross-sheet summary (post-hocs) ---")
    for src in METRIC_REGISTRY.keys():
        sn = f"PostHoc_{src}"
        if sn not in sheets:
            continue
        df = sheets[sn]
        n_total = len(df)
        n_sig = int(df["significant_BH"].sum())
        n_neg = int((df["A12_magnitude"] == "negligible").sum())
        print(f"  {sn:<28s} tests={n_total:>4d}  sig_BH={n_sig:>3d}  negligible_effect={n_neg}")


if __name__ == "__main__":
    main()