// Shared configuration for all load test scenarios
export const BASE_URL = __ENV.BASE_URL || "http://localhost:8091/api";
export const ADMIN_USER = __ENV.ADMIN_USER || "admin";
export const ADMIN_PASS = __ENV.ADMIN_PASS || "admin123";

// Standard thresholds
export const THRESHOLDS = {
    http_req_duration: ["p(95)<500", "p(99)<1000"],
    http_req_failed: ["rate<0.01"],
};

// Standard load profile: ramp up → sustain → ramp down
export function standardLoadProfile() {
    return [
        { duration: "30s", target: 10 },  // ramp up to 10 VUs
        { duration: "1m", target: 10 },   // sustain 10 VUs
        { duration: "30s", target: 30 },  // ramp up to 30 VUs
        { duration: "1m", target: 30 },   // sustain 30 VUs
        { duration: "30s", target: 0 },   // ramp down
    ];
}

// Smoke test profile: minimal load to verify system works
export function smokeProfile() {
    return [
        { duration: "30s", target: 1 },
        { duration: "1m", target: 1 },
    ];
}

// Stress test profile: push beyond normal
export function stressProfile() {
    return [
        { duration: "30s", target: 10 },
        { duration: "1m", target: 30 },
        { duration: "30s", target: 50 },
        { duration: "1m", target: 50 },
        { duration: "30s", target: 0 },
    ];
}
