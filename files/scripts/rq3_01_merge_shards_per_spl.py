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

Conventions
-----------
- Column ORDER in each output sheet is preserved exactly as it appears in the
  source shard CSVs (no hard-coded reordering). A consistency check warns if
  shards within the same category disagree on column order.
- The Java pipeline writes ``ESG-Fx_L0`` as the deterministic baseline label
  for the RandomWalk approach. This script renames it to the canonical
  ``RandomWalk`` so downstream analyses report the three approaches cleanly
  (ESG-Fx, EFG, RandomWalk).
- L=1 is kept here in raw form. Filtering for article-vs-thesis scope happens
  in rq3_02 (master aggregation), in keeping with RQ1/RQ2 policy.
- Approach sort order in deterministic sheets:
  ESG-Fx_L1, ESG-Fx_L2, ESG-Fx_L3, ESG-Fx_L4,
  EFG_L2, EFG_L3, EFG_L4,
  RandomWalk
- Excel column widths are set to ``len(header) + 2`` so headers fit on one line.
- CSV reading uses ``keep_default_na=False, na_values=['']`` so the literal
  string ``"None"`` (e.g. for ErrorReason cells) is not coerced to NaN.

Usage (from any directory above ``files/Cases``):
    python rq3_01_merge_shards_per_spl.py
"""

import glob
import sys
from pathlib import Path

import pandas as pd
from openpyxl.utils import get_column_letter


# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------

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

# Canonical approach order for deterministic sheets (used to sort rows).
# Anything not in this list (e.g. unexpected labels) goes to the end,
# alphabetically.
APPROACH_ORDER = [
    "ESG-Fx_L1", "ESG-Fx_L2", "ESG-Fx_L3", "ESG-Fx_L4",
    "EFG_L2", "EFG_L3", "EFG_L4",
    "RandomWalk",
]

# Java mislabels the RandomWalk baseline as "ESG-Fx_L0".
# We rename it everywhere we encounter it.
APPROACH_RENAMES = {
    "ESG-Fx_L0": "RandomWalk",
}


# ---------------------------------------------------------------------------
# Path discovery
# ---------------------------------------------------------------------------
def find_project_root():
    """Find project root by walking upward from cwd and from this file."""
    candidates = [Path.cwd(), *Path.cwd().parents]
    try:
        here = Path(__file__).resolve()
        candidates.extend([here.parent, *here.parents])
    except NameError:
        pass

    seen = set()
    for probe in candidates:
        if probe in seen:
            continue
        seen.add(probe)
        if (probe / "files" / "Cases").exists():
            return probe
    return None


# ---------------------------------------------------------------------------
# Shard loading + normalisation
# ---------------------------------------------------------------------------
def _read_one_shard(path):
    """Read a single shard CSV with project-wide conventions."""
    try:
        df = pd.read_csv(
            path,
            sep=";",
            decimal=",",
            keep_default_na=False,
            na_values=[""],
        )
    except Exception as e:
        print(f"      [!] error reading {Path(path).name}: {e}")
        return None
    df.columns = [c.strip() for c in df.columns]
    return df


def load_shards(directory, file_pattern):
    """
    Concatenate all shard CSVs matching a pattern into a single DataFrame.

    - Preserves the column order of the FIRST shard read (alphabetically by
      filename), and warns if any later shard disagrees.
    - Drops rows whose ProductID is missing (empty-shard rows that some Java
      writers emit).
    """
    search_path = directory / file_pattern
    shard_files = sorted(glob.glob(str(search_path)))

    if not shard_files:
        return None

    canonical_cols = None
    dfs = []
    for f in shard_files:
        df = _read_one_shard(f)
        if df is None or df.empty:
            continue

        if canonical_cols is None:
            canonical_cols = list(df.columns)
        else:
            if list(df.columns) != canonical_cols:
                # Column sets/orders differ between shards. Reorder to the
                # canonical column list (extras appended at the end) and warn.
                extras = [c for c in df.columns if c not in canonical_cols]
                missing = [c for c in canonical_cols if c not in df.columns]
                if extras or missing:
                    print(
                        f"      [!] {Path(f).name}: column mismatch "
                        f"(missing={missing}, extra={extras}); "
                        f"aligning to first shard's order"
                    )
                df = df.reindex(columns=canonical_cols + extras)

        dfs.append(df)

    if not dfs:
        return None

    combined = pd.concat(dfs, ignore_index=True, sort=False)

    # Drop rows with no product (empty-shard TestGen files emit these).
    if "ProductID" in combined.columns:
        before = len(combined)
        combined = combined.dropna(subset=["ProductID"]).reset_index(drop=True)
        dropped = before - len(combined)
        if dropped > 0:
            print(f"      [.] dropped {dropped} rows with empty ProductID")

    return combined


def _rename_approach_baseline(df):
    """
    Rename the mislabeled RandomWalk baseline (``ESG-Fx_L0``) to ``RandomWalk``
    in any sheet that has a TestingApproach column. Returns (df, renamed_count).
    """
    if "TestingApproach" not in df.columns:
        return df, 0
    mask = df["TestingApproach"].isin(APPROACH_RENAMES)
    n = int(mask.sum())
    if n:
        df = df.copy()
        df.loc[mask, "TestingApproach"] = df.loc[mask, "TestingApproach"].map(
            APPROACH_RENAMES
        )
    return df, n


def _sort_for_readability(df):
    """
    Order rows for human-readable output.

    - If TestingApproach is present, sort by ProductID then a categorical
      ordering of TestingApproach (APPROACH_ORDER first, unknowns appended
      alphabetically).
    - Otherwise sort by (ProductID, Seed) or (ProductID, DampingFactor)
      depending on which is present.
    """
    if df.empty:
        return df

    if "TestingApproach" in df.columns:
        unknown = sorted(set(df["TestingApproach"]) - set(APPROACH_ORDER))
        full_order = APPROACH_ORDER + unknown
        df = df.copy()
        df["TestingApproach"] = pd.Categorical(
            df["TestingApproach"], categories=full_order, ordered=True
        )
        sort_cols = [c for c in ("ProductID", "TestingApproach") if c in df.columns]
        df = df.sort_values(by=sort_cols, kind="mergesort").reset_index(drop=True)
        df["TestingApproach"] = df["TestingApproach"].astype(str)
        return df

    sort_cols = [
        c for c in ("ProductID", "Seed", "DampingFactor") if c in df.columns
    ]
    if sort_cols:
        df = df.sort_values(by=sort_cols, kind="mergesort").reset_index(drop=True)
    return df


# ---------------------------------------------------------------------------
# Excel writing
# ---------------------------------------------------------------------------
def _autosize_columns(worksheet, df):
    """Set every column width to len(header) + 2 so headers fit on one line."""
    for idx, col_name in enumerate(df.columns, start=1):
        worksheet.column_dimensions[get_column_letter(idx)].width = (
            len(str(col_name)) + 2
        )


def _write_excel(output_path, sheets_data):
    """Write all sheets and apply per-sheet column widths."""
    with pd.ExcelWriter(output_path, engine="openpyxl") as writer:
        for sheet_name, df in sheets_data.items():
            df.to_excel(writer, sheet_name=sheet_name, index=False)
            ws = writer.sheets[sheet_name]
            _autosize_columns(ws, df)


# ---------------------------------------------------------------------------
# Per-SPL pipeline
# ---------------------------------------------------------------------------
PER_PRODUCT_CATEGORIES = [
    # (sheet_name, shard_pattern_template)
    ("EdgeOmission",            "{prefix}_EdgeOmission_shard*.csv"),
    ("EventOmission",           "{prefix}_EventOmission_shard*.csv"),
    ("EdgeOmission_MultiSeed",  "{prefix}_EdgeOmission_MultiSeedRW_shard*.csv"),
    ("EventOmission_MultiSeed", "{prefix}_EventOmission_MultiSeedRW_shard*.csv"),
]

SENSITIVITY_CATEGORIES = [
    ("Sens_EdgeOmission",  "{prefix}_DampingSensitivity_EdgeOmission_shard*.csv"),
    ("Sens_EventOmission", "{prefix}_DampingSensitivity_EventOmission_shard*.csv"),
    ("Sens_TestGen",       "{prefix}_DampingSensitivity_TestGen_shard*.csv"),
]


def process_spl(spl_folder_name, cases_dir):
    """Process a single SPL: merge all shards and save the per-SPL Excel."""
    spl_dir = cases_dir / spl_folder_name
    fd_dir = spl_dir / "faultdetection"

    per_product_dir = fd_dir / "perProduct"
    sensitivity_dir = fd_dir / "sensitivity"

    file_prefix = SPL_NAME_MAPPING.get(spl_folder_name, spl_folder_name)

    print(f"\n{'=' * 80}")
    print(f"MERGING SHARDS FOR: {spl_folder_name}  (prefix: {file_prefix})")
    print(f"{'=' * 80}")

    sheets_data = {}
    total_renamed = 0

    # ---- perProduct (deterministic + multi-seed) -----------------------
    if per_product_dir.exists():
        for sheet_name, pattern_tpl in PER_PRODUCT_CATEGORIES:
            pattern = pattern_tpl.format(prefix=file_prefix)
            df = load_shards(per_product_dir, pattern)
            if df is None:
                continue
            df, n_renamed = _rename_approach_baseline(df)
            total_renamed += n_renamed
            df = _sort_for_readability(df)
            sheets_data[sheet_name] = df
            extra = f"  (renamed {n_renamed} ESG-Fx_L0 -> RandomWalk)" if n_renamed else ""
            print(f"  [OK] {sheet_name:27s}: {len(df):>5d} rows{extra}")
    else:
        print(f"  [!] perProduct directory missing: {per_product_dir}")

    # ---- sensitivity ---------------------------------------------------
    if sensitivity_dir.exists():
        print("  --- sensitivity ---")
        for sheet_name, pattern_tpl in SENSITIVITY_CATEGORIES:
            pattern = pattern_tpl.format(prefix=file_prefix)
            df = load_shards(sensitivity_dir, pattern)
            if df is None:
                continue
            df, n_renamed = _rename_approach_baseline(df)
            total_renamed += n_renamed
            df = _sort_for_readability(df)
            sheets_data[sheet_name] = df
            extra = f"  (renamed {n_renamed} ESG-Fx_L0 -> RandomWalk)" if n_renamed else ""
            print(f"  [OK] {sheet_name:27s}: {len(df):>5d} rows{extra}")
    else:
        print(f"  [!] sensitivity directory missing: {sensitivity_dir}")

    # ---- write -----------------------------------------------------------
    if sheets_data:
        output_excel = spl_dir / f"RQ3_{spl_folder_name}_perProduct_rawData.xlsx"
        try:
            _write_excel(output_excel, sheets_data)
            print(f"\n  SAVED: {output_excel.name}")
            if total_renamed:
                print(f"  TOTAL renamed ESG-Fx_L0 -> RandomWalk: {total_renamed} rows")
        except Exception as e:
            print(f"\n  [!!] ERROR saving Excel: {e}")
    else:
        print(f"\n  [!!] NO DATA to merge for {spl_folder_name}")


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------
def main():
    print("=" * 80)
    print("rq3_01: SHARD MERGER (RAW DATA COMPILATION)")
    print("=" * 80)

    project_root = find_project_root()
    if not project_root:
        print("ERROR: Could not find project root (no 'files/Cases' on the path).")
        sys.exit(1)

    cases_dir = project_root / "files" / "Cases"
    print(f"Cases directory: {cases_dir}")

    spls = [
        d.name
        for d in cases_dir.iterdir()
        if d.is_dir() and d.name in SPL_NAME_MAPPING
    ]
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