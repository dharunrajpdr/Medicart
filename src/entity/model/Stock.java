package entity.model;

import java.util.Objects;

public class Stock {
    private int stockId;
    private Medicine medicine;
    private Warehouse warehouse;
    private int quantityAvailable;

    public Stock() {
    }

    public Stock(int stockId, Medicine medicine, Warehouse warehouse, int quantityAvailable) {
        setStockId(stockId);
        setMedicine(medicine);
        setWarehouse(warehouse);
        setQuantityAvailable(quantityAvailable);
    }

    public int getStockId() {
        return stockId;
    }

    public void setStockId(int stockId) {
        this.stockId = ModelValidation.nonNegativeId(stockId, "stockId");
    }

    public Medicine getMedicine() {
        return medicine;
    }

    public void setMedicine(Medicine medicine) {
        this.medicine = Objects.requireNonNull(medicine, "medicine cannot be null");
    }

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = Objects.requireNonNull(warehouse, "warehouse cannot be null");
    }

    public synchronized int getQuantityAvailable() {
        return quantityAvailable;
    }

    public synchronized void setQuantityAvailable(int quantityAvailable) {
        if (quantityAvailable < 0) {
            throw new IllegalArgumentException("quantityAvailable cannot be negative");
        }
        this.quantityAvailable = quantityAvailable;
    }
}
