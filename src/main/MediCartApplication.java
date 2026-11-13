package main;

import dao.CustomerService;
import dao.DatabaseContext;
import dao.ICustomerService;
import dao.IMedicineService;
import dao.IOrderService;
import dao.IPrescriptionService;
import dao.IStockService;
import dao.IWarehouseService;
import dao.JdbcServiceFactory;
import dao.MedicineService;
import dao.OrderService;
import dao.PrescriptionService;
import dao.StockService;
import dao.WarehouseService;
import entity.model.Customer;
import entity.model.Medicine;
import entity.model.Order;
import entity.model.OverTheCounterMedicine;
import entity.model.Prescription;
import entity.model.PrescriptionMedicine;
import entity.model.Stock;
import entity.model.Warehouse;
import exception.PrescriptionRequiredException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

public class MediCartApplication {
    private final Scanner scanner;
    private final ICustomerService customerService;
    private final IMedicineService medicineService;
    private final IWarehouseService warehouseService;
    private final IStockService stockService;
    private final IPrescriptionService prescriptionService;
    private final IOrderService orderService;

    public MediCartApplication() {
        this(new Scanner(System.in));
    }

    public MediCartApplication(Scanner scanner) {
        this.scanner = Objects.requireNonNull(scanner, "scanner cannot be null");

        ICustomerService resolvedCustomerService;
        IMedicineService resolvedMedicineService;
        IWarehouseService resolvedWarehouseService;
        IStockService resolvedStockService;
        IPrescriptionService resolvedPrescriptionService;
        IOrderService resolvedOrderService;

        if (jdbcAvailable() && databasePropertiesPresent()) {
            try {
                JdbcServiceFactory factory = new JdbcServiceFactory(new DatabaseContext());
                resolvedCustomerService = factory.getCustomerService();
                resolvedMedicineService = factory.getMedicineService();
                resolvedWarehouseService = factory.getWarehouseService();
                resolvedStockService = factory.getStockService();
                resolvedPrescriptionService = factory.getPrescriptionService();
                resolvedOrderService = factory.getOrderService();
            } catch (RuntimeException exception) {
                resolvedCustomerService = new CustomerService();
                resolvedMedicineService = new MedicineService();
                resolvedWarehouseService = new WarehouseService();
                resolvedStockService = new StockService();
                resolvedPrescriptionService = new PrescriptionService();
                resolvedOrderService = new OrderService();
            }
        } else {
            resolvedCustomerService = new CustomerService();
            resolvedMedicineService = new MedicineService();
            resolvedWarehouseService = new WarehouseService();
            resolvedStockService = new StockService();
            resolvedPrescriptionService = new PrescriptionService();
            resolvedOrderService = new OrderService();
        }

        this.customerService = resolvedCustomerService;
        this.medicineService = resolvedMedicineService;
        this.warehouseService = resolvedWarehouseService;
        this.stockService = resolvedStockService;
        this.prescriptionService = resolvedPrescriptionService;
        this.orderService = resolvedOrderService;

        seedStarterData();
    }

    public void run() {
        boolean running = true;
        while (running) {
            printMenu();
            System.out.print("Choose an option: ");
            String input = scanner.nextLine().trim();
            try {
                switch (input) {
                    case "1":
                        addCustomer();
                        break;
                    case "2":
                        listCustomers();
                        break;
                    case "3":
                        addMedicine();
                        break;
                    case "4":
                        listMedicines();
                        break;
                    case "5":
                        addWarehouseAndStock();
                        break;
                    case "6":
                        uploadPrescription();
                        break;
                    case "7":
                        placeOrder();
                        break;
                    case "8":
                        listOrders();
                        break;
                    case "9":
                        running = false;
                        break;
                    default:
                        System.out.println("Invalid option. Please choose a number from the menu.");
                        break;
                }
            } catch (Exception exception) {
                System.out.println("Operation failed: " + exception.getMessage());
            }

            if (running) {
                System.out.println();
            }
        }

        System.out.println("Thank you for using MediCart. Closing the application.");
    }

    private void printMenu() {
        System.out.println("\n=== MediCart Pharmacy Console ===");
        System.out.println("1. Add customer");
        System.out.println("2. List customers");
        System.out.println("3. Add medicine");
        System.out.println("4. List medicines");
        System.out.println("5. Add warehouse and stock");
        System.out.println("6. Upload prescription");
        System.out.println("7. Place order");
        System.out.println("8. List orders");
        System.out.println("9. Exit");
    }

    private void addCustomer() {
        System.out.print("First name: ");
        String firstName = scanner.nextLine().trim();
        System.out.print("Last name: ");
        String lastName = scanner.nextLine().trim();
        System.out.print("Email: ");
        String email = scanner.nextLine().trim();
        System.out.print("Address: ");
        String address = scanner.nextLine().trim();

        Customer customer = new Customer(0, firstName, lastName, email, address, new ArrayList<>());
        customerService.addCustomer(customer);
        System.out.println("Customer added successfully with ID " + customer.getCustomerId());
    }

    private void listCustomers() {
        List<Customer> customers = customerService.getAllCustomers();
        if (customers.isEmpty()) {
            System.out.println("No customers found.");
            return;
        }

        System.out.println("Customers:");
        for (Customer customer : customers) {
            System.out.println("- " + customer.getCustomerId() + ": "
                    + customer.getFirstName() + " " + customer.getLastName()
                    + " (" + customer.getEmail() + ")");
        }
    }

    private void addMedicine() {
        System.out.print("Medicine name: ");
        String name = scanner.nextLine().trim();
        System.out.print("Price: ");
        BigDecimal price = new BigDecimal(scanner.nextLine().trim());
        System.out.print("Expiry days from today: ");
        int expiryDays = Integer.parseInt(scanner.nextLine().trim());
        System.out.print("Warehouse ID: ");
        int warehouseId = Integer.parseInt(scanner.nextLine().trim());
        System.out.print("Type [otc/prescription]: ");
        String type = scanner.nextLine().trim().toLowerCase();

        Medicine medicine = "prescription".equals(type)
                ? new PrescriptionMedicine(0, name, price, LocalDate.now().plusDays(expiryDays),
                        warehouseId, null)
                : new OverTheCounterMedicine(0, name, price, LocalDate.now().plusDays(expiryDays),
                        warehouseId);

        try {
            medicineService.addMedicine(medicine);
            System.out.println("Medicine added successfully with ID " + medicine.getMedicineId());
        } catch (Exception exception) {
            throw new IllegalArgumentException("Unable to add medicine: " + exception.getMessage(), exception);
        }
    }

    private void listMedicines() {
        List<Medicine> medicines = medicineService.getAllMedicines();
        if (medicines.isEmpty()) {
            System.out.println("No medicines available.");
            return;
        }

        System.out.println("Medicines:");
        for (Medicine medicine : medicines) {
            System.out.println("- " + medicine.getMedicineId() + ": " + medicine.getName()
                    + " | Price: " + medicine.getPrice() + " | Expires: "
                    + medicine.getExpiryDate());
        }
    }

    private void addWarehouseAndStock() {
        System.out.print("Warehouse name: ");
        String name = scanner.nextLine().trim();
        System.out.print("Warehouse city: ");
        String city = scanner.nextLine().trim();
        Warehouse warehouse = new Warehouse(0, name, city, new ArrayList<>());
        warehouseService.addWarehouse(warehouse);

        System.out.print("Medicine ID to stock: ");
        int medicineId = Integer.parseInt(scanner.nextLine().trim());
        Medicine medicine = medicineService.getMedicineById(medicineId);
        if (medicine == null) {
            throw new IllegalArgumentException("Medicine does not exist: " + medicineId);
        }

        System.out.print("Quantity available: ");
        int quantity = Integer.parseInt(scanner.nextLine().trim());
        Stock stock = new Stock(0, medicine, warehouse, quantity);
        stockService.addStock(stock);
        System.out.println("Stock created successfully for warehouse " + warehouse.getWarehouseId());
    }

    private void uploadPrescription() {
        System.out.print("Customer ID: ");
        int customerId = Integer.parseInt(scanner.nextLine().trim());
        Customer customer = customerService.getCustomerById(customerId);
        if (customer == null) {
            throw new IllegalArgumentException("Customer does not exist: " + customerId);
        }

        System.out.print("Document path: ");
        String documentPath = scanner.nextLine().trim();
        Prescription prescription = new Prescription(0, customer, documentPath, null, null, "Pending");
        customerService.uploadPrescription(customer, prescription);
        prescriptionService.addPrescription(prescription);
        System.out.println("Prescription uploaded with ID " + prescription.getPrescriptionId());
    }

    private void placeOrder() {
        System.out.print("Customer ID: ");
        int customerId = Integer.parseInt(scanner.nextLine().trim());
        Customer customer = customerService.getCustomerById(customerId);
        if (customer == null) {
            throw new IllegalArgumentException("Customer does not exist: " + customerId);
        }

        Order order = new Order(0, customer, new ArrayList<>(), LocalDateTime.now(), null,
                "Placed", BigDecimal.ZERO);
        System.out.println("Enter medicine IDs to add to the order. Type 0 when done.");
        while (true) {
            System.out.print("Medicine ID: ");
            int medicineId = Integer.parseInt(scanner.nextLine().trim());
            if (medicineId == 0) {
                break;
            }

            Medicine medicine = medicineService.getMedicineById(medicineId);
            if (medicine == null) {
                System.out.println("Medicine not found: " + medicineId);
                continue;
            }

            if (medicine.isPrescriptionRequired()) {
                System.out.print("Approved prescription ID for this medicine: ");
                int prescriptionId = Integer.parseInt(scanner.nextLine().trim());
                Prescription prescription = prescriptionService.getPrescriptionById(prescriptionId);
                if (prescription == null || !prescription.isApproved()) {
                    throw new IllegalArgumentException(
                            "An approved prescription is required for this medicine");
                }
                if (medicine instanceof PrescriptionMedicine) {
                    ((PrescriptionMedicine) medicine).setPrescription(prescription);
                }
                order.setPrescription(prescription);
            }

            try {
                medicineService.addToCart(medicine, order);
                System.out.println("Added " + medicine.getName() + " to the order.");
            } catch (Exception exception) {
                throw new IllegalArgumentException("Unable to add medicine to order: "
                        + exception.getMessage(), exception);
            }
        }

        if (order.getItems().isEmpty()) {
            throw new IllegalArgumentException("An order must contain at least one medicine.");
        }

        try {
            orderService.checkout(order);
            System.out.println("Order placed successfully with ID " + order.getOrderId()
                    + " and total " + order.getTotalAmount());
        } catch (Exception exception) {
            throw new IllegalArgumentException("Order could not be checked out: "
                    + exception.getMessage(), exception);
        }
    }

    private void listOrders() {
        List<Order> orders = orderService.getAllOrders();
        if (orders.isEmpty()) {
            System.out.println("No orders found.");
            return;
        }

        System.out.println("Orders:");
        for (Order order : orders) {
            System.out.println("- Order " + order.getOrderId() + " | Customer: "
                    + order.getCustomer().getCustomerId() + " | Status: " + order.getStatus()
                    + " | Total: " + order.getTotalAmount());
        }
    }

    private void seedStarterData() {
        if (!customerService.getAllCustomers().isEmpty() || !medicineService.getAllMedicines().isEmpty()) {
            return;
        }

        Customer customer = new Customer(0, "Demo", "Customer", "demo@medicart.com",
                "Bengaluru", new ArrayList<>());
        customerService.addCustomer(customer);

        Warehouse warehouse = new Warehouse(0, "Main Warehouse", "Bengaluru", new ArrayList<>());
        warehouseService.addWarehouse(warehouse);

        Medicine otc = new OverTheCounterMedicine(0, "Paracetamol", new BigDecimal("25.00"),
                LocalDate.now().plusDays(120), warehouse.getWarehouseId());
        try {
            medicineService.addMedicine(otc);
            stockService.addStock(new Stock(0, otc, warehouse, 50));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to seed OTC medicine", exception);
        }

        Medicine prescriptionMedicine = new PrescriptionMedicine(0, "Amoxicillin",
                new BigDecimal("120.00"), LocalDate.now().plusDays(90), warehouse.getWarehouseId(), null);
        try {
            medicineService.addMedicine(prescriptionMedicine);
            stockService.addStock(new Stock(0, prescriptionMedicine, warehouse, 20));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to seed prescription medicine", exception);
        }

        Prescription prescription = new Prescription(0, customer, "/prescriptions/demo.pdf",
                null, null, "Pending");
        customerService.uploadPrescription(customer, prescription);
        prescriptionService.addPrescription(prescription);
    }

    private static boolean jdbcAvailable() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return true;
        } catch (ClassNotFoundException exception) {
            return false;
        }
    }

    private static boolean databasePropertiesPresent() {
        String configuredPath = System.getenv("MEDICART_DB_PROPERTIES");
        if (configuredPath != null && !configuredPath.isBlank()) {
            return Files.isRegularFile(Path.of(configuredPath));
        }
        return Files.isRegularFile(Path.of("resources", "db.properties"));
    }
}
