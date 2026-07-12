# SYSTEM_REPOTREE.md — Complete Architecture Reference

This document is the single reference for the entire Procurement Management System's file/folder structure — backend (as built) and frontend (as recommended). It explains what every folder and file is for, and how they relate to each other. Use this alongside `README.md` (business/API reference) and `FRONTEND_HANDOFF.md` (API contract + UI guidance).

---

## PART 1 — BACKEND

```
backend/
├── build.gradle.kts            Gradle Kotlin DSL build script: dependencies, plugins, JVM target
├── settings.gradle.kts         Gradle root project name
├── gradle.properties           Kotlin/Gradle global properties
├── .env.example                Template for local environment variables (Mongo URI, JWT secret, port)
├── .gitignore                  Standard Kotlin/Gradle/IDE ignore rules
├── postman_collection.json     Importable Postman collection covering every endpoint, all 6 phases
├── README.md                   Business + API + architecture reference (this system's "what and why")
├── FRONTEND_HANDOFF.md         Complete API contract + UI guidance for frontend developers
├── SYSTEM_REPOTREE.md          This file — the "where is everything" reference
└── src/main/
    ├── kotlin/com/company/procurement/     all application code lives under this package root
    └── resources/                          Spring configuration files (see Part 1.9)
```

### 1.1 `ProcurementApplication.kt`
The single `@SpringBootApplication` entry point. `fun main()` calls `runApplication`. Nothing else lives here — all real logic is in the packages below.

### 1.2 `config/` — Application-level configuration, not business logic
| File | Purpose |
|---|---|
| `SecurityConfig.kt` | Defines the `SecurityFilterChain`: which routes are public (`/api/auth/login`, Swagger), CORS policy, password encoder bean, `AuthenticationManager` bean, and wires `JwtFilter` into the filter chain. Method-level role checks (`@PreAuthorize`) live on individual controllers, not here — this file only handles authentication, not per-endpoint authorization. |
| `SwaggerConfig.kt` | Builds the OpenAPI bean: title, description, and the `bearerAuth` security scheme used by every controller's `@SecurityRequirement`. |
| `DataSeeder.kt` | `CommandLineRunner` that seeds demo data on startup: users (one per role), departments, suppliers, products, and a complete Purchase Request to Approval to Purchase Order to Goods Receipt workflow demonstration. Every seed method checks `repository.count() > 0` (or looks up by unique field) first, so re-running the app never creates duplicates. |

### 1.3 `controller/` — REST endpoints only
Every controller follows the same shape: `@RestController`, `@RequestMapping("/api/...")`, `@Tag` for Swagger grouping, `@SecurityRequirement(name = "bearerAuth")` at the class level, and `@PreAuthorize` per method for role enforcement. Controllers never contain business logic — they validate (`@Valid`), delegate to exactly one service method, and wrap the result in a `ResponseEntity` with the correct HTTP status.

| Controller | Phase | Base path |
|---|---|---|
| `AuthController.kt` | 1 | `/api/auth` |
| `UserController.kt` | 1 | `/api/users` |
| `ProductController.kt` | 1 (extended in 2) | `/api/products` |
| `InventoryController.kt` | 1 | `/api/inventory` |
| `StockIssueController.kt` | 1 | `/api/issues` |
| `DashboardController.kt` | 1 (extended in 2-6) | `/api/dashboard` |
| `SupplierController.kt` | 2 | `/api/suppliers` |
| `DepartmentController.kt` | 3 | `/api/departments` |
| `PurchaseRequestController.kt` | 3 | `/api/purchase-requests` |
| `ApprovalController.kt` | 4 | `/api/approvals` |
| `PurchaseOrderController.kt` | 5 | `/api/purchase-orders` |
| `GoodsReceiptController.kt` | 6 | `/api/goods-receipts` |

### 1.4 `service/` — All business logic
This is where every rule in the system actually lives: validation beyond simple field checks, workflow transitions, calculations, and cross-entity coordination. Services depend on repositories and on each other (never the reverse, and never a circular dependency between two services).

| Service | Depends on (other services) | Core responsibility |
|---|---|---|
| `AuthService.kt` | - | Authenticates credentials via Spring Security, issues JWT |
| `UserService.kt` | - | User CRUD, password hashing |
| `ProductService.kt` | `SupplierService` | Product CRUD; validates `supplierId` exists; embeds `SupplierSummary` in responses |
| `InventoryService.kt` | - | Read-only views over Product stock/status; procurement recommendations |
| `StockIssueService.kt` | `ProductService` | Issues/returns stock to employees (the only module that decreases stock) |
| `DashboardService.kt` | - (reads repositories directly) | Aggregates statistics across every module |
| `SupplierService.kt` | - | Supplier CRUD, auto-generated codes, activation/deactivation, search |
| `DepartmentService.kt` | - | Department CRUD |
| `PurchaseRequestService.kt` | `ProductService` | PR CRUD, auto PR numbers, submit/cancel, the `FINANCE_APPROVAL_THRESHOLD` constant |
| `ApprovalService.kt` | `PurchaseRequestService` | Approval decisions, workflow-level advancement, ADMIN override, history |
| `PurchaseOrderService.kt` | `PurchaseRequestService`, `ProductService`, `SupplierService` | Converts approved PRs into POs, computes totals, status lifecycle, timeline |
| `GoodsReceiptService.kt` | `PurchaseOrderService`, `ProductService` | The only service that increases `Product.currentStock`. Records receipts, updates PO received quantities, closes POs |

### 1.5 `repository/` — Spring Data MongoDB interfaces
Thin interfaces extending `MongoRepository<T, String>` with derived query methods (`findByX`, `existsByX`, `countByX`). No logic here — Spring Data generates the implementation.

| Repository | Collection |
|---|---|
| `UserRepository.kt` | `users` |
| `ProductRepository.kt` | `products` |
| `StockIssueRepository.kt` | `stockIssues` |
| `SupplierRepository.kt` | `suppliers` |
| `DepartmentRepository.kt` | `departments` |
| `PurchaseRequestRepository.kt` | `purchaseRequests` |
| `ApprovalHistoryRepository.kt` | `approvalHistories` |
| `PurchaseOrderRepository.kt` | `purchaseOrders` |
| `GoodsReceiptRepository.kt` | `goodsReceipts` |

### 1.6 `model/` — MongoDB documents (the persistence layer's shape)
| File | Notes |
|---|---|
| `User.kt`, `Role.kt` | `Role` has 5 values: `ADMIN`, `STORE_MANAGER`, `PROCUREMENT_MANAGER`, `FINANCE_MANAGER`, `EMPLOYEE` |
| `Product.kt`, `ProductStatus.kt` | `Product.supplierId` (Phase 2) links to `Supplier`; `deriveStatus()` companion function computes `IN_STOCK`/`LOW_STOCK`/`OUT_OF_STOCK` |
| `StockIssue.kt`, `IssueStatus.kt` | Records who has what stock currently issued |
| `Supplier.kt`, `SupplierStatus.kt` | Supplier master data |
| `Department.kt` | Simple master-data entity used by Purchase Requests |
| `PurchaseRequest.kt` + `PurchaseRequestItem.kt` (embedded) + `PurchaseRequestStatus.kt` + `Priority.kt` | `items` is an embedded list — a snapshot of requested products, independent of later Product changes. `currentApprovalLevel: ApprovalLevel?` tracks workflow position |
| `ApprovalHistory.kt` + `ApprovalLevel.kt` + `ApprovalDecision.kt` | One document per decision — an append-only audit trail, never updated after creation |
| `PurchaseOrder.kt` + `PurchaseOrderItem.kt` (embedded, computed `lineSubtotal`/`lineTax`/`lineTotal`) + `PurchaseOrderTimelineEntry.kt` (embedded) + `PurchaseOrderStatus.kt` | `PurchaseOrderItem.receivedQuantity` accumulates across multiple Goods Receipts, enabling partial delivery tracking |
| `GoodsReceipt.kt` + `GoodsReceiptItem.kt` (embedded, computed `acceptedQuantity`) + `GoodsReceiptStatus.kt` + `InspectionStatus.kt` | One document per delivery event; supports multiple GRNs per PO |

### 1.7 `dto/` — Request/response shapes, one subfolder per module
DTOs never leak MongoDB `@Id`/annotations to the API surface, and never let the API dictate internal model shape. Every subfolder mirrors its model's module:

| Subfolder | Key types |
|---|---|
| `auth/` | `LoginRequest`, `LoginResponse` |
| `user/` | `UserRequest`, `UserResponse` |
| `product/` | `ProductRequest` (requires `supplierId`), `ProductResponse` (embeds `SupplierSummary`) |
| `inventory/` | `InventoryResponse`, `ProcurementRecommendationResponse` |
| `issue/` | `IssueRequest`, `IssueResponse` |
| `dashboard/` | `DashboardResponse` (grown every phase — see README section 15), `TopSupplierResponse` |
| `supplier/` | `SupplierRequest`, `SupplierUpdateRequest`, `SupplierResponse`, `SupplierSummary` (embedded in `ProductResponse`), `SupplierSearchResponse`, `SupplierStatisticsResponse`, `SupplierStatusUpdateRequest` |
| `department/` | `DepartmentRequest`, `DepartmentResponse` |
| `purchaserequest/` | `PurchaseRequestRequest`, `PurchaseRequestUpdateRequest`, `PurchaseRequestResponse`, `PurchaseRequestItemRequest`, `PurchaseRequestItemResponse` |
| `approval/` | `ApprovalDecisionRequest`, `ApprovalHistoryResponse` |
| `purchaseorder/` | `PurchaseOrderCreateRequest`, `PurchaseOrderItemInput`, `PurchaseOrderResponse`, `PurchaseOrderItemResponse`, `PurchaseOrderTimelineEntryResponse` |
| `goodsreceipt/` | `GoodsReceiptCreateRequest`, `GoodsReceiptItemInput`, `GoodsReceiptResponse`, `GoodsReceiptItemResponse` |
| `common/` | `ErrorResponse` (used by `GlobalExceptionHandler` for every error), `PagedResponse<T>` (reserved for future paginated endpoints) |

### 1.8 `security/` — Authentication mechanics (unchanged since Phase 1)
| File | Purpose |
|---|---|
| `JwtProvider.kt` | Generates and validates signed JWTs; extracts claims (`userId`, `role`, email as subject) |
| `JwtFilter.kt` | `OncePerRequestFilter` — runs on every request, validates the Bearer token, populates `SecurityContextHolder` |
| `UserPrincipal.kt` | Implements `UserDetails`; carries `id`, `role`, `firstName`, `lastName` so services can read the current actor via `SecurityContextHolder` without a database round-trip |
| `CustomUserDetailsService.kt` | Loads a `User` by email and wraps it in a `UserPrincipal` for Spring Security |

### 1.9 `exception/` — Centralized error handling (unchanged since Phase 1, reused by every new module)
| File | Purpose |
|---|---|
| `GlobalExceptionHandler.kt` | `@RestControllerAdvice` mapping every exception type to a standardized `ErrorResponse` and HTTP status |
| `ResourceNotFoundException.kt` | to 404. Thrown by every service's `getXEntityById` when an id doesn't exist |
| `BusinessException.kt` | to 409. Thrown for every workflow/business rule violation across all 6 phases (duplicate supplier, wrong approval level, insufficient stock, over-receiving, etc.) |
| `ValidationException.kt` | to 400. Reserved for validation failures not already caught by Bean Validation |
| `UnauthorizedException.kt` | to 401. Reserved for custom auth failures beyond Spring Security's own handling |

### 1.10 `resources/` — Spring configuration
| File | Purpose |
|---|---|
| `application.yml` | Base config: Mongo URI, server port, JWT secret/expiry, Swagger paths, active profile |
| `application-dev.yml` | Local development overrides (verbose logging) |
| `application-prod.yml` | Production overrides (Mongo URI must come from environment, minimal error detail in responses) |

### 1.11 How a request flows through these folders (end-to-end example)
`POST /api/goods-receipts/purchase-order/{poId}` (recording a delivery):
1. `JwtFilter` (security/) validates the token, populates `SecurityContextHolder`.
2. `GoodsReceiptController` (controller/) checks `@PreAuthorize`, validates `GoodsReceiptCreateRequest` (dto/goodsreceipt/), calls `GoodsReceiptService`.
3. `GoodsReceiptService` (service/) loads the `PurchaseOrder` via `PurchaseOrderService`, validates status and quantities, loads and updates each `Product` via `ProductService` (model/, repository/), builds and saves a `GoodsReceipt`, updates and saves the `PurchaseOrder`.
4. Any rule violation throws `BusinessException` or `ResourceNotFoundException` (exception/), caught by `GlobalExceptionHandler` and returned as a standardized `ErrorResponse` (dto/common/).
5. On success, the controller returns `201 Created` with a `GoodsReceiptResponse`.

---

## PART 2 — FRONTEND (recommended structure)

The frontend is not yet built by this handoff — this section documents the recommended structure so a frontend developer's file layout stays consistent with the backend's module boundaries.

```
frontend/
├── index.html, login.html, dashboard.html, products.html, inventory.html,
│   issues.html, users.html, suppliers.html, supplier-form.html,
│   departments.html, purchase-requests.html, purchase-request-form.html,
│   approvals.html, purchase-orders.html, purchase-order-form.html,
│   goods-receipts.html, goods-receipt-form.html
│       One page per screen in the hierarchy documented in FRONTEND_HANDOFF.md section 4.
│
├── assets/
│   ├── images/     Logo, avatars
│   ├── icons/       One icon per nav item (inventory, users, dashboard, suppliers,
│   │                 departments, purchase-requests, approvals, purchase-orders, goods-receipts)
│   └── fonts/
│
├── css/            One stylesheet per page, plus global.css for shared tokens
│                    (colors, spacing, status-badge palette from FRONTEND_HANDOFF.md section 6)
│
├── js/
│   ├── config/apiConfig.js        Base URL, shared fetch wrapper
│   ├── auth/                      login.js, logout.js, authGuard.js, roleGuard.js
│   │                              (roleGuard.js now checks 5 roles, not 3)
│   ├── api/                       One file per backend module, mirroring dto/ subfolders:
│   │   ├── authApi.js, productApi.js, inventoryApi.js, issueApi.js, userApi.js, dashboardApi.js
│   │   ├── supplierApi.js                    (Phase 2)
│   │   ├── departmentApi.js                  (Phase 3)
│   │   ├── purchaseRequestApi.js              (Phase 3)
│   │   ├── approvalApi.js                     (Phase 4)
│   │   ├── purchaseOrderApi.js                (Phase 5)
│   │   └── goodsReceiptApi.js                 (Phase 6)
│   ├── pages/                     One controller module per page, matching the .html files above
│   ├── components/                navbar.js, sidebar.js, modal.js, table.js, alerts.js,
│   │                               supplierDropdown.js, productDropdown.js, departmentDropdown.js,
│   │                               statusBadge.js, approvalTimeline.js, poTimeline.js
│   └── utils/                     helpers.js, constants.js (status enums, role list, badge colors),
│                                   validators.js, dateUtils.js
│
└── README.md
```

### Frontend-to-backend module mapping
| Frontend `js/api/*.js` | Backend controller | Backend DTOs it must know about |
|---|---|---|
| `authApi.js` | `AuthController` | `LoginRequest`/`LoginResponse` |
| `productApi.js` | `ProductController` | `ProductRequest`/`ProductResponse` (embeds `SupplierSummary`) |
| `supplierApi.js` | `SupplierController` | `SupplierRequest`/`SupplierUpdateRequest`/`SupplierResponse`/`SupplierSearchResponse`/`SupplierStatisticsResponse` |
| `departmentApi.js` | `DepartmentController` | `DepartmentRequest`/`DepartmentResponse` |
| `purchaseRequestApi.js` | `PurchaseRequestController` | `PurchaseRequestRequest`/`PurchaseRequestUpdateRequest`/`PurchaseRequestResponse` |
| `approvalApi.js` | `ApprovalController` | `ApprovalDecisionRequest`/`ApprovalHistoryResponse` |
| `purchaseOrderApi.js` | `PurchaseOrderController` | `PurchaseOrderCreateRequest`/`PurchaseOrderResponse` |
| `goodsReceiptApi.js` | `GoodsReceiptController` | `GoodsReceiptCreateRequest`/`GoodsReceiptResponse` |
| `dashboardApi.js` | `DashboardController` | `DashboardResponse` |

Keeping this 1:1 mapping between `js/api/*.js` files and backend controllers is what lets a frontend developer find the right file instantly when a backend endpoint changes.

---

## PART 3 — File Relationship Summary (the short version)

- **model** defines what's stored, **repository** stores/retrieves it, **service** enforces the rules around it, **controller** exposes it over HTTP, **dto** shapes what crosses that HTTP boundary, **security** decides who's allowed to make the request at all, **exception** catches everything that goes wrong along the way and standardizes it.
- Every new phase (3 through 6) added exactly one new row to each of these seven folders — no existing file's responsibility changed; only `Product`, `ProductRequest`, `ProductResponse`, `ProductService`, and `DashboardResponse`/`DashboardService` were extended in place to link in the new modules (supplier reference, dashboard metrics).
- The one deliberate architectural rule enforced across the whole codebase: only `GoodsReceiptService` ever increases `Product.currentStock`. Anyone extending this system (e.g. adding a "Stock Adjustment" or "Return to Supplier" module) must preserve that invariant or explicitly document why a new exception is justified.
