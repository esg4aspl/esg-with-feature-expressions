#!/bin/bash

# ============================================================
# RQ1 DATA COLLECTION AUTOMATION
# Description: Downloads comparativeEfficiencyTestPipeline and 
#              testsequences CSV/TXT files from remote servers.
# ============================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
IPS_FILE="$SCRIPT_DIR/ips.txt"

if [ ! -f "$IPS_FILE" ]; then
    echo "ERROR: ips.txt not found at $IPS_FILE"
    exit 1
fi

tr -d '\r' < "$IPS_FILE" > "${IPS_FILE}_clean" && mv "${IPS_FILE}_clean" "$IPS_FILE"
IFS=$'\n' read -d '' -r -a IPS < "$IPS_FILE"

CASES=(
    "SodaVendingMachine"
    "eMail"
    "Elevator"
    "BankAccountv2"
    "StudentAttendanceSystem"
    "Tesla"
    "syngovia"
    "HockertyShirts"
)

PIPELINE_SUBDIRS=(
    "EFG/L2"
    "EFG/L3"
    "EFG/L4"
    "ESG-Fx/L1"
    "ESG-Fx/L2"
    "ESG-Fx/L3"
    "ESG-Fx/L4"
    "RandomWalk/L0"
)

TESTSEQ_SUBDIRS=(
    "L0"
    "L1"
    "L2"
    "L3"
    "L4"
)

LOCAL_CASES_ROOT="/Users/dilekozturk/git/esg-with-feature-expressions/files/Cases"

echo "=== STARTING RQ1 DATA COLLECTION ==="

for IP in "${IPS[@]}"; do
    if [ -z "$IP" ]; then continue; fi

    echo "--------------------------------------------------"
    echo "📡 Processing Server: $IP"
    echo "--------------------------------------------------"

    for CASE in "${CASES[@]}"; do
        echo "   📂 Checking Case: $CASE"

        # 1. Collect comparativeEfficiencyTestPipeline Data
        for SUBDIR in "${PIPELINE_SUBDIRS[@]}"; do
            REMOTE_PATH="/root/esg-with-feature-expressions/files/Cases/$CASE/comparativeEfficiencyTestPipeline/$SUBDIR/*"
            LOCAL_TARGET_DIR="$LOCAL_CASES_ROOT/$CASE/comparativeEfficiencyTestPipeline/$SUBDIR"
            
            mkdir -p "$LOCAL_TARGET_DIR"

            scp -o StrictHostKeyChecking=no -o ConnectTimeout=10 \
                root@$IP:"$REMOTE_PATH" "$LOCAL_TARGET_DIR/" 2>/dev/null

            if [ $? -eq 0 ]; then
                echo "      ✅ [Pipeline: $SUBDIR] Downloaded."
            fi
        done

        # 2. Collect testsequences Data (TXT and CSV)
        for SUBDIR in "${TESTSEQ_SUBDIRS[@]}"; do
            REMOTE_PATH="/root/esg-with-feature-expressions/files/Cases/$CASE/testsequences/$SUBDIR/*"
            LOCAL_TARGET_DIR="$LOCAL_CASES_ROOT/$CASE/testsequences/$SUBDIR"
            
            mkdir -p "$LOCAL_TARGET_DIR"

            scp -o StrictHostKeyChecking=no -o ConnectTimeout=10 \
                root@$IP:"$REMOTE_PATH" "$LOCAL_TARGET_DIR/" 2>/dev/null

            if [ $? -eq 0 ]; then
                echo "      ✅ [TestSeq: $SUBDIR] Downloaded."
            fi
        done
        
    done
done

echo "=================================================="
echo "🎉 RQ1 Data Collection Completed."
echo "All files saved to: $LOCAL_CASES_ROOT"