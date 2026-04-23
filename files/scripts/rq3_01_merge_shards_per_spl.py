#!/usr/bin/env python3
"""
rq3_01_merge_shards_per_spl.py

Step 1 of the RQ3 analysis pipeline.

Reads all shard CSV files from each SPL's faultdetection/{perProduct,sensitivity}
directories, concatenates them, and writes a single Excel file per SPL:

    files/Cases/<SPL>/RQ3_<SPL>_perProduct_rawData.xlsx

Each Excel contains up to seven sheets:
    EdgeOmission             (deterministic: 1 row per product per approach)
    EventOmission
    EdgeOmission_MultiSeed   (multi-seed RW: 1 row per product per seed)
    EventOmission_MultiSeed
    Sens_EdgeOmission        (damping sensitivity: 1 row per product per factor)
    Sens_EventOmission
    Sens_TestGen             (test generation only: 1 row per product per factor)

Downstream scripts (rq3_02..rq3_06) expect this exact filename and sheet layout.

Usage (from any directory):
    python rq3_01_merge_shards_per_spl.py
"""

import pandas as pd
import sys
import glob
from pathlib import Path

# SPL name mapping: folder_name -> file_prefix used in shard CSVs
SPL_NAME_MAPPING = {
    "SodaVendingMachine": "SVM",
    "eMail": "eM",
    "Elevator": "El",
    "BankAccountv2": "BAv2",
    "StudentAttendanceSystem": "SAS",
    "Tesla": "Te",
    "syngovia": "Svia",
    "HockertyShirts": "HS",
}


def find_project_root():
    """Find project root by looking for 'files/Cases' directory."""
    current = Path.cwd()
    for probe in (current, current.parent, current.parent.parent):
        if (probe / "files" / "Cases").exists():
            return probe
    hardcoded = Path("/Users/dilekozturk/git/esg-with-feature-expressions")
    if (hardcoded / "files" / "Cases").exists():
        return hardcoded
    return None


def load_shards(directory, file_pattern):
    """Concatenate all shard CSVs matching a pattern into a single DataFrame."""
    search_path = directory / file_pattern
    shard_files = sorted(glob.glob(str(search_path)))

    if not shard_files:
        return None

    dfs = []
    for f in shard_files:
        try:
            df = pd.read_csv(f, sep=';', decimal=',')
            dfs.append(df)
        except Exception as e:
            print(f"      [!] Error reading {Path(f).name}: {e}")

    if not dfs:
        return None

    combined_df = pd.concat(dfs, ignore_index=True)

    # Drop rows with no product (empty-shard TestGen files)
    if "ProductID" in combined_df.columns:
        before = len(combined_df)
        combined_df = combined_df.dropna(subset=["ProductID"]).reset_index(drop=True)
        dropped = before - len(combined_df)
        if dropped > 0:
            print(f"      [.] dropped {dropped} rows with empty ProductID")

    # Sort logically for human-readable Excel output
    sort_columns = [c for c in ("ProductID", "TestingApproach", "Seed", "DampingFactor")
                    if c in combined_df.columns]
    if sort_columns:
        combined_df = combined_df.sort_values(by=sort_columns).reset_index(drop=True)

    return combined_df


def process_spl(spl_folder_name, cases_dir):
    """Process a single SPL: merge all shards and save Excel."""
    spl_dir = cases_dir / spl_folder_name
    fd_dir = spl_dir / "faultdetection"

    per_product_dir = fd_dir / "perProduct"
    sensitivity_dir = fd_dir / "sensitivity"

    file_prefix = SPL_NAME_MAPPING.get(spl_folder_name, spl_folder_name)

    print(f"\n{'=' * 80}")
    print(f"MERGING SHARDS FOR: {spl_folder_name}  (prefix: {file_prefix})")
    print(f"{'=' * 80}")

    sheets_data = {}

    # perProduct categories
    if per_product_dir.exists():
        per_product_categories = [
            ("EdgeOmission",              f"{file_prefix}_EdgeOmission_shard*.csv"),
            ("EventOmission",             f"{file_prefix}_EventOmission_shard*.csv"),
            ("EdgeOmission_MultiSeed",    f"{file_prefix}_EdgeOmission_MultiSeedRW_shard*.csv"),
            ("EventOmission_MultiSeed",   f"{file_prefix}_EventOmission_MultiSeedRW_shard*.csv"),
        ]
        for sheet_name, pattern in per_product_categories:
            df = load_shards(per_product_dir, pattern)
            if df is not None:
                sheets_data[sheet_name] = df
                print(f"  [OK] {sheet_name:27s}: {len(df):>5d} rows")
    else:
        print(f"  [!] perProduct directory missing: {per_product_dir}")

    # sensitivity categories
    if sensitivity_dir.exists():
        sensitivity_categories = [
            ("Sens_EdgeOmission",   f"{file_prefix}_DampingSensitivity_EdgeOmission_shard*.csv"),
            ("Sens_EventOmission",  f"{file_prefix}_DampingSensitivity_EventOmission_shard*.csv"),
            ("Sens_TestGen",        f"{file_prefix}_DampingSensitivity_TestGen_shard*.csv"),
        ]
        print("  --- sensitivity ---")
        for sheet_name, pattern in sensitivity_categories:
            df = load_shards(sensitivity_dir, pattern)
            if df is not None:
                sheets_data[sheet_name] = df
                print(f"  [OK] {sheet_name:27s}: {len(df):>5d} rows")
    else:
        print(f"  [!] sensitivity directory missing: {sensitivity_dir}")

    # Save Excel with the conventional name expected by downstream scripts
    if sheets_data:
        output_excel = spl_dir / f"RQ3_{spl_folder_name}_perProduct_rawData.xlsx"
        try:
            with pd.ExcelWriter(output_excel, engine='openpyxl') as writer:
                for sheet_name, df in sheets_data.items():
                    df.to_excel(writer, sheet_name=sheet_name, index=False)
            print(f"\n  SAVED: {output_excel.name}")
        except Exception as e:
            print(f"\n  [!!] ERROR saving Excel: {e}")
    else:
        print(f"\n  [!!] NO DATA to merge for {spl_folder_name}")


def main():
    print("=" * 80)
    print("rq3_01: SHARD MERGER (RAW DATA COMPILATION)")
    print("=" * 80)

    project_root = find_project_root()
    if not project_root:
        print("ERROR: Could not find project root (esg-with-feature-expressions).")
        sys.exit(1)

    cases_dir = project_root / "files" / "Cases"
    if not cases_dir.exists():
        print(f"ERROR: Cases directory not found at {cases_dir}")
        sys.exit(1)

    spls = [d.name for d in cases_dir.iterdir()
            if d.is_dir() and d.name in SPL_NAME_MAPPING]
    if not spls:
        print("ERROR: No valid SPL directories found!")
        sys.exit(1)

    for spl_name in sorted(spls):
        process_spl(spl_name, cases_dir)

    print("\n" + "=" * 80)
    print("rq3_01 DONE. Run rq3_02 next to aggregate across SPLs.")
    print("=" * 80)


if __name__ == "__main__":
    main()