#!/bin/bash
echo "=== User and environment ==="
echo "whoami: $(whoami)"
echo "HOME: $HOME"
echo "USER: $USER"

echo ""
echo "=== Maven and Java location ==="
which mvn 2>/dev/null && echo "mvn type: $(file $(which mvn) 2>/dev/null || echo 'unknown')" || echo "mvn not in PATH"
java -version 2>&1 | head -3

echo ""
echo "=== Find dockerd ==="
for p in $(echo $PATH | tr ':' ' ') /usr/bin /usr/local/bin /Docker/host/bin /sbin /usr/sbin; do
    if [ -f "$p/dockerd" ]; then
        echo "Found dockerd at: $p/dockerd"
        "$p/dockerd" --version 2>/dev/null || echo "  (version cmd failed)"
    fi
done

echo ""
echo "=== Existing sockets and processes ==="
ls -la /var/run/docker*.sock /tmp/d.sock 2>/dev/null || true
pgrep -a dockerd 2>/dev/null || echo "No dockerd running"
pgrep -a containerd 2>/dev/null | head -3 || echo "No containerd"

echo ""
echo "=== Test docker-cli.sock with various paths ==="
if [ -S /var/run/docker-cli.sock ]; then
    for path in /_ping /ping /version /info /v1.40/info /v1.44/info /v1.47/info; do
        CODE=$(curl -s -o /tmp/curlout.txt -w "%{http_code}" --connect-timeout 2 --unix-socket /var/run/docker-cli.sock "http://localhost${path}" 2>/dev/null)
        BODY=$(cat /tmp/curlout.txt 2>/dev/null | head -c 100)
        echo "  docker-cli.sock${path} -> HTTP $CODE | $BODY"
    done
else
    echo "/var/run/docker-cli.sock does not exist"
fi

echo ""
echo "=== Test /var/run/docker.sock with various paths ==="
for path in /_ping /ping /version /info /v1.46/info; do
    CODE=$(curl -s -o /tmp/curlout.txt -w "%{http_code}" --connect-timeout 2 --unix-socket /var/run/docker.sock "http://localhost${path}" 2>/dev/null)
    BODY=$(cat /tmp/curlout.txt 2>/dev/null | head -c 150)
    echo "  docker.sock${path} -> HTTP $CODE | $BODY"
done

echo ""
echo "=== Try starting dockerd on /tmp/d.sock ==="
# Find dockerd
DOCKERD=$(command -v dockerd 2>/dev/null)
if [ -z "$DOCKERD" ]; then
    for d in /Docker/host/bin/dockerd /usr/bin/dockerd; do
        [ -f "$d" ] && DOCKERD="$d" && break
    done
fi

if [ -n "$DOCKERD" ]; then
    echo "Using dockerd: $DOCKERD"
    rm -f /tmp/d.sock
    "$DOCKERD" --host unix:///tmp/d.sock --data-root /tmp/dkdata --pidfile /tmp/dkpid 2>/tmp/dklog &
    DPID=$!
    echo "dockerd PID: $DPID, waiting 8s..."
    sleep 8
    if [ -S /tmp/d.sock ]; then
        echo "SUCCESS: /tmp/d.sock created!"
        curl -s --unix-socket /tmp/d.sock http://localhost/version 2>/dev/null | python3 -c "import sys,json; d=json.load(sys.stdin); print('Docker version:', d.get('Version','?'))" 2>/dev/null || echo "curl test failed"
    else
        echo "FAILED: /tmp/d.sock not created"
        echo "--- dockerd log ---"
        cat /tmp/dklog 2>/dev/null | head -30
    fi
else
    echo "dockerd binary not found anywhere"
fi

echo ""
echo "=== Check netsh portproxy from WSL2 ==="
GW=$(ip route show default | cut -d' ' -f3)
echo "Gateway: $GW"
nc -z -w2 "$GW" 2375 2>/dev/null && echo "2375 OPEN" || echo "2375 CLOSED"
nc -z -w2 "$GW" 2376 2>/dev/null && echo "2376 OPEN" || echo "2376 CLOSED"

echo ""
echo "=== DIAGNOSIS COMPLETE ==="
