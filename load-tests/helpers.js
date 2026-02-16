import http from "k6/http";
import { check } from "k6";
import { BASE_URL, ADMIN_USER, ADMIN_PASS } from "./config.js";

// Login and return tokens
export function login(username, password) {
    const res = http.post(
        `${BASE_URL}/auth/login`,
        JSON.stringify({ username, password }),
        { headers: { "Content-Type": "application/json" } }
    );
    check(res, { "login successful": (r) => r.status === 200 });
    if (res.status === 200) {
        const body = JSON.parse(res.body);
        return {
            accessToken: body.accessToken,
            refreshToken: body.refreshToken,
        };
    }
    return null;
}

// Login as admin
export function loginAsAdmin() {
    return login(ADMIN_USER, ADMIN_PASS);
}

// Build auth headers
export function authHeaders(token) {
    return {
        headers: {
            Authorization: `Bearer ${token}`,
            "Content-Type": "application/json",
        },
    };
}

// Random integer between min and max (inclusive)
export function randomInt(min, max) {
    return Math.floor(Math.random() * (max - min + 1)) + min;
}
