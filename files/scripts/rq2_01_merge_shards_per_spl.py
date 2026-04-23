#!/usr/bin/env python3
"""
rq2_01_merge_shards_per_spl.py  (Step 1 of 3)

Input : files/Cases/<SPL>/extremeScalabilityTestPipeline/<Approach>/<Level>/<spl>_<approach>_<level>_shardNN.csv
Output: files/Cases/<SPL>/RQ2_perShard_<SPL>.xlsx  (one sheet per Approach_Level)

Reads European-formatted CSVs (sep=';', decimal=',') produced by the RQ2
extreme-scalability pipeline, tags each row with its Shard number, and
concatenates all shards of each Approach_Level into a single Excel sheet.

Expected shape per sheet:  80 shards x 11 runs = 880 rows.
Incomplete sheets are reported but not silently dropped.
"""

import pandas as pd
import sys
import re
from pathlib import Path

EXPECTED_SHARDS = 80

# Per-SPL expected run count. Small/medium SPLs use 11 repetitions for
# robust median estimation; large-scale SPLs use 3 repetitions because
# RQ2 measures throughput (not per-product variance) and full enumeration
# is already computationally expensive. This matches the study design
# documented in the experiment-critique skill.
LARGE_SCALE_SPLS = {'HockertyShirts', 'syngovia', 'Tesla'}
EXPECTED_RUNS_LARGE = 3
EXPECTED_RUNS_DEFAULT = 11


def expected_runs_for(spl_name):
    return EXPECTED_RUNS_LARGE if spl_name in LARGE_SCALE_SPLS else EXPECTED_RUNS_DEFAULT


def find_project_root():
    current = Path.cwd()
    for candidate in [current, current.parent, current.parent.parent]:
        if (candidate / "files" / "Cases").exists():
            return candidate
    hardcoded = Path("/Users/dilekozturk/git/esg-with-feature-expressions")
    if (hardcoded / "files" / "Cases").exists():
        return hardcoded
    return None


def process_spl(spl_dir):
    pipeline_dir = spl_dir / "extremeScalabilityTestPipeline"
    if not pipeline_dir.exists():
        return False

    print(f"\n{'='*80}")
    print(f"[Step 1] Merging shard CSVs for: {spl_dir.name}")
    print(f"{'='*80}")

    sheets_data = {}
    shard_files = list(pipeline_dir.rglob("*.csv"))
    if not shard_files:
        print(f"  [!] No CSV files found in {pipeline_dir.name}")
        return False

    for f in shard_files:
        try:
            match = re.search(r'shard(\d+)', f.stem, re.IGNORECASE)
            shard_no = match.group(1) if match else "Unknown"
            level = f.parent.name
            approach = f.parent.parent.name
            sheet_name = f"{approach}_{level}"

            df = pd.read_csv(f, sep=';', decimal=',')
            df['Shard'] = shard_no

            sheets_data.setdefault(sheet_name, []).append(df)
        except Exception as e:
            print(f"  [!] Error reading {f.name}: {e}")

    if not sheets_data:
        return False

    output_excel = spl_dir / f"RQ2_perShard_{spl_dir.name}.xlsx"
    try:
        with pd.ExcelWriter(output_excel, engine='openpyxl') as writer:
            for sheet_name, df_list in sheets_data.items():
                combined_df = pd.concat(df_list, ignore_index=True)

                if 'Shard' in combined_df.columns:
                    combined_df['Shard_Num'] = pd.to_numeric(combined_df['Shard'], errors='coerce')
                    sort_cols = ['Shard_Num']
                    if 'RunID' in combined_df.columns:
                        sort_cols.append('RunID')
                    elif 'Run ID' in combined_df.columns:
                        sort_cols.append('Run ID')

                    combined_df = (combined_df
                                   .sort_values(by=sort_cols)
                                   .drop(columns=['Shard_Num'])
                                   .reset_index(drop=True))

                    # Place Shard right after RunID for easier tracking
                    cols = combined_df.columns.tolist()
                    cols.remove('Shard')
                    insert_pos = 1
                    if 'RunID' in cols:
                        insert_pos = cols.index('RunID') + 1
                    elif 'Run ID' in cols:
                        insert_pos = cols.index('Run ID') + 1
                    cols.insert(insert_pos, 'Shard')
                    combined_df = combined_df[cols]

                # --- Sanity check vs expected shards x runs --------------
                run_col = 'RunID' if 'RunID' in combined_df.columns else 'Run ID'
                expected_runs = expected_runs_for(spl_dir.name)
                expected_rows = EXPECTED_SHARDS * expected_runs
                n_shards = combined_df['Shard'].nunique()
                n_rows = len(combined_df)

                flag = "" if n_rows == expected_rows else f"  <-- expected {expected_rows} ({EXPECTED_SHARDS}x{expected_runs})"
                print(f"  [OK] {sheet_name:15s} shards={n_shards:3d}  rows={n_rows:4d}{flag}")

                if n_shards != EXPECTED_SHARDS:
                    print(f"       WARNING: {EXPECTED_SHARDS - n_shards} shard(s) missing in {sheet_name}")

                # Also flag shards whose run count differs from expected
                # (scale of the deviation helps: a few missing runs vs a
                # pattern of identical under-counts across SPLs).
                if run_col in combined_df.columns:
                    runs_per_shard = combined_df.groupby('Shard')[run_col].count()
                    short_shards = runs_per_shard[runs_per_shard < expected_runs]
                    if len(short_shards) > 0:
                        total_missing = int((expected_runs - short_shards).sum())
                        print(f"       NOTE: {len(short_shards)} shard(s) short on runs "
                              f"(total {total_missing} run(s) missing, expected {expected_runs}/shard)")

                combined_df.to_excel(writer, sheet_name=sheet_name, index=False)

        print(f"\n  SAVED -> {output_excel.name}")
        return True
    except Exception as e:
        print(f"\n  ERROR saving Excel: {e}")
        return False


def main():
    print("="*80)
    print("RQ2 STEP 1 / 3 - MERGE SHARD CSVS")
    print("="*80)

    project_root = find_project_root()
    if not project_root:
        print("ERROR: Could not find project root (files/Cases/).")
        sys.exit(1)

    cases_dir = project_root / "files" / "Cases"
    if not cases_dir.exists():
        print(f"ERROR: Cases directory not found at {cases_dir}")
        sys.exit(1)

    spls = [d for d in cases_dir.iterdir() if d.is_dir()]
    processed_count = 0
    for spl_dir in sorted(spls):
        if process_spl(spl_dir):
            processed_count += 1

    print("\n" + "="*80)
    if processed_count > 0:
        print(f"DONE. RQ2_perShard_*.xlsx generated for {processed_count} SPLs.")
        print("Next: run rq2_02_median_across_runs.py")
    else:
        print("No RQ2 extreme-scalability data found to merge.")
    print("="*80)


if __name__ == "__main__":
    main()