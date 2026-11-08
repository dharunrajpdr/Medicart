package dao;

import entity.model.Customer;
import entity.model.Medicine;
import entity.model.Order;
import entity.model.OverTheCounterMedicine;
import entity.model.Prescription;
import entity.model.PrescriptionMedicine;
import entity.model.Stock;
import entity.model.Warehouse;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class JdbcServiceWiringChecks {
    private static int checks;

    private JdbcServiceWiringChecks() {
    }

    public static void main(String[] args) throws Exception {
        Map<String, AtomicInteger> calls = new ConcurrentHashMap<>();
        Customer customer = customer();
        Prescription pending = prescription(customer, "Pending");
        Prescription approved = prescription(customer, "Pending");

        ICustomerDao customerDao = dao(ICustomerDao.class, calls, Map.of());
        IMedicineDao medicineDao = dao(IMedicineDao.class, calls, Map.of());
        IStockDao stockDao = dao(IStockDao.class, calls, Map.of(
                "adjustAvailability", true));
        IWarehouseDao warehouseDao = dao(IWarehouseDao.class, calls, Map.of());
        IPrescriptionDao prescriptionDao = dao(IPrescriptionDao.class, calls, Map.of(
                "findById", pending, "transitionStatus", true));
        IOrderDao orderDao = dao(IOrderDao.class, calls, Map.of());

        new CustomerService(customerDao, new PrescriptionService()).addCustomer(customer);
        new MedicineService(medicineDao).addMedicine(medicine());
        new StockService(stockDao).addStock(stock());
        new WarehouseService(warehouseDao, stockDao).addWarehouse(warehouse());

        PrescriptionService prescriptionService = new PrescriptionService(prescriptionDao);
        prescriptionService.approve(approved);
        check("Approved".equals(approved.getStatus()),
                "JDBC-backed prescription service applies approved status");

        Order order = order();
        new OrderService(orderDao).checkout(order);

        check(count(calls, "ICustomerDao.insert") == 1,
                "customer service delegates insert to DAO interface");
        check(count(calls, "IMedicineDao.insert") == 1,
                "medicine service delegates insert to DAO interface");
        check(count(calls, "IStockDao.insert") == 1,
                "stock service delegates insert to DAO interface");
        check(count(calls, "IWarehouseDao.insert") == 1,
                "warehouse service delegates insert to DAO interface");
        check(count(calls, "IPrescriptionDao.transitionStatus") == 1,
                "prescription workflow delegates conditional transition to DAO");
        check(count(calls, "IOrderDao.insert") == 1,
                "order checkout delegates persistence to DAO interface");

        JdbcServiceFactory factory = new JdbcServiceFactory(new DatabaseContext());
        check(factory.getCustomerService() != null
                        && factory.getMedicineService() != null
                        && factory.getWarehouseService() != null
                        && factory.getStockService() != null
                        && factory.getPrescriptionService() != null
                        && factory.getOrderService() != null,
                "JDBC factory wires all six services without opening a connection");
        System.out.println("JDBC service wiring checks passed: " + checks);
    }

    private static <T> T dao(Class<T> daoType, Map<String, AtomicInteger> calls,
            Map<String, Object> configuredResults) {
        return daoType.cast(Proxy.newProxyInstance(
                daoType.getClassLoader(), new Class<?>[] {daoType}, (proxy, method, args) -> {
                    calls.computeIfAbsent(daoType.getSimpleName() + "." + method.getName(),
                            ignored -> new AtomicInteger()).incrementAndGet();
                    Object configured = configuredResults.get(method.getName());
                    if (configured != null) {
                        return configured;
                    }
                    if (method.getReturnType().equals(boolean.class)) {
                        return false;
                    }
                    if (List.class.isAssignableFrom(method.getReturnType())) {
                        return new ArrayList<>();
                    }
                    return null;
                }));
    }

    private static int count(Map<String, AtomicInteger> calls, String key) {
        AtomicInteger count = calls.get(key);
        return count == null ? 0 : count.get();
    }

    private static Customer customer() {
        return new Customer(1, "Test", "Customer", "jdbc@example.com",
                "Test Address", List.of());
    }

    private static Medicine medicine() {
        return new OverTheCounterMedicine(1, "OTC", new BigDecimal("10.00"),
                LocalDate.now().plusDays(10), 1);
    }

    private static Warehouse warehouse() {
        return new Warehouse(1, "Test Warehouse", "City", new ArrayList<>());
    }

    private static Stock stock() {
        return new Stock(1, medicine(), warehouse(), 5);
    }

    private static Prescription prescription(Customer customer, String status) {
        return new Prescription(1, customer, "/prescriptions/test.pdf",
                null, null, status);
    }

    private static Order order() {
        PrescriptionMedicine prescriptionMedicine = new PrescriptionMedicine(2,
                "Prescription medicine", new BigDecimal("20.00"),
                LocalDate.now().plusDays(10), 1, null);
        return new Order(0, customer(), List.of(prescriptionMedicine),
                LocalDateTime.now(), null, "Pending Verification", BigDecimal.ZERO);
    }

    private static void check(boolean condition, String description) {
        checks++;
        if (!condition) {
            throw new AssertionError(description);
        }
    }
}
