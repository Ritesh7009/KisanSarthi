# KisanSarthi Backend Parity & Drift Analysis

This document catalogs every API endpoint exposed by the Node.js/Express demo server (`server.ts`) alongside its canonical counterpart in the Java / Spring Boot backend (`/backend`).

> **Architectural Note:**  
> - **Canonical Source of Truth:** `backend/` (Spring Boot 3, Spring Security, Hibernate/JPA, PostgreSQL via Flyway, Redis, Twilio SMS).
> - **AI Studio Demo Server:** `server.ts` (Node.js/Express, in-memory state engine, WebSockets, JWT signing) designed for zero-config container hosting and live preview in Google AI Studio.

---

## Endpoint Parity Matrix

| Method | Demo Server (`server.ts`) | Spring Boot Canonical (`backend/`) | Parity Status | Notes & Differences |
|---|---|---|---|---|
| `GET` | `/api/health`, `/api/v1/health` | `/actuator/health` | **Equivalent** | Spring Boot exposes standard Spring Boot Actuator health metric; `server.ts` returns custom JSON `{ status: 'UP', service: 'kisansarthi-demo-backend' }`. |
| `POST` | `/api/v1/auth/send-otp` | `/api/v1/auth/send-otp` (`AuthController`) | **Exact Match** | Both enforce 60s cooldown, generate 6-digit OTP, dispatch via SMS (Twilio or mock), and never return OTP in response. |
| `POST` | `/api/v1/auth/verify-otp` | `/api/v1/auth/verify-otp` (`AuthController`) | **Exact Match** | Both verify SHA-256 OTP hash, enforce 5-minute TTL and max 3 attempts. Both issue signed JWT tokens. `server.ts` supports demo fallback strictly when `DEMO_MODE=true` and no OTP was issued. |
| `POST` | `/api/v1/auth/admin-login` | `/api/v1/auth/admin-login` (`AuthController`) | **High Parity** | `backend/` validates DB users with BCrypt; `server.ts` verifies against configured administrative passcodes and returns signed JWT token. |
| `POST` | *N/A* | `/api/v1/auth/refresh` (`AuthController`) | **Backend Only** | Token refresh rotation implemented in Spring Boot `AuthService`. |
| `POST` | *N/A* | `/api/v1/auth/logout` (`AuthController`) | **Backend Only** | In demo mode, client clears session tokens from browser storage. |
| `GET` | `/api/v1/crops` | `/api/v1/crops` (`CropController`) | **Exact Match** | Both return full list of procurement crops, standard MSP, and bonus. |
| `GET` | *N/A* | `/api/v1/crops/{id}` (`CropController`) | **Backend Only** | Direct crop lookup by ID. |
| `PUT`, `PATCH` | `/api/v1/crops/:cropId/msp` | `/api/v1/crops/{id}/msp` (`POST`, `CropController`) | **Behavioral Diff** | `server.ts` uses `PUT`/`PATCH` and accepts either `standardMsp` or `standardMspPerQuintal`; Spring Boot uses `POST` with `UpdateMspRequest`. |
| `GET` | `/api/v1/mandis` | `/api/v1/mandis` (`MandiController`) | **Exact Match** | Both accept optional `?district=` query filter. |
| `GET` | `/api/v1/mandis/:id` | `/api/v1/mandis/{id}` (`MandiController`) | **Exact Match** | Both return mandi details, capacity, and operational configuration. |
| `GET` | `/api/v1/mandis/:id/status` | `/api/v1/mandis/{id}/status` (`MandiController`) | **Exact Match** | Both return active queue counters and operational stats. |
| `GET` | `/api/v1/mandis/:id/queue` | `/api/v1/mandis/{mandiId}/queue` (`QueueController`) | **Exact Match** | Returns active token serving, token sequence, and waiting queue size. |
| `GET` | *N/A* | `/api/v1/mandis/{mandiId}/queue/events` | **Backend Only** | Spring Boot exposes queue event history via REST; `server.ts` broadcasts events over WebSockets (`/ws`). |
| `POST` | `/api/v1/mandis/:id/queue/next` | `/api/v1/mandis/{mandiId}/queue/next` | **Exact Match** | Advances queue, sends arrival SMS alert to farmer, broadcasts update. |
| `POST` | `/api/v1/mandis/:id/queue/skip` | *Handled via queue events/status* | **Demo Added** | `server.ts` adds direct `/skip` helper endpoint for operator convenience. |
| `POST` | `/api/v1/mandis/:id/queue/recall` | *Handled via queue events/status* | **Demo Added** | `server.ts` adds direct `/recall` helper endpoint for operator convenience. |
| `GET` | `/api/v1/mandis/:id/capacity` | *Derived from Slot capacities* | **Demo Added** | `server.ts` exposes direct capacity calculation endpoint. |
| `PUT` | `/api/v1/mandis/:id/capacity` | *Managed via Mandi Admin API* | **Demo Added** | `server.ts` allows updating daily capacity threshold directly. |
| `GET` | `/api/v1/slots` | `/api/v1/mandis/{mandiId}/slots` (`SlotController`) | **Shape Diff** | Spring Boot nests slots under mandi (`/mandis/{mandiId}/slots`); `server.ts` supports global `/api/v1/slots` with optional mandi filter. |
| `PATCH` | `/api/v1/slots/capacity` | `/api/v1/slots/{id}` (`PUT`, `SlotController`) | **Shape Diff** | `server.ts` patches by `timeSlot` key; Spring Boot updates specific slot entity by ID. |
| `GET` | `/api/v1/bookings` | `/api/v1/bookings` (`BookingController`) | **Exact Match** | Both support filtering by `farmerId`, `mandiId`, and `status`. |
| `GET` | `/api/v1/bookings/my` | `/api/v1/bookings/my` (`BookingController`) | **Exact Match** | Returns bookings belonging to currently authenticated farmer. |
| `GET` | `/api/v1/bookings/:id` | `/api/v1/bookings/{id}` (`BookingController`) | **Exact Match** | Returns single booking record with token and schedule details. |
| `POST` | `/api/v1/bookings` | `/api/v1/bookings` (`BookingController`) | **Exact Match** | Both support `Idempotency-Key` header, allocate sequential daily tokens per mandi, validate slot capacity, and dispatch confirmation SMS. |
| `PUT` | `/api/v1/bookings/:id/status` | `/api/v1/bookings/{id}/status` (`PATCH`, `BookingController`) | **Method Diff** | `server.ts` accepts `PUT` & `PATCH`; Spring Boot defines `PATCH`. Both validate lifecycle state transitions (`BOOKED` -> `GATE_CALLED` -> `WEIGHED` -> `COMPLETED`). |
| `POST`, `DELETE`| `/api/v1/bookings/:id/cancel` | `/api/v1/bookings/{id}/cancel` (`POST`, `BookingController`) | **Exact Match** | Cancels booking and releases capacity back to mandi. |
| `POST` | `/api/v1/bookings/:id/reject` | *Handled via status update* | **Demo Added** | Explicit rejection endpoint in `server.ts`. |
| `POST`, `GET` | `/api/v1/bookings/:id/weighment` | `/api/v1/bookings/{bookingId}/weighment` (`WeighmentController`) | **Exact Match** | Computes gross, tare, net weight, deductions, and creates weighment slip. |
| `POST`, `GET` | `/api/v1/bookings/:id/payment` | `/api/v1/bookings/{bookingId}/payment` (`PaymentController`) | **Exact Match** | Calculates payment using MSP + State bonus, generates transaction reference. |
| `GET` | `/api/v1/farmers` | `/api/v1/farmers` (`FarmerController`) | **Exact Match** | Search and list farmers by district or search term. |
| `GET` | `/api/v1/farmers/me` | `/api/v1/farmers/me` (`FarmerController`) | **Exact Match** | Returns profile of authenticated farmer. |
| `PUT` | `/api/v1/farmers/me` | `/api/v1/farmers/me` (`FarmerController`) | **Exact Match** | Updates land acreage, bank details, or contact information. |
| `GET` | `/api/v1/farmers/me/notifications` | `/api/v1/farmers/me/notifications` (`NotificationController`) | **Exact Match** | Farmer alerts and system notifications. |
| `GET` | `/api/v1/sms/config` | `/api/v1/sms/config` (`SmsController`) | **Exact Match** | Returns active SMS provider (`mock` or `twilio`), sender header, and trial mode status. |
| `GET` | `/api/v1/sms/logs` | `/api/v1/sms/logs` (`SmsController`) | **Exact Match** | Returns audit history of dispatched SMS messages. |
| `POST` | `/api/v1/sms/trial-test` | `/api/v1/sms/trial-test` (`SmsController`) | **Exact Match** | Dispatches a pre-verified test SMS via Twilio trial sandbox. |
| `POST` | `/api/v1/sms/send` | `/api/v1/sms/send` (`SmsController`) | **Exact Match** | Dispatches an arbitrary SMS notification to a given phone number. |
| `GET` | `/api/v1/reports/district-stats` | `/api/v1/reports/district-stats` (`ReportController`) | **Exact Match** | Returns district-wise procurement targets vs achieved quintals. |
| `GET` | `/api/v1/weather/:district` | `/api/v1/weather/{district}` (`WeatherController`) | **Exact Match** | District weather alerts, rainfall probabilities, and harvest advisories. |
| `POST` | `/api/v1/ai/slot-recommendation` | `/api/v1/ai/slot-recommendation` (`AiController`) | **Exact Match** | Recommends optimal slot based on weather, congestion, and yield. Uses Gemini model with rule-based fallback. |
| `POST` | `/api/v1/ai/yield-advisor` | `/api/v1/ai/yield-advisor` (`AiController`) | **Exact Match** | Estimates expected crop yield and MSP value based on acreage, soil, and historical data. Uses Gemini model with heuristic fallback. |
| `GET` | `/api/v1/audit/logs` | *Stored in PostgreSQL `audit_logs`* | **Demo Added** | Exposes in-memory audit log ring-buffer to the admin UI. |
| `WS` | `/ws` | `/ws` (`WebSocketConfig`, STOMP/SockJS) | **Protocol Diff** | `server.ts` uses native JSON WebSocket frames; Spring Boot uses STOMP over SockJS/WebSocket with topics `/topic/queue/{mandiId}`, `/topic/audit`, etc. |

---

## Response Envelope Convention

Both backends adhere to the standardized API response envelope:

```json
{
  "success": true,
  "data": { ... },
  "message": "Optional user-facing confirmation message",
  "timestamp": "2026-09-12T08:14:00Z"
}
```

Error responses follow:

```json
{
  "success": false,
  "error": "Human-readable error description",
  "code": "OPTIONAL_ERROR_CODE",
  "errors": { "field": "validation error message" }
}
```

---

## Synchronization Guidelines for Contributors

1. **New Endpoints**: Whenever adding an endpoint to `server.ts`, define its contract first in `backend/.../Controller.java` to prevent drift.
2. **Security & Tokens**: Always use signed JWT tokens with standard claims (`userId`, `role`, `phone`, `mandiId`). Never revert to unhashed or unexpired tokens in demo mode.
3. **Idempotency**: Maintain `Idempotency-Key` header support in any booking creation endpoint across both servers.
