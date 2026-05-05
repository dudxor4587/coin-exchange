import http from 'k6/http';
import { check, sleep, fail } from 'k6';

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

function login(email, password) {
    const res = http.post(
        `${BASE_URL}/api/users/login`,
        JSON.stringify({ email, password }),
        params,
    );
    if (res.status !== 200) {
        fail(`로그인 실패 (${email}): status=${res.status}`);
    }
    const cookie = res.cookies['accessToken'];
    if (!cookie || cookie.length === 0) {
        fail(`accessToken 쿠키 없음 (${email})`);
    }
    return cookie[0].value;
}

export function setup() {
    const buyerToken = login('buyer@test.com', 'test');
    const sellerToken = login('seller@test.com', 'test');
    return { buyerToken, sellerToken };
}

export default function (data) {
    const isBuy = (__VU % 2) === 0;
    const token = isBuy ? data.buyerToken : data.sellerToken;
    const url = `${BASE_URL}/api/orders/${isBuy ? 'buy' : 'sell'}`;

    const price = 9000 + Math.floor(Math.random() * 2001);
    const amount = 1 + Math.floor(Math.random() * 5);

    const payload = JSON.stringify({ coinId: 1, price, amount });
    const res = http.post(url, payload, {
        headers: { 'Content-Type': 'application/json' },
        cookies: { accessToken: token },
    });

    check(res, { 'order accepted': (r) => r.status === 200 });

    sleep(0.1);
}
