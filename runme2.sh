#!/bin/bash
# runme2.sh - Strip all non-essential headers to match Docker CLI exactly

echo "=== User info ==="
whoami && id

cat > /tmp/docker_proxy.py << 'PYEOF'
#!/usr/bin/env python3
import socket, threading, sys, re

UNIX_SOCKET = '/var/run/docker.sock'
TCP_PORT = 2377
LOG_FILE = '/tmp/docker_req.log'

def log(msg):
    try:
        with open(LOG_FILE, 'a') as f:
            f.write(msg + '\n')
    except:
        pass

# Headers to KEEP from docker-java (everything else is stripped)
KEEP_HEADERS = {b'content-type', b'content-length', b'transfer-encoding'}

def modify_request(data):
    """Strip request to minimal Docker CLI style."""
    try:
        sep = data.find(b'\r\n\r\n')
        if sep == -1:
            return data

        header_block = data[:sep]
        body = data[sep:]
        lines = header_block.split(b'\r\n')

        orig_all = '\n'.join(l.decode('utf-8', errors='replace') for l in lines[:20])
        log(f"\n=== ORIGINAL ===\n{orig_all}")

        new_lines = []
        for i, line in enumerate(lines):
            ll = line.lower().strip()
            if i == 0:
                # Request line: upgrade API version < 1.40 to 1.46
                def upgrade(m):
                    ver = float(m.group(1))
                    return b'/v1.46/' if ver < 1.40 else m.group(0)
                line = re.sub(rb'/v(1\.\d+)/', upgrade, line)
                new_lines.append(line)
            elif any(ll.startswith(h + b':') for h in KEEP_HEADERS):
                # Keep content headers
                new_lines.append(line)
            # Drop everything else (User-Agent, Host, Accept-Encoding,
            # Connection, x-tc-sid, Accept, etc.)

        # Add clean minimal headers
        new_lines.append(b'Host: localhost')
        new_lines.append(b'User-Agent: Docker-Client/29.5.3 (linux)')
        new_lines.append(b'Connection: close')

        result = b'\r\n'.join(new_lines) + body
        mod_all = '\n'.join(l.decode('utf-8', errors='replace') for l in new_lines[:15])
        log(f"=== MODIFIED ===\n{mod_all}\n")
        return result
    except Exception as e:
        log(f"modify error: {e}")
        return data

def read_http_request(conn):
    """Read a complete HTTP request from connection."""
    data = b''
    conn.settimeout(10)
    try:
        while b'\r\n\r\n' not in data:
            chunk = conn.recv(4096)
            if not chunk:
                return data
            data += chunk
        # If Content-Length header, read body
        cl_match = re.search(rb'content-length:\s*(\d+)', data, re.IGNORECASE)
        if cl_match:
            content_len = int(cl_match.group(1))
            headers_end = data.find(b'\r\n\r\n') + 4
            body_so_far = data[headers_end:]
            while len(body_so_far) < content_len:
                chunk = conn.recv(4096)
                if not chunk:
                    break
                data += chunk
                body_so_far = data[headers_end:]
    except:
        pass
    return data

def forward(src, dst):
    try:
        src.settimeout(30)
        while True:
            data = src.recv(65536)
            if not data:
                break
            dst.sendall(data)
    except:
        pass
    finally:
        try: src.close()
        except: pass
        try: dst.close()
        except: pass

def handle(client):
    try:
        # Handle each request on this connection
        while True:
            req_data = read_http_request(client)
            if not req_data:
                break
            modified = modify_request(req_data)

            unix_sock = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
            unix_sock.connect(UNIX_SOCKET)
            unix_sock.sendall(modified)

            # Read full response
            resp = b''
            unix_sock.settimeout(30)
            try:
                while True:
                    chunk = unix_sock.recv(65536)
                    if not chunk:
                        break
                    resp += chunk
                    # For Connection: close, server closes after response
            except:
                pass
            unix_sock.close()

            if not resp:
                break

            # Log first line of response
            first_line = resp.split(b'\r\n')[0].decode('utf-8', errors='replace')
            log(f"RESPONSE: {first_line}")

            client.sendall(resp)

            # Since we send Connection: close, server closes after each req
            # docker-java should open new connection for next request
            break
    except Exception as e:
        log(f"handle error: {e}")
    finally:
        try: client.close()
        except: pass

open(LOG_FILE, 'w').close()
server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
server.bind(('127.0.0.1', TCP_PORT))
server.listen(100)
print(f"Minimal-headers Docker proxy: 127.0.0.1:{TCP_PORT} -> {UNIX_SOCKET}", flush=True)
while True:
    client, _ = server.accept()
    threading.Thread(target=handle, args=(client,), daemon=True).start()
PYEOF

pkill -f "docker_proxy.py" 2>/dev/null; sleep 0.3
python3 /tmp/docker_proxy.py > /tmp/proxy.log 2>&1 &
PROXY_PID=$!
echo "Proxy PID: $PROXY_PID"
sleep 2

echo ""
echo "=== Verify proxy ==="
VER=$(curl -s --connect-timeout 3 "http://localhost:2377/version" 2>/dev/null \
    | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('Version','EMPTY'))" 2>/dev/null || echo "FAILED")
echo "Docker version via proxy: $VER"

if [ "$VER" = "FAILED" ] || [ -z "$VER" ]; then
    echo "ERROR: Proxy failed even for curl. Check proxy log:"
    cat /tmp/proxy.log
    kill $PROXY_PID 2>/dev/null
    DOCKER_HOST="unix:///var/run/docker.sock"
else
    echo "Proxy OK."
    DOCKER_HOST="tcp://localhost:2377"
fi

echo ""
echo "=== ~/.testcontainers.properties ==="
echo "docker.host=${DOCKER_HOST}" > /home/iqbal24/.testcontainers.properties
cat /home/iqbal24/.testcontainers.properties

echo ""
echo "=== Running Maven ==="
export DOCKER_HOST
export DOCKER_API_VERSION=1.46
cd "/mnt/a/AI Car Wash/car-wash-backend"
mvn test \
    -Dnet.bytebuddy.experimental=true \
    2>&1 | tee "/mnt/a/AI Car Wash/test-output.txt"
MAVEN_RESULT=${PIPESTATUS[0]}

echo ""
echo "=== Proxy request log ==="
cat /tmp/docker_req.log 2>/dev/null | head -80 || echo "(empty)"

echo ""
echo "MAVEN_EXIT: $MAVEN_RESULT"
kill $PROXY_PID 2>/dev/null || true
exit $MAVEN_RESULT
