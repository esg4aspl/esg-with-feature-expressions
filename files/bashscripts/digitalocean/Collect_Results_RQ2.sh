#!/bin/bash

# ============================================================
# RQ2 DATA COLLECTION AUTOMATION
# Description: Downloads extremeScalabilityTestPipeline CSV 
#              files from remote servers.
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
    "ESG-Fx/L1"
    "ESG-Fx/L2"
    "ESG-Fx/L3"
    "ESG-Fx/L4"
)

LOCAL_CASES_ROOT="/Users/dilekozturk/git/esg-with-feature-expressions/files/Cases"

echo "=== STARTING RQ2 DATA COLLECTION ==="

for IP in "${IPS[@]}"; do
    if [ -z "$IP" ]; then continue; fi

    echo "--------------------------------------------------"
    echo "Processing Server: $IP"
    echo "--------------------------------------------------"

    for CASE in "${CASES[@]}"; do
        echo "   Checking Case: $CASE"

        for SUBDIR in "${PIPELINE_SUBDIRS[@]}"; do
            REMOTE_PATH="/root/esg-with-feature-expressions/files/Cases/$CASE/extremeScalabilityTestPipeline/$SUBDIR/*"
            LOCAL_TARGET_DIR="$LOCAL_CASES_ROOT/$CASE/extremeScalabilityTestPipeline/$SUBDIR"
            
            mkdir -p "$LOCAL_TARGET_DIR"

            scp -o StrictHostKeyChecking=no -o ConnectTimeout=10 \
                root@$IP:"$REMOTE_PATH" "$LOCAL_TARGET_DIR/" 2>/dev/null

            if [ $? -eq 0 ]; then
                echo "      [Pipeline: $SUBDIR] Downloaded."
            fi
        done
    done
done

echo "=================================================="
echo "RQ2 Data Collection Completed."
echo "All files saved to: $LOCAL_CASES_ROOT"