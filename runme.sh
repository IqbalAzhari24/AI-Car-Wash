#!/bin/bash
set -e

echo "=== Step 1: Find and start dockerd ==="
DOCKERD=$(which dockerd 2>/dev/null || echo "")
if [ -z "$DOCKERD" ]; then
    DOCKERD=$(ls /Docker/host/bin/dockerd 2>/dev/null || echo "")
fi
echo "dockerd binary: $DOCKERD"

if [ -n "$DOCKERD" ] && [ -f "$DOCKERD" ]; then
    rm -f /tmp/d.sock
    nohup "$DOCKERD" --host unix:///tmp/d.sock --data-root /tmp/docker-data > /tmp/dockerd.log 2>&1 &
    echo "dockerd started, PID=$!, waiting 8s..."
    sleep 8
    echo "--- dockerd log ---"
    tail -15 /tmp/dockerd.log 2>/dev/null || true
    echo "--- socket check ---"
    ls -la /tmp/d.sock 2>/dev/null && echo "SOCKET_OK" || echo "SOCKET_MISSING"
else
    echo "dockerd not found, skipping"
fi

echo ""
echo "=== Step 2: Check /var/run/docker.sock and alternatives ==="
ls -la /var/run/docker.sock /var/run/docker-cli.sock /tmp/d.sock 2>/dev/null || true

echo ""
echo "=== Step 3: Determine working DOCKER_HOST ==="
DOCKER_SOCKET=""
for SOCK in /tmp/d.sock /var/run/docker.sock; do
    if [ -S "$SOCK" ]; then
        echo "Testing $SOCK ..."
        RESULT=$(curl -s --unix-socket "$SOCK" http://localhost/version 2>/dev/null | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('Version',''))" 2>/dev/null || echo "")
        if [ -n "$RESULT" ]; then
            echo "  Version: $RESULT - WORKS"
            DOCKER_SOCKET="$SOCK"
            break
        else
            echo "  No version response"
        fi
    fi
done

echo "Chosen socket: $DOCKER_SOCKET"

echo ""
echo "=== Step 4: Write testcontainers.properties ==="
TCPROPS=/home/iqbal24/.testcontainers.properties
if [ -n "$DOCKER_SOCKET" ]; then
    echo "docker.host=unix://${DOCKER_SOCKET}" > "$TCPROPS"
else
    echo "docker.host=unix:///var/run/docker.sock" > "$TCPROPS"
fi
cat "$TCPROPS"

echo ""
echo "=== Step 5: Set DOCKER_HOST and run Maven ==="
if [ -n "$DOCKER_SOCKET" ]; then
    export DOCKER_HOST="unix://${DOCKER_SOCKET}"
fi
echo "DOCKER_HOST=$DOCKER_HOST"
cd "/mnt/a/AI Car Wash/car-wash-backend"
mvn test \
    -Dnet.bytebuddy.experimental=true \
    -Ddocker.host="${DOCKER_HOST:-unix:///var/run/docker.sock}" \
    2>&1 | tee "/mnt/a/AI Car Wash/test-output.txt"
echo "MAVEN_EXIT: $?"
