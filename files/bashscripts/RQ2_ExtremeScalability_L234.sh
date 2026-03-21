#!/bin/bash
# USAGE: ./RQ2_ExtremeScalability_L234.sh <CaseName> <ShortName> <RunID> [TotalShards] [StartShard] [EndShard]

if [ -z "$1" ] || [ -z "$2" ] || [ -z "$3" ]; then
  echo "ERROR: Missing required arguments."
  exit 1
fi

CASE_NAME=$1
SHORT_NAME=$2
RUN_PARAM=$3
SHARD_PARAM=$4
START_PARAM=$5
END_PARAM=$6

# DÜZELTME BURADA: FILES_DIR ve PROJECT_ROOT tanımlandı
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FILES_DIR="$(dirname "$SCRIPT_DIR")"
PROJECT_ROOT="$(dirname "$FILES_DIR")"

if [ -n "$SHARD_PARAM" ]; then
  N=$SHARD_PARAM
else
  if [[ "$OSTYPE" == "darwin"* ]]; then N=4; else N=40; fi
fi

if [ -n "$START_PARAM" ]; then S_NODE=$START_PARAM; else S_NODE=0; fi
if [ -n "$END_PARAM" ]; then E_NODE=$END_PARAM; else E_NODE=$((N-1)); fi

if [[ "$OSTYPE" == "darwin"* ]]; then
  XMS=512m; XMX=1200m
else
  XMS=512m; XMX=1500m
fi

if [[ "$CASE_NAME" == "HockertyShirts" ]] || [[ "$CASE_NAME" == "HS" ]]; then
    MY_TIMEOUT_HOURS=12
else
    MY_TIMEOUT_HOURS=0
fi

# DÜZELTME BURADA: Proje kök dizinine geçildi
cd "$PROJECT_ROOT" || { echo "ERROR: Project root not found"; exit 1; }

LOG_DIR_BASE="${FILES_DIR}/logs/${CASE_NAME}/RQ2"
mkdir -p "$LOG_DIR_BASE"

mvn clean package dependency:copy-dependencies -DskipTests > "$LOG_DIR_BASE/RQ2_ESGFx_L234_build.log" 2>&1
export CP="target/classes:target/dependency/*"

MAIN="tr.edu.iyte.esgfx.cases.${CASE_NAME}.RQ2_ExtremeScalability_L234_${SHORT_NAME}"
JAVA_OPTS="-Xms$XMS -Xmx$XMX -XX:+UseG1GC"

echo "=== BENCHMARK START RQ2: SHARDS $S_NODE to $E_NODE | RunID: $RUN_PARAM ==="

for L_VAL in 2 3 4; do
    echo "--- Initiating Execution for L_LEVEL = $L_VAL ---"
    
    LOG_DIR="${LOG_DIR_BASE}/ESGFx_L${L_VAL}"
    mkdir -p "$LOG_DIR"

    for i in $(seq $S_NODE $E_NODE); do
      LOG="${LOG_DIR}/run_RQ2_ESGFx_L${L_VAL}_runID${RUN_PARAM}_shard$(printf "%02d" $i).log"
      
      N_SHARDS=$N SHARD=$i runID=$RUN_PARAM TIMEOUT_HOURS=$MY_TIMEOUT_HOURS L_LEVEL=$L_VAL \
      nohup java $JAVA_OPTS -cp "$CP" "$MAIN" > "$LOG" 2>&1 &
      
      echo "Shard $i dispatched -> $LOG"
      if [[ "$OSTYPE" == "darwin"* ]]; then sleep 1; else sleep 0.2; fi
    done
    
    wait
done