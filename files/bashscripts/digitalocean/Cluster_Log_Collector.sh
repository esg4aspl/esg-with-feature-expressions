#!/bin/bash

# ============================================================
# CLUSTER LOG COLLECTION
#
# Downloads logs from each DigitalOcean node, tags them with
# the shard range from ips.txt, and optionally compresses each
# node's logs into a tar.gz.
#
# ips.txt format (two whitespace-separated columns):
#   IP               TAG
# Example:
#   188.166.89.99    s00-07
#   164.92.215.173   s16-23
# Lines starting with # and blank lines are ignored.
#
# Environment variables (optional):
#   COMPRESS=1   create tar.gz per node (default)
#   COMPRESS=0   keep raw files only
#   KEEP_RAW=1   keep raw directory even after creating tar.gz
#   KEEP_RAW=0   delete raw directory after tar.gz (default)
#
# Remote sources collected:
#   /root/main_run*.log
#   /root/esg-with-feature-expressions/files/logs/ (recursive)
# ============================================================

set -u

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FILES_DIR="$(dirname "$(dirname "$SCRIPT_DIR")")"
IPS_FILE="$SCRIPT_DIR/ips.txt"
LOCAL_DEST="$FILES_DIR/logs/Cluster_Logs"

COMPRESS="${COMPRESS:-1}"
KEEP_RAW="${KEEP_RAW:-0}"

if [ ! -f "$IPS_FILE" ]; then
    echo "ERROR: ips.txt not found at $IPS_FILE"
    exit 1
fi

tr -d '\r' < "$IPS_FILE" > "${IPS_FILE}_clean" && mv "${IPS_FILE}_clean" "$IPS_FILE"

IPS=()
TAGS=()
while IFS= read -r line || [ -n "$line" ]; do
    case "$line" in
        '#'*|'') continue ;;
    esac
    ip=$(echo "$line" | awk '{print $1}')
    tag=$(echo "$line" | awk '{print $2}')
    if [ -z "$ip" ]; then continue; fi
    if [ -z "$tag" ]; then
        tag="node$((${#IPS[@]}))"
    fi
    IPS+=("$ip")
    TAGS+=("$tag")
done < "$IPS_FILE"

if [ "${#IPS[@]}" -eq 0 ]; then
    echo "ERROR: no IPs parsed from $IPS_FILE"
    exit 1
fi

mkdir -p "$LOCAL_DEST"

echo "=================================================="
echo "DOWNLOADING LOGS FROM CLUSTER NODES"
echo "Destination: $LOCAL_DEST"
echo "Nodes: ${#IPS[@]}"
if [ "$COMPRESS" -eq 1 ]; then
    echo "Compress  : yes  (KEEP_RAW=$KEEP_RAW)"
else
    echo "Compress  : no"
fi
echo "=================================================="

SUCCESS_NODES=0
FAILED_NODES=0

for i in "${!IPS[@]}"; do
    IP="${IPS[$i]}"
    TAG="${TAGS[$i]}"
    NODE_DIR="$LOCAL_DEST/${TAG}_${IP}"
    mkdir -p "$NODE_DIR"

    echo ""
    echo "--------------------------------------------------"
    echo "Node: $IP  (tag: $TAG)"
    echo "--------------------------------------------------"

    node_ok=0

    # 1) Orchestrator logs: /root/main_run*.log
    if ssh -q -o StrictHostKeyChecking=no -o ConnectTimeout=10 \
           "root@$IP" 'ls /root/main_run*.log 2>/dev/null | head -1' \
           | grep -q 'log'; then
        if scp -q -o StrictHostKeyChecking=no -o ConnectTimeout=10 \
               "root@$IP:/root/main_run*.log" "$NODE_DIR/" 2>/dev/null; then
            for f in "$NODE_DIR"/main_run*.log; do
                [ -f "$f" ] || continue
                base=$(basename "$f")
                mv "$f" "$NODE_DIR/${TAG}_${base}"
            done
            count=$(ls -1 "$NODE_DIR"/${TAG}_main_run*.log 2>/dev/null | wc -l | xargs)
            echo "  [OK] orchestrator logs : $count file(s)"
            node_ok=1
        else
            echo "  [!!] orchestrator logs : scp failed"
        fi
    else
        echo "  [--] orchestrator logs : none on remote"
    fi

    # 2) Application logs: /root/esg-with-feature-expressions/files/logs/
    REMOTE_LOGS="/root/esg-with-feature-expressions/files/logs"
    if ssh -q -o StrictHostKeyChecking=no -o ConnectTimeout=10 \
           "root@$IP" "test -d $REMOTE_LOGS && ls -A $REMOTE_LOGS 2>/dev/null | head -1" \
           | grep -q '.'; then
        APP_LOG_DIR="$NODE_DIR/app_logs"
        mkdir -p "$APP_LOG_DIR"
        if scp -q -r -o StrictHostKeyChecking=no -o ConnectTimeout=10 \
               "root@$IP:$REMOTE_LOGS/." "$APP_LOG_DIR/" 2>/dev/null; then
            # Tag top-level files and directories
            for item in "$APP_LOG_DIR"/*; do
                [ -e "$item" ] || continue
                base=$(basename "$item")
                case "$base" in
                    ${TAG}_*) continue ;;
                esac
                mv "$item" "$APP_LOG_DIR/${TAG}_${base}"
            done
            count=$(find "$APP_LOG_DIR" -type f 2>/dev/null | wc -l | xargs)
            echo "  [OK] application logs  : $count file(s) under app_logs/"
            node_ok=1
        else
            echo "  [!!] application logs  : scp failed"
        fi
    else
        echo "  [--] application logs  : none on remote"
    fi

    if [ "$node_ok" -eq 1 ]; then
        SUCCESS_NODES=$((SUCCESS_NODES + 1))
    else
        FAILED_NODES=$((FAILED_NODES + 1))
    fi

    # 3) Compression
    if [ "$COMPRESS" -eq 1 ]; then
        file_count=$(find "$NODE_DIR" -type f 2>/dev/null | wc -l | xargs)
        if [ "$file_count" -gt 0 ]; then
            TARBALL="$LOCAL_DEST/${TAG}_${IP}.tar.gz"
            if tar -czf "$TARBALL" -C "$LOCAL_DEST" "${TAG}_${IP}" 2>/dev/null; then
                raw_size=$(du -sh "$NODE_DIR" 2>/dev/null | awk '{print $1}')
                gz_size=$(du -sh "$TARBALL" 2>/dev/null | awk '{print $1}')
                echo "  [OK] compressed        : $raw_size -> $gz_size"
                if [ "$KEEP_RAW" -eq 0 ]; then
                    rm -rf "$NODE_DIR"
                fi
            else
                echo "  [!!] tar.gz failed; keeping raw directory"
            fi
        else
            echo "  [--] nothing to compress"
            rmdir "$NODE_DIR" 2>/dev/null || true
        fi
    fi
done


echo ""
echo "=================================================="
echo "LOG COLLECTION COMPLETE"
echo "  Nodes with data : $SUCCESS_NODES"
echo "  Nodes failed    : $FAILED_NODES"
echo "  Saved under     : $LOCAL_DEST"
echo "=================================================="

echo ""
echo "Per-node summary:"
for i in "${!IPS[@]}"; do
    TAG="${TAGS[$i]}"
    IP="${IPS[$i]}"

    if [ "$COMPRESS" -eq 1 ] && [ "$KEEP_RAW" -eq 0 ]; then
        TARBALL="$LOCAL_DEST/${TAG}_${IP}.tar.gz"
        if [ -f "$TARBALL" ]; then
            size=$(du -sh "$TARBALL" 2>/dev/null | awk '{print $1}')
            printf "  %-10s %-18s tar.gz  %s\n" "$TAG" "$IP" "$size"
        else
            printf "  %-10s %-18s [missing]\n" "$TAG" "$IP"
        fi
    else
        NODE_DIR="$LOCAL_DEST/${TAG}_${IP}"
        if [ -d "$NODE_DIR" ]; then
            files=$(find "$NODE_DIR" -type f 2>/dev/null | wc -l | xargs)
            size=$(du -sh "$NODE_DIR" 2>/dev/null | awk '{print $1}')
            printf "  %-10s %-18s %-6s file(s)  %s\n" "$TAG" "$IP" "$files" "$size"
        else
            printf "  %-10s %-18s [missing]\n" "$TAG" "$IP"
        fi
    fi
done