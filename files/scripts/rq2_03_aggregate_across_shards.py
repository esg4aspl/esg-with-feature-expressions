#!/usr/bin/env python3
"""
rq2_03_aggregate_across_shards.py  (Step 3 of 3)

Input : files/Cases/<SPL>/RQ2_summary_<SPL>.xlsx        (from Step 2)
Output: files/Cases/<SPL>/RQ2_final_summary_<SPL>.xlsx  (per-SPL, 8 approach blocks)
        files/Cases/RQ2_SPL_Summary.xlsx                (global, one sheet per SPL)

Collapses 80 per-shard-median rows into a single row per Approach_Level.

Aggregation rules (applied per column, per sheet):
    SUM         - count columns (products, vertices, edges, test cases,
                  test events, Aborted Sequences, Safety Limit Hit Count,
                  Processed Products, Failed Products).
    MAX         - any column whose name contains 'Peak Memory' (peak memory
                  cannot be meaningfully summed across parallel processes).
    SUM + MAX   - every time column (column name ending in 'Time(ms)').
                  The original-named column keeps SUM -- the total CPU time
                  across all 80 shards, i.e. the serial cost of processing
                  the whole SPL on one core (hardware-independent, per-
                  product algorithmic cost = SUM / Processed Products).
                  A companion column '<name> [wall-clock]' holds MAX across
                  shards -- the wall-clock time for one pass through the
                  SPL under 80-way shard parallelism (throughput in
                  products/second = Processed Products / [wall-clock]/1000).
                  The two together bracket algorithmic cost and practical
                  end-to-end latency.
    WAVG        - coverage percentages, weighted by the appropriate denom:
                    * 'Event Coverage(%)', 'Edge Coverage(%)', 'L1 Coverage(%)'
                      -> weighted by 'Processed Products'
                    * 'Avg Time on Safety Limit(ms)',
                      'Avg Steps on Safety Limit',
                      'Avg Coverage at Safety Limit(%)'
                      -> weighted by 'Safety Limit Hit Count'
                      (these are per-hit averages, not per-product; summing
                       them across shards is meaningless)

Known deviations from the original script:
  1. 'Avg Coverage at Safety Limit(%)' no longer falls through the classifier
     (the old substring check "'Coverage(%)' in name" silently failed on this
     column and summed it). It is now WAVG by hit count.
  2. 'Avg Time/Steps on Safety Limit' are WAVG by hit count, not SUM.
  3. Classifier uses endswith('Coverage(%)') instead of substring match so
     the distinction between main coverages and safety-limit coverage is
     unambiguous.
  4. Time columns now emit both SUM and MAX (two output columns per source
     column). SUM keeps the original column name for backward compatibility
     with downstream analysis; MAX is suffixed with '[wall-clock]'.
"""

import pandas as pd
import numpy as np
import sys
from pathlib import Path
from openpyxl import Workbook
from openpyxl.styles import Font, Alignment, Border, Side
from openpyxl.utils import get_column_letter

SHEET_ORDER = [
    'ESG-Fx_L1', 'ESG-Fx_L2', 'ESG-Fx_L3', 'ESG-Fx_L4',
    'EFG_L2', 'EFG_L3', 'EFG_L4',
    'RandomWalk_L0',
]

SPL_ORDER = [
    'SodaVendingMachine', 'eMail', 'Elevator', 'BankAccountv2',
    'StudentAttendanceSystem', 'syngovia', 'Tesla', 'HockertyShirts',
]

# Columns whose values are per-hit averages; must be weighted by hit count.
SAFETY_LIMIT_AVG_COLS = {
    'Avg Time on Safety Limit(ms)',
    'Avg Steps on Safety Limit',
    'Avg Coverage at Safety Limit(%)',
}

THIN_BORDER = Border(
    left=Side(style='thin'), right=Side(style='thin'),
    top=Side(style='thin'), bottom=Side(style='thin'),
)
HEADER_FONT = Font(bold=True, name='Calibri', size=11)
DATA_FONT = Font(name='Calibri', size=11)
HEADER_ALIGN = Alignment(horizontal='left', vertical='center')
DATA_ALIGN = Alignment(horizontal='left', vertical='center')


def aggregation_rule(col_name):
    """Return (rule, weight_column_or_None) for a given numeric column.

    Dispatch order matters: safety-limit special cases must be checked
    before the generic Time(ms) rule, because 'Avg Time on Safety Limit(ms)'
    is a per-hit average, not a wall-clock duration.
    """
    if 'Peak Memory' in col_name:
        return ('max', None)
    if col_name in SAFETY_LIMIT_AVG_COLS:
        return ('wavg', 'Safety Limit Hit Count')
    if col_name.endswith('Coverage(%)'):
        return ('wavg', 'Processed Products')
    if col_name.endswith('Time(ms)'):
        # Time columns: report BOTH total CPU time (SUM across 80 shards)
        # and wall-clock time (MAX across 80 shards) because each answers
        # a different RQ2 question (algorithmic cost vs practical latency).
        return ('sum_and_max', None)
    return ('sum', None)


def find_project_root():
    current = Path.cwd()
    for candidate in [current, current.parent, current.parent.parent]:
        if (candidate / "files" / "Cases").exists():
            return candidate
    hardcoded = Path("/Users/dilekozturk/git/esg-with-feature-expressions")
    if (hardcoded / "files" / "Cases").exists():
        return hardcoded
    return None


def auto_col_width(header_text):
    return max(len(str(header_text)) * 1.15 + 2, 8)


def summarize_one_sheet(df):
    """Collapse all shard rows of one Approach_Level into a single dict."""
    exclude = {'Shard', 'N_Runs'}
    numeric_cols = [c for c in df.columns
                    if c not in exclude and pd.api.types.is_numeric_dtype(df[c])]

    result = {'Approach': ''}
    for c in numeric_cols:
        rule, weight_col = aggregation_rule(c)
        if rule == 'max':
            result[c] = df[c].max()
        elif rule == 'sum_and_max':
            # Time column: emit SUM (original name) and MAX (with suffix).
            # Inserted as a pair so the two live next to each other in the
            # output sheet instead of being separated across the header row.
            result[c] = df[c].sum()
            result[f"{c} [wall-clock]"] = df[c].max()
        elif rule == 'wavg':
            if weight_col and weight_col in df.columns:
                weights = df[weight_col]
                tw = weights.sum()
                if tw > 0:
                    result[c] = (df[c] * weights).sum() / tw
                else:
                    # No hits / no products in any shard -> metric undefined.
                    # Emit NaN so the writer leaves the cell blank instead of
                    # writing a misleading 0.0 that looks like "zero average
                    # time", which reviewers would query. The companion count
                    # column (Safety Limit Hit Count / Processed Products)
                    # already carries the "nothing happened" signal.
                    result[c] = float('nan')
            else:
                result[c] = df[c].mean()
        else:  # sum
            result[c] = df[c].sum()
    return result


def process_spl(filepath):
    xls = pd.ExcelFile(filepath)
    available_sheets = [s for s in SHEET_ORDER if s in xls.sheet_names]
    rows_per_approach = {}
    for sheet_name in available_sheets:
        df = pd.read_excel(xls, sheet_name=sheet_name)
        row = summarize_one_sheet(df)
        row['Approach'] = sheet_name
        rows_per_approach[sheet_name] = row
    return rows_per_approach, available_sheets


def write_approaches_to_sheet(ws, rows_per_approach, available_sheets):
    """Each approach gets its own [header, data, blank] block because the
    column sets differ across approaches (EFG has Parse Time, RandomWalk has
    Safety Limit columns, etc.)."""
    current_row = 1
    max_col_widths = {}
    for sheet_name in available_sheets:
        row_data = rows_per_approach[sheet_name]
        headers = list(row_data.keys())

        for col_idx, h in enumerate(headers, 1):
            cell = ws.cell(current_row, col_idx, h)
            cell.font = HEADER_FONT
            cell.alignment = HEADER_ALIGN
            cell.border = THIN_BORDER

        for col_idx, h in enumerate(headers, 1):
            val = row_data[h]
            cell = ws.cell(current_row + 1, col_idx)
            if isinstance(val, (float, np.floating)):
                # NaN -> leave cell blank (undefined metric, e.g. no
                # safety-limit hits in any shard). See summarize_one_sheet().
                if np.isnan(val):
                    cell.value = None
                else:
                    cell.value = round(float(val), 2)
            elif isinstance(val, (int, np.integer)):
                cell.value = int(val)
            else:
                cell.value = val
            cell.font = DATA_FONT
            cell.alignment = DATA_ALIGN
            cell.border = THIN_BORDER

        for col_idx, h in enumerate(headers, 1):
            w = auto_col_width(h)
            max_col_widths[col_idx] = max(max_col_widths.get(col_idx, 0), w)
        current_row += 3

    for col_idx, w in max_col_widths.items():
        ws.column_dimensions[get_column_letter(col_idx)].width = w


def main():
    print("="*80)
    print("RQ2 STEP 3 / 3 - AGGREGATE 80 SHARDS INTO 1 ROW PER APPROACH")
    print("="*80)

    project_root = find_project_root()
    if not project_root:
        print("ERROR: Could not find project root (files/Cases/).")
        sys.exit(1)

    cases_dir = project_root / "files" / "Cases"
    files = sorted(cases_dir.rglob("RQ2_summary_*.xlsx"))
    if not files:
        print("ERROR: No RQ2_summary_*.xlsx files found! Run Step 2 first.")
        sys.exit(1)

    print(f"Found {len(files)} summary file(s)\n")
    all_spl_data = {}

    for f in files:
        spl_name = f.stem.replace("RQ2_summary_", "")
        spl_dir = f.parent
        print(f"Processing {spl_name}...")
        rows_per_approach, available_sheets = process_spl(f)

        output_path = spl_dir / f"RQ2_final_summary_{spl_name}.xlsx"
        wb = Workbook()
        ws = wb.active
        ws.title = spl_name
        write_approaches_to_sheet(ws, rows_per_approach, available_sheets)
        wb.save(output_path)
        print(f"  -> {output_path.name}")
        all_spl_data[spl_name] = (rows_per_approach, available_sheets)

    # Global cross-SPL workbook ------------------------------------------------
    global_path = cases_dir / "RQ2_SPL_Summary.xlsx"
    wb = Workbook()
    first = True
    for spl_name in SPL_ORDER:
        if spl_name not in all_spl_data:
            print(f"  [skip] {spl_name}  (no summary file)")
            continue
        rows, sheets = all_spl_data[spl_name]
        if first:
            ws = wb.active
            ws.title = spl_name
            first = False
        else:
            ws = wb.create_sheet(title=spl_name)
        write_approaches_to_sheet(ws, rows, sheets)

    # Also append any SPLs that weren't in SPL_ORDER (defensive).
    for spl_name, (rows, sheets) in all_spl_data.items():
        if spl_name in SPL_ORDER:
            continue
        ws = wb.create_sheet(title=spl_name)
        write_approaches_to_sheet(ws, rows, sheets)

    wb.save(global_path)
    print(f"\nGlobal summary: {global_path}")
    print("\nDONE.")


if __name__ == "__main__":
    main()