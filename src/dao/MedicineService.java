package dao;

import entity.model.Customer;
import entity.model.Medicine;
import entity.model.Order;
import entity.model.Prescription;
import entity.model.PrescriptionMedicine;
import exception.ExpiredMedicineException;
import exception.InvalidMedicineDataException;
import exception.PrescriptionRequiredException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MedicineService implements IMedicineService {
    private final InMemoryRepository<Medicine> medicines = new InMemoryRepository<>();

    @Override
    public Medicine getMedicineById(int medicineId) {
        return medicines.get(medicineId);
    }

    @Override
    public List<Medicine> getAllMedicines() {
        return medicines.getAll();
    }

    @Override
    public synchronized void addMedicine(Medicine medicine) throws InvalidMedicineDataException {
        validateMedicine(medicine);
        try {
            medicines.add(medicine, Medicine::getMedicineId, Medicine::setMedicineId);
        } catch (RuntimeException exception) {
            throw new InvalidMedicineDataException("Unable to add medicine", exception);
        }
    }

    @Override
    public synchronized void updateMedicine(Medicine medicine) throws InvalidMedicineDataException {
        validateMedicine(medicine);
        try {
            medicines.update(medicine, Medicine::getMedicineId);
        } catch (RuntimeException exception) {
            throw new InvalidMedicineDataException("Unable to update medicine", exception);
        }
    }

    @Override
    public synchronized void deleteMedicine(int medicineId) {
        if (!medicines.delete(medicineId)) {
            throw new IllegalArgumentException("Medicine does not exist: " + medicineId);
        }
    }

    @Override
    public boolean isExpired(Medicine medicine) {
        return Objects.requireNonNull(medicine, "medicine cannot be null").isExpired();
    }

    @Override
    public boolean isNearExpiry(Medicine medicine, int days) {
        return Objects.requireNonNull(medicine, "medicine cannot be null").isNearExpiry(days);
    }

    @Override
    public void addToCart(Medicine medicine, Order order)
            throws ExpiredMedicineException, PrescriptionRequiredException {
        Objects.requireNonNull(medicine, "medicine cannot be null");
        Objects.requireNonNull(order, "order cannot be null");
        if (medicine.isExpired()) {
            throw new ExpiredMedicineException("Expired medicine cannot be added to an order");
        }
        if (medicine.isPrescriptionRequired()) {
            if (!(medicine instanceof PrescriptionMedicine)) {
                throw new PrescriptionRequiredException(
                        "Prescription medicines must provide a prescription");
            }
            PrescriptionMedicine prescriptionMedicine = (PrescriptionMedicine) medicine;
            Prescription prescription = prescriptionMedicine.getPrescription();
            if (prescription == null) {
                prescription = order.getPrescription();
            }
            if (prescription == null || !prescription.isApproved()
                    || !belongsToOrderCustomer(prescription, order.getCustomer())) {
                throw new PrescriptionRequiredException(
                        "An approved prescription for the order customer is required for "
                                + medicine.getName());
            }
            prescriptionMedicine.setPrescription(prescription);
        }

        medicine.addToCart();
        List<Medicine> items = new ArrayList<>(order.getItems());
        items.add(medicine);
        order.setItems(items);
    }

    private static boolean belongsToOrderCustomer(Prescription prescription, Customer customer) {
        Customer prescriptionCustomer = prescription.getCustomer();
        return prescriptionCustomer != null && customer != null
                && (prescriptionCustomer == customer
                || prescriptionCustomer.getCustomerId() > 0
                && prescriptionCustomer.getCustomerId() == customer.getCustomerId());
    }

    private static void validateMedicine(Medicine medicine)
            throws InvalidMedicineDataException {
        try {
            Objects.requireNonNull(medicine, "medicine cannot be null");
            Objects.requireNonNull(medicine.getName(), "name cannot be null");
            Objects.requireNonNull(medicine.getPrice(), "price cannot be null");
            Objects.requireNonNull(medicine.getExpiryDate(), "expiryDate cannot be null");
            if (medicine.getWarehouseId() <= 0) {
                throw new IllegalArgumentException("warehouseId must be positive");
            }
        } catch (RuntimeException exception) {
            throw new InvalidMedicineDataException("Medicine data is invalid", exception);
        }
    }
}
