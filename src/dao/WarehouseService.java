package dao;

import entity.model.Medicine;
import entity.model.Warehouse;
import exception.InsufficientStockException;
import java.util.List;
import java.util.Objects;

public class WarehouseService implements IWarehouseService {
    private final InMemoryRepository<Warehouse> warehouses = new InMemoryRepository<>();

    @Override
    public Warehouse getWarehouseById(int warehouseId) {
        return warehouses.get(warehouseId);
    }

    @Override
    public List<Warehouse> getAllWarehouses() {
        return warehouses.getAll();
    }

    @Override
    public void addWarehouse(Warehouse warehouse) {
        validateWarehouse(warehouse);
        warehouses.add(warehouse, Warehouse::getWarehouseId, Warehouse::setWarehouseId);
    }

    @Override
    public void updateWarehouse(Warehouse warehouse) {
        validateWarehouse(warehouse);
        if (!warehouses.contains(warehouse.getWarehouseId())) {
            throw new IllegalArgumentException(
                    "Warehouse does not exist: " + warehouse.getWarehouseId());
        }
        warehouses.update(warehouse, Warehouse::getWarehouseId);
    }

    @Override
    public void deleteWarehouse(int warehouseId) {
        Warehouse warehouse = warehouses.get(warehouseId);
        if (warehouse == null) {
            throw new IllegalArgumentException("Warehouse does not exist: " + warehouseId);
        }
        if (!warehouse.getStock().isEmpty()) {
            throw new IllegalStateException("A warehouse with stock cannot be deleted");
        }
        warehouses.delete(warehouseId);
    }

    @Override
    public synchronized void reserveStock(Warehouse warehouse, Medicine medicine, int quantity)
            throws InsufficientStockException {
        Warehouse storedWarehouse = requireStoredWarehouse(warehouse);
        Objects.requireNonNull(medicine, "medicine cannot be null");
        try {
            storedWarehouse.reserveStock(medicine, quantity);
        } catch (IllegalStateException exception) {
            throw new InsufficientStockException(exception.getMessage(), exception);
        }
    }

    @Override
    public synchronized void releaseStock(Warehouse warehouse, Medicine medicine, int quantity) {
        Warehouse storedWarehouse = requireStoredWarehouse(warehouse);
        storedWarehouse.releaseStock(medicine, quantity);
    }

    private Warehouse requireStoredWarehouse(Warehouse warehouse) {
        Objects.requireNonNull(warehouse, "warehouse cannot be null");
        Warehouse stored = warehouses.get(warehouse.getWarehouseId());
        if (stored == null) {
            throw new IllegalArgumentException(
                    "Warehouse does not exist: " + warehouse.getWarehouseId());
        }
        return stored;
    }

    private static void validateWarehouse(Warehouse warehouse) {
        Objects.requireNonNull(warehouse, "warehouse cannot be null");
        Objects.requireNonNull(warehouse.getWarehouseName(), "warehouseName cannot be null");
        Objects.requireNonNull(warehouse.getCity(), "city cannot be null");
    }
}
