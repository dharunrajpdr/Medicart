package dao;

import entity.model.Customer;
import entity.model.Medicine;
import entity.model.Order;
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
import exception.WarehouseCapacityException;
import java.lang.reflect.Method;
import java.util.List;

public final class ServiceContractChecks {
    private ServiceContractChecks() {
    }

    public static void main(String[] args) throws Exception {
        checkCrud(ICustomerService.class, "getCustomerById", int.class,
                "getAllCustomers", "addCustomer", Customer.class,
                "updateCustomer", Customer.class, "deleteCustomer", int.class);
        checkCrud(IMedicineService.class, "getMedicineById", int.class,
                "getAllMedicines", "addMedicine", Medicine.class,
                "updateMedicine", Medicine.class, "deleteMedicine", int.class);
        checkCrud(IWarehouseService.class, "getWarehouseById", int.class,
                "getAllWarehouses", "addWarehouse", Warehouse.class,
                "updateWarehouse", Warehouse.class, "deleteWarehouse", int.class);
        checkCrud(IStockService.class, "getStockById", int.class,
                "getAllStocks", "addStock", Stock.class,
                "updateStock", Stock.class, "deleteStock", int.class);
        checkCrud(IPrescriptionService.class, "getPrescriptionById", int.class,
                "getAllPrescriptions", "addPrescription", Prescription.class,
                "updatePrescription", Prescription.class, "deletePrescription", int.class);
        checkCrud(IOrderService.class, "getOrderById", int.class,
                "getAllOrders", "addOrder", Order.class,
                "updateOrder", Order.class, "deleteOrder", int.class);

        checkMethod(ICustomerService.class, "uploadPrescription",
                Customer.class, Prescription.class);
        checkMethod(IMedicineService.class, "isExpired", Medicine.class);
        checkMethod(IMedicineService.class, "isNearExpiry", Medicine.class, int.class);
        checkMethod(IMedicineService.class, "addToCart", Medicine.class, Order.class);
        checkMethod(IWarehouseService.class, "reserveStock",
                Warehouse.class, Medicine.class, int.class);
        checkMethod(IWarehouseService.class, "releaseStock",
                Warehouse.class, Medicine.class, int.class);
        checkMethod(IPrescriptionService.class, "approve", Prescription.class);
        checkMethod(IPrescriptionService.class, "reject", Prescription.class);
        checkMethod(IPrescriptionService.class, "verifyPrescription",
                PrescriptionMedicine.class, Prescription.class);
        checkMethod(IOrderService.class, "checkout", Order.class);
        checkMethod(IOrderService.class, "holdForVerification", Order.class);

        checkCheckedException(PrescriptionRequiredException.class);
        checkCheckedException(InsufficientStockException.class);
        checkCheckedException(ExpiredMedicineException.class);
        checkCheckedException(PrescriptionAlreadyVerifiedException.class);
        checkCheckedException(InvalidMedicineDataException.class);
        checkCheckedException(OrderNotFoundException.class);
        checkCheckedException(WarehouseCapacityException.class);

        checkDeclares(IMedicineService.class.getMethod("addMedicine", Medicine.class),
                InvalidMedicineDataException.class);
        checkDeclares(IMedicineService.class.getMethod("addToCart", Medicine.class, Order.class),
                ExpiredMedicineException.class, PrescriptionRequiredException.class);
        checkDeclares(IWarehouseService.class.getMethod("reserveStock",
                Warehouse.class, Medicine.class, int.class), InsufficientStockException.class);
        checkDeclares(IPrescriptionService.class.getMethod("approve", Prescription.class),
                PrescriptionAlreadyVerifiedException.class);
        checkDeclares(IOrderService.class.getMethod("getOrderById", int.class),
                OrderNotFoundException.class);

        System.out.println("Service contract checks passed.");
    }

    private static void checkCrud(Class<?> service, String getById, Class<?> idType,
            String getAll, String add, Class<?> modelType, String update,
            Class<?> updateType, String delete, Class<?> deleteType) throws Exception {
        if (!service.isInterface()) {
            throw new AssertionError(service.getSimpleName() + " must be an interface");
        }
        checkMethod(service, getById, idType);
        checkMethod(service, getAll);
        checkMethod(service, add, modelType);
        checkMethod(service, update, updateType);
        checkMethod(service, delete, deleteType);
        if (!List.class.isAssignableFrom(service.getMethod(getAll).getReturnType())) {
            throw new AssertionError(getAll + " must return a List");
        }
    }

    private static Method checkMethod(Class<?> type, String name, Class<?>... parameters)
            throws Exception {
        return type.getMethod(name, parameters);
    }

    private static void checkCheckedException(Class<? extends Exception> exceptionType)
            throws Exception {
        if (RuntimeException.class.isAssignableFrom(exceptionType)) {
            throw new AssertionError(exceptionType.getSimpleName() + " must be checked");
        }
        exceptionType.getConstructor();
        exceptionType.getConstructor(String.class);
        exceptionType.getConstructor(String.class, Throwable.class);
    }

    private static void checkDeclares(Method method, Class<?>... expectedExceptions) {
        Class<?>[] declared = method.getExceptionTypes();
        for (Class<?> expected : expectedExceptions) {
            boolean found = false;
            for (Class<?> actual : declared) {
                if (expected.equals(actual)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new AssertionError(method.getName()
                        + " must declare " + expected.getSimpleName());
            }
        }
    }
}
