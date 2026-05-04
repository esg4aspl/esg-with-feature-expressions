#!/usr/bin/env python3
"""
rq3_02_aggregate_across_spls.py

Step 2 of the RQ3 analysis pipeline.

Aggregates per-product RQ3 fault detection data across all SPLs into summary
Excel files. Supports both EdgeOmission and EventOmission operators.

Input (per SPL): files/Cases/<SPL>/RQ3_<SPL>_perProduct_rawData.xlsx
    Sheets: EdgeOmission, EventOmission,
            EdgeOmission_MultiSeed, EventOmission_MultiSeed,
            Sens_EdgeOmission, Sens_EventOmission, Sens_TestGen

Output (per operator, two scopes):
    files/Cases/RQ3_Summary_<Operator>.xlsx              -- article-scope (L=1 EXCLUDED)
    files/Cases/RQ3_Summary_<Operator>_ForPhDThesis.xlsx -- thesis-scope (L=1 INCLUDED)

The two scopes follow the same RQ1/RQ2 policy:
- Article output drops ESG-Fx_L1 rows because L=1 uses a structurally different
  generation procedure (single Euler cycle, no L-sequence transformation) and
  does not belong on the same axis as L>=2 results.
- Thesis output keeps all rows for completeness.
- Both files are produced in a single run.

Each summary contains four sheets:
    <Op>            -- deterministic fault detection (1 row per SPL+approach)
    <Op>_MultiSeed  -- aggregated multi-seed RW fault detection
    Sens_<Op>       -- damping sensitivity fault detection (1 row per damping)
    Sens_TestGen    -- damping sensitivity test-generation metrics
                       (operator-independent, included in both files for
                       self-containment)

Conventions
-----------
- Approach order is canonical: ESG-Fx_L1..L4 -> EFG_L2..L4 -> RandomWalk
  -> RandomWalk_MultiSeed -> RandomWalk_Damping_<factor>.
  Anything else is appended at the end alphabetically.
- Defensive rename: any leftover ``ESG-Fx_L0`` is mapped to ``RandomWalk``,
  in case rq3_01 hasn't been re-run after the fix.
- Excel column widths are set to ``len(header) + 2`` so headers fit on one line.

Usage:
    python rq3_02_aggregate_across_spls.py --operator edge
    python rq3_02_aggregate_across_spls.py --operator event
    python rq3_02_aggregate_across_spls.py --operator both        # default
"""

from __future__ import annotations

import argparse
import sys
from pathlib import Path

import pandas as pd
from openpyxl.utils import get_column_letter


# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------

# Java mislabels the deterministic RandomWalk baseline as "ESG-Fx_L0".
# Same rename as rq3_01; applied defensively here in case rq3_01 wasn't re-run.
APPROACH_RENAMES = {
    "ESG-Fx_L0": "RandomWalk",
}

# Canonical approach order for sorting summary rows. Anything not listed here
# is appended at the end (alphabetically).
APPROACH_ORDER = [
    "ESG-Fx_L1", "ESG-Fx_L2", "ESG-Fx_L3", "ESG-Fx_L4",
    "EFG_L2", "EFG_L3", "EFG_L4",
    "RandomWalk",
    "RandomWalk_MultiSeed",
    # RandomWalk_Damping_<factor> entries are sorted numerically below.
]

# L=1 is excluded from the article-scope output and kept in the thesis-scope
# output. See module docstring for the rationale.
ARTICLE_EXCLUDED_APPROACHES = {"ESG-Fx_L1"}

# Aggregation maps -- median across products for every metric reported by RQ3.
FAULT_DETECTION_AGG = {
    "TotalMutants": "median",
    "DetectedMutants": "median",
    "MutationScore(%)": "median",
    "TotalEventsInSuite": "median",
    "EventsToDetect": "median",
    "PercentageOfSuiteToDetect(%)": "median",
}

TESTGEN_AGG = {
    "NumTestCases": "median",
    "NumTestEvents": "median",
    "AchievedEdgeCoverage(%)": "median",
    "StepsTaken": "median",
}

OPERATORS = {
    "edge": "EdgeOmission",
    "event": "EventOmission",
}


# ---------------------------------------------------------------------------
# Path discovery
# ---------------------------------------------------------------------------
def find_project_root() -> Path | None:
    """Find project root by walking upward from cwd and from this file."""
    candidates = [Path.cwd(), *Path.cwd().parents]
    try:
        here = Path(__file__).resolve()
        candidates.extend([here.parent, *here.parents])
    except NameError:
        pass

    seen = set()
    for probe in candidates:
        if probe in seen:
            continue
        seen.add(probe)
        if (probe / "files" / "Cases").exists():
            return probe
    return None


# ---------------------------------------------------------------------------
# Aggregation helpers
# ---------------------------------------------------------------------------
def _rename_multiseed_columns(df: pd.DataFrame) -> pd.DataFrame:
    """Multi-seed sheets use Median* names; unify with deterministic columns."""
    return df.rename(columns={
        "MedianEventsToDetect": "EventsToDetect",
        "MedianPercentageOfSuiteToDetect(%)": "PercentageOfSuiteToDetect(%)",
    })


def _apply_baseline_rename(df: pd.DataFrame) -> pd.DataFrame:
    """Defensively rename any lingering ESG-Fx_L0 to RandomWalk."""
    if "TestingApproach" not in df.columns or df.empty:
        return df
    mask = df["TestingApproach"].isin(APPROACH_RENAMES)
    if mask.any():
        df = df.copy()
        df.loc[mask, "TestingApproach"] = df.loc[mask, "TestingApproach"].map(
            APPROACH_RENAMES
        )
    return df


def _aggregate(df: pd.DataFrame, agg_map: dict) -> pd.DataFrame:
    """Group by (SPL, TestingApproach) and apply medians."""
    # Only aggregate columns that actually exist in this DataFrame; keep the
    # caller-provided order.
    present = {col: how for col, how in agg_map.items() if col in df.columns}
    if not present:
        return pd.DataFrame()
    return df.groupby(["SPL", "TestingApproach"], as_index=False).agg(present)


# ---------------------------------------------------------------------------
# Sorting + filtering
# ---------------------------------------------------------------------------
def _approach_sort_key(approach: str):
    """
    Sort key that respects APPROACH_ORDER and orders RandomWalk_Damping_<factor>
    numerically by factor.
    """
    if approach in APPROACH_ORDER:
        return (0, APPROACH_ORDER.index(approach), 0.0, approach)
    if approach.startswith("RandomWalk_Damping_"):
        try:
            factor = float(approach.split("RandomWalk_Damping_", 1)[1])
        except ValueError:
            factor = float("inf")
        return (1, 0, factor, approach)
    return (2, 0, 0.0, approach)


def _sort_summary(df: pd.DataFrame) -> pd.DataFrame:
    """Sort an aggregated summary DataFrame by SPL then approach order."""
    if df.empty or "TestingApproach" not in df.columns:
        return df
    df = df.copy()
    df["_sort_key"] = df["TestingApproach"].map(_approach_sort_key)
    df = df.sort_values(by=["SPL", "_sort_key"], kind="mergesort").reset_index(drop=True)
    return df.drop(columns=["_sort_key"])


def _filter_for_scope(df: pd.DataFrame, scope: str) -> pd.DataFrame:
    """
    Return a copy of df filtered for the given scope.

    scope == 'article'  -> drops ESG-Fx_L1 rows
    scope == 'thesis'   -> returns all rows
    """
    if scope == "thesis" or "TestingApproach" not in df.columns:
        return df.copy()
    return df[~df["TestingApproach"].isin(ARTICLE_EXCLUDED_APPROACHES)].copy()


# ---------------------------------------------------------------------------
# Excel writing
# ---------------------------------------------------------------------------
def _autosize_columns(worksheet, df: pd.DataFrame) -> None:
    """Set every column width to len(header) + 2 so headers fit on one line."""
    for idx, col_name in enumerate(df.columns, start=1):
        worksheet.column_dimensions[get_column_letter(idx)].width = (
            len(str(col_name)) + 2
        )


# ---------------------------------------------------------------------------
# Per-operator pipeline
# ---------------------------------------------------------------------------
def aggregate_for_operator(
    cases_dir: Path, operator: str
) -> dict[str, list[pd.DataFrame]]:
    """
    Aggregate all SPLs for one operator (EdgeOmission or EventOmission).

    Returns a dict keyed by output sheet name; each value is a list of
    per-SPL aggregated DataFrames waiting to be concatenated.
    """
    op_sheet = operator                         # e.g. "EdgeOmission"
    op_multiseed = f"{operator}_MultiSeed"      # e.g. "EdgeOmission_MultiSeed"
    op_sens = f"Sens_{operator}"                # e.g. "Sens_EdgeOmission"

    buckets: dict[str, list[pd.DataFrame]] = {
        op_sheet: [],
        op_multiseed: [],
        op_sens: [],
        "Sens_TestGen": [],
    }

    for spl_dir in sorted(cases_dir.iterdir()):
        if not spl_dir.is_dir():
            continue

        # Look for the file produced by rq3_01.
        target_file = spl_dir / f"RQ3_{spl_dir.name}_perProduct_rawData.xlsx"
        if not target_file.exists():
            continue

        print(f"Reading: {spl_dir.name}")
        try:
            xl = pd.ExcelFile(target_file, engine="openpyxl")
            sheets = xl.sheet_names
        except Exception as e:
            print(f"  ERROR reading {target_file.name}: {e}")
            continue

        # 1) Deterministic fault detection ---------------------------------
        if op_sheet in sheets:
            df = xl.parse(op_sheet)
            df = _apply_baseline_rename(df)
            if not df.empty:
                agg = _aggregate(df, FAULT_DETECTION_AGG)
                if not agg.empty:
                    buckets[op_sheet].append(agg)
                    print(f"  [OK] {op_sheet}")

        # 2) Multi-seed RandomWalk fault detection -------------------------
        if op_multiseed in sheets:
            df = xl.parse(op_multiseed)
            if not df.empty:
                df = _rename_multiseed_columns(df)
                # Multi-seed sheets have no TestingApproach column; assign one.
                df = df.copy()
                df["TestingApproach"] = "RandomWalk_MultiSeed"
                agg = _aggregate(df, FAULT_DETECTION_AGG)
                if not agg.empty:
                    buckets[op_multiseed].append(agg)
                    print(f"  [OK] {op_multiseed}")

        # 3) Damping sensitivity fault detection ---------------------------
        if op_sens in sheets:
            df = xl.parse(op_sens)
            if not df.empty:
                df = _rename_multiseed_columns(df)
                df = df.copy()
                df["TestingApproach"] = (
                    "RandomWalk_Damping_" + df["DampingFactor"].astype(str)
                )
                agg = _aggregate(df, FAULT_DETECTION_AGG)
                if not agg.empty:
                    buckets[op_sens].append(agg)
                    print(f"  [OK] {op_sens}")

        # 4) Damping sensitivity test-generation metrics -------------------
        if "Sens_TestGen" in sheets:
            df = xl.parse("Sens_TestGen")
            if not df.empty:
                df = df.copy()
                df["TestingApproach"] = (
                    "RandomWalk_Damping_" + df["DampingFactor"].astype(str)
                )
                agg = _aggregate(df, TESTGEN_AGG)
                if not agg.empty:
                    buckets["Sens_TestGen"].append(agg)
                    print(f"  [OK] Sens_TestGen")

    return buckets


def write_summary(
    cases_dir: Path,
    operator: str,
    buckets: dict[str, list[pd.DataFrame]],
    scope: str,
) -> None:
    """
    Concatenate aggregated buckets and write one summary file for the given
    scope. ``scope`` is either 'article' or 'thesis'.
    """
    if scope == "article":
        out_file = cases_dir / f"RQ3_Summary_{operator}.xlsx"
    elif scope == "thesis":
        out_file = cases_dir / f"RQ3_Summary_{operator}_ForPhDThesis.xlsx"
    else:
        raise ValueError(f"unknown scope: {scope!r}")

    print(f"\nWriting ({scope}): {out_file}")

    any_written = False
    with pd.ExcelWriter(out_file, engine="openpyxl") as writer:
        for sheet_name, df_list in buckets.items():
            if not df_list:
                print(f"  sheet '{sheet_name}': empty, skipped")
                continue

            final = pd.concat(df_list, ignore_index=True)
            final = _filter_for_scope(final, scope)

            if final.empty:
                print(f"  sheet '{sheet_name}': empty after scope filter, skipped")
                continue

            final = _sort_summary(final)
            final = final.round(2)

            final.to_excel(writer, index=False, sheet_name=sheet_name)
            _autosize_columns(writer.sheets[sheet_name], final)
            print(f"  sheet '{sheet_name}': {len(final)} rows")
            any_written = True

    if not any_written:
        print("  (nothing aggregated; output file may be empty)")
    print(f"Saved: {out_file}")


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------
def main() -> None:
    parser = argparse.ArgumentParser(
        description="Aggregate RQ3 fault detection results across SPLs."
    )
    parser.add_argument(
        "--operator",
        choices=["edge", "event", "both"],
        default="both",
        help="Which operator(s) to aggregate (default: both).",
    )
    args = parser.parse_args()

    print("=" * 80)
    print("RQ3 FAULT DETECTION AGGREGATOR")
    print(f"Operator: {args.operator}")
    print("=" * 80)

    project_root = find_project_root()
    if project_root is None:
        print("ERROR: Could not find project root (no 'files/Cases' on the path).")
        sys.exit(1)
    cases_dir = project_root / "files" / "Cases"
    print(f"Cases directory: {cases_dir}")

    ops_to_run = (
        [OPERATORS["edge"], OPERATORS["event"]]
        if args.operator == "both"
        else [OPERATORS[args.operator]]
    )

    for op in ops_to_run:
        print(f"\n--- Processing {op} ---")
        buckets = aggregate_for_operator(cases_dir, op)
        # Two outputs in a single run: article-scope and thesis-scope.
        write_summary(cases_dir, op, buckets, scope="article")
        write_summary(cases_dir, op, buckets, scope="thesis")

    print("\n" + "=" * 80)
    print("rq3_02 DONE.")
    print("=" * 80)


if __name__ == "__main__":
    main()