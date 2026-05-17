// load-test.js
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

// Custom metrics để xem trong k6 report
const transferSuccess = new Counter('transfer_success');
const transferFail    = new Rate('transfer_fail_rate');
const transferLatency = new Trend('transfer_latency', true);

// ── Kịch bản test: tăng dần lên 500 TPS ──────────────────────────
export const options = {
  scenarios: {
    // Giai đoạn 1: ramp up từ 0 lên 100 VU trong 30s
    // Giai đoạn 2: giữ 100 VU trong 1 phút
    // Giai đoạn 3: tăng lên 200 VU trong 30s
    // Giai đoạn 4: giữ 200 VU trong 2 phút
    // Giai đoạn 5: ramp down
    stress_test: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 50  },
        { duration: '1m',  target: 50  },
        { duration: '30s', target: 150 },
        { duration: '2m',  target: 150 },
        { duration: '30s', target: 300 },
        { duration: '2m',  target: 300 },
        { duration: '30s', target: 0   },  // ramp down
      ],
    },
  },

  // ── Tiêu chí pass/fail ────────────────────────────────────────
  thresholds: {
    // 99% request hoàn thành dưới 500ms
    'http_req_duration{name:transfer}': ['p(99)<500'],
    // Error rate dưới 1%
    'transfer_fail_rate': ['rate<0.01'],
    // 95% request dưới 200ms
    'transfer_latency': ['p(95)<200'],
  },
};

// Danh sách account để test — tạo nhiều cặp để tránh hot account
const accounts = [
  { from: 'ACC001', to: 'ACC002' },
  { from: 'ACC002', to: 'ACC001' },
  // Thêm nhiều cặp nếu có
];

export default function () {
  // Chọn ngẫu nhiên để phân tán load
  const pair = accounts[Math.floor(Math.random() * accounts.length)];

  const payload = JSON.stringify({
    fromAccountNumber: pair.from,
    toAccountNumber:   pair.to,
    amount:            Math.floor(Math.random() * 9000) + 1000,  // 1k–10k
    description:       'Load test transfer',
    idempotencyKey:    `k6-${__VU}-${__ITER}`,  // unique per VU+iteration
  });

  const params = {
    headers: { 'Content-Type': 'application/json' },
    tags:    { name: 'transfer' },   // group metric theo tên
  };

  const start = Date.now();
  const res = http.post('http://localhost:8080/api/v1/transfer', payload, params);
  transferLatency.add(Date.now() - start);

  const ok = check(res, {
    'status 200':          (r) => r.status === 200,
    'has transactionId':   (r) => JSON.parse(r.body).transactionId !== undefined,
    'latency under 500ms': (r) => r.timings.duration < 500,
  });

  if (ok) {
    transferSuccess.add(1);
  } else {
    transferFail.add(1);
    // Log response lỗi để debug
    if (res.status !== 200) {
      console.log(`FAIL: status=${res.status} body=${res.body}`);
    }
  }

  // Không sleep → tối đa hóa TPS
  // sleep(0.1) nếu muốn ~10 req/VU/s
}

// Chạy khi test kết thúc — in summary
export function handleSummary(data) {
  return {
    'stdout': JSON.stringify({
      totalRequests:  data.metrics.http_reqs.values.count,
      successRate:    (1 - data.metrics.transfer_fail_rate?.values.rate) * 100 + '%',
      p99Latency:     data.metrics.http_req_duration?.values['p(99)'] + 'ms',
      p95Latency:     data.metrics.http_req_duration?.values['p(95)'] + 'ms',
      avgTPS:         data.metrics.http_reqs.values.rate + ' req/s',
    }, null, 2),
  };
}