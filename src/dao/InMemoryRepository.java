package dao;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.ObjIntConsumer;
import java.util.function.ToIntFunction;

final class InMemoryRepository<T> {
    private final Map<Integer, T> records = new LinkedHashMap<>();
    private long nextId = 1;

    synchronized T get(int id) {
        return records.get(id);
    }

    synchronized List<T> getAll() {
        return new ArrayList<>(records.values());
    }

    synchronized boolean contains(int id) {
        return records.containsKey(id);
    }

    synchronized int add(T record, ToIntFunction<T> idReader, ObjIntConsumer<T> idWriter) {
        Objects.requireNonNull(record, "record cannot be null");
        int id = idReader.applyAsInt(record);
        if (id < 0) {
            throw new IllegalArgumentException("record ID cannot be negative");
        }
        if (id == 0) {
            if (nextId > Integer.MAX_VALUE) {
                throw new IllegalStateException("No record IDs remain");
            }
            id = (int) nextId;
        }
        if (records.containsKey(id)) {
            throw new IllegalArgumentException("Record ID already exists: " + id);
        }

        idWriter.accept(record, id);
        records.put(id, record);
        nextId = Math.max(nextId, (long) id + 1);
        return id;
    }

    synchronized void update(T record, ToIntFunction<T> idReader) {
        Objects.requireNonNull(record, "record cannot be null");
        int id = idReader.applyAsInt(record);
        if (id <= 0 || !records.containsKey(id)) {
            throw new IllegalArgumentException("Record does not exist: " + id);
        }
        records.put(id, record);
    }

    synchronized boolean delete(int id) {
        return records.remove(id) != null;
    }
}
