# GroPOS Architecture Documentation

> System architecture specifications for Kotlin + Compose Multiplatform implementation

---

## Overview

This folder contains architecture documentation for the GroPOS application, covering state management, data flow, dependency injection, and navigation patterns.

---

## Document Structure

| Document | Description | Priority |
|----------|-------------|----------|
| [STATE_MANAGEMENT.md](./STATE_MANAGEMENT.md) | OrderStore, AppStore, StateFlow patterns | P0 |
| [DATA_FLOW.md](./DATA_FLOW.md) | Request/response patterns, data transformation | P1 |
| [NAVIGATION.md](./NAVIGATION.md) | Screen navigation with Compose | P1 |
| [API_INTEGRATION.md](./API_INTEGRATION.md) | Ktor client, OpenAPI integration | P1 |

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        GroPOS Architecture (Kotlin/Compose)                  │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │                         UI LAYER (Compose)                              │ │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐                  │ │
│  │  │  HomeScreen  │  │   PayScreen  │  │ ReturnScreen │  ...             │ │
│  │  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘                  │ │
│  │         │                 │                 │                           │ │
│  │         └─────────────────┴─────────────────┘                           │ │
│  │                           │                                             │ │
│  │                           ▼                                             │ │
│  │  ┌────────────────────────────────────────────────────────────────────┐│ │
│  │  │                    VIEWMODEL LAYER                                 ││ │
│  │  │  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐       ││ │
│  │  │  │ HomeViewModel  │  │  PayViewModel  │  │ReturnViewModel │       ││ │
│  │  │  └───────┬────────┘  └───────┬────────┘  └───────┬────────┘       ││ │
│  │  └──────────┼───────────────────┼───────────────────┼─────────────────┘│ │
│  └─────────────┼───────────────────┼───────────────────┼──────────────────┘ │
│                │                   │                   │                    │
│                └───────────────────┴───────────────────┘                    │
│                                    │                                        │
│                                    ▼                                        │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │                         STATE LAYER (Stores)                           │ │
│  │  ┌────────────────────────┐    ┌────────────────────────────────────┐  │ │
│  │  │       OrderStore       │    │            AppStore                │  │ │
│  │  │  - orderItems: Flow    │    │  - currentEmployee: Flow           │  │ │
│  │  │  - payments: Flow      │    │  - branchSettings: Flow            │  │ │
│  │  │  - totals: Flow        │    │  - deviceInfo: Flow                │  │ │
│  │  └────────────────────────┘    └────────────────────────────────────┘  │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                    │                                        │
│                                    ▼                                        │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │                       SERVICE LAYER (Business Logic)                   │ │
│  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐      │ │
│  │  │   Price     │ │    Tax      │ │  Discount   │ │   Payment   │      │ │
│  │  │ Calculator  │ │ Calculator  │ │ Calculator  │ │   Service   │      │ │
│  │  └─────────────┘ └─────────────┘ └─────────────┘ └─────────────┘      │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                    │                                        │
│                                    ▼                                        │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │                          DATA LAYER                                    │ │
│  │  ┌────────────────────────┐    ┌────────────────────────────────────┐  │ │
│  │  │  Local (CouchbaseLite) │    │       Remote (Ktor Client)         │  │ │
│  │  │  - Products            │    │  - Transaction API                 │  │ │
│  │  │  - Transactions        │    │  - Device API                      │  │ │
│  │  │  - Settings            │    │  - Employee API                    │  │ │
│  │  └────────────────────────┘    └────────────────────────────────────┘  │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## Technology Stack

| Layer | Technology | Notes |
|-------|------------|-------|
| UI | Compose Multiplatform | Shared UI for Desktop + Android |
| State | Kotlin StateFlow | Reactive state management |
| DI | Koin | Multiplatform dependency injection |
| Navigation | Compose Navigation | Screen routing |
| Networking | Ktor Client | HTTP client with OpenAPI codegen |
| Database | CouchbaseLite KMM | Offline-first local storage |
| Serialization | kotlinx.serialization | JSON handling |

---

## Multiplatform Code Sharing

```
┌─────────────────────────────────────────────────────────────────┐
│                    commonMain (85%)                              │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │ • All UI components (Compose)                               ││
│  │ • All business logic (calculations, validations)           ││
│  │ • State management (OrderStore, AppStore)                   ││
│  │ • API clients (Ktor)                                        ││
│  │ • Data models (ViewModels)                                  ││
│  │ • Navigation                                                ││
│  │ • Receipt formatting                                        ││
│  └─────────────────────────────────────────────────────────────┘│
│                                                                  │
│  ┌─────────────────────┐   ┌─────────────────────────────────┐  │
│  │  desktopMain (10%)  │   │       androidMain (5%)          │  │
│  │                     │   │                                 │  │
│  │  • JavaPOS wrapper  │   │  • Sunmi/PAX SDK wrapper        │  │
│  │  • PAX PosLink      │   │  • Android Print API            │  │
│  │  • Serial ports     │   │  • Device-specific scanner      │  │
│  │  • Window mgmt      │   │  • Android lifecycle            │  │
│  └─────────────────────┘   └─────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

---

## Quick Links

| I need to... | Read... |
|--------------|---------|
| Implement state management | [STATE_MANAGEMENT.md](./STATE_MANAGEMENT.md) |
| Understand data flows | [DATA_FLOW.md](./DATA_FLOW.md) |
| Set up navigation | [NAVIGATION.md](./NAVIGATION.md) |
| Integrate with backend API | [API_INTEGRATION.md](./API_INTEGRATION.md) |

---

*Last Updated: January 2026*

