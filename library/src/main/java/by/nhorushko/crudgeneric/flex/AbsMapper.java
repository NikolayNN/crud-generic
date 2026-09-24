package by.nhorushko.crudgeneric.flex;

import by.nhorushko.crudgeneric.flex.exception.MappingNotFoundException;
import by.nhorushko.crudgeneric.flex.mapper.Mapper;
import by.nhorushko.crudgeneric.flex.mapper.MapperRegistry;
import by.nhorushko.crudgeneric.flex.mapper.Updater;
import by.nhorushko.crudgeneric.flex.model.AbstractDto;
import by.nhorushko.crudgeneric.flex.model.AbstractEntity;
import jakarta.persistence.EntityManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Facade over {@link MapperRegistry}: every call goes to the {@link Mapper} or {@link Updater}
 * registered for the exact pair of classes, and a missing pair fails with
 * {@link MappingNotFoundException}. Nothing is copied implicitly.
 * <p>
 * The registry is taken from the supplier on the first mapping call, not at construction. That
 * breaks the bean cycle <em>mapper bean → AbsMapper → MapperRegistry → every mapper bean</em>, so
 * mapper beans can inject {@code AbsMapper} in their constructors. The flip side: do not map in a
 * constructor or {@code @PostConstruct} of a bean that any mapper depends on.
 * </p>
 */
public class AbsMapper {

    private final Supplier<MapperRegistry> registrySupplier;
    private final EntityManager entityManager;
    private volatile MapperRegistry registry;

    /**
     * For tests and manual wiring, with the registry already built.
     */
    public AbsMapper(MapperRegistry registry, EntityManager entityManager) {
        this(constant(registry), entityManager);
    }

    /**
     * For Spring. The supplier is called on the first mapping call, not here. Under a race on that
     * first call it may run more than once, so it must return the same registry every time
     * ({@code ObjectProvider::getObject} of a singleton does).
     */
    public AbsMapper(Supplier<MapperRegistry> registry, EntityManager entityManager) {
        this.registrySupplier = Objects.requireNonNull(registry, "registry");
        this.entityManager = entityManager;
    }

    /**
     * Maps {@code source} to a new {@code destinationType} instance with the {@link Mapper}
     * registered for {@code (source.getClass(), destinationType)}.
     *
     * @return the mapped object, or {@code null} if {@code source} is {@code null}
     * @throws MappingNotFoundException if no mapper is registered for the pair
     */
    public <T> T map(Object source, Class<T> destinationType) {
        if (source == null) {
            return null;
        }
        Objects.requireNonNull(destinationType, "destinationType");
        return mapWith(registry().getMapper(source.getClass(), destinationType), source);
    }

    /**
     * Writes {@code source} onto the existing {@code destination} with the {@link Updater}
     * registered for {@code (source.getClass(), destination.getClass())}. Only what the updater
     * writes changes.
     *
     * @return {@code destination}, unchanged if {@code source} is {@code null}
     * @throws MappingNotFoundException if no updater is registered for the pair
     */
    public <T> T update(Object source, T destination) {
        if (source == null) {
            return destination;
        }
        Objects.requireNonNull(destination, "destination");
        updateWith(registry().getUpdater(source.getClass(), destination.getClass()), source, destination);
        return destination;
    }

    /**
     * Maps every element with {@link #map(Object, Class)}. The result is a new mutable
     * {@link ArrayList}, so it can be set into an entity collection that Hibernate manages.
     *
     * @return the mapped list, or {@code null} if {@code source} is {@code null}
     */
    public <T> List<T> mapAll(Collection<?> source, Class<T> destinationType) {
        if (source == null) {
            return null;
        }
        List<T> result = new ArrayList<>(source.size());
        for (Object element : source) {
            result.add(map(element, destinationType));
        }
        return result;
    }

    /**
     * A reference to the entity with the DTO's id, without loading it and without copying any
     * field. This is how a relation is set from a read DTO.
     */
    public <T extends AbstractEntity<?>> T reference(AbstractDto<?> dto, Class<T> entityClass) {
        return referenceById(dto.getId(), entityClass);
    }

    /**
     * A reference to the entity with the given id, without loading it.
     */
    public <T extends AbstractEntity<?>> T referenceById(Object id, Class<T> entityClass) {
        return entityManager.getReference(entityClass, id);
    }

    public EntityManager getEntityManager() {
        return entityManager;
    }

    private MapperRegistry registry() {
        MapperRegistry result = registry;
        if (result == null) {
            result = Objects.requireNonNull(registrySupplier.get(), "MapperRegistry supplier returned null");
            registry = result;
        }
        return result;
    }

    private static Supplier<MapperRegistry> constant(MapperRegistry registry) {
        Objects.requireNonNull(registry, "registry");
        return () -> registry;
    }

    @SuppressWarnings("unchecked")
    private static <F, T> T mapWith(Mapper<F, T> mapper, Object source) {
        return mapper.map((F) source);
    }

    @SuppressWarnings("unchecked")
    private static <F, T> void updateWith(Updater<F, T> updater, Object source, Object destination) {
        updater.update((F) source, (T) destination);
    }
}
