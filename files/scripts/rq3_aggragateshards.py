#!/usr/bin/env python3
"""
RQ3 Fault Detection Data Aggregation Script - RAW SHARD MERGER

Description:
Reads all shard CSV files from 'perProduct' and 'sensitivity' directories,
concatenates them, sorts them logically, and outputs a single comprehensive 
Excel file per SPL named 'RQ3_perProduct.xlsx'.

Each Excel file contains one sheet per fault detection category.
"""

import pandas as pd
import sys
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

def find_project_root():
    """Find project root by looking for 'files/Cases' directory."""
    current = Path.cwd()
    if (current / "files" / "Cases").exists(): return current
    if (current.parent / "files" / "Cases").exists(): return current.parent
    if (current.parent.parent / "files" / "Cases").exists(): return current.parent.parent
    
    # Fallback to hardcoded path provided by user if dynamic search fails
    hardcoded_path = Path("/Users/dilekozturk/git/esg-with-feature-expressions")
    if (hardcoded_path / "files" / "Cases").exists(): return hardcoded_path
    
    return None

def load_shards(directory, file_pattern):
    """
    Finds all shard files matching the pattern, reads them, and concatenates into a single DataFrame.
    """
    search_path = directory / file_pattern
    shard_files = sorted(glob.glob(str(search_path)))
    
    if not shard_files:
        return None
        
    dfs = []
    for f in shard_files:
        try:
            # Important: Use sep=';' and decimal=',' for European number format in CSVs
            df = pd.read_csv(f, sep=';', decimal=',')
            dfs.append(df)
        except Exception as e:
            print(f"        [!] Error reading {Path(f).name}: {e}")
            
    if not dfs:
        return None
        
    combined_df = pd.concat(dfs, ignore_index=True)
    
    # Sort logically to make the resulting Excel file human-readable
    sort_columns = []
    for col in ['ProductID', 'TestingApproach', 'Seed', 'DampingFactor']:
        if col in combined_df.columns:
            sort_columns.append(col)
            
    if sort_columns:
        combined_df = combined_df.sort_values(by=sort_columns).reset_index(drop=True)
        
    return combined_df

def process_spl(spl_folder_name, cases_dir):
    """Processes a single SPL and generates its combined Excel file."""
    spl_dir = cases_dir / spl_folder_name
    fd_dir = spl_dir / "faultdetection"
    
    per_product_dir = fd_dir / "perProduct"
    sensitivity_dir = fd_dir / "sensitivity"
    
    file_prefix = SPL_NAME_MAPPING.get(spl_folder_name, spl_folder_name)
    
    print(f"\n{'='*80}")
    print(f"📊 MERGING SHARDS FOR: {spl_folder_name}")
    print(f"{'='*80}")
    
    sheets_data = {}
    
    # 1. Process 'perProduct' categories (For ALL SPLs)
    if per_product_dir.exists():
        per_product_categories = [
            ("EdgeOmission", f"{file_prefix}_EdgeOmission_shard*.csv"),
            ("EventOmission", f"{file_prefix}_EventOmission_shard*.csv"),
            ("EdgeOmission_MultiSeed", f"{file_prefix}_EdgeOmission_MultiSeedRW_shard*.csv"),
            ("EventOmission_MultiSeed", f"{file_prefix}_EventOmission_MultiSeedRW_shard*.csv")
        ]
        
        for sheet_name, pattern in per_product_categories:
            df = load_shards(per_product_dir, pattern)
            if df is not None:
                sheets_data[sheet_name] = df
                print(f"  [✓] {sheet_name:25s}: {len(df)} rows combined.")
    else:
        print(f"  [!] Directory not found: {per_product_dir}")

    # 2. Process 'sensitivity' categories (For ALL SPLs)
    if sensitivity_dir.exists():
        sensitivity_categories = [
            ("Sens_EdgeOmission", f"{file_prefix}_DampingSensitivity_EdgeOmission_shard*.csv"),
            ("Sens_EventOmission", f"{file_prefix}_DampingSensitivity_EventOmission_shard*.csv"),
            ("Sens_TestGen", f"{file_prefix}_DampingSensitivity_TestGen_shard*.csv")
        ]
        
        print(f"  --- Sensitivity Data ---")
        for sheet_name, pattern in sensitivity_categories:
            df = load_shards(sensitivity_dir, pattern)
            if df is not None:
                sheets_data[sheet_name] = df
                print(f"  [✓] {sheet_name:25s}: {len(df)} rows combined.")
    else:
        print(f"  [!] Sensitivity directory missing for {spl_folder_name}, skipping.")
            
    # 3. Save to Excel
    if sheets_data:
        output_excel = spl_dir / "RQ3_perProduct.xlsx"
        
        try:
            with pd.ExcelWriter(output_excel, engine='openpyxl') as writer:
                for sheet_name, df in sheets_data.items():
                    df.to_excel(writer, sheet_name=sheet_name, index=False)
            
            print(f"\n  💾 SUCCESS: Saved -> {output_excel}")
        except Exception as e:
            print(f"\n  ❌ ERROR saving Excel: {e}")
    else:
        print(f"\n  ⚠️ NO DATA FOUND to merge for {spl_folder_name}.")

def main():
    print("="*80)
    print("RQ3 SHARD MERGER (RAW DATA COMPILATION)")
    print("="*80)
    
    project_root = find_project_root()
    if not project_root:
        print("❌ ERROR: Could not find project root (esg-with-feature-expressions).")
        sys.exit(1)
        
    cases_dir = project_root / "files" / "Cases"
    
    if not cases_dir.exists():
        print(f"❌ ERROR: Cases directory not found at {cases_dir}")
        sys.exit(1)
        
    spls = [d.name for d in cases_dir.iterdir() if d.is_dir() and d.name in SPL_NAME_MAPPING.keys()]
    
    if not spls:
        print("❌ No valid SPL directories found!")
        sys.exit(1)
        
    for spl_name in sorted(spls):
        process_spl(spl_name, cases_dir)
        
    print("\n" + "="*80)
    print("ALL DONE! You can now analyze the RQ3_perProduct.xlsx files.")
    print("="*80)

if __name__ == "__main__":
    main()