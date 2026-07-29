#!/usr/bin/env bash
# k6 native 부하 실행기. VU 레벨을 단계별로 올려가며 dm.js 를 반복 실행하고,
# 레벨별 요약(JSON) + mid-run docker stats 를 모아 PERFORMANCE.md 붙여넣기용 표를 출력한다.
#
# 사용법:
#   ./scripts/k6-load.sh <round-name>      # 전체 레벨 (50 100 200 500 1000)
#   ./scripts/k6-load.sh smoke             # 짧은 스모크 (10 50, 각 15s)
#   STEPS="100 200" DUR_S=60 ./scripts/k6-load.sh r2-jwt
#
# env override: STEPS, DUR_S, RAMP_S, BASE
set -u

ROOT=/Users/hwangsang-ik/IdeaProjects/newsfeed
ROUND=${1:-baseline}
BASE=${BASE:-http://localhost:8080}
OUT=$ROOT/loadtest/results/k6/$ROUND
mkdir -p "$OUT"

if [[ "$ROUND" == "smoke" ]]; then
  STEPS_DEFAULT="10 50"; DUR_S=${DUR_S:-15}; RAMP_S=${RAMP_S:-5}
else
  STEPS_DEFAULT="50 100 200 500 1000"; DUR_S=${DUR_S:-120}; RAMP_S=${RAMP_S:-30}
fi
read -r -a STEPS <<< "${STEPS:-$STEPS_DEFAULT}"

PW="Aa12345!"; TS=$(date +%s)
SENDER="k6s.${TS}@e.com"; RECV="k6r.${TS}@e.com"

signup() {
  curl -s -X POST "$BASE/members/signup" -H 'Content-Type: application/json' \
    -d "{\"name\":\"$1\",\"email\":\"$2\",\"password\":\"$PW\",\"phoneNumber\":\"010-1\",\"address\":\"x\",\"age\":25,\"role\":\"USER\"}" >/dev/null
}
login_field() { # $1 email, $2 field(accessToken|id)
  curl -s -X POST "$BASE/auth/login" -H 'Content-Type: application/json' \
    -d "{\"email\":\"$1\",\"password\":\"$PW\"}" \
    | python3 -c "import json,sys;print(json.load(sys.stdin)['data']['$2'])"
}

echo "=== k6-load: round=$ROUND  steps=${STEPS[*]}  dur=${DUR_S}s ramp=${RAMP_S}s"
signup "K6Send" "$SENDER"
signup "K6Recv" "$RECV"
TOKEN=$(login_field "$SENDER" accessToken)
PEER=$(login_field "$RECV" id)
if [[ -z "$TOKEN" || -z "$PEER" ]]; then
  echo "FATAL: 토큰/peer 시드 실패 (TOKEN='$TOKEN' PEER='$PEER')" >&2; exit 1
fi
echo "seeded: sender token ok, receiver id=$PEER"
echo

for V in "${STEPS[@]}"; do
  SO="$OUT/vus_${V}.json"
  # mid-run docker stats 스냅샷 (ramp 끝나고 duration 절반 지점)
  ( sleep $((RAMP_S + DUR_S/2)); \
    docker stats --no-stream --format "{{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}" nf-app nf-mysql nf-redis \
      > "$OUT/stats_${V}.txt" 2>&1 ) &
  echo "── VUS=$V 실행중..."
  TOKEN="$TOKEN" PEER="$PEER" BASE="$BASE" VUS="$V" DURATION="${DUR_S}s" RAMP="${RAMP_S}s" SUMMARY_OUT="$SO" \
    k6 run --tag round="$ROUND" --out influxdb=http://localhost:8086/k6 "$ROOT/loadtest/k6/dm.js" \
    > "$OUT/k6_${V}.log" 2>&1
  wait
done

echo
echo "=== round=$ROUND 요약 표 (PERFORMANCE.md 붙여넣기용) ==="
python3 - "$OUT" <<'PY'
import json, glob, os, sys, re
out = sys.argv[1]
rows = []
for f in sorted(glob.glob(f"{out}/vus_*.json"), key=lambda p: int(re.search(r'vus_(\d+)', p).group(1))):
    with open(f) as fh:
        d = json.load(fh)
    rows.append(d)
print(f"  {'VUS':>5} {'samples':>9} {'TPS':>8} {'avg':>6} {'p50':>6} {'p95':>7} {'p99':>7} {'err%':>6}")
print("  " + "-"*62)
for d in rows:
    print(f"  {d['vus']:>5} {d['samples']:>9} {d['tps']:>8.1f} {d['avg']:>6.0f} {d['p50']:>6.0f} {d['p95']:>7.0f} {d['p99']:>7.0f} {d['err_pct']:>6.2f}")
print()
print("  docker stats (mid-run):")
for sf in sorted(glob.glob(f"{out}/stats_*.txt"), key=lambda p: int(re.search(r'stats_(\d+)', p).group(1))):
    v = re.search(r'stats_(\d+)', sf).group(1)
    print(f"  VUS={v}:")
    with open(sf) as fh:
        for line in fh:
            print("    " + line.rstrip())
PY
echo
echo "결과 원본: $OUT"
