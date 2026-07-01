$ErrorActionPreference = "Continue"
wsl bash -c "cd '/mnt/a/AI Car Wash/car-wash-backend' && mvn test -Dnet.bytebuddy.experimental=true -Dexclude='**/*SeleniumTest.java' 2>&1 | tee /mnt/a/AI\ Car\ Wash/test-output.txt"
