package dao;

import entity.model.Stock;
import java.util.List;

public interface IStockService {
    Stock getStockById(int stockId);

    List<Stock> getAllStocks();

    void addStock(Stock stock);

    void updateStock(Stock stock);

    void deleteStock(int stockId);
}
