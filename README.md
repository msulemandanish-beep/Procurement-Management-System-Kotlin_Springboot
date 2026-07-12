# Procurement Management System

A production-ready, enterprise-grade procurement backend built with Kotlin, Spring Boot, and MongoDB. The system covers the full procurement lifecycle — from inventory and supplier management through purchase requests, multi-level approvals, purchase orders, and goods receipt — with JWT authentication, role-based access control, and complete OpenAPI documentation.

## 1. Project Overview

This service is the backend for a Procurement Management System designed for medium and large organizations. It has been built incrementally across six phases, and every phase remains fully functional and backward compatible with the ones before it:

| Phase | Module | Status |
|---|---|---|
| 1 | Inventory Management (Users, Products, Inventory, Stock Issues, Dashboard) | ✅ Complete |
| 2 | Supplier Management | ✅ Complete |
| 3 | Purchase Request Management | ✅ Complete |
| 4 | Approval Workflow | ✅ Complete |
| 5 | Purchase Order Management | ✅ Complete |
| 6 | Goods Receipt (GRN) | ✅ Complete |

### The end-to-end business flow

```
Supplier ──▶ Product ──▶ Inventory
                              │
                              ▼
                    Purchase Request (Employee)
                              │
                              ▼
                    Approval Workflow (multi-level)
                              │
                              ▼
                    Purchase Order (to Supplier)
                              │
                              ▼
                    Goods Receipt (GRN)
                              │
                              ▼
                    Inventory stock increased
```

**Critical invariant: Goods Receipt is the ONLY module in the entire system that increases `Product.currentStock`.** Every other module — Purchase Requests, Approvals, Purchase Orders — only plans, approves, and orders. Stock Issue (Phase 1) is still the only module that *decreases* stock.

## 2. Features

**Phase 1 — Inventory Management**
- JWT-based authentication with BCrypt password hashing
- Full CRUD for Users and Products
- Automatic stock status derivation (`IN_STOCK`, `LOW_STOCK`, `OUT_OF_STOCK`)
- Stock issuance and return workflow with automatic stock adjustment
- Procurement recommendation engine (products below minimum stock)

**Phase 2 — Supplier Management**
- Supplier CRUD with auto-generated supplier codes (`SUP-0001`, ...)
- Activation/deactivation, search, and statistics
- Every product links to a preferred supplier; product responses embed a supplier summary

**Phase 3 — Purchase Request Management**
- Employees create, update, submit, and cancel purchase requests with multiple line items
- Auto-generated PR numbers (`PR-0001`, ...)
- Search/filter by status, employee, department, and priority
- Priority levels including `EMERGENCY`, which bypasses the normal approval workflow

**Phase 4 — Approval Workflow**
- Configurable multi-level sequence: Store Manager → Procurement Manager → Finance Manager
- Finance Manager approval is only required above a configurable high-value threshold
- Full approval history with approver identity, decision, comments, and timestamp
- ADMIN override to approve immediately, bypassing the normal sequence
- Guards against duplicate approvals and approving cancelled requests

**Phase 5 — Purchase Order Management**
- Converts an APPROVED purchase request into a Purchase Order
- Auto-generated PO numbers (`PO-0001`, ...)
- Supplier is derived automatically from the ordered product; ADMIN can override it
- Automatic subtotal, tax, discount, and grand total calculation
- Full status lifecycle (`DRAFT` → `ISSUED`/`EMAIL_SENT` → `PARTIALLY_RECEIVED`/`COMPLETED`, or `CANCELLED`) with a timeline of every transition

**Phase 6 — Goods Receipt (GRN)**
- Records goods received against a Purchase Order, including batch numbers, serial numbers, expiry dates, warehouse, and storage location
- Supports partial deliveries and multiple receipts against a single Purchase Order
- Automatically increases inventory for accepted quantities and closes the Purchase Order once fully received
- Inspection status and quality notes for basic quality control

**Cross-cutting**
- Role-based access control across five roles: `ADMIN`, `STORE_MANAGER`, `PROCUREMENT_MANAGER`, `FINANCE_MANAGER`, `EMPLOYEE`
- Centralized exception handling with standardized error responses
- Request validation via Bean Validation throughout
- OpenAPI/Swagger documentation for every endpoint
- Startup seed data demonstrating the complete procurement workflow end-to-end

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

## 4. Architecture

Clean, layered architecture — each layer only depends on the layer beneath it. This has not changed since Phase 1; every new module (Purchase Requests, Approvals, Purchase Orders, Goods Receipt) was added using the exact same pattern.

```
Controller  →  Service  →  Repository  →  MongoDB
     ↑             ↑
   DTOs        Domain Models
     ↑
 Security (JwtFilter → SecurityContext)
     ↑
GlobalExceptionHandler (cross-cutting)
```

```
                ┌────────────────────┐
   HTTP Request │   JwtFilter        │  validates Bearer token,
                │  (Security Filter) │  populates SecurityContext
                └─────────┬──────────┘
                          ▼
                ┌────────────────────┐
                │    Controller      │  validates request DTO,
                │  (@RestController) │  delegates to Service,
                │                    │  enforces @PreAuthorize role checks
                └─────────┬──────────┘
                          ▼
                ┌────────────────────┐
                │      Service       │  business logic, workflow rules,
                │                    │  domain exceptions
                └─────────┬──────────┘
                          ▼
                ┌────────────────────┐
                │    Repository      │  MongoRepository interfaces
                │ (Spring Data Mongo)│
                └─────────┬──────────┘
                          ▼
                   ┌─────────────┐
                   │   MongoDB   │
                   └─────────────┘
```

### Database relationships (full procurement lifecycle)

```
Supplier ──supplies──▶ Product ──has──▶ Inventory (stock levels)
                                              │
                                              ▼
                                   Purchase Request (Employee)
                                              │
                                     Approval Workflow
                                    (STORE_MANAGER → PROCUREMENT_MANAGER
                                     → FINANCE_MANAGER if high value)
                                              │
                                              ▼
                                     Purchase Order ──▶ Supplier
                                              │
                                              ▼
                                     Goods Receipt (GRN)
                                              │
                                              ▼
                                  Inventory stock increased
                                  (ONLY module allowed to do this)
```

- `Product.supplierId` references the preferred supplier for that product; `ProductService` validates the supplier exists and embeds a `SupplierSummary` in every `ProductResponse`.
- `PurchaseRequest.items` embeds a snapshot of each requested product (id, name, quantity, estimated price) — the request survives even if the underlying product is later changed.
- `PurchaseRequest.currentApprovalLevel` tracks which role must act next; it is `null` once the request reaches a terminal state.
- `ApprovalHistory` is an append-only audit trail, one document per decision, linked to its `PurchaseRequest` by id.
- `PurchaseOrder.items` tracks `receivedQuantity` per line so partial deliveries and multiple Goods Receipts against a single PO are supported.
- `GoodsReceipt` is the only writer of `Product.currentStock` increases anywhere in the codebase — enforced by convention (only `GoodsReceiptService` calls `ProductService.saveProduct` with an increased stock value) and documented here as an architectural invariant for anyone extending the system.

## 5. Request Flow

1. Client sends a request with `Authorization: Bearer <token>` (except `/api/auth/login` and Swagger routes).
2. `JwtFilter` extracts and validates the token, loads the user via `CustomUserDetailsService`, and populates the `SecurityContext`.
3. `@PreAuthorize` annotations on controller methods enforce role-based access.
4. The controller validates the request body (`@Valid`) and calls the relevant service.
5. The service applies business rules, interacts with repositories, and returns a DTO.
6. Any exception thrown anywhere in the chain is caught by `GlobalExceptionHandler` and converted into a standardized `ErrorResponse`.

## 6. Authentication Flow

1. `POST /api/auth/login` with `email` and `password`.
2. `AuthService` delegates to Spring Security's `AuthenticationManager`, which uses `CustomUserDetailsService` and `BCryptPasswordEncoder` to verify credentials.
3. On success, `JwtProvider` issues a signed JWT containing `userId`, `role`, `firstName`, `lastName`, and standard claims (`sub`, `iat`, `exp`).
4. The client stores the token and sends it as `Authorization: Bearer <token>` on subsequent requests.
5. `JwtFilter` validates the token's signature and expiry on every request.

Authentication itself has not changed since Phase 1 — only the set of possible `role` values has grown (see Role Matrix below).

## 7. MongoDB Collections

| Collection | Purpose |
|---|---|
| `users` | Application users across all five roles |
| `products` | Product catalog with stock levels; each document references a `supplierId` |
| `stockIssues` | Records of stock issued to employees and returns |
| `suppliers` | Supplier master data (contact info, address, payment terms, status) |
| `departments` | Organizational departments used on purchase requests |
| `purchaseRequests` | Employee purchase requests, embedded line items, and workflow state |
| `approvalHistories` | Append-only audit trail of every approval decision |
| `purchaseOrders` | Purchase orders issued to suppliers, embedded line items and status timeline |
| `goodsReceipts` | Records of goods received against purchase orders |

## 8. Folder Structure

```
backend/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── .env.example
├── .gitignore
├── postman_collection.json
├── README.md
├── FRONTEND_HANDOFF.md
├── SYSTEM_REPOTREE.md
└── src/main/
    ├── kotlin/com/company/procurement/
    │   ├── ProcurementApplication.kt
    │   ├── config/
    │   │   ├── SecurityConfig.kt
    │   │   ├── SwaggerConfig.kt
    │   │   └── DataSeeder.kt
    │   ├── controller/
    │   │   ├── AuthController.kt
    │   │   ├── UserController.kt
    │   │   ├── ProductController.kt
    │   │   ├── InventoryController.kt
    │   │   ├── StockIssueController.kt
    │   │   ├── DashboardController.kt
    │   │   ├── SupplierController.kt
    │   │   ├── DepartmentController.kt
    │   │   ├── PurchaseRequestController.kt
    │   │   ├── ApprovalController.kt
    │   │   ├── PurchaseOrderController.kt
    │   │   └── GoodsReceiptController.kt
    │   ├── service/
    │   │   ├── AuthService.kt
    │   │   ├── UserService.kt
    │   │   ├── ProductService.kt
    │   │   ├── InventoryService.kt
    │   │   ├── StockIssueService.kt
    │   │   ├── DashboardService.kt
    │   │   ├── SupplierService.kt
    │   │   ├── DepartmentService.kt
    │   │   ├── PurchaseRequestService.kt
    │   │   ├── ApprovalService.kt
    │   │   ├── PurchaseOrderService.kt
    │   │   └── GoodsReceiptService.kt
    │   ├── repository/
    │   │   ├── UserRepository.kt
    │   │   ├── ProductRepository.kt
    │   │   ├── StockIssueRepository.kt
    │   │   ├── SupplierRepository.kt
    │   │   ├── DepartmentRepository.kt
    │   │   ├── PurchaseRequestRepository.kt
    │   │   ├── ApprovalHistoryRepository.kt
    │   │   ├── PurchaseOrderRepository.kt
    │   │   └── GoodsReceiptRepository.kt
    │   ├── model/
    │   │   ├── User.kt, Role.kt
    │   │   ├── Product.kt, ProductStatus.kt
    │   │   ├── StockIssue.kt, IssueStatus.kt
    │   │   ├── Supplier.kt, SupplierStatus.kt
    │   │   ├── Department.kt
    │   │   ├── PurchaseRequest.kt, PurchaseRequestItem.kt, PurchaseRequestStatus.kt, Priority.kt
    │   │   ├── ApprovalHistory.kt, ApprovalLevel.kt, ApprovalDecision.kt
    │   │   ├── PurchaseOrder.kt, PurchaseOrderItem.kt, PurchaseOrderTimelineEntry.kt, PurchaseOrderStatus.kt
    │   │   └── GoodsReceipt.kt, GoodsReceiptItem.kt, GoodsReceiptStatus.kt, InspectionStatus.kt
    │   ├── dto/
    │   │   ├── auth/, user/, product/, inventory/, issue/, dashboard/, supplier/, common/
    │   │   ├── department/ (DepartmentRequest, DepartmentResponse)
    │   │   ├── purchaserequest/ (PurchaseRequestRequest, PurchaseRequestUpdateRequest, PurchaseRequestResponse, item DTOs)
    │   │   ├── approval/ (ApprovalDecisionRequest, ApprovalHistoryResponse)
    │   │   ├── purchaseorder/ (PurchaseOrderCreateRequest, PurchaseOrderResponse, item/timeline DTOs)
    │   │   └── goodsreceipt/ (GoodsReceiptCreateRequest, GoodsReceiptResponse, item DTOs)
    │   ├── security/
    │   │   ├── JwtProvider.kt
    │   │   ├── JwtFilter.kt
    │   │   ├── UserPrincipal.kt
    │   │   └── CustomUserDetailsService.kt
    │   └── exception/
    │       ├── GlobalExceptionHandler.kt
    │       ├── ResourceNotFoundException.kt
    │       ├── ValidationException.kt
    │       ├── BusinessException.kt
    │       └── UnauthorizedException.kt
    └── resources/
        ├── application.yml
        ├── application-dev.yml
        └── application-prod.yml
```

See `SYSTEM_REPOTREE.md` for a file-by-file explanation of the entire backend (and a suggested frontend structure).

## 9. Installation

### Prerequisites

- JDK 17+
- MongoDB 6.x+ (local install or Docker)
- Gradle 8.x (or use the Gradle wrapper once generated — see note below)

### MongoDB Setup

**Option A — Local install:** Install MongoDB Community Edition and ensure the service is running on port `27017`.

**Option B — Docker (recommended):**

```bash
docker run -d --name procurement-mongo -p 27017:27017 mongo:7
```

No manual schema setup is required — Spring Data MongoDB and the built-in `DataSeeder` create all nine collections and seed demo data automatically on first startup.

### Gradle Wrapper Note

This project ships without a pre-built `gradlew` binary wrapper (no network access was available to fetch the wrapper jar during generation). Before building, generate it once using a local Gradle install:

```bash
gradle wrapper --gradle-version 8.8
```

After that, use `./gradlew` for all subsequent commands. Alternatively, use your IDE's built-in Gradle support (IntelliJ IDEA detects `build.gradle.kts` automatically) or a system-installed `gradle` binary directly.

## 10. Configuration

Copy `.env.example` to `.env` (or export the variables in your shell) and adjust as needed:

```
MONGODB_URI=mongodb://localhost:27017/procurement_db
SERVER_PORT=8080
JWT_SECRET=<a long random secret, 256+ bits>
JWT_EXPIRATION_MS=86400000
SPRING_PROFILES_ACTIVE=dev
```

`application.yml` reads these via `${VAR:default}` placeholders, so the app runs with sane defaults even without a `.env` file for local development.

## 11. Build Instructions

```bash
cd backend
./gradlew clean build
```

This compiles the code, runs tests, and produces `build/libs/procurement-management-system-0.0.1-SNAPSHOT.jar`.

## 12. Run Instructions

**Via Gradle:**

```bash
./gradlew bootRun
```

**Via built JAR:**

```bash
java -jar build/libs/procurement-management-system-0.0.1-SNAPSHOT.jar
```

**With an explicit profile:**

```bash
java -jar build/libs/procurement-management-system-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

The API will be available at `http://localhost:8080`.

## 13. Swagger / OpenAPI

Once the app is running, open:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Raw OpenAPI spec: `http://localhost:8080/v3/api-docs`

Use the **Authorize** button in Swagger UI and paste your JWT (without the `Bearer ` prefix — Swagger adds it) to test secured endpoints interactively. Every endpoint across all six phases is documented, including request/response schemas and role requirements.

## 14. Seeded Test Accounts

| Role | Email | Password |
|---|---|---|
| ADMIN | admin@procurement.com | Admin@123 |
| STORE_MANAGER | storemanager@procurement.com | Manager@123 |
| PROCUREMENT_MANAGER | procurementmanager@procurement.com | Procurement@123 |
| FINANCE_MANAGER | financemanager@procurement.com | Finance@123 |
| EMPLOYEE | employee@procurement.com | Employee@123 |

### What's seeded

- 5 departments (IT, Procurement, Finance, Operations, HR)
- 5 suppliers (`SUP-0001`–`SUP-0005`)
- 7 products, each linked to a supplier
- A **complete procurement workflow demo**, safe to re-run (idempotent — re-running the app looks up existing data instead of duplicating it):
  - `PR-0001` → fully approved → converted to `PO-0001` → received via `GRN-0001` → **laptop stock increased from 0 to 3** (demonstrates the entire Phase 3–6 lifecycle)
  - `PR-0002` → `SUBMITTED`, awaiting Store Manager approval
  - `PR-0003` → `REJECTED` at the Store Manager stage
  - `PR-0004` → high-value request (`$9,500`), already approved by Store Manager and Procurement Manager, currently `UNDER_REVIEW` awaiting **Finance Manager** approval (demonstrates the value-based escalation rule)

## 15. API List

### Auth
| Method | Endpoint | Access |
|---|---|---|
| POST | `/api/auth/login` | Public |

### Users
| Method | Endpoint | Access |
|---|---|---|
| GET / GET {id} / POST / PUT {id} / DELETE {id} | `/api/users` | ADMIN |

### Products
| Method | Endpoint | Access |
|---|---|---|
| GET, GET {id} | `/api/products` | All roles |
| POST, PUT {id}, DELETE {id} | `/api/products` | ADMIN |

### Inventory
| Method | Endpoint | Access |
|---|---|---|
| GET `/api/inventory`, `/low-stock`, `/out-of-stock`, `/status`, `/procurement-recommendations` | ADMIN, STORE_MANAGER |

### Stock Issues
| Method | Endpoint | Access |
|---|---|---|
| GET `/api/issues`, `/history`; POST `/api/issues`; PUT `/api/issues/{id}/return` | ADMIN, STORE_MANAGER |

### Suppliers
| Method | Endpoint | Access |
|---|---|---|
| GET `/api/suppliers` (+ `/{id}`, `/code/{code}`, `/search`, `/active`, `/inactive`, `/statistics`) | ADMIN, STORE_MANAGER |
| POST, PUT {id}, DELETE {id}, PATCH `/activate`, PATCH `/deactivate` | ADMIN |

### Departments
| Method | Endpoint | Access |
|---|---|---|
| GET `/api/departments`, GET `/{id}` | All roles |
| POST, PUT {id}, DELETE {id} | ADMIN |

### Purchase Requests
| Method | Endpoint | Access |
|---|---|---|
| GET `/api/purchase-requests` | ADMIN, STORE_MANAGER, PROCUREMENT_MANAGER, FINANCE_MANAGER |
| GET `/api/purchase-requests/my-requests` | All roles |
| GET `/api/purchase-requests/{id}` | All roles |
| GET `/api/purchase-requests/search` | ADMIN, STORE_MANAGER, PROCUREMENT_MANAGER, FINANCE_MANAGER |
| POST `/api/purchase-requests` | All roles |
| PUT `/api/purchase-requests/{id}` | All roles (only while DRAFT) |
| PATCH `/api/purchase-requests/{id}/submit` | All roles |
| PATCH `/api/purchase-requests/{id}/cancel` | All roles |

### Approval Workflow
| Method | Endpoint | Access |
|---|---|---|
| GET `/api/approvals/{prId}/history` | All roles |
| POST `/api/approvals/{prId}/store-manager` | ADMIN, STORE_MANAGER |
| POST `/api/approvals/{prId}/procurement-manager` | ADMIN, PROCUREMENT_MANAGER |
| POST `/api/approvals/{prId}/finance-manager` | ADMIN, FINANCE_MANAGER |
| POST `/api/approvals/{prId}/override` | ADMIN |

### Purchase Orders
| Method | Endpoint | Access |
|---|---|---|
| GET `/api/purchase-orders` (+ `/{id}`, `/status/{status}`, `/supplier/{supplierId}`) | ADMIN, STORE_MANAGER, PROCUREMENT_MANAGER, FINANCE_MANAGER |
| POST `/api/purchase-orders/from-request/{prId}` | ADMIN, PROCUREMENT_MANAGER |
| PATCH `/api/purchase-orders/{id}/issue`, `/mark-email-sent`, `/cancel` | ADMIN, PROCUREMENT_MANAGER |

### Goods Receipt
| Method | Endpoint | Access |
|---|---|---|
| GET `/api/goods-receipts` (+ `/{id}`, `/purchase-order/{poId}`) | ADMIN, STORE_MANAGER, PROCUREMENT_MANAGER |
| POST `/api/goods-receipts/purchase-order/{poId}` | ADMIN, STORE_MANAGER |

### Dashboard
| Method | Endpoint | Access |
|---|---|---|
| GET `/api/dashboard` | ADMIN, STORE_MANAGER |

## 16. Sample Requests & Responses

### Login

```json
POST /api/auth/login
{ "email": "employee@procurement.com", "password": "Employee@123" }
```
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "userId": "665f1c2e8a1b2c3d4e5f6789",
  "email": "employee@procurement.com",
  "firstName": "Usman",
  "lastName": "Employee",
  "role": "EMPLOYEE"
}
```

### Create Purchase Request

```json
POST /api/purchase-requests
Authorization: Bearer <token>
{
  "department": "Information Technology",
  "items": [
    { "productId": "665f...", "requestedQuantity": 5, "estimatedUnitPrice": 65.0, "notes": "Backup drives" }
  ],
  "purpose": "Replenish backup storage devices",
  "businessJustification": "Current drives are near end-of-life",
  "priority": "MEDIUM",
  "requiredDate": "2026-12-31T00:00:00Z"
}
```
```json
{
  "id": "665f...",
  "prNumber": "PR-0005",
  "status": "DRAFT",
  "currentApprovalLevel": null,
  "estimatedTotal": 325.0,
  "items": [
    { "productId": "665f...", "productName": "External Hard Drive 1TB", "requestedQuantity": 5, "estimatedUnitPrice": 65.0, "estimatedLineTotal": 325.0, "notes": "Backup drives" }
  ],
  "createdBy": "employee@procurement.com",
  "createdAt": "2026-07-11T10:00:00Z",
  "updatedAt": "2026-07-11T10:00:00Z"
}
```

After creation, call `PATCH /api/purchase-requests/{id}/submit` to enter the approval workflow (`currentApprovalLevel` becomes `STORE_MANAGER`, unless priority is `EMERGENCY`, in which case it is auto-approved immediately).

### Approve at Store Manager level

```json
POST /api/approvals/{purchaseRequestId}/store-manager
Authorization: Bearer <storeManagerToken>
{ "decision": "APPROVED", "comments": "Confirmed need" }
```
```json
{
  "id": "665f...",
  "purchaseRequestId": "665f...",
  "prNumber": "PR-0005",
  "level": "STORE_MANAGER",
  "approverName": "Sara Manager",
  "decision": "APPROVED",
  "comments": "Confirmed need",
  "isOverride": false,
  "timestamp": "2026-07-11T10:05:00Z"
}
```

The purchase request's `currentApprovalLevel` automatically advances to `PROCUREMENT_MANAGER` (and further to `FINANCE_MANAGER` only if `estimatedTotal >= 5000`).

### Create Purchase Order from an approved request

```json
POST /api/purchase-orders/from-request/{purchaseRequestId}
Authorization: Bearer <procurementManagerToken>
{
  "items": [
    { "productId": "665f...", "orderedQuantity": 5, "unitPrice": 65.0, "taxRate": 5.0, "discount": 0.0 }
  ],
  "shipping": 15.0,
  "currency": "USD",
  "expectedDeliveryDate": "2026-12-31T00:00:00Z"
}
```
```json
{
  "id": "665f...",
  "poNumber": "PO-0002",
  "supplierName": "Computer World",
  "subtotal": 325.0,
  "taxTotal": 16.25,
  "discountTotal": 0.0,
  "shipping": 15.0,
  "grandTotal": 356.25,
  "status": "DRAFT",
  "timeline": [ { "status": "DRAFT", "remarks": "Purchase Order created from purchase request PR-0005", "...": "..." } ]
}
```

### Record a Goods Receipt

```json
POST /api/goods-receipts/purchase-order/{purchaseOrderId}
Authorization: Bearer <storeManagerToken>
{
  "items": [
    { "productId": "665f...", "receivedQuantity": 5, "rejectedQuantity": 0, "batchNumber": "BATCH-2026-010" }
  ],
  "warehouse": "Main Warehouse",
  "storageLocation": "Rack B2",
  "inspectionStatus": "PASSED"
}
```
```json
{
  "grnNumber": "GRN-0002",
  "poNumber": "PO-0002",
  "items": [
    { "productId": "665f...", "receivedQuantity": 5, "rejectedQuantity": 0, "acceptedQuantity": 5, "batchNumber": "BATCH-2026-010" }
  ],
  "status": "COMPLETED"
}
```

The associated Purchase Order automatically moves to `COMPLETED` (or `PARTIALLY_RECEIVED` if only some items/quantities were fulfilled), and `Product.currentStock` for the external hard drive increases by 5.

### Dashboard Statistics

```json
GET /api/dashboard
```
```json
{
  "totalProducts": 7,
  "totalInventoryItems": 229,
  "lowStockProducts": 2,
  "outOfStockProducts": 0,
  "totalIssuedProducts": 0,
  "productsNeedingPurchase": 2,
  "totalSuppliers": 5,
  "activeSuppliers": 5,
  "inactiveSuppliers": 0,
  "pendingPurchaseRequests": 1,
  "approvedPurchaseRequests": 1,
  "rejectedPurchaseRequests": 1,
  "itemsWaitingApproval": 1,
  "totalPurchaseOrders": 1,
  "pendingPurchaseOrders": 0,
  "completedPurchaseOrders": 1,
  "totalGoodsReceipts": 1,
  "pendingDeliveries": 0,
  "monthlyProcurementSpend": 2872.50,
  "inventoryValue": 24387.75,
  "topSuppliers": [
    { "supplierId": "665f...", "supplierName": "Tech Solutions Ltd", "totalPurchaseOrderValue": 2872.50, "purchaseOrderCount": 1 }
  ]
}
```

### Error Response Format

```json
{
  "timestamp": "2026-07-11T10:10:00Z",
  "status": 409,
  "error": "Conflict",
  "message": "Purchase request 'PR-0005' is not currently awaiting approval at level 'FINANCE_MANAGER'. It is awaiting: PROCUREMENT_MANAGER",
  "path": "/api/approvals/665f.../finance-manager",
  "details": []
}
```

## 17. Security Model

- Passwords are hashed with BCrypt before storage; plaintext passwords are never persisted or logged.
- Stateless JWT authentication — no server-side session state. Unchanged since Phase 1.
- `JwtFilter` runs once per request, validating signature and expiry before populating the Spring Security context.
- Method-level authorization via `@PreAuthorize("hasRole(...)")` / `hasAnyRole(...)` on every controller method, across all six phases.
- CORS is configured centrally in `SecurityConfig`.
- All error responses are standardized and never leak stack traces.
- `/api/auth/login` and Swagger routes are the only public endpoints; everything else requires a valid Bearer token.
- Workflow-level guards (in addition to role checks) prevent: approving a request at the wrong level, approving/rejecting an already-decided or cancelled request, converting anything but an APPROVED request into a Purchase Order, receiving goods against a non-issued Purchase Order, and over-receiving beyond the ordered quantity.

## 18. Role Matrix

| Capability | ADMIN | STORE_MANAGER | PROCUREMENT_MANAGER | FINANCE_MANAGER | EMPLOYEE |
|---|:---:|:---:|:---:|:---:|:---:|
| Manage Users | ✅ | ❌ | ❌ | ❌ | ❌ |
| Manage Products | ✅ | ❌ | ❌ | ❌ | ❌ |
| View Products | ✅ | ✅ | ✅ | ✅ | ✅ |
| Manage Inventory | ✅ | ✅ | ❌ | ❌ | ❌ |
| Manage Stock Issues | ✅ | ✅ | ❌ | ❌ | ❌ |
| Manage Suppliers | ✅ | ❌ | ❌ | ❌ | ❌ |
| View/Search Suppliers | ✅ | ✅ | ❌ | ❌ | ❌ |
| Manage Departments | ✅ | ❌ | ❌ | ❌ | ❌ |
| Create Purchase Request | ✅ | ✅ | ✅ | ✅ | ✅ |
| View Own Purchase Requests | ✅ | ✅ | ✅ | ✅ | ✅ |
| View All Purchase Requests | ✅ | ✅ | ✅ | ✅ | ❌ |
| Approve at Store Manager level | ✅ | ✅ | ❌ | ❌ | ❌ |
| Approve at Procurement Manager level | ✅ | ❌ | ✅ | ❌ | ❌ |
| Approve at Finance Manager level | ✅ | ❌ | ❌ | ✅ | ❌ |
| Admin Override Approval | ✅ | ❌ | ❌ | ❌ | ❌ |
| Create/Issue/Cancel Purchase Orders | ✅ | ❌ | ✅ | ❌ | ❌ |
| View Purchase Orders | ✅ | ✅ | ✅ | ✅ | ❌ |
| Record Goods Receipt | ✅ | ✅ | ❌ | ❌ | ❌ |
| View Goods Receipts | ✅ | ✅ | ✅ | ❌ | ❌ |
| View Dashboard | ✅ | ✅ | ❌ | ❌ | ❌ |
| View Own Profile | ✅ | ✅ | ✅ | ✅ | ✅ |

## 19. Future Roadmap

- ~~**Suppliers** module~~ ✅ Phase 2
- ~~**Purchase Requests**~~ ✅ Phase 3
- ~~**Approval Workflow**~~ ✅ Phase 4
- ~~**Purchase Orders**~~ ✅ Phase 5
- ~~**Goods Receipt (GRN)**~~ ✅ Phase 6
- **Warehouses & Locations** as first-class entities (currently free-text fields on Goods Receipt)
- **Categories & Subcategories** as first-class entities (currently a free-text field on Product)
- **Procurement budgets** with validation against departmental spend limits
- **Supplier performance metrics** — on-time delivery rate, rejection rate, average lead time, computed from Goods Receipt history
- **Multiple suppliers per product** with per-supplier pricing history
- **PDF generation** for printable Purchase Orders (the `PurchaseOrderResponse` DTO is already structured to support this directly)
- **Reports** — exportable procurement, spend, and supplier performance reports (CSV/PDF)
- **Notifications** — email/push notifications for pending approvals, low stock, and PO status changes
- **Attachments** on purchase requests and purchase orders (e.g. quotes, specifications)
- **Optimistic locking** (version field) once concurrent multi-approver editing becomes a real-world concern

The package structure (`config`, `controller`, `service`, `repository`, `model`, `dto`, `security`, `exception`) remains intentionally generic so each future item above can be added as new files within the same layers without refactoring existing code.
