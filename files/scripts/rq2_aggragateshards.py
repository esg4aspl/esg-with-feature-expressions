#!/usr/bin/env python3
"""
rq2_aggragateshards.py

Description:
Reads all shard CSV files from the 'extremeScalabilityTestPipeline' directory 
for each SPL, concatenates them by Approach and Level, adds a 'Shard' column, 
and outputs a single comprehensive Excel file per SPL named 'RQ2_perShard_<SPLName>.xlsx'.
"""

import pandas as pd
import sys
import re
from pathlib import Path

def find_project_root():
    current = Path.cwd()
    if (current / "files" / "Cases").exists(): return current
    if (current.parent / "files" / "Cases").exists(): return current.parent
    if (current.parent.parent / "files" / "Cases").exists(): return current.parent.parent
    
    # Fallback to hardcoded path
    hardcoded_path = Path("/Users/dilekozturk/git/esg-with-feature-expressions")
    if (hardcoded_path / "files" / "Cases").exists(): return hardcoded_path
    
    return None

def process_spl(spl_dir):
    pipeline_dir = spl_dir / "extremeScalabilityTestPipeline"
    
    if not pipeline_dir.exists():
        return False
        
    print(f"\n{'='*80}")
    print(f"📊 AGGREGATING RQ2 SHARDS FOR: {spl_dir.name}")
    print(f"{'='*80}")

    sheets_data = {}
    shard_files = list(pipeline_dir.rglob("*.csv"))

    if not shard_files:
        print(f"  [!] No CSV files found in {pipeline_dir.name}")
        return False

    for f in shard_files:
        try:
            # Extract shard number using regex (e.g., from 'Svia_EFG_L4_shard79.csv' -> '79')
            match = re.search(r'shard(\d+)', f.stem, re.IGNORECASE)
            shard_no = match.group(1) if match else "Unknown"
            
            # Deduce Approach and Level from directory structure
            # Structure: extremeScalabilityTestPipeline / Approach / Level / file.csv
            level = f.parent.name
            approach = f.parent.parent.name
            
            sheet_name = f"{approach}_{level}"
            
            # Read CSV (handling European decimals and semicolons)
            df = pd.read_csv(f, sep=';', decimal=',')
            
            # Add Shard column
            df['Shard'] = shard_no
            
            if sheet_name not in sheets_data:
                sheets_data[sheet_name] = []
                
            sheets_data[sheet_name].append(df)
            
        except Exception as e:
            print(f"  [!] Error reading {f.name}: {e}")

    if not sheets_data:
        return False

    output_excel = spl_dir / f"RQ2_perShard_{spl_dir.name}.xlsx"

    try:
        with pd.ExcelWriter(output_excel, engine='openpyxl') as writer:
            for sheet_name, df_list in sheets_data.items():
                combined_df = pd.concat(df_list, ignore_index=True)
                
                # Convert Shard to numeric for proper sorting (e.g., 2 comes before 10)
                if 'Shard' in combined_df.columns:
                    combined_df['Shard_Num'] = pd.to_numeric(combined_df['Shard'], errors='coerce')
                    
                    # Sort primarily by Shard_Num, then by RunID if exists
                    sort_cols = ['Shard_Num']
                    if 'RunID' in combined_df.columns:
                        sort_cols.append('RunID')
                    elif 'Run ID' in combined_df.columns:
                        sort_cols.append('Run ID')
                        
                    combined_df = combined_df.sort_values(by=sort_cols).drop(columns=['Shard_Num']).reset_index(drop=True)
                    
                    # Rearrange columns to put 'Shard' right after 'RunID' for easy tracking
                    cols = combined_df.columns.tolist()
                    cols.remove('Shard')
                    
                    insert_pos = 1
                    if 'RunID' in cols:
                        insert_pos = cols.index('RunID') + 1
                    elif 'Run ID' in cols:
                        insert_pos = cols.index('Run ID') + 1
                        
                    cols.insert(insert_pos, 'Shard')
                    combined_df = combined_df[cols]
                
                combined_df.to_excel(writer, sheet_name=sheet_name, index=False)
                print(f"  [✓] Sheet '{sheet_name:15s}' -> {len(combined_df)} rows combined.")
                
        print(f"\n  💾 SUCCESS: Saved -> {output_excel.name}")
        return True
        
    except Exception as e:
        print(f"\n  ❌ ERROR saving Excel: {e}")
        return False

def main():
    print("="*80)
    print("RQ2 SHARD MERGER (EXTREME SCALABILITY PIPELINE)")
    print("="*80)
    
    project_root = find_project_root()
    if not project_root:
        print("❌ ERROR: Could not find project root.")
        sys.exit(1)
        
    cases_dir = project_root / "files" / "Cases"
    if not cases_dir.exists():
        print(f"❌ ERROR: Cases directory not found at {cases_dir}")
        sys.exit(1)
        
    spls = [d for d in cases_dir.iterdir() if d.is_dir()]
    processed_count = 0
    
    for spl_dir in sorted(spls):
        if process_spl(spl_dir):
            processed_count += 1
            
    print("\n" + "="*80)
    if processed_count > 0:
        print(f"🎉 ALL DONE! Successfully generated RQ2 Excel files for {processed_count} SPLs.")
    else:
        print("⚠️ No RQ2 extreme scalability data found to merge.")
    print("="*80)

if __name__ == "__main__":
    main()