package entity.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PrescriptionMedicine extends Medicine {
    private Prescription prescription;

    public PrescriptionMedicine() {
    }

    public PrescriptionMedicine(int medicineId, String name, BigDecimal price,
            LocalDate expiryDate, int warehouseId, Prescription prescription) {
        super(medicineId, name, price, expiryDate, warehouseId);
        this.prescription = prescription;
    }

    @Override
    public boolean isPrescriptionRequired() {
        return true;
    }

    public Prescription getPrescription() {
        return prescription;
    }

    public void setPrescription(Prescription prescription) {
        this.prescription = prescription;
    }
}
