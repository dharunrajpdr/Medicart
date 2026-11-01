package entity.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public abstract class Medicine {
    private int medicineId;
    private String name;
    private BigDecimal price;
    private LocalDate expiryDate;
    private int warehouseId;

    public Medicine() {
    }

    protected Medicine(int medicineId, String name, BigDecimal price,
            LocalDate expiryDate, int warehouseId) {
        setMedicineId(medicineId);
        setName(name);
        setPrice(price);
        setExpiryDate(expiryDate);
        setWarehouseId(warehouseId);
    }

    public abstract boolean isPrescriptionRequired();

    public int getMedicineId() {
        return medicineId;
    }

    public void setMedicineId(int medicineId) {
        this.medicineId = ModelValidation.nonNegativeId(medicineId, "medicineId");
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = ModelValidation.requiredText(name, "name", 100);
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = ModelValidation.nonNegativeAmount(price, "price");
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = Objects.requireNonNull(expiryDate, "expiryDate cannot be null");
    }

    public int getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(int warehouseId) {
        this.warehouseId = ModelValidation.positiveId(warehouseId, "warehouseId");
    }
}
