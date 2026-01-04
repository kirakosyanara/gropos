# Remediation & Implementation Plans

This folder contains detailed remediation plans and implementation specifications for fixing gaps between the documented architecture and the actual implementation.

---

## Contents

### Active Remediation Plans

| Document | Status | Description |
|----------|--------|-------------|
| [DEVICE_REGISTRATION_REMEDIATION.md](./DEVICE_REGISTRATION_REMEDIATION.md) | ✅ Complete | Fixes for device registration flow (QR code, polling, API key storage) |
| [LOCK_SCREEN_TILL_REMEDIATION.md](./LOCK_SCREEN_TILL_REMEDIATION.md) | ✅ Complete | Fixes for lock screen, cashier login, and till assignment workflows |
| [TRANSACTION_API_SUBMISSION_IMPLEMENTATION_PLAN.md](./TRANSACTION_API_SUBMISSION_IMPLEMENTATION_PLAN.md) | 📋 Draft | Implementation plan for transaction submission to backend API |

### Phase 4 Remediation

The `phase_4/` subfolder contains documentation specific to the Phase 4 hardening effort:

| Document | Description |
|----------|-------------|
| [phase_4/PHASE_4_GAP_ANALYSIS.md](./phase_4/PHASE_4_GAP_ANALYSIS.md) | Gap analysis between docs and implementation |
| [phase_4/PHASE_4_IMPLEMENTATION_PLAN.md](./phase_4/PHASE_4_IMPLEMENTATION_PLAN.md) | Detailed implementation plan for Phase 4 |
| [phase_4/PHASE_4_STATUS_REPORT.md](./phase_4/PHASE_4_STATUS_REPORT.md) | Current status and progress tracking |
| [phase_4/REMEDIATION_CHECKLIST.md](./phase_4/REMEDIATION_CHECKLIST.md) | Checklist of items requiring remediation |

---

## Remediation Workflow

1. **Gap Analysis:** Identify discrepancies between documentation and code
2. **Remediation Plan:** Create detailed implementation specification
3. **Implementation:** Execute changes following the plan
4. **Verification:** Confirm fixes via audit (see `/docs/qa-reports/`)
5. **Documentation Update:** Mark remediation as complete

