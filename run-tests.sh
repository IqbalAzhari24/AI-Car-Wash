#!/bin/bash
# Run integration tests — called from Windows via: wsl bash /mnt/a/AI\ Car\ Wash/run-tests.sh
set -e
cd "/mnt/a/AI Car Wash/car-wash-backend"

echo "=== Checking Docker availability ==="
docker info --format "Docker version: {{.ServerVersion}}" 2>&1 || echo "Docker not accessible — tests will fail"

echo ""
echo "=== Running mvn test (excluding Selenium tests) ==="
mvn test \
  -Dnet.bytebuddy.experimental=true \
  -Dexclude="**/*SeleniumTest.java" \
  2>&1 | tee /mnt/a/AI\ Car\ Wash/test-output.txt

echo ""
echo "=== Test run complete. Output saved to test-output.txt ==="
