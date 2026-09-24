package by.nhorushko.crudgeneric.flex.mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * Collects the {@link Updater}s for the PATCH bodies of one entity: one
 * {@code add(BodyClass.class, (body, entity) -> ...)} per body class. A body is a partial class
 * without id — the id comes from the path. Filled by the entity's mapping config.
 *
 * @param <ENTITY> the entity every body is written onto
 */
public final class Patches<ENTITY> {

    private final Class<ENTITY> entityClass;
    private final List<Updater<?, ENTITY>> updaters = new ArrayList<>();

    Patches(Class<ENTITY> entityClass) {
        this.entityClass = Objects.requireNonNull(entityClass, "entityClass");
    }

    public <P> Patches<ENTITY> add(Class<P> patchClass, BiConsumer<P, ENTITY> apply) {
        updaters.add(Updater.of(patchClass, entityClass, apply));
        return this;
    }

    List<Updater<?, ENTITY>> updaters() {
        return List.copyOf(updaters);
    }
}
