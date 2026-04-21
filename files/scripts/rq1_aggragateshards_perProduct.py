#!/usr/bin/env python3
"""
rq1_aggregate_shards_perProduct.py — Aggregates PerProduct shard CSVs for RQ1
===============================================================================
Reads PerProductLog CSV files from each SPL, combines them into one Excel per SPL
with one sheet per approach×level. No aggregation here — just collection.

Output per SPL:
  - RQ1_{CaseName}_perProduct.xlsx

Usage:
    python rq1_aggregate_shards_perProduct.py
"""

import pandas as pd
from pathlib import Path

def find_project_root():
    current = Path.cwd()
    for p in [current, current.parent, current.parent.parent]:
        if (p / "files" / "Cases").exists():
            return p
    return None


def read_per_product_csv(filepath, approach, level):
    try:
        df = pd.read_csv(filepath, sep=';', decimal=',')
        if len(df.columns) == 1:
            df = pd.read_csv(filepath, sep=',', decimal='.')
        df.columns = [c.strip() if not c.startswith(' ') else c for c in df.columns]
        df['Approach'] = approach
        df['Coverage Type'] = level  # Standardized: always "Coverage Type"
        return df
    except Exception as e:
        print(f"  ERROR: {filepath.name}: {e}")
        return None


def process_per_product(spl_dir):
    print(f"\n[PER-PRODUCT] {spl_dir.name}")

    testseq_dir = spl_dir / "testsequences"
    efg_dir = spl_dir / "EFGs" / "efg_results"

    all_dfs = []

    if testseq_dir.exists():
        for csv_file in testseq_dir.rglob("*PerProductLog*.csv"):
            level = csv_file.parent.name
            approach = "RandomWalk" if "RandomWalk" in csv_file.name else "ESG-Fx"
            df = read_per_product_csv(csv_file, approach, level)
            if df is not None:
                all_dfs.append(df)

    if efg_dir.exists():
        for csv_file in efg_dir.rglob("*PerProductLog*.csv"):
            level = csv_file.parent.name
            df = read_per_product_csv(csv_file, "EFG", level)
            if df is not None:
                all_dfs.append(df)

    if not all_dfs:
        print("  WARNING: No PerProduct CSVs found.")
        return

    final_df = pd.concat(all_dfs, ignore_index=True)

    output_path = spl_dir / f"RQ1_{spl_dir.name}_perProduct.xlsx"

    with pd.ExcelWriter(output_path, engine='openpyxl') as writer:
        for (approach, level), group_df in final_df.groupby(['Approach', 'Coverage Type']):
            sheet_name = f"{approach}_{level}"[:31]
            clean = group_df.dropna(axis=1, how='all')
            clean.to_excel(writer, sheet_name=sheet_name, index=False)

    print(f"  DONE: {output_path.name}")


def main():
    root = find_project_root()
    if not root:
        print("ERROR: Project root not found.")
        return

    cases_dir = root / "files" / "Cases"
    spls = [d for d in sorted(cases_dir.iterdir()) if d.is_dir() and not d.name.startswith('_')]

    for spl_dir in spls:
        process_per_product(spl_dir)


if __name__ == "__main__":
    main()