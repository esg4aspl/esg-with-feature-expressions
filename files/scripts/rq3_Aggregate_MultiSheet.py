#!/usr/bin/env python3
"""
RQ3_Aggregate_MultiSheet.py

Aggregates per-product RQ3 fault detection data across all SPLs into a single
summary Excel file. Supports both EdgeOmission and EventOmission operators.

Input (per SPL): files/Cases/<SPL>/RQ3_perProduct.xlsx
    Sheets: EdgeOmission, EventOmission,
            EdgeOmission_MultiSeed, EventOmission_MultiSeed,
            Sens_EdgeOmission, Sens_EventOmission, Sens_TestGen

Output: files/Cases/RQ3_Summary_<Operator>.xlsx
    Sheets produced: <Op>, <Op>_MultiSeed, Sens_<Op>, Sens_TestGen

Usage:
    python RQ3_Aggregate_MultiSheet.py --operator edge
    python RQ3_Aggregate_MultiSheet.py --operator event
    python RQ3_Aggregate_MultiSheet.py --operator both

Notes:
    - Sens_TestGen is operator-independent (it describes RW walk behaviour,
      not fault detection). It is included in both summary files for
      convenience so each file is self-contained for downstream analysis.
"""
import argparse
import sys
from pathlib import Path

import pandas as pd


# --------------------------------------------------------------------------
# Project root discovery (reuses your existing convention)
# --------------------------------------------------------------------------
def find_project_root() -> Path | None:
    current = Path.cwd()
    for probe in (current, current.parent, current.parent.parent):
        if (probe / "files" / "Cases").exists():
            return probe
    hardcoded = Path("/Users/dilekozturk/git/esg-with-feature-expressions")
    if (hardcoded / "files" / "Cases").exists():
        return hardcoded
    return None


# --------------------------------------------------------------------------
# Aggregation helpers
# --------------------------------------------------------------------------
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


def _rename_multiseed_columns(df: pd.DataFrame) -> pd.DataFrame:
    """Multi-seed sheets use Median* names; unify with deterministic columns."""
    return df.rename(columns={
        "MedianEventsToDetect": "EventsToDetect",
        "MedianPercentageOfSuiteToDetect(%)": "PercentageOfSuiteToDetect(%)",
    })


def _aggregate(df: pd.DataFrame, agg_map: dict) -> pd.DataFrame:
    return df.groupby(["SPL", "TestingApproach"]).agg(agg_map).reset_index()


# --------------------------------------------------------------------------
# Per-operator pipeline
# --------------------------------------------------------------------------
def aggregate_for_operator(cases_dir: Path, operator: str) -> dict[str, list[pd.DataFrame]]:
    """
    Aggregate all SPLs for one operator (EdgeOmission or EventOmission).

    Returns a dict keyed by output sheet name; each value is a list of
    per-SPL DataFrames waiting to be concatenated.
    """
    op_sheet = operator                        # e.g. "EdgeOmission"
    op_multiseed = f"{operator}_MultiSeed"     # e.g. "EdgeOmission_MultiSeed"
    op_sens = f"Sens_{operator}"               # e.g. "Sens_EdgeOmission"

    buckets: dict[str, list[pd.DataFrame]] = {
        op_sheet: [],
        op_multiseed: [],
        op_sens: [],
        "Sens_TestGen": [],
    }

    for spl_dir in sorted(cases_dir.iterdir()):
        if not spl_dir.is_dir():
            continue
        target_file = spl_dir / "RQ3_perProduct.xlsx"
        if not target_file.exists():
            continue

        print(f"Reading: {spl_dir.name}")
        try:
            xl = pd.ExcelFile(target_file, engine="openpyxl")
            sheets = xl.sheet_names
        except Exception as e:
            print(f"  ERROR reading {target_file.name}: {e}")
            continue

        # 1) Deterministic fault detection
        if op_sheet in sheets:
            df = xl.parse(op_sheet)
            if not df.empty:
                buckets[op_sheet].append(_aggregate(df, FAULT_DETECTION_AGG))
                print(f"  [OK] {op_sheet}")

        # 2) Multi-seed RandomWalk fault detection
        if op_multiseed in sheets:
            df = xl.parse(op_multiseed)
            if not df.empty:
                df = _rename_multiseed_columns(df)
                df["TestingApproach"] = "RandomWalk_MultiSeed"
                buckets[op_multiseed].append(_aggregate(df, FAULT_DETECTION_AGG))
                print(f"  [OK] {op_multiseed}")

        # 3) Damping sensitivity fault detection
        if op_sens in sheets:
            df = xl.parse(op_sens)
            if not df.empty:
                df = _rename_multiseed_columns(df)
                df["TestingApproach"] = "RandomWalk_Damping_" + df["DampingFactor"].astype(str)
                buckets[op_sens].append(_aggregate(df, FAULT_DETECTION_AGG))
                print(f"  [OK] {op_sens}")

        # 4) Damping sensitivity test-generation metrics (operator-independent,
        #    but included per-operator output file for self-containment)
        if "Sens_TestGen" in sheets:
            df = xl.parse("Sens_TestGen")
            if not df.empty:
                df["TestingApproach"] = "RandomWalk_Damping_" + df["DampingFactor"].astype(str)
                buckets["Sens_TestGen"].append(_aggregate(df, TESTGEN_AGG))
                print(f"  [OK] Sens_TestGen")

    return buckets


def write_summary(cases_dir: Path, operator: str, buckets: dict[str, list[pd.DataFrame]]) -> None:
    """Write aggregated buckets to files/Cases/RQ3_Summary_<Operator>.xlsx."""
    out_file = cases_dir / f"RQ3_Summary_{operator}.xlsx"
    print(f"\nWriting: {out_file}")

    any_written = False
    with pd.ExcelWriter(out_file, engine="openpyxl") as writer:
        for sheet_name, df_list in buckets.items():
            if df_list:
                final = pd.concat(df_list, ignore_index=True)
                final = final.sort_values(by=["SPL", "TestingApproach"]).reset_index(drop=True)
                final = final.round(2)
                final.to_excel(writer, index=False, sheet_name=sheet_name)
                print(f"  sheet '{sheet_name}': {len(final)} rows")
                any_written = True
            else:
                print(f"  sheet '{sheet_name}': empty, skipped")

    if not any_written:
        # pandas still creates the file; clean up if nothing was written
        print("  (nothing aggregated; output file may be empty)")

    print(f"Saved: {out_file}")


# --------------------------------------------------------------------------
# Main
# --------------------------------------------------------------------------
OPERATORS = {
    "edge": "EdgeOmission",
    "event": "EventOmission",
}


def main() -> None:
    parser = argparse.ArgumentParser(description="Aggregate RQ3 fault detection results across SPLs.")
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
        print("ERROR: Could not find project root (missing files/Cases).")
        sys.exit(1)
    cases_dir = project_root / "files" / "Cases"

    ops_to_run = (
        [OPERATORS["edge"], OPERATORS["event"]]
        if args.operator == "both"
        else [OPERATORS[args.operator]]
    )

    for op in ops_to_run:
        print(f"\n--- Processing {op} ---")
        buckets = aggregate_for_operator(cases_dir, op)
        write_summary(cases_dir, op, buckets)


if __name__ == "__main__":
    main()