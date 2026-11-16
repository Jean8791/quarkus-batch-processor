#!/usr/bin/env bash
# Runs the whole benchmark and writes the results as CSV.
#   ./benchmark/run.sh               default grid: 100k and 1M rows, -Xmx256m and -Xmx512m, 5 repetitions
#   ./benchmark/run.sh --with-5m     also 5M rows in the "read" scenario (slow)
#   ./benchmark/run.sh --quick       smoke test: 100k rows, -Xmx256m, 2 repetitions; output is not committed
# Needs: Docker, Java 17, python3. Installs the library into the local Maven repository (~/.m2).
set -euo pipefail

cd "$(dirname "$0")"
root="$(cd .. && pwd)"

reps=5
sizes=(100000 1000000)
heaps=(256m 512m)
with_5m=false
prefix=""
for arg in "$@"; do
  case "$arg" in
    --with-5m) with_5m=true ;;
    --quick) reps=2; sizes=(100000); heaps=(256m); prefix="quick-" ;;
    *) echo "unknown option: $arg" >&2; exit 1 ;;
  esac
done

image="postgres:16-alpine"
max_rows=1000000
$with_5m && max_rows=5000000
results="results/${prefix}results.csv"
summary="results/${prefix}summary.csv"
environment="results/${prefix}environment.txt"

workdir="$(mktemp -d)"
container="bench-pg-$$"
cleanup() { docker rm -f "$container" >/dev/null 2>&1 || true; rm -rf "$workdir"; }
trap cleanup EXIT

echo "▶ building the library and the benchmark app"
(cd "$root" && ./mvnw -q install -DskipTests)
../mvnw -q package

echo "▶ starting PostgreSQL ($image)"
# Throwaway credentials: generated here, kept in a 600 file outside the repository.
envfile="$workdir/pg.env"
( umask 177; printf 'POSTGRES_USER=bench\nPOSTGRES_DB=bench\nPOSTGRES_PASSWORD=%s\n' "$(openssl rand -hex 16)" > "$envfile" )
docker run -d --name "$container" --env-file "$envfile" -p 127.0.0.1::5432 "$image" >/dev/null
port="$(docker port "$container" 5432/tcp | head -1 | sed 's/.*://')"
until docker exec "$container" pg_isready -U bench -d bench >/dev/null 2>&1; do sleep 1; done
sleep 2

psql() { docker exec -i "$container" psql -q -v ON_ERROR_STOP=1 -U bench -d bench "$@"; }

echo "▶ seeding $max_rows rows"
psql <<SQL
create table bench_row (
  id bigint primary key,
  label varchar(40) not null,
  description varchar(120) not null,
  amount numeric(12,2) not null,
  created_at timestamp not null
);
insert into bench_row
select g, 'label-' || g, repeat(md5(g::text), 3), (g % 100000) / 100.0, now() - (g % 1000000) * interval '1 second'
from generate_series(1, $max_rows) g;
analyze bench_row;
SQL

mkdir -p results
{
  echo "date_utc: $(date -u +%Y-%m-%dT%H:%MZ)"
  echo "cpu: $(LC_ALL=C lscpu | sed -n 's/^Model name:[[:space:]]*//p' | head -1) ($(nproc) logical cores)"
  echo "ram: $(free -h | awk '/^Mem/ {print $2}') total"
  echo "os: $(uname -sr)"
  echo "java: $(java -version 2>&1 | head -1)"
  echo "postgresql: $(psql -tA -c 'select version()')"
  echo "postgres_image: $image"
  echo "docker: $(docker --version)"
  echo "library: $(grep -m1 '<version>' "$root/pom.xml" | sed 's/[^0-9.]//g') (commit $(git -C "$root" rev-parse --short HEAD))"
  echo "jvm_flags: -Xmx per case; default GC (G1), no other flags"
  echo "chunk_size: 1000"
  echo "repetitions: $reps (first one per case is discarded as warm-up of the machine and the database)"
} > "$environment"

export QUARKUS_DATASOURCE_JDBC_URL="jdbc:postgresql://127.0.0.1:$port/bench"
set -a; source "$envfile"; set +a
export QUARKUS_DATASOURCE_PASSWORD="$POSTGRES_PASSWORD"
unset POSTGRES_PASSWORD

echo "scenario,impl,rows,xmx,rep,status,millis,peak_retained_mb,peak_used_mb,rows_processed,baseline_mb" > "$results"

run_case() {
  local scenario=$1 impl=$2 rows=$3 xmx=$4 rep=$5 out line code=0
  out="$workdir/out.txt"
  timeout 1800 java "-Xmx$xmx" -jar target/quarkus-app/quarkus-run.jar "$scenario" "$impl" "$rows" > "$out" 2>&1 || code=$?
  line="$(grep '^RESULT,' "$out" | tail -1 || true)"
  if [ -n "$line" ]; then
    IFS=, read -r _ _ _ _ status millis retained used processed baseline <<< "$line"
  else
    tail -15 "$out" >&2
    status="CRASH(exit=$code)"; millis=""; retained=""; used=""; processed=""; baseline=""
  fi
  echo "$scenario,$impl,$rows,$xmx,$rep,$status,$millis,$retained,$used,$processed,$baseline" >> "$results"
  printf '  %-5s %-8s %8s %5s rep %s -> %s %s ms, retained %s MB, used %s MB\n' \
    "$scenario" "$impl" "$rows" "$xmx" "$rep" "$status" "$millis" "$retained" "$used"
}

for scenario in read xlsx; do
  for rows in "${sizes[@]}" $($with_5m && [ "$scenario" = read ] && echo 5000000); do
    for xmx in "${heaps[@]}"; do
      impls=(naive library library-in-tx)
      [ "$scenario" = read ] && impls+=(library-tx)
      for impl in "${impls[@]}"; do
        for rep in $(seq 1 "$reps"); do
          run_case "$scenario" "$impl" "$rows" "$xmx" "$rep"
        done
      done
    done
  done
done

echo "▶ summary (repetition 1 of each case is discarded)"
python3 - "$results" "$summary" <<'PY'
import csv, statistics, sys
from collections import defaultdict

groups = defaultdict(list)
with open(sys.argv[1]) as f:
    for row in csv.DictReader(f):
        if row["rep"] != "1":
            groups[(row["scenario"], row["impl"], int(row["rows"]), row["xmx"])].append(row)

def median(rows, key):
    values = [int(r[key]) for r in rows if r["status"] == "OK" and r[key]]
    return int(statistics.median(values)) if values else ""

def bound(rows, key, fn):
    values = [int(r[key]) for r in rows if r["status"] == "OK" and r[key]]
    return fn(values) if values else ""

with open(sys.argv[2], "w", newline="") as f:
    out = csv.writer(f, lineterminator="\n")
    out.writerow(["scenario", "impl", "rows", "xmx", "runs", "ok", "oom", "other_failures",
                  "median_ms", "min_ms", "max_ms", "median_peak_retained_mb", "median_peak_used_mb", "median_baseline_mb"])
    for (scenario, impl, rows, xmx), items in sorted(groups.items()):
        ok = sum(r["status"] == "OK" for r in items)
        oom = sum(r["status"] == "OOM" for r in items)
        out.writerow([scenario, impl, rows, xmx, len(items), ok, oom, len(items) - ok - oom,
                      median(items, "millis"), bound(items, "millis", min), bound(items, "millis", max),
                      median(items, "peak_retained_mb"), median(items, "peak_used_mb"), median(items, "baseline_mb")])
PY
column -s, -t "$summary"
echo; echo "✔ done: $results, $summary, $environment"
