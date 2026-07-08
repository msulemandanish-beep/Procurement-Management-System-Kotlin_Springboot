PROCUREMENT-MANAGEMENT-SYSTEM
│
├── backend
│   │
│   ├── src
│   │   ├── main
│   │   │   ├── kotlin
│   │   │   │   └── com
│   │   │   │       └── company
│   │   │   │           └── procurement
│   │   │   │
│   │   │   │               ├── config
│   │   │   │               │   ├── SecurityConfig.kt
│   │   │   │               │   ├── SwaggerConfig.kt
│   │   │   │               │   └── DataSeeder.kt
│   │   │   │
│   │   │   │               ├── controller
│   │   │   │               │   ├── AuthController.kt
│   │   │   │               │   ├── UserController.kt
│   │   │   │               │   ├── ProductController.kt
│   │   │   │               │   ├── InventoryController.kt
│   │   │   │               │   ├── StockIssueController.kt
│   │   │   │               │   ├── DashboardController.kt
│   │   │   │               │   └── SupplierController.kt          ← Phase 2
│   │   │   │
│   │   │   │               ├── service
│   │   │   │               │   ├── AuthService.kt
│   │   │   │               │   ├── UserService.kt
│   │   │   │               │   ├── ProductService.kt
│   │   │   │               │   ├── InventoryService.kt
│   │   │   │               │   ├── StockIssueService.kt
│   │   │   │               │   ├── DashboardService.kt
│   │   │   │               │   └── SupplierService.kt             ← Phase 2
│   │   │   │
│   │   │   │               ├── repository
│   │   │   │               │   ├── UserRepository.kt
│   │   │   │               │   ├── ProductRepository.kt
│   │   │   │               │   ├── StockIssueRepository.kt
│   │   │   │               │   └── SupplierRepository.kt          ← Phase 2
│   │   │   │
│   │   │   │               ├── model
│   │   │   │               │   ├── User.kt
│   │   │   │               │   ├── Role.kt
│   │   │   │               │   ├── Product.kt                     (+ supplierId field)
│   │   │   │               │   ├── ProductStatus.kt
│   │   │   │               │   ├── StockIssue.kt
│   │   │   │               │   ├── IssueStatus.kt
│   │   │   │               │   ├── Supplier.kt                    ← Phase 2
│   │   │   │               │   └── SupplierStatus.kt               ← Phase 2
│   │   │   │
│   │   │   │               ├── dto
│   │   │   │               │   ├── auth
│   │   │   │               │   │   ├── LoginRequest.kt
│   │   │   │               │   │   └── LoginResponse.kt
│   │   │   │               │   │
│   │   │   │               │   ├── user
│   │   │   │               │   │   ├── UserRequest.kt
│   │   │   │               │   │   └── UserResponse.kt
│   │   │   │               │   │
│   │   │   │               │   ├── product
│   │   │   │               │   │   ├── ProductRequest.kt          (+ supplierId)
│   │   │   │               │   │   └── ProductResponse.kt         (+ supplier summary)
│   │   │   │               │   │
│   │   │   │               │   ├── inventory
│   │   │   │               │   │   ├── InventoryResponse.kt
│   │   │   │               │   │   └── ProcurementRecommendationResponse.kt
│   │   │   │               │   │
│   │   │   │               │   ├── issue
│   │   │   │               │   │   ├── IssueRequest.kt
│   │   │   │               │   │   └── IssueResponse.kt
│   │   │   │               │   │
│   │   │   │               │   ├── dashboard
│   │   │   │               │   │   └── DashboardResponse.kt       (+ supplier counts)
│   │   │   │               │   │
│   │   │   │               │   ├── supplier                       ← Phase 2
│   │   │   │               │   │   ├── SupplierRequest.kt
│   │   │   │               │   │   ├── SupplierUpdateRequest.kt
│   │   │   │               │   │   ├── SupplierResponse.kt
│   │   │   │               │   │   ├── SupplierSummary.kt
│   │   │   │               │   │   ├── SupplierStatusUpdateRequest.kt
│   │   │   │               │   │   ├── SupplierSearchResponse.kt
│   │   │   │               │   │   └── SupplierStatisticsResponse.kt
│   │   │   │               │   │
│   │   │   │               │   └── common
│   │   │   │               │       └── ErrorResponse.kt
│   │   │   │
│   │   │   │               ├── security
│   │   │   │               │   ├── JwtFilter.kt
│   │   │   │               │   ├── JwtProvider.kt
│   │   │   │               │   ├── UserPrincipal.kt
│   │   │   │               │   └── CustomUserDetailsService.kt
│   │   │   │
│   │   │   │               ├── exception
│   │   │   │               │   ├── GlobalExceptionHandler.kt
│   │   │   │               │   ├── ResourceNotFoundException.kt
│   │   │   │               │   ├── ValidationException.kt
│   │   │   │               │   ├── BusinessException.kt
│   │   │   │               │   └── UnauthorizedException.kt
│   │   │   │
│   │   │   │               ├── util                                (reserved for Phase 3+)
│   │   │   │
│   │   │   │               └── ProcurementApplication.kt
│   │   │
│   │   └── resources
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       └── application-prod.yml
│   │
│   ├── build.gradle.kts
│   ├── settings.gradle.kts
│   ├── gradle.properties
│   ├── .env.example
│   ├── .gitignore
│   ├── postman_collection.json                   (+ Supplier Management folder)
│   ├── README.md                                  (updated for Phase 2)
│   └── FRONTEND_HANDOFF.md                        (updated for Phase 2)
│
│
├── frontend
│   │
│   ├── index.html
│   ├── login.html
│   ├── dashboard.html
│   ├── products.html
│   ├── inventory.html
│   ├── issues.html
│   ├── users.html
│   ├── suppliers.html                             ← NEW (Phase 2 list/detail page)
│   ├── supplier-form.html                         ← NEW (Phase 2 add/edit page, or a modal in suppliers.html)
│   │
│   ├── assets
│   │   ├── images
│   │   │   ├── logo.png
│   │   │   └── avatar.png
│   │   │
│   │   ├── icons
│   │   │   ├── inventory.svg
│   │   │   ├── users.svg
│   │   │   ├── dashboard.svg
│   │   │   └── suppliers.svg                      ← NEW
│   │   │
│   │   └── fonts
│   │
│   ├── css
│   │   ├── global.css
│   │   ├── login.css
│   │   ├── dashboard.css
│   │   ├── products.css
│   │   ├── inventory.css
│   │   ├── issues.css
│   │   ├── users.css
│   │   └── suppliers.css                          ← NEW
│   │
│   ├── js
│   │   │
│   │   ├── config
│   │   │   └── apiConfig.js
│   │   │
│   │   ├── auth
│   │   │   ├── login.js
│   │   │   ├── logout.js
│   │   │   ├── authGuard.js
│   │   │   └── roleGuard.js
│   │   │
│   │   ├── api
│   │   │   ├── authApi.js
│   │   │   ├── productApi.js                       (updated: sends/reads supplierId + supplier summary)
│   │   │   ├── inventoryApi.js
│   │   │   ├── issueApi.js
│   │   │   ├── userApi.js
│   │   │   ├── dashboardApi.js                      (updated: reads supplier counts)
│   │   │   └── supplierApi.js                       ← NEW (CRUD, activate/deactivate, search, statistics)
│   │   │
│   │   ├── pages
│   │   │   ├── dashboard.js                         (updated: renders supplier stat cards)
│   │   │   ├── products.js                          (updated: supplier dropdown in product form)
│   │   │   ├── inventory.js
│   │   │   ├── issues.js
│   │   │   ├── users.js
│   │   │   └── suppliers.js                         ← NEW (list, search, activate/deactivate, form handling)
│   │   │
│   │   ├── components
│   │   │   ├── navbar.js                            (updated: Suppliers nav item, role-gated)
│   │   │   ├── sidebar.js                           (updated: Suppliers nav item, role-gated)
│   │   │   ├── modal.js
│   │   │   ├── table.js
│   │   │   ├── alerts.js
│   │   │   └── supplierDropdown.js                  ← NEW (reusable active-supplier picker for Product form)
│   │   │
│   │   └── utils
│   │       ├── helpers.js
│   │       ├── constants.js
│   │       ├── validators.js                        (updated: supplier field validators)
│   │       └── dateUtils.js
│   │
│   └── README.md
│
│
├── docs
│   ├── API-DOCUMENTATION.md                         (updated: Supplier endpoints)
│   ├── DATABASE-DESIGN.md                           (updated: suppliers collection, Product.supplierId)
│   ├── SYSTEM-FLOW.md                               (updated: Supplier → Product → Inventory flow)
│   ├── ROLE-MATRIX.md                               (updated: Supplier permissions)
│   └── UI-WIREFRAMES.pdf                            (should include Suppliers page wireframes)
│
│
└── deployment
    ├── mongodb
    │   └── mongodb-setup.md                          (note: suppliers collection auto-created)
    │
    ├── backend
    │   └── deployment-guide.md
    │
    └── frontend
        └── hosting-guide.md