CREATE TABLE OrderItem (
    OrderID INT NOT NULL,
    ItemNumber INT NOT NULL,
    MedicineID INT NOT NULL,
    PrescriptionID INT NULL,
    PRIMARY KEY (OrderID, ItemNumber),
    CONSTRAINT fk_order_item_order FOREIGN KEY (OrderID)
        REFERENCES `Order`(OrderID) ON DELETE CASCADE,
    CONSTRAINT fk_order_item_medicine FOREIGN KEY (MedicineID)
        REFERENCES Medicine(MedicineID),
    CONSTRAINT fk_order_item_prescription FOREIGN KEY (PrescriptionID)
        REFERENCES Prescription(PrescriptionID),
    CONSTRAINT chk_order_item_number CHECK (ItemNumber > 0)
) ENGINE=InnoDB;

CREATE INDEX idx_order_item_medicine ON OrderItem(MedicineID);
