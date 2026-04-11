#!/bin/bash

# ============================================================
# RQ1 DATA COLLECTION AUTOMATION 
# Download .csv files. Skip (.txt) and (.EFG, .DOT).
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

# LOKAL PATH - Senin bilgisayarındaki tam yol
LOCAL_CASES_ROOT="/Users/dilekozturk/git/esg-with-feature-expressions/files/Cases"

# Sadece CSV'lerini çekeceğimiz alt dizinler
SUBDIRS_TO_CHECK=(
    "comparativeEfficiencyTestPipeline/EFG/L2"
    "comparativeEfficiencyTestPipeline/EFG/L3"
    "comparativeEfficiencyTestPipeline/EFG/L4"
    "comparativeEfficiencyTestPipeline/ESG-Fx/L1"
    "comparativeEfficiencyTestPipeline/ESG-Fx/L2"
    "comparativeEfficiencyTestPipeline/ESG-Fx/L3"
    "comparativeEfficiencyTestPipeline/ESG-Fx/L4"
    "comparativeEfficiencyTestPipeline/RandomWalk/L0"
    "DOTs"
    "EFGs"
    "EFGs/efg_results/L2"
    "EFGs/efg_results/L3"
    "EFGs/efg_results/L4"
    "testsequences/L0"
    "testsequences/L1"
    "testsequences/L2"
    "testsequences/L3"
    "testsequences/L4"
)

echo "=== STARTING RQ1 DATA COLLECTION (ONLY CSVs) ==="

for IP in "${IPS[@]}"; do
    if [ -z "$IP" ]; then continue; fi

    echo "--------------------------------------------------"
    echo "Processing Server: $IP"
    echo "--------------------------------------------------"

    for CASE in "${CASES[@]}"; do
        echo "   📂 Checking Case: $CASE"

        for SUBDIR in "${SUBDIRS_TO_CHECK[@]}"; do
            
            REMOTE_PATH="/root/esg-with-feature-expressions/files/Cases/$CASE/$SUBDIR/*.csv"
            LOCAL_TARGET_DIR="$LOCAL_CASES_ROOT/$CASE/$SUBDIR"
            
            mkdir -p "$LOCAL_TARGET_DIR"

            scp -o StrictHostKeyChecking=no -o ConnectTimeout=10 \
                root@$IP:"$REMOTE_PATH" "$LOCAL_TARGET_DIR/" 2>/dev/null

            
            if [ $? -eq 0 ]; then
                count=$(ls -1q "$LOCAL_TARGET_DIR"/*.csv 2>/dev/null | wc -l | xargs)
                if [ "$count" -gt 0 ]; then
                    echo "      ✅ [$SUBDIR] $count CSV downloaded."
                fi
            fi
        done
    done
done

echo "=== DATA COLLECTION COMPLETE ==="