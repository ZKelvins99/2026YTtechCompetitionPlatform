#!/bin/sh
# ./ry.sh start|stop|restart|status
AppName=ruoyi-admin.jar
JAVA_BIN=/usr/local/jdk17/bin/java
CONFIG_OPTS="--spring.config.additional-location=/opt/ruoyi/config/application-prod.yml"
JVM_OPTS="-Djava.awt.headless=true -Dname=$AppName -Duser.timezone=Asia/Shanghai -Xms512m -Xmx1024m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=512m"
APP_HOME=$(cd "$(dirname "$0")" && pwd)
LOG_DIR=$APP_HOME/logs
mkdir -p "$LOG_DIR"

cd "$APP_HOME" || exit 1

start() {
  PID=$(ps -ef | grep java | grep "$AppName" | grep -v grep | awk '{print $2}')
  if [ -n "$PID" ]; then
    echo "$AppName is running (pid:$PID)"
  else
    nohup "$JAVA_BIN" $JVM_OPTS -jar "$AppName" $CONFIG_OPTS > "$LOG_DIR/stdout.log" 2>&1 &
    echo "Start $AppName success, log: $LOG_DIR/stdout.log"
  fi
}

stop() {
  PID=$(ps -ef | grep java | grep "$AppName" | grep -v grep | awk '{print $2}')
  if [ -n "$PID" ]; then
    kill -TERM "$PID"
    echo "Stop $AppName (pid:$PID)"
    i=0
    while [ $i -lt 30 ]; do
      if ! ps -p "$PID" > /dev/null 2>&1; then
        break
      fi
      i=$((i + 1))
      sleep 1
    done
  else
    echo "$AppName already stopped"
  fi
}

wait_ready() {
  echo "Waiting for backend on :8080..."
  i=0
  while [ $i -lt 60 ]; do
    if curl -sf -o /dev/null "http://127.0.0.1:8080/captchaImage" 2>/dev/null; then
      echo "Backend is ready."
      return 0
    fi
    i=$((i + 1))
    sleep 1
  done
  echo "WARN: Backend not ready within 60s, check $LOG_DIR/stdout.log"
  return 1
}

status() {
  PID=$(ps -ef | grep java | grep "$AppName" | grep -v grep | awk '{print $2}')
  if [ -n "$PID" ]; then
    echo "$AppName is running (pid:$PID)"
  else
    echo "$AppName is not running"
  fi
}

case "$1" in
  start) start ;;
  stop) stop ;;
  restart) stop; start; wait_ready ;;
  status) status ;;
  *) echo "Usage: $0 {start|stop|restart|status}"; exit 1 ;;
esac
