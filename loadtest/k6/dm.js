// DM hot path 부하 시나리오 (k6 native).
// 기존 loadtest/jmx/dm.jmx 와 동일: POST /messages 70% + GET /messages/with/{peer} 30%, 단일 토큰.
// 한 번 실행 = 하나의 VU 레벨(VUS)을 RAMP 후 DURATION 동안 유지. 레벨별로 반복 호출은 scripts/k6-load.sh 담당.
//
// 필요한 env:
//   BASE     기본 http://localhost:8080
//   TOKEN    sender access token (필수)
//   PEER     receiver memberId (필수)
//   VUS      동시 가상 사용자 수 (기본 50)
//   DURATION 정상 부하 유지 시간 (기본 120s)
//   RAMP     ramp-up 시간 (기본 30s)
//   SUMMARY_OUT  요약 JSON 저장 경로 (기본 stdout)
import http from 'k6/http';
import { check } from 'k6';

const BASE = __ENV.BASE || 'http://localhost:8080';
const TOKEN = __ENV.TOKEN;
const PEER = __ENV.PEER || '2';
const VUS = Number(__ENV.VUS || 50);
const DURATION = __ENV.DURATION || '120s';
const RAMP = __ENV.RAMP || '30s';

export const options = {
  scenarios: {
    dm: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: RAMP, target: VUS },
        { duration: DURATION, target: VUS },
      ],
      gracefulStop: '5s',
    },
  },
  thresholds: { http_req_failed: ['rate<0.05'] },
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
  discardResponseBodies: true, // 클라이언트 오버헤드 최소화 — 상태코드만 검사
};

const headers = { Authorization: `Bearer ${TOKEN}`, 'Content-Type': 'application/json' };

export default function () {
  let res;
  if (Math.random() < 0.7) {
    res = http.post(
      `${BASE}/messages`,
      JSON.stringify({ receiverId: Number(PEER), content: 'load test' }),
      { headers, tags: { name: 'POST /messages' } },
    );
  } else {
    res = http.get(`${BASE}/messages/with/${PEER}?page=0&size=20`, {
      headers,
      tags: { name: 'GET /messages/with' },
    });
  }
  check(res, { 'status < 400': (r) => r.status < 400 });
}

export function handleSummary(data) {
  const dur = (data.metrics.http_req_duration || {}).values || {};
  const reqs = (data.metrics.http_reqs || {}).values || {};
  const failed = (data.metrics.http_req_failed || {}).values || {};
  const out = {
    vus: VUS,
    samples: reqs.count || 0,
    tps: reqs.rate || 0,
    avg: dur.avg || 0,
    p50: dur.med || 0,
    p95: dur['p(95)'] || 0,
    p99: dur['p(99)'] || 0,
    err_pct: (failed.rate || 0) * 100,
  };
  const result = {
    stdout: `\n  VUS=${VUS}  samples=${out.samples}  TPS=${out.tps.toFixed(1)}  p95=${out.p95.toFixed(0)}ms  err=${out.err_pct.toFixed(2)}%\n`,
  };
  if (__ENV.SUMMARY_OUT) {
    result[__ENV.SUMMARY_OUT] = JSON.stringify(out, null, 2);
  }
  return result;
}
