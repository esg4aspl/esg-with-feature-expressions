#!/bin/bash

# ============================================================
# RQ1 DATA COLLECTION (CSVs only)
#
# Downloads all .csv files from the RQ1-related directories
# on each remote node. Skips .txt, .DOT, .EFG files.
#
# ips.txt format (two whitespace-separated columns):
#   IP               TAG
# Lines starting with # and blank lines are ignored.
# ============================================================

set -u

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
IPS_FILE="$SCRIPT_DIR/ips.txt"

if [ ! -f "$IPS_FILE" ]; then
    echo "ERROR: ips.txt not found at $IPS_FILE"
    exit 1
fi

# Normalize line endings
tr -d '\r' < "$IPS_FILE" > "${IPS_FILE}_clean" && mv "${IPS_FILE}_clean" "$IPS_FILE"

# Parse two-column ips.txt (extract ONLY the IP)
IPS=()
TAGS=()
while IFS= read -r line || [ -n "$line" ]; do
    case "$line" in
        '#'*|'') continue ;;
    esac
    ip=$(echo "$line" | awk '{print $1}')
    tag=$(echo "$line" | awk '{print $2}')
    if [ -z "$ip" ]; then continue; fi
    if [ -z "$tag" ]; then tag="node$((${#IPS[@]}))"; fi
    IPS+=("$ip")
    TAGS+=("$tag")
done < "$IPS_FILE"

if [ "${#IPS[@]}" -eq 0 ]; then
    echo "ERROR: no IPs parsed from $IPS_FILE"
    exit 1
fi

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

# Directories under each case root to collect CSVs from
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

echo "===================================================="
echo "RQ1 DATA COLLECTION (CSVs only)"
echo "===================================================="
echo "Nodes: ${#IPS[@]}"
echo "SPLs : ${#CASES[@]}"
echo "===================================================="

TOTAL_DOWNLOADED=0
TOTAL_SKIPPED=0

for i in "${!IPS[@]}"; do
    IP="${IPS[$i]}"
    TAG="${TAGS[$i]}"

    echo ""
    echo "--------------------------------------------------"
    echo "Node: $IP  (tag: $TAG)"
    echo "--------------------------------------------------"

    for CASE in "${CASES[@]}"; do
        case_downloads=0

        for SUBDIR in "${SUBDIRS_TO_CHECK[@]}"; do
            REMOTE_PATH="/root/esg-with-feature-expressions/files/Cases/$CASE/$SUBDIR/*.csv"
            LOCAL_TARGET_DIR="$LOCAL_CASES_ROOT/$CASE/$SUBDIR"
            mkdir -p "$LOCAL_TARGET_DIR"

            # Count local CSVs BEFORE scp (to compute delta)
            before=$(ls -1q "$LOCAL_TARGET_DIR"/*.csv 2>/dev/null | wc -l | xargs)

            # scp errors to a temp file instead of /dev/null so we can diagnose
            err_file=$(mktemp)
            scp -q -o StrictHostKeyChecking=no -o ConnectTimeout=10 \
                "root@$IP:$REMOTE_PATH" "$LOCAL_TARGET_DIR/" 2>"$err_file"
            rc=$?

            after=$(ls -1q "$LOCAL_TARGET_DIR"/*.csv 2>/dev/null | wc -l | xargs)
            delta=$((after - before))

            if [ "$delta" -gt 0 ]; then
                echo "  [$CASE/$SUBDIR] +$delta CSV"
                case_downloads=$((case_downloads + delta))
                TOTAL_DOWNLOADED=$((TOTAL_DOWNLOADED + delta))
            else
                # Diagnose non-zero exit codes that are not just "no files"
                if [ "$rc" -ne 0 ] && [ -s "$err_file" ]; then
                    err_msg=$(head -1 "$err_file")
                    case "$err_msg" in
                        *"No such file"*|*"no matches"*|*"matches no file"*)
                            # Normal: remote dir has no CSVs; silent skip
                            TOTAL_SKIPPED=$((TOTAL_SKIPPED + 1))
                            ;;
                        *)
                            # Unexpected error: surface it
                            echo "  [$CASE/$SUBDIR] ERROR: $err_msg"
                            ;;
                    esac
                fi
            fi
            rm -f "$err_file"
        done

        if [ "$case_downloads" -eq 0 ]; then
            echo "  [$CASE] nothing new from this node"
        fi
    done
done

echo ""
echo "===================================================="
echo "RQ1 DATA COLLECTION COMPLETE"
echo "  Total CSVs downloaded : $TOTAL_DOWNLOADED"
echo "  Empty remote dirs     : $TOTAL_SKIPPED"
echo "===================================================="
echo ""
echo "Per-SPL totals on local disk:"
for CASE in "${CASES[@]}"; do
    case_total=0
    for SUBDIR in "${SUBDIRS_TO_CHECK[@]}"; do
        c=$(ls -1q "$LOCAL_CASES_ROOT/$CASE/$SUBDIR"/*.csv 2>/dev/null | wc -l | xargs)
        case_total=$((case_total + c))
    done
    printf "  %-24s %5d CSV(s)\n" "$CASE" "$case_total"
done