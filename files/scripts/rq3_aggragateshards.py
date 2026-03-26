#!/usr/bin/env python3
"""
RQ3 Fault Detection Data Aggregation Script - 4-SHEET VERSION

Each SPL gets 2 Excel files with 4 sheets each:
  RQ3_rawData_<SPL>.xlsx:
    - EdgeOmission (deterministic: ESG-Fx L0-4, EFG L2-4)
    - EventOmission (deterministic: ESG-Fx L0-4, EFG L2-4)
    - EdgeOmission_MultiSeed (Random Walk: seeds 42-51)
    - EventOmission_MultiSeed (Random Walk: seeds 42-51)
  
  RQ3_aggregated_<SPL>.xlsx:
    - EdgeOmission (aggregated by L-level)
    - EventOmission (aggregated by L-level)
    - EdgeOmission_MultiSeed (aggregated by product across seeds)
    - EventOmission_MultiSeed (aggregated by product across seeds)
"""

import pandas as pd
import sys
import os
import glob
from pathlib import Path

# SPL name mapping: folder_name -> file_prefix
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

# Expected product counts for validation
EXPECTED_PRODUCTS = {
    "Elevator": 42,
    "eMail": 23,
    "SodaVendingMachine": 12,
    "BankAccountv2": 498,
    "StudentAttendanceSystem": 2664,
    "Tesla": 400,  # Sample
    "syngovia": 400,  # Sample
    "HockertyShirts": 400  # Sample
}

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

def discover_spls_with_fault_detection(project_root):
    """Discover all SPLs that have fault detection data."""
    cases_dir = project_root / "files" / "Cases"
    
    if not cases_dir.exists():
        print(f"❌ ERROR: Cases directory not found: {cases_dir}")
        return []
    
    spls = []
    
    for spl_dir in sorted(cases_dir.iterdir()):
        if not spl_dir.is_dir():
            continue
        
        fd_dir = spl_dir / "faultdetection" / "perProduct"
        
        if fd_dir.exists() and fd_dir.is_dir():
            csv_files = list(fd_dir.glob("*.csv"))
            if csv_files:
                spls.append((spl_dir.name, spl_dir / "faultdetection"))
    
    return spls

def load_operator_data(per_product_dir, spl_folder_name, spl_file_prefix, operator, is_multiseed=False):
    """
    Load data for a specific operator (EdgeOmission or EventOmission).
    
    Returns:
        DataFrame or None if no data found
    """
    suffix = "_MultiSeedRW" if is_multiseed else ""
    
    # Try with file prefix first, then folder name
    pattern1 = f"{spl_file_prefix}_{operator}{suffix}_shard*.csv"
    pattern2 = f"{spl_folder_name}_{operator}{suffix}_shard*.csv"
    
    shard_files = sorted(glob.glob(str(per_product_dir / pattern1)))
    
    if not shard_files:
        shard_files = sorted(glob.glob(str(per_product_dir / pattern2)))
    
    if not shard_files:
        return None
    
    # Read and concatenate all shards
    dfs = []
    
    for f in shard_files:
        try:
            df = pd.read_csv(f, sep=';', decimal=',')
            dfs.append(df)
        except Exception as e:
            print(f"        ⚠️  Error reading {Path(f).name}: {e}")
            continue
    
    if not dfs:
        return None
    
    # Combine all shards
    combined = pd.concat(dfs, ignore_index=True)
    
    # Sort by ProductID for consistency
    if 'ProductID' in combined.columns:
        combined = combined.sort_values('ProductID').reset_index(drop=True)
    
    return combined

def generate_deterministic_aggregation(df, operator):
    """Aggregate deterministic ESG-Fx results by L-level and approach."""
    grouped = df.groupby(['TestingApproach'])
    
    agg_stats = grouped.agg({
        'TotalMutants': ['median', 'mean'],
        'DetectedMutants': ['median', 'mean', 'std'],
        'MutationScore(%)': ['median', 'mean', 'std', 'min', 'max'],
        'TotalEventsInSuite': ['median', 'mean', 'std'],
        'EventsToDetect': ['median', 'mean', 'std'],
        'PercentageOfSuiteToDetect(%)': ['median', 'mean', 'std']
    }).reset_index()
    
    agg_stats.columns = ['_'.join(col).strip('_') if col[1] else col[0] 
                         for col in agg_stats.columns.values]
    
    product_counts = df.groupby(['TestingApproach']).size().reset_index(name='ProductCount')
    agg_stats = agg_stats.merge(product_counts, on=['TestingApproach'])
    
    return agg_stats

def generate_multiseed_aggregation(df, operator):
    """Aggregate multi-seed Random Walk results per product."""
    grouped = df.groupby(['ProductID'])
    
    agg_stats = grouped.agg({
        'TotalMutants': 'first',
        'DetectedMutants': ['median', 'mean', 'std', 'min', 'max'],
        'MutationScore(%)': ['median', 'mean', 'std', 'min', 'max'],
        'TotalEventsInSuite': ['median', 'mean', 'std'],
        'MedianEventsToDetect': ['median', 'mean', 'std'],
        'MedianPercentageOfSuiteToDetect(%)': ['median', 'mean', 'std']
    }).reset_index()
    
    agg_stats.columns = ['_'.join(col).strip('_') if col[1] else col[0] 
                         for col in agg_stats.columns.values]
    
    return agg_stats

def validate_data_completeness(df, spl_name, operator, is_multiseed):
    """
    Validate that the data is complete and reasonable.
    Returns list of warning messages.
    """
    warnings = []
    
    # Check product count
    unique_products = df['ProductID'].nunique()
    
    if spl_name in EXPECTED_PRODUCTS:
        expected = EXPECTED_PRODUCTS[spl_name]
        pct = (unique_products / expected) * 100
        
        if pct < 90:
            warnings.append(f"⚠️  Only {unique_products}/{expected} products ({pct:.1f}%) - INCOMPLETE DATA")
        elif pct < 100:
            warnings.append(f"ℹ️  {unique_products}/{expected} products ({pct:.1f}%)")
    
    # For multi-seed, check that we have 10 seeds per product
    if is_multiseed:
        seed_counts = df.groupby('ProductID')['Seed'].nunique()
        incomplete_products = seed_counts[seed_counts < 10]
        
        if len(incomplete_products) > 0:
            warnings.append(f"⚠️  {len(incomplete_products)} products have < 10 seeds")
        
        # Check for zero variation (bug indicator)
        if len(df) > 0:
            sample_product = df['ProductID'].iloc[0]
            sample_data = df[df['ProductID'] == sample_product]
            if len(sample_data) >= 10:
                ms_std = sample_data['MutationScore(%)'].std()
                if ms_std == 0:
                    warnings.append(f"🔴 CRITICAL: Zero variation across seeds - FileToTestSuiteConverter bug detected!")
    
    return warnings

def aggregate_fault_detection(spl_folder_name, base_dir, output_dir):
    """
    Aggregate fault detection results for a given SPL.
    Creates 2 Excel files with 4 sheets each.
    """
    
    per_product_dir = base_dir / "perProduct"
    
    if not per_product_dir.exists():
        print(f"⚠️  Skipping {spl_folder_name}: perProduct directory not found")
        return False
    
    spl_file_prefix = SPL_NAME_MAPPING.get(spl_folder_name, spl_folder_name)
    
    print(f"\n{'='*80}")
    print(f"📊 PROCESSING: {spl_folder_name}")
    if spl_file_prefix != spl_folder_name:
        print(f"   File prefix: {spl_file_prefix}")
    print(f"{'='*80}")
    print(f"Input:  {per_product_dir}")
    print(f"Output: {output_dir}")
    
    operators = ["EdgeOmission", "EventOmission"]
    
    # Collect data for each sheet
    raw_sheets = {}
    agg_sheets = {}
    
    all_warnings = []
    
    for operator in operators:
        print(f"\n  🔸 {operator}")
        
        # Deterministic approaches (ESG-Fx L0-4, EFG L2-4)
        det_data = load_operator_data(per_product_dir, spl_folder_name, spl_file_prefix, operator, is_multiseed=False)
        
        if det_data is not None:
            print(f"     ✅ Deterministic: {len(det_data)} rows, {det_data['ProductID'].nunique()} products")
            
            # Validate
            warnings = validate_data_completeness(det_data, spl_folder_name, operator, is_multiseed=False)
            for w in warnings:
                print(f"        {w}")
                all_warnings.append(f"{operator}: {w}")
            
            # Store for raw data (as separate sheet)
            raw_sheets[operator] = det_data
            
            # Generate aggregated stats
            try:
                agg_stats = generate_deterministic_aggregation(det_data, operator)
                agg_sheets[operator] = agg_stats
                print(f"     ✅ Aggregated: {len(agg_stats)} rows")
            except Exception as e:
                print(f"     ❌ Aggregation failed: {e}")
        else:
            print(f"     ⚠️  No data files found")
        
        # Multi-seed Random Walk
        ms_data = load_operator_data(per_product_dir, spl_folder_name, spl_file_prefix, operator, is_multiseed=True)
        
        if ms_data is not None:
            print(f"     ✅ Multi-seed: {len(ms_data)} rows, {ms_data['ProductID'].nunique()} products")
            
            # Validate
            warnings = validate_data_completeness(ms_data, spl_folder_name, operator, is_multiseed=True)
            for w in warnings:
                print(f"        {w}")
                all_warnings.append(f"{operator}_MultiSeed: {w}")
            
            # Store for raw data (as separate sheet)
            raw_sheets[f"{operator}_MultiSeed"] = ms_data
            
            # Generate aggregated stats
            try:
                agg_stats = generate_multiseed_aggregation(ms_data, operator)
                agg_sheets[f"{operator}_MultiSeed"] = agg_stats
                print(f"     ✅ Aggregated: {len(agg_stats)} rows")
            except Exception as e:
                print(f"     ❌ Aggregation failed: {e}")
        else:
            print(f"     ⚠️  No multi-seed data files found")
    
    if not raw_sheets and not agg_sheets:
        print(f"\n  ❌ No data collected for {spl_folder_name}")
        return False
    
    # Save to Excel files with multiple sheets
    print(f"\n  💾 Saving Excel files...")
    
    # Save raw data
    if raw_sheets:
        raw_filename = f"RQ3_rawData_{spl_folder_name}.xlsx"
        raw_path = output_dir / raw_filename
        
        try:
            with pd.ExcelWriter(raw_path, engine='openpyxl') as writer:
                for sheet_name, df in raw_sheets.items():
                    df.to_excel(writer, sheet_name=sheet_name, index=False)
            
            sheet_list = ', '.join(raw_sheets.keys())
            print(f"     ✅ Raw data: {raw_filename}")
            print(f"        Sheets: {sheet_list}")
        except Exception as e:
            print(f"     ❌ Failed to save raw data: {e}")
    
    # Save aggregated data
    if agg_sheets:
        agg_filename = f"RQ3_aggregated_{spl_folder_name}.xlsx"
        agg_path = output_dir / agg_filename
        
        try:
            with pd.ExcelWriter(agg_path, engine='openpyxl') as writer:
                for sheet_name, df in agg_sheets.items():
                    df.to_excel(writer, sheet_name=sheet_name, index=False)
            
            sheet_list = ', '.join(agg_sheets.keys())
            print(f"     ✅ Aggregated: {agg_filename}")
            print(f"        Sheets: {sheet_list}")
        except Exception as e:
            print(f"     ❌ Failed to save aggregated data: {e}")
    
    # Print summary of warnings
    if all_warnings:
        print(f"\n  ⚠️  VALIDATION WARNINGS:")
        for w in all_warnings:
            print(f"     {w}")
    
    return True

def main():
    print("="*80)
    print("RQ3 FAULT DETECTION AGGREGATION - 4-SHEET VERSION")
    print("="*80)
    print("\nOutput format:")
    print("  Each SPL gets 2 Excel files (raw + aggregated)")
    print("  Each file has 4 sheets:")
    print("    • EdgeOmission (deterministic: ESG-Fx L0-4, EFG L2-4)")
    print("    • EventOmission (deterministic: ESG-Fx L0-4, EFG L2-4)")
    print("    • EdgeOmission_MultiSeed (Random Walk: seeds 42-51)")
    print("    • EventOmission_MultiSeed (Random Walk: seeds 42-51)")
    print()
    
    project_root = find_project_root()
    
    if project_root is None:
        print("❌ ERROR: Could not find project root!")
        print(f"Current directory: {Path.cwd()}")
        sys.exit(1)
    
    print(f"Project root: {project_root}")
    print(f"Cases directory: {project_root / 'files' / 'Cases'}")
    print()
    
    print("🔍 Discovering SPLs with fault detection data...")
    spls = discover_spls_with_fault_detection(project_root)
    
    if not spls:
        print("❌ No SPLs with fault detection data found!")
        sys.exit(1)
    
    print(f"✅ Found {len(spls)} SPLs with fault detection data:")
    for spl_name, _ in spls:
        file_prefix = SPL_NAME_MAPPING.get(spl_name, spl_name)
        if file_prefix != spl_name:
            print(f"   - {spl_name:25s} (files: {file_prefix}_*.csv)")
        else:
            print(f"   - {spl_name}")
    
    print("\n" + "="*80)
    print("PROCESSING SPLs")
    print("="*80)
    
    processed_count = 0
    failed_count = 0
    
    for spl_name, fd_dir in spls:
        try:
            success = aggregate_fault_detection(spl_name, fd_dir, fd_dir)
            if success:
                processed_count += 1
            else:
                failed_count += 1
        except Exception as e:
            print(f"\n❌ ERROR processing {spl_name}: {e}")
            import traceback
            traceback.print_exc()
            failed_count += 1
    
    print("\n" + "="*80)
    print("SUMMARY")
    print("="*80)
    print(f"✅ Successfully processed: {processed_count} SPLs")
    if failed_count > 0:
        print(f"❌ Failed: {failed_count} SPLs")
    print()
    print("Output format:")
    print("  RQ3_rawData_<SPL>.xlsx:")
    print("    - EdgeOmission (deterministic)")
    print("    - EventOmission (deterministic)")
    print("    - EdgeOmission_MultiSeed (Random Walk)")
    print("    - EventOmission_MultiSeed (Random Walk)")
    print()
    print("  RQ3_aggregated_<SPL>.xlsx:")
    print("    - Same 4 sheets with aggregated statistics")
    print("\n" + "="*80)

if __name__ == "__main__":
    main()