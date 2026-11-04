DROP TABLE IF EXISTS `Order`;
DROP TABLE IF EXISTS Prescription;
DROP TABLE IF EXISTS Stock;
DROP TABLE IF EXISTS Medicine;
DROP TABLE IF EXISTS Warehouse;
DROP TABLE IF EXISTS Customer;

CREATE TABLE Customer (
    CustomerID INT NOT NULL AUTO_INCREMENT,
    FirstName VARCHAR(50) NOT NULL,
    LastName VARCHAR(50) NOT NULL,
    Email VARCHAR(100) NOT NULL,
    Address VARCHAR(200) NOT NULL,
    PRIMARY KEY (CustomerID),
    UNIQUE (Email)
) ENGINE=InnoDB;

CREATE TABLE Warehouse (
    WarehouseID INT NOT NULL AUTO_INCREMENT,
    WarehouseName VARCHAR(100) NOT NULL,
    City VARCHAR(50) NOT NULL,
    PRIMARY KEY (WarehouseID)
) ENGINE=InnoDB;

CREATE TABLE Medicine (
    MedicineID INT NOT NULL AUTO_INCREMENT,
    Name VARCHAR(100) NOT NULL,
    RequiresPrescription BOOLEAN NOT NULL DEFAULT FALSE,
    Price DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    ExpiryDate DATE NOT NULL,
    WarehouseID INT NOT NULL,
    PRIMARY KEY (MedicineID),
    CONSTRAINT fk_medicine_warehouse FOREIGN KEY (WarehouseID) REFERENCES Warehouse(WarehouseID),
    CONSTRAINT chk_medicine_price CHECK (Price >= 0)
) ENGINE=InnoDB;

CREATE TABLE Stock (
    StockID INT NOT NULL AUTO_INCREMENT,
    MedicineID INT NOT NULL,
    WarehouseID INT NOT NULL,
    QuantityAvailable INT NOT NULL DEFAULT 0,
    PRIMARY KEY (StockID),
    CONSTRAINT fk_stock_medicine FOREIGN KEY (MedicineID) REFERENCES Medicine(MedicineID),
    CONSTRAINT fk_stock_warehouse FOREIGN KEY (WarehouseID) REFERENCES Warehouse(WarehouseID),
    CONSTRAINT chk_stock_quantity CHECK (QuantityAvailable >= 0),
    UNIQUE (MedicineID, WarehouseID)
) ENGINE=InnoDB;

CREATE TABLE Prescription (
    PrescriptionID INT NOT NULL AUTO_INCREMENT,
    CustomerID INT NOT NULL,
    DocumentPath VARCHAR(200) NOT NULL,
    UploadedOn DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    VerifiedBy INT NULL,
    VerifiedOn DATE NULL,
    Status VARCHAR(20) NOT NULL DEFAULT 'Pending',
    PRIMARY KEY (PrescriptionID),
    CONSTRAINT fk_prescription_customer FOREIGN KEY (CustomerID) REFERENCES Customer(CustomerID),
    CONSTRAINT chk_prescription_status CHECK (Status IN ('Pending', 'Approved', 'Rejected'))
) ENGINE=InnoDB;

CREATE TABLE `Order` (
    OrderID INT NOT NULL AUTO_INCREMENT,
    CustomerID INT NOT NULL,
    OrderDate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PrescriptionID INT NULL,
    Status VARCHAR(20) NOT NULL DEFAULT 'Placed',
    TotalAmount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    PRIMARY KEY (OrderID),
    CONSTRAINT fk_order_customer FOREIGN KEY (CustomerID) REFERENCES Customer(CustomerID),
    CONSTRAINT fk_order_prescription FOREIGN KEY (PrescriptionID) REFERENCES Prescription(PrescriptionID),
    CONSTRAINT chk_order_status CHECK (Status IN ('Placed', 'Pending Verification', 'Shipped', 'Cancelled', 'Completed'))
) ENGINE=InnoDB;

CREATE INDEX idx_medicine_warehouse ON Medicine(WarehouseID);
CREATE INDEX idx_stock_warehouse ON Stock(WarehouseID);
CREATE INDEX idx_prescription_customer ON Prescription(CustomerID);
CREATE INDEX idx_order_customer ON `Order`(CustomerID);
CREATE INDEX idx_order_prescription ON `Order`(PrescriptionID);
