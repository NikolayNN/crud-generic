package by.nhorushko.crudgeneric.flex.mapper;

import java.util.Collection;

/**
 * A bean that contributes several mappers and updaters at once. {@link MapperRegistry} unwraps it
 * when it is built; the entity mapping config implements it.
 */
public interface MapperSource {

    Collection<Mapper<?, ?>> mappers();

    Collection<Updater<?, ?>> updaters();
}
