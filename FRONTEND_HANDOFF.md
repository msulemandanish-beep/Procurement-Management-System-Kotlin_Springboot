# Frontend Handoff Document — Procurement Management System

Everything a frontend developer needs to build the entire system: authentication, every API across all 18 phases, business rules, error handling, screen-by-screen guidance, and a recommended React component architecture.

Base URL (local): `http://localhost:8080`

---

## 1. Authentication Flow

```json
POST /api/auth/login
{ "email": "employee@procurement.com", "password": "Employee@123" }
```
```json
{ "token": "eyJ...", "tokenType": "Bearer", "userId": "...", "email": "...", "firstName": "...", "lastName": "...", "role": "EMPLOYEE" }
```

Roles: `ADMIN`, `STORE_MANAGER`, `PROCUREMENT_MANAGER`, `FINANCE_MANAGER`, `EMPLOYEE`.

1. Store `token` + `role` (in memory or secure storage).
2. Attach `Authorization: Bearer <token>` to every request.
3. `401` anywhere → clear auth, redirect to Login.
4. `403` → "access denied" toast, do not log out.
5. Tokens expire after 24h by default.

## 2. Response & Error Conventions (unchanged since Phase 6)

- `200`/`201`/`204` on success.
- Every error body: `{ timestamp, status, error, message, path, details[] }` — `message` is always safe to show directly; `details[]` populated on `400` validation failures (one string per field).
- `409` = business rule violation (workflow state, duplicates, budget/stock issues) — `message` is specific and end-user-safe.

### Pagination (Phase 15)
`GET /api/{products|suppliers|purchase-requests|purchase-orders}/page?page=0&size=20&sort=<field>&direction=ASC|DESC&search=<text>` (plus each module's own filters) returns:
```json
{ "content": [...], "page": 0, "size": 20, "totalElements": 47, "totalPages": 3, "last": false }
```
The original unpaginated endpoints (`GET /api/products`, etc.) still return plain arrays — use them for small reference lists (e.g. populating a dropdown), and the `/page` variants for main data tables.

### CSV downloads (Phase 7)
Any `/api/reports/*` endpoint called with `?format=csv` returns `text/csv` with a `Content-Disposition: attachment` header — trigger it as a normal file download (e.g. `window.open(url)` or an `<a download>` link with the Authorization header attached via fetch + blob).

---

## 3. Complete API Contract

### 3.1–3.6 (Phases 1–2) — Auth, Users, Products, Inventory, Stock Issues, Suppliers
Unchanged from the Phase 6 handoff **except**:
- `ProductRequest` now sends `categoryId` (required) instead of `category`, plus optional `sku`, `barcode`, `unitOfMeasure` (default `"EA"`), `currency` (default `"USD"`), `imageUrl`.
- `ProductResponse` now embeds `category: { id, name, parentCategoryName }` alongside `supplier`, plus a computed `stockValue`.
- `GET /api/suppliers/{id}/performance` (Phase 12) returns a read-only scorecard — see section 3.12.

### 3.7 Departments — unchanged.

### 3.8 Purchase Requests — unchanged contract, **plus**:
- `PurchaseRequestResponse.timeline[]` (Phase 17): `{ status, remarks, actorId, actorName, timestamp }[]` — render as a vertical activity log on the detail page.
- Creating a request now runs server-side duplicate detection: a `409` with a message about "a similar purchase request... already active" means the same employee already has a non-terminal request for the same department + product set. Show this inline rather than retrying.
- New: `GET /api/purchase-requests/page` (Phase 15) — see section 2.

### 3.9 Approval Workflow — unchanged contract, **plus**:
- Rejecting now requires `comments` — if omitted, expect a `400`/`409` with message "A rejection reason is mandatory...". Make the comments field required in the UI whenever "Reject" is selected.
- Approvals may now route to Finance even below the $5,000 threshold if the department's budget would be exceeded — don't hardcode the threshold client-side; always trust `purchaseRequest.currentApprovalLevel`.

### 3.10 Purchase Orders — unchanged contract, plus `GET /api/purchase-orders/page` (Phase 15).

### 3.11 Goods Receipt — unchanged contract from Phase 6.

### 3.12 Supplier Performance (Phase 12)
```
GET /api/suppliers/{id}/performance
```
```json
{
  "supplierId": "...", "supplierName": "...", "totalPurchaseOrders": 4, "completedOrders": 3,
  "cancelledOrders": 0, "averageDeliveryTimeDays": 6.3, "lateDeliveries": 1,
  "acceptedQuantity": 42, "rejectedQuantity": 1, "onTimeDeliveryPercentage": 66.7,
  "supplierRating": 4.1, "averageOrderValue": 1875.50, "totalProcurementValue": 7502.00
}
```
Read-only, entirely computed. No edit form should ever exist for these fields.

### 3.13 Categories (Phase 9)
| Method | Endpoint | Auth |
|---|---|---|
| GET | `/api/categories` (nested main+sub), `/{id}`, `/search?keyword=`, `/statistics` | all roles (statistics: managers/admin) |
| POST, PUT `/{id}`, PATCH `/{id}/activate`, `/deactivate`, DELETE `/{id}` | ADMIN |

`CategoryRequest`: `{ name, parentCategoryId?, description?, active }` — omit `parentCategoryId` for a main category.
`CategoryResponse` includes a nested `subcategories: CategoryResponse[]` when it's a main category.

### 3.14 Notifications (Phase 8)
| Method | Endpoint |
|---|---|
| GET | `/api/notifications`, `/unread`, `/unread-count` |
| PATCH | `/{id}/read`, `/read-all` |

`NotificationResponse`: `{ id, type, title, message, relatedEntityType, relatedEntityId, read, readAt, createdAt }`. `type` values: `PURCHASE_REQUEST_SUBMITTED`, `APPROVAL_REQUIRED`, `APPROVAL_REJECTED`, `APPROVAL_APPROVED`, `PURCHASE_ORDER_CREATED`, `PURCHASE_ORDER_ISSUED`, `GOODS_RECEIVED`, `LOW_STOCK_WARNING`, `OUT_OF_STOCK_WARNING`, `SUPPLIER_DEACTIVATED`, `BUDGET_EXCEEDED`, `BUDGET_NEARLY_EXHAUSTED`. Use `relatedEntityType`/`relatedEntityId` to deep-link the notification to the right detail page. Poll `GET /unread-count` (e.g. every 30-60s) for a navbar badge, or refresh on route change.

### 3.15 Budgets (Phase 11)
| Method | Endpoint | Auth |
|---|---|---|
| GET | `/api/budgets?fiscalYear=`, `/department/{departmentId}?fiscalYear=` | ADMIN, FINANCE_MANAGER, PROCUREMENT_MANAGER |
| POST | `/api/budgets` (create/update annual budget) | ADMIN, FINANCE_MANAGER |

`DepartmentBudgetResponse`: `{ id, departmentId, departmentName, fiscalYear, annualBudget, reservedAmount, spentAmount, remainingAmount, availableAmount, utilizationPercentage, warningLevel }` — `warningLevel` is `HEALTHY | NEARLY_EXHAUSTED | EXCEEDED`; drive a progress bar color from it directly (green/orange/red). `reservedAmount`/`spentAmount` are never directly editable — only `annualBudget` is sent in the request.

### 3.16 Attachments (Phase 10)
| Method | Endpoint |
|---|---|
| POST | `/api/attachments/{ownerType}/{ownerId}` — multipart form, fields: `documentType` (text) + `file` (binary) |
| GET | `/api/attachments/{ownerType}/{ownerId}` — metadata list |
| GET | `/api/attachments/{id}/download` — raw file stream |
| DELETE | `/api/attachments/{id}` |

`ownerType` is `PURCHASE_REQUEST` or `PURCHASE_ORDER`. `documentType` is `QUOTATION | VENDOR_QUOTATION | INVOICE | TECHNICAL_SPECIFICATION | SUPPORTING_DOCUMENT`. Upload with `FormData` + `fetch` (don't set `Content-Type` manually — let the browser set the multipart boundary).

### 3.17 Reports (Phase 7)
`GET /api/reports/{inventory|low-stock|inventory-value|suppliers|purchase-requests|purchase-orders|goods-receipts|department-spending|procurement-spending|monthly-summary}` — each accepts relevant filters (`fromDate`, `toDate`, `departmentId`, `supplierId`, `status`, `employeeId`, `categoryId`) plus `format=json|csv`. JSON responses are wrapped: `{ reportName, generatedAt, rowCount, rows: [...] }`.

### 3.18 Advanced Analytics (Phase 13)
`GET /api/dashboard/charts/{monthly-procurement-spending|purchase-trends|inventory-value-by-supplier|top-suppliers|top-products|department-spending|stock-movement|goods-received-by-month|pending-approvals|low-stock-trend}` — most return `ChartDataPoint[]` (`{ label, value }`), directly usable by any charting library (Recharts/Chart.js: map straight to `[{ name: label, value }]`). `top-products` returns `TopProductResponse[]`; `department-spending` returns `DepartmentSpendingChartResponse[]`.

### 3.19 Audit Logs (Phase 18, ADMIN only)
`GET /api/audit-logs`, `GET /api/audit-logs/search?userId=&module=&action=&fromDate=&toDate=` → `AuditLogResponse[]`: `{ id, userId, username, action, module, entityId, oldValue, newValue, ipAddress, timestamp }`.

### 3.20 Dashboard (extended)
`GET /api/dashboard` now also returns: `totalCategories`, `totalAnnualBudget`, `totalReservedBudget`, `totalSpentBudget`, `averageBudgetUtilizationPercentage`, `departmentsOverBudget`, `openStockWarnings` — add these as additional stat cards / a budget-utilization gauge.

---

## 4. Recommended Page Hierarchy & Navigation

```
Login
└── App Shell (sidebar + navbar with notification bell, role-gated)
    ├── Dashboard                         (ADMIN, STORE_MANAGER)
    ├── Products                          (all view; ADMIN manages) — supports paginated table
    ├── Categories                        (ADMIN) — tree view, main + sub
    ├── Inventory                         (ADMIN, STORE_MANAGER)
    ├── Stock Issues                      (ADMIN, STORE_MANAGER)
    ├── Suppliers                         (ADMIN full; STORE_MANAGER/PROCUREMENT_MANAGER view)
    │   └── Supplier Detail → Performance tab (Phase 12 scorecard)
    ├── Departments                       (ADMIN)
    ├── Budgets                           (ADMIN, FINANCE_MANAGER edit; PROCUREMENT_MANAGER view)
    ├── Purchase Requests
    │   ├── My Requests / All Requests / Detail (with Timeline + Attachments tabs) / Form
    ├── Approvals Inbox
    ├── Purchase Orders
    │   ├── List / Detail (Timeline + Attachments + linked Goods Receipts) / Create-from-request Form
    ├── Goods Receipts
    ├── Reports                           (managers/admin) — filter panel + JSON table + CSV download button
    ├── Analytics / Charts                (managers/admin) — the Phase 13 chart grid
    ├── Notifications                     (all roles) — full history page behind the navbar bell
    ├── Audit Logs                        (ADMIN)
    └── Users                             (ADMIN)
```

### Role-based menu visibility (additions over the Phase 6 table)
| Nav item | ADMIN | STORE_MANAGER | PROCUREMENT_MANAGER | FINANCE_MANAGER | EMPLOYEE |
|---|:---:|:---:|:---:|:---:|:---:|
| Categories | Yes | No | No | No | No |
| Budgets | Yes | No | View only | Yes | No |
| Reports | Yes | Yes | Yes | Yes | No |
| Analytics | Yes | Yes | Yes | Yes | No |
| Audit Logs | Yes | No | No | No | No |
| Notifications | Yes | Yes | Yes | Yes | Yes |

---

## 5. Screen Notes for New Modules

### Reports page
- Left panel: report picker (10 reports) + filter form (date range picker, department/supplier/status/employee/category selects as relevant to the chosen report).
- Right panel: results table (client-paginate the JSON `rows[]` if large) + a "Download CSV" button that re-issues the same request with `format=csv`.

### Analytics / Charts page
- Grid of chart cards, one per endpoint in section 3.18. Suggested chart types: line chart for monthly spending/trends/goods-received-by-month, bar chart for top suppliers/products/department spending/low-stock-trend, donut for pending-approvals-by-level.

### Budgets page
- Table: Department, Annual Budget, Reserved, Spent, Available, Utilization % (progress bar colored by `warningLevel`).
- Edit modal (ADMIN/FINANCE_MANAGER): single field, Annual Budget — reserved/spent are always read-only displays.

### Categories page
- Tree/accordion view: main categories expandable to show subcategories.
- Add Subcategory action available directly from a main category row (pre-fills `parentCategoryId`).
- Deleting a category with active subcategories is blocked server-side (`409`) — show that message inline rather than a generic error.

### Attachments (embedded in Purchase Request / Purchase Order detail pages)
- "Attachments" tab: table of uploaded files (name, type, size, uploader, date) + a drag-and-drop or file-picker upload widget with a Document Type selector.
- Download icon → `GET /{id}/download` (open in new tab or trigger blob download).
- Delete icon with confirmation.

### Notifications page / bell dropdown
- Bell icon in navbar shows `unreadCount` as a badge; clicking opens a dropdown of the 5-10 most recent (from `/unread` or `/` sliced client-side) with a "View all" link to the full Notifications page.
- Each item click marks it read (`PATCH /{id}/read`) and deep-links via `relatedEntityType`/`relatedEntityId` (e.g. `PurchaseRequest` → `/purchase-requests/{id}`).
- "Mark all as read" button at the top of the full page.

### Audit Logs page (ADMIN)
- Filter bar: user, module, action, date range.
- Table: Timestamp, User, Action (badge), Module, Entity ID, Old → New value (monospace, truncated with a "view full" expandable row).

### Purchase Request / Purchase Order detail — Timeline tab
- Vertical stepper/timeline component fed by `timeline[]` — icon per status, remarks as the secondary line, actor name + timestamp as metadata.

---

## 6. Suggested React Component Architecture

```
src/
├── api/                     one file per backend module (axios/fetch wrapper + typed calls),
│                            mirroring the backend's dto/ subfolders 1:1 — see table below
├── components/
│   ├── common/              Table, Modal, Pagination, StatusBadge, ConfirmDialog, Toast,
│   │                        EmptyState, LoadingSkeleton, FilterBar, DateRangePicker
│   ├── layout/               AppShell, Sidebar, Navbar, NotificationBell, RoleGuard
│   ├── charts/                ChartCard (wraps Recharts/Chart.js), reused by Dashboard + Analytics
│   └── domain/                ApprovalTimeline, PurchaseOrderTimeline, AttachmentList,
│                              AttachmentUploader, BudgetUtilizationBar, CategoryTree,
│                              SupplierDropdown, ProductDropdown, DepartmentDropdown
├── pages/                   one folder per nav item in section 4, each with List/Detail/Form
│                            subcomponents as needed
├── hooks/                    useAuth, usePagination, useNotifications (polling), useDebouncedSearch
├── context/                   AuthContext (token, role, user), NotificationContext
└── utils/                     constants (status enums, role list, badge color map), validators,
                              dateUtils, csvDownload
```

### `api/` to backend controller mapping
| `api/*.ts` | Backend controller |
|---|---|
| `authApi` | AuthController |
| `productApi`, `categoryApi` | ProductController, CategoryController |
| `supplierApi`, `supplierPerformanceApi` | SupplierController, SupplierPerformanceController |
| `departmentApi`, `budgetApi` | DepartmentController, BudgetController |
| `purchaseRequestApi` | PurchaseRequestController |
| `approvalApi` | ApprovalController |
| `purchaseOrderApi` | PurchaseOrderController |
| `goodsReceiptApi` | GoodsReceiptController |
| `attachmentApi` | AttachmentController |
| `notificationApi` | NotificationController |
| `reportApi` | ReportController |
| `analyticsApi` | AnalyticsController |
| `auditLogApi` | AuditLogController |
| `dashboardApi` | DashboardController |

## 7. API Integration Sequence (build order)

1. Auth + role-gated shell (navbar/sidebar) first — everything else depends on it.
2. Master data screens sharing one CRUD pattern: Products, Categories, Suppliers, Departments, Users.
3. Budgets (depends on Departments existing).
4. Purchase Request create/edit/submit/cancel + My Requests / All Requests lists.
5. Approvals Inbox + approval actions on the PR detail page (depends on step 4).
6. Purchase Orders (depends on approved PRs from step 5).
7. Goods Receipt (depends on issued POs from step 6) — verify stock actually increases end-to-end here.
8. Attachments (embed into PR/PO detail pages — can be built in parallel with steps 4-7).
9. Notifications (bell + full page — can be built any time after step 1, since it's read-only against seeded/real data).
10. Reports + Analytics + Supplier Performance + Dashboard extensions (read-only, build last, exercise the richest data set).
11. Audit Logs (ADMIN-only, lowest priority).

## 8. Validation, Loading, and Empty States

- Mirror every Bean Validation constraint client-side for instant feedback, but always treat the server's `400`/`409` as the source of truth (network latency, race conditions).
- Loading: skeleton rows for tables, skeleton cards for dashboard/analytics, spinner on submit buttons.
- Empty states: every list screen needs a friendly "nothing here yet" state with a primary action where applicable (e.g. Notifications: "You're all caught up").
- Toasts: green on success, red on error using `message` verbatim, amber for warnings (e.g. a `NEARLY_EXHAUSTED` budget shown after a PR create that doesn't block submission).

## 9. Integration Checklist

1. Start MongoDB + backend, confirm `http://localhost:8080/swagger-ui.html`.
2. Point the frontend at `http://localhost:8080`; centralize the fetch/axios instance with a token interceptor and global `401` handling.
3. Import `postman_collection.json` and walk the full workflow once manually: PR → submit → approve x N → PO → issue → GRN → confirm stock + budget + dashboard numbers all move together.
4. Build in the order given in section 7.
5. CORS is open (`*`) for local development — restrict `allowedOriginPatterns` in `SecurityConfig.kt` before production deployment.
6. `UPLOAD_DIR` must point to a writable, persistent directory in any real deployment (local disk attachments are lost on ephemeral containers) — see README.md section 15 for the cloud-storage migration path.
