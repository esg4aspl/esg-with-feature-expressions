#!/usr/bin/env python3
"""Checks the results of a run of V3_Node_Run.sh before they are used.

Usage: python3 v3_check.py <folder> [--full]

<folder> is a repository root, or a folder that holds the unpacked archives
of several nodes (node00, node01, ...) as V3_Collect.sh leaves them. With
--full the number of products of every line is also compared with the
expected number; without it (a rehearsal) the numbers are only printed.

Every check prints one line. The script ends with exit code 1 if a check
failed, so it can be used to decide whether the complete study may start.
"""
import csv
import glob
import gzip
import os
import sys
from collections import Counter, defaultdict

EXPECTED_PRODUCTS = {"SVM": 12, "eM": 24, "El": 42, "BAv2": 504, "SAS": 2664, "Svia": 219, "Te": 400, "HS": 416}
EXPECTED_ENUMERATED = {"SVM": 12, "eM": 24, "El": 42, "BAv2": 504, "SAS": 2664, "Svia": 219, "Te": 428544}
LEVEL_COLUMNS = {1: ("EventsRequired", "EventsCovered"), 2: ("CouplesRequired", "CouplesCovered"),
                 3: ("TriplesRequired", "TriplesCovered"), 4: ("QuadruplesRequired", "QuadruplesCovered")}

failures = []


def report(ok, text):
    print(("ok    " if ok else "FAIL  ") + text)
    if not ok:
        failures.append(text)


def rows(path):
    opener = gzip.open if path.endswith(".gz") else open
    with opener(path, "rt", encoding="utf-8", newline="") as handle:
        lines = [line for line in handle if not line.startswith("#")]
    return list(csv.DictReader(lines, delimiter=";"))


def find(root, pattern):
    return sorted(glob.glob(os.path.join(root, "**", pattern), recursive=True))


def short_name(path):
    return os.path.basename(path).split("_")[0]


def check_jobs(root):
    files = find(root, "jobs.csv")
    report(bool(files), "jobs.csv found (%d)" % len(files))
    jobs = failed = 0
    for path in files:
        for row in rows(path):
            jobs += 1
            if row["failed_jvms"] != "0":
                failed += 1
                print("        failed: %s (%s JVMs) in %s" % (row["job"], row["failed_jvms"], path))
    report(failed == 0, "%d jobs, %d with a failed JVM" % (jobs, failed))
    done = find(root, "ALL_DONE")
    report(bool(done), "nodes that reached the end: %d" % len(done))


def check_rq1(root, full):
    logs = [p for p in find(root, "*PerProductLog*.csv") if "testsequences" in p or "efg_results" in p]
    report(bool(logs), "RQ1 per-product logs found (%d)" % len(logs))
    products = defaultdict(set)
    counts = Counter()
    for path in logs:
        name = os.path.basename(path)
        approach = "EFG" if "EFG" in name else "RW" if "RandomWalk" in name else "ESG-Fx"
        level = 0
        for k in (1, 2, 3, 4):
            if name.replace(".csv", "").endswith("_L%d" % k):
                level = k
        for row in rows(path):
            if None in row or any(value is None for value in row.values()):
                counts["rows with a wrong number of columns"] += 1
                continue
            key = (short_name(path), approach, level)
            products[key].add(row["ProductID"])
            counts["rows"] += 1
            if row["Status"] != "SUCCESS":
                counts["rows not SUCCESS"] += 1
            if approach == "EFG":
                # GUITAR writes one test for every sequence of its length; with a correct Event
                # Flow Graph file its suite covers all of them and all event couples.
                required, covered = LEVEL_COLUMNS[level]
                if row[required] != row[covered] or row["CouplesRequired"] != row["CouplesCovered"]:
                    counts["structural suites below 100%% at their length or at event-couple level"] += 1
                if level == 2 and row["NumberOfEFGEdges"] != row["CouplesRequired"]:
                    counts["Event Flow Graph files whose edge count is not the number of event couples"] += 1
            if approach != "EFG" and row["ReplayViolations"] != "0":
                counts["suites of the approach or of random with a replay violation"] += 1
            if approach == "ESG-Fx":
                for k in range(1, level + 1):
                    required, covered = LEVEL_COLUMNS[k]
                    if row[required] != row[covered]:
                        counts["suites of the approach below 100%% at or under their level"] += 1
                        break
            if approach == "RW" and row.get("SafetyLimitHit", "false").lower() == "false" \
                    and row["CouplesRequired"] != row["CouplesCovered"]:
                counts["random suites that stopped below full event-couple coverage without the step limit"] += 1
    report(counts["rows"] > 0, "RQ1 rows read: %d" % counts["rows"])
    for text in ("structural suites below 100%% at their length or at event-couple level",
                 "Event Flow Graph files whose edge count is not the number of event couples"):
        report(counts[text] == 0, "RQ1 %s: %d" % (text.replace("%%", "%"), counts[text]))
    for text in ("rows with a wrong number of columns", "rows not SUCCESS",
                 "suites of the approach or of random with a replay violation",
                 "suites of the approach below 100%% at or under their level",
                 "random suites that stopped below full event-couple coverage without the step limit"):
        report(counts[text] == 0, "RQ1 %s: %d" % (text.replace("%%", "%"), counts[text]))
    for key in sorted(products):
        spl, approach, level = key
        found = len(products[key])
        if full:
            report(found == EXPECTED_PRODUCTS.get(spl, found),
                   "RQ1 %s %s L%d: %d products (expected %s)" % (spl, approach, level, found, EXPECTED_PRODUCTS.get(spl)))
        else:
            print("      RQ1 %s %s L%d: %d products" % (spl, approach, level, found))


def check_rq2(root, full):
    files = [p for p in find(root, "*_jvm.csv") if "extremeScalabilityTestPipeline" in p]
    report(bool(files), "RQ2 JVM rows found in %d files" % len(files))
    counts = Counter()
    enumerated = defaultdict(set)
    for path in files:
        for row in rows(path):
            counts["rows"] += 1
            spl = short_name(path)
            stop = row.get("StopReason", "")
            counts["stop: " + stop] += 1
            if stop == "Completed":
                enumerated[spl].add(row["EnumeratedConfigurations"])
            if row["FailedProducts"] != "0":
                counts["JVMs with failed products"] += 1
            if row["Approach"] != "EFG" and row["ReplayViolations"] != "0":
                counts["JVMs of the approach or of random with replay violations"] += 1
            if row["Approach"] == "ESG-Fx":
                level = int(row["Level"].replace("L", ""))
                required, covered = LEVEL_COLUMNS[level]
                if row[required] != row[covered]:
                    counts["JVMs of the approach below 100% at their level"] += 1
    for key in sorted(counts):
        if key.startswith("stop: "):
            print("      RQ2 JVMs with %s: %d" % (key, counts[key]))
    report(counts["stop: OutOfMemoryInEnumeration"] == 0, "RQ2 JVMs stopped by memory in the enumeration: %d"
           % counts["stop: OutOfMemoryInEnumeration"])
    for text in ("JVMs with failed products", "JVMs of the approach or of random with replay violations",
                 "JVMs of the approach below 100% at their level"):
        report(counts[text] == 0, "RQ2 %s: %d" % (text, counts[text]))
    for spl in sorted(enumerated):
        values = enumerated[spl]
        expected = EXPECTED_ENUMERATED.get(spl)
        ok = len(values) == 1 and (expected is None or values == {str(expected)})
        report(ok, "RQ2 %s: products enumerated by the completed JVMs: %s (expected %s)" % (spl, sorted(values), expected))
    records = [p for p in find(root, "*_products.csv.gz")]
    report(bool(records), "RQ2 per-product records found (%d files)" % len(records))
    bad = 0
    for path in records[:2000]:
        try:
            for row in rows(path):
                if row["Status"] != "SUCCESS":
                    bad += 1
        except Exception as error:  # a truncated file
            report(False, "RQ2 record cannot be read: %s (%s)" % (path, error))
    report(bad == 0, "RQ2 products not SUCCESS in the records read: %d" % bad)


def check_rq3(root):
    def load(pattern):
        result = {}
        for path in find(root, pattern):
            with open(path, encoding="utf-8", newline="") as handle:
                data = list(csv.reader(handle, delimiter=";"))
            for row in data[1:]:
                result[(row[0], row[1], row[2], row[3])] = row
        return result
    sequence = load("*_SequenceOmission_shard*.csv")
    report(bool(sequence), "RQ3 sequence-omission rows: %d" % len(sequence))
    below = 0
    for (spl, product, operator, approach), row in sequence.items():
        if approach.startswith("ESG-Fx_L") and approach != "ESG-Fx_L0":
            level = int(approach[-1])
            k = int(operator[-1])
            if k <= level and row[4] != row[5]:
                below += 1
    report(below == 0, "RQ3 suites of the approach that do not kill every mutant at or under their level: %d" % below)
    for operator, k in (("EdgeOmission", 2), ("EventOmission", 1)):
        old = load("*_%s_shard*.csv" % operator)
        compared = different = 0
        for (spl, product, _, approach), row in old.items():
            other = sequence.get((spl, product, "SequenceOmission%d" % k, approach))
            if other is None:
                continue
            compared += 1
            if row[4:11] != other[4:11]:
                different += 1
        report(compared > 0 and different == 0,
               "RQ3 %s rows equal to the sequence-omission rows of length %d: %d compared, %d different"
               % (operator, k, compared, different))


def check_stores(root):
    stores = find(root, "*.txt.gz")
    report(bool(stores), "stored suite files found (%d)" % len(stores))
    unreadable = empty = damaged = 0
    for path in stores:
        try:
            with gzip.open(path, "rt", encoding="utf-8") as handle:
                has_sequence = False
                for line in handle:
                    if line.startswith("S;"):
                        has_sequence = True
                    # An event name written without UTF-8 ("?berturbine Wheels" for "\u00dcberturbine Wheels").
                    elif "\ufffd" in line or ("berturbine" in line and "\u00dcberturbine" not in line):
                        damaged += 1
                if not has_sequence:
                    empty += 1
        except Exception:
            unreadable += 1
    report(unreadable == 0, "stored suite files that cannot be read to the end: %d" % unreadable)
    report(damaged == 0, "stored event names damaged by the character encoding: %d" % damaged)
    print("      stored suite files without a sequence (shards without a product): %d" % empty)


def main():
    if len(sys.argv) < 2:
        print(__doc__)
        sys.exit(2)
    root = sys.argv[1]
    full = "--full" in sys.argv
    check_jobs(root)
    check_rq1(root, full)
    check_rq2(root, full)
    check_rq3(root)
    check_stores(root)
    print()
    if failures:
        print("%d CHECKS FAILED" % len(failures))
        sys.exit(1)
    print("ALL CHECKS PASSED")


if __name__ == "__main__":
    main()
