#!/bin/sh
set -eu

PG_BIN=/usr/lib/postgresql/16/bin
PG_DATA=/app/pgdata

as_postgres() {
    setpriv --reuid=postgres --regid=postgres --init-groups "$@"
}

if [ ! -s "$PG_DATA/PG_VERSION" ]; then
    as_postgres "$PG_BIN/initdb" -D "$PG_DATA" --username=postgres \
        --auth-local=trust --auth-host=trust --no-instructions
fi

# PostgreSQL is reachable only from inside this container.
as_postgres "$PG_BIN/pg_ctl" -D "$PG_DATA" -l "$PG_DATA/server.log" \
    -o "-c listen_addresses=127.0.0.1 -c unix_socket_directories=/tmp -c shared_buffers=16MB -c max_connections=20" \
    -w start

if ! as_postgres "$PG_BIN/psql" -h 127.0.0.1 -U postgres -d postgres \
    -tAc "SELECT 1 FROM pg_database WHERE datname = 'courserec'" | grep -qx 1; then
    as_postgres "$PG_BIN/createdb" -h 127.0.0.1 -U postgres courserec
fi

app_pid=
stop_services() {
    trap - INT TERM
    if [ -n "$app_pid" ]; then
        kill "$app_pid" 2>/dev/null || true
        wait "$app_pid" 2>/dev/null || true
    fi
    as_postgres "$PG_BIN/pg_ctl" -D "$PG_DATA" -m fast -w stop || true
}

trap stop_services INT TERM
as_postgres java -jar /app/app.jar --server.port="${PORT:-10000}" &
app_pid=$!

set +e
wait "$app_pid"
exit_code=$?
set -e
stop_services
exit "$exit_code"
