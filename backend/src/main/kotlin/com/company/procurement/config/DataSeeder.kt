package com.company.procurement.config

import com.company.procurement.model.ApprovalDecision
import com.company.procurement.model.ApprovalHistory
import com.company.procurement.model.ApprovalLevel
import com.company.procurement.model.Category
import com.company.procurement.model.Department
import com.company.procurement.model.DepartmentBudget
import com.company.procurement.model.GoodsReceipt
import com.company.procurement.model.GoodsReceiptItem
import com.company.procurement.model.GoodsReceiptStatus
import com.company.procurement.model.InspectionStatus
import com.company.procurement.model.Priority
import com.company.procurement.model.Product
import com.company.procurement.model.PurchaseOrder
import com.company.procurement.model.PurchaseOrderItem
import com.company.procurement.model.PurchaseOrderStatus
import com.company.procurement.model.PurchaseOrderTimelineEntry
import com.company.procurement.model.PurchaseRequest
import com.company.procurement.model.PurchaseRequestItem
import com.company.procurement.model.PurchaseRequestStatus
import com.company.procurement.model.PurchaseRequestTimelineEntry
import com.company.procurement.model.Role
import com.company.procurement.model.Supplier
import com.company.procurement.model.SupplierStatus
import com.company.procurement.model.User
import com.company.procurement.repository.ApprovalHistoryRepository
import com.company.procurement.repository.CategoryRepository
import com.company.procurement.repository.DepartmentBudgetRepository
import com.company.procurement.repository.DepartmentRepository
import com.company.procurement.repository.GoodsReceiptRepository
import com.company.procurement.repository.ProductRepository
import com.company.procurement.repository.PurchaseOrderRepository
import com.company.procurement.repository.PurchaseRequestRepository
import com.company.procurement.repository.SupplierRepository
import com.company.procurement.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

@Component
class DataSeeder(
    private val userRepository: UserRepository,
    private val productRepository: ProductRepository,
    private val supplierRepository: SupplierRepository,
    private val departmentRepository: DepartmentRepository,
    private val categoryRepository: CategoryRepository,
    private val departmentBudgetRepository: DepartmentBudgetRepository,
    private val purchaseRequestRepository: PurchaseRequestRepository,
    private val approvalHistoryRepository: ApprovalHistoryRepository,
    private val purchaseOrderRepository: PurchaseOrderRepository,
    private val goodsReceiptRepository: GoodsReceiptRepository,
    private val passwordEncoder: PasswordEncoder
) : CommandLineRunner {

    private val logger = LoggerFactory.getLogger(DataSeeder::class.java)

    override fun run(vararg args: String?) {
        val users = seedUsers()
        seedDepartments()
        seedBudgets()
        val categoryIds = seedCategories()
        val supplierIds = seedSuppliers()
        val products = seedProducts(supplierIds, categoryIds)
        seedProcurementWorkflowDemo(users, products)
    }

    private fun seedUsers(): Map<String, User> {
        val existingUsers = userRepository.findAll().associateBy { it.email }
        if (existingUsers.isNotEmpty()) {
            logger.info("Users already seeded. Skipping user seed data.")
            return existingUsers
        }

        val admin = User(
            firstName = "Ali",
            lastName = "Admin",
            email = "admin@procurement.com",
            password = passwordEncoder.encode("Admin@123"),
            role = Role.ADMIN,
            active = true
        )

        val storeManager = User(
            firstName = "Sara",
            lastName = "Manager",
            email = "storemanager@procurement.com",
            password = passwordEncoder.encode("Manager@123"),
            role = Role.STORE_MANAGER,
            active = true
        )

        val procurementManager = User(
            firstName = "Imran",
            lastName = "Chaudhry",
            email = "procurementmanager@procurement.com",
            password = passwordEncoder.encode("Procurement@123"),
            role = Role.PROCUREMENT_MANAGER,
            active = true
        )

        val financeManager = User(
            firstName = "Ayesha",
            lastName = "Farooq",
            email = "financemanager@procurement.com",
            password = passwordEncoder.encode("Finance@123"),
            role = Role.FINANCE_MANAGER,
            active = true
        )

        val employee = User(
            firstName = "Usman",
            lastName = "Employee",
            email = "employee@procurement.com",
            password = passwordEncoder.encode("Employee@123"),
            role = Role.EMPLOYEE,
            active = true
        )

        val saved = userRepository.saveAll(listOf(admin, storeManager, procurementManager, financeManager, employee))
        logger.info(
            "Seeded default users: admin@procurement.com, storemanager@procurement.com, " +
                "procurementmanager@procurement.com, financemanager@procurement.com, employee@procurement.com"
        )
        return saved.associateBy { it.email }
    }

    private fun seedDepartments() {
        if (departmentRepository.count() > 0) {
            logger.info("Departments already seeded. Skipping department seed data.")
            return
        }

        val departments = listOf(
            Department(name = "Information Technology", code = "IT", description = "Manages hardware, software, and infrastructure"),
            Department(name = "Procurement", code = "PROC", description = "Handles sourcing and supplier relationships"),
            Department(name = "Finance", code = "FIN", description = "Manages budgets and financial approvals"),
            Department(name = "Operations", code = "OPS", description = "Day-to-day operational support"),
            Department(name = "Human Resources", code = "HR", description = "Employee and workplace administration")
        )

        departmentRepository.saveAll(departments)
        logger.info("Seeded ${departments.size} default departments")
    }

    /**
     * Seeds an annual budget for the main spending departments (Phase 11) for the
     * current fiscal year, so the Budget endpoints have realistic data to show
     * immediately. Reserved/spent amounts start at zero and are only ever moved
     * by BudgetService as real approvals/completions happen from this point on —
     * the completed PR-0001/PO-0001/GRN-0001 demo cycle seeded further below was
     * inserted directly via repositories (bypassing the services), so it does not
     * retroactively affect these starting budget figures.
     */
    private fun seedBudgets() {
        if (departmentBudgetRepository.count() > 0) {
            logger.info("Department budgets already seeded. Skipping budget seed data.")
            return
        }

        val fiscalYear = Instant.now().atZone(ZoneOffset.UTC).year
        val departments = departmentRepository.findAll()

        val annualBudgetsByCode = mapOf(
            "IT" to 50000.0,
            "PROC" to 20000.0,
            "FIN" to 15000.0,
            "OPS" to 30000.0,
            "HR" to 10000.0
        )

        val budgets = departments.mapNotNull { department ->
            val annualBudget = annualBudgetsByCode[department.code] ?: return@mapNotNull null
            DepartmentBudget(
                departmentId = department.id ?: "",
                departmentName = department.name,
                fiscalYear = fiscalYear,
                annualBudget = annualBudget
            )
        }

        departmentBudgetRepository.saveAll(budgets)
        logger.info("Seeded ${budgets.size} department budgets for fiscal year $fiscalYear")
    }

    /**
     * Seeds a two-level category hierarchy (Phase 9): three main categories, two
     * of which have subcategories. Returns a map keyed by category name so
     * seedProducts() can assign the correct categoryId to every product.
     */
    private fun seedCategories(): Map<String, String> {
        val existingCategories = categoryRepository.findAll()
        if (existingCategories.isNotEmpty()) {
            logger.info("Categories already seeded. Skipping category seed data.")
            return existingCategories.associate { it.name to (it.id ?: "") }
        }

        val officeSupplies = categoryRepository.save(
            Category(name = "Office Supplies", parentCategoryId = null, description = "Consumable office materials")
        )
        val itEquipment = categoryRepository.save(
            Category(name = "IT Equipment", parentCategoryId = null, description = "Computing and networking hardware")
        )
        val furniture = categoryRepository.save(
            Category(name = "Furniture", parentCategoryId = null, description = "Office furniture")
        )

        val computers = categoryRepository.save(
            Category(name = "Computers", parentCategoryId = itEquipment.id, description = "Laptops and desktops")
        )
        val peripherals = categoryRepository.save(
            Category(name = "Peripherals", parentCategoryId = itEquipment.id, description = "Monitors, drives, and accessories")
        )
        val officeChairs = categoryRepository.save(
            Category(name = "Office Chairs", parentCategoryId = furniture.id, description = "Seating")
        )

        logger.info("Seeded 6 default categories (3 main, 3 sub)")

        return mapOf(
            "Office Supplies" to (officeSupplies.id ?: ""),
            "IT Equipment" to (itEquipment.id ?: ""),
            "Furniture" to (furniture.id ?: ""),
            "Computers" to (computers.id ?: ""),
            "Peripherals" to (peripherals.id ?: ""),
            "Office Chairs" to (officeChairs.id ?: "")
        )
    }

    /**
     * Seeds five realistic suppliers if none exist yet. Returns a map keyed by a short
     * logical name so seedProducts() can assign a valid supplierId to every product.
     * If suppliers already exist (e.g. app restart), the existing suppliers are looked
     * up by company name instead of being re-created, so seeding is always safe to re-run.
     */
    private fun seedSuppliers(): Map<String, String> {
        val supplierDefinitions = listOf(
            SupplierSeed(
                companyName = "ABC Office Supplies",
                contactPerson = "Bilal Anwar",
                email = "sales@abcofficesupplies.com",
                phone = "+92-42-1234567",
                address = "12 Mall Road",
                city = "Lahore",
                state = "Punjab",
                country = "Pakistan",
                postalCode = "54000",
                taxNumber = "NTN-1000001",
                paymentTerms = "Net 30",
                deliveryLeadTime = 5
            ),
            SupplierSeed(
                companyName = "Tech Solutions Ltd",
                contactPerson = "Ahmed Raza",
                email = "contact@techsolutionsltd.com",
                phone = "+92-21-2345678",
                address = "45 Shahrah-e-Faisal",
                city = "Karachi",
                state = "Sindh",
                country = "Pakistan",
                postalCode = "75300",
                taxNumber = "NTN-1000002",
                paymentTerms = "Net 45",
                deliveryLeadTime = 10
            ),
            SupplierSeed(
                companyName = "Global Stationers",
                contactPerson = "Fatima Noor",
                email = "info@globalstationers.com",
                phone = "+92-51-3456789",
                address = "7 Blue Area",
                city = "Islamabad",
                state = "Islamabad Capital Territory",
                country = "Pakistan",
                postalCode = "44000",
                taxNumber = "NTN-1000003",
                paymentTerms = "Net 15",
                deliveryLeadTime = 3
            ),
            SupplierSeed(
                companyName = "Computer World",
                contactPerson = "Hassan Iqbal",
                email = "sales@computerworld.com",
                phone = "+92-42-4567890",
                address = "88 Hall Road",
                city = "Lahore",
                state = "Punjab",
                country = "Pakistan",
                postalCode = "54600",
                taxNumber = "NTN-1000004",
                paymentTerms = "Net 30",
                deliveryLeadTime = 7
            ),
            SupplierSeed(
                companyName = "Prime Electronics",
                contactPerson = "Zainab Sheikh",
                email = "orders@primeelectronics.com",
                phone = "+92-21-5678901",
                address = "23 I.I. Chundrigar Road",
                city = "Karachi",
                state = "Sindh",
                country = "Pakistan",
                postalCode = "74000",
                taxNumber = "NTN-1000005",
                paymentTerms = "Net 60",
                deliveryLeadTime = 14
            )
        )

        val idsByCompanyName = mutableMapOf<String, String>()
        var createdCount = 0

        supplierDefinitions.forEachIndexed { index, seed ->
            val existing = supplierRepository.findByCompanyName(seed.companyName)
            if (existing != null) {
                idsByCompanyName[seed.companyName] = existing.id ?: ""
                return@forEachIndexed
            }

            val supplierCode = "SUP-%04d".format(index + 1)

            val supplier = Supplier(
                supplierCode = supplierCode,
                companyName = seed.companyName,
                contactPerson = seed.contactPerson,
                email = seed.email,
                phone = seed.phone,
                address = seed.address,
                city = seed.city,
                state = seed.state,
                country = seed.country,
                postalCode = seed.postalCode,
                taxNumber = seed.taxNumber,
                paymentTerms = seed.paymentTerms,
                deliveryLeadTime = seed.deliveryLeadTime,
                status = SupplierStatus.ACTIVE
            )

            val saved = supplierRepository.save(supplier)
            idsByCompanyName[seed.companyName] = saved.id ?: ""
            createdCount++
        }

        if (createdCount > 0) {
            logger.info("Seeded $createdCount default suppliers")
        } else {
            logger.info("Suppliers already seeded. Skipping supplier seed data.")
        }

        return idsByCompanyName
    }

    private fun seedProducts(supplierIdsByCompanyName: Map<String, String>, categoryIdsByName: Map<String, String>): Map<String, Product> {
        val existingProducts = productRepository.findAll()
        if (existingProducts.isNotEmpty()) {
            logger.info("Products already seeded. Skipping product seed data.")
            return existingProducts.associateBy { it.name }
        }

        val abcOfficeSupplies = supplierIdsByCompanyName.getValue("ABC Office Supplies")
        val techSolutions = supplierIdsByCompanyName.getValue("Tech Solutions Ltd")
        val globalStationers = supplierIdsByCompanyName.getValue("Global Stationers")
        val computerWorld = supplierIdsByCompanyName.getValue("Computer World")
        val primeElectronics = supplierIdsByCompanyName.getValue("Prime Electronics")

        val officeSuppliesCategory = categoryIdsByName.getValue("Office Supplies")
        val computersCategory = categoryIdsByName.getValue("Computers")
        val peripheralsCategory = categoryIdsByName.getValue("Peripherals")
        val officeChairsCategory = categoryIdsByName.getValue("Office Chairs")

        val products = listOf(
            Product(
                name = "A4 Printing Paper (Ream)",
                description = "500 sheets of premium quality A4 printing paper",
                categoryId = officeSuppliesCategory,
                sku = "OFS-PAP-001",
                barcode = "8901234500011",
                unitOfMeasure = "REAM",
                currency = "USD",
                unitPrice = 5.99,
                currentStock = 120,
                minimumStock = 30,
                supplierId = abcOfficeSupplies,
                status = Product.deriveStatus(120, 30)
            ),
            Product(
                name = "Ballpoint Pens (Box of 50)",
                description = "Blue ink ballpoint pens, box of 50",
                categoryId = officeSuppliesCategory,
                sku = "OFS-PEN-002",
                barcode = "8901234500028",
                unitOfMeasure = "BOX",
                currency = "USD",
                unitPrice = 12.50,
                currentStock = 15,
                minimumStock = 20,
                supplierId = globalStationers,
                status = Product.deriveStatus(15, 20)
            ),
            Product(
                name = "Laptop - Dell Latitude 5440",
                description = "14-inch business laptop, Intel i5, 16GB RAM, 512GB SSD",
                categoryId = computersCategory,
                sku = "ITE-LAP-003",
                barcode = "8901234500035",
                unitOfMeasure = "EA",
                currency = "USD",
                unitPrice = 950.00,
                currentStock = 0,
                minimumStock = 5,
                supplierId = techSolutions,
                status = Product.deriveStatus(0, 5)
            ),
            Product(
                name = "Office Chair - Ergonomic",
                description = "Ergonomic mesh office chair with lumbar support",
                categoryId = officeChairsCategory,
                sku = "FUR-CHR-004",
                barcode = "8901234500042",
                unitOfMeasure = "EA",
                currency = "USD",
                unitPrice = 145.00,
                currentStock = 40,
                minimumStock = 10,
                supplierId = abcOfficeSupplies,
                status = Product.deriveStatus(40, 10)
            ),
            Product(
                name = "Whiteboard Markers (Pack of 12)",
                description = "Assorted color dry-erase whiteboard markers",
                categoryId = officeSuppliesCategory,
                sku = "OFS-MRK-005",
                barcode = "8901234500059",
                unitOfMeasure = "PACK",
                currency = "USD",
                unitPrice = 8.25,
                currentStock = 8,
                minimumStock = 15,
                supplierId = globalStationers,
                status = Product.deriveStatus(8, 15)
            ),
            Product(
                name = "External Hard Drive 1TB",
                description = "Portable USB 3.0 external hard drive, 1TB capacity",
                categoryId = peripheralsCategory,
                sku = "ITE-HDD-006",
                barcode = "8901234500066",
                unitOfMeasure = "EA",
                currency = "USD",
                unitPrice = 65.00,
                currentStock = 25,
                minimumStock = 10,
                supplierId = computerWorld,
                status = Product.deriveStatus(25, 10)
            ),
            Product(
                name = "LED Monitor 24-inch",
                description = "24-inch Full HD LED monitor with HDMI and VGA input",
                categoryId = peripheralsCategory,
                sku = "ITE-MON-007",
                barcode = "8901234500073",
                unitOfMeasure = "EA",
                currency = "USD",
                unitPrice = 110.00,
                currentStock = 18,
                minimumStock = 6,
                supplierId = primeElectronics,
                status = Product.deriveStatus(18, 6)
            )
        )

        val saved = productRepository.saveAll(products)
        logger.info("Seeded ${saved.size} default products")
        return saved.associateBy { it.name }
    }

    /**
     * Seeds a complete, realistic procurement workflow so the system demonstrates
     * every stage end-to-end: a fully completed PR -> PO -> GRN cycle (which also
     * increases inventory), a request pending Store Manager approval, a rejected
     * request, and a high-value request currently awaiting Finance Manager approval.
     * Safe to re-run: skipped entirely if any purchase request already exists.
     */
    private fun seedProcurementWorkflowDemo(users: Map<String, User>, products: Map<String, Product>) {
        if (purchaseRequestRepository.count() > 0) {
            logger.info("Purchase requests already seeded. Skipping procurement workflow demo data.")
            return
        }

        val employee = users["employee@procurement.com"] ?: return
        val storeManager = users["storemanager@procurement.com"] ?: return
        val procurementManager = users["procurementmanager@procurement.com"] ?: return

        val laptop = products["Laptop - Dell Latitude 5440"] ?: return
        val officeChair = products["Office Chair - Ergonomic"] ?: return
        val externalHardDrive = products["External Hard Drive 1TB"] ?: return
        val ledMonitor = products["LED Monitor 24-inch"] ?: return

        val now = Instant.now()

        // ---- PR-0001: fully completed workflow (approved -> PO -> GRN -> stock increased) ----
        val pr1Items = listOf(
            PurchaseRequestItem(
                productId = laptop.id ?: "",
                productName = laptop.name,
                requestedQuantity = 3,
                estimatedUnitPrice = laptop.unitPrice,
                notes = "Replacement laptops for new IT hires"
            )
        )
        val pr1 = purchaseRequestRepository.save(
            PurchaseRequest(
                prNumber = "PR-0001",
                employeeId = employee.id ?: "",
                employeeName = "${employee.firstName} ${employee.lastName}",
                department = "Information Technology",
                items = pr1Items,
                purpose = "Onboarding new IT department hires",
                businessJustification = "Three new engineers joining next month require laptops",
                priority = Priority.HIGH,
                requiredDate = now.plus(14, ChronoUnit.DAYS),
                status = PurchaseRequestStatus.CONVERTED_TO_PO,
                currentApprovalLevel = null,
                createdBy = employee.email,
                updatedBy = procurementManager.email,
                createdAt = now.minus(10, ChronoUnit.DAYS),
                updatedAt = now.minus(6, ChronoUnit.DAYS)
            )
        )

        approvalHistoryRepository.saveAll(
            listOf(
                ApprovalHistory(
                    purchaseRequestId = pr1.id ?: "",
                    prNumber = pr1.prNumber,
                    level = ApprovalLevel.STORE_MANAGER,
                    approverId = storeManager.id ?: "",
                    approverName = "${storeManager.firstName} ${storeManager.lastName}",
                    decision = ApprovalDecision.APPROVED,
                    comments = "Stock confirmed unavailable, approved for procurement",
                    timestamp = now.minus(9, ChronoUnit.DAYS)
                ),
                ApprovalHistory(
                    purchaseRequestId = pr1.id ?: "",
                    prNumber = pr1.prNumber,
                    level = ApprovalLevel.PROCUREMENT_MANAGER,
                    approverId = procurementManager.id ?: "",
                    approverName = "${procurementManager.firstName} ${procurementManager.lastName}",
                    decision = ApprovalDecision.APPROVED,
                    comments = "Approved, within budget threshold",
                    timestamp = now.minus(8, ChronoUnit.DAYS)
                )
            )
        )

        val po1ItemInitial = PurchaseOrderItem(
            productId = laptop.id ?: "",
            productName = laptop.name,
            orderedQuantity = 3,
            unitPrice = laptop.unitPrice,
            taxRate = 5.0,
            discount = 0.0,
            receivedQuantity = 3
        )
        val po1Subtotal = po1ItemInitial.lineSubtotal
        val po1Tax = po1ItemInitial.lineTax
        val po1GrandTotal = (po1Subtotal - po1ItemInitial.discount) + po1Tax

        val po1 = purchaseOrderRepository.save(
            PurchaseOrder(
                poNumber = "PO-0001",
                purchaseRequestId = pr1.id ?: "",
                prNumber = pr1.prNumber,
                supplierId = laptop.supplierId,
                supplierName = "Tech Solutions Ltd",
                supplierContact = "Ahmed Raza",
                items = listOf(po1ItemInitial),
                subtotal = po1Subtotal,
                taxTotal = po1Tax,
                discountTotal = po1ItemInitial.discount,
                shipping = 25.0,
                grandTotal = po1GrandTotal + 25.0,
                currency = "USD",
                expectedDeliveryDate = now.minus(3, ChronoUnit.DAYS),
                status = PurchaseOrderStatus.COMPLETED,
                timeline = listOf(
                    PurchaseOrderTimelineEntry(
                        status = PurchaseOrderStatus.DRAFT,
                        remarks = "Purchase Order created from purchase request PR-0001",
                        actorId = procurementManager.id ?: "",
                        actorName = "${procurementManager.firstName} ${procurementManager.lastName}",
                        timestamp = now.minus(8, ChronoUnit.DAYS)
                    ),
                    PurchaseOrderTimelineEntry(
                        status = PurchaseOrderStatus.ISSUED,
                        remarks = "Purchase Order issued to supplier",
                        actorId = procurementManager.id ?: "",
                        actorName = "${procurementManager.firstName} ${procurementManager.lastName}",
                        timestamp = now.minus(7, ChronoUnit.DAYS)
                    ),
                    PurchaseOrderTimelineEntry(
                        status = PurchaseOrderStatus.COMPLETED,
                        remarks = "Purchase Order fully received and completed",
                        actorId = storeManager.id ?: "",
                        actorName = "${storeManager.firstName} ${storeManager.lastName}",
                        timestamp = now.minus(6, ChronoUnit.DAYS)
                    )
                ),
                createdBy = procurementManager.email,
                updatedBy = storeManager.email,
                createdAt = now.minus(8, ChronoUnit.DAYS),
                updatedAt = now.minus(6, ChronoUnit.DAYS)
            )
        )

        goodsReceiptRepository.save(
            GoodsReceipt(
                grnNumber = "GRN-0001",
                purchaseOrderId = po1.id ?: "",
                poNumber = po1.poNumber,
                supplierId = po1.supplierId,
                supplierName = po1.supplierName,
                items = listOf(
                    GoodsReceiptItem(
                        productId = laptop.id ?: "",
                        productName = laptop.name,
                        receivedQuantity = 3,
                        rejectedQuantity = 0,
                        batchNumber = "BATCH-2026-001"
                    )
                ),
                warehouse = "Main Warehouse",
                storageLocation = "Rack A1",
                receivedBy = storeManager.email,
                receivedDate = now.minus(6, ChronoUnit.DAYS),
                inspectionStatus = InspectionStatus.PASSED,
                qualityNotes = "All units inspected and in good condition",
                status = GoodsReceiptStatus.COMPLETED,
                createdAt = now.minus(6, ChronoUnit.DAYS)
            )
        )

        // The GRN above is the only stock-increasing event in the system, so we
        // apply it here exactly as GoodsReceiptService would: laptop stock 0 -> 3.
        val updatedLaptop = laptop.copy(
            currentStock = laptop.currentStock + 3,
            status = Product.deriveStatus(laptop.currentStock + 3, laptop.minimumStock),
            updatedAt = now.minus(6, ChronoUnit.DAYS)
        )
        productRepository.save(updatedLaptop)

        // ---- PR-0002: awaiting Store Manager approval ----
        purchaseRequestRepository.save(
            PurchaseRequest(
                prNumber = "PR-0002",
                employeeId = employee.id ?: "",
                employeeName = "${employee.firstName} ${employee.lastName}",
                department = "Operations",
                items = listOf(
                    PurchaseRequestItem(
                        productId = officeChair.id ?: "",
                        productName = officeChair.name,
                        requestedQuantity = 10,
                        estimatedUnitPrice = officeChair.unitPrice,
                        notes = "New workstations for the operations floor"
                    )
                ),
                purpose = "Furnishing new operations workstations",
                businessJustification = "Ten new hires starting next quarter need seating",
                priority = Priority.MEDIUM,
                requiredDate = now.plus(30, ChronoUnit.DAYS),
                status = PurchaseRequestStatus.SUBMITTED,
                currentApprovalLevel = ApprovalLevel.STORE_MANAGER,
                createdBy = employee.email,
                createdAt = now.minus(2, ChronoUnit.DAYS),
                updatedAt = now.minus(2, ChronoUnit.DAYS)
            )
        )

        // ---- PR-0003: rejected at Store Manager stage ----
        val pr3 = purchaseRequestRepository.save(
            PurchaseRequest(
                prNumber = "PR-0003",
                employeeId = employee.id ?: "",
                employeeName = "${employee.firstName} ${employee.lastName}",
                department = "Information Technology",
                items = listOf(
                    PurchaseRequestItem(
                        productId = externalHardDrive.id ?: "",
                        productName = externalHardDrive.name,
                        requestedQuantity = 5,
                        estimatedUnitPrice = externalHardDrive.unitPrice
                    ),
                    PurchaseRequestItem(
                        productId = ledMonitor.id ?: "",
                        productName = ledMonitor.name,
                        requestedQuantity = 4,
                        estimatedUnitPrice = ledMonitor.unitPrice
                    )
                ),
                purpose = "Additional backup drives and monitors",
                businessJustification = "Requested for personal desk setup preference",
                priority = Priority.LOW,
                requiredDate = now.plus(20, ChronoUnit.DAYS),
                status = PurchaseRequestStatus.REJECTED,
                currentApprovalLevel = null,
                createdBy = employee.email,
                updatedBy = storeManager.email,
                createdAt = now.minus(5, ChronoUnit.DAYS),
                updatedAt = now.minus(4, ChronoUnit.DAYS)
            )
        )

        approvalHistoryRepository.save(
            ApprovalHistory(
                purchaseRequestId = pr3.id ?: "",
                prNumber = pr3.prNumber,
                level = ApprovalLevel.STORE_MANAGER,
                approverId = storeManager.id ?: "",
                approverName = "${storeManager.firstName} ${storeManager.lastName}",
                decision = ApprovalDecision.REJECTED,
                comments = "Not a business-critical requirement; existing equipment is sufficient",
                timestamp = now.minus(4, ChronoUnit.DAYS)
            )
        )

        // ---- PR-0004: high-value request awaiting Finance Manager approval ----
        val pr4 = purchaseRequestRepository.save(
            PurchaseRequest(
                prNumber = "PR-0004",
                employeeId = employee.id ?: "",
                employeeName = "${employee.firstName} ${employee.lastName}",
                department = "Finance",
                items = listOf(
                    PurchaseRequestItem(
                        productId = laptop.id ?: "",
                        productName = laptop.name,
                        requestedQuantity = 10,
                        estimatedUnitPrice = laptop.unitPrice,
                        notes = "Department-wide laptop refresh"
                    )
                ),
                purpose = "Annual laptop refresh for the finance department",
                businessJustification = "Existing laptops are past end-of-life and impacting productivity",
                priority = Priority.HIGH,
                requiredDate = now.plus(45, ChronoUnit.DAYS),
                status = PurchaseRequestStatus.UNDER_REVIEW,
                currentApprovalLevel = ApprovalLevel.FINANCE_MANAGER,
                createdBy = employee.email,
                updatedBy = procurementManager.email,
                createdAt = now.minus(3, ChronoUnit.DAYS),
                updatedAt = now.minus(1, ChronoUnit.DAYS)
            )
        )

        approvalHistoryRepository.saveAll(
            listOf(
                ApprovalHistory(
                    purchaseRequestId = pr4.id ?: "",
                    prNumber = pr4.prNumber,
                    level = ApprovalLevel.STORE_MANAGER,
                    approverId = storeManager.id ?: "",
                    approverName = "${storeManager.firstName} ${storeManager.lastName}",
                    decision = ApprovalDecision.APPROVED,
                    comments = "Confirmed need across the finance team",
                    timestamp = now.minus(2, ChronoUnit.DAYS)
                ),
                ApprovalHistory(
                    purchaseRequestId = pr4.id ?: "",
                    prNumber = pr4.prNumber,
                    level = ApprovalLevel.PROCUREMENT_MANAGER,
                    approverId = procurementManager.id ?: "",
                    approverName = "${procurementManager.firstName} ${procurementManager.lastName}",
                    decision = ApprovalDecision.APPROVED,
                    comments = "Supplier availability confirmed, forwarding to Finance due to value",
                    timestamp = now.minus(1, ChronoUnit.DAYS)
                )
            )
        )

        logger.info("Seeded a complete procurement workflow demo: PR-0001..PR-0004, PO-0001, GRN-0001")
    }

    private data class SupplierSeed(
        val companyName: String,
        val contactPerson: String,
        val email: String,
        val phone: String,
        val address: String,
        val city: String,
        val state: String,
        val country: String,
        val postalCode: String,
        val taxNumber: String,
        val paymentTerms: String,
        val deliveryLeadTime: Int
    )
}
