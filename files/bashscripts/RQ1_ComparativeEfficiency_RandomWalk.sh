#!/bin/bash
# USAGE: ./RQ1_ComparativeEfficiency_RandomWalk.sh <Case> <Short> <RunID> [TotalShards] [StartShard] [EndShard]

if [ -z "$1" ] || [ -z "$2" ] || [ -z "$3" ]; then
  echo "ERROR: Missing required arguments."
  echo "Usage: ./RQ1_ComparativeEfficiency_RandomWalk.sh <CaseName> <ShortName> <RunID> [TotalShards] [StartShard] [EndShard]"
  exit 1
fi

CASE_NAME=$1
SHORT_NAME=$2
RUN_PARAM=$3
SHARD_PARAM=$4
START_PARAM=$5
END_PARAM=$6

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

cd "$PROJECT_ROOT" || { echo "ERROR: Project root not found: $PROJECT_ROOT"; exit 1; }

LOG_DIR="${FILES_DIR}/logs/${CASE_NAME}/RQ1/ESGFx_L0"
mkdir -p "$LOG_DIR"

echo "Compiling project and gathering dependencies..."
mvn clean package dependency:copy-dependencies -DskipTests > "$LOG_DIR/$LOG_DIR_BASE/RQ1_ESGFx_L0_build.log" 2>&1
if [ $? -ne 0 ]; then
    echo "COMPILATION ERROR! Check '$LOG_DIR/RQ1_ESGFx_L0_build.log' for details."
    exit 1
fi

export CP="target/classes:target/dependency/*"

# IMPORTANT: Using CASE_NAME directly to prevent NoClassDefFoundError
MAIN="tr.edu.iyte.esgfx.cases.${CASE_NAME}.RQ1_ComparativeEfficiency_RandomWalk_${SHORT_NAME}"

JAVA_OPTS="-Xms$XMS -Xmx$XMX -XX:+UseG1GC"

echo "=== BENCHMARK START: SHARDS $S_NODE to $E_NODE (Total Context: $N) | RunID: $RUN_PARAM ==="

for i in $(seq $S_NODE $E_NODE); do

  LOG="${LOG_DIR}/run_RQ1_ESGFx_L0_runID${RUN_PARAM}_shard$(printf "%02d" $i).log"
  
  N_SHARDS=$N SHARD=$i runID=$RUN_PARAM \
  nohup java $JAVA_OPTS -cp "$CP" "$MAIN" > "$LOG" 2>&1 &
  
  echo "Shard $i dispatched -> $LOG"
  
  if [[ "$OSTYPE" == "darwin"* ]]; then sleep 1; else sleep 0.2; fi
done

echo "--------------------------------------------------"
echo "PROCESS DISPATCHED FOR $CASE_NAME (RandomWalk | Range: $S_NODE-$E_NODE | RunID: $RUN_PARAM)"