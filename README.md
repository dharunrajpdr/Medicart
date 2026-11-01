# MediCart – Online Pharmacy System

## Phase 1 – Requirement Analysis and Project Foundation

This repository is being prepared for the MediCart online pharmacy case study in Java. The project is currently in the Phase 1 foundation stage and does not contain application business logic yet. The official case study is treated as the source of truth for requirements, naming, layered architecture, and sprint progression.

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

## Phase 2 Status
The database layer has been implemented in the official MySQL schema.

Current status:
- Requirement analysis complete
- Architecture plan documented
- Project structure created
- Database schema implemented in `database/schema.sql`
- Sample data added in `database/sample_data.sql`
- This phase is the database design and implementation stage for the MediCart case study

## Phase-to-Branch Mapping
- feature/phase-01-foundation
- feature/phase-02-database

## Current Branch
The repository is currently on the `feature/phase-02-database` branch.

## Database Summary
The implemented schema includes:
- Customer
- Warehouse
- Medicine
- Stock
- Prescription
- Order

The schema enforces primary keys, foreign keys, unique constraints, checks, defaults, and indexes to reflect the case study requirements.
