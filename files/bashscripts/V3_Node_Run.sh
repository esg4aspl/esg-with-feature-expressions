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
#                           rq2        RQ2 only, measured again: one row at a
#                                      time, the other cores of the node idle
#                           rq2-rehearsal  the same, shortened
#
# One job starts one JVM per shard of this node and waits for them. Every JVM,
# GUITAR included, is started as a one-processor JVM, so that one JVM uses one
# core whatever the machine has.
#
# RQ1 measures the time of one product, so it runs first and alone: one job at
# a time, the other cores of the node idle. After it, four lanes run side by
# side, so a node needs four times as many cores as it has shards (8 shards per
# node on a 32-core node):
#   lane 1  RQ3 of every product line, RQ2 of the six smaller ones, then rows
#           of the second and third run of Tesla
#   lane 2  RQ2 of Tesla, first run, then rows of its second run
#   lane 3  RQ2 of Hockerty Shirts, the approach at its four levels (6 h per
#           row), then rows of the second and third run of Tesla
#   lane 4  RQ2 of Hockerty Shirts, the two baselines (6 h per row), then
#           rows of the third run of Tesla
#   In lanes 3 and 4 the rows of Hockerty Shirts run twice more with a budget
#   of one hour, right after the six-hour run.
# Hockerty Shirts is enumerated in the same order in every run, so the first
# hour of the three runs handles the same products.
#
# Mode rq2: the rows of RQ2 run one after the other, in the order of rq2_alone below, so that
# every row is measured with the same number of busy cores (the shards of this node and
# nothing else). The random baseline runs with the step limit of RQ1 and RQ3
# (min(5|V|^3, 2,000,000)).
#
# Next to the log of every JVM, GNU time writes shardNN.time: elapsed seconds; processor
# seconds in user mode; in kernel mode; peak resident memory (kB); exit status. The processor
# seconds include the processes the JVM started and waited for (GUITAR).
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
  # V3_REHEARSAL_LARGE_RUNS and V3_REHEARSAL_MINUTES let a rehearsal also go through the
  # later runs of the two large lines.
  SMALL_RUNS=1; LARGE_RUNS=${V3_REHEARSAL_LARGE_RUNS:-1}; RQ1_RUNS=1; SEEDS=3
  LARGE_TIMEOUT_MINUTES=${V3_REHEARSAL_MINUTES:-5}
elif [ "$MODE" = "rq2-rehearsal" ]; then
  SMALL_RUNS=1; LARGE_RUNS=2; RQ1_RUNS=0; SEEDS=0
  LARGE_TIMEOUT_MINUTES=${V3_REHEARSAL_MINUTES:-2}
elif [ "$MODE" = "full" ] || [ "$MODE" = "rq2" ]; then
  SMALL_RUNS=11; LARGE_RUNS=3; RQ1_RUNS=11; SEEDS=100; LARGE_TIMEOUT_MINUTES=0
else
  echo "Unknown mode: $MODE"; exit 1
fi
# Step limit of the random baseline in RQ2 (0: 5|V|^3 without an upper limit, as in the run
# of mode full).
case "$MODE" in rq2*) RQ2_RW_STEP_CAP=2000000 ;; *) RQ2_RW_STEP_CAP=0 ;; esac
HOCKERTY_TIMEOUT_HOURS=6
HOCKERTY_SHORT_TIMEOUT_HOURS=1

ALL_CASES=("SodaVendingMachine SVM" "eMail eM" "Elevator El" "BankAccountv2 BAv2"
           "StudentAttendanceSystem SAS" "syngovia Svia" "Tesla Te" "HockertyShirts HS")
SMALLER_CASES=("SodaVendingMachine SVM" "eMail eM" "Elevator El" "BankAccountv2 BAv2"
               "StudentAttendanceSystem SAS" "syngovia Svia")

# ---------------------------------------------------------------- machine ---
{
  echo "time: $(date -u +%Y-%m-%dT%H:%M:%SZ)"
  echo "mode: $MODE"
  echo "shards: $START_SHARD-$END_SHARD of $TOTAL_SHARDS"
  echo "RQ2 random step cap (RW_STEP_CAP): $RQ2_RW_STEP_CAP"
  echo "host: $(hostname)"
  echo "commit: $(git rev-parse HEAD 2>/dev/null)"
  echo "branch: $(git rev-parse --abbrev-ref HEAD 2>/dev/null)"
  echo "changed source files: $(git status --porcelain --untracked-files=no -- src pom.xml guitar-libs 2>/dev/null | wc -l)"
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
# -XX:ActiveProcessorCount=1: the JVM sizes its garbage collector and its compiler threads for
# one processor instead of for all the cores of the node.
ONE_CPU="-XX:ActiveProcessorCount=1"
# Event names contain letters outside ASCII (Tesla: "Überturbine Wheels"). Without a fixed
# encoding the JVM takes it from the locale of the session that started the node, and an
# unknown locale makes it write such a letter as "?". Every JVM reads and writes UTF-8.
export LC_ALL=C.UTF-8 LANG=C.UTF-8
UTF8="-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8"
ONE_CPU="$ONE_CPU $UTF8"
JAVA_OPTS="-Xms512m -Xmx1500m -XX:+UseG1GC $ONE_CPU"
export GUITAR_JVM_FLAGS="$ONE_CPU"
# RQ2 of the two large lines: every shard enumerates the whole configuration space and keeps one
# clause per configuration, so its heap grows with the enumeration.
JAVA_OPTS_TESLA_RQ2="-Xms512m -Xmx2500m -XX:+UseG1GC $ONE_CPU"
JAVA_OPTS_HOCKERTY_RQ2="-Xms512m -Xmx3000m -XX:+UseG1GC $ONE_CPU"
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
  local opts="$JAVA_OPTS"
  case "$driver:$short" in
    RQ2_*:Te) opts="$JAVA_OPTS_TESLA_RQ2" ;;
    RQ2_*:HS) opts="$JAVA_OPTS_HOCKERTY_RQ2" ;;
  esac
  for shard in $(seq "$START_SHARD" "$END_SHARD"); do
    env N_SHARDS=$TOTAL_SHARDS SHARD=$shard "$@" \
      /usr/bin/time -o "$log_dir/shard$(printf '%02d' "$shard").time" -f '%e;%U;%S;%M;%x' \
      java $opts -cp "$CP" "$LAUNCHER" "$case_pkg" "$short" "$driver" \
      > "$log_dir/shard$(printf '%02d' "$shard").log" 2>&1 &
    pids+=($!)
  done
  for pid in "${pids[@]}"; do wait "$pid" || failed=$((failed + 1)); done
  local end=$(date +%s)
  echo "$lane;$job;$start;$end;$((end - start));${#pids[@]};$failed" >> "$JOBS_CSV"
  [ "$failed" -gt 0 ] && echo "lane $lane: $job finished with $failed failed JVMs" || echo "lane $lane: $job finished"
}

timeout_args() {   # <short name> [hours for Hockerty Shirts] -> environment for the time budget of a row
  local short=$1 hockerty_hours=${2:-$HOCKERTY_TIMEOUT_HOURS}
  if [ "$LARGE_TIMEOUT_MINUTES" -gt 0 ] && { [ "$short" = "HS" ] || [ "$short" = "Te" ]; }; then
    echo "TIMEOUT_MINUTES=$LARGE_TIMEOUT_MINUTES"
  elif [ "$short" = "HS" ]; then
    echo "TIMEOUT_HOURS=$hockerty_hours"
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
rq1() {            # <lane> <case package> <short name>
  local lane=$1 c=$2 s=$3
  for run in $(seq 1 "$RQ1_RUNS"); do
    run_job "$lane" "RQ1_${s}_L1_run$run"  "$c" "$s" RQ1_ESGFx_L1   runID=$run
    for level in 2 3 4; do
      run_job "$lane" "RQ1_${s}_L${level}_run$run"     "$c" "$s" RQ1_ESGFx_L234 runID=$run L_LEVEL=$level
      run_job "$lane" "RQ1_${s}_EFG_L${level}_run$run" "$c" "$s" RQ1_EFG_L234   runID=$run L_LEVEL=$level
    done
  done
  for seed in $(seq 0 $((SEEDS - 1))); do
    run_job "$lane" "RQ1_${s}_RW_seed$seed" "$c" "$s" RQ1_RandomWalk runID=1 RW_SEED=$seed
  done
  # Warmed times, in runs of their own (run 0), so that the repeated generations do not warm
  # the JVM of a run whose times are reported. Each product is generated three times without
  # measurement and then once with.
  run_job "$lane" "RQ1_${s}_L1_warm" "$c" "$s" RQ1_ESGFx_L1 runID=0 WARM_REPEATS=3
  for level in 2 3 4; do
    run_job "$lane" "RQ1_${s}_L${level}_warm" "$c" "$s" RQ1_ESGFx_L234 runID=0 L_LEVEL=$level WARM_REPEATS=3
  done
  run_job "$lane" "RQ1_${s}_RW_warm" "$c" "$s" RQ1_RandomWalk runID=0 RW_SEED=0 WARM_REPEATS=3
}

rq3() {            # <lane> <case package> <short name>
  local lane=$1 c=$2 s=$3
  # RQ3 reads the GUITAR suites that RQ1 wrote on this node.
  run_job "$lane" "RQ3_${s}_EdgeOmission"     "$c" "$s" RQ3_EdgeOmitter     RQ3_MODE=memory RW_SEEDS=$SEEDS
  run_job "$lane" "RQ3_${s}_EventOmission"    "$c" "$s" RQ3_EventOmitter    RQ3_MODE=memory RW_SEEDS=$SEEDS
  run_job "$lane" "RQ3_${s}_SequenceOmission" "$c" "$s" RQ3_SequenceOmitter RW_SEEDS=$SEEDS
}

rq2_rows() {       # <lane> <case package> <short name> <run> <row> [<row> ...]
  local lane=$1 c=$2 s=$3 run=$4; shift 4
  local budget; budget=$(timeout_args "$s" "${HOCKERTY_HOURS_OF_THIS_CALL:-}")
  for row in "$@"; do
    case $row in
      RW)    run_job "$lane" "RQ2_${s}_RW_run$run" "$c" "$s" RQ2_RandomWalk runID=$run RW_STEP_CAP=$RQ2_RW_STEP_CAP $budget ;;
      L1)    run_job "$lane" "RQ2_${s}_L1_run$run" "$c" "$s" RQ2_L1         runID=$run $budget ;;
      L2|L3|L4)
             run_job "$lane" "RQ2_${s}_${row}_run$run" "$c" "$s" RQ2_L234   runID=$run L_LEVEL=${row#L} $budget ;;
      EFG2|EFG3|EFG4)
             run_job "$lane" "RQ2_${s}_EFG_L${row#EFG}_run$run" "$c" "$s" RQ2_EFG_L234 runID=$run L_LEVEL=${row#EFG} $budget ;;
    esac
  done
}

# The rows of Tesla beyond its first run are spread over the lanes that become free.
tesla() { [ "$LARGE_RUNS" -ge "$2" ] && rq2_rows "$1" Tesla Te "$2" "${@:3}"; }

# Second and third run of Hockerty Shirts with the short budget. They follow the six-hour run
# in the same two lanes, two rows side by side as in that run and while the other two lanes
# are still busy, so that the first hour of the three runs is measured under the same load.
hockerty_short() {  # <lane> <row> <row>
  [ "$LARGE_RUNS" -ge 2 ] || return 0
  for run in $(seq 2 "$LARGE_RUNS"); do
    HOCKERTY_HOURS_OF_THIS_CALL=$HOCKERTY_SHORT_TIMEOUT_HOURS rq2_rows "$1" HockertyShirts HS "$run" "${@:2}"
  done
}

lane1() {
  for entry in "${ALL_CASES[@]}"; do set -- $entry; rq3 1 "$1" "$2"; done
  for run in $(seq 1 "$SMALL_RUNS"); do
    for entry in "${SMALLER_CASES[@]}"; do
      set -- $entry; rq2_rows 1 "$1" "$2" "$run" L1 L2 L3 L4 RW EFG2 EFG3 EFG4
    done
  done
  tesla 1 2 EFG3
  tesla 1 2 EFG4
  tesla 1 3 L1 L2 L3 L4 RW
  tesla 1 3 EFG3
}
lane2() {
  rq2_rows 2 Tesla Te 1 L1 L2 L3 L4 RW EFG2 EFG3 EFG4
  tesla 2 2 EFG2
}
lane3() {
  rq2_rows 3 HockertyShirts HS 1 L1 L2 L3 L4
  hockerty_short 3 L1 L2 L3 L4
  tesla 3 2 L1 L2 L3 L4 RW
  tesla 3 3 EFG4
}
lane4() {
  rq2_rows 4 HockertyShirts HS 1 RW EFG2 EFG3 EFG4
  hockerty_short 4 RW EFG2 EFG3 EFG4
  tesla 4 3 EFG2
}

# syngo.via is the only line whose 5|V|^3 exceeds 2,000,000, so its random baseline is also
# measured without the cap (the rule of the earlier runs of RQ2). These records are kept apart,
# in files/Cases/syngovia/RQ2_RandomWalk_NoStepCap, and the records with the cap stay in place.
svia_random_without_cap() {
  local base="$FILES_DIR/Cases/syngovia/extremeScalabilityTestPipeline"
  local keep="$FILES_DIR/Cases/syngovia/RQ2_RandomWalk_NoStepCap"
  local aside="$FILES_DIR/Cases/syngovia/RQ2_RandomWalk_aside"
  [ "$RQ2_RW_STEP_CAP" -gt 0 ] && [ ! -e "$keep" ] && [ -d "$base/RandomWalk" ] || return 0
  mkdir -p "$aside/perProduct" "$keep/perProduct"
  mv "$base/RandomWalk" "$aside/RandomWalk" && mv "$base"/perProduct/Svia_RandomWalk_* "$aside/perProduct/" || return 1
  for run in $(seq 1 "$SMALL_RUNS"); do
    run_job 0 "RQ2_Svia_RWnocap_run$run" syngovia Svia RQ2_RandomWalk runID=$run RW_STEP_CAP=0 TIMEOUT_HOURS=0
  done
  mv "$base/RandomWalk" "$keep/RandomWalk"; mv "$base"/perProduct/Svia_RandomWalk_* "$keep/perProduct/"
  mv "$aside/RandomWalk" "$base/RandomWalk" && mv "$aside"/perProduct/* "$base/perProduct/" \
    && rmdir "$aside/perProduct" "$aside"
}

# Mode rq2: every row alone on the node. The six smaller lines and Tesla first, Hockerty
# Shirts last (six hours per row, then the runs of one hour).
rq2_alone() {
  local rows="L1 L2 L3 L4 RW EFG2 EFG3 EFG4"
  for run in $(seq 1 "$SMALL_RUNS"); do
    for entry in "${SMALLER_CASES[@]}"; do set -- $entry; rq2_rows 0 "$1" "$2" "$run" $rows; done
  done
  svia_random_without_cap
  for run in $(seq 1 "$LARGE_RUNS"); do rq2_rows 0 Tesla Te "$run" $rows; done
  rq2_rows 0 HockertyShirts HS 1 $rows
  hockerty_short 0 $rows
}
case "$MODE" in rq2*)
  rq2_alone > "$LOG_ROOT/rq2_${STAMP}.log" 2>&1
  echo "finished: $(date -u +%Y-%m-%dT%H:%M:%SZ)" >> "$LOG_ROOT/node_info_${STAMP}.txt"
  touch "$LOG_ROOT/ALL_DONE"
  echo "RQ2 FINISHED"
  exit 0 ;;
esac

# RQ1 first and alone.
for entry in "${ALL_CASES[@]}"; do set -- $entry; rq1 0 "$1" "$2"; done > "$LOG_ROOT/rq1_${STAMP}.log" 2>&1
echo "RQ1 finished: $(date -u +%Y-%m-%dT%H:%M:%SZ)"

lane1 > "$LOG_ROOT/lane1_${STAMP}.log" 2>&1 &
lane2 > "$LOG_ROOT/lane2_${STAMP}.log" 2>&1 &
lane3 > "$LOG_ROOT/lane3_${STAMP}.log" 2>&1 &
lane4 > "$LOG_ROOT/lane4_${STAMP}.log" 2>&1 &
wait

echo "finished: $(date -u +%Y-%m-%dT%H:%M:%SZ)" >> "$LOG_ROOT/node_info_${STAMP}.txt"
touch "$LOG_ROOT/ALL_DONE"
echo "ALL LANES FINISHED"
