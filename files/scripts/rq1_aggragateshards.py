#!/usr/bin/env python3
"""
rq1_aggregate_shards.py — Aggregates shard-level CSV files for RQ1
====================================================================
Each shard CSV contains per-run summaries for a subset of products.
This script produces two outputs per case study:

1. RQ1_rawData_{case}.xlsx  — Raw shard data concatenated (for traceability)
2. RQ1_aggregated_{case}.xlsx — Shards merged by Run ID (for statistical analysis)

The aggregated file has 11 rows per sheet (one per run) with metrics
summed/maxed/averaged across shards, ready for median/IQR computation.

Aggregation rules per column type:
  - Time columns:    SUM (total CPU time across all shards)
  - Peak Memory:     MAX (highest peak across shards)
  - Counts:          SUM (vertices, edges, test cases, products, etc.)
  - Coverage %:      WEIGHTED AVERAGE (weighted by Processed Products)
  - Safety Limit Avg: WEIGHTED AVERAGE (weighted by Safety Limit Hit Count)

Usage:
    python3 rq1_aggregate_shards.py [cases_root]
    Default: /Users/dilekozturk/git/esg-with-feature-expressions/files/Cases
"""

import os
import sys
import glob
import pandas as pd
from openpyxl import Workbook
from openpyxl.styles import Font, PatternFill, Alignment, Border, Side

# =============================================================================
# CONFIGURATION
# =============================================================================

DEFAULT_CASES_ROOT = "/Users/dilekozturk/git/esg-with-feature-expressions/files/Cases"

# Column classification per approach
# Keys: 'sum', 'max', 'weighted_avg', 'key', 'skip'

EFG_RULES = {
    'key':  ['Run ID', 'SPL Name', 'Coverage Type'],
    'sum':  ['Total Elapsed Time(ms)', 'Total TestGenTime(ms)', 'Total Parse Time (ms)',
             'Total NumberOfEFGVertices', 'Total NumberOfEFGEdges',
             'Total NumberOfEFGTestCases', 'Total NumberOfEFGTestEvents',
             'Event Coverage Analysis Time(ms)', 'Edge Coverage Analysis Time(ms)',
             'Total TestExecTime(ms)',
             'Total ESGFx_Vertices', 'Total ESGFx_Edges', 'Total ESGFxModelLoadTimeMs',
             'Processed Products', 'Failed Products'],
    'max':  ['Total TestGenPeakMemory(MB)', 'Total TestExecPeakMemory(MB)'],
    'weighted_avg': {
        'Event Coverage(%)': 'Processed Products',
        'Edge Coverage(%)': 'Processed Products',
    },
}

ESGFX_RULES = {
    'key':  ['Run ID', 'SPL Name', 'Coverage Type'],
    'sum':  ['Total Elapsed Time(ms)', 'Test Generation Time(ms)', 'Transformation Time(ms)',
             'Number of ESGFx Vertices', 'Number of ESGFx Edges',
             'Number of ESGFx Test Cases', 'Number of ESGFx Test Events',
             'Test Case Recording Time(ms)', 'Coverage Analysis Time(ms)',
             'Test Execution Time(ms)', 'ESGFx Model Load Time(ms)',
             'Processed Products', 'Failed Products'],
    'max':  ['Test Generation Peak Memory(MB)', 'Test Execution Peak Memory(MB)'],
    'weighted_avg': {},  # Coverage column name varies by L level, handled dynamically
}

RW_RULES = {
    'key':  ['Run ID', 'SPL Name', 'Coverage Type'],
    'sum':  ['Total Elapsed Time(ms)', 'Model Load Time(ms)', 'Test Gen Time(ms)',
             'Total Vertices', 'Total Edges',
             'Total Test Cases', 'Total Test Events', 'Aborted Sequences',
             'TestCase Recording Time(ms)',
             'Event Coverage Analysis  Time(ms)', 'Edge Coverage Analysis  Time(ms)',
             'Test Exec Time(ms)',
             'Safety Limit Hit Count',
             'Processed Products', 'Failed Products'],
    'max':  ['Test Gen Peak Memory(MB)', 'Test Exec Peak Memory(MB)'],
    'weighted_avg': {
        'Event Coverage(%)': 'Processed Products',
        'Edge Coverage(%)': 'Processed Products',
        'Avg Time on Safety Limit(ms)': 'Safety Limit Hit Count',
        'Avg Steps on Safety Limit': 'Safety Limit Hit Count',
        'Avg Edge Coverage on Safety Limit(%)': 'Safety Limit Hit Count',
    },
}


def detect_approach(columns):
    """Detect which approach a CSV belongs to based on its columns."""
    col_set = set(c.strip() for c in columns)
    if 'Total NumberOfEFGVertices' in col_set or 'Total NumberOfEFGTestCases' in col_set:
        return 'EFG', EFG_RULES
    elif 'Transformation Time(ms)' in col_set or 'Test Case Recording Time(ms)' in col_set:
        return 'ESG-Fx', ESGFX_RULES
    elif 'Aborted Sequences' in col_set or 'Safety Limit Hit Count' in col_set:
        return 'RandomWalk', RW_RULES
    else:
        return 'Unknown', None


def find_coverage_pct_columns(columns):
    """Find coverage percentage columns dynamically (ESG-Fx uses L2 Percent(%), L3 Percent(%), etc.)."""
    pct_cols = {}
    for col in columns:
        col_stripped = col.strip()
        if 'Percent(%)' in col_stripped or 'Coverage(%)' in col_stripped:
            pct_cols[col_stripped] = 'Processed Products'
    return pct_cols


# =============================================================================
# AGGREGATION ENGINE
# =============================================================================

def aggregate_shards(shard_dfs, rules, all_columns):
    """
    Aggregates multiple shard DataFrames into one summary by Run ID.
    
    Returns: DataFrame with one row per Run ID.
    """
    combined = pd.concat(shard_dfs, ignore_index=True)

    # Clean column names
    combined.columns = [c.strip() for c in combined.columns]

    key_cols = [c for c in rules['key'] if c in combined.columns]
    if not key_cols:
        return combined  # Can't aggregate without keys

    # Detect dynamic coverage columns for ESG-Fx
    weighted_avg_cols = dict(rules.get('weighted_avg', {}))
    for col in combined.columns:
        if 'Percent(%)' in col or 'Coverage(%)' in col:
            if col not in weighted_avg_cols and col not in rules.get('sum', []):
                weighted_avg_cols[col] = 'Processed Products'

    # Build aggregation dict
    agg_dict = {}
    sum_cols = [c for c in rules.get('sum', []) if c in combined.columns]
    max_cols = [c for c in rules.get('max', []) if c in combined.columns]

    for col in sum_cols:
        agg_dict[col] = 'sum'
    for col in max_cols:
        agg_dict[col] = 'max'

    # Weighted average columns need custom handling
    wavg_cols_present = {k: v for k, v in weighted_avg_cols.items()
                         if k in combined.columns and v in combined.columns}

    # For weighted avg cols, we first compute weight * value, then divide by sum of weights
    # Add them to sum temporarily so groupby doesn't drop them
    temp_weighted_cols = {}
    for val_col, weight_col in wavg_cols_present.items():
        temp_name = f'_wavg_numerator_{val_col}'
        combined[temp_name] = combined[val_col] * combined[weight_col]
        agg_dict[temp_name] = 'sum'
        temp_weighted_cols[val_col] = (temp_name, weight_col)
        # Don't aggregate the original — we'll recalculate
        if val_col in agg_dict:
            del agg_dict[val_col]

    # Any remaining numeric columns not classified
    for col in combined.columns:
        if col in key_cols or col in agg_dict or col in wavg_cols_present:
            continue
        if col.startswith('_wavg_'):
            continue
        if combined[col].dtype in ['float64', 'int64', 'float32', 'int32']:
            agg_dict[col] = 'sum'  # Default: sum

    # Group and aggregate
    result = combined.groupby(key_cols, as_index=False).agg(agg_dict)

    # Compute weighted averages
    for val_col, (num_col, weight_col) in temp_weighted_cols.items():
        weight_sum = combined.groupby(key_cols, as_index=False)[weight_col].sum()
        result = result.merge(weight_sum, on=key_cols, suffixes=('', '_total_weight'))

        weight_total_col = f'{weight_col}_total_weight' if f'{weight_col}_total_weight' in result.columns else weight_col
        result[val_col] = result[num_col] / result[weight_total_col].replace(0, 1)

        # Cleanup temp columns
        result.drop(columns=[num_col], inplace=True, errors='ignore')
        if weight_total_col != weight_col:
            result.drop(columns=[weight_total_col], inplace=True, errors='ignore')

    # Reorder columns to match original
    final_cols = [c for c in all_columns if c in result.columns]
    remaining = [c for c in result.columns if c not in final_cols and not c.startswith('_')]
    result = result[final_cols + remaining]

    return result.sort_values(by=key_cols).reset_index(drop=True)


# =============================================================================
# EXCEL STYLING
# =============================================================================

HEADER_FONT = Font(name='Arial', bold=True, size=11, color='FFFFFF')
HEADER_FILL = PatternFill(start_color='4472C4', end_color='4472C4', fill_type='solid')
CELL_FONT = Font(name='Arial', size=10)
THIN_BORDER = Border(
    left=Side(style='thin'), right=Side(style='thin'),
    top=Side(style='thin'), bottom=Side(style='thin')
)


def style_sheet(ws, df):
    """Apply professional styling."""
    for col_idx in range(1, len(df.columns) + 1):
        cell = ws.cell(row=1, column=col_idx)
        cell.font = HEADER_FONT
        cell.fill = HEADER_FILL
        cell.alignment = Alignment(horizontal='center', wrap_text=True)
        cell.border = THIN_BORDER

    for row_idx in range(2, len(df) + 2):
        for col_idx in range(1, len(df.columns) + 1):
            cell = ws.cell(row=row_idx, column=col_idx)
            cell.font = CELL_FONT
            cell.border = THIN_BORDER
            if isinstance(cell.value, float):
                cell.number_format = '0.00'

    for col_idx in range(1, len(df.columns) + 1):
        header = str(df.columns[col_idx - 1])
        max_len = min(max(len(header), 10), 30)
        ws.column_dimensions[ws.cell(row=1, column=col_idx).column_letter].width = max_len + 3

    ws.freeze_panes = 'A2'


# =============================================================================
# MAIN PIPELINE
# =============================================================================

def process_cases(cases_root):
    """Main pipeline: discovers, concatenates, aggregates, writes Excel."""
    search_pattern = os.path.join(cases_root, "*", "comparativeEfficiencyTestPipeline", "*", "*", "*.csv")
    all_files = glob.glob(search_pattern)

    if not all_files:
        print("No CSV files found.")
        return

    print(f"Found {len(all_files)} CSV files.")

    # Organize: case -> sheet_name -> list of (filepath, df)
    cases_data = {}

    for filepath in sorted(all_files):
        try:
            path_parts = filepath.split(os.sep)
            pipeline_idx = path_parts.index("comparativeEfficiencyTestPipeline")
            case_name = path_parts[pipeline_idx - 1]
            approach_dir = path_parts[pipeline_idx + 1]
            level_dir = path_parts[pipeline_idx + 2]

            sheet_name = f"{approach_dir}_{level_dir}"[:31]

            df = pd.read_csv(filepath, sep=';', decimal=',')
            df.columns = [str(c).strip() for c in df.columns]

            if case_name not in cases_data:
                cases_data[case_name] = {}
            if sheet_name not in cases_data[case_name]:
                cases_data[case_name][sheet_name] = []

            cases_data[case_name][sheet_name].append(df)

        except Exception as e:
            print(f"  Error reading {filepath}: {e}")

    # Process each case
    for case_name in sorted(cases_data.keys()):
        sheets_dict = cases_data[case_name]
        output_dir = os.path.join(cases_root, case_name, "comparativeEfficiencyTestPipeline")
        os.makedirs(output_dir, exist_ok=True)

        raw_path = os.path.join(output_dir, f"RQ1_rawData_{case_name}.xlsx")
        agg_path = os.path.join(output_dir, f"RQ1_aggregated_{case_name}.xlsx")

        print(f"\n{'─' * 50}")
        print(f"Case: {case_name}")
        print(f"  Sheets: {sorted(sheets_dict.keys())}")

        # === RAW DATA (concat only) ===
        with pd.ExcelWriter(raw_path, engine='openpyxl') as writer:
            for sheet_name in sorted(sheets_dict.keys()):
                combined = pd.concat(sheets_dict[sheet_name], ignore_index=True)

                # Sort by Run ID + shard order
                sort_cols = []
                for col in combined.columns:
                    if col.strip().lower().replace(' ', '') == 'runid':
                        sort_cols.append(col)
                        break

                if sort_cols:
                    combined = combined.sort_values(by=sort_cols).reset_index(drop=True)

                combined.to_excel(writer, sheet_name=sheet_name, index=False, float_format="%.2f")
                print(f"  Raw {sheet_name}: {len(combined)} rows ({len(sheets_dict[sheet_name])} shards)")

        # Style raw file
        from openpyxl import load_workbook
        wb = load_workbook(raw_path)
        for ws_name in wb.sheetnames:
            ws = wb[ws_name]
            # Just style headers
            for col_idx in range(1, ws.max_column + 1):
                cell = ws.cell(row=1, column=col_idx)
                cell.font = HEADER_FONT
                cell.fill = HEADER_FILL
                cell.alignment = Alignment(horizontal='center', wrap_text=True)
            ws.freeze_panes = 'A2'
        wb.save(raw_path)
        print(f"  Saved: {raw_path}")

        # === AGGREGATED DATA (merged by Run ID) ===
        with pd.ExcelWriter(agg_path, engine='openpyxl') as writer:
            for sheet_name in sorted(sheets_dict.keys()):
                shard_dfs = sheets_dict[sheet_name]
                all_columns = list(shard_dfs[0].columns)

                # Detect approach from columns
                approach_type, rules = detect_approach(all_columns)

                if rules is None:
                    print(f"  WARNING: Could not detect approach for {sheet_name}. Falling back to sum.")
                    combined = pd.concat(shard_dfs, ignore_index=True)
                    combined.to_excel(writer, sheet_name=sheet_name, index=False, float_format="%.2f")
                    continue

                agg_df = aggregate_shards(shard_dfs, rules, all_columns)

                agg_df.to_excel(writer, sheet_name=sheet_name, index=False, float_format="%.2f")
                print(f"  Agg {sheet_name} ({approach_type}): {len(agg_df)} rows "
                      f"(from {len(shard_dfs)} shards × {len(shard_dfs[0])} runs)")

        # Style aggregated file
        wb = load_workbook(agg_path)
        for ws_name in wb.sheetnames:
            ws = wb[ws_name]
            for col_idx in range(1, ws.max_column + 1):
                cell = ws.cell(row=1, column=col_idx)
                cell.font = HEADER_FONT
                cell.fill = HEADER_FILL
                cell.alignment = Alignment(horizontal='center', wrap_text=True)
                cell.border = THIN_BORDER
            for row_idx in range(2, ws.max_row + 1):
                for col_idx in range(1, ws.max_column + 1):
                    cell = ws.cell(row=row_idx, column=col_idx)
                    cell.font = CELL_FONT
                    cell.border = THIN_BORDER
                    if isinstance(cell.value, float):
                        cell.number_format = '0.00'
            ws.freeze_panes = 'A2'
        wb.save(agg_path)
        print(f"  Saved: {agg_path}")


def main():
    cases_root = sys.argv[1] if len(sys.argv) > 1 else DEFAULT_CASES_ROOT
    print(f"Cases root: {cases_root}")
    print(f"{'=' * 50}")
    process_cases(cases_root)
    print(f"\n{'=' * 50}")
    print("All cases processed.")


if __name__ == '__main__':
    main()