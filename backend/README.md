# Procurement Management System — Phase 1 + Phase 2: Inventory & Supplier Management

A production-ready Kotlin + Spring Boot + MongoDB backend implementing the Inventory Management module (Phase 1) and the Supplier Management module (Phase 2) of a larger Procurement Management System. Built with clean architecture, JWT authentication, role-based access control, and full OpenAPI documentation.

## 1. Project Overview

This service manages users, products, inventory levels, stock issuance to employees, dashboard statistics, procurement recommendations, and — as of Phase 2 — suppliers. Every product now references a preferred supplier, so the dashboard, product catalog, and procurement recommendations all reflect where stock should be replenished from. The codebase is designed so future modules (Purchase Requests, Purchase Orders, Approvals, Reports, Notifications) can be added without restructuring existing code.

### What's new in Phase 2

- Full **Supplier Management** module: CRUD, activation/deactivation, search, and statistics
- Auto-generated, unique supplier codes (`SUP-0001`, `SUP-0002`, ...)
- `Product` now requires a `supplierId`; the API validates the supplier exists and embeds a lightweight `supplier` summary object in every `ProductResponse`
- Dashboard extended with `totalSuppliers`, `activeSuppliers`, and `inactiveSuppliers`
- Data seeder extended with 5 realistic suppliers, and all seeded products now reference one
- Postman collection extended with a "Supplier Management" folder
- All Phase 1 functionality (Auth, Users, Products, Inventory, Stock Issues, Dashboard) is unchanged and fully backward compatible aside from the new required `supplierId` field on Product create/update requests

## 2. Features

- JWT-based authentication with BCrypt password hashing
- Role-based access control: `ADMIN`, `STORE_MANAGER`, `EMPLOYEE`
- Full CRUD for Users, Products, and Suppliers
- Automatic stock status derivation (`IN_STOCK`, `LOW_STOCK`, `OUT_OF_STOCK`)
- Stock issuance and return workflow with automatic stock adjustment
- Supplier management with auto-generated supplier codes, activation/deactivation, and search
- Every product links to its preferred supplier; product responses embed a supplier summary
- Dashboard statistics aggregation, including supplier counts
- Procurement recommendation engine (products below minimum stock)
- Centralized exception handling with standardized error responses
- Request validation via Bean Validation
- OpenAPI/Swagger documentation for every endpoint
- Startup seed data (Admin, Store Manager, Employee, 5 suppliers, sample products linked to suppliers)

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

Clean, layered architecture — each layer only depends on the layer beneath it:

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
                │  (@RestController) │  delegates to Service
                └─────────┬──────────┘
                          ▼
                ┌────────────────────┐
                │      Service       │  business logic,
                │                    │  domain rules, exceptions
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

### Supplier ↔ Product ↔ Inventory relationship

Suppliers do **not** touch inventory quantities directly. The relationship is intentionally one-directional so future modules (Purchase Orders, Goods Receipt) can slot in cleanly:

```
Supplier ──supplies──▶ Product ──has──▶ Inventory (stock levels)
                                              │
                                              ▼
                          (future) Purchase Order ──▶ Goods Receipt ──▶ increases stock
```

- `Product.supplierId` references the preferred supplier for that product.
- `ProductService` validates the referenced supplier exists on every create/update and embeds a `SupplierSummary` (`id`, `supplierCode`, `companyName`) in every `ProductResponse` — the frontend never needs a second request just to show which supplier a product comes from.
- `InventoryService` and `StockIssueService` are untouched by this change — they continue to operate purely on stock quantities and know nothing about suppliers.
- Later, Purchase Orders will reference `supplierId` directly, and Goods Receipt will be the only thing that increases `Product.currentStock`.

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

## 7. MongoDB Collections

| Collection | Purpose |
|---|---|
| `users` | Application users (Admin, Store Manager, Employee) |
| `products` | Product catalog with stock levels; each document references a `supplierId` |
| `stockIssues` | Records of stock issued to employees and returns |
| `suppliers` | Supplier master data (contact info, address, payment terms, status) |

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
    │   │   └── SupplierController.kt
    │   ├── service/
    │   │   ├── AuthService.kt
    │   │   ├── UserService.kt
    │   │   ├── ProductService.kt
    │   │   ├── InventoryService.kt
    │   │   ├── StockIssueService.kt
    │   │   ├── DashboardService.kt
    │   │   └── SupplierService.kt
    │   ├── repository/
    │   │   ├── UserRepository.kt
    │   │   ├── ProductRepository.kt
    │   │   ├── StockIssueRepository.kt
    │   │   └── SupplierRepository.kt
    │   ├── model/
    │   │   ├── User.kt
    │   │   ├── Product.kt
    │   │   ├── StockIssue.kt
    │   │   ├── Supplier.kt
    │   │   ├── Role.kt
    │   │   ├── ProductStatus.kt
    │   │   ├── IssueStatus.kt
    │   │   └── SupplierStatus.kt
    │   ├── dto/
    │   │   ├── auth/ (LoginRequest, LoginResponse)
    │   │   ├── user/ (UserRequest, UserResponse)
    │   │   ├── product/ (ProductRequest, ProductResponse)
    │   │   ├── inventory/ (InventoryResponse, ProcurementRecommendationResponse)
    │   │   ├── issue/ (IssueRequest, IssueResponse)
    │   │   ├── dashboard/ (DashboardResponse)
    │   │   ├── supplier/ (SupplierRequest, SupplierUpdateRequest, SupplierResponse, SupplierSummary, SupplierStatusUpdateRequest, SupplierSearchResponse, SupplierStatisticsResponse)
    │   │   └── common/ (ErrorResponse)
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

No manual schema setup is required — Spring Data MongoDB and the built-in `DataSeeder` create collections and seed data automatically on first startup.

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

Use the **Authorize** button in Swagger UI and paste your JWT (without the `Bearer ` prefix — Swagger adds it) to test secured endpoints interactively.

## 14. Seeded Test Accounts

| Role | Email | Password |
|---|---|---|
| ADMIN | admin@procurement.com | Admin@123 |
| STORE_MANAGER | storemanager@procurement.com | Manager@123 |
| EMPLOYEE | employee@procurement.com | Employee@123 |

Seven sample products (including one out-of-stock and two low-stock items) are seeded automatically on first run, and five suppliers (`SUP-0001`–`SUP-0005`) are seeded before them so every product can reference a valid supplier:

| Code | Company Name |
|---|---|
| SUP-0001 | ABC Office Supplies |
| SUP-0002 | Tech Solutions Ltd |
| SUP-0003 | Global Stationers |
| SUP-0004 | Computer World |
| SUP-0005 | Prime Electronics |

Seeding is idempotent — re-running the app looks up suppliers by company name instead of creating duplicates, so restarts never produce duplicate suppliers or broken `supplierId` references.

## 15. API List

### Auth
| Method | Endpoint | Access |
|---|---|---|
| POST | `/api/auth/login` | Public |

### Users
| Method | Endpoint | Access |
|---|---|---|
| GET | `/api/users` | ADMIN |
| GET | `/api/users/{id}` | ADMIN |
| POST | `/api/users` | ADMIN |
| PUT | `/api/users/{id}` | ADMIN |
| DELETE | `/api/users/{id}` | ADMIN |

### Products
| Method | Endpoint | Access |
|---|---|---|
| GET | `/api/products` | ADMIN, STORE_MANAGER, EMPLOYEE |
| GET | `/api/products/{id}` | ADMIN, STORE_MANAGER, EMPLOYEE |
| POST | `/api/products` | ADMIN |
| PUT | `/api/products/{id}` | ADMIN |
| DELETE | `/api/products/{id}` | ADMIN |

### Inventory
| Method | Endpoint | Access |
|---|---|---|
| GET | `/api/inventory` | ADMIN, STORE_MANAGER |
| GET | `/api/inventory/low-stock` | ADMIN, STORE_MANAGER |
| GET | `/api/inventory/out-of-stock` | ADMIN, STORE_MANAGER |
| GET | `/api/inventory/status` | ADMIN, STORE_MANAGER |
| GET | `/api/inventory/procurement-recommendations` | ADMIN, STORE_MANAGER |

### Stock Issues
| Method | Endpoint | Access |
|---|---|---|
| GET | `/api/issues` | ADMIN, STORE_MANAGER |
| GET | `/api/issues/history` | ADMIN, STORE_MANAGER |
| POST | `/api/issues` | ADMIN, STORE_MANAGER |
| PUT | `/api/issues/{id}/return` | ADMIN, STORE_MANAGER |

### Dashboard
| Method | Endpoint | Access |
|---|---|---|
| GET | `/api/dashboard` | ADMIN, STORE_MANAGER |

### Suppliers
| Method | Endpoint | Access |
|---|---|---|
| GET | `/api/suppliers` | ADMIN, STORE_MANAGER |
| GET | `/api/suppliers/{id}` | ADMIN, STORE_MANAGER |
| GET | `/api/suppliers/code/{supplierCode}` | ADMIN, STORE_MANAGER |
| POST | `/api/suppliers` | ADMIN |
| PUT | `/api/suppliers/{id}` | ADMIN |
| DELETE | `/api/suppliers/{id}` | ADMIN |
| PATCH | `/api/suppliers/{id}/activate` | ADMIN |
| PATCH | `/api/suppliers/{id}/deactivate` | ADMIN |
| GET | `/api/suppliers/search?keyword=` | ADMIN, STORE_MANAGER |
| GET | `/api/suppliers/active` | ADMIN, STORE_MANAGER |
| GET | `/api/suppliers/inactive` | ADMIN, STORE_MANAGER |
| GET | `/api/suppliers/statistics` | ADMIN, STORE_MANAGER |

## 16. Sample Requests & Responses

### Login

Request:
```json
POST /api/auth/login
{
  "email": "admin@procurement.com",
  "password": "Admin@123"
}
```

Response:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "userId": "665f1c2e8a1b2c3d4e5f6789",
  "email": "admin@procurement.com",
  "firstName": "Ali",
  "lastName": "Admin",
  "role": "ADMIN"
}
```

### Create Product

Request:
```json
POST /api/products
Authorization: Bearer <token>
{
  "name": "Stapler",
  "description": "Heavy duty stapler",
  "category": "Office Supplies",
  "unitPrice": 4.5,
  "currentStock": 50,
  "minimumStock": 10,
  "supplierId": "665f1c2e8a1b2c3d4e5f6700"
}
```

Response:
```json
{
  "id": "665f1c2e8a1b2c3d4e5f679a",
  "name": "Stapler",
  "description": "Heavy duty stapler",
  "category": "Office Supplies",
  "unitPrice": 4.5,
  "currentStock": 50,
  "minimumStock": 10,
  "supplier": {
    "id": "665f1c2e8a1b2c3d4e5f6700",
    "supplierCode": "SUP-0001",
    "companyName": "ABC Office Supplies"
  },
  "status": "IN_STOCK",
  "createdAt": "2026-07-07T10:00:00Z",
  "updatedAt": "2026-07-07T10:00:00Z"
}
```

If `supplierId` does not reference an existing supplier, the API returns `404 Not Found` with message `Supplier not found with id: <id>`.

### Create Supplier

Request:
```json
POST /api/suppliers
Authorization: Bearer <token>
{
  "companyName": "Sunrise Traders",
  "contactPerson": "Kamran Malik",
  "email": "sales@sunrisetraders.com",
  "phone": "+92-42-9998877",
  "alternatePhone": "+92-300-1112233",
  "website": "https://sunrisetraders.com",
  "address": "10 Ferozepur Road",
  "city": "Lahore",
  "state": "Punjab",
  "country": "Pakistan",
  "postalCode": "54700",
  "taxNumber": "NTN-2000010",
  "paymentTerms": "Net 30",
  "deliveryLeadTime": 7,
  "notes": "Preferred supplier for office consumables"
}
```

Response:
```json
{
  "id": "665f1c2e8a1b2c3d4e5f6799",
  "supplierCode": "SUP-0006",
  "companyName": "Sunrise Traders",
  "contactPerson": "Kamran Malik",
  "email": "sales@sunrisetraders.com",
  "phone": "+92-42-9998877",
  "alternatePhone": "+92-300-1112233",
  "website": "https://sunrisetraders.com",
  "address": "10 Ferozepur Road",
  "city": "Lahore",
  "state": "Punjab",
  "country": "Pakistan",
  "postalCode": "54700",
  "taxNumber": "NTN-2000010",
  "paymentTerms": "Net 30",
  "deliveryLeadTime": 7,
  "notes": "Preferred supplier for office consumables",
  "status": "ACTIVE",
  "createdAt": "2026-07-07T10:00:00Z",
  "updatedAt": "2026-07-07T10:00:00Z"
}
```

`supplierCode` is auto-generated (`SUP-0001`, `SUP-0002`, ...) — do not send it in the request. Duplicate `companyName`, `email`, or `supplierCode` values return `409 Conflict`.

### Supplier Statistics

Request:
```
GET /api/suppliers/statistics
Authorization: Bearer <token>
```

Response:
```json
{
  "totalSuppliers": 6,
  "activeSuppliers": 5,
  "inactiveSuppliers": 1
}
```

### Issue Stock

Request:
```json
POST /api/issues
Authorization: Bearer <token>
{
  "productId": "665f1c2e8a1b2c3d4e5f679a",
  "employeeId": "665f1c2e8a1b2c3d4e5f6789",
  "quantity": 2
}
```

Response:
```json
{
  "id": "665f1c2e8a1b2c3d4e5f679b",
  "productId": "665f1c2e8a1b2c3d4e5f679a",
  "productName": "Stapler",
  "employeeId": "665f1c2e8a1b2c3d4e5f6789",
  "employeeName": "Usman Employee",
  "quantity": 2,
  "issueDate": "2026-07-07T10:05:00Z",
  "issuedBy": "storemanager@procurement.com",
  "status": "ISSUED",
  "returnDate": null
}
```

### Dashboard Statistics

Request:
```
GET /api/dashboard
Authorization: Bearer <token>
```

Response:
```json
{
  "totalProducts": 7,
  "totalInventoryItems": 226,
  "lowStockProducts": 2,
  "outOfStockProducts": 1,
  "totalIssuedProducts": 0,
  "productsNeedingPurchase": 3,
  "totalSuppliers": 5,
  "activeSuppliers": 5,
  "inactiveSuppliers": 0
}
```

### Error Response Format

```json
{
  "timestamp": "2026-07-07T10:10:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Product not found with id: abc123",
  "path": "/api/products/abc123",
  "details": []
}
```

## 17. Security Model

- Passwords are hashed with BCrypt before storage; plaintext passwords are never persisted or logged.
- Stateless JWT authentication — no server-side session state.
- `JwtFilter` runs once per request, validating signature and expiry before populating the Spring Security context.
- Method-level authorization via `@PreAuthorize("hasRole(...)")` / `hasAnyRole(...)` on every controller method.
- CORS is configured centrally in `SecurityConfig`.
- All error responses are standardized and never leak stack traces.
- `/api/auth/login` and Swagger routes are the only public endpoints; everything else requires a valid Bearer token.

## 18. Role Matrix

| Capability | ADMIN | STORE_MANAGER | EMPLOYEE |
|---|:---:|:---:|:---:|
| Manage Users | ✅ | ❌ | ❌ |
| Manage Products | ✅ | ❌ | ❌ |
| View Products | ✅ | ✅ | ✅ |
| Manage Inventory | ✅ | ✅ | ❌ |
| Manage Stock Issues | ✅ | ✅ | ❌ |
| View Dashboard | ✅ | ✅ | ❌ |
| Manage Suppliers (create/update/delete/activate/deactivate) | ✅ | ❌ | ❌ |
| View & Search Suppliers | ✅ | ✅ | ❌ |
| View Own Profile | ✅ | ✅ | ✅ |

## 19. Future Roadmap

- ~~**Suppliers** module — supplier master data, contact info, contracts~~ ✅ Completed in Phase 2
- **Purchase Requests** — employee/store-manager initiated requests feeding procurement recommendations
- **Purchase Orders** — formal orders issued to suppliers (using `Supplier.id`), linked to purchase requests
- **Goods Receipt** — the only future module that increases `Product.currentStock`, tied to Purchase Orders and suppliers
- **Approvals** — configurable multi-level approval workflow for purchase requests/orders
- **Reports** — exportable inventory, procurement, supplier, and consumption reports (CSV/PDF)
- **Notifications** — email/push notifications for low stock, approvals, and order status changes

The package structure (`config`, `controller`, `service`, `repository`, `model`, `dto`, `security`, `exception`) remains intentionally generic so each future module — including Purchase Requests and Purchase Orders, which will build directly on the Supplier module added in Phase 2 — can be added as new files within the same layers without refactoring existing code.
