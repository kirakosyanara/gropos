# Authentication Architecture

**Version:** 2.0  
**Last Updated:** January 2026  
**Status:** Authoritative Reference (Updated after Java codebase analysis)

---

## Overview

GroPOS uses a **HYBRID architecture** with TWO different endpoint types. This document is the **authoritative reference** based on analysis of the working Java codebase.

---

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                     HYBRID Endpoint Architecture                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   ┌─────────────────────────────────────────────────────────────────────┐   │
│   │  APIM Gateway (server.transactions.url.base)                        │   │
│   │  https://apim-service-unisight-{env}.azure-api.net                  │   │
│   │                                                                      │   │
│   │  Used for:                                                           │   │
│   │  • POST /api/Registration/CreateQRCodeRegistration ← Registration   │   │
│   │  • GET /api/Registration/GetDeviceRegistrationStatusById            │   │
│   │  • GET /heartbeat ← Device heartbeat                                │   │
│   └─────────────────────────────────────────────────────────────────────┘   │
│                                                                              │
│   ┌─────────────────────────────────────────────────────────────────────┐   │
│   │  App Service DIRECT (server.url.base)                               │   │
│   │  https://app-pos-api-{env}-001.azurewebsites.net                    │   │
│   │                                                                      │   │
│   │  Used for:                                                           │   │
│   │  • GET /api/Employee/GetCashierEmployees ← Cashier list             │   │
│   │  • POST /api/Employee/Login ← Employee login                         │   │
│   │  • GET /api/Product/* ← Products, Categories, Taxes                 │   │
│   │  • All other main POS operations                                    │   │
│   └─────────────────────────────────────────────────────────────────────┘   │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Critical Rule: HYBRID Routing

| Operation | Backend | URL Pattern |
|-----------|---------|-------------|
| **Device Registration** | APIM | `apim-service-unisight-*` |
| **Heartbeat** | APIM | `apim-service-unisight-*` |
| **Get Cashiers** | App Service | `app-pos-api-*` |
| **Employee Login** | App Service | `app-pos-api-*` |
| **Products/Categories** | App Service | `app-pos-api-*` |
| **All POS Operations** | App Service | `app-pos-api-*` |

---

## Environment URLs

### APIM Gateway URLs (Device Registration, Heartbeat)

| Environment | APIM Gateway URL |
|-------------|------------------|
| **Development** | `https://apim-service-unisight-dev.azure-api.net` |
| **Staging** | `https://apim-service-unisight-staging.azure-api.net` |
| **Production** | `https://apim-service-unisight-prod.azure-api.net` |

### App Service URLs (Cashiers, Login, Products, POS Operations)

| Environment | App Service URL |
|-------------|-----------------|
| **Development** | `https://app-pos-api-dev-001.azurewebsites.net` |
| **Staging** | `https://app-pos-api-staging-001.azurewebsites.net` |
| **Production** | `https://app-pos-api-prod-001.azurewebsites.net` |

---

## Two Authentication Methods

| Authentication | Header | When Used | Source |
|----------------|--------|-----------|--------|
| **Device API Key** | `x-api-key: <device-api-key>` | Device-level operations (products, heartbeat, cashier list) | From device registration response |
| **Bearer Token** | `Authorization: Bearer <token>` | Employee-authenticated operations (transactions, login) | From employee login response |

---

## Authentication Flow

### 1. Device Registration (Before Login)

```
POST /api/Registration/GetDeviceRegistrationStatusById
↓
Returns: apiKey (device-specific key)
↓
Stored in Couchbase Lite locally
```

### 2. Device-Level Requests (No employee logged in)

Used for operations that don't require an employee to be logged in.

```http
GET /api/Employee/GetCashierEmployees HTTP/1.1
Host: apim-service-unisight-dev.azure-api.net
x-api-key: <device-api-key-from-registration>
version: v1
```

**Examples:**
- Get cashier list
- Get products
- Device heartbeat

### 3. Employee-Level Requests (After login)

Used for operations that require employee authentication.

```http
POST /api/Transaction/Create HTTP/1.1
Host: apim-service-unisight-dev.azure-api.net
x-api-key: <device-api-key>
Authorization: Bearer <employee-access-token>
version: v1
Content-Type: application/json

{ ... request body ... }
```

**Examples:**
- Create transaction
- Process payment
- Print receipt

---

## Required Headers

| Header | Required | Description |
|--------|----------|-------------|
| `x-api-key` | **Yes** | Device API key from registration |
| `version` | **Yes** | Always `v1` |
| `Authorization` | Conditional | Bearer token (required for employee-level operations) |
| `Content-Type` | POST/PUT | `application/json` |

---

## Key Questions & Answers

| Question | Answer |
|----------|--------|
| Does `x-api-key` work with both APIM and App Service? | Yes - issued by backend, validated by backend, APIM passes it through |
| Should App Service be called directly? | **NO** - always go through APIM |
| Does App Service need a different key? | No - same `x-api-key`, just routed via APIM |
| Why use APIM instead of calling App Service directly? | APIM provides rate limiting, analytics, security policies, and subscription management |

---

## Code Implementation

### ApiClient Configuration (Kotlin)

```kotlin
// All requests go through APIM gateway
val config = ApiClientConfig(
    baseUrl = "https://apim-service-unisight-prod.azure-api.net",
    clientVersion = "1.0.0"
)
```

### Request Headers (Ktor)

```kotlin
// Applied to all requests via defaultRequest
defaultRequest {
    url(config.baseUrl)
    header("version", "v1")
    contentType(ContentType.Application.Json)
}

// Added dynamically for authenticated requests
apiKey?.let { header("x-api-key", it) }
token?.let { header("Authorization", "Bearer $it") }
```

---

## Troubleshooting

### 302 Found Redirect to Company Homepage

**Symptom:** API returns `302 Found` with `Location: https://www.unisight.com`

**Cause:** APIM doesn't recognize the endpoint or API key

**Solutions:**
1. Verify the endpoint path is correct (check APIM configuration)
2. Verify the `x-api-key` header is present and valid
3. Verify the `version` header is set to `v1`
4. Check if the device registration is still valid

### 401 Unauthorized

**Symptom:** API returns `401 Unauthorized`

**Causes:**
- Invalid or expired API key
- Missing Bearer token for employee-level operations
- Expired Bearer token (need refresh)

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | January 2026 | Initial document based on authentication architecture clarification |

