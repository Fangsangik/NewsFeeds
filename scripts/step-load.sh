#!/usr/bin/env bash
# DM step-load: thread를 단계적으로 올려가며 백엔드 plateau 측정.
# 사용법: ./scripts/step-load.sh [scenario]
#   scenario: dm (기본) — POST /messages + GET conversation
set -u

JMX_DIR=/Users/hwangsang-ik/IdeaProjects/newsfeed/loadtest/jmx
OUT_BASE=/Users/hwangsang-ik/IdeaProjects/newsfeed/loadtest/results/step
SCENARIO=${1:-dm}
JMX_NAME=${SCENARIO}.jmx

mkdir -p $OUT_BASE
rm -rf $OUT_BASE/*

# Reuse the already-seeded receiver + first token from users.csv.
TOK=$(awk -F, 'NR==2 {print $1}' /Users/hwangsang-ik/IdeaProjects/newsfeed/loadtest/jmx/users.csv)
RID=$(cat /tmp/_jmrid)
if [[ -z "$TOK" || -z "$RID" ]]; then
  echo "FATAL: token or receiver id missing. Re-seed first." >&2
  exit 1
fi

STEPS=(1000 500 200 100 50)   # max-first then step-down (recovery)
DUR=120
RAMP=30

echo "=== step-load: scenario=$SCENARIO  receiver=$RID  steps=${STEPS[*]}"
echo

for T in "${STEPS[@]}"; do
  STAMP=$(printf "%04d" $T)
  OUT=$OUT_BASE/t$STAMP
  mkdir -p $OUT
  echo "── step T=$T (duration=${DUR}s ramp=${RAMP}s)"

  # Snapshot docker stats before / during for the app + db
  ( docker stats --no-stream --format "{{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}" nf-app nf-mysql nf-redis > $OUT/stats.before.txt 2>&1 ) &

  docker run --rm \
    -v $JMX_DIR:/jmx \
    -v $OUT:/results \
    --network newsfeed_default \
    justb4/jmeter:5.5 \
    -n -t /jmx/$JMX_NAME -l /results/result.jtl \
    -JTHREADS=$T -JDURATION=$DUR -JRAMP=$RAMP \
    -JTOKEN="$TOK" -JRECEIVER_ID=$RID \
    -Jjmeter.save.saveservice.output_format=csv \
    > $OUT/jmeter.log 2>&1 &
  JM_PID=$!

  # Sample stats once mid-run
  sleep $((RAMP + DUR/2))
  docker stats --no-stream --format "{{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}" nf-app nf-mysql nf-redis > $OUT/stats.mid.txt 2>&1

  wait $JM_PID
  echo "  jmeter exit=$?"
  echo
done

echo "=== summary"
python3 - <<'PY'
import csv, statistics, collections, glob, os, re
base='/Users/hwangsang-ik/IdeaProjects/newsfeed/loadtest/results/step'
rows=[]
for d in sorted(glob.glob(f'{base}/t*')):
    T=int(re.search(r't(\d+)', d).group(1))
    jtl=f'{d}/result.jtl'
    if not os.path.exists(jtl):
        rows.append((T,0,0,0,0,0,0,0)); continue
    samples=collections.defaultdict(list); codes=collections.Counter()
    ts_min=ts_max=None; total=0
    with open(jtl) as f:
        for row in csv.DictReader(f):
            try:
                lbl=row['label']; t=int(row['timeStamp']); e=int(row['elapsed'])
            except: continue
            samples[lbl].append(e); codes[row['responseCode']]+=1
            ts_min=t if ts_min is None else min(ts_min,t)
            ts_max=t if ts_max is None else max(ts_max,t)
            total+=1
    dur=(ts_max-ts_min)/1000.0 if ts_min else 0
    all_x=[e for lst in samples.values() for e in lst]
    if not all_x:
        rows.append((T,0,0,0,0,0,0,0)); continue
    all_x.sort()
    p=lambda q: all_x[int(round(q*(len(all_x)-1)))]
    bad=sum(v for k,v in codes.items() if not(k.isdigit() and 200<=int(k)<300))
    err=100*bad/total if total else 0
    tps=total/dur if dur else 0
    rows.append((T,total,tps,statistics.mean(all_x),p(0.50),p(0.95),p(0.99),err))
print(f"  {'T':>5} {'samples':>8} {'TPS':>7} {'avg':>6} {'p50':>5} {'p95':>6} {'p99':>6} {'err%':>5}")
print("  "+("-"*60))
for T,total,tps,avg,p50,p95,p99,err in rows:
    print(f"  {T:>5} {total:>8} {tps:>7.1f} {avg:>6.0f} {p50:>5.0f} {p95:>6.0f} {p99:>6.0f} {err:>5.1f}")
PY

echo
echo "=== docker stats (mid-run snapshots)"
for d in $OUT_BASE/t*; do
  T=$(basename $d | sed 's/t//')
  echo "T=$T:"
  cat $d/stats.mid.txt 2>/dev/null | sed 's/^/  /'
done
