package entity.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class OverTheCounterMedicine extends Medicine {
    public OverTheCounterMedicine() {
    }

    public OverTheCounterMedicine(int medicineId, String name, BigDecimal price,
            LocalDate expiryDate, int warehouseId) {
        super(medicineId, name, price, expiryDate, warehouseId);
    }

    @Override
    public boolean isPrescriptionRequired() {
        return false;
    }
}
