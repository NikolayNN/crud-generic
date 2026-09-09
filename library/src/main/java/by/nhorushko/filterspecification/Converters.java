package by.nhorushko.filterspecification;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.chrono.ChronoLocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public abstract class Converters {

    protected Map<Class<?>, Function<String, ? extends Comparable<?>>> map = new HashMap<>();

    public Converters() {
        init();
    }

    public void init() {
        map.put(String.class, s -> s);
        map.put(Long.class, Long::valueOf);
        map.put(Integer.class, Integer::valueOf);
        map.put(Float.class, Float::valueOf);
        map.put(Double.class, Double::valueOf);
        map.put(ChronoLocalDate.class, LocalDate::parse);
        map.put(LocalTime.class, LocalTime::parse);
        map.put(Boolean.class, Boolean::valueOf);
        map.put(Instant.class, Instant::parse);
        addConverters();
    }

    // Add custom converters
    public abstract void addConverters();

    @SuppressWarnings("unchecked")
    public <T extends Comparable<T>> Function<String, T> getFunction(Class<?> classObj) {
        return (Function<String, T>) map.get(classObj);
    }
}
