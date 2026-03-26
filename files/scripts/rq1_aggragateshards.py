#!/usr/bin/env python3
"""
rq1_aggregate_shards.py — Aggregates shard-level CSV files for RQ1
====================================================================
FIXED VERSION: Handles SPL name mismatches between folder names and file prefixes.

Usage:
    python3 rq1_aggragateshards.py
"""

import os
import sys
import glob
import pandas as pd
from pathlib import Path
from openpyxl import Workbook
from openpyxl.styles import Font, PatternFill, Alignment, Border, Side

# =============================================================================
# SPL NAME MAPPING
# =============================================================================

SPL_NAME_MAPPING = {
    "SodaVendingMachine": "SVM",
    "eMail": "eM",
    "Elevator": "El",
    "BankAccountv2": "BAv2",
    "StudentAttendanceSystem": "SAS",
    "Tesla": "Te",
    "syngovia": "Svia",
    "HockertyShirts": "HS"
}

# =============================================================================
# PROJECT ROOT FINDER
# =============================================================================

def find_project_root():
    """Find project root by looking for 'files/Cases' directory."""
    current = Path.cwd()
    
    if (current / "files" / "Cases").exists():
        return current
    if (current.parent / "files" / "Cases").exists():
        return current.parent
    if (current.parent.parent / "files" / "Cases").exists():
        return current.parent.parent
    if current.name == "scripts" and current.parent.name == "files":
        return current.parent.parent
    
    return None

# =============================================================================
# COLUMN CLASSIFICATION
# =============================================================================

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
    'weighted_avg': {},  # Handled dynamically
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
    """Detect which approach a CSV belongs to."""
    col_set = set(c.strip() for c in columns)
    if 'Total NumberOfEFGVertices' in col_set or 'Total NumberOfEFGTestCases' in col_set:
        return 'EFG', EFG_RULES
    elif 'Transformation Time(ms)' in col_set or 'Test Case Recording Time(ms)' in col_set:
        return 'ESG-Fx', ESGFX_RULES
    elif 'Aborted Sequences' in col_set or 'Safety Limit Hit Count' in col_set:
        return 'RandomWalk', RW_RULES
    else:
        return 'Unknown', {}


# =============================================================================
# AGGREGATION LOGIC
# =============================================================================

def aggregate_by_run_id(df, rules):
    """Aggregate shards by Run ID according to rules."""
    if df.empty or 'Run ID' not in df.columns:
        return df
    
    grouped = df.groupby('Run ID', sort=False)
    aggregated_rows = []
    
    for run_id, group in grouped:
        row = {'Run ID': run_id}
        
        # Keep first value for key columns
        for col in rules.get('key', []):
            if col in group.columns and col != 'Run ID':
                row[col] = group[col].iloc[0]
        
        # Sum columns
        for col in rules.get('sum', []):
            if col in group.columns:
                row[col] = group[col].sum()
        
        # Max columns
        for col in rules.get('max', []):
            if col in group.columns:
                row[col] = group[col].max()
        
        # Weighted averages
        for col, weight_col in rules.get('weighted_avg', {}).items():
            if col in group.columns and weight_col in group.columns:
                weights = group[weight_col]
                values = group[col]
                total_weight = weights.sum()
                if total_weight > 0:
                    row[col] = (values * weights).sum() / total_weight
                else:
                    row[col] = 0.0
        
        # Handle ESG-Fx coverage columns dynamically
        if 'approach' in locals() and approach == 'ESG-Fx':
            for col in group.columns:
                if col.startswith('L') and 'Coverage(%)' in col and col not in row:
                    weight_col = 'Processed Products'
                    if weight_col in group.columns:
                        weights = group[weight_col]
                        values = group[col]
                        total_weight = weights.sum()
                        if total_weight > 0:
                            row[col] = (values * weights).sum() / total_weight
        
        aggregated_rows.append(row)
    
    result = pd.DataFrame(aggregated_rows)
    
    # Preserve column order from original
    ordered_cols = [c for c in df.columns if c in result.columns]
    return result[ordered_cols]


# =============================================================================
# FILE DISCOVERY WITH SPL NAME MAPPING
# =============================================================================

def discover_shard_files(base_dir, spl_folder_name):
    """
    Discover shard CSV files for a given SPL.
    Handles name mismatch between folder name and file prefix.
    """
    spl_file_prefix = SPL_NAME_MAPPING.get(spl_folder_name, spl_folder_name)
    
    shard_files = {}
    
    # Try both folder name and file prefix patterns
    patterns = [
        f"{spl_file_prefix}_*.csv",
        f"{spl_folder_name}_*.csv"
    ]
    
    for pattern in patterns:
        files = list(base_dir.glob(pattern))
        if files:
            for f in files:
                # Extract approach from filename
                fname = f.name
                if '_EFG_' in fname:
                    approach = 'EFG'
                elif '_ESG-Fx_' in fname or '_RandomWalk_' in fname:
                    approach = 'ESG-Fx' if '_ESG-Fx_' in fname else 'RandomWalk'
                else:
                    continue
                
                if approach not in shard_files:
                    shard_files[approach] = []
                shard_files[approach].append(f)
    
    return shard_files


# =============================================================================
# PROCESS SINGLE SPL
# =============================================================================

def process_spl(spl_folder_name, base_dir, output_dir):
    """Process all shard files for a single SPL."""
    
    print(f"\n{'='*80}")
    print(f"📊 PROCESSING: {spl_folder_name}")
    spl_file_prefix = SPL_NAME_MAPPING.get(spl_folder_name, spl_folder_name)
    if spl_file_prefix != spl_folder_name:
        print(f"   File prefix: {spl_file_prefix}")
    print(f"{'='*80}")
    print(f"Input:  {base_dir}")
    print(f"Output: {output_dir}")
    
    shard_files = discover_shard_files(base_dir, spl_folder_name)
    
    if not shard_files:
        print("  ⚠️  No shard files found")
        return False
    
    all_raw_data = []
    all_agg_data = []
    
    for approach, files in sorted(shard_files.items()):
        print(f"\n  🔸 {approach}: {len(files)} shard files")
        
        # Read all shards
        dfs = []
        for f in sorted(files):
            try:
                df = pd.read_csv(f, sep=';', decimal=',')
                dfs.append(df)
            except Exception as e:
                print(f"     ❌ Error reading {f.name}: {e}")
                continue
        
        if not dfs:
            continue
        
        # Concatenate
        raw_data = pd.concat(dfs, ignore_index=True)
        print(f"     Combined: {len(raw_data)} rows")
        
        # Detect approach and aggregate
        detected_approach, rules = detect_approach(raw_data.columns)
        
        if rules:
            agg_data = aggregate_by_run_id(raw_data, rules)
            print(f"     Aggregated: {len(agg_data)} runs")
            
            raw_data['Approach'] = approach
            agg_data['Approach'] = approach
            
            all_raw_data.append(raw_data)
            all_agg_data.append(agg_data)
        else:
            print(f"     ⚠️  Unknown format, skipping aggregation")
    
    if not all_raw_data:
        return False
    
    # Save combined Excel files
    print(f"\n  💾 Saving files...")
    
    raw_combined = pd.concat(all_raw_data, ignore_index=True)
    agg_combined = pd.concat(all_agg_data, ignore_index=True)
    
    raw_path = output_dir / f"RQ1_rawData_{spl_folder_name}.xlsx"
    agg_path = output_dir / f"RQ1_aggregated_{spl_folder_name}.xlsx"
    
    try:
        raw_combined.to_excel(raw_path, index=False, engine='openpyxl')
        print(f"     ✅ Raw: {raw_path.name} ({len(raw_combined)} rows)")
    except Exception as e:
        print(f"     ❌ Failed to save raw: {e}")
    
    try:
        agg_combined.to_excel(agg_path, index=False, engine='openpyxl')
        print(f"     ✅ Aggregated: {agg_path.name} ({len(agg_combined)} rows)")
    except Exception as e:
        print(f"     ❌ Failed to save aggregated: {e}")
    
    return True


# =============================================================================
# MAIN
# =============================================================================

def main():
    print("="*80)
    print("RQ1 SHARD AGGREGATION - AUTOMATIC")
    print("="*80)
    
    project_root = find_project_root()
    
    if not project_root:
        print("❌ ERROR: Could not find project root!")
        sys.exit(1)
    
    cases_dir = project_root / "files" / "Cases"
    print(f"Project root: {project_root}")
    print(f"Cases dir:    {cases_dir}")
    
    # Discover SPLs
    print("\n🔍 Discovering SPLs...")
    spls = [d for d in sorted(cases_dir.iterdir()) if d.is_dir()]
    
    if not spls:
        print("❌ No SPL directories found!")
        sys.exit(1)
    
    print(f"✅ Found {len(spls)} SPLs:")
    for spl_dir in spls:
        file_prefix = SPL_NAME_MAPPING.get(spl_dir.name, spl_dir.name)
        if file_prefix != spl_dir.name:
            print(f"   - {spl_dir.name:25s} (files: {file_prefix}_*.csv)")
        else:
            print(f"   - {spl_dir.name}")
    
    print("\n" + "="*80)
    print("PROCESSING SPLs")
    print("="*80)
    
    processed_count = 0
    failed_count = 0
    
    for spl_dir in spls:
        base_dir = spl_dir / "rq1_shardfiles"
        output_dir = spl_dir / "rq1_shardfiles"
        
        if not base_dir.exists():
            print(f"\n⚠️  Skipping {spl_dir.name}: no rq1_shardfiles directory")
            continue
        
        try:
            success = process_spl(spl_dir.name, base_dir, output_dir)
            if success:
                processed_count += 1
            else:
                failed_count += 1
        except Exception as e:
            print(f"\n❌ ERROR processing {spl_dir.name}: {e}")
            import traceback
            traceback.print_exc()
            failed_count += 1
    
    print("\n" + "="*80)
    print("SUMMARY")
    print("="*80)
    print(f"✅ Successfully processed: {processed_count} SPLs")
    if failed_count > 0:
        print(f"❌ Failed: {failed_count} SPLs")
    print("\nOutput files in each SPL's rq1_shardfiles/ directory:")
    print("  - RQ1_rawData_<SPL>.xlsx")
    print("  - RQ1_aggregated_<SPL>.xlsx")
    print("="*80)


if __name__ == "__main__":
    main()