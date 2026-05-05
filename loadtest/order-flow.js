import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    scenarios: {
        order_flow: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '10s', target: 200 },
                { duration: '60s', target: 200 },
                { duration: '10s', target: 0 },
            ],
            gracefulRampDown: '5s',
        },
    },
};

const BASE_URL = 'http://localhost:8080';
const params = { headers: { 'Content-Type': 'application/json' } };

export default function () {
    const isBuy = (__VU % 2) === 0;
    const url = `${BASE_URL}/api/orders/${isBuy ? 'buy' : 'sell'}`;

    const price = 9000 + Math.floor(Math.random() * 2001);
    const amount = 1 + Math.floor(Math.random() * 5);

    const payload = JSON.stringify({ coinId: 1, price, amount });
    const res = http.post(url, payload, params);

    check(res, { 'order accepted': (r) => r.status === 200 });

    sleep(0.1);
}
