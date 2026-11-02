package dao;

import entity.model.Medicine;
import entity.model.Order;
import exception.ExpiredMedicineException;
import exception.InvalidMedicineDataException;
import exception.PrescriptionRequiredException;
import java.util.List;

public interface IMedicineService {
    Medicine getMedicineById(int medicineId);

    List<Medicine> getAllMedicines();

    void addMedicine(Medicine medicine) throws InvalidMedicineDataException;

    void updateMedicine(Medicine medicine) throws InvalidMedicineDataException;

    void deleteMedicine(int medicineId);

    boolean isExpired(Medicine medicine);

    boolean isNearExpiry(Medicine medicine, int days);

    void addToCart(Medicine medicine, Order order)
            throws ExpiredMedicineException, PrescriptionRequiredException;
}
