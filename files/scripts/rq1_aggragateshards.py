#!/usr/bin/env python3
"""
rq1_aggregate_shards.py — Aggregates shard-level CSV files for RQ1
====================================================================
UPDATED: 
- Raw Data: Keeps Shard ID and Run ID as separate columns.
- Aggregated Data: Groups across ALL shards by Run ID. Results in exactly 
  1 row per run (e.g., 11 rows total), eliminating Shard ID and summing up 
  Processed Products to represent the entire SPL.
- Master Summary: Calculates the median of the 11 runs for each SPL 
  and writes a single comprehensive Excel file under files/Cases/ with 
  1 row per SPL per approach.

Usage:
    python rq1_aggragateshards.py
"""

import os
import sys
import re
import pandas as pd
from pathlib import Path

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
    current = Path.cwd()
    if (current / "files" / "Cases").exists(): return current
    if (current.parent / "files" / "Cases").exists(): return current.parent
    if (current.parent.parent / "files" / "Cases").exists(): return current.parent.parent
    return None

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
    'weighted_avg': {
        'Edge Coverage(%)': 'Processed Products'
    },
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

APPROACH_RULES_MAP = {
    'EFG': EFG_RULES,
    'ESG-Fx': ESGFX_RULES,
    'RandomWalk': RW_RULES
}

def aggregate_entire_spl(raw_df):
    if raw_df.empty: return raw_df
    
    aggregated_rows = []
    
    for (approach, cov_type), group_df in raw_df.groupby(['Approach', 'Coverage Type']):
        rules = APPROACH_RULES_MAP.get(approach, {})
        if not rules: continue
        
        for run_id, run_group in group_df.groupby('Run ID'):
            row = {'Approach': approach, 'Coverage Type': cov_type, 'Run ID': run_id}
            
            for col in rules.get('key', []):
                if col in run_group.columns and col not in row:
                    row[col] = run_group[col].iloc[0]
                    
            for col in rules.get('sum', []):
                if col in run_group.columns:
                    row[col] = run_group[col].sum()
                    
            for col in rules.get('max', []):
                if col in run_group.columns:
                    row[col] = run_group[col].max()
                    
            for col, weight_col in rules.get('weighted_avg', {}).items():
                if col in run_group.columns and weight_col in run_group.columns:
                    weights = run_group[weight_col]
                    values = run_group[col]
                    total_weight = weights.sum()
                    row[col] = (values * weights).sum() / total_weight if total_weight > 0 else 0.0
                    
            aggregated_rows.append(row)
            
    result = pd.DataFrame(aggregated_rows)
    ordered_cols = [c for c in raw_df.columns if c in result.columns and c != 'Shard ID']
    return result[ordered_cols]

def get_spl_median_summary(agg_df, spl_name):
    """
    Takes the aggregated 11-run dataframe of a single SPL, 
    calculates the median for all numeric columns, 
    and returns a 1-row-per-approach summary dataframe.
    """
    if agg_df.empty: return pd.DataFrame()
    
    summaries = []
    # Kapsama ve Yaklaşıma göre grupla (örn: ESG-Fx ve EdgeCoverage)
    for (approach, cov_type), group in agg_df.groupby(['Approach', 'Coverage Type']):
        # Yalnızca matematiksel ortalaması alınabilecek sayısal sütunları seç
        numeric_cols = group.select_dtypes(include='number').columns
        cols_to_median = [c for c in numeric_cols if c != 'Run ID']
        
        # Orijinal tablo yapısını korumak için anahtar sütunları ayarla
        row = {
            'SPL Name': SPL_NAME_MAPPING.get(spl_name, spl_name), # Kısaltma varsa kullan
            'Approach': approach,
            'Coverage Type': cov_type
        }
        
        # Her bir sütun için median değerini hesapla
        for c in cols_to_median:
            row[c] = group[c].median()
            
        summaries.append(row)
        
    return pd.DataFrame(summaries)

def save_to_sheets(df, output_path):
    with pd.ExcelWriter(output_path, engine='openpyxl') as writer:
        if df.empty:
            df.to_excel(writer, sheet_name='No_Data', index=False)
            return
            
        for (approach, level), group_df in df.groupby(['Approach', 'Coverage Type']):
            sheet_name = f"{approach}_{level}"
            clean_group_df = group_df.dropna(axis=1, how='all')
            clean_group_df.to_excel(writer, sheet_name=sheet_name, index=False)

def process_spl(spl_folder_name, base_dir, output_dir):
    print(f"\n[SUMMARY] PROCESSING: {spl_folder_name}")
    
    pipeline_dir = base_dir / "comparativeEfficiencyTestPipeline"
    if not pipeline_dir.exists():
        print("  WARNING: comparativeEfficiencyTestPipeline directory not found.")
        return None

    all_raw_data = []
    
    for csv_file in pipeline_dir.rglob("*.csv"):
        fname = csv_file.name
        if '_EFG_' in fname: approach = 'EFG'
        elif '_ESG-Fx_' in fname: approach = 'ESG-Fx'
        elif '_RandomWalk_' in fname: approach = 'RandomWalk'
        else: continue

        shard_match = re.search(r'(shard_?\d+)', str(csv_file), re.IGNORECASE)
        shard_name = shard_match.group(1).lower() if shard_match else "shard0"

        try:
            df = pd.read_csv(csv_file, sep=';', decimal=',')
            if len(df.columns) == 1:
                df = pd.read_csv(csv_file, sep=',', decimal='.')
            
            rename_dict = {}
            for col in df.columns:
                if col.strip() in ['L2 Percent(%)', 'L3 Percent(%)', 'L4 Percent(%)']:
                    rename_dict[col] = 'Edge Coverage(%)'
            if rename_dict:
                df.rename(columns=rename_dict, inplace=True)
            
            df['Approach'] = approach
            df.insert(0, 'Shard ID', shard_name)
            
            if 'Run ID' in df.columns:
                cleaned_run_id = df['Run ID'].astype(str).str.replace(r'\.0$', '', regex=True)
                df['Run ID'] = pd.to_numeric(cleaned_run_id, errors='ignore')
            
            all_raw_data.append(df)
                
        except Exception as e:
            print(f"  ERROR: {csv_file.name} - {e}")

    if not all_raw_data: return None

    raw_combined = pd.concat(all_raw_data, ignore_index=True)
    agg_combined = aggregate_entire_spl(raw_combined)
    
    raw_path = output_dir / f"RQ1_Summary_rawData_{spl_folder_name}.xlsx"
    agg_path = output_dir / f"RQ1_Summary_aggregated_{spl_folder_name}.xlsx"
    
    save_to_sheets(raw_combined, raw_path)
    save_to_sheets(agg_combined, agg_path)
    
    print(f"  DONE: Generated summary excels for {spl_folder_name}.")
    
    # Döngüde master dosyaya eklenmek üzere aggregated veriyi döndürüyoruz
    return agg_combined

def main():
    root = find_project_root()
    if not root: return
    cases_dir = root / "files" / "Cases"
    spls = [d for d in sorted(cases_dir.iterdir()) if d.is_dir()]
    
    all_master_summaries = []
    
    for spl_dir in spls:
        # 1. Her SPL'i işle ve o SPL'e ait 11 run'lık veriyi al
        agg_df = process_spl(spl_dir.name, spl_dir, spl_dir)
        
        # 2. Eğer veri başarıyla geldiyse medyan alıp ana listeye ekle
        if agg_df is not None and not agg_df.empty:
            spl_median_summary = get_spl_median_summary(agg_df, spl_dir.name)
            all_master_summaries.append(spl_median_summary)
            
    # 3. Tüm SPL'ler bittiğinde tek bir master (ana) Excel dosyası oluştur
    if all_master_summaries:
        master_combined = pd.concat(all_master_summaries, ignore_index=True)
        master_path = cases_dir / "Master_RQ1_Summary.xlsx"
        
        save_to_sheets(master_combined, master_path)
        print(f"\n=======================================================")
        print(f"[MASTER SUMMARY] Bütün SPL verileri medyanlanıp birleştirildi.")
        print(f"Dosya şuraya kaydedildi: {master_path}")
        print(f"=======================================================")

if __name__ == "__main__":
    main()