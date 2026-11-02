package dao;

import entity.model.Medicine;
import entity.model.Warehouse;
import exception.InsufficientStockException;
import java.util.List;

public interface IWarehouseService {
    Warehouse getWarehouseById(int warehouseId);

    List<Warehouse> getAllWarehouses();

    void addWarehouse(Warehouse warehouse);

    void updateWarehouse(Warehouse warehouse);

    void deleteWarehouse(int warehouseId);

    void reserveStock(Warehouse warehouse, Medicine medicine, int quantity)
            throws InsufficientStockException;

    void releaseStock(Warehouse warehouse, Medicine medicine, int quantity);
}
