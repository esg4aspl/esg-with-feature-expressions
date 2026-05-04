#!/usr/bin/env python3
"""
rq2_02_median_across_runs.py  (Step 2 of 3)

Input : files/Cases/<SPL>/RQ2_perShard_<SPL>.xlsx     (from Step 1)
Output: files/Cases/<SPL>/RQ2_summary_<SPL>.xlsx

For each shard, reduces the 11 (or 3) runs to a single row by taking
the median of every numeric column. Non-numeric fields (Approach,
SPLName, CoverageType) are carried through via .first() since they
don't vary across runs of the same shard.

Why median (not mean)? JVM time measurements are right-skewed due to
warm-up and GC effects; median is robust to those outliers and matches
the analysis plan documented in the experiment-critique skill.

T_pipeline(ms) is treated like any other numeric column -- per-shard
median of T_pipeline is the right "typical CPU cost" of that shard,
ready for Step 3 to aggregate across the 80 shards into a serial
total or wall-clock max.

Adds an N_Runs column so downstream steps can detect incomplete
shards. Writes a warnings.txt alongside the output if any shard has
fewer than the expected number of runs.
"""

import pandas as pd
import sys
from pathlib import Path


def find_project_root():
    """Pure relative discovery -- no hardcoded user paths."""
    candidates = [Path.cwd()]
    candidates.extend(Path.cwd().parents)
    candidates.append(Path(__file__).resolve().parent)
    candidates.extend(Path(__file__).resolve().parents)
    seen = set()
    for c in candidates:
        c = c.resolve()
        if c in seen:
            continue
        seen.add(c)
        if (c / "files" / "Cases").exists():
            return c
    return None


def summarize_file(filepath):
    spl_name = filepath.stem.replace("RQ2_perShard_", "")
    spl_dir = filepath.parent
    xls = pd.ExcelFile(filepath)

    print(f"\n{'='*70}")
    print(f"  SPL: {spl_name}  |  Sheets: {len(xls.sheet_names)}")
    print(f"{'='*70}")

    summary_sheets = {}
    log_lines = []

    for sheet_name in xls.sheet_names:
        df = pd.read_excel(xls, sheet_name=sheet_name)

        run_col = ('RunID' if 'RunID' in df.columns
                   else ('Run ID' if 'Run ID' in df.columns else None))
        if run_col is None or 'Shard' not in df.columns:
            print(f"  [!] Skipping {sheet_name}: missing RunID or Shard")
            continue

        all_runs = sorted(df[run_col].unique())
        all_shards = sorted(df['Shard'].unique())
        expected_runs = len(all_runs)

        print(f"\n  --- {sheet_name} ---")
        print(f"  Shards: {len(all_shards)}  Runs seen: {list(all_runs)}  "
              f"Total rows: {len(df)}")

        incomplete = []
        for shard in all_shards:
            shard_runs = sorted(df[df['Shard'] == shard][run_col].unique())
            if len(shard_runs) < expected_runs:
                incomplete.append((shard, shard_runs))
                msg = (f"Shard {shard}: only {len(shard_runs)}/"
                       f"{expected_runs} runs -> {list(shard_runs)}")
                print(f"  WARN {msg}")
                log_lines.append(f"[{sheet_name}] {msg}")

        if not incomplete:
            print(f"  OK: all shards have {expected_runs} runs")

        # Reduce: median of numeric, first of constant string columns.
        exclude = {run_col, 'Shard'}
        numeric_cols = [c for c in df.columns
                        if c not in exclude and pd.api.types.is_numeric_dtype(df[c])]
        constant_cols = [c for c in df.columns
                         if c not in exclude and not pd.api.types.is_numeric_dtype(df[c])]

        grouped = df.groupby('Shard')[numeric_cols].median()
        for col in constant_cols:
            grouped[col] = df.groupby('Shard')[col].first()
        grouped['N_Runs'] = df.groupby('Shard')[run_col].count()

        # Preserve Step 1's column order (skip excluded), prepend N_Runs.
        original_order = [c for c in df.columns if c not in exclude]
        final_cols = ['N_Runs'] + original_order
        seen, dedup = set(), []
        for c in final_cols:
            if c not in seen and c in grouped.columns:
                dedup.append(c)
                seen.add(c)

        grouped = (grouped[dedup]
                   .reset_index()
                   .sort_values('Shard')
                   .reset_index(drop=True))
        summary_sheets[sheet_name] = grouped
        print(f"  -> Summary: {len(grouped)} rows, {len(grouped.columns)} columns")

    output_path = spl_dir / f"RQ2_summary_{spl_name}.xlsx"
    with pd.ExcelWriter(output_path, engine='openpyxl') as writer:
        for sheet_name, df_summary in summary_sheets.items():
            df_summary.to_excel(writer, sheet_name=sheet_name, index=False)

    if log_lines:
        log_path = spl_dir / f"RQ2_summary_{spl_name}_warnings.txt"
        with open(log_path, 'w') as f:
            f.write(f"Incomplete run warnings for {spl_name}\n{'='*50}\n")
            for line in log_lines:
                f.write(line + '\n')
        print(f"\n  WARNINGS saved: {log_path.name}")

    print(f"  SAVED: {output_path.name}")
    return output_path


def main():
    print("="*80)
    print("RQ2 STEP 2 / 3 - PER-SHARD MEDIAN ACROSS RUNS")
    print("="*80)

    project_root = find_project_root()
    if not project_root:
        print("ERROR: Could not find project root containing files/Cases/.")
        sys.exit(1)

    cases_dir = project_root / "files" / "Cases"
    print(f"Project root: {project_root}")
    print(f"Cases dir   : {cases_dir}")

    files = sorted(cases_dir.rglob("RQ2_perShard_*.xlsx"))
    if not files:
        print("ERROR: No RQ2_perShard_*.xlsx files found! Run Step 1 first.")
        sys.exit(1)

    print(f"Found {len(files)} file(s) to process")
    outputs = []
    for f in files:
        outputs.append(summarize_file(f))

    print(f"\n{'='*70}")
    print(f"DONE. {len(outputs)} summary file(s) created.")
    print("Next: run rq2_03_aggregate_across_shards.py")
    print(f"{'='*70}")


if __name__ == "__main__":
    main()