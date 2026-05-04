#!/usr/bin/env python3
"""
rq2_01_merge_shards_per_spl.py  (Step 1 of 3)

Reads per-shard CSVs at:
    files/Cases/<SPL>/extremeScalabilityTestPipeline/<Approach>/<Level>/
        <spl>_<approach>_<level>_shardNN.csv

Produces:
    files/Cases/<SPL>/RQ2_perShard_<SPL>.xlsx
    (one sheet per Approach_Level; each row = one shard x one run)

This step performs:
  1. CSV merge with European format (sep=';', decimal=',')
  2. Adds Shard column from filename (00..79) and Approach column
  3. Renames Java's column names to the canonical no-space CamelCase
     schema. SPECIAL CASE for ESG-Fx_L1: Java's 'L1 Coverage(%)' is
     conceptually event coverage of the original ESG-Fx (single Euler
     cycle, no L-sequence transformation -- transfer note section 4),
     so it is renamed to EventCoverage(%) for consistency across
     approaches.
  4. Computes per-row T_pipeline(ms) using the canonical formulas:

       ESG-Fx: SatTime + ProdGenTime + TestGenTime + TestExecTime
               (TransformationTime is ALREADY counted inside TestGenTime
                in the Java instrumentation -- adding it would double-
                count. Coverage analysis is excluded as a validation
                observer, not a deployment cost.)
       EFG:    SatTime + ProdGenTime + EFGTransformationTime
                + TestGenTime + ParsingTime + TestExecTime
       RW:     SatTime + ProdGenTime + TestGenTime + TestExecTime

  5. Reorders columns to the canonical schema; preserves the original
     'Total Elapsed Time(ms)' as 'Java_T_total_buggy(ms)' at the end
     of the row for audit. Manuscript figures use T_pipeline, NOT this
     audit column.
"""

import pandas as pd
import sys
import re
from pathlib import Path


EXPECTED_SHARDS = 80

# Per-SPL expected run count. Small/medium SPLs use 11 repetitions for
# robust median estimation; large-scale SPLs use 3 because RQ2 measures
# throughput (not per-product variance) and full enumeration is itself
# computationally expensive (Hockerty: 124B configs).
LARGE_SCALE_SPLS = {'HockertyShirts', 'syngovia', 'Tesla'}
EXPECTED_RUNS_LARGE = 3
EXPECTED_RUNS_DEFAULT = 11


# ─── Rename mappings (Java column -> canonical) ────────────────────────
COMMON_RENAME = {
    "SPL Name": "SPLName",
    "Coverage Type": "CoverageType",
    "Total Elapsed Time(ms)": "Java_T_total_buggy(ms)",
    "SAT Time(ms)": "SatTime(ms)",
    "Product Gen Time(ms)": "ProdGenTime(ms)",
    "Test Generation Time(ms)": "TestGenTime(ms)",
    "Test Execution Time(ms)": "TestExecTime(ms)",
    "Test Generation Peak Memory(MB)": "TestGenPeakMemory(MB)",
    "Test Execution Peak Memory(MB)": "TestExecPeakMemory(MB)",
    "Processed Products": "HandledProducts",
    "Failed Products": "FailedProducts",
}

ESGFX_RENAME = {
    **COMMON_RENAME,
    "Transformation Time(ms)": "TransformationTime(ms)",
    "Number of ESGFx Vertices": "NumberOfESGFxVertices",
    "Number of ESGFx Edges": "NumberOfESGFxEdges",
    "Number of ESGFx Test Cases": "NumberOfESGFxTestCases",
    "Number of ESGFx Test Events": "NumberOfESGFxTestEvents",
    "Edge Coverage(%)": "EdgeCoverage(%)",
}

# L=1 specific: 'L1 Coverage(%)' is semantically event coverage of the
# original (un-transformed) ESG-Fx graph -- the L1 prefix is just the
# historical Java label. Rename to EventCoverage(%) for cross-approach
# consistency. The single 'Coverage Analysis Time(ms)' is paired with it.
ESGFX_L1_EXTRA_RENAME = {
    "L1 Coverage(%)": "EventCoverage(%)",
    "Coverage Analysis Time(ms)": "EventCoverageAnalysisTime(ms)",
}

# L>=2: MOGA pipeline only measures edge coverage (event analyser is
# intentionally absent -- the asymmetry noted in transfer note 2.2).
# The single 'Coverage Analysis Time(ms)' is the edge analyser.
ESGFX_L234_EXTRA_RENAME = {
    "Coverage Analysis Time(ms)": "EdgeCoverageAnalysisTime(ms)",
}

EFG_RENAME = {
    **COMMON_RENAME,
    "EFG Transformation Time(ms)": "EFGTransformationTime(ms)",
    "Parse Time(ms)": "ParsingTime(ms)",
    "Number of EFG Vertices": "NumberOfEFGVertices",
    "Number of EFG Edges": "NumberOfEFGEdges",
    "Number of EFG Test Cases": "NumberOfEFGTestCases",
    "Number of EFG Test Events": "NumberOfEFGTestEvents",
    "Event Coverage(%)": "EventCoverage(%)",
    "Event Coverage Analysis Time(ms)": "EventCoverageAnalysisTime(ms)",
    "Edge Coverage(%)": "EdgeCoverage(%)",
    "Edge Coverage Analysis Time(ms)": "EdgeCoverageAnalysisTime(ms)",
}

# RW operates on the ESG-Fx graph too (random-walk traversal instead of
# MOGA), so its Vertices/Edges/TestCases/TestEvents columns map to the
# NumberOfESGFx* canonical names per transfer note section 6.B.
RW_RENAME = {
    **COMMON_RENAME,
    "Number of Vertices": "NumberOfESGFxVertices",
    "Number of Edges": "NumberOfESGFxEdges",
    "Number of Test Cases": "NumberOfESGFxTestCases",
    "Number of Test Events": "NumberOfESGFxTestEvents",
    "Event Coverage(%)": "EventCoverage(%)",
    "Event Coverage Analysis Time(ms)": "EventCoverageAnalysisTime(ms)",
    "Edge Coverage(%)": "EdgeCoverage(%)",
    "Edge Coverage Analysis Time(ms)": "EdgeCoverageAnalysisTime(ms)",
    "Aborted Sequences": "AbortedSequences",
    "Safety Limit Hit Count": "SafetyLimitHitCount",
    "Avg Time on Safety Limit(ms)": "AvgTimeOnSafetyLimit(ms)",
    "Avg Steps on Safety Limit": "AvgStepsOnSafetyLimit",
    "Avg Coverage at Safety Limit(%)": "AvgCoverageAtSafetyLimit(%)",
}


def select_rename_map(approach_label, level_label):
    if approach_label == "ESG-Fx":
        rename = dict(ESGFX_RENAME)
        if level_label == "L1":
            rename.update(ESGFX_L1_EXTRA_RENAME)
        else:
            rename.update(ESGFX_L234_EXTRA_RENAME)
        return rename
    if approach_label == "EFG":
        return dict(EFG_RENAME)
    if approach_label == "RandomWalk":
        return dict(RW_RENAME)
    return {}


# ─── Canonical column order per (approach, level) ──────────────────────
def canonical_column_order(approach_label, level_label):
    """Return the desired column order for one sheet.

    Approach-specific columns are inserted at canonical positions so
    every sheet shares the same skeleton (ID -> time -> memory ->
    graph -> coverage -> approach-specific -> audit).
    """
    is_esgfx = approach_label == "ESG-Fx"
    is_efg = approach_label == "EFG"
    is_rw = approach_label == "RandomWalk"
    is_l1 = level_label == "L1"

    # ID block
    cols = ["Approach", "SPLName", "CoverageType", "RunID", "Shard",
            "HandledProducts", "FailedProducts"]

    # Time block (T_pipeline first, then components in pipeline order)
    cols.append("T_pipeline(ms)")
    cols.append("SatTime(ms)")
    cols.append("ProdGenTime(ms)")
    if is_efg:
        cols.append("EFGTransformationTime(ms)")
    if is_esgfx and not is_l1:
        cols.append("TransformationTime(ms)")
    cols.append("TestGenTime(ms)")
    if is_efg:
        cols.append("ParsingTime(ms)")
    cols.append("TestExecTime(ms)")

    # Memory
    cols.append("TestGenPeakMemory(MB)")
    cols.append("TestExecPeakMemory(MB)")

    # Graph metrics
    if is_efg:
        cols.extend(["NumberOfEFGVertices", "NumberOfEFGEdges",
                     "NumberOfEFGTestCases", "NumberOfEFGTestEvents"])
    else:
        cols.extend(["NumberOfESGFxVertices", "NumberOfESGFxEdges",
                     "NumberOfESGFxTestCases", "NumberOfESGFxTestEvents"])

    # Coverage block:
    #   ESG-Fx L=1 : event only (renamed from L1)
    #   ESG-Fx L>=2: edge only (MOGA asymmetry)
    #   EFG / RW   : both event and edge
    has_event = is_efg or is_rw or (is_esgfx and is_l1)
    has_edge = is_efg or is_rw or (is_esgfx and not is_l1)
    if has_event:
        cols.extend(["EventCoverage(%)", "EventCoverageAnalysisTime(ms)"])
    if has_edge:
        cols.extend(["EdgeCoverage(%)", "EdgeCoverageAnalysisTime(ms)"])

    # RW-specific
    if is_rw:
        cols.extend(["AbortedSequences", "SafetyLimitHitCount",
                     "AvgTimeOnSafetyLimit(ms)", "AvgStepsOnSafetyLimit",
                     "AvgCoverageAtSafetyLimit(%)"])

    # Audit
    cols.append("Java_T_total_buggy(ms)")
    # Status / ErrorReason, if present, are appended after this list.
    return cols


# ─── Pipeline-time formulas ────────────────────────────────────────────
def t_pipeline_for_row(row, approach_label):
    """Compute per-row T_pipeline using canonical formulas.

    Returns NaN if any required component is missing -- a missing
    component means the run failed before reaching that stage, in
    which case the pipeline time isn't well-defined.
    """
    if approach_label == "ESG-Fx":
        # NB: TransformationTime intentionally excluded -- Java already
        # counts it inside TestGenTime. For L=1 this column doesn't
        # exist (no transformation step), but the formula still works
        # because we only sum the four components below.
        cols = ["SatTime(ms)", "ProdGenTime(ms)",
                "TestGenTime(ms)", "TestExecTime(ms)"]
    elif approach_label == "EFG":
        cols = ["SatTime(ms)", "ProdGenTime(ms)",
                "EFGTransformationTime(ms)", "TestGenTime(ms)",
                "ParsingTime(ms)", "TestExecTime(ms)"]
    elif approach_label == "RandomWalk":
        cols = ["SatTime(ms)", "ProdGenTime(ms)",
                "TestGenTime(ms)", "TestExecTime(ms)"]
    else:
        return float('nan')

    total = 0.0
    for c in cols:
        v = row.get(c)
        if v is None or pd.isna(v):
            return float('nan')
        total += float(v)
    return total


# ─── Path helpers ──────────────────────────────────────────────────────
def find_project_root():
    """Walk up from cwd and from this script's path looking for files/Cases/.

    Pure relative discovery -- no hardcoded user paths. Run this script
    from anywhere inside the repo and it will locate the root.
    """
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


def expected_runs_for(spl_name):
    return EXPECTED_RUNS_LARGE if spl_name in LARGE_SCALE_SPLS else EXPECTED_RUNS_DEFAULT


def parse_approach_level(sheet_name):
    """'<Approach>_<Level>' -> (approach, level). Splits on the LAST '_'
    so 'ESG-Fx_L2' parses correctly even though 'ESG-Fx' contains a
    hyphen."""
    if "_" not in sheet_name:
        return sheet_name, ""
    idx = sheet_name.rfind("_")
    return sheet_name[:idx], sheet_name[idx + 1:]


def reorder_and_pad(df, desired_order):
    """Reorder df so 'desired_order' columns come first; append any
    unexpected columns at the end with a console note."""
    actual = list(df.columns)
    front = [c for c in desired_order if c in actual]
    extras = [c for c in actual if c not in desired_order]
    if extras:
        print(f"    Note: {len(extras)} unexpected column(s) appended at end: {extras}")
    return df[front + extras]


# ─── Per-SPL processing ────────────────────────────────────────────────
def process_spl(spl_dir):
    pipeline_dir = spl_dir / "extremeScalabilityTestPipeline"
    if not pipeline_dir.exists():
        return False

    print(f"\n{'='*80}")
    print(f"[Step 1] Merging shard CSVs for: {spl_dir.name}")
    print(f"{'='*80}")

    shard_files = list(pipeline_dir.rglob("*.csv"))
    if not shard_files:
        print(f"  [!] No CSV files found in {pipeline_dir.name}")
        return False

    sheets_data = {}
    for f in shard_files:
        match = re.search(r'shard(\d+)', f.stem, re.IGNORECASE)
        shard_no = match.group(1) if match else "Unknown"
        level = f.parent.name           # e.g. 'L2'
        approach = f.parent.parent.name  # e.g. 'ESG-Fx'
        sheet_name = f"{approach}_{level}"

        try:
            df = pd.read_csv(f, sep=';', decimal=',')
        except Exception as e:
            print(f"  [!] Error reading {f.name}: {e}")
            continue
        df.columns = [c.strip() for c in df.columns]
        df['Shard'] = shard_no
        sheets_data.setdefault(sheet_name, []).append(df)

    if not sheets_data:
        return False

    output_excel = spl_dir / f"RQ2_perShard_{spl_dir.name}.xlsx"
    try:
        with pd.ExcelWriter(output_excel, engine='openpyxl') as writer:
            for sheet_name, df_list in sheets_data.items():
                approach_label, level_label = parse_approach_level(sheet_name)
                combined = pd.concat(df_list, ignore_index=True)

                # Apply rename map (canonical schema)
                rename_map = select_rename_map(approach_label, level_label)

                # Java instrumentation quirk: for ESG-Fx L=1 on some
                # SPLs, Java emits BOTH 'L1 Coverage(%)' (the original
                # column) AND 'Event Coverage(%)' (a later-added
                # duplicate). The two are complementary -- different
                # subsets of rows are populated in each. Merge them
                # before rename so no L=1 measurement is lost.
                if approach_label == "ESG-Fx" and level_label == "L1":
                    if ("L1 Coverage(%)" in combined.columns
                            and "Event Coverage(%)" in combined.columns):
                        combined["L1 Coverage(%)"] = combined["L1 Coverage(%)"].fillna(
                            combined["Event Coverage(%)"])
                        combined = combined.drop(columns=["Event Coverage(%)"])
                    elif "Event Coverage(%)" in combined.columns:
                        # Only the new name is present -- treat it as L=1 coverage
                        combined = combined.rename(
                            columns={"Event Coverage(%)": "L1 Coverage(%)"})

                combined = combined.rename(columns=rename_map)

                # Add Approach as a first-class column
                combined.insert(0, 'Approach', approach_label)

                # Compute per-row T_pipeline
                combined['T_pipeline(ms)'] = combined.apply(
                    lambda r: t_pipeline_for_row(r, approach_label), axis=1)

                # Sort: shard ascending, then RunID ascending
                combined['_shard_num'] = pd.to_numeric(
                    combined['Shard'], errors='coerce')
                run_col = ('RunID' if 'RunID' in combined.columns
                           else ('Run ID' if 'Run ID' in combined.columns else None))
                sort_cols = ['_shard_num']
                if run_col:
                    sort_cols.append(run_col)
                combined = (combined.sort_values(by=sort_cols)
                                    .drop(columns=['_shard_num'])
                                    .reset_index(drop=True))

                # Reorder to canonical schema (with Status/ErrorReason
                # appended at the very end if present)
                desired = canonical_column_order(approach_label, level_label)
                for c in ('Status', 'ErrorReason'):
                    if c in combined.columns and c not in desired:
                        desired.append(c)
                combined = reorder_and_pad(combined, desired)

                # --- Sanity check vs expected shards x runs ----------
                expected_runs = expected_runs_for(spl_dir.name)
                expected_rows = EXPECTED_SHARDS * expected_runs
                n_shards = combined['Shard'].nunique()
                n_rows = len(combined)
                flag = "" if n_rows == expected_rows else \
                       f"  <-- expected {expected_rows} ({EXPECTED_SHARDS}x{expected_runs})"
                print(f"  [OK] {sheet_name:15s} shards={n_shards:3d}  rows={n_rows:4d}{flag}")
                if n_shards != EXPECTED_SHARDS:
                    print(f"       WARNING: {EXPECTED_SHARDS - n_shards} shard(s) missing in {sheet_name}")
                if run_col and run_col in combined.columns:
                    runs_per_shard = combined.groupby('Shard')[run_col].count()
                    short = runs_per_shard[runs_per_shard < expected_runs]
                    if len(short) > 0:
                        total_missing = int((expected_runs - short).sum())
                        print(f"       NOTE: {len(short)} shard(s) short on runs "
                              f"(total {total_missing} missing, expected {expected_runs}/shard)")

                combined.to_excel(writer, sheet_name=sheet_name, index=False)

        print(f"\n  SAVED -> {output_excel.name}")
        return True
    except Exception as e:
        print(f"\n  ERROR saving Excel: {e}")
        return False


def main():
    print("="*80)
    print("RQ2 STEP 1 / 3 - MERGE SHARD CSVS  (canonical schema + T_pipeline)")
    print("="*80)

    project_root = find_project_root()
    if not project_root:
        print("ERROR: Could not find project root containing files/Cases/.")
        print("Run this script from inside the repo.")
        sys.exit(1)

    cases_dir = project_root / "files" / "Cases"
    print(f"Project root: {project_root}")
    print(f"Cases dir   : {cases_dir}")

    spls = [d for d in cases_dir.iterdir() if d.is_dir()]
    processed = 0
    for spl_dir in sorted(spls):
        if process_spl(spl_dir):
            processed += 1

    print("\n" + "="*80)
    if processed:
        print(f"DONE. RQ2_perShard_*.xlsx generated for {processed} SPL(s).")
        print("Next: run rq2_02_median_across_runs.py")
    else:
        print("No RQ2 extreme-scalability data found to merge.")
    print("="*80)


if __name__ == "__main__":
    main()