package entity.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Warehouse {
    private int warehouseId;
    private String warehouseName;
    private String city;
    private List<Stock> stock = new ArrayList<>();

    public Warehouse() {
    }

    public Warehouse(int warehouseId, String warehouseName, String city, List<Stock> stock) {
        setWarehouseId(warehouseId);
        setWarehouseName(warehouseName);
        setCity(city);
        setStock(stock);
    }

    public int getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(int warehouseId) {
        this.warehouseId = ModelValidation.nonNegativeId(warehouseId, "warehouseId");
    }

    public String getWarehouseName() {
        return warehouseName;
    }

    public void setWarehouseName(String warehouseName) {
        this.warehouseName = ModelValidation.requiredText(warehouseName, "warehouseName", 100);
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = ModelValidation.requiredText(city, "city", 50);
    }

    public List<Stock> getStock() {
        return new ArrayList<>(stock);
    }

    public void setStock(List<Stock> stock) {
        Objects.requireNonNull(stock, "stock cannot be null");
        for (Stock item : stock) {
            if (item == null) {
                throw new IllegalArgumentException("stock cannot contain null");
            }
        }
        this.stock = new ArrayList<>(stock);
    }

    public void reserveStock(Medicine medicine, int quantity) {
        Objects.requireNonNull(medicine, "medicine cannot be null");
        requirePositiveQuantity(quantity);
        Stock item = findStock(medicine);
        if (item.getQuantityAvailable() < quantity) {
            throw new IllegalStateException("Insufficient stock for " + medicine.getName());
        }
        item.setQuantityAvailable(item.getQuantityAvailable() - quantity);
    }

    public void releaseStock(Medicine medicine, int quantity) {
        Objects.requireNonNull(medicine, "medicine cannot be null");
        requirePositiveQuantity(quantity);
        Stock item = findStock(medicine);
        item.setQuantityAvailable(Math.addExact(item.getQuantityAvailable(), quantity));
    }

    private Stock findStock(Medicine medicine) {
        for (Stock item : stock) {
            if (item.getMedicine().getMedicineId() == medicine.getMedicineId()) {
                return item;
            }
        }
        throw new IllegalArgumentException("Medicine is not stocked at this warehouse");
    }

    private static void requirePositiveQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
    }
}
