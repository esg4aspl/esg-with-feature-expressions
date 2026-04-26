#!/bin/bash
# ============================================================
# RERUN ORCHESTRATOR — MULTI-SPEC (sequential on one node)
# ============================================================
# USAGE: ./Rerun_Orchestrator.sh <max_concurrent> <spec1> [spec2] [spec3] ...
#
# Arguments:
#   max_concurrent     concurrency cap (e.g. 8 for an 8-core node)
#   spec1, spec2, ...  one or more spec file names in files/bashscripts/
#
# Runs each spec SEQUENTIALLY through master runner v5. The master
# runner builds the project once at the start of EACH spec invocation
# (mvn clean package), so two specs = two builds (~30-60s overhead each).
# This is intentional to keep the master runner unchanged and reusable.
#
# Sequential (not parallel) execution is deliberate: an 8-core node
# cannot productively run 16 parallel JVMs, so running RW and L1
# back-to-back is faster than racing them.
# ============================================================

MAX_CONCURRENT="${1:-8}"
shift
SPEC_FILES=("$@")

if [ ${#SPEC_FILES[@]} -eq 0 ]; then
    echo "USAGE: $0 <max_concurrent> <spec1> [spec2] ..."
    exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR" || exit 1

echo "=================================================="
echo "🎯 MULTI-SPEC RERUN ORCHESTRATOR"
echo "   Max concurrent: $MAX_CONCURRENT"
echo "   Spec files (run in order):"
for s in "${SPEC_FILES[@]}"; do
    echo "     - $s"
done
echo "=================================================="

GLOBAL_START=$(date +%s)

for SPEC_FILE_NAME in "${SPEC_FILES[@]}"; do
    SPEC_FILE_PATH="$SCRIPT_DIR/$SPEC_FILE_NAME"

    if [ ! -f "$SPEC_FILE_PATH" ]; then
        echo "❌ ERROR: Spec file not found: $SPEC_FILE_PATH — skipping"
        continue
    fi

    echo ""
    echo "=================================================="
    echo "▶  STARTING SPEC: $SPEC_FILE_NAME"
    echo "=================================================="

    bash ./RQ2_ExtremeScalability_Master_Runner_v5.sh "$SPEC_FILE_PATH" "$MAX_CONCURRENT"
    rc=$?

    if [ "$rc" -eq 0 ]; then
        echo "✅ SPEC COMPLETE: $SPEC_FILE_NAME"
    else
        echo "⚠️  SPEC FINISHED WITH NON-ZERO EXIT ($rc): $SPEC_FILE_NAME"
        echo "   Continuing to next spec anyway."
    fi
done

GLOBAL_END=$(date +%s)
ELAPSED=$((GLOBAL_END - GLOBAL_START))

echo ""
echo "=================================================="
echo "🏆 ALL SPECS COMPLETE"
echo "   Total wall-clock: $(( ELAPSED / 3600 ))h $(( (ELAPSED % 3600) / 60 ))m $(( ELAPSED % 60 ))s"
echo "=================================================="