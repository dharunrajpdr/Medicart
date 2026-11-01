# MediCart – Online Pharmacy System

## Project Status

This repository is in Phase 4 of the MediCart online pharmacy case study. The Phase 1 foundation, Phase 2 MySQL schema, Phase 3 entity models, and Phase 4 domain behavior are prepared. Service and application workflows are deferred to their approved phases. The official case study is treated as the source of truth for requirements, naming, layered architecture, and sprint progression.

## Project Overview
Wellstone Pharmacy Network operates an online storefront for prescription and over-the-counter medicines, fulfilled by three regional warehouses. The system must support customer management, medicine catalog management, prescription verification, stock tracking, order processing, reporting, and concurrent operations.

## Technology Stack
- Java 21 or later (LTS)
- MySQL
- JDBC
- JUnit 5
- Mockito
- Git and GitHub workflow
- Eclipse/VS Code compatible project layout

## Official Sprint Breakdown
### Sprint 1 – Database
- Phase 1: requirement analysis and project foundation
- Phase 2: database design and implementation

### Sprint 2 – Object-Oriented Programming
- Phase 3: entity classes and encapsulation
- Phase 4: inheritance and business logic

### Sprint 3 – Advanced Programming
- Phase 5: service interfaces and custom exceptions
- Phase 6: collection-based service implementation
- Phase 7: report generation and analytics
- Phase 8: concurrency and multithreading

### Sprint 4 – JDBC, Testing and Integration
- Phase 9: JDBC database connectivity
- Phase 10: main module, testing and final integration

## Functional Requirements Summary
- Customer creation, retrieval, update, and deletion
- Medicine catalog support with OTC and prescription-based validation
- Prescription upload, verification, approval, and rejection workflows
- Warehouse inventory management and stock checks
- Order placement and lifecycle rules
- Expiry tracking and low-stock review
- Report generation for revenue, stock, and prescription status
- Concurrent operations for stock and prescription workflows
- Menu-driven console operations

## Non-Functional Requirements Summary
- Object-oriented design with encapsulation, inheritance, polymorphism, and composition
- Reusable and maintainable Java code
- Validation and user-defined exception handling
- PreparedStatement-based SQL access
- Transaction management and rollback support
- Unit testing with JUnit 5 and Mockito
- One PR per development phase

## Database Requirements Summary
The official schema includes the following tables:
- Customer
- Medicine
- Warehouse
- Stock
- Prescription
- Order

The database must enforce primary keys, foreign keys, unique constraints, checks, defaults, and referential integrity.

## Expected Entity Manifest
- Customer
- Medicine
- OverTheCounterMedicine
- PrescriptionMedicine
- Prescription
- Warehouse
- Stock
- Order

## Expected Service Manifest
- ICustomerService / CustomerService
- IMedicineService / MedicineService
- IWarehouseService / WarehouseService
- IStockService / StockService
- IPrescriptionService / PrescriptionService
- IOrderService / OrderService
- DatabaseContext
- ReportGenerator

## Expected Exception Manifest
- PrescriptionRequiredException
- InsufficientStockException
- ExpiredMedicineException
- PrescriptionAlreadyVerifiedException
- InvalidMedicineDataException
- OrderNotFoundException
- WarehouseCapacityException

## Expected Report Methods
- GetMedicinesNearExpiry(medicines, daysThreshold)
- GetLowStockMedicines(stock, threshold)
- GetTopSellingMedicines(items, topN)
- GetPendingPrescriptions(prescriptions)
- GetMonthlyRevenueByWarehouse(orders)
- GetFrequentCustomers(orders, minOrders)

## Expected Concurrency Scenarios
- Parallel warehouse stock lookup
- Bulk expiry sweep
- Prescription verification queue
- Dashboard loading of independent data sets

## Project Structure
```text
MediCart/
├── .github/
│   └── pull_request_template.md
├── resources/
│   └── db.properties
├── src/
│   ├── dao/
│   ├── entity/
│   │   └── model/
│   ├── exception/
│   ├── main/
│   └── util/
├── test/
│   ├── dao/
│   ├── entity/
│   ├── exception/
│   └── reports/
├── database/
│   ├── schema.sql
│   └── sample_data.sql
├── README.md
├── .gitignore
├── .classpath
├── .project
└── .gitkeep (optional placeholder files in empty folders)
```

## Phase 4 Status
The entity layer and its domain behaviors are implemented under `src/entity/model` with Java package `entity.model`.

Current status:
- Requirement analysis complete
- Architecture plan documented
- Project structure created
- Database schema implemented in `database/schema.sql`
- Sample data added in `database/sample_data.sql`
- Entity models implemented for customer, medicine variants, prescription, warehouse, stock, and order
- Entity validation covers required text, identifiers, non-negative amounts and quantities, status values, and defensive collection copies
- Domain behavior includes expiry checks, prescription approval/rejection, stock reservation/release, cart validation, and order checkout holds
- Executable domain behavior checks are under `test/entity`; run them with `javac -d bin src\entity\model\*.java test\entity\DomainBehaviorChecks.java` followed by `java -cp bin entity.DomainBehaviorChecks`
- Order totals sum one medicine price per entry in `Order.Items`; item quantities are not defined by the case study
- Domain-specific exception classes remain for Phase 5

## Phase-to-Branch Mapping
- feature/phase-01-foundation
- feature/phase-02-database
- feature/phase-03-entities
- feature/phase-04-domain-logic

## Current Branch
The repository is currently on the `feature/phase-04-domain-logic` branch.

## Database Summary
The implemented schema includes:
- Customer
- Warehouse
- Medicine
- Stock
- Prescription
- Order

The schema enforces primary keys, foreign keys, unique constraints, checks, defaults, and indexes to reflect the case study requirements.

## Entity Modeling Notes
- `Medicine` is abstract; prescription requirement is represented by its OTC/prescription subtype.
- Foreign-key relationships are modeled with entity references where applicable.
- `Order.Items` is currently a `List<Medicine>` as approved; item quantities and order-item persistence are not modeled because the case study does not define an OrderItem entity or table.
