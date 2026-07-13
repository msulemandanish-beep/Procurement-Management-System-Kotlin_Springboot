# Procurement Management System

A lightweight, enterprise-style procurement backend built with Kotlin, Spring Boot, and MongoDB. It covers the complete procurement lifecycle for a small-to-medium organization: inventory, suppliers, purchase requests, multi-level approvals, purchase orders, goods receipt, reporting, notifications, budgets, categories, attachments, audit logging, and analytics — all behind JWT authentication and role-based access control.

## 1. Project Overview

Built incrementally across 18 phases, each preserving everything before it:

| Phase | Module |
|---|---|
| 1 | Inventory Management (Users, Products, Inventory, Stock Issues, Dashboard) |
| 2 | Supplier Management |
| 3 | Purchase Request Management |
| 4 | Approval Workflow |
| 5 | Purchase Order Management |
| 6 | Goods Receipt (GRN) |
| 7 | Reports |
| 8 | Notifications |
| 9 | Product Categories (hierarchical) |
| 10 | File Attachments |
| 11 | Procurement Budgets |
| 12 | Supplier Performance |
| 13 | Advanced Dashboard & Analytics |
| 14 | Advanced Search & Filtering |
| 15 | Pagination |
| 16 | Soft Delete |
| 17 | Activity Timeline |
| 18 | Audit Logs |

### The end-to-end business flow

```
Supplier ──▶ Product (Category) ──▶ Inventory
                                        │
                                        ▼
                       Purchase Request (Employee, Department, Budget check)
                                        │
                                        ▼
                       Approval Workflow (multi-level, budget-aware)
                                        │
                                        ▼
                       Purchase Order (to Supplier)
                                        │
                                        ▼
                       Goods Receipt (GRN) ──▶ Inventory increased, Budget spent
```

**Architectural invariant, unchanged since Phase 6: only Goods Receipt increases `Product.currentStock`.** Stock Issue is still the only module that decreases it. Every mutation everywhere is now additionally recorded to the audit log and, where relevant, triggers an in-app notification.

## 2. Features by Phase

**Phases 1–6** — see the original Inventory/Supplier/Purchase Request/Approval/Purchase Order/Goods Receipt feature set; unchanged in behavior, extended in data shape (see "What changed" below).

**Phase 7 — Reports:** Inventory, Low Stock, Supplier, Purchase Request, Purchase Order, Goods Receipt, Department Spending, Procurement Spending, Inventory Value, and Monthly Procurement Summary reports. Every report accepts date range / department / supplier / status / employee / product / category filters and can be returned as JSON or downloaded as CSV (`?format=csv`).

**Phase 8 — Notifications:** In-app, MongoDB-persisted notifications for PR submission, approval required/approved/rejected, PO created/issued, goods received, low/out-of-stock warnings, supplier deactivation, and budget warnings. Unread/read state, mark-one/mark-all-read, and full history.

**Phase 9 — Categories:** Proper `Category` entity replacing the old free-text field, supporting a two-level hierarchy (main category → subcategory), CRUD, activate/deactivate, search, and statistics. Every product now references a `categoryId`.

**Phase 10 — Attachments:** Upload/download/delete/metadata for files attached to Purchase Requests and Purchase Orders (quotations, invoices, technical specs, supporting documents). Stored as local files with metadata in MongoDB; the storage mechanism is isolated behind `AttachmentService` so Cloudinary/S3 can replace it later without touching any controller.

**Phase 11 — Budgets:** Per-department, per-fiscal-year budgets. Approving a Purchase Request reserves funds; completing the resulting Purchase Order converts the reservation into actual spend. Exceeding a department's remaining budget forces the request through Finance Manager approval regardless of the normal value threshold.

**Phase 12 — Supplier Performance:** Fully computed (never manually edited) scorecards: order counts, on-time delivery percentage, accepted/rejected quantities, average delivery time, and a composite rating — all derived from Purchase Order and Goods Receipt history.

**Phase 13 — Advanced Dashboard & Analytics:** Chart-ready datasets: monthly procurement spending, purchase trends, inventory value by supplier, top suppliers, top/most-requested products, department spending, stock movement, goods received by month, pending approvals by level, and low-stock trend by category.

**Phase 14/15 — Advanced Search & Pagination:** `/page` endpoints on Products, Suppliers, Purchase Requests, and Purchase Orders support `page`, `size`, `sort`, `direction`, and free-text `search`, alongside the existing filter parameters. The original unpaginated endpoints remain for backward compatibility.

**Phase 16 — Soft Delete:** Products, Suppliers, Departments, and Categories are never physically deleted — a `deleted` flag hides them from list/search results while preserving every historical Purchase Request/Order/Receipt that references them.

**Phase 17 — Activity Timeline:** Purchase Requests now carry a `timeline[]` (created, edited, submitted, approved, rejected, returned, converted) alongside the Purchase Order `timeline[]` introduced in Phase 5. Both are embedded directly in their respective API responses.

**Phase 18 — Audit Logs:** Every significant mutation (create/update/delete/approve/reject/submit/cancel/issue/receive/activate/deactivate) is recorded with actor, timestamp, module, entity id, and a best-effort before/after summary. Searchable by user, module, action, and date range (ADMIN only).

### Small professional features included
Product SKU, barcode, unit of measure, per-product currency, product image URL; supplier contact/lead-time fields (already present since Phase 2); mandatory rejection reason on approval rejection; duplicate purchase request detection; role-broadcast "approval required" notifications.

## 3. Technologies

| Layer | Technology |
|---|---|
| Language | Kotlin 1.9 |
| Framework | Spring Boot 3.3 |
| Build Tool | Gradle (Kotlin DSL) |
| Database | MongoDB (Spring Data MongoDB) |
| Security | Spring Security + JJWT (JWT) |
| Docs | springdoc-openapi (Swagger UI) |
| Validation | Jakarta Bean Validation |
| File storage | Local filesystem (pluggable — see Phase 10) |

No new dependencies were added for Phases 7–18: reporting CSV export uses a small hand-written `CsvWriter` (see "Reporting Module" below) rather than pulling in Apache POI/iText, keeping the dependency footprint identical to Phase 6.

## 4. Architecture

Unchanged layering since Phase 1:

```
Controller  →  Service  →  Repository  →  MongoDB
     ↑             ↑
   DTOs        Domain Models
     ↑
 Security (JwtFilter → SecurityContext)
     ↑
GlobalExceptionHandler (cross-cutting)
```

Phases 7–18 added **cross-cutting services** that other services call into rather than new layers:

```
                    ┌─────────────────────┐
                    │  NotificationService │  ← called by StockIssue/Approval/
                    │  (Phase 8)           │    PurchaseOrder/GoodsReceipt/Supplier
                    └─────────────────────┘
                    ┌─────────────────────┐
                    │  AuditLogService     │  ← called by every mutating
                    │  (Phase 18)          │    service after a successful write
                    └─────────────────────┘
                    ┌─────────────────────┐
                    │  BudgetService        │  ← called by ApprovalService
                    │  (Phase 11)           │    (reserve) and GoodsReceiptService
                    └─────────────────────┘    (spend)
```

Both are deliberately "fire and forget" from the calling service's point of view — a notification or audit-log failure never rolls back the actual business operation, keeping the core workflow's reliability independent of these cross-cutting concerns.

### Database relationships (full procurement lifecycle, Phase 9 + 11 additions highlighted)

```
Supplier ──supplies──▶ Product ──belongs to──▶ Category (hierarchy)
                            │
                            ▼
                       Inventory (stock levels)
                            │
                            ▼
              Purchase Request (Department, Budget check)
                            │
                   Approval Workflow
          (STORE_MANAGER → PROCUREMENT_MANAGER
           → FINANCE_MANAGER if high value OR budget exceeded)
                            │  on full approval: Budget.reserve()
                            ▼
                    Purchase Order ──▶ Supplier
                            │
                            ▼
                    Goods Receipt (GRN)
                            │  on completion: Budget.spend()
                            ▼
                 Inventory stock increased
                 (ONLY module allowed to do this)
```

## 5. What changed in existing entities (Phases 7–18)

- **Product**: `category` (free text) → `categoryId` (Phase 9); added `sku`, `barcode`, `unitOfMeasure`, `currency`, `imageUrl`, `deleted` (Phase 16). `ProductResponse` now embeds a `CategorySummary` alongside the existing `SupplierSummary`, plus a computed `stockValue`.
- **Supplier**: added `deleted` (Phase 16); deactivation now triggers a notification and an audit log entry.
- **Department**: added `deleted` (Phase 16).
- **PurchaseRequest**: added `timeline[]` (Phase 17); creation now runs a duplicate-active-request check (small feature); approval now interacts with `BudgetService`.
- **DashboardResponse**: extended with category count, budget totals/utilization/departments-over-budget, and an "open stock warnings" count.

## 6. MongoDB Collections

| Collection | Purpose | Introduced |
|---|---|---|
| `users` | Application users across all five roles | Phase 1 |
| `products` | Product catalog; references `categoryId` and `supplierId` | Phase 1, extended 2/9/16 |
| `stockIssues` | Stock issued to employees and returns | Phase 1 |
| `suppliers` | Supplier master data | Phase 2, extended 16 |
| `departments` | Organizational departments | Phase 3, extended 16 |
| `purchaseRequests` | Employee purchase requests, embedded items and timeline | Phase 3, extended 17 |
| `approvalHistories` | Append-only approval audit trail | Phase 4 |
| `purchaseOrders` | Purchase orders, embedded items and timeline | Phase 5 |
| `goodsReceipts` | Goods received against purchase orders | Phase 6 |
| `categories` | Main-category/subcategory hierarchy | Phase 9 |
| `notifications` | In-app notifications per user | Phase 8 |
| `attachments` | File metadata + local storage path | Phase 10 |
| `departmentBudgets` | Per-department, per-fiscal-year budget figures | Phase 11 |
| `auditLogs` | System-wide audit trail | Phase 18 |

## 7. Folder Structure

```
backend/
├── build.gradle.kts, settings.gradle.kts, gradle.properties
├── .env.example, .gitignore
├── postman_collection.json
├── README.md, FRONTEND_HANDOFF.md, SYSTEM_REPOTREE.md
└── src/main/
    ├── kotlin/com/company/procurement/
    │   ├── ProcurementApplication.kt
    │   ├── config/            SecurityConfig, SwaggerConfig, DataSeeder
    │   ├── controller/        24 controllers — see SYSTEM_REPOTREE.md for the full list
    │   ├── service/           23 services — see SYSTEM_REPOTREE.md for the full list
    │   ├── repository/        18 Spring Data MongoDB repositories
    │   ├── model/              MongoDB documents, one subject per file
    │   ├── dto/                one subfolder per module (auth, user, product, supplier,
    │   │                       department, purchaserequest, approval, purchaseorder,
    │   │                       goodsreceipt, category, notification, budget, attachment,
    │   │                       report, supplierperformance, analytics, common)
    │   ├── security/          JwtProvider, JwtFilter, UserPrincipal, CustomUserDetailsService
    │   ├── exception/         GlobalExceptionHandler + 4 business exceptions
    │   └── util/               CsvWriter (Phase 7), PaginationUtil (Phase 15)
    └── resources/
        ├── application.yml, application-dev.yml, application-prod.yml
```

See `SYSTEM_REPOTREE.md` for a file-by-file explanation of the entire backend and a recommended frontend structure.

## 8. Installation, Configuration, Build, Run

### Option A — Docker (recommended)
```bash
cp .env.example .env   # set a real JWT_SECRET
docker compose up --build
```
Brings up MongoDB and the app together; the app image runs as a non-root user, exposes `/actuator/health` for its container healthcheck, and persists `uploads/` and MongoDB data in named volumes.

### Option B — Local JDK + Gradle
Unchanged since Phase 1 — see below for the full sequence.

### Prerequisites
JDK 17+, MongoDB 6.x+ (local or Docker), Gradle 8.x.

### MongoDB
```bash
docker run -d --name procurement-mongo -p 27017:27017 mongo:7
```
All 14 collections are created automatically by Spring Data MongoDB and `DataSeeder` on first run.

### Environment variables
Copy `.env.example` to `.env`:
```
MONGODB_URI=mongodb://localhost:27017/procurement_db
SERVER_PORT=8080
JWT_SECRET=<a long random secret, 256+ bits>
JWT_EXPIRATION_MS=86400000
SPRING_PROFILES_ACTIVE=dev
UPLOAD_DIR=uploads
```
`UPLOAD_DIR` (Phase 10) controls where uploaded attachment files are written; defaults to `./uploads` relative to the working directory the JVM is started from.

### Gradle wrapper note
No `gradlew` binary ships with this generated project (no network access to fetch it in this environment). Run once locally:
```bash
gradle wrapper --gradle-version 8.8
```

### Build & run
```bash
cd backend
./gradlew clean build
./gradlew bootRun
# or:
java -jar build/libs/procurement-management-system-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

### Swagger
`http://localhost:8080/swagger-ui.html` — every endpoint across all 18 phases is documented with request/response schemas and role requirements. Use **Authorize** with a raw JWT (no `Bearer ` prefix).

## 9. Seeded Test Accounts

| Role | Email | Password |
|---|---|---|
| ADMIN | admin@procurement.com | Admin@123 |
| STORE_MANAGER | storemanager@procurement.com | Manager@123 |
| PROCUREMENT_MANAGER | procurementmanager@procurement.com | Procurement@123 |
| FINANCE_MANAGER | financemanager@procurement.com | Finance@123 |
| EMPLOYEE | employee@procurement.com | Employee@123 |

### What's seeded (idempotent — safe to restart)
- 5 departments, each with an annual budget for the current fiscal year (Phase 11)
- 6 categories: 3 main (Office Supplies, IT Equipment, Furniture) + 3 sub (Computers, Peripherals, Office Chairs) (Phase 9)
- 5 suppliers (`SUP-0001`–`SUP-0005`)
- 7 products, each with a SKU/barcode and linked to a category and supplier
- The same complete procurement workflow demo as Phase 6 (`PR-0001` → `PO-0001` → `GRN-0001` completed; `PR-0002` pending; `PR-0003` rejected; `PR-0004` awaiting Finance)

Note: the seeded demo workflow is inserted directly via repositories (bypassing the services, for deterministic seed data), so it does not itself move the seeded budget figures — the budgets start at their full annual amount and only change from real API-driven approvals/completions made after startup.

## 10. Complete API List

### Phases 1–6 (unchanged paths)
`/api/auth`, `/api/users`, `/api/products` (+`/page`), `/api/inventory`, `/api/issues`, `/api/suppliers` (+`/page`, `/{id}/performance`), `/api/departments`, `/api/purchase-requests` (+`/page`), `/api/approvals`, `/api/purchase-orders` (+`/page`), `/api/goods-receipts`, `/api/dashboard`.

### Phase 7 — Reports (`/api/reports`, ADMIN/STORE_MANAGER/PROCUREMENT_MANAGER/FINANCE_MANAGER)
`GET /inventory`, `/low-stock`, `/inventory-value`, `/suppliers`, `/purchase-requests`, `/purchase-orders`, `/goods-receipts`, `/department-spending`, `/procurement-spending`, `/monthly-summary` — every one accepts `?format=json|csv` plus report-specific filters.

### Phase 8 — Notifications (`/api/notifications`, all roles, own notifications only)
`GET /`, `GET /unread`, `GET /unread-count`, `PATCH /{id}/read`, `PATCH /read-all`.

### Phase 9 — Categories (`/api/categories`)
`GET /`, `GET /{id}`, `GET /search`, `GET /statistics` (all roles); `POST`, `PUT /{id}`, `PATCH /{id}/activate`, `PATCH /{id}/deactivate`, `DELETE /{id}` (ADMIN).

### Phase 10 — Attachments (`/api/attachments`)
`POST /{ownerType}/{ownerId}` (multipart upload), `GET /{ownerType}/{ownerId}` (metadata list), `GET /{id}/download`, `DELETE /{id}` — all roles.

### Phase 11 — Budgets (`/api/budgets`)
`GET /` (+`?fiscalYear=`), `GET /department/{departmentId}` (ADMIN, FINANCE_MANAGER, PROCUREMENT_MANAGER); `POST /` to set a department's annual budget (ADMIN, FINANCE_MANAGER).

### Phase 12 — Supplier Performance
`GET /api/suppliers/{id}/performance` (ADMIN, PROCUREMENT_MANAGER, STORE_MANAGER).

### Phase 13 — Advanced Analytics (`/api/dashboard/charts`, managers/admin)
`GET /monthly-procurement-spending`, `/purchase-trends`, `/inventory-value-by-supplier`, `/top-suppliers`, `/top-products`, `/department-spending`, `/stock-movement`, `/goods-received-by-month`, `/pending-approvals`, `/low-stock-trend`.

### Phase 18 — Audit Logs (`/api/audit-logs`, ADMIN only)
`GET /`, `GET /search?userId=&module=&action=&fromDate=&toDate=`.

## 11. Sample Requests & Responses

### Create a category and a subcategory
```json
POST /api/categories
{ "name": "Electronics", "description": "Electronic goods" }
```
```json
POST /api/categories
{ "name": "Switches", "parentCategoryId": "<electronics-id>" }
```

### Create a product (Phase 9 — categoryId now required)
```json
POST /api/products
{
  "name": "Stapler", "description": "Heavy duty stapler", "categoryId": "<category-id>",
  "sku": "OFS-STA-010", "unitOfMeasure": "EA", "currency": "USD",
  "unitPrice": 4.5, "currentStock": 50, "minimumStock": 10, "supplierId": "<supplier-id>"
}
```

### Download a CSV report
```
GET /api/reports/low-stock?format=csv
Authorization: Bearer <token>
```
Returns `text/csv` with a `Content-Disposition: attachment` header — the browser/HTTP client downloads it directly.

### Get unread notifications
```json
GET /api/notifications/unread
```
```json
[
  {
    "id": "...", "type": "APPROVAL_REQUIRED", "title": "Approval required",
    "message": "Purchase request PR-0007 is awaiting your approval.",
    "relatedEntityType": "PurchaseRequest", "relatedEntityId": "...",
    "read": false, "readAt": null, "createdAt": "2026-07-12T09:00:00Z"
  }
]
```

### Set a department budget
```json
POST /api/budgets
{ "departmentId": "<it-department-id>", "fiscalYear": 2026, "annualBudget": 60000.0 }
```
```json
{
  "id": "...", "departmentId": "...", "departmentName": "Information Technology", "fiscalYear": 2026,
  "annualBudget": 60000.0, "reservedAmount": 0.0, "spentAmount": 0.0,
  "remainingAmount": 60000.0, "availableAmount": 60000.0, "utilizationPercentage": 0.0, "warningLevel": "HEALTHY"
}
```

### Get a paginated page of purchase requests
```
GET /api/purchase-requests/page?page=0&size=10&sort=estimatedTotal&direction=DESC&status=SUBMITTED
```
```json
{
  "content": [ /* PurchaseRequestResponse[] */ ],
  "page": 0, "size": 10, "totalElements": 4, "totalPages": 1, "last": true
}
```

### Supplier performance scorecard
```json
GET /api/suppliers/{id}/performance
```
```json
{
  "supplierId": "...", "supplierName": "Tech Solutions Ltd",
  "totalPurchaseOrders": 4, "completedOrders": 3, "cancelledOrders": 0,
  "averageDeliveryTimeDays": 6.3, "lateDeliveries": 1,
  "acceptedQuantity": 42, "rejectedQuantity": 1,
  "onTimeDeliveryPercentage": 66.7, "supplierRating": 4.1,
  "averageOrderValue": 1875.50, "totalProcurementValue": 7502.00
}
```

### Error response (unchanged format)
```json
{
  "timestamp": "2026-07-12T10:10:00Z", "status": 409, "error": "Conflict",
  "message": "A rejection reason is mandatory — please provide comments explaining the rejection",
  "path": "/api/approvals/665f.../store-manager", "details": []
}
```

## 12. Reporting Module — implementation note

Every report is generated by `ReportService` from live MongoDB data and returned either as JSON (`ReportResult<T>` wrapper) or as CSV (`text/csv`, generated by the dependency-free `CsvWriter` utility). **True binary PDF and XLSX export are not implemented in this build** — adding them is a matter of writing one more `ReportExporter`-style branch per format (e.g. Apache POI for `.xlsx`, OpenPDF/iText for `.pdf`) inside `ReportController.respond(...)`; the report-generation logic in `ReportService` would not need to change at all. This was a deliberate scope decision to keep the dependency footprint identical to Phases 1–6 rather than adding two new heavyweight libraries; JSON + CSV cover every report's data faithfully today.

## 13. Security Model

Unchanged core (JWT, BCrypt, stateless sessions, `@PreAuthorize` per endpoint) — see Phase 6 README history for the full explanation. Hardening added in this pass:

- **Login rate limiting**: `LoginRateLimiter` caps login attempts to 10/minute per client IP (via `X-Forwarded-For` if present, else socket address), returning `429 Too Many Requests`. In-memory and single-instance by design — a real multi-instance production deployment should also rate-limit at the load balancer/API gateway.
- **Startup security check**: `StartupSecurityCheck` logs a loud warning (error-level under the `prod` profile) if the application starts with the bundled default `JWT_SECRET`, so a misconfigured deployment is impossible to miss in the logs.
- **Structured logging**: `logback-spring.xml` adds profile-aware console + rolling-file logging (prod), separate from `application-*.yml`'s `logging.level` overrides, which still apply on top of it.

Additions in Phases 7-18:

- Every mutating endpoint across Phases 7-18 also carries an `@PreAuthorize` role check consistent with the Role Matrix below.
- A rejection at any approval level now requires non-blank `comments` (enforced server-side, `400`/`409`).
- Soft-deleted entities (`deleted = true`) are excluded from every list/search endpoint but still resolvable by id for historical record integrity — `GET .../{id}` on a soft-deleted Category/Department returns `404`, while Product/Supplier lookups used internally by other services (e.g. resolving a historic Purchase Order line) intentionally bypass the deleted filter. Uniqueness checks (product name, supplier company name/email, department name/code, category name) are deleted-aware, so a soft-deleted record's identifiers can be reused by a new one — supplier codes are the deliberate exception, kept globally unique forever since they're used as a permanent external reference.
- Audit logs are written by the server using the authenticated principal from `SecurityContextHolder` — they cannot be spoofed by a request body field.

## 14. Role Matrix

| Capability | ADMIN | STORE_MANAGER | PROCUREMENT_MANAGER | FINANCE_MANAGER | EMPLOYEE |
|---|:---:|:---:|:---:|:---:|:---:|
| Manage Users | ✅ | ❌ | ❌ | ❌ | ❌ |
| Manage Products / Categories | ✅ | ❌ | ❌ | ❌ | ❌ |
| View Products / Categories | ✅ | ✅ | ✅ | ✅ | ✅ |
| Manage Inventory / Stock Issues | ✅ | ✅ | ❌ | ❌ | ❌ |
| Manage Suppliers | ✅ | ❌ | ❌ | ❌ | ❌ |
| View/Search Suppliers, View Performance | ✅ | ✅ | ✅ | ❌ | ❌ |
| Manage Departments | ✅ | ❌ | ❌ | ❌ | ❌ |
| Create / View Own Purchase Requests | ✅ | ✅ | ✅ | ✅ | ✅ |
| View All Purchase Requests | ✅ | ✅ | ✅ | ✅ | ❌ |
| Approve at Store Manager / Procurement Manager / Finance Manager level | per level | SM only | PM only | FM only | ❌ |
| Admin Override Approval | ✅ | ❌ | ❌ | ❌ | ❌ |
| Create/Issue/Cancel Purchase Orders | ✅ | ❌ | ✅ | ❌ | ❌ |
| View Purchase Orders | ✅ | ✅ | ✅ | ✅ | ❌ |
| Record Goods Receipt | ✅ | ✅ | ❌ | ❌ | ❌ |
| View Goods Receipts | ✅ | ✅ | ✅ | ❌ | ❌ |
| View Dashboard, Reports, Analytics | ✅ | ✅ (dashboard) | ✅ (reports/analytics) | ✅ (reports/analytics) | ❌ |
| Manage Budgets | ✅ | ❌ | ❌ | ✅ | ❌ |
| View Budgets | ✅ | ❌ | ✅ | ✅ | ❌ |
| Upload/Download/Delete Attachments | ✅ | ✅ | ✅ | ✅ | ✅ |
| View/Manage Own Notifications | ✅ | ✅ | ✅ | ✅ | ✅ |
| View Audit Logs | ✅ | ❌ | ❌ | ❌ | ❌ |
| View Own Profile | ✅ | ✅ | ✅ | ✅ | ✅ |

## 15. Future Roadmap

- ~~Phases 1–18~~ ✅ all complete
- **Warehouses & Locations** as first-class entities (currently free-text on Goods Receipt)
- **True PDF/XLSX report export** (Apache POI / OpenPDF) — see section 12 for the exact extension point
- **Printable Purchase Order PDF** using the same exporter abstraction
- **Multiple suppliers per product** with per-supplier price history
- **Email/push delivery** for notifications (the abstraction in `NotificationService` already anticipates this)
- **True MongoDB-level pagination** (skip/limit queries) if collection sizes grow beyond what in-memory paging comfortably handles
- **Field-level audit diffing** (currently a best-effort before/after summary string, not a full diff)
- **Cloud storage** for attachments (Cloudinary/S3) — `AttachmentService`'s public API is already storage-agnostic

The package structure (`config`, `controller`, `service`, `repository`, `model`, `dto`, `security`, `exception`, `util`) remains intentionally generic so every item above can be added as new files within the same layers without refactoring existing code.
