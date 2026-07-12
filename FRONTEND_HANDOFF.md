# Frontend Handoff Document — Procurement Management System

This document explains everything a frontend developer needs to build the entire Procurement Management System UI without asking backend questions: authentication, every API contract across all six phases, business rules, error handling, and page-by-page/screen-by-screen UI guidance.

Base URL (local): `http://localhost:8080`

---

## 1. Authentication & JWT Handling

All endpoints except `POST /api/auth/login` and Swagger routes require:

```
Authorization: Bearer <token>
```

### Login

```json
POST /api/auth/login
{ "email": "employee@procurement.com", "password": "Employee@123" }
```
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "userId": "665f...",
  "email": "employee@procurement.com",
  "firstName": "Usman",
  "lastName": "Employee",
  "role": "EMPLOYEE"
}
```

Roles: `ADMIN`, `STORE_MANAGER`, `PROCUREMENT_MANAGER`, `FINANCE_MANAGER`, `EMPLOYEE`.

### Handling rules

1. Store `token` and `role` after login (in memory or secure storage — avoid `localStorage` in production-grade builds if possible).
2. Attach `Authorization: Bearer <token>` to every request.
3. On `401 Unauthorized` anywhere: clear auth state, redirect to Login.
4. On `403 Forbidden`: user is authenticated but lacks permission — show "access denied", do not log them out.
5. Tokens expire after 24h by default (`86400000` ms).
6. Use `role` to drive navigation, page access, and button visibility per the Role Matrix (Section 10).

---

## 2. Standard Response Conventions

### Headers
Every authenticated request needs:
```
Authorization: Bearer <token>
Content-Type: application/json   (for POST/PUT/PATCH bodies)
```

### Success responses
- `200 OK` — GET, PUT, PATCH, most successful reads/updates
- `201 Created` — POST that creates a resource
- `204 No Content` — DELETE

### Error response (identical shape everywhere)
```json
{
  "timestamp": "2026-07-11T10:10:00Z",
  "status": 409,
  "error": "Conflict",
  "message": "human-readable message — safe to show directly in a toast",
  "path": "/api/approvals/665f.../finance-manager",
  "details": ["field: message", "..."]
}
```
- `400` — validation failure; `details` lists each field error, show them inline on the form.
- `401` — invalid/expired token → redirect to Login.
- `403` — authenticated but forbidden → show access-denied message.
- `404` — resource not found.
- `409` — business rule violation (duplicate, wrong workflow state, insufficient stock, etc.) — `message` is always end-user-safe and specific; show it directly.
- `500` — unexpected error → generic "something went wrong" toast.

### Pagination, search, and filtering
Most list endpoints across Phases 1–2 return plain arrays (no pagination — the datasets are small: users, products, suppliers). Phase 3+ list endpoints that can grow large (`/api/purchase-requests`, `/api/purchase-orders`, `/api/goods-receipts`) also currently return plain arrays for simplicity, but support **query-parameter filtering**:

- `GET /api/purchase-requests/search?status=SUBMITTED&department=IT&priority=HIGH&employeeId=...`
- `GET /api/purchase-orders/status/{status}`
- `GET /api/purchase-orders/supplier/{supplierId}`

All filter parameters are optional and combinable. Build list pages assuming client-side pagination/sorting on top of these arrays (e.g. a data-table component with built-in paging) until a dedicated paginated endpoint is introduced.

---

## 3. Complete API Contract

### 3.1 Auth
| Method | Endpoint | Auth | Body → Response |
|---|---|---|---|
| POST | `/api/auth/login` | No | `{email,password}` → `LoginResponse` |

### 3.2 Users (ADMIN only)
| Method | Endpoint | Body → Response |
|---|---|---|
| GET | `/api/users` | → `UserResponse[]` |
| GET | `/api/users/{id}` | → `UserResponse` |
| POST | `/api/users` | `UserRequest` → `UserResponse` (201) |
| PUT | `/api/users/{id}` | `UserRequest` → `UserResponse` |
| DELETE | `/api/users/{id}` | → 204 |

`UserRequest`: `{ firstName, lastName, email, password?, role, active }` — `role` is one of the 5 roles; `password` optional on update (omit to keep current).

### 3.3 Products
| Method | Endpoint | Auth | Body → Response |
|---|---|---|---|
| GET | `/api/products`, `/api/products/{id}` | All roles | → `ProductResponse` |
| POST, PUT, DELETE | `/api/products` | ADMIN | `ProductRequest` → `ProductResponse` |

`ProductRequest`: `{ name, description, category, unitPrice, currentStock, minimumStock, supplierId }` — `supplierId` **required**, must reference an existing supplier (404 otherwise).

`ProductResponse` embeds supplier so no second call is needed:
```json
{
  "id": "...", "name": "...", "unitPrice": 65.0, "currentStock": 25, "minimumStock": 10,
  "supplier": { "id": "...", "supplierCode": "SUP-0004", "companyName": "Computer World" },
  "status": "IN_STOCK", "createdAt": "...", "updatedAt": "..."
}
```
Populate the Supplier dropdown on the Product form from `GET /api/suppliers/active`.

### 3.4 Inventory (ADMIN, STORE_MANAGER)
| Method | Endpoint | Response |
|---|---|---|
| GET | `/api/inventory` | `InventoryResponse[]` |
| GET | `/api/inventory/low-stock` | `InventoryResponse[]` |
| GET | `/api/inventory/out-of-stock` | `InventoryResponse[]` |
| GET | `/api/inventory/status` | `{IN_STOCK, LOW_STOCK, OUT_OF_STOCK}` counts |
| GET | `/api/inventory/procurement-recommendations` | `ProcurementRecommendationResponse[]` |

### 3.5 Stock Issues (ADMIN, STORE_MANAGER)
| Method | Endpoint | Body → Response |
|---|---|---|
| GET | `/api/issues`, `/api/issues/history` | → `IssueResponse[]` |
| POST | `/api/issues` | `{productId, employeeId, quantity}` → `IssueResponse` |
| PUT | `/api/issues/{id}/return` | → `IssueResponse` |

### 3.6 Suppliers
| Method | Endpoint | Auth | Body → Response |
|---|---|---|---|
| GET | `/api/suppliers` (+ `/{id}`, `/code/{code}`, `/search?keyword=`, `/active`, `/inactive`, `/statistics`) | ADMIN, STORE_MANAGER | → `SupplierResponse[]` / `SupplierSearchResponse[]` / `SupplierStatisticsResponse` |
| POST | `/api/suppliers` | ADMIN | `SupplierRequest` → `SupplierResponse` (201) |
| PUT | `/api/suppliers/{id}` | ADMIN | `SupplierUpdateRequest` → `SupplierResponse` |
| DELETE | `/api/suppliers/{id}` | ADMIN | → 204 |
| PATCH | `/api/suppliers/{id}/activate`, `/deactivate` | ADMIN | → `SupplierResponse` |

`SupplierRequest`: `{ companyName, contactPerson, email, phone, alternatePhone?, website?, address, city, state, country, postalCode, taxNumber, paymentTerms, deliveryLeadTime, notes? }`. `supplierCode` is server-generated (`SUP-0001`, ...) — never send it.

### 3.7 Departments
| Method | Endpoint | Auth | Body → Response |
|---|---|---|---|
| GET | `/api/departments`, `/api/departments/{id}` | All roles | → `DepartmentResponse[]` |
| POST, PUT, DELETE | `/api/departments` | ADMIN | `DepartmentRequest` → `DepartmentResponse` |

`DepartmentRequest`: `{ name, code, description?, active }`. Populate the Purchase Request form's Department field from `GET /api/departments`.

### 3.8 Purchase Requests (Phase 3)
| Method | Endpoint | Auth | Body → Response |
|---|---|---|---|
| GET | `/api/purchase-requests` | ADMIN, STORE_MANAGER, PROCUREMENT_MANAGER, FINANCE_MANAGER | → `PurchaseRequestResponse[]` |
| GET | `/api/purchase-requests/my-requests` | All roles | → `PurchaseRequestResponse[]` (current user's own) |
| GET | `/api/purchase-requests/{id}` | All roles | → `PurchaseRequestResponse` |
| GET | `/api/purchase-requests/search?status=&employeeId=&department=&priority=` | Managers/Admin | → `PurchaseRequestResponse[]` |
| POST | `/api/purchase-requests` | All roles | `PurchaseRequestRequest` → `PurchaseRequestResponse` (201, status `DRAFT`) |
| PUT | `/api/purchase-requests/{id}` | All roles | `PurchaseRequestUpdateRequest` → `PurchaseRequestResponse` (only while `DRAFT`) |
| PATCH | `/api/purchase-requests/{id}/submit` | All roles | → `PurchaseRequestResponse` |
| PATCH | `/api/purchase-requests/{id}/cancel` | All roles | → `PurchaseRequestResponse` |

`PurchaseRequestRequest`:
```json
{
  "department": "Information Technology",
  "items": [
    { "productId": "...", "requestedQuantity": 5, "estimatedUnitPrice": 65.0, "notes": "optional" }
  ],
  "purpose": "string",
  "businessJustification": "string",
  "priority": "LOW | MEDIUM | HIGH | EMERGENCY",
  "requiredDate": "ISO-8601, must be in the future",
  "remarks": "optional"
}
```

`PurchaseRequestResponse` (key fields): `prNumber` (auto, e.g. `PR-0005`), `status` (`DRAFT|SUBMITTED|UNDER_REVIEW|APPROVED|PARTIALLY_APPROVED|REJECTED|CANCELLED|CONVERTED_TO_PO`), `currentApprovalLevel` (`STORE_MANAGER|PROCUREMENT_MANAGER|FINANCE_MANAGER|ADMIN|null`), `estimatedTotal` (auto-computed), `items[]` with `estimatedLineTotal` per line.

**Business rules the UI must respect:**
- `PUT` (update) only works while `status === "DRAFT"` — hide/disable the Edit button otherwise.
- `submit` moves `DRAFT → SUBMITTED` and sets `currentApprovalLevel = STORE_MANAGER`, **unless** `priority === "EMERGENCY"`, in which case it jumps straight to `APPROVED` with no approval steps — show a distinct "Emergency — auto-approved" badge in that case.
- `cancel` is blocked once `status === "CONVERTED_TO_PO"` (409 error) — hide the Cancel button in that state.

### 3.9 Approval Workflow (Phase 4)
| Method | Endpoint | Auth | Body → Response |
|---|---|---|---|
| GET | `/api/approvals/{purchaseRequestId}/history` | All roles | → `ApprovalHistoryResponse[]` (chronological) |
| POST | `/api/approvals/{prId}/store-manager` | ADMIN, STORE_MANAGER | `ApprovalDecisionRequest` → `ApprovalHistoryResponse` |
| POST | `/api/approvals/{prId}/procurement-manager` | ADMIN, PROCUREMENT_MANAGER | same |
| POST | `/api/approvals/{prId}/finance-manager` | ADMIN, FINANCE_MANAGER | same |
| POST | `/api/approvals/{prId}/override?comments=` | ADMIN | → `ApprovalHistoryResponse` |

`ApprovalDecisionRequest`: `{ "decision": "APPROVED | REJECTED | RETURN_FOR_CHANGES", "comments": "optional" }`.

**Workflow logic the UI should reflect (already enforced server-side, but mirror it for good UX):**
- Sequence: `STORE_MANAGER → PROCUREMENT_MANAGER → FINANCE_MANAGER` (Finance step only appears if `estimatedTotal >= 5000`).
- Only call the endpoint matching `purchaseRequest.currentApprovalLevel` — e.g. don't show the "Procurement Manager Approve" button unless `currentApprovalLevel === "PROCUREMENT_MANAGER"`.
- `APPROVED` decision advances `currentApprovalLevel` to the next stage (or to `null` + request `status = APPROVED` if this was the last stage).
- `REJECTED` sets `status = REJECTED`, ends the workflow.
- `RETURN_FOR_CHANGES` sets `status = DRAFT` back — the employee must edit and resubmit.
- `override` (ADMIN only) immediately approves regardless of current stage — show this as a distinct "Emergency Override" action, visually separated from normal approve/reject buttons.
- A `409` here almost always means "someone already acted on this, or it's not at this stage" — refetch the request and show the current state instead of retrying blindly.

### 3.10 Purchase Orders (Phase 5)
| Method | Endpoint | Auth | Body → Response |
|---|---|---|---|
| GET | `/api/purchase-orders` (+ `/{id}`, `/status/{status}`, `/supplier/{supplierId}`) | ADMIN, STORE_MANAGER, PROCUREMENT_MANAGER, FINANCE_MANAGER | → `PurchaseOrderResponse[]` |
| POST | `/api/purchase-orders/from-request/{purchaseRequestId}` | ADMIN, PROCUREMENT_MANAGER | `PurchaseOrderCreateRequest` → `PurchaseOrderResponse` (201) |
| PATCH | `/api/purchase-orders/{id}/issue` | ADMIN, PROCUREMENT_MANAGER | → `PurchaseOrderResponse` |
| PATCH | `/api/purchase-orders/{id}/mark-email-sent` | ADMIN, PROCUREMENT_MANAGER | → `PurchaseOrderResponse` |
| PATCH | `/api/purchase-orders/{id}/cancel` | ADMIN, PROCUREMENT_MANAGER | → `PurchaseOrderResponse` |

`PurchaseOrderCreateRequest`:
```json
{
  "items": [ { "productId": "...", "orderedQuantity": 5, "unitPrice": 65.0, "taxRate": 5.0, "discount": 0.0 } ],
  "supplierIdOverride": "optional, ADMIN only",
  "shipping": 15.0,
  "currency": "USD",
  "expectedDeliveryDate": "ISO-8601, must be in the future"
}
```

**Business rules:**
- Only callable when the source `PurchaseRequest.status === "APPROVED"` (409 otherwise).
- Supplier is derived automatically from the first item's product — don't show a supplier picker unless the user is ADMIN, in which case show `supplierIdOverride` as an optional field.
- `subtotal`, `taxTotal`, `discountTotal`, `grandTotal` are all computed server-side — display them, don't recompute client-side (except for a live preview before submit, which is fine as a UX nicety).
- Status lifecycle: `DRAFT → ISSUED (→ EMAIL_SENT) → PARTIALLY_RECEIVED → COMPLETED`, or `CANCELLED` at any point before `COMPLETED`. Show `timeline[]` as a vertical stepper/history on the PO detail page.
- Creating the PO automatically flips the source Purchase Request to `CONVERTED_TO_PO`.

### 3.11 Goods Receipt / GRN (Phase 6)
| Method | Endpoint | Auth | Body → Response |
|---|---|---|---|
| GET | `/api/goods-receipts` (+ `/{id}`, `/purchase-order/{poId}`) | ADMIN, STORE_MANAGER, PROCUREMENT_MANAGER | → `GoodsReceiptResponse[]` |
| POST | `/api/goods-receipts/purchase-order/{purchaseOrderId}` | ADMIN, STORE_MANAGER | `GoodsReceiptCreateRequest` → `GoodsReceiptResponse` (201) |

`GoodsReceiptCreateRequest`:
```json
{
  "items": [
    { "productId": "...", "receivedQuantity": 5, "rejectedQuantity": 0, "batchNumber": "optional", "serialNumbers": ["optional"], "expiryDate": "optional ISO-8601" }
  ],
  "warehouse": "string",
  "storageLocation": "string",
  "inspectionStatus": "PENDING | PASSED | FAILED | PARTIAL_PASS",
  "qualityNotes": "optional"
}
```

**Business rules — this is the only screen in the whole app that increases inventory:**
- Only callable when the PO is `ISSUED`, `EMAIL_SENT`, or `PARTIALLY_RECEIVED` (409 otherwise — e.g. can't receive against a `DRAFT` or `CANCELLED` PO).
- `receivedQuantity` per line cannot exceed the PO line's remaining unreceived quantity — the server rejects with a 409 naming the exact remaining amount; surface that message directly.
- `rejectedQuantity ≤ receivedQuantity`.
- The PO automatically becomes `PARTIALLY_RECEIVED` or `COMPLETED` depending on whether every line is now fully received — refetch the PO after a successful GRN and update the UI accordingly.
- Support multiple GRNs against one PO (partial deliveries) — the GRN list/detail page for a PO should show every receipt, not just the latest.

### 3.12 Dashboard
| Method | Endpoint | Auth | Response |
|---|---|---|---|
| GET | `/api/dashboard` | ADMIN, STORE_MANAGER | `DashboardResponse` |

```json
{
  "totalProducts": 7, "totalInventoryItems": 229, "lowStockProducts": 2, "outOfStockProducts": 0,
  "totalIssuedProducts": 0, "productsNeedingPurchase": 2,
  "totalSuppliers": 5, "activeSuppliers": 5, "inactiveSuppliers": 0,
  "pendingPurchaseRequests": 1, "approvedPurchaseRequests": 1, "rejectedPurchaseRequests": 1, "itemsWaitingApproval": 1,
  "totalPurchaseOrders": 1, "pendingPurchaseOrders": 0, "completedPurchaseOrders": 1,
  "totalGoodsReceipts": 1, "pendingDeliveries": 0,
  "monthlyProcurementSpend": 2872.50, "inventoryValue": 24387.75,
  "topSuppliers": [ { "supplierId": "...", "supplierName": "Tech Solutions Ltd", "totalPurchaseOrderValue": 2872.50, "purchaseOrderCount": 1 } ]
}
```

---

## 4. Recommended Page Hierarchy & Navigation

```
Login
└── App Shell (sidebar + navbar, role-gated items)
    ├── Dashboard                (ADMIN, STORE_MANAGER)
    ├── Products                 (all roles view; ADMIN manages)
    ├── Inventory                (ADMIN, STORE_MANAGER)
    ├── Stock Issues             (ADMIN, STORE_MANAGER)
    ├── Suppliers                (ADMIN full; STORE_MANAGER view/search)
    ├── Departments              (ADMIN)
    ├── Purchase Requests
    │   ├── My Requests          (all roles)
    │   ├── All Requests         (ADMIN, STORE_MANAGER, PROCUREMENT_MANAGER, FINANCE_MANAGER)
    │   ├── Request Detail       (includes embedded Approval History timeline)
    │   └── New/Edit Request Form
    ├── Approvals Inbox          (role-filtered: show only requests awaiting the viewer's level)
    ├── Purchase Orders
    │   ├── All Purchase Orders  (managers/admin)
    │   ├── PO Detail            (includes timeline + linked Goods Receipts)
    │   └── Create PO (from an approved request)
    ├── Goods Receipts
    │   ├── All Goods Receipts   (ADMIN, STORE_MANAGER, PROCUREMENT_MANAGER)
    │   └── Record GRN (against a PO)
    └── Users                    (ADMIN)
```

### Role-based menu visibility
| Nav item | ADMIN | STORE_MANAGER | PROCUREMENT_MANAGER | FINANCE_MANAGER | EMPLOYEE |
|---|:---:|:---:|:---:|:---:|:---:|
| Dashboard | ✅ | ✅ | ❌ | ❌ | ❌ |
| Products | ✅ | ✅ | ✅ | ✅ | ✅ |
| Inventory | ✅ | ✅ | ❌ | ❌ | ❌ |
| Stock Issues | ✅ | ✅ | ❌ | ❌ | ❌ |
| Suppliers | ✅ | ✅ (view/search only) | ❌ | ❌ | ❌ |
| Departments | ✅ | ❌ | ❌ | ❌ | ❌ |
| My Purchase Requests | ✅ | ✅ | ✅ | ✅ | ✅ |
| All Purchase Requests | ✅ | ✅ | ✅ | ✅ | ❌ |
| Approvals Inbox | ✅ | ✅ | ✅ | ✅ | ❌ |
| Purchase Orders | ✅ | ✅ (view only) | ✅ | ✅ (view only) | ❌ |
| Goods Receipts | ✅ | ✅ | ✅ (view only) | ❌ | ❌ |
| Users | ✅ | ❌ | ❌ | ❌ | ❌ |

---

## 5. Screen-by-Screen Suggestions

### Login
- Email + password form, "Login" button, inline error banner on 401.
- Redirect to Dashboard (ADMIN/STORE_MANAGER) or My Purchase Requests (everyone else) after login.

### Dashboard
- Stat cards: Total Products, Low/Out-of-Stock counts, Total Suppliers (active/inactive), Pending/Approved/Rejected Purchase Requests, Pending/Completed Purchase Orders, Pending Deliveries, Monthly Procurement Spend, Inventory Value.
- Chart suggestions: bar chart for "Top Suppliers by PO value", donut chart for Purchase Request status breakdown, donut for Product stock status breakdown.
- Table: "Procurement Recommendations" (from `/api/inventory/procurement-recommendations`).

### Products
- Table with Supplier column (clickable → Supplier detail), Status badge.
- Add/Edit modal includes a required Supplier dropdown (populate from `/api/suppliers/active`).

### Purchase Requests — List
- Tabs: "My Requests" / "All Requests" (managers/admin only).
- Filters: status, department, priority (wire to `/search`).
- Status badges (suggested colors): `DRAFT` gray, `SUBMITTED`/`UNDER_REVIEW` blue, `APPROVED`/`PARTIALLY_APPROVED` green, `REJECTED` red, `CANCELLED` gray-strikethrough, `CONVERTED_TO_PO` purple.
- Priority badges: `LOW` gray, `MEDIUM` blue, `HIGH` orange, `EMERGENCY` red (pulsing/bold treatment recommended — it bypasses approval entirely).
- Row actions: View, Edit (only if `DRAFT`), Submit (only if `DRAFT`), Cancel (only if not `CONVERTED_TO_PO`/`CANCELLED`).

### Purchase Request — Detail
- Header: PR number, status badge, priority badge, estimated total.
- Line items table with product, quantity, estimated unit price, line total.
- Embedded **Approval History timeline** (from `/api/approvals/{id}/history`): level, approver, decision (color-coded), comments, timestamp.
- If viewer's role matches `currentApprovalLevel` (or is ADMIN): show Approve / Reject / Return-for-Changes buttons with a comments textarea, plus an "Override" button for ADMIN.
- If `status === "APPROVED"` and viewer is ADMIN/PROCUREMENT_MANAGER: show a "Create Purchase Order" button linking to the PO creation form pre-filled with this request's items.

### Purchase Request — New/Edit Form
- Department dropdown (from `/api/departments`).
- Dynamic line-item rows: Product dropdown (from `/api/products`), Quantity, Estimated Unit Price (can prefill from `product.unitPrice`), Notes. "Add item" / "Remove item" buttons.
- Purpose, Business Justification (textareas), Priority dropdown, Required Date (date picker, must be future), Remarks (optional).
- Save as Draft vs. Save & Submit (two buttons — the latter calls create then immediately `PATCH .../submit`).

### Approvals Inbox
- List purchase requests where `currentApprovalLevel` matches the viewer's role (client-side filter of `/api/purchase-requests` or `/search?status=SUBMITTED&status=UNDER_REVIEW`, then filter by `currentApprovalLevel` client-side, since there's no dedicated "my inbox" endpoint yet).
- Same row actions as the Purchase Request detail page's approval section, but accessible in bulk/list form.

### Purchase Orders — List & Detail
- List: PO number, supplier, grand total, status badge, expected delivery date.
- Status badges: `DRAFT` gray, `ISSUED`/`EMAIL_SENT` blue, `PARTIALLY_RECEIVED` orange, `COMPLETED` green, `CANCELLED` red.
- Detail: items table with ordered/received quantity progress bar per line, financial summary (subtotal/tax/discount/shipping/grand total), vertical timeline component driven by `timeline[]`.
- Actions: Issue (if `DRAFT`), Mark Email Sent (if `ISSUED`), Cancel (if not `COMPLETED`/`CANCELLED`), "Record Goods Receipt" (if `ISSUED`/`EMAIL_SENT`/`PARTIALLY_RECEIVED`) linking to the GRN form pre-filled with remaining quantities per line.

### Goods Receipt — List & Form
- List grouped/filterable by PO.
- Form: pre-fill each line's max receivable quantity from `orderedQuantity - receivedQuantity` (compute client-side from the PO detail response); Warehouse, Storage Location, Inspection Status dropdown, Quality Notes.
- On submit success, show a success toast naming the new PO status ("Purchase Order PO-0002 is now COMPLETED" or "... PARTIALLY_RECEIVED").

### Suppliers, Departments, Users
- Same list/detail/modal CRUD pattern as Products — keep visual consistency across all "master data" screens.

---

## 6. UI/UX Conventions

- **Loading states:** skeleton rows/cards for tables and dashboard stat cards; spinner on buttons during submit.
- **Empty states:** friendly message + primary action (e.g. "No purchase requests yet — Create one" for My Requests).
- **Toasts:** success (green) on every successful POST/PUT/PATCH/DELETE; error (red) using the `message` field verbatim from the error response body.
- **Confirmation dialogs:** required before Delete (Users/Products/Suppliers/Departments), Cancel (Purchase Request/Purchase Order), and Reject/Override approval actions.
- **Status badge color palette (suggested, reuse across all modules):** gray = draft/inactive/cancelled, blue = in-progress/submitted, orange = partial/warning, green = approved/completed/active, red = rejected/out-of-stock/error, purple = converted/terminal-success.

---

## 7. JavaScript Fetch Examples

```javascript
async function apiRequest(method, path, token, body) {
  const response = await fetch(`http://localhost:8080${path}`, {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { 'Authorization': `Bearer ${token}` } : {})
    },
    body: body ? JSON.stringify(body) : undefined
  });
  if (response.status === 204) return null;
  const data = await response.json();
  if (!response.ok) throw new Error(data.message || 'Request failed');
  return data;
}

// Login
const { token, role } = await apiRequest('POST', '/api/auth/login', null, { email, password });

// Create a Purchase Request, then submit it
const pr = await apiRequest('POST', '/api/purchase-requests', token, prPayload);
await apiRequest('PATCH', `/api/purchase-requests/${pr.id}/submit`, token);

// Store Manager approves
await apiRequest('POST', `/api/approvals/${pr.id}/store-manager`, token, { decision: 'APPROVED', comments: 'Looks good' });

// Procurement Manager converts an approved request into a PO
const po = await apiRequest('POST', `/api/purchase-orders/from-request/${pr.id}`, token, poPayload);
await apiRequest('PATCH', `/api/purchase-orders/${po.id}/issue`, token);

// Store Manager records a Goods Receipt (increases stock)
const grn = await apiRequest('POST', `/api/goods-receipts/purchase-order/${po.id}`, token, grnPayload);
```

---

## 8. Integration Checklist

1. Start MongoDB and the backend (see `README.md`), confirm `http://localhost:8080/swagger-ui.html` loads.
2. Point the frontend's API base URL at `http://localhost:8080`.
3. Build a shared `apiRequest`/Axios-interceptor helper that attaches the token and handles `401` globally.
4. Use `role` from login to drive the nav menu (Section 4) and page guards.
5. Test the full workflow via `postman_collection.json` first: create PR → submit → approve at each level → create PO → issue → record GRN → confirm stock increased and dashboard numbers update.
6. Build master-data screens (Products, Suppliers, Departments, Users) first — they share one CRUD pattern — then layer the workflow screens (Purchase Requests → Approvals → Purchase Orders → Goods Receipts) on top, since each depends on the one before it.
7. CORS is open (`*`) by default in this build for local development — restrict `allowedOriginPatterns` in `SecurityConfig.kt` before deploying to production.
