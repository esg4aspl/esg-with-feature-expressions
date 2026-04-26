#!/usr/bin/env python3
"""
rq1_02_aggregate_shards_perProduct.py
Step 1b of the RQ1 pipeline.

Reads PerProductLog CSV files from each SPL (produced by the Java pipelines
in comparativeEfficiencyTestPipeline/) and combines them into one Excel file
per SPL, with one sheet per approach x level combination.

In addition to straight collection, this revision redefines the reported
test-generation cost for ESG-Fx and RandomWalk sheets so that they are
scope-symmetric with the EFG baseline (where GuitarGenTime captures
input-read + algorithm + disk-write inside a single opaque sub-process):

    - ESG-Fx (any L): rename the existing algorithmic column
      ('TotalTestGenTime(ms)' or, for L1-style schemas, 'TestGenTime(ms)')
      to 'AlgTestGenTime(ms)'; add a new 'TotalTestGenTime(ms)' column
      computed as ESGFxModelLoadTimeMs + AlgTestGenTime + TestCaseRecordingTime.
    - RandomWalk: rename 'TestGenTime(ms)' to 'AlgTestGenTime(ms)'; add a new
      'TotalTestGenTime(ms)' column computed as
      ModelLoadTime(ms) + AlgTestGenTime + TestCaseRecordingTime.
    - EFG: left untouched. GuitarGenTime(ms) already represents the same
      disk-to-disk scope (GUITAR reads its .EFG input and writes its .tst
      output inside a single Java 8 sub-process).

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
# Sheet ordering and column sizing
# ---------------------------------------------------------------------------

# Desired approach order in the output workbook.
_APPROACH_ORDER = {'ESG-Fx': 0, 'EFG': 1, 'RandomWalk': 2}


def _level_sort_key(level):
    """Order coverage-type labels like L0, L1, L2, L3, L4 naturally."""
    s = str(level)
    if s.startswith('L') and s[1:].isdigit():
        return int(s[1:])
    return 10**6  # unknowns sink to the end but stay deterministic


def _sheet_sort_key(pair):
    approach, level = pair
    return (_APPROACH_ORDER.get(approach, 10**6),
            _level_sort_key(level),
            str(approach), str(level))


def _autosize_header_widths(sheet, columns, pad=2):
    """Size each column wide enough to show its header text plus a pad."""
    for idx, col in enumerate(columns, start=1):
        width = len(str(col)) + pad
        sheet.column_dimensions[get_column_letter(idx)].width = width


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
    """Read a single PerProductLog CSV, tolerant of comma vs dot decimals."""
    try:
        df = pd.read_csv(filepath, sep=';', decimal=',')
        if len(df.columns) == 1:
            df = pd.read_csv(filepath, sep=',', decimal='.')
        df.columns = [c.strip() for c in df.columns]
        df['Approach'] = approach
        df['Coverage Type'] = level
        return df
    except Exception as e:
        print(f"  ERROR: {filepath.name}: {e}")
        return None


# ---------------------------------------------------------------------------
# Column redefinition: AlgTestGenTime(ms) + new TotalTestGenTime(ms)
# ---------------------------------------------------------------------------

_ALG_OUT = 'AlgTestGenTime(ms)'
_TOTAL_OUT = 'TotalTestGenTime(ms)'

# Columns we sum (aside from AlgTestGenTime) to form the new TotalTestGenTime.
# Keys are approach labels as written by the Java pipelines.
_SYMMETRIC_COMPONENTS = {
    'ESG-Fx': {
        'alg_candidates': ['TotalTestGenTime(ms)', 'TestGenTime(ms)'],
        'model_load':     'ESGFxModelLoadTimeMs',
        'recording':      'TestCaseRecordingTime(ms)',
    },
    'RandomWalk': {
        'alg_candidates': ['TestGenTime(ms)'],
        'model_load':     'ModelLoadTime(ms)',
        'recording':      'TestCaseRecordingTime(ms)',
    },
}


def redefine_test_gen_time(df, approach):
    """In-place rename of the algorithmic column and insertion of the new
    scope-symmetric TotalTestGenTime(ms). EFG and unknown approaches are
    passed through unchanged."""
    spec = _SYMMETRIC_COMPONENTS.get(approach)
    if spec is None:
        return df

    # Find the source algorithmic column.
    alg_src = next((c for c in spec['alg_candidates'] if c in df.columns), None)
    if alg_src is None:
        print(f"  WARNING ({approach}): no algorithmic-time column found "
              f"(expected one of {spec['alg_candidates']}); skipping redefinition.")
        return df

    ml_col = spec['model_load']
    rec_col = spec['recording']
    missing = [c for c in (ml_col, rec_col) if c not in df.columns]
    if missing:
        print(f"  WARNING ({approach}): missing component columns {missing}; "
              f"skipping redefinition.")
        return df

    # Preserve original column order and insert the new Total right after Alg.
    df = df.rename(columns={alg_src: _ALG_OUT})

    # Arithmetic — pandas treats non-numeric as NaN, so the sum propagates.
    alg_v = pd.to_numeric(df[_ALG_OUT], errors='coerce')
    ml_v  = pd.to_numeric(df[ml_col],   errors='coerce')
    rec_v = pd.to_numeric(df[rec_col],  errors='coerce')
    total = alg_v + ml_v + rec_v

    cols = list(df.columns)
    insert_at = cols.index(_ALG_OUT) + 1
    # Avoid duplicate column name if a stale Total exists somewhere else.
    if _TOTAL_OUT in df.columns:
        df = df.drop(columns=[_TOTAL_OUT])
        cols = list(df.columns)
        insert_at = cols.index(_ALG_OUT) + 1
    df.insert(insert_at, _TOTAL_OUT, total)

    return df


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
                df = redefine_test_gen_time(df, approach)
                all_dfs.append(df)

    if efg_dir.exists():
        for csv_file in efg_dir.rglob("*PerProductLog*.csv"):
            level = csv_file.parent.name
            df = read_per_product_csv(csv_file, "EFG", level)
            if df is not None:
                # EFG intentionally left untouched: GuitarGenTime(ms) is
                # already the disk-to-disk measurement.
                all_dfs.append(df)

    if not all_dfs:
        print("  WARNING: No PerProduct CSVs found.")
        return

    final_df = pd.concat(all_dfs, ignore_index=True)
    output_path = spl_dir / f"RQ1_{spl_dir.name}_perProduct.xlsx"

    # Build the sheet list in the requested order (ESG-Fx → EFG → RandomWalk,
    # each ordered by L level) before writing.
    groups = dict(tuple(final_df.groupby(['Approach', 'Coverage Type'])))
    ordered_keys = sorted(groups.keys(), key=_sheet_sort_key)

    with pd.ExcelWriter(output_path, engine='openpyxl') as writer:
        for (approach, level) in ordered_keys:
            group_df = groups[(approach, level)]
            sheet_name = f"{approach}_{level}"[:31]
            clean = group_df.dropna(axis=1, how='all')
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