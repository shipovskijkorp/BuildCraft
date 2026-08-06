#!/usr/bin/env bash
set -Eeuo pipefail

startup_timeout="${SERVER_STARTUP_TIMEOUT:-360}"
runtime_profile="${SERVER_RUNTIME_PROFILE:-base}"
active_target="$(sed -nE 's/.*stonecutter active "([^"]+)".*/\1/p' stonecutter.gradle.kts | head -n 1)"
target="${STONECUTTER_TARGET:-$active_target}"
if [[ -z "$target" ]]; then
  echo "Unable to determine the active Stonecutter target" >&2
  exit 2
fi
server_log="${SERVER_LOG_FILE:-ci-server-${target}-${runtime_profile}.log}"
run_dir="run/${target}"
latest_log="${run_dir}/logs/latest.log"

mkdir -p "$run_dir"
printf 'eula=true\n' > "${run_dir}/eula.txt"
cat > "${run_dir}/server.properties" <<'PROPERTIES'
online-mode=false
server-ip=127.0.0.1
server-port=25565
level-name=ci-world
motd=BuildCraft CI server smoke test
enable-command-block=false
spawn-protection=0
max-tick-time=-1
PROPERTIES

: > "$server_log"
rm -f "$latest_log"

echo "Starting dedicated server smoke test (target: ${target}, profile: ${runtime_profile}, timeout: ${startup_timeout}s)."
setsid ./gradlew --no-daemon --console=plain --stacktrace \
  -Pci_runtime_profile="${runtime_profile}" ":${target}:runServer" > "$server_log" 2>&1 &
server_pid=$!

cleanup() {
  if kill -0 "$server_pid" 2>/dev/null; then
    echo "Stopping CI server process group."
    kill -TERM -- "-$server_pid" 2>/dev/null || kill -TERM "$server_pid" 2>/dev/null || true

    for _ in $(seq 1 10); do
      if ! kill -0 "$server_pid" 2>/dev/null; then
        return
      fi
      sleep 1
    done

    kill -KILL -- "-$server_pid" 2>/dev/null || kill -KILL "$server_pid" 2>/dev/null || true
  fi
}
trap cleanup EXIT

show_log_tail() {
  echo "--- ${server_log} (last 200 lines) ---"
  tail -n 200 "$server_log" 2>/dev/null || true

  if [[ -f "$latest_log" ]]; then
    echo "--- ${latest_log} (last 200 lines) ---"
    tail -n 200 "$latest_log" 2>/dev/null || true
  fi
}

success_regex='Done \([0-9.,]+s\)! For help, type "help"|Done \([0-9.,]+s\)!'
fatal_regex='Failed to start the minecraft server|Exception in server tick loop|Loading errors encountered|Mod loading has failed|A fatal error has been detected'
deadline=$((SECONDS + startup_timeout))

while (( SECONDS < deadline )); do
  log_files=("$server_log")
  [[ -f "$latest_log" ]] && log_files+=("$latest_log")

  if grep -Eiq "$success_regex" "${log_files[@]}" 2>/dev/null; then
    echo "Dedicated server reached the ready state successfully."
    exit 0
  fi

  if grep -Eiq "$fatal_regex" "${log_files[@]}" 2>/dev/null; then
    echo "Dedicated server reported a fatal startup error."
    show_log_tail
    exit 1
  fi

  if ! kill -0 "$server_pid" 2>/dev/null; then
    set +e
    wait "$server_pid"
    status=$?
    set -e
    echo "Dedicated server process exited before becoming ready (status $status)."
    show_log_tail
    exit 1
  fi

  sleep 5
done

echo "Dedicated server did not reach the ready state within ${startup_timeout}s."
show_log_tail
exit 1
