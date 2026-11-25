#!/bin/bash
# ============================================================================
# Runs the experiments of one cluster node.
#
# USAGE: ./V3_Node_Run.sh <START_SHARD> <END_SHARD> <MODE>
#   START_SHARD, END_SHARD  the shards of this node out of 80 (for example 0 7)
#   MODE                    full       the complete study
#                           rehearsal  the same pipeline, shortened: one run,
#                                      three random seeds, five minutes per
#                                      row on the three large product lines
#
# One job starts one JVM per shard of this node and waits for them. Four lanes
# run side by side, so a node needs four times as many cores as it has shards
# (8 shards per node on a 32-core node):
#   lane 1  RQ1 and RQ3 of every product line, then RQ2 of the six smaller ones
#   lane 2  RQ2 of Tesla
#   lane 3  RQ2 of Hockerty Shirts, the approach at its four levels
#   lane 4  RQ2 of Hockerty Shirts, the two baselines
# Every lane runs its first repetition of every row before the second.
#
# Results of earlier runs found in the working tree are moved to
# files/archive_before_<time>/ before anything starts; nothing is deleted.
# ============================================================================
set -u

START_SHARD=${1:?first shard of this node}
END_SHARD=${2:?last shard of this node}
MODE=${3:-full}
TOTAL_SHARDS=80

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FILES_DIR="$(dirname "$SCRIPT_DIR")"
PROJECT_ROOT="$(dirname "$FILES_DIR")"
cd "$PROJECT_ROOT" || { echo "Project root not found"; exit 1; }

STAMP=$(date +%Y%m%d_%H%M%S)
LOG_ROOT="$FILES_DIR/logs/v3"
mkdir -p "$LOG_ROOT"
JOBS_CSV="$LOG_ROOT/jobs.csv"

if [ "$MODE" = "rehearsal" ]; then
  SMALL_RUNS=1; LARGE_RUNS=1; RQ1_RUNS=1; SEEDS=3; LARGE_TIMEOUT_MINUTES=5
else
  SMALL_RUNS=11; LARGE_RUNS=3; RQ1_RUNS=11; SEEDS=100; LARGE_TIMEOUT_MINUTES=0
fi
HOCKERTY_TIMEOUT_HOURS=6

ALL_CASES=("SodaVendingMachine SVM" "eMail eM" "Elevator El" "BankAccountv2 BAv2"
           "StudentAttendanceSystem SAS" "syngovia Svia" "Tesla Te" "HockertyShirts HS")
SMALLER_CASES=("SodaVendingMachine SVM" "eMail eM" "Elevator El" "BankAccountv2 BAv2"
               "StudentAttendanceSystem SAS" "syngovia Svia")

# ---------------------------------------------------------------- machine ---
{
  echo "time: $(date -u +%Y-%m-%dT%H:%M:%SZ)"
  echo "mode: $MODE"
  echo "shards: $START_SHARD-$END_SHARD of $TOTAL_SHARDS"
  echo "host: $(hostname)"
  echo "commit: $(git rev-parse HEAD 2>/dev/null)"
  echo "branch: $(git rev-parse --abbrev-ref HEAD 2>/dev/null)"
  echo "uncommitted files: $(git status --porcelain 2>/dev/null | wc -l)"
  echo "--- lscpu"; lscpu 2>/dev/null
  echo "--- memory"; free -m 2>/dev/null
  echo "--- java"; java -version 2>&1
  echo "--- java 8"; "${JAVA_8_EXE:-java}" -version 2>&1
  echo "--- gnu time"; ls -l /usr/bin/time 2>&1
} > "$LOG_ROOT/node_info_${STAMP}.txt"
export GIT_COMMIT="$(git rev-parse HEAD 2>/dev/null)"

# ------------------------------------------------- results of earlier runs ---
ARCHIVE="$FILES_DIR/archive_before_${STAMP}"
for case_dir in "$FILES_DIR"/Cases/*/; do
  case_name=$(basename "$case_dir")
  for sub in testsequences comparativeEfficiencyTestPipeline extremeScalabilityTestPipeline faultdetection \
             EFGs/efg_testsequences EFGs/efg_results RQ1 RQ2 RQ3; do
    if [ -e "$case_dir$sub" ]; then
      mkdir -p "$ARCHIVE/$case_name/$(dirname "$sub")"
      mv "$case_dir$sub" "$ARCHIVE/$case_name/$sub"
    fi
  done
done
[ -d "$ARCHIVE" ] && echo "Earlier results moved to $ARCHIVE"

# ------------------------------------------------------------------ build ---
echo "Building..."
mvn clean package dependency:copy-dependencies -DskipTests -Dfile.encoding=UTF-8 > "$LOG_ROOT/build_${STAMP}.log" 2>&1 \
  || { echo "BUILD FAILED, see $LOG_ROOT/build_${STAMP}.log"; exit 1; }
CP="target/classes:target/dependency/*"
JAVA_OPTS="-Xms512m -Xmx1500m -XX:+UseG1GC"
# Heap limit of every GUITAR process (the default of the 16 GB machines of the earlier runs).
export GUITAR_XMX=4g
LAUNCHER="tr.edu.iyte.esgfx.cases.ExperimentLauncher"

[ -f "$JOBS_CSV" ] || echo "lane;job;start;end;seconds;jvms;failed_jvms" > "$JOBS_CSV"

# run_job <lane> <job name> <case package> <short name> <driver> [VAR=value ...]
# Starts one JVM per shard of this node with the given environment and waits.
run_job() {
  local lane=$1 job=$2 case_pkg=$3 short=$4 driver=$5; shift 5
  local log_dir="$LOG_ROOT/lane$lane/$job"
  mkdir -p "$log_dir"
  local start=$(date +%s) pids=() failed=0
  for shard in $(seq "$START_SHARD" "$END_SHARD"); do
    env N_SHARDS=$TOTAL_SHARDS SHARD=$shard "$@" \
      java $JAVA_OPTS -cp "$CP" "$LAUNCHER" "$case_pkg" "$short" "$driver" \
      > "$log_dir/shard$(printf '%02d' "$shard").log" 2>&1 &
    pids+=($!)
  done
  for pid in "${pids[@]}"; do wait "$pid" || failed=$((failed + 1)); done
  local end=$(date +%s)
  echo "$lane;$job;$start;$end;$((end - start));${#pids[@]};$failed" >> "$JOBS_CSV"
  [ "$failed" -gt 0 ] && echo "lane $lane: $job finished with $failed failed JVMs" || echo "lane $lane: $job finished"
}

timeout_args() {   # <short name> -> environment for the time budget of a row
  local short=$1
  if [ "$LARGE_TIMEOUT_MINUTES" -gt 0 ] && { [ "$short" = "HS" ] || [ "$short" = "Te" ]; }; then
    echo "TIMEOUT_MINUTES=$LARGE_TIMEOUT_MINUTES"
  elif [ "$short" = "HS" ]; then
    echo "TIMEOUT_HOURS=$HOCKERTY_TIMEOUT_HOURS"
  else
    echo "TIMEOUT_HOURS=0"
  fi
}

# ------------------------------------------------------- product models ---
# Every node writes the product models of all eight lines; the result is the same on every node.
echo "Writing the product models..."
for entry in "${ALL_CASES[@]}"; do
  set -- $entry
  java $JAVA_OPTS -cp "$CP" "$LAUNCHER" "$1" "$2" ProductModels > "$LOG_ROOT/products_$2_${STAMP}.log" 2>&1 \
    || { echo "PRODUCT MODELS FAILED for $1, see $LOG_ROOT/products_$2_${STAMP}.log"; exit 1; }
  echo "  $1: $(ls "$FILES_DIR/Cases/$1/DOTs/L2" | grep -ci '\.dot$') product models"
done

# ------------------------------------------------------------------ lanes ---
rq1_and_rq3() {    # <lane> <case package> <short name>
  local lane=$1 c=$2 s=$3
  for run in $(seq 1 "$RQ1_RUNS"); do
    run_job "$lane" "RQ1_${s}_L1_run$run"  "$c" "$s" RQ1_ESGFx_L1   runID=$run WARM_REPEATS=3
    for level in 2 3 4; do
      run_job "$lane" "RQ1_${s}_L${level}_run$run"     "$c" "$s" RQ1_ESGFx_L234 runID=$run L_LEVEL=$level WARM_REPEATS=3
      run_job "$lane" "RQ1_${s}_EFG_L${level}_run$run" "$c" "$s" RQ1_EFG_L234   runID=$run L_LEVEL=$level
    done
  done
  for seed in $(seq 0 $((SEEDS - 1))); do
    run_job "$lane" "RQ1_${s}_RW_seed$seed" "$c" "$s" RQ1_RandomWalk runID=1 RW_SEED=$seed
  done
  # RQ3 reads the GUITAR suites that RQ1 wrote on this node.
  run_job "$lane" "RQ3_${s}_EdgeOmission"     "$c" "$s" RQ3_EdgeOmitter     RQ3_MODE=memory RW_SEEDS=$SEEDS
  run_job "$lane" "RQ3_${s}_EventOmission"    "$c" "$s" RQ3_EventOmitter    RQ3_MODE=memory RW_SEEDS=$SEEDS
  run_job "$lane" "RQ3_${s}_SequenceOmission" "$c" "$s" RQ3_SequenceOmitter RW_SEEDS=$SEEDS
}

rq2_rows() {       # <lane> <case package> <short name> <run> <row> [<row> ...]
  local lane=$1 c=$2 s=$3 run=$4; shift 4
  local budget; budget=$(timeout_args "$s")
  for row in "$@"; do
    case $row in
      RW)    run_job "$lane" "RQ2_${s}_RW_run$run" "$c" "$s" RQ2_RandomWalk runID=$run $budget ;;
      L1)    run_job "$lane" "RQ2_${s}_L1_run$run" "$c" "$s" RQ2_L1         runID=$run $budget ;;
      L2|L3|L4)
             run_job "$lane" "RQ2_${s}_${row}_run$run" "$c" "$s" RQ2_L234   runID=$run L_LEVEL=${row#L} $budget ;;
      EFG2|EFG3|EFG4)
             run_job "$lane" "RQ2_${s}_EFG_L${row#EFG}_run$run" "$c" "$s" RQ2_EFG_L234 runID=$run L_LEVEL=${row#EFG} $budget ;;
    esac
  done
}

lane1() {
  for entry in "${ALL_CASES[@]}"; do set -- $entry; rq1_and_rq3 1 "$1" "$2"; done
  for run in $(seq 1 "$SMALL_RUNS"); do
    for entry in "${SMALLER_CASES[@]}"; do
      set -- $entry; rq2_rows 1 "$1" "$2" "$run" L1 L2 L3 L4 RW EFG2 EFG3 EFG4
    done
  done
}
lane2() { for run in $(seq 1 "$LARGE_RUNS"); do rq2_rows 2 Tesla Te "$run" L1 L2 L3 L4 RW EFG2 EFG3 EFG4; done; }
lane3() { for run in $(seq 1 "$LARGE_RUNS"); do rq2_rows 3 HockertyShirts HS "$run" L1 L2 L3 L4; done; }
lane4() { for run in $(seq 1 "$LARGE_RUNS"); do rq2_rows 4 HockertyShirts HS "$run" RW EFG2 EFG3 EFG4; done; }

lane1 > "$LOG_ROOT/lane1_${STAMP}.log" 2>&1 &
lane2 > "$LOG_ROOT/lane2_${STAMP}.log" 2>&1 &
lane3 > "$LOG_ROOT/lane3_${STAMP}.log" 2>&1 &
lane4 > "$LOG_ROOT/lane4_${STAMP}.log" 2>&1 &
wait

echo "finished: $(date -u +%Y-%m-%dT%H:%M:%SZ)" >> "$LOG_ROOT/node_info_${STAMP}.txt"
touch "$LOG_ROOT/ALL_DONE"
echo "ALL LANES FINISHED"
