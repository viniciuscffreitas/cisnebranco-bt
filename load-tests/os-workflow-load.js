import http from "k6/http";
import { check, sleep, group } from "k6";
import { BASE_URL, THRESHOLDS } from "./config.js";
import { loginAsAdmin, authHeaders } from "./helpers.js";
import { standardLoadProfile } from "./config.js";

export const options = {
    scenarios: {
        os_workflow: {
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

    group("List OS with filters", () => {
        // List all OS (paginated)
        const listRes = http.get(`${BASE_URL}/os?page=0&size=10`, params);
        check(listRes, {
            "list OS status 200": (r) => r.status === 200,
        });
        sleep(0.3);

        // Filter by status
        const filterRes = http.get(
            `${BASE_URL}/os?status=WAITING&page=0&size=10`,
            params
        );
        check(filterRes, {
            "filter OS status 200": (r) => r.status === 200,
        });
        sleep(0.3);
    });

    group("Get single OS", () => {
        // Attempt to get OS #1 (may or may not exist)
        const getRes = http.get(`${BASE_URL}/os/1`, params);
        check(getRes, {
            "get OS status 200 or 404": (r) =>
                r.status === 200 || r.status === 404,
        });
        sleep(0.3);
    });

    group("List clients", () => {
        const clientsRes = http.get(
            `${BASE_URL}/clients?page=0&size=10`,
            params
        );
        check(clientsRes, {
            "list clients status 200": (r) => r.status === 200,
        });
        sleep(0.3);
    });

    group("List groomers", () => {
        const groomersRes = http.get(`${BASE_URL}/groomers`, params);
        check(groomersRes, {
            "list groomers status 200": (r) => r.status === 200,
        });
        sleep(0.3);
    });

    group("List service types", () => {
        const typesRes = http.get(`${BASE_URL}/service-types`, params);
        check(typesRes, {
            "list service types status 200": (r) => r.status === 200,
        });
        sleep(0.3);
    });

    sleep(1);
}
