import http from "k6/http";
import { check, sleep, group } from "k6";
import { BASE_URL, THRESHOLDS } from "./config.js";
import { loginAsAdmin, authHeaders } from "./helpers.js";
import { standardLoadProfile } from "./config.js";

export const options = {
    scenarios: {
        reports: {
            executor: "ramping-vus",
            stages: standardLoadProfile(),
        },
    },
    thresholds: {
        http_req_duration: ["p(95)<1000", "p(99)<2000"], // reports are heavier
        http_req_failed: ["rate<0.01"],
    },
};

export function setup() {
    const tokens = loginAsAdmin();
    if (!tokens) throw new Error("Admin login failed during setup");
    return { token: tokens.accessToken };
}

export default function (data) {
    const params = authHeaders(data.token);
    const startDate = "2025-01-01";
    const endDate = "2026-12-31";

    group("Daily revenue report", () => {
        const res = http.get(
            `${BASE_URL}/reports/revenue/daily?startDate=${startDate}&endDate=${endDate}`,
            params
        );
        check(res, {
            "revenue report status 200": (r) => r.status === 200,
        });
        sleep(0.5);
    });

    group("Service type report", () => {
        const res = http.get(
            `${BASE_URL}/reports/service-types?startDate=${startDate}&endDate=${endDate}`,
            params
        );
        check(res, {
            "service type report status 200": (r) => r.status === 200,
        });
        sleep(0.5);
    });

    group("Top clients report", () => {
        const res = http.get(
            `${BASE_URL}/reports/clients/top?limit=20`,
            params
        );
        check(res, {
            "top clients report status 200": (r) => r.status === 200,
        });
        sleep(0.5);
    });

    group("Groomer performance report", () => {
        const res = http.get(
            `${BASE_URL}/reports/groomers/performance?startDate=${startDate}&endDate=${endDate}`,
            params
        );
        check(res, {
            "groomer performance status 200": (r) => r.status === 200,
        });
        sleep(0.5);
    });

    group("Status distribution", () => {
        const res = http.get(
            `${BASE_URL}/reports/status-distribution`,
            params
        );
        check(res, {
            "status distribution status 200": (r) => r.status === 200,
        });
        sleep(0.5);
    });

    group("Payment methods stats", () => {
        const res = http.get(`${BASE_URL}/reports/payment-methods`, params);
        check(res, {
            "payment methods status 200": (r) => r.status === 200,
        });
        sleep(0.5);
    });

    group("CSV export - daily revenue", () => {
        const res = http.get(
            `${BASE_URL}/reports/revenue/daily/csv?startDate=${startDate}&endDate=${endDate}`,
            params
        );
        check(res, {
            "CSV export status 200": (r) => r.status === 200,
            "CSV content type": (r) =>
                r.headers["Content-Type"].includes("text/csv"),
        });
        sleep(0.5);
    });

    group("PDF export - daily revenue", () => {
        const res = http.get(
            `${BASE_URL}/reports/revenue/daily/pdf?startDate=${startDate}&endDate=${endDate}`,
            params
        );
        check(res, {
            "PDF export status 200": (r) => r.status === 200,
            "PDF content type": (r) =>
                r.headers["Content-Type"].includes("application/pdf"),
        });
        sleep(0.5);
    });

    sleep(1);
}
