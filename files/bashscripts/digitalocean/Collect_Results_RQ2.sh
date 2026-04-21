#!/bin/bash

# ============================================================
# RQ2 DATA COLLECTION AUTOMATION
# Description: Downloads extremeScalabilityTestPipeline CSV 
#              files from remote servers and aggregates them 
#              into the identical local directory structure.
# ============================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Navigate two levels up to locate the main "files" directory
FILES_DIR="$(dirname "$(dirname "$SCRIPT_DIR")")"
IPS_FILE="$SCRIPT_DIR/ips.txt"

if [ ! -f "$IPS_FILE" ]; then
    echo "ERROR: ips.txt not found at $IPS_FILE"
    exit 1
fi

# Clean carriage returns from ips.txt and read into an array
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
    #"ESG-Fx/L1"
    "ESG-Fx/L2"
    "ESG-Fx/L3"
    "ESG-Fx/L4"
    "EFG/L2"
    "EFG/L3"
    "EFG/L4"
    #"RandomWalk/L0"
)

# Base path for local storage
LOCAL_CASES_ROOT="$FILES_DIR/Cases"

echo "=== STARTING RQ2 DATA COLLECTION ==="

for IP in "${IPS[@]}"; do
    # Skip empty lines in the IP list
    if [ -z "$IP" ]; then continue; fi

    echo "--------------------------------------------------"
    echo "Processing Server: $IP"
    echo "--------------------------------------------------"

    for CASE in "${CASES[@]}"; do
        echo "   Checking Case: $CASE"

        for SUBDIR in "${PIPELINE_SUBDIRS[@]}"; do
            REMOTE_PATH="/root/esg-with-feature-expressions/files/Cases/$CASE/extremeScalabilityTestPipeline/$SUBDIR/*.csv"
            LOCAL_TARGET_DIR="$LOCAL_CASES_ROOT/$CASE/extremeScalabilityTestPipeline/$SUBDIR"
            
            # Ensure the exact same directory structure exists locally
            mkdir -p "$LOCAL_TARGET_DIR"

            # Download the CSV files quietly
            scp -q -o StrictHostKeyChecking=no -o ConnectTimeout=10 \
                root@$IP:"$REMOTE_PATH" "$LOCAL_TARGET_DIR/" 2>/dev/null

            # Check if the transfer was successful and files exist
            if [ $? -eq 0 ]; then
                # Count the total accumulated CSV files locally
                count=$(ls -1 "$LOCAL_TARGET_DIR"/*.csv 2>/dev/null | wc -l | xargs)
                if [ "$count" -gt 0 ]; then
                    echo "      [✓] $SUBDIR -> Local total: $count files"
                fi
            fi
        done
    done
done

echo "=================================================="
echo "RQ2 DATA COLLECTION COMPLETE!"
echo "=================================================="