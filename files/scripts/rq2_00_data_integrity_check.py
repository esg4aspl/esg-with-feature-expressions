#!/usr/bin/env python3
"""
rq2_00_data_integrity_check.py

Step 0 of the RQ2 pipeline — data integrity audit.

Reads files/Cases/<SPL>/RQ2_perShard_<SPL>.xlsx (produced by your
existing aggregation), and for each (SPL, approach, level, shard)
checks:

  - Number of runs present vs expected
  - Completely missing shards
  - Completeness ratio

Produces a per-SPL warnings report and a cross-SPL summary in one
Excel file. If the summary shows all cells green, the pipeline can
proceed to rq2_04+.

Expected run counts:
  - Small/Medium SPLs (SVM, eMail, Elevator, BAv2, SAS): 11 runs per shard
  - Large SPLs (Tesla, syngo.via, Hockerty): 3 runs per shard
  - ESG-Fx_L1: thesis-only; warnings here do not block article analysis

Paths (relative to this script):
    Script: files/scripts/statistical_test_scripts/rq2_00_data_integrity_check.py
    Data  : files/Cases/<SPL>/RQ2_perShard_<SPL>.xlsx
    Out   : files/scripts/statistical_test_scripts/rq2_result/
            rq2_data_integrity.xlsx

Usage:
    python rq2_00_data_integrity_check.py

Dependencies: pandas, openpyxl
"""
from __future__ import annotations

import sys
from pathlib import Path

import pandas as pd


# ─── Paths ─────────────────────────────────────────────────────────────────
SCRIPT_DIR = Path(__file__).resolve().parent
SCRIPTS_DIR = SCRIPT_DIR.parent
FILES_DIR = SCRIPTS_DIR.parent
DATA_DIR = FILES_DIR / "Cases"
OUTPUT_DIR = SCRIPT_DIR / "rq2_result"


# ─── Configuration ─────────────────────────────────────────────────────────
SPL_MAPPING = {
    "SodaVendingMachine": "SVM",
    "eMail": "eM",
    "Elevator": "El",
    "BankAccountv2": "BAv2",
    "StudentAttendanceSystem": "SAS",
    "Tesla": "Te",
    "syngovia": "Svia",
    "HockertyShirts": "HS",
}

SMALL_MEDIUM = {"SodaVendingMachine", "eMail", "Elevator",
                "BankAccountv2", "StudentAttendanceSystem"}
LARGE = {"Tesla", "syngovia", "HockertyShirts"}


def expected_runs(spl_folder, coverage_type):
    """Expected run count per (SPL, coverage_type).

    Note: for some small SPLs (SVM: 12 products, eMail: 23, El: 42),
    shards > product_count won't have any products to process — this is
    normal, not a missing data issue. We report shards with 0 products
    separately from truly missing runs.
    """
    if spl_folder in LARGE:
        return 3
    return 11


# ─── Core ──────────────────────────────────────────────────────────────────
def audit_sheet(df_sheet, expected, spl_folder, sheet_name):
    """Audit one (SPL, coverage_type) sheet. Returns list of dict rows."""
    if df_sheet is None or df_sheet.empty:
        return [], []

    rows = []
    warnings = []

    # Shards expected: always 80 on the cluster (80-way split)
    shard_col = "Shard" if "Shard" in df_sheet.columns else "Shard ID"
    if shard_col not in df_sheet.columns:
        return [], [f"[{sheet_name}] Shard column not found"]

    run_col = "RunID" if "RunID" in df_sheet.columns else "Run ID"
    if run_col not in df_sheet.columns:
        return [], [f"[{sheet_name}] RunID column not found"]

    products_col = None
    for candidate in ["Processed Products", " Processed Products"]:
        if candidate in df_sheet.columns:
            products_col = candidate
            break

    for shard in range(80):
        shard_rows = df_sheet[df_sheet[shard_col] == shard]
        runs_present = sorted(set(shard_rows[run_col].dropna().astype(int).tolist()))
        n_runs = len(runs_present)

        # Products processed this shard (median of runs to stay robust)
        prod_count = 0
        if products_col is not None and not shard_rows.empty:
            prod_count = int(shard_rows[products_col].median())

        # Missing runs are the gap: expected {1..N} minus present
        missing = [r for r in range(1, expected + 1) if r not in runs_present]

        rows.append({
            "SPL": spl_folder,
            "Coverage Type": sheet_name,
            "Shard": shard,
            "Runs Expected": expected,
            "Runs Present": n_runs,
            "Missing Runs": ", ".join(str(r) for r in missing) if missing else "",
            "Products This Shard": prod_count,
            "Status": "OK" if n_runs == expected else ("EMPTY" if prod_count == 0 else "INCOMPLETE"),
        })

        if missing and prod_count > 0:
            warnings.append(
                f"[{sheet_name}] Shard {shard}: {n_runs}/{expected} runs "
                f"(missing: {missing}, products: {prod_count})"
            )

    return rows, warnings


def audit_spl(spl_folder):
    """Audit all sheets of one SPL."""
    excel_path = DATA_DIR / spl_folder / f"RQ2_perShard_{spl_folder}.xlsx"
    if not excel_path.exists():
        return None, [f"[{spl_folder}] perShard Excel missing: {excel_path.name}"]

    xls = pd.ExcelFile(excel_path)
    all_rows = []
    all_warnings = []

    for sheet in xls.sheet_names:
        if sheet == "ESG-Fx_L1":
            # L1 is thesis-only; still audited but flagged
            expected = expected_runs(spl_folder, sheet)
        else:
            expected = expected_runs(spl_folder, sheet)

        df_sheet = pd.read_excel(xls, sheet_name=sheet)
        rows, warns = audit_sheet(df_sheet, expected, spl_folder, sheet)
        all_rows.extend(rows)
        all_warnings.extend(warns)

    return all_rows, all_warnings


# ─── Main ──────────────────────────────────────────────────────────────────
def main():
    print("=" * 70)
    print("rq2_00: RQ2 Data Integrity Check")
    print("=" * 70)
    print(f"Data directory   : {DATA_DIR}")
    print(f"Output directory : {OUTPUT_DIR}")

    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

    all_rows = []
    all_warnings_by_spl = {}

    for spl_folder in SPL_MAPPING:
        print(f"\nAuditing {spl_folder}...")
        rows, warnings = audit_spl(spl_folder)
        if rows is None:
            print(f"  [SKIP] {warnings[0]}")
            continue
        all_rows.extend(rows)
        all_warnings_by_spl[spl_folder] = warnings

        n_incomplete = sum(1 for r in rows if r["Status"] == "INCOMPLETE")
        n_empty = sum(1 for r in rows if r["Status"] == "EMPTY")
        n_ok = sum(1 for r in rows if r["Status"] == "OK")
        print(f"  OK: {n_ok}  INCOMPLETE: {n_incomplete}  EMPTY: {n_empty}")
        if n_incomplete > 0:
            print(f"  !! {n_incomplete} (SPL x approach x shard) cells have missing runs")

    if not all_rows:
        print("\nERROR: no data loaded. Did you run aggregation first?")
        sys.exit(1)

    df = pd.DataFrame(all_rows)

    # Cross-SPL summary
    summary_rows = []
    for (spl, cov), g in df.groupby(["SPL", "Coverage Type"]):
        n_non_empty = (g["Status"] != "EMPTY").sum()
        n_incomplete = (g["Status"] == "INCOMPLETE").sum()
        n_ok = (g["Status"] == "OK").sum()
        completeness = 100.0 * n_ok / n_non_empty if n_non_empty > 0 else 0.0
        summary_rows.append({
            "SPL": spl,
            "Coverage Type": cov,
            "Non-empty shards": n_non_empty,
            "OK shards": n_ok,
            "Incomplete shards": n_incomplete,
            "Completeness %": round(completeness, 2),
            "Ready for stats": "YES" if n_incomplete == 0 else "NO",
        })
    summary_df = pd.DataFrame(summary_rows)

    # Write output workbook
    output_file = OUTPUT_DIR / "rq2_data_integrity.xlsx"
    with pd.ExcelWriter(output_file, engine="openpyxl") as writer:
        # Sheet 1: cross-SPL summary
        summary_df.to_excel(writer, sheet_name="summary", index=False)

        # Sheet 2: full per-shard table
        df.to_excel(writer, sheet_name="per_shard_detail", index=False)

        # Sheet 3: only incomplete rows (action list)
        incomplete = df[df["Status"] == "INCOMPLETE"].copy()
        if not incomplete.empty:
            incomplete.to_excel(writer, sheet_name="incomplete_rerun_list", index=False)

    # Text warnings report
    warnings_file = OUTPUT_DIR / "rq2_data_integrity_warnings.txt"
    with open(warnings_file, "w") as f:
        for spl, warns in all_warnings_by_spl.items():
            f.write(f"=== {spl} ===\n")
            if warns:
                for w in warns:
                    f.write(f"  {w}\n")
            else:
                f.write("  (no warnings — all shards complete)\n")
            f.write("\n")

    print(f"\nSaved: {output_file.name}")
    print(f"Saved: {warnings_file.name}")
    print("\n=== Summary ===")
    print(summary_df.to_string(index=False))
    print("\nrq2_00 DONE.")


if __name__ == "__main__":
    main()