INSERT INTO Customer (FirstName, LastName, Email, Address) VALUES
('Aarav', 'Sharma', 'aarav.sharma@example.com', 'MG Road, Bengaluru'),
('Nisha', 'Patel', 'nisha.patel@example.com', 'Koramangala, Bengaluru'),
('Rohan', 'Iyer', 'rohan.iyer@example.com', 'Anna Nagar, Chennai'),
('Meera', 'Nair', 'meera.nair@example.com', 'Banjara Hills, Hyderabad'),
('Karan', 'Singh', 'karan.singh@example.com', 'Indore, Madhya Pradesh');

INSERT INTO Warehouse (WarehouseName, City) VALUES
('North Hub', 'Bengaluru'),
('Central Depot', 'Chennai'),
('South Distribution Centre', 'Hyderabad');

INSERT INTO Medicine (Name, RequiresPrescription, Price, ExpiryDate, WarehouseID) VALUES
('Paracetamol', FALSE, 45.00, '2026-12-31', 1),
('Amoxicillin', TRUE, 180.00, '2026-10-15', 1),
('Cough Syrup', FALSE, 120.00, '2026-08-30', 2),
('Vitamin D3', FALSE, 260.00, '2027-02-20', 3),
('Antibiotic Plus', TRUE, 340.00, '2026-09-30', 2),
('Pain Relief Cream', FALSE, 90.00, '2027-05-10', 3);

INSERT INTO Stock (MedicineID, WarehouseID, QuantityAvailable) VALUES
(1, 1, 120),
(2, 1, 45),
(3, 2, 60),
(4, 3, 35),
(5, 2, 22),
(6, 3, 80);

INSERT INTO Prescription (CustomerID, DocumentPath, VerifiedBy, VerifiedOn, Status) VALUES
(2, '/prescriptions/nisha_2026_01.pdf', 101, '2026-01-12', 'Approved'),
(4, '/prescriptions/meera_2026_02.pdf', 102, '2026-02-10', 'Pending');

INSERT INTO `Order` (CustomerID, OrderDate, PrescriptionID, Status, TotalAmount) VALUES
(1, '2026-01-08 09:30:00', NULL, 'Completed', 90.00),
(2, '2026-01-12 15:45:00', 1, 'Shipped', 220.00),
(3, '2026-02-03 11:15:00', NULL, 'Placed', 120.00),
(4, '2026-02-10 17:20:00', 2, 'Pending Verification', 260.00),
(5, '2026-02-18 12:00:00', NULL, 'Completed', 150.00);
