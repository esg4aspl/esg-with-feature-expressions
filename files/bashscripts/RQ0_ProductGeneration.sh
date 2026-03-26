#!/bin/bash
# USAGE: ./RQ0_ProductGeneration.sh <CaseName> <ShortName>

if [ -z "$1" ] || [ -z "$2" ]; then
  echo "ERROR: Missing required arguments."
  echo "Usage: ./RQ0_ProductGeneration.sh <CaseName> <ShortName>"
  exit 1
fi

CASE_NAME=$1
SHORT_NAME=$2

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FILES_DIR="$(dirname "$SCRIPT_DIR")"
PROJECT_ROOT="$(dirname "$FILES_DIR")"

if [[ "$OSTYPE" == "darwin"* ]]; then
  XMS="512m"; XMX="1200m"
else
  XMS="512m"; XMX="1500m"
fi

cd "$PROJECT_ROOT" || { echo "ERROR: Project root not found!"; exit 1; }

LOG_DIR="${FILES_DIR}/logs/${CASE_NAME}/RQ0_Generation"
mkdir -p "$LOG_DIR"

echo "Compiling project for Generation phase..."
#mvn clean package dependency:copy-dependencies -DskipTests > "$LOG_DIR/RQ0_build.log" 2>&1

if [ $? -ne 0 ]; then
    echo "COMPILATION ERROR! Check '$LOG_DIR/RQ0_build.log' for details."
    exit 1
fi

export CP="target/classes:target/dependency/*:target/esg-with-feature-expressions-0.0.1-SNAPSHOT.jar"
MAIN="tr.edu.iyte.esgfx.cases.${CASE_NAME}.ProductESGFxToEFGAndDOTFileWriter_${SHORT_NAME}"

JAVA_OPTS="-Xms$XMS -Xmx$XMX -XX:+UseG1GC"
LOG="${LOG_DIR}/RQ0_productgeneration.log"

echo "=== PRODUCT GENERATION START (ALL PRODUCTS) ==="

nohup java $JAVA_OPTS -cp "$CP" "$MAIN" > "$LOG" 2>&1 &

sleep 3
echo "Generation dispatched -> $LOG"
echo "GENERATION DISPATCHED FOR $CASE_NAME"