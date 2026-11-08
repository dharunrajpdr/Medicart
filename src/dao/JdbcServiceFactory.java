package dao;

import java.util.Objects;

public final class JdbcServiceFactory {
    private final ICustomerService customerService;
    private final IMedicineService medicineService;
    private final IWarehouseService warehouseService;
    private final IStockService stockService;
    private final IPrescriptionService prescriptionService;
    private final IOrderService orderService;

    public JdbcServiceFactory() {
        this(new DatabaseContext());
    }

    public JdbcServiceFactory(DatabaseContext database) {
        Objects.requireNonNull(database, "database cannot be null");
        ICustomerDao customerDao = new JdbcCustomerDao(database);
        IMedicineDao medicineDao = new JdbcMedicineDao(database);
        IWarehouseDao warehouseDao = new JdbcWarehouseDao(database);
        IStockDao stockDao = new JdbcStockDao(database);
        IPrescriptionDao prescriptionDao = new JdbcPrescriptionDao(database);
        IOrderDao orderDao = new JdbcOrderDao(database);

        prescriptionService = new PrescriptionService(prescriptionDao);
        customerService = new CustomerService(customerDao, prescriptionService);
        medicineService = new MedicineService(medicineDao);
        warehouseService = new WarehouseService(warehouseDao, stockDao);
        stockService = new StockService(stockDao);
        orderService = new OrderService(orderDao);
    }

    public ICustomerService getCustomerService() {
        return customerService;
    }

    public IMedicineService getMedicineService() {
        return medicineService;
    }

    public IWarehouseService getWarehouseService() {
        return warehouseService;
    }

    public IStockService getStockService() {
        return stockService;
    }

    public IPrescriptionService getPrescriptionService() {
        return prescriptionService;
    }

    public IOrderService getOrderService() {
        return orderService;
    }
}
