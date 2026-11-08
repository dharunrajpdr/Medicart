package dao;

import entity.model.Medicine;
import entity.model.Warehouse;
import exception.InsufficientStockException;
import java.util.List;
import java.util.Objects;

public class WarehouseService implements IWarehouseService {
    private final InMemoryRepository<Warehouse> warehouses = new InMemoryRepository<>();
    private final IWarehouseDao warehouseDao;
    private final IStockDao stockDao;

    public WarehouseService() {
        this(null, null);
    }

    public WarehouseService(IWarehouseDao warehouseDao, IStockDao stockDao) {
        if ((warehouseDao == null) != (stockDao == null)) {
            throw new IllegalArgumentException(
                    "Warehouse and stock DAOs must be supplied together");
        }
        this.warehouseDao = warehouseDao;
        this.stockDao = stockDao;
    }

    @Override
    public Warehouse getWarehouseById(int warehouseId) {
        return warehouseDao == null
                ? warehouses.get(warehouseId) : warehouseDao.findById(warehouseId);
    }

    @Override
    public List<Warehouse> getAllWarehouses() {
        return warehouseDao == null ? warehouses.getAll() : warehouseDao.findAll();
    }

    @Override
    public void addWarehouse(Warehouse warehouse) {
        validateWarehouse(warehouse);
        if (warehouseDao != null) {
            warehouseDao.insert(warehouse);
            return;
        }
        warehouses.add(warehouse, Warehouse::getWarehouseId, Warehouse::setWarehouseId);
    }

    @Override
    public void updateWarehouse(Warehouse warehouse) {
        validateWarehouse(warehouse);
        if (warehouseDao != null) {
            warehouseDao.update(warehouse);
            return;
        }
        if (!warehouses.contains(warehouse.getWarehouseId())) {
            throw new IllegalArgumentException(
                    "Warehouse does not exist: " + warehouse.getWarehouseId());
        }
        warehouses.update(warehouse, Warehouse::getWarehouseId);
    }

    @Override
    public void deleteWarehouse(int warehouseId) {
        if (warehouseDao != null) {
            Warehouse warehouse = warehouseDao.findById(warehouseId);
            if (warehouse == null) {
                throw new IllegalArgumentException("Warehouse does not exist: " + warehouseId);
            }
            if (!warehouse.getStock().isEmpty()) {
                throw new IllegalStateException("A warehouse with stock cannot be deleted");
            }
            warehouseDao.delete(warehouseId);
            return;
        }
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
        if (warehouseDao != null) {
            requirePersistedStockInputs(warehouse, medicine, quantity);
            if (!stockDao.adjustAvailability(medicine.getMedicineId(),
                    warehouse.getWarehouseId(), -quantity)) {
                throw new InsufficientStockException(
                        "Insufficient or missing stock for " + medicine.getName());
            }
            return;
        }
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
        if (warehouseDao != null) {
            requirePersistedStockInputs(warehouse, medicine, quantity);
            if (!stockDao.adjustAvailability(medicine.getMedicineId(),
                    warehouse.getWarehouseId(), quantity)) {
                throw new IllegalArgumentException(
                        "Medicine is not stocked at this warehouse");
            }
            return;
        }
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

    private static void requirePersistedStockInputs(
            Warehouse warehouse, Medicine medicine, int quantity) {
        Objects.requireNonNull(warehouse, "warehouse cannot be null");
        Objects.requireNonNull(medicine, "medicine cannot be null");
        if (warehouse.getWarehouseId() <= 0 || medicine.getMedicineId() <= 0) {
            throw new IllegalArgumentException(
                    "Warehouse and medicine must have persisted IDs");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
    }
}
