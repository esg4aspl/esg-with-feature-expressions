#!/bin/bash

# ============================================================
# SPECIAL DATA COLLECTION: ONLY DOTs and EFGs CSV FILES
# To prevent files from overlapping, add the server IP (server tag) information to the end of the names.
#
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

LOCAL_CASES_ROOT="/Users/dilekozturk/git/esg-with-feature-expressions/files/Cases"
SUBDIRS_TO_CHECK=("DOTs" "EFGs")

echo "=== FETCHING DOTs  EFGs CSV FILES (OVERWRITE PROTECTED) ==="

for IP in "${IPS[@]}"; do
    if [ -z "$IP" ]; then continue; fi

    IP_TAG=$(echo "$IP" | tr '.' '_')

    echo "--------------------------------------------------"
    echo "Server: $IP ..."
    echo "--------------------------------------------------"

    for CASE in "${CASES[@]}"; do
        for SUBDIR in "${SUBDIRS_TO_CHECK[@]}"; do
            REMOTE_PATH="/root/esg-with-feature-expressions/files/Cases/$CASE/$SUBDIR/*.csv"
            LOCAL_TARGET_DIR="$LOCAL_CASES_ROOT/$CASE/$SUBDIR"
            
            mkdir -p "$LOCAL_TARGET_DIR"

            
            TMP_DIR=$(mktemp -d)

            scp -o StrictHostKeyChecking=no -o ConnectTimeout=10 \
                root@$IP:"$REMOTE_PATH" "$TMP_DIR/" 2>/dev/null

            
            if [ $? -eq 0 ]; then
                for file in "$TMP_DIR"/*.csv; do
                    if [ -f "$file" ]; then
                        
                        basename=$(basename "$file" .csv)
                        
                        
                        mv "$file" "$LOCAL_TARGET_DIR/${basename}_server_${IP_TAG}.csv"
                    fi
                done
                
                count=$(ls -1q "$LOCAL_TARGET_DIR"/*_server_${IP_TAG}.csv 2>/dev/null | wc -l | xargs)
                if [ "$count" -gt 0 ]; then
                    echo "      ✅ [$CASE/$SUBDIR] $count CSV downloaded with TAG."
                fi
            fi
            
            rm -rf "$TMP_DIR"
        done
    done
done

echo "=== DONE ==="