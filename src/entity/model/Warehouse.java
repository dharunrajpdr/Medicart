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
}
