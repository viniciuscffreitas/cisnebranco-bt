import http from "k6/http";
import { check, sleep } from "k6";
import { BASE_URL, ADMIN_USER, ADMIN_PASS, THRESHOLDS } from "./config.js";
import { standardLoadProfile } from "./config.js";

export const options = {
    scenarios: {
        auth_flow: {
            executor: "ramping-vus",
            stages: standardLoadProfile(),
        },
    },
    thresholds: THRESHOLDS,
};

export default function () {
    // Login — each VU gets its own access + refresh token
    const loginRes = http.post(
        `${BASE_URL}/auth/login`,
        JSON.stringify({ username: ADMIN_USER, password: ADMIN_PASS }),
        { headers: { "Content-Type": "application/json" } }
    );
    check(loginRes, {
        "login status 200": (r) => r.status === 200,
        "has access token": (r) => JSON.parse(r.body).accessToken !== undefined,
    });

    if (loginRes.status !== 200) {
        sleep(1);
        return;
    }

    const tokens = JSON.parse(loginRes.body);
    sleep(0.5);

    // Refresh token — consumes old refresh token, gets new pair
    const refreshRes = http.post(
        `${BASE_URL}/auth/refresh`,
        JSON.stringify({ refreshToken: tokens.refreshToken }),
        { headers: { "Content-Type": "application/json" } }
    );
    check(refreshRes, {
        "refresh status 200": (r) => r.status === 200,
        "has new access token": (r) => JSON.parse(r.body).accessToken !== undefined,
    });

    // NOTE: Logout is intentionally skipped in multi-VU load tests.
    // AuthService.logout() revokes ALL refresh tokens for the user,
    // which would cascade-invalidate tokens held by other VUs sharing
    // the same admin account.

    sleep(1);
}
