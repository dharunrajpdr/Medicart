package dao;

import entity.model.Stock;
import java.util.List;
import java.util.Objects;

public class StockService implements IStockService {
    private final InMemoryRepository<Stock> stocks = new InMemoryRepository<>();

    @Override
    public Stock getStockById(int stockId) {
        return stocks.get(stockId);
    }

    @Override
    public List<Stock> getAllStocks() {
        return stocks.getAll();
    }

    @Override
    public synchronized void addStock(Stock stock) {
        validateStock(stock);
        ensureMedicineWarehouseUnique(stock, 0);
        stocks.add(stock, Stock::getStockId, Stock::setStockId);
    }

    @Override
    public synchronized void updateStock(Stock stock) {
        validateStock(stock);
        if (!stocks.contains(stock.getStockId())) {
            throw new IllegalArgumentException("Stock does not exist: " + stock.getStockId());
        }
        ensureMedicineWarehouseUnique(stock, stock.getStockId());
        stocks.update(stock, Stock::getStockId);
    }

    @Override
    public synchronized void deleteStock(int stockId) {
        if (!stocks.delete(stockId)) {
            throw new IllegalArgumentException("Stock does not exist: " + stockId);
        }
    }

    private void ensureMedicineWarehouseUnique(Stock candidate, int excludedStockId) {
        for (Stock existing : stocks.getAll()) {
            boolean sameMedicine = existing.getMedicine().getMedicineId()
                    == candidate.getMedicine().getMedicineId();
            boolean sameWarehouse = existing.getWarehouse().getWarehouseId()
                    == candidate.getWarehouse().getWarehouseId();
            if (existing.getStockId() != excludedStockId && sameMedicine && sameWarehouse) {
                throw new IllegalArgumentException(
                        "A stock record already exists for this medicine and warehouse");
            }
        }
    }

    private static void validateStock(Stock stock) {
        Objects.requireNonNull(stock, "stock cannot be null");
        Objects.requireNonNull(stock.getMedicine(), "medicine cannot be null");
        Objects.requireNonNull(stock.getWarehouse(), "warehouse cannot be null");
        if (stock.getMedicine().getMedicineId() <= 0
                || stock.getWarehouse().getWarehouseId() <= 0) {
            throw new IllegalArgumentException("Medicine and warehouse must have persisted IDs");
        }
        if (stock.getQuantityAvailable() < 0) {
            throw new IllegalArgumentException("quantityAvailable cannot be negative");
        }
    }
}
