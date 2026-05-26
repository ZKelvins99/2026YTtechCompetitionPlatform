#!/bin/bash
set -e
RY=/opt/ruoyi/backend/ry.sh
if ! grep -q java.awt.headless "$RY"; then
  perl -pi -e 's/-Dname=/-Djava.awt.headless=true -Dname=/' "$RY"
fi
grep '^JVM_OPTS=' "$RY"
cd /opt/ruoyi/backend
./ry.sh restart
sleep 15
curl -s -w "\nHTTP:%{http_code}\n" http://127.0.0.1/prod-api/captchaImage | head -c 600
