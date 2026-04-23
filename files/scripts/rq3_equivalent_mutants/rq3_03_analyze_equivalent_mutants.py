#!/usr/bin/env python3
"""
analyze_equivalent_mutants.py

Classifies missed mutants into:
  (1) coverage-equivalent mutants — mutants whose mutated element (edge or
      vertex) is never touched by any test sequence under the current kill
      criterion, i.e. the mutant is structurally uncoverable at the given
      coverage level.
  (2) genuine misses — mutants whose element IS touched by at least one
      sequence but the RQ3 pipeline still reported them as missed.
      Should not occur for deterministic approaches; diagnostic signal.

Supports two mutation operators (see FaultDetector.java for kill criteria):

EDGE OMISSION (--operator edge)
  Mutant removes a single real edge (v_a, v_b).
  Kill criterion: some sequence contains v_a immediately followed by v_b.
  Coverage-equivalent = no sequence contains the pair (v_a, v_b).

EVENT OMISSION (--operator event)
  Mutant removes a single real vertex v_x (and all edges touching it).
  Kill criterion: some sequence visits v_x (mutantCurrentVertex == null fires
  when the detector encounters a vertex that no longer exists in the mutant).
  Coverage-equivalent = no sequence visits v_x.

Test suite parsing (mirrors FileToTestSuiteConverter.parseESGFxTestFile):
  - L0, L1, L2 files: comma-separated list of vertex tokens per line.
  - L3, L4 files    : comma-separated tokens, each a colon-joined path.
                      First token contributes all its vertices; subsequent
                      tokens contribute only their LAST vertex (sliding-window
                      semantics that yields a single flat vertex list).
  Vertex name matching: trailing '_<digits>' is stripped from both test-file
  tokens and DOT labels (Java findVertexByCleanName + getBaseName logic).

DOT parsing:
  Accepts the format produced by the project (digraph with 'a -> b' edges
  and 'v [label = "..."]' labels). Pseudo-start '[' and pseudo-end ']'
  are excluded; only real edges and real vertices are counted as potential
  mutation targets.

Paths (relative to this script's location):
    Script:  files/scripts/missing_faults/analyze_equivalent_mutants.py
    DOTs  :  files/Cases/<SPL>/DOTs/L2/<Product>.DOT
    Tests :  files/Cases/<SPL>/testsequences/L<k>/<Product>_L<k>.txt
             (L0 file name is <Product>_RandomWalk.txt)
    Output:  files/scripts/missing_faults/results/
             <SPL>_equivalent_mutants_<operator>_L<k>.xlsx
             <SPL>_equivalent_mutants_<operator>_L<k>_summary.csv

Usage (from any directory):
    # Edge omission, BAv2, L=2/3/4 (default operator is edge)
    python files/scripts/missing_faults/analyze_equivalent_mutants.py \
        --spl BankAccountv2 --levels 2 3 4

    # Event omission, eMail, L=4
    python files/scripts/missing_faults/analyze_equivalent_mutants.py \
        --spl eMail --levels 4 --operator event

    # Both operators in one call (--operator both)
    python files/scripts/missing_faults/analyze_equivalent_mutants.py \
        --spl BankAccountv2 --levels 4 --operator both

    # Specific products only
    python files/scripts/missing_faults/analyze_equivalent_mutants.py \
        --spl BankAccountv2 --levels 4 --operator both \
        --products P0497 P0498

If --products is omitted, every DOT file under DOTs/L2 is analyzed.

Dependencies: pandas, openpyxl
"""
from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path
from typing import Iterable

import pandas as pd


# --------------------------------------------------------------------------
# Paths
# --------------------------------------------------------------------------
SCRIPT_DIR = Path(__file__).resolve().parent
SCRIPTS_DIR = SCRIPT_DIR.parent
FILES_DIR = SCRIPTS_DIR.parent
CASES_DIR = FILES_DIR / "Cases"
RESULTS_DIR = SCRIPT_DIR / "results"


# --------------------------------------------------------------------------
# Regexes (DOT parser)
# --------------------------------------------------------------------------
EDGE_RE = re.compile(r'(\w+)\s*->\s*(\w+)')
LABEL_RE = re.compile(r'(\w+)\s*\[\s*label\s*=\s*"([^"]+)"')


# --------------------------------------------------------------------------
# Name normalization
# --------------------------------------------------------------------------
def strip_trailing_index(name: str) -> str:
    """Java getBaseName / findVertexByCleanName equivalent: strip '_<digits>' suffix."""
    m = re.match(r"^(.*?)_\d+$", name)
    return m.group(1) if m else name


def normalize_token(raw_token: str) -> str:
    """Clean test-file token: replace spaces with underscores, strip trailing _N."""
    cleaned = raw_token.strip().replace(" ", "_")
    return strip_trailing_index(cleaned)


def dot_vertex_matchkey(raw_label: str) -> str:
    """Extract match key from a DOT label such as 'get balance/b' -> 'get_balance'."""
    base = raw_label.split("/")[0].strip()
    return strip_trailing_index(base.replace(" ", "_"))


# --------------------------------------------------------------------------
# DOT parsing
# --------------------------------------------------------------------------
def parse_dot(dot_path: Path) -> tuple[set[tuple[str, str]], set[str], dict[str, str]]:
    """
    Parse a DOT file. Returns:
      - real_edges: set of (src_mk, tgt_mk) pairs, excluding pseudo vertices.
      - real_vertices: set of vertex match-keys, excluding pseudo '[' and ']'.
      - vertex_to_label_map: raw DOT id -> raw label (for diagnostics).
    """
    dot = dot_path.read_text(encoding="utf-8")

    label_map: dict[str, str] = {}
    for m in LABEL_RE.finditer(dot):
        vid, label = m.group(1), m.group(2)
        label_map[vid] = label

    if not label_map:
        raise ValueError(f"{dot_path}: no labels parsed — DOT format unexpected")

    match_key = {vid: dot_vertex_matchkey(lbl) for vid, lbl in label_map.items()}

    real_vertices: set[str] = set()
    for vid, lbl in label_map.items():
        if lbl in ("[", "]"):
            continue
        real_vertices.add(match_key[vid])

    real_edges: set[tuple[str, str]] = set()
    for m in EDGE_RE.finditer(dot):
        src, tgt = m.group(1), m.group(2)
        if src not in label_map or tgt not in label_map:
            continue
        if label_map[src] in ("[", "]") or label_map[tgt] in ("[", "]"):
            continue
        real_edges.add((match_key[src], match_key[tgt]))

    return real_edges, real_vertices, label_map


# --------------------------------------------------------------------------
# Test-file parsing
# --------------------------------------------------------------------------
def parse_test_file(test_path: Path, level: int) -> tuple[set[tuple[str, str]], set[str]]:
    """
    Parse a test-sequence file and return:
      - covered_pairs: set of (v_i, v_{i+1}) consecutive pairs — the edges
        that an Edge-Omission mutant CAN be killed by.
      - visited_vertices: set of vertex match-keys — the vertices that an
        Event-Omission mutant CAN be killed by.

    Format rules (mirrors FileToTestSuiteConverter.parseESGFxTestFile):
      - Skip empty lines.
      - Skip lines containing ' is ' (the 'L<k> is <coverage>%' footer).
      - Valid lines are 'N : <tokens>' where N is an int (#edges covered).
      - Tokens are split by ', '.
      - For L <= 2 (and L0/L1): each token is a single vertex name.
      - For L >= 3: each token is ':'-joined path; first token contributes all
        its parts, each subsequent token contributes only its last part.
    """
    covered_pairs: set[tuple[str, str]] = set()
    visited_vertices: set[str] = set()

    if not test_path.exists():
        return covered_pairs, visited_vertices

    with test_path.open(encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line or " is " in line:
                continue
            if " : " not in line:
                continue
            _, seq = line.split(" : ", 1)
            tokens = [t.strip() for t in seq.split(",") if t.strip()]

            vertex_list: list[str] = []
            if level <= 2:
                for tok in tokens:
                    vertex_list.append(normalize_token(tok))
            else:
                for i, tok in enumerate(tokens):
                    parts = [normalize_token(p) for p in tok.split(":")]
                    if i == 0:
                        vertex_list.extend(parts)
                    else:
                        vertex_list.append(parts[-1])

            visited_vertices.update(vertex_list)
            for a, b in zip(vertex_list[:-1], vertex_list[1:]):
                covered_pairs.add((a, b))

    return covered_pairs, visited_vertices


# --------------------------------------------------------------------------
# Path resolvers
# --------------------------------------------------------------------------
def dot_dir_for(spl: str) -> Path:
    return CASES_DIR / spl / "DOTs" / "L2"


def test_file_for(spl: str, product: str, level: int) -> Path:
    tests_root = CASES_DIR / spl / "testsequences" / f"L{level}"
    if level == 0:
        return tests_root / f"{product}_RandomWalk.txt"
    return tests_root / f"{product}_L{level}.txt"


def list_products(spl: str, explicit: Iterable[str] | None) -> list[str]:
    if explicit:
        return list(explicit)
    dot_dir = dot_dir_for(spl)
    if not dot_dir.exists():
        raise FileNotFoundError(f"DOT directory not found: {dot_dir}")
    upper = {p.stem for p in dot_dir.glob("*.DOT")}
    lower = {p.stem for p in dot_dir.glob("*.dot")}
    return sorted(upper | lower)


# --------------------------------------------------------------------------
# Per-product analysis — operator-aware
# --------------------------------------------------------------------------
def analyze_product(spl: str, product: str, level: int, operator: str) -> dict:
    """
    Classify every real mutant (edge or vertex, depending on operator) of
    `product` as covered or coverage-equivalent at the requested `level`.
    """
    dot_path = dot_dir_for(spl) / f"{product}.DOT"
    if not dot_path.exists():
        dot_path = dot_dir_for(spl) / f"{product}.dot"
    if not dot_path.exists():
        raise FileNotFoundError(f"DOT file not found for {product} in {dot_dir_for(spl)}")

    real_edges, real_vertices, _ = parse_dot(dot_path)
    test_path = test_file_for(spl, product, level)
    covered_pairs, visited_vertices = parse_test_file(test_path, level)

    rows: list[dict] = []

    if operator == "edge":
        for src, tgt in sorted(real_edges):
            is_covered = (src, tgt) in covered_pairs
            rows.append({
                "SPL": spl,
                "Product": product,
                "Level": f"L{level}",
                "Operator": "EdgeOmission",
                "MutatedElement": f"{src} -> {tgt}",
                "Source": src,
                "Target": tgt,
                "CoveredByTestSuite": is_covered,
                "Classification": "covered" if is_covered else "coverage_equivalent",
            })
        n_total = len(real_edges)

    elif operator == "event":
        for v in sorted(real_vertices):
            is_covered = v in visited_vertices
            rows.append({
                "SPL": spl,
                "Product": product,
                "Level": f"L{level}",
                "Operator": "EventOmission",
                "MutatedElement": v,
                "Source": v,
                "Target": "",
                "CoveredByTestSuite": is_covered,
                "Classification": "covered" if is_covered else "coverage_equivalent",
            })
        n_total = len(real_vertices)

    else:
        raise ValueError(f"Unknown operator: {operator}")

    n_covered = sum(r["CoveredByTestSuite"] for r in rows)
    n_equiv = n_total - n_covered

    return {
        "rows": rows,
        "summary": {
            "SPL": spl,
            "Product": product,
            "Level": f"L{level}",
            "Operator": "EdgeOmission" if operator == "edge" else "EventOmission",
            "TotalMutants": n_total,
            "CoveredMutants": n_covered,
            "CoverageEquivalentMutants": n_equiv,
            "CoveragePercent": round(100.0 * n_covered / n_total, 2) if n_total else 0.0,
            "TestFileFound": test_path.exists(),
            "TestFilePath": str(test_path),
        },
    }


# --------------------------------------------------------------------------
# Output writer
# --------------------------------------------------------------------------
def write_outputs(spl: str, level: int, operator: str,
                  all_rows: list[dict], all_summaries: list[dict]) -> None:
    RESULTS_DIR.mkdir(parents=True, exist_ok=True)

    df_rows = pd.DataFrame(all_rows)
    df_summary = pd.DataFrame(all_summaries)

    xlsx_path = RESULTS_DIR / f"{spl}_equivalent_mutants_{operator}_L{level}.xlsx"
    with pd.ExcelWriter(xlsx_path, engine="openpyxl") as w:
        df_rows.to_excel(w, sheet_name="PerMutant", index=False)
        df_summary.to_excel(w, sheet_name="PerProduct", index=False)

    csv_path = RESULTS_DIR / f"{spl}_equivalent_mutants_{operator}_L{level}_summary.csv"
    df_summary.to_csv(csv_path, index=False, sep=";")

    print(f"  Wrote {xlsx_path}")
    print(f"  Wrote {csv_path}")


# --------------------------------------------------------------------------
# Aggregate reporter
# --------------------------------------------------------------------------
def aggregate_report(spl: str, level: int, operator: str,
                     all_summaries: list[dict]) -> None:
    df = pd.DataFrame(all_summaries)
    total_products = len(df)
    found = df["TestFileFound"].sum()
    missing = total_products - found
    total_mutants = df["TotalMutants"].sum()
    total_covered = df["CoveredMutants"].sum()
    total_equiv = df["CoverageEquivalentMutants"].sum()
    prods_with_equiv = (df["CoverageEquivalentMutants"] > 0).sum()

    op_label = "EdgeOmission" if operator == "edge" else "EventOmission"
    print(f"\n--- {spl} @ L{level} / {op_label} aggregate ---")
    print(f"  Products analyzed          : {total_products}")
    print(f"  Test files found / missing : {found} / {missing}")
    print(f"  Total mutants (sum)        : {total_mutants}")
    print(f"  Total covered mutants      : {total_covered}")
    print(f"  Total coverage-equivalent  : {total_equiv}")
    print(f"  Products with >=1 equiv    : {prods_with_equiv}")
    if total_mutants > 0:
        print(f"  Global coverage            : {100.0 * total_covered / total_mutants:.2f}%")
    if prods_with_equiv > 0:
        avg = total_equiv / prods_with_equiv
        print(f"  Avg equiv per affected prod: {avg:.2f}")


# --------------------------------------------------------------------------
# Main
# --------------------------------------------------------------------------
def main() -> None:
    parser = argparse.ArgumentParser(
        description="Classify missed mutants as coverage-equivalent vs. genuine "
                    "for Edge-Omission or Event-Omission operators."
    )
    parser.add_argument("--spl", required=True,
                        help="SPL folder name under files/Cases/")
    parser.add_argument("--levels", nargs="+", type=int, default=[4],
                        help="Coverage levels to analyze (default: 4).")
    parser.add_argument("--operator", choices=["edge", "event", "both"], default="edge",
                        help="Mutation operator (default: edge).")
    parser.add_argument("--products", nargs="*", default=None,
                        help="Specific product IDs (e.g. P0497 P0498). Default: all.")
    args = parser.parse_args()

    print(f"Script     : {SCRIPT_DIR}")
    print(f"Cases root : {CASES_DIR}")
    print(f"SPL        : {args.spl}")
    print(f"Levels     : {args.levels}")
    print(f"Operator   : {args.operator}")

    try:
        products = list_products(args.spl, args.products)
    except FileNotFoundError as e:
        print(f"ERROR: {e}", file=sys.stderr)
        sys.exit(1)

    if not products:
        print(f"ERROR: no products found for {args.spl}", file=sys.stderr)
        sys.exit(1)
    print(f"Products   : {len(products)} found")

    operators = ["edge", "event"] if args.operator == "both" else [args.operator]

    for operator in operators:
        for level in args.levels:
            print(f"\n=== Analyzing L{level} / {operator.upper()} OMISSION ===")
            all_rows: list[dict] = []
            all_summaries: list[dict] = []
            for product in products:
                try:
                    result = analyze_product(args.spl, product, level, operator)
                except FileNotFoundError as e:
                    print(f"  {product}: SKIP ({e})")
                    continue
                all_rows.extend(result["rows"])
                all_summaries.append(result["summary"])

            if not all_summaries:
                print(f"  No products analyzable at L{level} / {operator}")
                continue

            aggregate_report(args.spl, level, operator, all_summaries)
            write_outputs(args.spl, level, operator, all_rows, all_summaries)

    print("\nDone.")


if __name__ == "__main__":
    main()