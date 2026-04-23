#!/bin/bash
# ============================================================
# RQ2 EXTREME SCALABILITY MASTER RUNNER — v5 (SPL + RUN-ID BATCHED)
# ============================================================
# Two-level batching matches ORIGINAL execution pattern:
#   for each SPL in spec:
#       for each RUN_ID of this SPL:
#           launch all shards of (this SPL, this RUN_ID) in parallel
#           wait for all to finish
# 
# This ensures that at any moment, only shards from ONE (SPL, runID)
# combination are running — matches original master runner behavior.
#
# USAGE:
#   ./RQ2_ExtremeScalability_Master_Runner_v5.sh <spec_file> [max_concurrent]
# ============================================================

SPEC_FILE="${1:-}"
MAX_CONCURRENT="${2:-8}"

if [ -z "$SPEC_FILE" ]; then
  echo "USAGE: $0 <spec_file> [max_concurrent]"
  exit 1
fi

if [ ! -f "$SPEC_FILE" ]; then
  echo "ERROR: Spec file not found: $SPEC_FILE"
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FILES_DIR="$(dirname "$SCRIPT_DIR")"
PROJECT_ROOT="$(dirname "$FILES_DIR")"

cd "$PROJECT_ROOT" || { echo "CRITICAL ERROR: Project root not found!"; exit 1; }

SPEC_COUNT=$(grep -cvE '^\s*(#|$)' "$SPEC_FILE")

echo "--------------------------------------------------"
echo "🎯  RQ2 TARGETED RERUN (v5 — SPL + RUN-ID BATCHED)"
echo "📄  Spec file: $SPEC_FILE"
echo "📊  Total entries: $SPEC_COUNT"
echo "🔀  Max concurrent: $MAX_CONCURRENT"
echo "--------------------------------------------------"

if [[ "$OSTYPE" == "darwin"* ]]; then
  TARGET_SHARDS=4
else
  TARGET_SHARDS=80
fi

# ============================================================
# COMPILE ONCE
# ============================================================
echo ""
echo "=== COMPILING PROJECT ONCE ==="
mkdir -p "${FILES_DIR}/logs"
mvn clean package dependency:copy-dependencies -DskipTests -Dfile.encoding=UTF-8 \
    > "${FILES_DIR}/logs/RQ2_Master_Build_v5_$$.log" 2>&1
if [ $? -ne 0 ]; then
    echo "CRITICAL ERROR: Maven build FAILED! See logs/RQ2_Master_Build_v5_$$.log"
    exit 1
fi
echo "=== COMPILATION OK ==="

export CP="target/classes:target/dependency/*:target/esg-with-feature-expressions-0.0.1-SNAPSHOT.jar"

# ============================================================
# Helpers
# ============================================================
get_main_class() {
    local key=$1 case_name=$2 short=$3
    case "$key" in
        randomwalk) echo "tr.edu.iyte.esgfx.cases.${case_name}.RQ2_ExtremeScalability_RandomWalk_${short}" ;;
        l1)         echo "tr.edu.iyte.esgfx.cases.${case_name}.RQ2_ExtremeScalability_L1_${short}" ;;
        *)          echo "" ;;
    esac
}

get_log_subdir() {
    case "$1" in
        randomwalk) echo "RandomWalk" ;;
        l1)         echo "ESGFx/L1" ;;
        *)          echo "Unknown" ;;
    esac
}

get_log_prefix() {
    case "$1" in
        randomwalk) echo "run_RQ2_RandomWalk" ;;
        l1)         echo "run_RQ2_L1" ;;
        *)          echo "run_RQ2_Unknown" ;;
    esac
}

get_timeout_hours() {
    local case_name=$1 key=$2 run_id=$3
    if [[ "$case_name" == "HockertyShirts" ]]; then
        echo 6
    elif [[ "$case_name" == "syngovia" ]] && [[ "$key" == "randomwalk" ]]; then
        if [ "$run_id" -eq 1 ]; then echo 0; else echo 8; fi
    else
        echo 0
    fi
}

get_java_opts() {
    case "$1" in
        randomwalk) echo "-Xms512m -Xmx2000m -XX:+UseG1GC" ;;
        l1)         echo "-Xms512m -Xmx1500m -XX:+UseG1GC" ;;
        *)          echo "-Xms512m -Xmx1500m -XX:+UseG1GC" ;;
    esac
}

launch_entry() {
    local case_name=$1 short=$2 key=$3 shard=$4 run=$5

    local main_class=$(get_main_class "$key" "$case_name" "$short")
    if [ -z "$main_class" ]; then
        echo "⚠️  Unknown script_key='$key', skipping"
        return 1
    fi

    local log_subdir=$(get_log_subdir "$key")
    local log_prefix=$(get_log_prefix "$key")
    local log_dir="${FILES_DIR}/logs/${case_name}/RQ2/${log_subdir}"
    mkdir -p "$log_dir"

    local log_file="${log_dir}/${log_prefix}_runID${run}_shard$(printf "%02d" $shard).log"
    local timeout_h=$(get_timeout_hours "$case_name" "$key" "$run")
    local java_opts=$(get_java_opts "$key")

    N_SHARDS=$TARGET_SHARDS SHARD=$shard runID=$run TIMEOUT_HOURS=$timeout_h \
    nohup java $java_opts -cp "$CP" "$main_class" > "$log_file" 2>&1 &

    local pid=$!
    echo "      [LAUNCH] pid=$pid shard=$shard  ->  $(basename $log_file)"
}

wait_for_slot() {
    while true; do
        local n_running=$(jobs -rp | wc -l)
        if [ "$n_running" -lt "$MAX_CONCURRENT" ]; then
            break
        fi
        sleep 5
    done
}

# ============================================================
# PARSE SPEC: extract unique SPLs and unique runIDs per SPL
# (preserving first-occurrence order)
# ============================================================
echo ""
echo "=== Parsing spec file ==="

# Unique SPLs in order of first appearance
UNIQUE_SPLS=$(grep -vE '^\s*(#|$)' "$SPEC_FILE" | awk '{print $1}' | awk '!seen[$0]++')
echo "SPLs found: $UNIQUE_SPLS"

# ============================================================
# MAIN LOOP: SPL outer, runID inner
# ============================================================
echo ""
echo "=== RQ2 SPL+RUN-ID BATCHED RERUN STARTED ==="
START_TS=$(date +%s)

TOTAL_PROCESSED=0
for SPL_FOLDER in $UNIQUE_SPLS; do
    echo ""
    echo "============================================================"
    echo "  SPL: $SPL_FOLDER"
    echo "============================================================"

    # Unique runIDs for THIS SPL, in order
    RUN_IDS_FOR_SPL=$(grep -vE '^\s*(#|$)' "$SPEC_FILE" \
        | awk -v spl="$SPL_FOLDER" '$1 == spl {print $5}' | awk '!seen[$0]++')

    for RUN_ID in $RUN_IDS_FOR_SPL; do
        echo ""
        echo "    --- $SPL_FOLDER | Run ID $RUN_ID ---"

        # Entries for this (SPL, runID) combination
        BATCH_ENTRIES=$(grep -vE '^\s*(#|$)' "$SPEC_FILE" \
            | awk -v spl="$SPL_FOLDER" -v rid="$RUN_ID" '$1 == spl && $5 == rid')
        BATCH_COUNT=$(echo "$BATCH_ENTRIES" | grep -c .)
        echo "    Entries in batch: $BATCH_COUNT"

        while IFS= read -r line; do
            [[ -z "${line// }" ]] && continue
            read -r CASE_NAME SHORT_NAME SCRIPT_KEY SHARD_ID RUN_ID_LINE <<< "$line"

            if [ -z "$CASE_NAME" ] || [ -z "$SHARD_ID" ]; then
                continue
            fi

            TOTAL_PROCESSED=$((TOTAL_PROCESSED + 1))
            wait_for_slot
            launch_entry "$CASE_NAME" "$SHORT_NAME" "$SCRIPT_KEY" "$SHARD_ID" "$RUN_ID_LINE"

        done <<< "$BATCH_ENTRIES"

        # Wait for this batch to complete before next runID
        wait
        echo "    Batch $SPL_FOLDER runID=$RUN_ID complete."
    done

    echo "  SPL $SPL_FOLDER complete."
done

END_TS=$(date +%s)
ELAPSED=$((END_TS - START_TS))

echo ""
echo "=================================================="
echo "🏆 RQ2 SPL+RUN-ID BATCHED RERUN COMPLETE!"
echo "=================================================="
echo "Total entries processed: $TOTAL_PROCESSED"
echo "Wall-clock time: $(( ELAPSED / 3600 ))h $(( (ELAPSED % 3600) / 60 ))m $(( ELAPSED % 60 ))s"
echo ""