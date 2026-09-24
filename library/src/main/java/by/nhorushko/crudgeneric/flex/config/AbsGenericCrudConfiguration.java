package by.nhorushko.crudgeneric.flex.config;

import by.nhorushko.crudgeneric.flex.AbsMapper;
import by.nhorushko.crudgeneric.flex.mapper.Mapper;
import by.nhorushko.crudgeneric.flex.mapper.MapperRegistry;
import by.nhorushko.crudgeneric.flex.mapper.MapperSource;
import by.nhorushko.crudgeneric.flex.mapper.Updater;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.util.Map;

/**
 * Spring wiring of the explicit mapping stack: a {@link MapperRegistry} built from every
 * {@link Mapper}, {@link Updater} and {@link MapperSource} bean, and the {@link AbsMapper} facade
 * over it.
 * <p>
 * {@code AbsMapper} gets the registry lazily through an {@link ObjectProvider}, so mapper beans can
 * inject {@code AbsMapper} without a cycle with the registry, which injects them.
 * </p>
 */
@Configuration
public class AbsGenericCrudConfiguration {

    /**
     * Collects every mapper bean by bean name, so a registry build failure names the beans. Spring
     * creates them all to satisfy the injection and passes an empty map when the application has none
     * of a kind.
     * <p>
     * Never lazy, even under {@code spring.main.lazy-initialization=true}: a duplicate pair or a
     * broken mapper must fail the start, not the first request that maps something.
     * </p>
     */
    @Bean
    @Lazy(false)
    public MapperRegistry mapperRegistry(Map<String, Mapper<?, ?>> mappers,
                                         Map<String, Updater<?, ?>> updaters,
                                         Map<String, MapperSource> sources) {
        return new MapperRegistry(mappers, updaters, sources);
    }

    @Bean
    public AbsMapper absMapper(ObjectProvider<MapperRegistry> registry, EntityManager entityManager) {
        return new AbsMapper(registry::getObject, entityManager);
    }
}
