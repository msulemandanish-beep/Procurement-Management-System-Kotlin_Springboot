# Frontend Handoff Document — Procurement Management System (Phase 1: Inventory)

This document gives frontend developers everything needed to integrate with the backend: full API contract, authentication instructions, and page-by-page UI requirements.

Base URL (local): `http://localhost:8080`

## 1. Authentication

All endpoints except `POST /api/auth/login` and Swagger routes require a JWT sent as:

```
Authorization: Bearer <token>
```

### Login

- **Method:** POST
- **URL:** `/api/auth/login`
- **Auth required:** No

Request body:
```json
{
  "email": "admin@procurement.com",
  "password": "Admin@123"
}
```

Response body:
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

Store `token` (e.g. in memory or a secure storage mechanism — avoid `localStorage` for production-grade security if possible) and attach it to every subsequent request. Store `role` to drive UI visibility (see per-page notes below).

### JWT Usage Instructions

1. On successful login, save `token` and `role` from the response.
2. Attach `Authorization: Bearer <token>` to every API call.
3. If any API call returns `401 Unauthorized`, clear stored auth state and redirect to the Login page.
4. If a call returns `403 Forbidden`, the user is authenticated but lacks permission — show an "access denied" message rather than logging them out.
5. Tokens expire after 24 hours (`86400000` ms) by default — handle expiry by redirecting to Login on `401`.

## 2. Complete API Contract

### Auth
| Method | Endpoint | Auth | Body | Response |
|---|---|---|---|---|
| POST | `/api/auth/login` | No | `{ email, password }` | `{ token, tokenType, userId, email, firstName, lastName, role }` |

### Users (ADMIN only)
| Method | Endpoint | Body | Response |
|---|---|---|---|
| GET | `/api/users` | — | `UserResponse[]` |
| GET | `/api/users/{id}` | — | `UserResponse` |
| POST | `/api/users` | `UserRequest` | `UserResponse` (201) |
| PUT | `/api/users/{id}` | `UserRequest` | `UserResponse` |
| DELETE | `/api/users/{id}` | — | 204 No Content |

`UserRequest`:
```json
{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@procurement.com",
  "password": "Password@123",
  "role": "ADMIN | STORE_MANAGER | EMPLOYEE",
  "active": true
}
```
Note: `password` is optional on update — omit it to keep the existing password.

`UserResponse`:
```json
{
  "id": "string",
  "firstName": "string",
  "lastName": "string",
  "email": "string",
  "role": "ADMIN | STORE_MANAGER | EMPLOYEE",
  "active": true,
  "createdAt": "ISO-8601 timestamp",
  "updatedAt": "ISO-8601 timestamp"
}
```

### Products
| Method | Endpoint | Auth | Body | Response |
|---|---|---|---|---|
| GET | `/api/products` | Any authenticated role | — | `ProductResponse[]` |
| GET | `/api/products/{id}` | Any authenticated role | — | `ProductResponse` |
| POST | `/api/products` | ADMIN | `ProductRequest` | `ProductResponse` (201) |
| PUT | `/api/products/{id}` | ADMIN | `ProductRequest` | `ProductResponse` |
| DELETE | `/api/products/{id}` | ADMIN | — | 204 No Content |

`ProductRequest` (note: **`supplierId` is now required** — Phase 2):
```json
{
  "name": "string",
  "description": "string",
  "category": "string",
  "unitPrice": 4.5,
  "currentStock": 50,
  "minimumStock": 10,
  "supplierId": "string (must reference an existing supplier's id)"
}
```

`ProductResponse` (note: `supplier` is now embedded — Phase 2):
```json
{
  "id": "string",
  "name": "string",
  "description": "string",
  "category": "string",
  "unitPrice": 4.5,
  "currentStock": 50,
  "minimumStock": 10,
  "supplier": {
    "id": "string",
    "supplierCode": "SUP-0001",
    "companyName": "ABC Office Supplies"
  },
  "status": "IN_STOCK | LOW_STOCK | OUT_OF_STOCK",
  "createdAt": "ISO-8601 timestamp",
  "updatedAt": "ISO-8601 timestamp"
}
```

If `supplierId` does not reference an existing supplier, `POST`/`PUT` return `404 Not Found` with message `Supplier not found with id: <id>`. The frontend should populate the supplier dropdown from `GET /api/suppliers/active` so users can only select suppliers that are currently active.

### Inventory (ADMIN, STORE_MANAGER)
| Method | Endpoint | Response |
|---|---|---|
| GET | `/api/inventory` | `InventoryResponse[]` |
| GET | `/api/inventory/low-stock` | `InventoryResponse[]` |
| GET | `/api/inventory/out-of-stock` | `InventoryResponse[]` |
| GET | `/api/inventory/status` | `{ "IN_STOCK": number, "LOW_STOCK": number, "OUT_OF_STOCK": number }` |
| GET | `/api/inventory/procurement-recommendations` | `ProcurementRecommendationResponse[]` |

`InventoryResponse`:
```json
{
  "productId": "string",
  "productName": "string",
  "currentStock": 8,
  "minimumStock": 15,
  "status": "LOW_STOCK"
}
```

`ProcurementRecommendationResponse`:
```json
{
  "productId": "string",
  "productName": "string",
  "currentStock": 8,
  "minimumStock": 15,
  "recommendedPurchaseQuantity": 22
}
```

### Stock Issues (ADMIN, STORE_MANAGER)
| Method | Endpoint | Body | Response |
|---|---|---|---|
| GET | `/api/issues` | — | `IssueResponse[]` |
| GET | `/api/issues/history` | — | `IssueResponse[]` (sorted newest first) |
| POST | `/api/issues` | `IssueRequest` | `IssueResponse` (201) |
| PUT | `/api/issues/{id}/return` | — | `IssueResponse` |

`IssueRequest`:
```json
{
  "productId": "string",
  "employeeId": "string",
  "quantity": 2
}
```

`IssueResponse`:
```json
{
  "id": "string",
  "productId": "string",
  "productName": "string",
  "employeeId": "string",
  "employeeName": "string",
  "quantity": 2,
  "issueDate": "ISO-8601 timestamp",
  "issuedBy": "string (email of the issuer)",
  "status": "ISSUED | RETURNED",
  "returnDate": "ISO-8601 timestamp | null"
}
```

### Dashboard (ADMIN, STORE_MANAGER)
| Method | Endpoint | Response |
|---|---|---|
| GET | `/api/dashboard` | `DashboardResponse` |

`DashboardResponse`:
```json
{
  "totalProducts": 6,
  "totalInventoryItems": 208,
  "lowStockProducts": 2,
  "outOfStockProducts": 1,
  "totalIssuedProducts": 3,
  "productsNeedingPurchase": 3
}
```

### Suppliers
| Method | Endpoint | Auth | Body | Response |
|---|---|---|---|---|
| GET | `/api/suppliers` | ADMIN, STORE_MANAGER | — | `SupplierResponse[]` |
| GET | `/api/suppliers/{id}` | ADMIN, STORE_MANAGER | — | `SupplierResponse` |
| GET | `/api/suppliers/code/{supplierCode}` | ADMIN, STORE_MANAGER | — | `SupplierResponse` |
| POST | `/api/suppliers` | ADMIN | `SupplierRequest` | `SupplierResponse` (201) |
| PUT | `/api/suppliers/{id}` | ADMIN | `SupplierUpdateRequest` | `SupplierResponse` |
| DELETE | `/api/suppliers/{id}` | ADMIN | — | 204 No Content |
| PATCH | `/api/suppliers/{id}/activate` | ADMIN | — | `SupplierResponse` |
| PATCH | `/api/suppliers/{id}/deactivate` | ADMIN | — | `SupplierResponse` |
| GET | `/api/suppliers/search?keyword=` | ADMIN, STORE_MANAGER | — | `SupplierSearchResponse[]` |
| GET | `/api/suppliers/active` | ADMIN, STORE_MANAGER | — | `SupplierResponse[]` |
| GET | `/api/suppliers/inactive` | ADMIN, STORE_MANAGER | — | `SupplierResponse[]` |
| GET | `/api/suppliers/statistics` | ADMIN, STORE_MANAGER | — | `SupplierStatisticsResponse` |

`SupplierRequest` (create — do **not** send `supplierCode`, it is auto-generated as `SUP-0001`, `SUP-0002`, etc.):
```json
{
  "companyName": "string (must be unique)",
  "contactPerson": "string",
  "email": "string (valid email, must be unique)",
  "phone": "string",
  "alternatePhone": "string | null (optional)",
  "website": "string | null (optional)",
  "address": "string",
  "city": "string",
  "state": "string",
  "country": "string",
  "postalCode": "string",
  "taxNumber": "string (NTN or equivalent)",
  "paymentTerms": "string (e.g. 'Net 30')",
  "deliveryLeadTime": 7,
  "notes": "string | null (optional)"
}
```

`SupplierUpdateRequest` — identical shape to `SupplierRequest`, used on `PUT /api/suppliers/{id}`.

`SupplierResponse`:
```json
{
  "id": "string",
  "supplierCode": "SUP-0001",
  "companyName": "string",
  "contactPerson": "string",
  "email": "string",
  "phone": "string",
  "alternatePhone": "string | null",
  "website": "string | null",
  "address": "string",
  "city": "string",
  "state": "string",
  "country": "string",
  "postalCode": "string",
  "taxNumber": "string",
  "paymentTerms": "string",
  "deliveryLeadTime": 7,
  "notes": "string | null",
  "status": "ACTIVE | INACTIVE",
  "createdAt": "ISO-8601 timestamp",
  "updatedAt": "ISO-8601 timestamp"
}
```

`SupplierSearchResponse` (lightweight, returned by `/api/suppliers/search`):
```json
{
  "id": "string",
  "supplierCode": "SUP-0001",
  "companyName": "string",
  "contactPerson": "string",
  "email": "string",
  "phone": "string",
  "status": "ACTIVE | INACTIVE"
}
```

`SupplierStatisticsResponse`:
```json
{
  "totalSuppliers": 6,
  "activeSuppliers": 5,
  "inactiveSuppliers": 1
}
```

Validation & error behavior:
- Duplicate `companyName`, `email`, or (auto-generated, so effectively never client-triggered) `supplierCode` → `409 Conflict` with a descriptive message.
- Activating an already-active supplier, or deactivating an already-inactive one → `409 Conflict`.
- Missing/invalid required fields → `400 Bad Request` with a `details` array listing each field error.
- Non-existent supplier `id`/`supplierCode` → `404 Not Found`.

### Dashboard (updated for Phase 2)

`DashboardResponse` now also includes supplier counts:
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

### Error Response (all endpoints)
```json
{
  "timestamp": "ISO-8601 timestamp",
  "status": 404,
  "error": "Not Found",
  "message": "human readable message",
  "path": "/api/products/abc123",
  "details": ["field: message"]
}
```

## 3. Page-by-Page Frontend Requirements

### Page: Login
- **UI elements:** logo/title, email input, password input, "Login" button, error banner area.
- **Forms:** single form with `email` (validated as email format) and `password` (required).
- **Buttons:** "Login" (submit), optionally "Show password" toggle.
- **API calls:** `POST /api/auth/login`. On success, store token + role, redirect to Dashboard (ADMIN/STORE_MANAGER) or Products (EMPLOYEE).
- **Error handling:** show inline error banner on 401 ("Invalid email or password").

### Page: Dashboard (ADMIN, STORE_MANAGER only)
- **UI elements:** six stat cards (Total Products, Total Inventory Items, Low Stock Products, Out of Stock Products, Total Issued Products, Products Needing Purchase), a table or list of procurement recommendations.
- **Tables:** "Procurement Recommendations" table with columns Product Name, Current Stock, Minimum Stock, Recommended Purchase Quantity.
- **Buttons:** "Refresh" to re-fetch stats.
- **API calls:** `GET /api/dashboard`, `GET /api/inventory/procurement-recommendations`.
- **Access:** hide/redirect away from this page for EMPLOYEE role.

### Page: Products
- **UI elements:** product table, search/filter by category or status, "Add Product" button (ADMIN only).
- **Table columns:** Name, Category, Unit Price, Current Stock, Minimum Stock, **Supplier** (show `companyName`, clickable to `/suppliers/{id}` detail page), Status (badge: green=IN_STOCK, yellow=LOW_STOCK, red=OUT_OF_STOCK), Actions (Edit/Delete — ADMIN only).
- **Forms:** "Add/Edit Product" modal with fields Name, Description, Category, Unit Price, Current Stock, Minimum Stock, and a **Supplier dropdown** (new in Phase 2) — all required, numeric fields validated as non-negative. Populate the Supplier dropdown from `GET /api/suppliers/active` (label: `companyName` + `supplierCode`, value: `id`) so users can't accidentally assign an inactive supplier to a product.
- **Buttons:** "Add Product" (ADMIN), "Edit" (ADMIN), "Delete" with confirmation dialog (ADMIN).
- **API calls:** `GET /api/products`, `GET /api/products/{id}`, `POST /api/products`, `PUT /api/products/{id}`, `DELETE /api/products/{id}`, `GET /api/suppliers/active` (to populate the dropdown).
- **Error handling:** if the selected supplier was deleted between page load and submit, the API returns `404` with `Supplier not found with id: ...` — show this inline and refresh the dropdown.
- **Access:** all roles can view; only ADMIN sees Add/Edit/Delete controls.

### Page: Inventory (ADMIN, STORE_MANAGER only)
- **UI elements:** tabs or filter buttons for "All", "Low Stock", "Out of Stock"; summary status counts at top.
- **Table columns:** Product Name, Current Stock, Minimum Stock, Status (badge).
- **API calls:** `GET /api/inventory`, `GET /api/inventory/low-stock`, `GET /api/inventory/out-of-stock`, `GET /api/inventory/status`.
- **Access:** hide/redirect away from this page for EMPLOYEE role.

### Page: Issues
- **UI elements:** two tables/tabs — "Active Issues" and "History"; "Issue Stock" button (ADMIN, STORE_MANAGER).
- **Forms:** "Issue Stock" modal with Product dropdown (populated from `GET /api/products`), Employee dropdown (populated from `GET /api/users` filtered to EMPLOYEE role, or all users), Quantity input (required, min 1).
- **Table columns:** Product Name, Employee Name, Quantity, Issue Date, Issued By, Status (badge: blue=ISSUED, gray=RETURNED), Return Date, Actions ("Return" button visible only when status is ISSUED).
- **API calls:** `GET /api/issues`, `GET /api/issues/history`, `POST /api/issues`, `PUT /api/issues/{id}/return`.
- **Error handling:** surface `409 Conflict` business errors (e.g., "Insufficient stock for product...") as inline form errors.
- **Access:** hide/redirect away from this page for EMPLOYEE role.

### Page: Users (ADMIN only)
- **UI elements:** user table, "Add User" button.
- **Table columns:** First Name, Last Name, Email, Role (badge), Active (toggle/badge), Actions (Edit/Delete).
- **Forms:** "Add/Edit User" modal with First Name, Last Name, Email, Password (required on create, optional on edit — show helper text "Leave blank to keep current password"), Role dropdown (ADMIN/STORE_MANAGER/EMPLOYEE), Active checkbox.
- **Buttons:** "Add User", "Edit", "Delete" with confirmation dialog.
- **API calls:** `GET /api/users`, `GET /api/users/{id}`, `POST /api/users`, `PUT /api/users/{id}`, `DELETE /api/users/{id}`.
- **Access:** entire page hidden/redirected for non-ADMIN roles.

### Page: Suppliers (ADMIN full access; STORE_MANAGER view/search only)
- **UI elements:** supplier table, search box (calls `/api/suppliers/search?keyword=`), status filter tabs ("All" / "Active" / "Inactive"), stat cards for `totalSuppliers` / `activeSuppliers` / `inactiveSuppliers`, "Add Supplier" button (ADMIN only).
- **Table columns:** Supplier Code, Company Name, Contact Person, Email, Phone, Delivery Lead Time (days), Status (badge: green=ACTIVE, gray=INACTIVE), Actions (View/Edit/Delete/Activate/Deactivate — ADMIN only; View available to STORE_MANAGER too).
- **Forms:** "Add/Edit Supplier" modal (ADMIN only) with fields: Company Name, Contact Person, Email, Phone, Alternate Phone (optional), Website (optional), Address, City, State, Country, Postal Code, Tax Number / NTN, Payment Terms, Delivery Lead Time (days, numeric), Notes (optional). Do not show a Supplier Code field on create — it is server-generated and returned in the response; show it read-only on the edit form.
- **Buttons:**
  - "Add Supplier" (ADMIN) → `POST /api/suppliers`
  - "Edit" (ADMIN) → `PUT /api/suppliers/{id}`
  - "Delete" with confirmation dialog (ADMIN) → `DELETE /api/suppliers/{id}`
  - "Activate" (ADMIN, shown only when status is INACTIVE) → `PATCH /api/suppliers/{id}/activate`
  - "Deactivate" (ADMIN, shown only when status is ACTIVE) → `PATCH /api/suppliers/{id}/deactivate`
- **Supplier Detail view:** show all `SupplierResponse` fields plus a "Products supplied" panel — fetch `GET /api/products` and filter client-side where `product.supplier.id` matches this supplier's `id` (there is no dedicated "products by supplier" endpoint in Phase 2).
- **API calls:** `GET /api/suppliers`, `GET /api/suppliers/{id}`, `GET /api/suppliers/code/{supplierCode}`, `POST /api/suppliers`, `PUT /api/suppliers/{id}`, `DELETE /api/suppliers/{id}`, `PATCH /api/suppliers/{id}/activate`, `PATCH /api/suppliers/{id}/deactivate`, `GET /api/suppliers/search?keyword=`, `GET /api/suppliers/active`, `GET /api/suppliers/inactive`, `GET /api/suppliers/statistics`.
- **Error handling:** surface `409 Conflict` (duplicate company name/email, or activating/deactivating a supplier already in that state) as inline form/action errors.
- **Access:** ADMIN sees full CRUD + activate/deactivate; STORE_MANAGER sees the list, detail view, search, and statistics but no Add/Edit/Delete/Activate/Deactivate controls; EMPLOYEE cannot access this page at all (hide the nav item and redirect if navigated to directly).

## 4. JavaScript Fetch Examples

### Login
```javascript
async function login(email, password) {
  const response = await fetch('http://localhost:8080/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password })
  });
  if (!response.ok) {
    throw new Error('Login failed');
  }
  const data = await response.json();
  // Store data.token and data.role for subsequent requests
  return data;
}
```

### Authenticated GET
```javascript
async function getProducts(token) {
  const response = await fetch('http://localhost:8080/api/products', {
    headers: { 'Authorization': `Bearer ${token}` }
  });
  if (response.status === 401) {
    // token expired or invalid — redirect to login
  }
  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message);
  }
  return response.json();
}
```

### Authenticated POST
```javascript
async function issueStock(token, productId, employeeId, quantity) {
  const response = await fetch('http://localhost:8080/api/issues', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    },
    body: JSON.stringify({ productId, employeeId, quantity })
  });
  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message);
  }
  return response.json();
}
```

### Authenticated PUT (Return Stock)
```javascript
async function returnStock(token, issueId) {
  const response = await fetch(`http://localhost:8080/api/issues/${issueId}/return`, {
    method: 'PUT',
    headers: { 'Authorization': `Bearer ${token}` }
  });
  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message);
  }
  return response.json();
}
```

### Authenticated DELETE
```javascript
async function deleteProduct(token, productId) {
  const response = await fetch(`http://localhost:8080/api/products/${productId}`, {
    method: 'DELETE',
    headers: { 'Authorization': `Bearer ${token}` }
  });
  if (response.status !== 204) {
    const error = await response.json();
    throw new Error(error.message);
  }
}
```

### Get Active Suppliers (for a Product form dropdown)
```javascript
async function getActiveSuppliers(token) {
  const response = await fetch('http://localhost:8080/api/suppliers/active', {
    headers: { 'Authorization': `Bearer ${token}` }
  });
  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message);
  }
  return response.json();
}
```

### Create Supplier
```javascript
async function createSupplier(token, supplierData) {
  const response = await fetch('http://localhost:8080/api/suppliers', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    },
    body: JSON.stringify(supplierData)
  });
  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message);
  }
  return response.json();
}
```

### Search Suppliers
```javascript
async function searchSuppliers(token, keyword) {
  const url = `http://localhost:8080/api/suppliers/search?keyword=${encodeURIComponent(keyword)}`;
  const response = await fetch(url, {
    headers: { 'Authorization': `Bearer ${token}` }
  });
  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message);
  }
  return response.json();
}
```

### Activate / Deactivate Supplier
```javascript
async function setSupplierStatus(token, supplierId, activate) {
  const action = activate ? 'activate' : 'deactivate';
  const response = await fetch(`http://localhost:8080/api/suppliers/${supplierId}/${action}`, {
    method: 'PATCH',
    headers: { 'Authorization': `Bearer ${token}` }
  });
  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message);
  }
  return response.json();
}
```

## 5. Integration Instructions

1. Start MongoDB and the backend (see main `README.md` for setup/build/run instructions).
2. Confirm the backend is reachable at `http://localhost:8080/swagger-ui.html`.
3. Point your frontend's API base URL at `http://localhost:8080` (or your deployed host).
4. Implement a small auth wrapper/interceptor (e.g., Axios interceptor or a `fetchWithAuth` helper) that automatically attaches `Authorization: Bearer <token>` and handles `401` globally by redirecting to Login.
5. Use the `role` returned at login to conditionally render navigation items and page access per the Role Matrix in `README.md` section 18.
6. Test all flows first via the provided `postman_collection.json` to confirm expected payloads before wiring up UI components.
7. CORS is open (`*`) by default in this build for ease of local development — restrict `allowedOriginPatterns` in `SecurityConfig.kt` before deploying to production.
8. **Phase 2 migration note:** the Product create/edit form now requires a Supplier selection. Fetch `GET /api/suppliers/active` when the Products page (or its Add/Edit modal) loads, and block form submission client-side if no supplier is selected — the server also enforces this via `400 Bad Request` (missing field) or `404 Not Found` (invalid supplier id), but validating client-side gives users faster feedback.
9. Build the Suppliers page and nav item following the same list/detail/modal patterns already used for Products and Users so the UI stays visually and behaviorally consistent.
