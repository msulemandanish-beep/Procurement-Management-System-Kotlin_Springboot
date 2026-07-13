# SYSTEM_REPOTREE.md — Complete Architecture Reference

The single reference for every file in this system, backend (as built) and frontend (as recommended), across all 18 phases. Read alongside `README.md` (business/API reference) and `FRONTEND_HANDOFF.md` (UI guidance).

---

## PART 1 — BACKEND

### 1.0 Root files

| File | Purpose |
|---|---|
| `ProcurementApplication.kt` | `@SpringBootApplication` entry point. Nothing else lives here. |
| `build.gradle.kts` | Dependencies, plugins, JVM target. No new dependencies were added for Phases 7-18 (see README §3). |
| `settings.gradle.kts`, `gradle.properties` | Gradle project metadata. |
| `.env.example` | Template for `MONGODB_URI`, `JWT_SECRET`, `UPLOAD_DIR`, etc. |
| `postman_collection.json` | Every endpoint across all 18 phases, organized into 20 folders. |
| `README.md`, `FRONTEND_HANDOFF.md`, `SYSTEM_REPOTREE.md` | The three documentation files. |

### 1.1 `config/` — application-level wiring, not business logic

| File | Responsibility | Relationships |
|---|---|---|
| `SecurityConfig.kt` | `SecurityFilterChain`: public routes (`/api/auth/login`, Swagger), CORS, password encoder bean, `AuthenticationManager` bean, wires `JwtFilter`. | Depends on `CustomUserDetailsService`, `JwtFilter`. Per-endpoint role checks live in controllers via `@PreAuthorize`, not here. |
| `SwaggerConfig.kt` | OpenAPI bean: title, description, `bearerAuth` scheme used by every controller. | None. |
| `DataSeeder.kt` | `CommandLineRunner`. Seeds, in order: users (5 roles) → departments → department budgets → categories (3 main + 3 sub) → suppliers → products (linked to category+supplier) → a full PR→Approval→PO→GRN demo workflow. Every method is idempotent (checks `count() > 0` or looks up by unique field first). | Depends on nearly every repository; the only place in the codebase that constructs entities across every module at once. |

### 1.2 `controller/` — 24 REST controllers, one per resource family

Every controller: `@RestController`, `@RequestMapping("/api/...")`, `@Tag` (Swagger), `@SecurityRequirement(name = "bearerAuth")`, `@PreAuthorize` per method. No business logic — validate (`@Valid`), call one service method, wrap in `ResponseEntity`.

| Controller | Phase | Base path | Key methods |
|---|---|---|---|
| `AuthController` | 1 | `/api/auth` | `login` |
| `UserController` | 1 | `/api/users` | CRUD |
| `ProductController` | 1, ext. 2/9/14/15 | `/api/products` | CRUD, `getProductsPage` (Phase 15) |
| `InventoryController` | 1 | `/api/inventory` | status views, procurement recommendations |
| `StockIssueController` | 1 | `/api/issues` | issue, return |
| `DashboardController` | 1, ext. 2-18 | `/api/dashboard` | `getDashboardStatistics` |
| `SupplierController` | 2, ext. 14/15 | `/api/suppliers` | CRUD, activate/deactivate, search, `getSuppliersPage` |
| `SupplierPerformanceController` | 12 | `/api/suppliers/{id}/performance` | `getPerformance` |
| `DepartmentController` | 3 | `/api/departments` | CRUD |
| `PurchaseRequestController` | 3, ext. 14/15 | `/api/purchase-requests` | CRUD, submit, cancel, search, `getRequestsPage` |
| `ApprovalController` | 4 | `/api/approvals` | per-level decision endpoints, override, history |
| `PurchaseOrderController` | 5, ext. 14/15 | `/api/purchase-orders` | create-from-request, issue, mark-email-sent, cancel, `getOrdersPage` |
| `GoodsReceiptController` | 6 | `/api/goods-receipts` | create, list, list-by-PO |
| `CategoryController` | 9 | `/api/categories` | CRUD, activate/deactivate, search, statistics |
| `NotificationController` | 8 | `/api/notifications` | list, unread, unread-count, mark-read, mark-all-read |
| `AttachmentController` | 10 | `/api/attachments` | upload, metadata, download, delete |
| `BudgetController` | 11 | `/api/budgets` | list, get-by-department, create-or-update |
| `AnalyticsController` | 13 | `/api/dashboard/charts` | 10 chart-data endpoints |
| `ReportController` | 7 | `/api/reports` | 10 report endpoints, each JSON or CSV |
| `AuditLogController` | 18 | `/api/audit-logs` | list, search |

### 1.3 `service/` — 23 services, all business logic lives here

| Service | Depends on (services) | Responsibility |
|---|---|---|
| `AuthService` | - | Authenticate, issue JWT |
| `UserService` | - | User CRUD, password hashing |
| `ProductService` | `SupplierService`, `CategoryService` | Product CRUD, category+supplier validation, soft delete (Phase 16), pagination (Phase 15) |
| `CategoryService` | - | Category CRUD, hierarchy assembly, soft delete |
| `InventoryService` | - | Read-only stock views, procurement recommendations |
| `StockIssueService` | `ProductService`, `NotificationService` | Issue/return stock; triggers low/out-of-stock notifications |
| `SupplierService` | `NotificationService`, `AuditLogService` | Supplier CRUD, soft delete, deactivation notification+audit |
| `SupplierPerformanceService` | `SupplierService` | Computes scorecards from `PurchaseOrderRepository`+`GoodsReceiptRepository` — nothing stored, nothing editable |
| `DepartmentService` | - | Department CRUD, soft delete |
| `BudgetService` | `DepartmentService` | Reserve (on PR approval)/spend (on PO completion)/exceeded-check; never lets an API consumer set reserved/spent directly |
| `PurchaseRequestService` | `ProductService`, `NotificationService`, `AuditLogService` | PR CRUD, PR-number generation, timeline (Phase 17), duplicate detection, approver notification broadcast |
| `ApprovalService` | `PurchaseRequestService`, `BudgetService`, `NotificationService`, `AuditLogService` | Workflow advancement, mandatory rejection reason, budget-aware Finance escalation, ADMIN override |
| `PurchaseOrderService` | `PurchaseRequestService`, `ProductService`, `SupplierService`, `NotificationService`, `AuditLogService` | Converts approved PR to PO, totals calculation, status timeline, pagination |
| `GoodsReceiptService` | `PurchaseOrderService`, `PurchaseRequestService`, `ProductService`, `BudgetService`, `NotificationService`, `AuditLogService` | **The only service that increases `Product.currentStock`.** Also triggers `BudgetService.spend` on PO completion |
| `DashboardService` | - (reads repositories directly) | Aggregates every module's statistics into one response |
| `AnalyticsService` | - (reads repositories directly) | Chart-ready datasets (Phase 13) |
| `ReportService` | - (reads repositories directly) | Reusable report generation (Phase 7); one method per report, shared `ReportFilter` |
| `NotificationService` | - | Persists/retrieves notifications; deliberately delivery-mechanism-agnostic (Phase 8) |
| `AuditLogService` | - | Persists/searches audit entries; reads the actor from `SecurityContextHolder` (Phase 18) |
| `AttachmentService` | - | Local-disk file storage + MongoDB metadata, storage mechanism isolated behind its public API (Phase 10) |

### 1.4 `repository/` — 18 Spring Data MongoDB interfaces

Thin `MongoRepository<T, String>` extensions with derived query methods only — no logic.

| Repository | Collection | Added |
|---|---|---|
| `UserRepository` | `users` | Phase 1, `findByRole` added Phase 8 |
| `ProductRepository` | `products` | Phase 1 |
| `StockIssueRepository` | `stockIssues` | Phase 1 |
| `SupplierRepository` | `suppliers` | Phase 2 |
| `DepartmentRepository` | `departments` | Phase 3 |
| `PurchaseRequestRepository` | `purchaseRequests` | Phase 3 |
| `ApprovalHistoryRepository` | `approvalHistories` | Phase 4 |
| `PurchaseOrderRepository` | `purchaseOrders` | Phase 5 |
| `GoodsReceiptRepository` | `goodsReceipts` | Phase 6 |
| `CategoryRepository` | `categories` | Phase 9 |
| `NotificationRepository` | `notifications` | Phase 8 |
| `AttachmentRepository` | `attachments` | Phase 10 |
| `DepartmentBudgetRepository` | `departmentBudgets` | Phase 11 |
| `AuditLogRepository` | `auditLogs` | Phase 18 |

### 1.5 `model/` — MongoDB documents

| File | Notes |
|---|---|
| `User.kt`, `Role.kt` | 5 roles: `ADMIN`, `STORE_MANAGER`, `PROCUREMENT_MANAGER`, `FINANCE_MANAGER`, `EMPLOYEE` |
| `Product.kt`, `ProductStatus.kt` | `categoryId` (Phase 9, replaces old free-text), `sku`/`barcode`/`unitOfMeasure`/`currency`/`imageUrl`, `deleted` (Phase 16) |
| `Category.kt` | Self-referencing `parentCategoryId: String?` gives a two-level hierarchy; `deleted` flag |
| `StockIssue.kt`, `IssueStatus.kt` | Unchanged since Phase 1 |
| `Supplier.kt`, `SupplierStatus.kt` | `deleted` flag added Phase 16 |
| `Department.kt` | `deleted` flag added Phase 16 |
| `DepartmentBudget.kt` | One per department per fiscal year; computed properties `remainingAmount`/`availableAmount`/`utilizationPercentage` |
| `PurchaseRequest.kt` + `PurchaseRequestItem.kt` + `PurchaseRequestTimelineEntry.kt` + `PurchaseRequestStatus.kt` + `Priority.kt` | `timeline: List<PurchaseRequestTimelineEntry>` added Phase 17 |
| `ApprovalHistory.kt` + `ApprovalLevel.kt` + `ApprovalDecision.kt` | Append-only audit trail, unchanged since Phase 4 |
| `PurchaseOrder.kt` + `PurchaseOrderItem.kt` + `PurchaseOrderTimelineEntry.kt` + `PurchaseOrderStatus.kt` | Unchanged since Phase 5/6 |
| `GoodsReceipt.kt` + `GoodsReceiptItem.kt` + `GoodsReceiptStatus.kt` + `InspectionStatus.kt` | Unchanged since Phase 6 |
| `Notification.kt`, `NotificationType.kt` | Phase 8 |
| `Attachment.kt`, `AttachmentOwnerType.kt`, `AttachmentDocumentType.kt` | Phase 10; `storagePath` is an opaque local-disk path today |
| `AuditLog.kt`, `AuditAction.kt` | Phase 18 |

### 1.6 `dto/` — one subfolder per module

| Subfolder | Key types | Phase |
|---|---|---|
| `auth/` | `LoginRequest`, `LoginResponse` | 1 |
| `user/` | `UserRequest`, `UserResponse` | 1 |
| `product/` | `ProductRequest` (now `categoryId`+SKU/barcode/UoM/currency/image), `ProductResponse` (embeds `CategorySummary`+`SupplierSummary`+`stockValue`) | 1, ext. 9 |
| `category/` | `CategoryRequest`, `CategoryResponse` (nested `subcategories`), `CategorySummary`, `CategoryStatisticsResponse` | 9 |
| `inventory/` | `InventoryResponse`, `ProcurementRecommendationResponse` | 1 |
| `issue/` | `IssueRequest`, `IssueResponse` | 1 |
| `supplier/` | `SupplierRequest`/`Update`, `SupplierResponse`, `SupplierSummary`, `SupplierSearchResponse`, `SupplierStatisticsResponse`, `SupplierStatusUpdateRequest` | 2 |
| `supplierperformance/` | `SupplierPerformanceResponse` | 12 |
| `department/` | `DepartmentRequest`, `DepartmentResponse` | 3 |
| `budget/` | `DepartmentBudgetRequest`, `DepartmentBudgetResponse` | 11 |
| `purchaserequest/` | `PurchaseRequestRequest`/`UpdateRequest`, `PurchaseRequestResponse` (now includes `timeline`), item DTOs, `PurchaseRequestTimelineEntryResponse` | 3, ext. 17 |
| `approval/` | `ApprovalDecisionRequest`, `ApprovalHistoryResponse` | 4 |
| `purchaseorder/` | `PurchaseOrderCreateRequest`, `PurchaseOrderResponse`, item/timeline DTOs | 5 |
| `goodsreceipt/` | `GoodsReceiptCreateRequest`, `GoodsReceiptResponse`, item DTOs | 6 |
| `notification/` | `NotificationResponse`, `NotificationCountResponse` | 8 |
| `attachment/` | `AttachmentResponse` | 10 |
| `report/` | `ReportFilter`, `ReportResult<T>`, one `*ReportRow` per report | 7 |
| `analytics/` | `ChartDataPoint`, `TopProductResponse`, `DepartmentSpendingChartResponse` | 13 |
| `audit/` | `AuditLogResponse` | 18 |
| `dashboard/` | `DashboardResponse` (grown every phase), `TopSupplierResponse` | 1, ext. every phase |
| `common/` | `ErrorResponse`, `PagedResponse<T>` (Phase 15) | 1, 15 |

### 1.7 `security/` — unchanged since Phase 1

| File | Purpose |
|---|---|
| `JwtProvider.kt` | Generate/validate signed JWTs; extract claims |
| `JwtFilter.kt` | Runs once per request, populates `SecurityContextHolder` |
| `UserPrincipal.kt` | `UserDetails` implementation carrying `id`/`role`/`firstName`/`lastName`; every service that needs "who is calling right now" reads this via `SecurityContextHolder` |
| `CustomUserDetailsService.kt` | Loads `User` by email for Spring Security |

### 1.8 `exception/` — unchanged since Phase 1, reused by every module

| File | HTTP status | Used for |
|---|---|---|
| `GlobalExceptionHandler.kt` | - | `@RestControllerAdvice`, maps every exception to `ErrorResponse` |
| `ResourceNotFoundException.kt` | 404 | Any `getXEntityById` miss, including soft-deleted lookups |
| `BusinessException.kt` | 409 | Every workflow/business rule violation across all 18 phases |
| `ValidationException.kt` | 400 | Reserved for hand-rolled validation beyond Bean Validation |
| `UnauthorizedException.kt` | 401 | Reserved for custom auth failures |

### 1.9 `util/` — new in Phase 7/15

| File | Purpose |
|---|---|
| `CsvWriter.kt` | Dependency-free CSV generation used by `ReportController` (Phase 7) |
| `PaginationUtil.kt` | In-memory sort+paginate helper shared by `ProductService`, `SupplierService`, `PurchaseRequestService`, `PurchaseOrderService` (Phase 15) |

### 1.10 `resources/`

| File | Purpose |
|---|---|
| `application.yml` | Mongo URI, port, JWT secret/expiry, `app.upload-dir` (Phase 10), Swagger paths |
| `application-dev.yml` | Verbose logging override |
| `application-prod.yml` | Production overrides (env-only Mongo URI, minimal error detail) |

### 1.11 End-to-end request example (Phase 11 budget reservation)

`POST /api/approvals/{prId}/procurement-manager` with `{ decision: APPROVED }`, where this is the final required approval level:
1. `JwtFilter` validates the token.
2. `ApprovalController` checks `@PreAuthorize`, calls `ApprovalService.decide(...)`.
3. `ApprovalService` loads the `PurchaseRequest` (via `PurchaseRequestService`), validates the decision is legal, asks `BudgetService.isBudgetExceeded(...)` to decide whether Finance is still required, determines this is the final stage, sets `status = APPROVED`.
4. `ApprovalService` calls `BudgetService.reserve(department, estimatedTotal)` — `BudgetService` loads the department's `DepartmentBudget` (via `DepartmentBudgetRepository`) and increments `reservedAmount`.
5. `ApprovalService` calls `AuditLogService.log(...)` and `NotificationService.notify(...)` (the requesting employee).
6. Controller returns `200` with the `ApprovalHistoryResponse`.

Later, `POST /api/goods-receipts/purchase-order/{poId}` that fully completes the resulting PO calls `BudgetService.spend(department, reservedAmountToRelease, actualAmountSpent)`, moving that same amount from `reservedAmount` to `spentAmount`.

---

## PART 2 — FRONTEND (recommended structure)

Not yet built by this handoff. Structure recommended so a frontend developer's layout stays 1:1 with backend module boundaries.

```
frontend/
├── src/
│   ├── api/                  one file per backend controller (see FRONTEND_HANDOFF.md §6 table)
│   ├── components/
│   │   ├── common/            Table, Modal, Pagination, StatusBadge, ConfirmDialog, Toast,
│   │   │                      EmptyState, LoadingSkeleton, FilterBar, DateRangePicker
│   │   ├── layout/             AppShell, Sidebar, Navbar, NotificationBell, RoleGuard
│   │   ├── charts/              ChartCard (Dashboard + Analytics pages both reuse this)
│   │   └── domain/              ApprovalTimeline, PurchaseOrderTimeline, AttachmentList,
│   │                            AttachmentUploader, BudgetUtilizationBar, CategoryTree,
│   │                            SupplierDropdown, ProductDropdown, DepartmentDropdown
│   ├── pages/                 Login, Dashboard, Products, Categories, Inventory, Issues,
│   │                          Suppliers, Departments, Budgets, PurchaseRequests, Approvals,
│   │                          PurchaseOrders, GoodsReceipts, Reports, Analytics,
│   │                          Notifications, AuditLogs, Users — one folder each,
│   │                          List/Detail/Form subcomponents as needed
│   ├── hooks/                  useAuth, usePagination, useNotifications, useDebouncedSearch
│   ├── context/                 AuthContext, NotificationContext
│   └── utils/                   constants, validators, dateUtils, csvDownload
└── README.md
```

Full page-by-page and component-level guidance lives in `FRONTEND_HANDOFF.md` §§4-6 — this file only maps structure, not behavior.

---

## PART 3 — Relationship Summary

- **model** defines what's stored → **repository** stores/retrieves it → **service** enforces every rule → **controller** exposes it over HTTP → **dto** shapes the HTTP boundary → **security** gates who may call it → **exception** standardizes everything that goes wrong → **util** holds small, dependency-free cross-cutting helpers (CSV, pagination) that any service can call without creating a circular dependency.
- Cross-cutting services (`NotificationService`, `AuditLogService`, `BudgetService`) are called *into* by the workflow services (`PurchaseRequestService`, `ApprovalService`, `PurchaseOrderService`, `GoodsReceiptService`, `StockIssueService`, `SupplierService`) — never the other way around. This keeps the dependency graph a DAG with no cycles; see README §4 for the diagram.
- The one architectural invariant preserved through all 18 phases: **only `GoodsReceiptService` increases `Product.currentStock`.** Anyone extending this system must preserve that invariant or explicitly document why a new exception is justified.
