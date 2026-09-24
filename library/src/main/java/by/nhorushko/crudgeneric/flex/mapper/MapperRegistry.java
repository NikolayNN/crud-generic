package by.nhorushko.crudgeneric.flex.mapper;

import by.nhorushko.crudgeneric.flex.exception.MappingNotFoundException;
import jakarta.persistence.Entity;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Every {@link Mapper} and {@link Updater} of the application, keyed by the exact pair
 * {@code (fromClass, toClass)}.
 * <p>
 * Built once from the mapper beans, the updater beans and the {@link MapperSource} beans, whose
 * contents are unwrapped. A pair registered twice, or a mapper or updater that returns {@code null}
 * from {@code fromClass()} or {@code toClass()}, fails the build with {@link IllegalStateException}
 * naming the culprit. After the build the pairs never change.
 * </p>
 * <p>
 * Lookup uses the exact key: no walking up superclasses, no interfaces. For a DTO hierarchy register
 * one pair per concrete class. The only normalisation is for JPA proxies: a class that is not
 * annotated {@link Entity @Entity} but has an {@code @Entity} ancestor is replaced by the nearest such
 * ancestor, so Hibernate proxies resolve to their entity. An {@code @Entity} subclass of an
 * {@code @Entity} class keeps its own key. The rule applies to both classes of every lookup, so a
 * pair declared for a non-{@code @Entity} subclass of an entity could never be found: it fails the
 * build with {@link IllegalStateException} instead.
 * </p>
 */
public class MapperRegistry {

    private static final String MAPPER = "Mapper";
    private static final String UPDATER = "Updater";

    private final Map<Pair, Mapper<?, ?>> mappers = new HashMap<>();
    private final Map<Pair, Updater<?, ?>> updaters = new HashMap<>();
    private final ConcurrentMap<Class<?>, Class<?>> normalised = new ConcurrentHashMap<>();

    /**
     * For tests and manual wiring: a failure names the culprit by its class.
     */
    public MapperRegistry(Collection<? extends Mapper<?, ?>> mapperBeans,
                          Collection<? extends Updater<?, ?>> updaterBeans,
                          Collection<? extends MapperSource> sources) {
        build(byClass(mapperBeans), byClass(updaterBeans), byClass(sources));
    }

    /**
     * For Spring: each key is the bean name, so a failure names the bean as well as its class —
     * {@code Mapper.of} beans are all anonymous {@code Mapper$1}, and two beans may share a class.
     */
    public MapperRegistry(Map<String, ? extends Mapper<?, ?>> mapperBeans,
                          Map<String, ? extends Updater<?, ?>> updaterBeans,
                          Map<String, ? extends MapperSource> sources) {
        build(byBeanName(mapperBeans), byBeanName(updaterBeans), byBeanName(sources));
    }

    private void build(List<Origin<Mapper<?, ?>>> mapperBeans,
                       List<Origin<Updater<?, ?>>> updaterBeans,
                       List<Origin<MapperSource>> sources) {
        Map<Pair, String> mapperOrigins = new HashMap<>();
        Map<Pair, String> updaterOrigins = new HashMap<>();
        for (Origin<Mapper<?, ?>> bean : mapperBeans) {
            Mapper<?, ?> mapper = bean.value();
            register(mappers, mapperOrigins, MAPPER, mapper.fromClass(), mapper.toClass(), mapper, bean.description());
        }
        for (Origin<Updater<?, ?>> bean : updaterBeans) {
            Updater<?, ?> updater = bean.value();
            register(updaters, updaterOrigins, UPDATER, updater.fromClass(), updater.toClass(), updater, bean.description());
        }
        for (Origin<MapperSource> source : sources) {
            for (Mapper<?, ?> mapper : source.value().mappers()) {
                register(mappers, mapperOrigins, MAPPER, mapper.fromClass(), mapper.toClass(), mapper, source.description());
            }
            for (Updater<?, ?> updater : source.value().updaters()) {
                register(updaters, updaterOrigins, UPDATER, updater.fromClass(), updater.toClass(), updater, source.description());
            }
        }
    }

    @SuppressWarnings("unchecked")
    public <F, T> Optional<Mapper<F, T>> findMapper(Class<F> from, Class<T> to) {
        return Optional.ofNullable((Mapper<F, T>) mappers.get(key(from, to)));
    }

    @SuppressWarnings("unchecked")
    public <F, T> Optional<Updater<F, T>> findUpdater(Class<F> from, Class<T> to) {
        return Optional.ofNullable((Updater<F, T>) updaters.get(key(from, to)));
    }

    /**
     * @throws MappingNotFoundException if no mapper is registered for the pair
     */
    public <F, T> Mapper<F, T> getMapper(Class<F> from, Class<T> to) {
        return findMapper(from, to).orElseThrow(() -> notFound(MAPPER, from, to));
    }

    /**
     * @throws MappingNotFoundException if no updater is registered for the pair
     */
    public <F, T> Updater<F, T> getUpdater(Class<F> from, Class<T> to) {
        return findUpdater(from, to).orElseThrow(() -> notFound(UPDATER, from, to));
    }

    private <V> void register(Map<Pair, V> target, Map<Pair, String> origins, String kind,
                              Class<?> from, Class<?> to, V implementation, String origin) {
        if (from == null || to == null) {
            throw new IllegalStateException(String.format(
                    "%s from %s declares fromClass()=%s and toClass()=%s; both must be non-null",
                    kind, origin, from, to));
        }
        requireOwnKey(kind, from, origin);
        requireOwnKey(kind, to, origin);
        Pair pair = new Pair(from, to);
        String first = origins.putIfAbsent(pair, origin);
        if (first != null) {
            throw new IllegalStateException(String.format("Duplicate %s for %s -> %s: %s and %s",
                    kind, from.getName(), to.getName(), first, origin));
        }
        target.put(pair, implementation);
    }

    /**
     * Lookups normalise both classes, so a pair is only findable if its classes are their own
     * normalised form. A non-@Entity subclass of an entity is treated as a proxy of that entity.
     */
    private void requireOwnKey(String kind, Class<?> type, String origin) {
        Class<?> entity = normalise(type);
        if (entity != type) {
            throw new IllegalStateException(String.format(
                    "%s from %s declares %s, which is not an @Entity but extends @Entity %s: lookups treat it"
                            + " as a proxy of that entity, so this pair could never be found."
                            + " Annotate it with @Entity or declare the pair for the entity",
                    kind, origin, type.getName(), entity.getName()));
        }
    }

    private static <T> List<Origin<T>> byClass(Collection<? extends T> beans) {
        return beans.stream()
                .map(bean -> new Origin<T>(bean.getClass().getName(), bean))
                .toList();
    }

    private static <T> List<Origin<T>> byBeanName(Map<String, ? extends T> beans) {
        return beans.entrySet().stream()
                .map(bean -> new Origin<T>(
                        "bean '" + bean.getKey() + "' (" + bean.getValue().getClass().getName() + ")",
                        bean.getValue()))
                .toList();
    }

    private MappingNotFoundException notFound(String kind, Class<?> from, Class<?> to) {
        return new MappingNotFoundException(kind, normalise(from), normalise(to));
    }

    private Pair key(Class<?> from, Class<?> to) {
        return new Pair(normalise(from), normalise(to));
    }

    private Class<?> normalise(Class<?> type) {
        return normalised.computeIfAbsent(type, MapperRegistry::nearestEntity);
    }

    private static Class<?> nearestEntity(Class<?> type) {
        if (type.isAnnotationPresent(Entity.class)) {
            return type;
        }
        for (Class<?> ancestor = type.getSuperclass(); ancestor != null; ancestor = ancestor.getSuperclass()) {
            if (ancestor.isAnnotationPresent(Entity.class)) {
                return ancestor;
            }
        }
        return type;
    }

    private record Pair(Class<?> from, Class<?> to) {
    }

    /** A bean the registry is built from, with how a build failure names it. */
    private record Origin<T>(String description, T value) {
    }
}
