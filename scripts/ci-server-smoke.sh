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

# Forge 1.21.x userdev treats the compiled classes and processed resources as
# separate mod files. That cannot represent BuildCraft's single jar containing
# nine [[mods]] entries: classes/main discovers only part of the mod set while
# resources/main has metadata but no classes. The distributable 1.21.x Forge
# jar uses Mojmap and can be loaded directly by the userdev server, so smoke-test
# the production artifact instead of the broken exploded-mod representation.
use_production_jar=false
case "$target" in
  1.21.*-forge) use_production_jar=true ;;
esac

production_mod=""
if [[ "$use_production_jar" == true ]]; then
  jar_dir="versions/${target}/build/libs"
  if [[ ! -d "$jar_dir" ]]; then
    echo "Production jar directory does not exist: $jar_dir" >&2
    echo "Run buildAndCollect (or :${target}:build) before the smoke test." >&2
    exit 2
  fi

  mapfile -t production_jars < <(
    find "$jar_dir" -maxdepth 1 -type f -name '*.jar' \
      ! -name '*-sources.jar' ! -name '*-javadoc.jar' | sort
  )
  if (( ${#production_jars[@]} != 1 )); then
    echo "Expected exactly one production jar in $jar_dir, found ${#production_jars[@]}." >&2
    printf '  %s\n' "${production_jars[@]}" >&2
    exit 2
  fi

  mkdir -p "${run_dir}/mods"
  production_mod="${run_dir}/mods/$(basename "${production_jars[0]}")"
  rm -f "${run_dir}/mods/BuildCraft"*.jar
  cp "${production_jars[0]}" "$production_mod"
  echo "Forge 1.21.x production-jar smoke mode: $production_mod"
fi

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
gradle_args=(
  --no-daemon
  --console=plain
  --stacktrace
  -Pci_runtime_profile="${runtime_profile}"
)
if [[ "$use_production_jar" == true ]]; then
  gradle_args+=(-Puse_production_jar_runtime=true)
fi
setsid ./gradlew "${gradle_args[@]}" ":${target}:runServer" > "$server_log" 2>&1 &
server_pid=$!

cleanup() {
  if kill -0 "$server_pid" 2>/dev/null; then
    echo "Stopping CI server process group."
    kill -TERM -- "-$server_pid" 2>/dev/null || kill -TERM "$server_pid" 2>/dev/null || true

    for _ in $(seq 1 10); do
      if ! kill -0 "$server_pid" 2>/dev/null; then
        break
      fi
      sleep 1
    done


    if kill -0 "$server_pid" 2>/dev/null; then
      kill -KILL -- "-$server_pid" 2>/dev/null || kill -KILL "$server_pid" 2>/dev/null || true
    fi
  fi

  if [[ -n "$production_mod" ]]; then
    rm -f "$production_mod"
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
