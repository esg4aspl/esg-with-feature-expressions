#!/usr/bin/env python3
"""
RQ3_Aggregate_EdgeOmission_MultiSheet.py

Description: 
Reads RQ3_perProduct.xlsx for all SPLs, extracts EdgeOmission, 
EdgeOmission_MultiSeed, Sens_EdgeOmission, and Sens_TestGen.
Aggregates them using medians, and outputs them into 4 SEPARATE SHEETS 
in a single summary Excel file.
"""

import pandas as pd
import sys
from pathlib import Path

def find_project_root():
    current = Path.cwd()
    if (current / "files" / "Cases").exists(): return current
    if (current.parent / "files" / "Cases").exists(): return current.parent
    if (current.parent.parent / "files" / "Cases").exists(): return current.parent.parent
    
    hardcoded_path = Path("/Users/dilekozturk/git/esg-with-feature-expressions")
    if (hardcoded_path / "files" / "Cases").exists(): return hardcoded_path
    return None

def main():
    print("="*80)
    print("RQ3 EDGE OMISSION SUMMARY AGGREGATOR (MULTI-SHEET)")
    print("="*80)
    
    project_root = find_project_root()
    if not project_root:
        print("❌ ERROR: Could not find project root.")
        sys.exit(1)
        
    cases_dir = project_root / "files" / "Cases"
    
    # Dictionaries to hold aggregated dataframes for each category
    summary_data = {
        'EdgeOmission': [],
        'EdgeOmission_MultiSeed': [],
        'Sens_EdgeOmission': [],
        'Sens_TestGen': []
    }
    
    # Iterate through all SPL directories
    for spl_dir in sorted(cases_dir.iterdir()):
        if not spl_dir.is_dir(): continue
        
        target_file = spl_dir / "RQ3_perProduct.xlsx"
        if not target_file.exists(): continue
            
        print(f"Reading: {spl_dir.name}")
        
        try:
            xl = pd.ExcelFile(target_file, engine='openpyxl')
            sheet_names = xl.sheet_names
        except Exception as e:
            print(f"  ❌ Error reading {target_file.name}: {e}")
            continue

        # 1. Process Deterministic EdgeOmission
        if 'EdgeOmission' in sheet_names:
            df = xl.parse('EdgeOmission')
            if not df.empty:
                agg = df.groupby(['SPL', 'TestingApproach']).agg({
                    'TotalMutants': 'median',
                    'DetectedMutants': 'median',
                    'MutationScore(%)': 'median',
                    'TotalEventsInSuite': 'median',
                    'EventsToDetect': 'median',
                    'PercentageOfSuiteToDetect(%)': 'median'
                }).reset_index()
                summary_data['EdgeOmission'].append(agg)
                print("  [✓] EdgeOmission")

        # 2. Process MultiSeed Random Walk
        if 'EdgeOmission_MultiSeed' in sheet_names:
            df = xl.parse('EdgeOmission_MultiSeed')
            if not df.empty:
                df = df.rename(columns={
                    'MedianEventsToDetect': 'EventsToDetect',
                    'MedianPercentageOfSuiteToDetect(%)': 'PercentageOfSuiteToDetect(%)'
                })
                df['TestingApproach'] = 'RandomWalk_MultiSeed'
                
                agg = df.groupby(['SPL', 'TestingApproach']).agg({
                    'TotalMutants': 'median',
                    'DetectedMutants': 'median',
                    'MutationScore(%)': 'median',
                    'TotalEventsInSuite': 'median',
                    'EventsToDetect': 'median',
                    'PercentageOfSuiteToDetect(%)': 'median'
                }).reset_index()
                summary_data['EdgeOmission_MultiSeed'].append(agg)
                print("  [✓] EdgeOmission_MultiSeed")

        # 3. Process Sensitivity EdgeOmission
        if 'Sens_EdgeOmission' in sheet_names:
            df = xl.parse('Sens_EdgeOmission')
            if not df.empty:
                df = df.rename(columns={
                    'MedianEventsToDetect': 'EventsToDetect',
                    'MedianPercentageOfSuiteToDetect(%)': 'PercentageOfSuiteToDetect(%)'
                })
                df['TestingApproach'] = 'RandomWalk_Damping_' + df['DampingFactor'].astype(str)
                
                agg = df.groupby(['SPL', 'TestingApproach']).agg({
                    'TotalMutants': 'median',
                    'DetectedMutants': 'median',
                    'MutationScore(%)': 'median',
                    'TotalEventsInSuite': 'median',
                    'EventsToDetect': 'median',
                    'PercentageOfSuiteToDetect(%)': 'median'
                }).reset_index()
                summary_data['Sens_EdgeOmission'].append(agg)
                print("  [✓] Sens_EdgeOmission")

        # 4. Process Sensitivity TestGen (Steps, Coverage, Events)
        if 'Sens_TestGen' in sheet_names:
            df = xl.parse('Sens_TestGen')
            if not df.empty:
                df['TestingApproach'] = 'RandomWalk_Damping_' + df['DampingFactor'].astype(str)
                
                agg = df.groupby(['SPL', 'TestingApproach']).agg({
                    'NumTestCases': 'median',
                    'NumTestEvents': 'median',
                    'AchievedEdgeCoverage(%)': 'median',
                    'StepsTaken': 'median'
                }).reset_index()
                summary_data['Sens_TestGen'].append(agg)
                print("  [✓] Sens_TestGen")

    # Combine and save
    output_excel = cases_dir / "RQ3_Summary_EdgeOmission.xlsx"
    
    try:
        with pd.ExcelWriter(output_excel, engine='openpyxl') as writer:
            for sheet_name, df_list in summary_data.items():
                if df_list:
                    final_df = pd.concat(df_list, ignore_index=True)
                    final_df = final_df.sort_values(by=['SPL', 'TestingApproach']).reset_index(drop=True)
                    final_df = final_df.round(2)
                    final_df.to_excel(writer, index=False, sheet_name=sheet_name)
                    print(f"\n✅ Sheet created: {sheet_name} ({len(final_df)} rows)")
                else:
                    print(f"\n⚠️ No data for sheet: {sheet_name}")
                    
        print("\n" + "="*80)
        print(f"🎉 SUCCESS: Summary Excel generated at:\n{output_excel}")
        print("="*80)
    except Exception as e:
        print(f"\n❌ ERROR saving Excel: {e}")

if __name__ == "__main__":
    main()