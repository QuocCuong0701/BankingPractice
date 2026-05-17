// load-test.js
import http from 'k6/http';
import {check, sleep} from 'k6';
import {Counter, Rate, Trend} from 'k6/metrics';

// Custom metrics để xem trong k6 report
const transferSuccess = new Counter('transfer_success');
const transferFail = new Rate('transfer_fail_rate');
const transferLatency = new Trend('transfer_latency', true);

// ── Kịch bản test: tăng dần lên 500 TPS ──────────────────────────
export const options = {
  summaryTrendStats: [
    'avg',
    'min',
    'med',
    'max',
    'p(90)',
    'p(95)',
    'p(99)',
  ],
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
        {duration: '30s', target: 50},
        {duration: '1m', target: 50},
        {duration: '30s', target: 150},
        {duration: '2m', target: 150},
        {duration: '30s', target: 300},
        {duration: '2m', target: 300},
        {duration: '30s', target: 0},  // ramp down
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
// const accounts = [
//   {from: 'ACC0001', to: 'ACC0002'},
//   {from: 'ACC0002', to: 'ACC0003'},
//   {from: 'ACC0003', to: 'ACC0004'},
//   {from: 'ACC0004', to: 'ACC0005'},
//   {from: 'ACC0005', to: 'ACC0001'},
//   {from: 'ACC0001', to: 'ACC0003'},
//   {from: 'ACC0004', to: 'ACC0002'},
//   {from: 'ACC0002', to: 'ACC0005'},
//   // Thêm nhiều cặp nếu có
// ];

/**
 * Hàm sinh danh sách các cặp tài khoản ngẫu nhiên không trùng lặp
 * @param {number} startNum - Số bắt đầu (ví dụ: 6 cho ACC006)
 * @param {number} endNum - Số kết thúc (ví dụ: 1006 cho ACC1006)
 * @param {number} totalPairsNeeded - Số lượng cặp bạn muốn tạo ra để test
 * @returns {Array} Danh sách các cặp {from, to}
 */
function generateRandomAccountPairs(startNum, endNum, totalPairsNeeded) {
  const pairsSet = new Set(); // Dùng Set để tự động loại bỏ trùng lặp cặp
  const result = [];

  // Hàm helper định dạng số thành chuỗi dạng ACC006 hoặc ACC1006
  // Chỉnh số 3 thành số 4 nếu bạn muốn format 4 chữ số (ACC0006)
  const formatAcc = (num) => 'ACC' + String(num).padStart(4, '0');

  const totalPossibleAccounts = endNum - startNum + 1;
  // Kiểm tra an toàn: Số cặp tối đa có thể sinh ra là n * (n - 1)
  const maxPossiblePairs = totalPossibleAccounts * (totalPossibleAccounts - 1);
  const actualPairsToGenerate = Math.min(totalPairsNeeded, maxPossiblePairs);

  while (pairsSet.size < actualPairsToGenerate) {
    // Sinh ngẫu nhiên 2 số trong khoảng [startNum, endNum]
    const fromRand = Math.floor(Math.random() * totalPossibleAccounts) + startNum;
    const toRand = Math.floor(Math.random() * totalPossibleAccounts) + startNum;

    // Điều kiện 1: Không tự chuyển cho chính mình
    if (fromRand === toRand) continue;

    // Tạo key đại diện cho cặp để check trùng trong Set, ví dụ: "6-100"
    const pairKey = `${fromRand}-${toRand}`;

    // Điều kiện 2: Cặp này chưa từng xuất hiện trước đó
    if (!pairsSet.has(pairKey)) {
      pairsSet.add(pairKey);

      result.push({
        from: formatAcc(fromRand),
        to: formatAcc(toRand)
      });
    }
  }

  return result;
}

export default function () {
  // Chọn ngẫu nhiên để phân tán load
  const accounts = generateRandomAccountPairs(1, 1006, 1000);
  const pair = accounts[Math.floor(Math.random() * accounts.length)];

  const payload = JSON.stringify({
    fromAccountNumber: pair.from,
    toAccountNumber: pair.to,
    amount: Math.floor(Math.random() * 9000) + 1000,  // 1k–10k
    description: 'Load test transfer',
    idempotencyKey: `k6-${__VU}-${__ITER}`,  // unique per VU+iteration
  });

  const params = {
    headers: {'Content-Type': 'application/json'},
    tags: {name: 'transfer'},   // group metric theo tên
  };

  const start = Date.now();
  const res = http.post('http://localhost:8080/api/v1/transfer', payload, params);
  transferLatency.add(Date.now() - start);

  const ok = check(res, {
    'status 200': (r) => r?.status === 200,
    'has transactionId': (r) => r.body && JSON.parse(r.body)?.transactionId !== undefined,
    'latency under 500ms': (r) => r?.timings?.duration < 500,
  });
  transferFail.add(!ok);

  if (ok) {
    transferSuccess.add(1);
  } else if (res.status !== 200) {
    console.log(`FAIL: status=${res.status} body=${res.body}`);
  }

  // Không sleep → tối đa hóa TPS
  sleep(0.1) // nếu muốn ~10 req/VU/s
}

// Chạy khi test kết thúc — in summary
export function handleSummary(data) {
  console.log(JSON.stringify(data.metrics.transfer_latency.values, null, 2));
  return {
    'stdout': JSON.stringify({
      totalRequests: data.metrics.http_reqs.values.count,
      successRate: (1 - data.metrics.transfer_fail_rate?.values.rate) * 100 + '%',
      p99Latency: data.metrics.transfer_latency?.values['p(99)'] + 'ms',
      p95Latency: data.metrics.transfer_latency?.values['p(95)'] + 'ms',
      avgTPS: data.metrics.http_reqs.values.rate + ' req/s',
    }, null, 2),
  };
}