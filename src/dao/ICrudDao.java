package dao;

import java.util.List;

public interface ICrudDao<T> {
    T findById(int id);

    List<T> findAll();

    void insert(T entity);

    void update(T entity);

    void delete(int id);
}
