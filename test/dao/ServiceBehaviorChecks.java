package dao;

import entity.model.Customer;
import entity.model.Medicine;
import entity.model.Order;
import entity.model.OverTheCounterMedicine;
import entity.model.Prescription;
import entity.model.PrescriptionMedicine;
import entity.model.Stock;
import entity.model.Warehouse;
import exception.ExpiredMedicineException;
import exception.InsufficientStockException;
import exception.InvalidMedicineDataException;
import exception.OrderNotFoundException;
import exception.PrescriptionAlreadyVerifiedException;
import exception.PrescriptionRequiredException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class ServiceBehaviorChecks {
    private static int checks;

    private ServiceBehaviorChecks() {
    }

    public static void main(String[] args) throws Exception {
        checkCustomerService();
        checkMedicineService();
        checkWarehouseService();
        checkStockService();
        checkPrescriptionService();
        checkOrderService();
        System.out.println("Service behavior checks passed: " + checks);
    }

    private static void checkCustomerService() {
        PrescriptionService prescriptions = new PrescriptionService();
        CustomerService service = new CustomerService(prescriptions);
        Customer customer = customer(0, "customer@example.com");
        service.addCustomer(customer);
        check(customer.getCustomerId() == 1, "customer ID is assigned");
        check(service.getCustomerById(1) == customer, "customer lookup");
        check(service.getAllCustomers().size() == 1, "customer list");
        service.getAllCustomers().clear();
        check(service.getAllCustomers().size() == 1, "customer list is a snapshot");
        expectThrows(IllegalArgumentException.class,
                () -> service.addCustomer(customer(0, "CUSTOMER@example.com")));
        customer.setAddress("Updated Address");
        service.updateCustomer(customer);
        check("Updated Address".equals(service.getCustomerById(1).getAddress()),
                "customer update");
        expectThrows(IllegalArgumentException.class,
                () -> service.updateCustomer(customer(99, "missing@example.com")));

        Prescription prescription = pendingPrescription(customer);
        service.uploadPrescription(customer, prescription);
        check(prescription.getPrescriptionId() == 1, "uploaded prescription is registered");
        check(prescriptions.getPrescriptionById(1) == prescription,
                "uploaded prescription is available to prescription service");
        expectThrows(IllegalArgumentException.class,
                () -> service.uploadPrescription(customer(0, "other@example.com"), prescription));
        expectThrows(IllegalStateException.class, () -> service.deleteCustomer(1));
        prescriptions.deletePrescription(prescription.getPrescriptionId());
        service.deleteCustomer(1);
        check(service.getCustomerById(1) == null, "customer deletion");
        expectThrows(IllegalArgumentException.class, () -> service.deleteCustomer(1));
    }

    private static void checkMedicineService() throws Exception {
        MedicineService service = new MedicineService();
        Medicine otc = medicine(0, "OTC", LocalDate.now().plusDays(2), 1);
        service.addMedicine(otc);
        check(otc.getMedicineId() == 1, "medicine ID is assigned");
        check(service.getMedicineById(1) == otc, "medicine lookup");
        check(service.getAllMedicines().size() == 1, "medicine list");
        check(!service.isExpired(otc), "medicine expiry service");
        check(service.isNearExpiry(otc, 2), "near-expiry service");
        service.updateMedicine(otc);
        expectThrows(InvalidMedicineDataException.class, () -> service.addMedicine(null));
        expectThrows(InvalidMedicineDataException.class,
                () -> service.addMedicine(medicine(1, "Duplicate", LocalDate.now().plusDays(10), 1)));

        Order cart = order(0, customer(1, "buyer@example.com"), List.of(), null);
        service.addToCart(otc, cart);
        check(cart.getItems().size() == 1, "OTC item is added to cart");
        PrescriptionMedicine prescriptionMedicine = new PrescriptionMedicine(
                2, "Prescription Medicine", new BigDecimal("20.00"),
                LocalDate.now().plusDays(10), 1, null);
        expectThrows(PrescriptionRequiredException.class,
                () -> service.addToCart(prescriptionMedicine, cart));
        Prescription approvedPrescription = pendingPrescription(cart.getCustomer());
        PrescriptionService prescriptionService = new PrescriptionService();
        prescriptionService.addPrescription(approvedPrescription);
        prescriptionService.approve(approvedPrescription);
        cart.setPrescription(approvedPrescription);
        service.addToCart(prescriptionMedicine, cart);
        check(prescriptionMedicine.getPrescription() == approvedPrescription,
                "cart uses the order's approved prescription when medicine has none");
        Order wrongCustomerCart = order(0, customer(2, "other@example.com"), List.of(), null);
        expectThrows(PrescriptionRequiredException.class,
                () -> service.addToCart(prescriptionMedicine, wrongCustomerCart));
        Medicine expired = medicine(3, "Expired", LocalDate.now().minusDays(1), 1);
        expectThrows(ExpiredMedicineException.class, () -> service.addToCart(expired, cart));
        expectThrows(IllegalArgumentException.class, () -> service.deleteMedicine(90));
        service.deleteMedicine(1);
        check(service.getMedicineById(1) == null, "medicine deletion");
    }

    private static void checkWarehouseService() throws Exception {
        WarehouseService service = new WarehouseService();
        Warehouse warehouse = new Warehouse(0, "Central", "City", new ArrayList<>());
        Medicine medicine = medicine(1, "Stocked", LocalDate.now().plusDays(10), 1);
        warehouse.setStock(List.of(new Stock(1, medicine, warehouse, 5)));
        service.addWarehouse(warehouse);
        check(warehouse.getWarehouseId() == 1, "warehouse ID is assigned");
        check(service.getWarehouseById(1) == warehouse, "warehouse lookup");
        check(service.getAllWarehouses().size() == 1, "warehouse list");
        service.addWarehouse(new Warehouse(0, "central", "Other", new ArrayList<>()));
        check(service.getAllWarehouses().size() == 2,
                "warehouse names are not unique unless specified by the schema");

        service.reserveStock(warehouse, medicine, 3);
        check(warehouse.getStock().get(0).getQuantityAvailable() == 2,
                "warehouse stock reservation");
        expectThrows(InsufficientStockException.class,
                () -> service.reserveStock(warehouse, medicine, 3));
        service.releaseStock(warehouse, medicine, 4);
        check(warehouse.getStock().get(0).getQuantityAvailable() == 6,
                "warehouse stock release");
        expectThrows(IllegalStateException.class, () -> service.deleteWarehouse(1));
        warehouse.setStock(new ArrayList<>());
        service.updateWarehouse(warehouse);
        service.deleteWarehouse(1);
        check(service.getWarehouseById(1) == null, "warehouse deletion");
        service.deleteWarehouse(2);
    }

    private static void checkStockService() {
        StockService service = new StockService();
        Medicine medicine = medicine(1, "Stocked", LocalDate.now().plusDays(10), 1);
        Warehouse warehouse = new Warehouse(1, "Central", "City", new ArrayList<>());
        Stock stock = new Stock(0, medicine, warehouse, 8);
        service.addStock(stock);
        check(stock.getStockId() == 1, "stock ID is assigned");
        check(service.getStockById(1) == stock, "stock lookup");
        check(service.getAllStocks().size() == 1, "stock list");
        expectThrows(IllegalArgumentException.class,
                () -> service.addStock(new Stock(2, medicine, warehouse, 4)));
        stock.setQuantityAvailable(3);
        service.updateStock(stock);
        check(service.getStockById(1).getQuantityAvailable() == 3, "stock update");
        expectThrows(IllegalArgumentException.class,
                () -> service.updateStock(new Stock(99, medicine, warehouse, 2)));
        service.deleteStock(1);
        check(service.getStockById(1) == null, "stock deletion");
    }

    private static void checkPrescriptionService() throws Exception {
        PrescriptionService service = new PrescriptionService();
        Customer customer = customer(1, "patient@example.com");
        Prescription prescription = pendingPrescription(customer);
        service.addPrescription(prescription);
        check(prescription.getPrescriptionId() == 1, "prescription ID is assigned");
        check(service.getPrescriptionById(1) == prescription, "prescription lookup");
        check(service.getAllPrescriptions().size() == 1, "prescription list");
        service.approve(prescription);
        check(prescription.isApproved(), "prescription approval");
        check(prescription.getVerifiedOn() != null, "approval date is recorded");
        expectThrows(PrescriptionAlreadyVerifiedException.class,
                () -> service.approve(prescription));
        Prescription resetPrescription = new Prescription(prescription.getPrescriptionId(),
                customer, prescription.getDocumentPath(), null, null, "Pending");
        expectThrows(IllegalArgumentException.class,
                () -> service.updatePrescription(resetPrescription));

        Prescription rejected = pendingPrescription(customer);
        service.addPrescription(rejected);
        service.reject(rejected);
        check("Rejected".equals(rejected.getStatus()), "prescription rejection");
        expectThrows(PrescriptionAlreadyVerifiedException.class,
                () -> service.reject(rejected));

        Prescription pending = pendingPrescription(customer);
        service.addPrescription(pending);
        PrescriptionMedicine medicine = prescriptionMedicine();
        service.verifyPrescription(medicine, pending);
        check(medicine.getPrescription() == pending && pending.isApproved(),
                "verification approves and links prescription");
        expectThrows(PrescriptionRequiredException.class,
                () -> service.verifyPrescription(medicine, null));
        expectThrows(IllegalArgumentException.class, () -> service.deletePrescription(99));
        service.deletePrescription(pending.getPrescriptionId());
        check(service.getPrescriptionById(pending.getPrescriptionId()) == null,
                "prescription deletion");
    }

    private static void checkOrderService() throws Exception {
        OrderService service = new OrderService();
        Customer customer = customer(1, "buyer@example.com");
        Medicine otc = medicine(1, "OTC", LocalDate.now().plusDays(10), 1);
        Order order = order(0, customer, List.of(otc), null);
        service.checkout(order);
        check(order.getOrderId() == 1, "checked-out order is registered");
        check(new BigDecimal("10.00").compareTo(order.getTotalAmount()) == 0,
                "checkout calculates total");
        check(service.getOrderById(1) == order, "order lookup");
        check(service.getAllOrders().size() == 1, "order list");

        PrescriptionMedicine rx = prescriptionMedicine();
        Order held = order(0, customer, List.of(rx), null);
        service.checkout(held);
        check("Pending Verification".equals(held.getStatus()),
                "checkout holds prescription orders without approval");
        expectThrows(IllegalStateException.class, () -> held.setStatus("Shipped"));

        Order expiredOrder = order(0, customer,
                List.of(medicine(3, "Expired", LocalDate.now().minusDays(1), 1)), null);
        expectThrows(ExpiredMedicineException.class, () -> service.checkout(expiredOrder));
        Order emptyOrder = order(0, customer, List.of(), null);
        expectThrows(IllegalArgumentException.class, () -> service.checkout(emptyOrder));
        expectThrows(OrderNotFoundException.class, () -> service.getOrderById(90));
        expectThrows(OrderNotFoundException.class, () -> service.updateOrder(order(90, customer, List.of(otc), null)));
        expectThrows(OrderNotFoundException.class, () -> service.deleteOrder(90));
        order.setStatus("Completed");
        service.updateOrder(order);
        check("Completed".equals(service.getOrderById(1).getStatus()), "order update");
        service.deleteOrder(1);
        check(service.getAllOrders().size() == 1, "other orders remain after deletion");
        expectThrows(OrderNotFoundException.class, () -> service.getOrderById(1));
    }

    private static Customer customer(int id, String email) {
        return new Customer(id, "Test", "Customer", email, "Address", new ArrayList<>());
    }

    private static Medicine medicine(int id, String name, LocalDate expiryDate, int warehouseId) {
        return new OverTheCounterMedicine(id, name, new BigDecimal("10.00"), expiryDate, warehouseId);
    }

    private static Prescription pendingPrescription(Customer customer) {
        return new Prescription(0, customer, "/prescriptions/test.pdf",
                null, null, "Pending");
    }

    private static PrescriptionMedicine prescriptionMedicine() {
        return new PrescriptionMedicine(2, "Prescription Medicine",
                new BigDecimal("20.00"), LocalDate.now().plusDays(10), 1, null);
    }

    private static Order order(int id, Customer customer, List<Medicine> medicines,
            Prescription prescription) {
        return new Order(id, customer, medicines, LocalDateTime.now(),
                prescription, "Placed", BigDecimal.ZERO);
    }

    private static void check(boolean condition, String description) {
        checks++;
        if (!condition) {
            throw new AssertionError(description);
        }
    }

    private static <T extends Throwable> void expectThrows(Class<T> expected,
            ThrowingRunnable operation) {
        checks++;
        try {
            operation.run();
        } catch (Throwable actual) {
            if (expected.isInstance(actual)) {
                return;
            }
            throw new AssertionError("Expected " + expected.getSimpleName()
                    + " but got " + actual.getClass().getSimpleName(), actual);
        }
        throw new AssertionError("Expected " + expected.getSimpleName());
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
