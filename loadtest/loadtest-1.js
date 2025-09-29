import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Counter } from 'k6/metrics';

export let tradeLatency = new Trend('trade_latency_ms');

export let options = {
    vus: 10,
    duration: '30s',
    thresholds: {
        http_req_duration: ['p(95)<500'],
        http_req_failed: ['rate<0.01'],
    },
};

export default function () {
    const buyUrl = 'http://localhost:8080/api/orders/buy';
    const sellUrl = 'http://localhost:8080/api/orders/sell';
    const params = { headers: { 'Content-Type': 'application/json' } };

    const price = 10000;           // 고정 가격
    const amount = 1 + Math.floor(Math.random() * 10); // 수량만 랜덤

    // VU id 기반으로 반반씩 매수/매도
    const isBuy = (__VU % 2) === 0;

    if (isBuy) {
        const buyPayload = JSON.stringify({ coinId: 1, price, amount });
        const buyRes = http.post(buyUrl, buyPayload, params);
        check(buyRes, { 'buy order created': (r) => r.status === 200 });
        tradeLatency.add(buyRes.timings.duration);
    } else {
        const sellPayload = JSON.stringify({ coinId: 1, price, amount });
        const sellRes = http.post(sellUrl, sellPayload, params);
        check(sellRes, { 'sell order created': (r) => r.status === 200 });
        tradeLatency.add(sellRes.timings.duration);
    }

    sleep(0.1);

}
