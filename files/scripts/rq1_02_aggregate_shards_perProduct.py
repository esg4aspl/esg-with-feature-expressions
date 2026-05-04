#!/usr/bin/env python3
"""
rq1_02_aggregate_shards_perProduct.py
Step 1b of the RQ1 pipeline.

Reads PerProductLog CSV files from each SPL (produced by the Java pipelines
in comparativeEfficiencyTestPipeline/) and combines them into one Excel file
per SPL, with one sheet per approach x level combination.

Key post-processing:

  1. Adds a new 'TotalTestGenTime(ms)' column for ESG-Fx and RandomWalk that
     is scope-symmetric with the EFG baseline (GuitarGenTime already captures
     input-read + algorithm + disk-write inside a single opaque sub-process):

       ESG-Fx:     AlgTestGenTime + ESGFxModelLoadTimeMs + TestCaseRecordingTime(ms)
       RandomWalk: AlgTestGenTime + ESGFxModelLoadTimeMs + TestCaseRecordingTime(ms)

     The algorithmic column ('TotalTestGenTime(ms)' in L2/L3/L4, 'TestGenTime(ms)'
     in L1 and RandomWalk) is renamed to 'AlgTestGenTime(ms)'.

  2. Applies a unified cross-approach naming scheme so that the same concept
     uses the same column name in every sheet (e.g., 'NumberOfESGFxVertices'
     everywhere, 'EdgeCoverage(%)' everywhere, etc.).

  3. Reorders columns inside each sheet to a fixed, human-friendly layout
     (Approach + Coverage Type + RunID + ProductID first, then generation-cost
     columns, then graph metrics, then coverage metrics, then status columns).

  4. Sheets are ordered ESG-Fx (by L level) -> EFG (by L level) -> RandomWalk.

  5. Each column is sized to fit its header text.

Output per SPL:
    files/Cases/<SPL>/RQ1_<SPL>_perProduct.xlsx

Usage (from any directory inside or next to the project tree):
    python rq1_02_aggregate_shards_perProduct.py
"""
import sys
from pathlib import Path

import pandas as pd
from openpyxl.utils import get_column_letter


# ---------------------------------------------------------------------------
# Project root discovery
# ---------------------------------------------------------------------------

def find_project_root():
    """Locate the project root by walking up from cwd and from this script."""
    candidates = [Path.cwd()]
    try:
        candidates.append(Path(__file__).resolve().parent)
    except NameError:
        pass

    for start in candidates:
        for p in [start, *start.parents]:
            if (p / "files" / "Cases").is_dir():
                return p
    return None


# ---------------------------------------------------------------------------
# CSV reading
# ---------------------------------------------------------------------------

def read_per_product_csv(filepath, approach, level):
    """Read a single PerProductLog CSV, tolerant of comma vs dot decimals.
    Only empty cells become NaN; the literal string 'None' (which the Java
    writers emit in ErrorReason for successful runs) is kept as-is."""
    try:
        df = pd.read_csv(filepath, sep=';', decimal=',',
                         keep_default_na=False, na_values=[''])
        if len(df.columns) == 1:
            df = pd.read_csv(filepath, sep=',', decimal='.',
                             keep_default_na=False, na_values=[''])
        df.columns = [c.strip() for c in df.columns]
        df['Approach'] = approach
        df['Coverage Type'] = level
        return df
    except Exception as e:
        print(f"  ERROR: {filepath.name}: {e}")
        return None


# ---------------------------------------------------------------------------
# Step 1 — cross-approach rename map (applied BEFORE redefine_test_gen_time)
# ---------------------------------------------------------------------------

_RENAME_MAPS = {
    'ESG-Fx': {
        # Time-convention unification: prefer '(ms)' / '(MB)' parentheses
        'ESGFxModelLoadTimeMs':   'ESGFxModelLoadTime(ms)',
        'TestExecTimeMs':         'TestExecTime(ms)',
        'TestExecPeakMemoryMB':   'TestExecPeakMemory(MB)',
    },
    'EFG': {
        'ParentPeakMemory(MB)':        'TestGenPeakMemory(MB)',
        'ESGFx_Vertices':              'NumberOfESGFxVertices',
        'ESGFx_Edges':                 'NumberOfESGFxEdges',
        'EventCoverage':               'EventCoverage(%)',
        'EventCoverageAnalysisTimeMs': 'EventCoverageAnalysisTime(ms)',
        'EdgeCoverage':                'EdgeCoverage(%)',
        'EdgeCoverageAnalysisTimeMs':  'EdgeCoverageAnalysisTime(ms)',
        # Time-convention unification: prefer '(ms)' / '(MB)' parentheses
        'ESGFxModelLoadTimeMs':        'ESGFxModelLoadTime(ms)',
        'TestExecTimeMs':              'TestExecTime(ms)',
        'TestExecPeakMemoryMB':        'TestExecPeakMemory(MB)',
        'ParsingTimeMs':               'ParsingTime(ms)',
    },
    'RandomWalk': {
        'ModelLoadTime(ms)':       'ESGFxModelLoadTime(ms)',
        'Vertices':                'NumberOfESGFxVertices',
        'Edges':                   'NumberOfESGFxEdges',
        'TestCases':               'NumberOfESGFxTestCases',
        'TestEvents':              'NumberOfESGFxTestEvents',
        'EventCoverageTime(ms)':   'EventCoverageAnalysisTime(ms)',
        'EdgeCoverageTime(ms)':    'EdgeCoverageAnalysisTime(ms)',
        # TestExecTime(ms) and TestExecPeakMemory(MB) already use (ms)/(MB) — no change.
    },
}


def apply_renames(df, approach):
    rename_map = _RENAME_MAPS.get(approach, {})
    if rename_map:
        applicable = {k: v for k, v in rename_map.items() if k in df.columns}
        if applicable:
            df = df.rename(columns=applicable)
    return df


# ---------------------------------------------------------------------------
# Step 2 — add AlgTestGenTime(ms) + scope-symmetric TotalTestGenTime(ms)
# ---------------------------------------------------------------------------

_ALG_OUT = 'AlgTestGenTime(ms)'
_TOTAL_OUT = 'TotalTestGenTime(ms)'

_SYMMETRIC_COMPONENTS = {
    'ESG-Fx': {
        'alg_candidates': ['TotalTestGenTime(ms)', 'TestGenTime(ms)'],
        'model_load':     'ESGFxModelLoadTime(ms)',
        'recording':      'TestCaseRecordingTime(ms)',
    },
    'RandomWalk': {
        # After apply_renames, RandomWalk uses 'ESGFxModelLoadTime(ms)'.
        'alg_candidates': ['TestGenTime(ms)'],
        'model_load':     'ESGFxModelLoadTime(ms)',
        'recording':      'TestCaseRecordingTime(ms)',
    },
}


def redefine_test_gen_time(df, approach):
    spec = _SYMMETRIC_COMPONENTS.get(approach)
    if spec is None:
        return df

    alg_src = next((c for c in spec['alg_candidates'] if c in df.columns), None)
    if alg_src is None:
        print(f"  WARNING ({approach}): no algorithmic-time column found "
              f"(expected one of {spec['alg_candidates']}); skipping redefinition.")
        return df

    ml_col, rec_col = spec['model_load'], spec['recording']
    missing = [c for c in (ml_col, rec_col) if c not in df.columns]
    if missing:
        print(f"  WARNING ({approach}): missing component columns {missing}; "
              f"skipping redefinition.")
        return df

    df = df.rename(columns={alg_src: _ALG_OUT})

    alg_v = pd.to_numeric(df[_ALG_OUT], errors='coerce')
    ml_v  = pd.to_numeric(df[ml_col],   errors='coerce')
    rec_v = pd.to_numeric(df[rec_col],  errors='coerce')
    total = alg_v + ml_v + rec_v

    if _TOTAL_OUT in df.columns:
        df = df.drop(columns=[_TOTAL_OUT])
    cols = list(df.columns)
    insert_at = cols.index(_ALG_OUT) + 1
    df.insert(insert_at, _TOTAL_OUT, total)

    return df


# ---------------------------------------------------------------------------
# Step 2b — TransformationShare(%) for ESG-Fx
# ---------------------------------------------------------------------------

_TRANSFORM_TIME = 'TransformationTime(ms)'
_TRANSFORM_SHARE = 'TransformationShare(%)'


def add_transformation_share(df, approach):
    """For ESG-Fx, add a column expressing TransformationTime as a percentage
    of the algorithmic test-generation time. Inserted directly after
    TransformationTime(ms) in column order. Skipped silently for other
    approaches or if the source columns are missing."""
    if approach != 'ESG-Fx':
        return df
    if _TRANSFORM_TIME not in df.columns or _ALG_OUT not in df.columns:
        return df

    tr = pd.to_numeric(df[_TRANSFORM_TIME], errors='coerce')
    alg = pd.to_numeric(df[_ALG_OUT], errors='coerce')
    # Avoid divide-by-zero: where alg == 0, leave NaN.
    share = (tr / alg.where(alg != 0)) * 100.0

    if _TRANSFORM_SHARE in df.columns:
        df = df.drop(columns=[_TRANSFORM_SHARE])
    cols = list(df.columns)
    insert_at = cols.index(_TRANSFORM_TIME) + 1
    df.insert(insert_at, _TRANSFORM_SHARE, share)
    return df

_COLUMN_ORDER = {
    'ESG-Fx': [
        'Approach', 'Coverage Type', 'RunID', 'ProductID',
        'TotalTestGenTime(ms)', 'AlgTestGenTime(ms)',
        'ESGFxModelLoadTime(ms)', 'TestCaseRecordingTime(ms)',
        'TransformationTime(ms)', 'TransformationShare(%)',
        'TestGenPeakMemory(MB)',
        'TestExecTime(ms)', 'TestExecPeakMemory(MB)',
        'NumberOfESGFxVertices', 'NumberOfESGFxEdges',
        'NumberOfESGFxTestCases', 'NumberOfESGFxTestEvents',
        'EventCoverage(%)', 'EventCoverageAnalysisTime(ms)',
        'EdgeCoverage(%)',  'EdgeCoverageAnalysisTime(ms)',
        'Status', 'ErrorReason',
    ],
    'EFG': [
        'Approach', 'Coverage Type', 'RunID', 'ProductID',
        'GuitarGenTime(ms)',
        'TestGenPeakMemory(MB)',
        'TestExecTime(ms)', 'TestExecPeakMemory(MB)',
        'ESGFxModelLoadTime(ms)',
        'NumberOfESGFxVertices', 'NumberOfESGFxEdges',
        'NumberOfEFGVertices', 'NumberOfEFGEdges',
        'NumberOfEFGTestCases', 'NumberOfEFGTestEvents',
        'ParsingTime(ms)',
        'EventCoverage(%)', 'EventCoverageAnalysisTime(ms)',
        'EdgeCoverage(%)',  'EdgeCoverageAnalysisTime(ms)',
        'Status', 'ErrorReason',
    ],
    'RandomWalk': [
        'Approach', 'Coverage Type', 'RunID', 'ProductID',
        'TotalTestGenTime(ms)', 'AlgTestGenTime(ms)',
        'ESGFxModelLoadTime(ms)', 'TestCaseRecordingTime(ms)',
        'TestGenPeakMemory(MB)',
        'TestExecTime(ms)', 'TestExecPeakMemory(MB)',
        'NumberOfESGFxVertices', 'NumberOfESGFxEdges',
        'NumberOfESGFxTestCases', 'NumberOfESGFxTestEvents',
        'EventCoverage(%)', 'EventCoverageAnalysisTime(ms)',
        'EdgeCoverage(%)',  'EdgeCoverageAnalysisTime(ms)',
        'SafetyLimitHit', 'AchievedCoverageIfHit', 'AbortedSequences',
        'Status', 'ErrorReason',
    ],
}


def reorder_columns(df, approach, sheet_name):
    """Return df with columns in the canonical order; any unexpected columns
    are appended at the end (with a notice) so nothing is silently dropped."""
    desired = _COLUMN_ORDER.get(approach, list(df.columns))
    present = [c for c in desired if c in df.columns]
    leftover = [c for c in df.columns if c not in desired]
    if leftover:
        print(f"  NOTE ({sheet_name}): extra columns appended at end: {leftover}")
    return df[present + leftover]


# ---------------------------------------------------------------------------
# Sheet ordering and column sizing
# ---------------------------------------------------------------------------

_APPROACH_ORDER = {'ESG-Fx': 0, 'EFG': 1, 'RandomWalk': 2}


def _level_sort_key(level):
    s = str(level)
    if s.startswith('L') and s[1:].isdigit():
        return int(s[1:])
    return 10**6


def _sheet_sort_key(pair):
    approach, level = pair
    return (_APPROACH_ORDER.get(approach, 10**6),
            _level_sort_key(level),
            str(approach), str(level))


def _autosize_header_widths(sheet, columns, pad=2):
    for idx, col in enumerate(columns, start=1):
        width = len(str(col)) + pad
        sheet.column_dimensions[get_column_letter(idx)].width = width


# ---------------------------------------------------------------------------
# Per-SPL processing
# ---------------------------------------------------------------------------

def process_per_product(spl_dir):
    print(f"\n[PER-PRODUCT] {spl_dir.name}")
    testseq_dir = spl_dir / "testsequences"
    efg_dir     = spl_dir / "EFGs" / "efg_results"

    all_dfs = []

    if testseq_dir.exists():
        for csv_file in testseq_dir.rglob("*PerProductLog*.csv"):
            level = csv_file.parent.name
            approach = "RandomWalk" if "RandomWalk" in csv_file.name else "ESG-Fx"
            df = read_per_product_csv(csv_file, approach, level)
            if df is not None:
                df = apply_renames(df, approach)
                df = redefine_test_gen_time(df, approach)
                df = add_transformation_share(df, approach)
                all_dfs.append(df)

    if efg_dir.exists():
        for csv_file in efg_dir.rglob("*PerProductLog*.csv"):
            level = csv_file.parent.name
            df = read_per_product_csv(csv_file, "EFG", level)
            if df is not None:
                df = apply_renames(df, "EFG")
                all_dfs.append(df)

    if not all_dfs:
        print("  WARNING: No PerProduct CSVs found.")
        return

    final_df = pd.concat(all_dfs, ignore_index=True)
    output_path = spl_dir / f"RQ1_{spl_dir.name}_perProduct.xlsx"

    groups = dict(tuple(final_df.groupby(['Approach', 'Coverage Type'])))
    ordered_keys = sorted(groups.keys(), key=_sheet_sort_key)

    with pd.ExcelWriter(output_path, engine='openpyxl') as writer:
        for (approach, level) in ordered_keys:
            group_df = groups[(approach, level)]
            sheet_name = f"{approach}_{level}"[:31]
            clean = group_df.dropna(axis=1, how='all')
            clean = reorder_columns(clean, approach, sheet_name)
            clean.to_excel(writer, sheet_name=sheet_name, index=False)
            _autosize_header_widths(writer.sheets[sheet_name], clean.columns)

    print(f"  DONE: {output_path.name}")


# ---------------------------------------------------------------------------
# Entry point
# ---------------------------------------------------------------------------

def main():
    root = find_project_root()
    if root is None:
        print("ERROR: Project root not found. Run the script from inside the "
              "project tree, or make sure a 'files/Cases' directory is "
              "reachable from the current working directory or the script "
              "location.", file=sys.stderr)
        sys.exit(1)

    print(f"Project root: {root}")
    cases_dir = root / "files" / "Cases"
    spls = [d for d in sorted(cases_dir.iterdir())
            if d.is_dir() and not d.name.startswith('_')]

    for spl_dir in spls:
        process_per_product(spl_dir)

    print("\nrq1_02 DONE.")


if __name__ == "__main__":
    main()