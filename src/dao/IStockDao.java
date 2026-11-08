package dao;

import entity.model.Stock;
import java.util.List;

public interface IStockDao extends ICrudDao<Stock> {
    List<Stock> findByWarehouseId(int warehouseId);

    boolean adjustAvailability(int medicineId, int warehouseId, int quantityDelta);
}
