package com.company.procurement.config

import com.company.procurement.model.Product
import com.company.procurement.model.Role
import com.company.procurement.model.Supplier
import com.company.procurement.model.SupplierStatus
import com.company.procurement.model.User
import com.company.procurement.repository.ProductRepository
import com.company.procurement.repository.SupplierRepository
import com.company.procurement.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
class DataSeeder(
    private val userRepository: UserRepository,
    private val productRepository: ProductRepository,
    private val supplierRepository: SupplierRepository,
    private val passwordEncoder: PasswordEncoder
) : CommandLineRunner {

    private val logger = LoggerFactory.getLogger(DataSeeder::class.java)

    override fun run(vararg args: String?) {
        seedUsers()
        val supplierIds = seedSuppliers()
        seedProducts(supplierIds)
    }

    private fun seedUsers() {
        if (userRepository.count() > 0) {
            logger.info("Users already seeded. Skipping user seed data.")
            return
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

        val employee = User(
            firstName = "Usman",
            lastName = "Employee",
            email = "employee@procurement.com",
            password = passwordEncoder.encode("Employee@123"),
            role = Role.EMPLOYEE,
            active = true
        )

        userRepository.saveAll(listOf(admin, storeManager, employee))
        logger.info("Seeded default users: admin@procurement.com, storemanager@procurement.com, employee@procurement.com")
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

    private fun seedProducts(supplierIdsByCompanyName: Map<String, String>) {
        if (productRepository.count() > 0) {
            logger.info("Products already seeded. Skipping product seed data.")
            return
        }

        val abcOfficeSupplies = supplierIdsByCompanyName.getValue("ABC Office Supplies")
        val techSolutions = supplierIdsByCompanyName.getValue("Tech Solutions Ltd")
        val globalStationers = supplierIdsByCompanyName.getValue("Global Stationers")
        val computerWorld = supplierIdsByCompanyName.getValue("Computer World")
        val primeElectronics = supplierIdsByCompanyName.getValue("Prime Electronics")

        val products = listOf(
            Product(
                name = "A4 Printing Paper (Ream)",
                description = "500 sheets of premium quality A4 printing paper",
                category = "Office Supplies",
                unitPrice = 5.99,
                currentStock = 120,
                minimumStock = 30,
                supplierId = abcOfficeSupplies,
                status = Product.deriveStatus(120, 30)
            ),
            Product(
                name = "Ballpoint Pens (Box of 50)",
                description = "Blue ink ballpoint pens, box of 50",
                category = "Office Supplies",
                unitPrice = 12.50,
                currentStock = 15,
                minimumStock = 20,
                supplierId = globalStationers,
                status = Product.deriveStatus(15, 20)
            ),
            Product(
                name = "Laptop - Dell Latitude 5440",
                description = "14-inch business laptop, Intel i5, 16GB RAM, 512GB SSD",
                category = "IT Equipment",
                unitPrice = 950.00,
                currentStock = 0,
                minimumStock = 5,
                supplierId = techSolutions,
                status = Product.deriveStatus(0, 5)
            ),
            Product(
                name = "Office Chair - Ergonomic",
                description = "Ergonomic mesh office chair with lumbar support",
                category = "Furniture",
                unitPrice = 145.00,
                currentStock = 40,
                minimumStock = 10,
                supplierId = abcOfficeSupplies,
                status = Product.deriveStatus(40, 10)
            ),
            Product(
                name = "Whiteboard Markers (Pack of 12)",
                description = "Assorted color dry-erase whiteboard markers",
                category = "Office Supplies",
                unitPrice = 8.25,
                currentStock = 8,
                minimumStock = 15,
                supplierId = globalStationers,
                status = Product.deriveStatus(8, 15)
            ),
            Product(
                name = "External Hard Drive 1TB",
                description = "Portable USB 3.0 external hard drive, 1TB capacity",
                category = "IT Equipment",
                unitPrice = 65.00,
                currentStock = 25,
                minimumStock = 10,
                supplierId = computerWorld,
                status = Product.deriveStatus(25, 10)
            ),
            Product(
                name = "LED Monitor 24-inch",
                description = "24-inch Full HD LED monitor with HDMI and VGA input",
                category = "IT Equipment",
                unitPrice = 110.00,
                currentStock = 18,
                minimumStock = 6,
                supplierId = primeElectronics,
                status = Product.deriveStatus(18, 6)
            )
        )

        productRepository.saveAll(products)
        logger.info("Seeded ${products.size} default products")
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
