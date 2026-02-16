import http from "k6/http";
import { check, sleep, group } from "k6";
import { BASE_URL, THRESHOLDS } from "./config.js";
import { loginAsAdmin, authHeaders } from "./helpers.js";
import { standardLoadProfile } from "./config.js";

export const options = {
    scenarios: {
        appointments: {
            executor: "ramping-vus",
            stages: standardLoadProfile(),
        },
    },
    thresholds: THRESHOLDS,
};

export function setup() {
    const tokens = loginAsAdmin();
    if (!tokens) throw new Error("Admin login failed during setup");
    return { token: tokens.accessToken };
}

export default function (data) {
    const params = authHeaders(data.token);

    group("Available slots query", () => {
        // Query available slots for groomer 1, service type 1, tomorrow
        const tomorrow = new Date();
        tomorrow.setDate(tomorrow.getDate() + 1);
        const dateStr = tomorrow.toISOString().split("T")[0];

        const res = http.get(
            `${BASE_URL}/appointments/available-slots?groomerId=1&serviceTypeId=1&date=${dateStr}`,
            params
        );
        check(res, {
            "available slots status 200 or 404": (r) =>
                r.status === 200 || r.status === 404,
        });
        sleep(0.5);
    });

    group("List appointments by date range", () => {
        const startDate = "2026-01-01";
        const endDate = "2026-12-31";
        const res = http.get(
            `${BASE_URL}/appointments?startDate=${startDate}&endDate=${endDate}&page=0&size=20`,
            params
        );
        check(res, {
            "list appointments status 200": (r) => r.status === 200,
        });
        sleep(0.5);
    });

    group("Groomer availability windows", () => {
        const res = http.get(
            `${BASE_URL}/groomers/1/availability`,
            params
        );
        check(res, {
            "availability status 200 or 404": (r) =>
                r.status === 200 || r.status === 404,
        });
        sleep(0.5);
    });

    sleep(1);
}
