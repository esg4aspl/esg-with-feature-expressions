#!/usr/bin/env python3
"""
rq1_aggregate_shards.py — Aggregates shard-level CSV files for RQ1
====================================================================
FIXED VERSION: Handles SPL name mismatches between folder names and file prefixes.

Usage:
    python rq1_aggregateshards_perProduct.py
"""



import os
import sys
import pandas as pd
from pathlib import Path

def find_project_root():
    current = Path.cwd()
    if (current / "files" / "Cases").exists(): return current
    if (current.parent / "files" / "Cases").exists(): return current.parent
    if (current.parent.parent / "files" / "Cases").exists(): return current.parent.parent
    return None

def process_per_product(spl_dir):
    print(f"\n[PER-PRODUCT] PROCESSING: {spl_dir.name}")
    
    testseq_dir = spl_dir / "testsequences"
    efg_dir = spl_dir / "EFGs" / "efg_results"
    
    all_dfs = []

    if testseq_dir.exists():
        for csv_file in testseq_dir.rglob("*PerProductLog*.csv"):
            level = csv_file.parent.name
            approach = "RandomWalk" if "RandomWalk" in csv_file.name else "ESG-Fx"
            df = read_per_product_csv(csv_file, approach, level)
            if df is not None: all_dfs.append(df)

    if efg_dir.exists():
        for csv_file in efg_dir.rglob("*PerProductLog*.csv"):
            level = csv_file.parent.name
            df = read_per_product_csv(csv_file, "EFG", level)
            if df is not None: all_dfs.append(df)

    if not all_dfs:
        print("  WARNING: No PerProduct CSVs found.")
        return
    
    # Hepsini birleştir (Sütun isimlerini hizalamak ve ortaklaştırmak için)
    final_df = pd.concat(all_dfs, ignore_index=True)
    
    # İsimlendirme standardizasyonu (L2, L3, L4 Percent -> Edge Coverage)
    rename_dict = {}
    for col in final_df.columns:
        if col.strip() in ['L2 Percent(%)', 'L3 Percent(%)', 'L4 Percent(%)']:
            rename_dict[col] = 'Edge Coverage(%)'
    if rename_dict:
        final_df.rename(columns=rename_dict, inplace=True)
        
    output_path = spl_dir / f"RQ1_PerProduct_rawData_{spl_dir.name}.xlsx"
    
    with pd.ExcelWriter(output_path, engine='openpyxl') as writer:
        for (approach, level), group_df in final_df.groupby(['Approach', 'Coverage Level']):
            sheet_name = f"{approach}_{level}"
            
            # KRİTİK NOKTA: Bu yaklaşım/seviye için tamamen boş (NaN) olan sütunları at.
            # Böylece EFG sekmesinde ESG-Fx sütunları, ESG-Fx sekmesinde EFG sütunları gözükmez!
            clean_group_df = group_df.dropna(axis=1, how='all')
            
            clean_group_df.to_excel(writer, sheet_name=sheet_name, index=False)
            
    print(f"  DONE: {output_path.name} created with clean, approach-specific sheets.")

def read_per_product_csv(filepath, approach, level):
    try:
        df = pd.read_csv(filepath, sep=';', decimal=',')
        df['Approach'] = approach
        df['Coverage Level'] = level
        return df
    except Exception as e:
        print(f"  ERROR: Could not read {filepath.name}: {e}")
        return None

def main():
    root = find_project_root()
    if not root:
        print("ERROR: Project root (files/Cases) not found.")
        return
    
    cases_dir = root / "files" / "Cases"
    spls = [d for d in sorted(cases_dir.iterdir()) if d.is_dir()]
    
    for spl_dir in spls:
        process_per_product(spl_dir)

if __name__ == "__main__":
    main()