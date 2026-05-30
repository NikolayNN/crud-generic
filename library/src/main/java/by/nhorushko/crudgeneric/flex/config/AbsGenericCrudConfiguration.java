package by.nhorushko.crudgeneric.flex.config;

import by.nhorushko.crudgeneric.flex.AbsModelMapper;
import by.nhorushko.crudgeneric.flex.service.AbsFlexServiceR;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.persistence.EntityManager;
import java.util.List;

/**
 * Central configuration class for the Generic CRUD framework.
 * <p>
 * This class defines essential beans for the framework's operation, including the configuration of
 * ModelMapper and custom mapper instances. It ensures that ModelMapper is correctly set up to handle
 * DTO to entity mappings and vice versa, with specific configurations to suit common CRUD operations
 * needs.
 * </p>
 */
@Configuration
public class AbsGenericCrudConfiguration {

    /**
     * Configures and provides a ModelMapper bean.
     * <p>
     * This method sets up a ModelMapper with configurations tailored for the framework, such as enabling
     * field matching, ignoring ambiguities, setting the matching strategy to STANDARD, allowing public
     * field access, and enabling skipping of null values during mapping. These settings are optimized for
     * CRUD operations within a Spring Boot application.
     * </p>
     * <p>
     * The bean is registered under the explicit name {@code absGenericCrudModelMapper} so applications
     * are free to declare their own {@code modelMapper} bean (e.g. with looser matching rules) without
     * triggering {@link org.springframework.beans.factory.support.BeanDefinitionOverrideException}.
     * Consumer apps that declare their own {@code ModelMapper} bean <strong>must</strong> annotate it
     * {@link org.springframework.context.annotation.Primary @Primary} so that {@link #absModelMapper}
     * (which injects {@code ModelMapper} by type) resolves unambiguously; otherwise the dual-bean
     * context will throw {@code NoUniqueBeanDefinitionException} when {@code absModelMapper} is wired.
     * </p>
     *
     * @return A configured ModelMapper instance.
     */
    @Bean("absGenericCrudModelMapper")
    public ModelMapper absGenericCrudModelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration()
                .setFieldMatchingEnabled(true)
                .setAmbiguityIgnored(true)
                .setMatchingStrategy(MatchingStrategies.STANDARD)
                .setFieldAccessLevel(org.modelmapper.config.Configuration.AccessLevel.PUBLIC)
                .setSkipNullEnabled(true);
        return modelMapper;
    }

    /**
     * Provides an AbsDtoModelMapper bean, facilitating advanced mapping functionalities.
     * <p>
     * The AbsDtoModelMapper extends ModelMapper's capabilities by integrating with the EntityManager
     * for entity reference resolution, crucial for handling relational mappings efficiently.
     * </p>
     *
     * @param modelMapper    The ModelMapper instance.
     * @param entityManager  The EntityManager for JPA entity management.
     * @return An instance of AbsDtoModelMapper.
     */
    @Bean
    public AbsModelMapper absModelMapper(ModelMapper modelMapper, EntityManager entityManager) {
        return new AbsModelMapper(modelMapper, entityManager);
    }


    /**
     * Creates and configures an {@link AbsTypeMapChecker} bean to verify the correctness of ModelMapper type mappings.
     * <p>
     * Behavior is controlled by an optional {@link AbsCrudCustomizer} bean: if the application registers one,
     * the {@code typeMapCheckerEnabled} flag from that customizer determines whether validation runs. If no
     * customizer bean is registered, the default ({@code typeMapCheckerEnabled = true}) is used.
     * </p>
     * <p>
     * The {@link AbsTypeMapChecker} bean is always registered to preserve {@link SmartLifecycle} ordering.
     * When the customizer disables validation, the bean's {@code start()} method becomes a no-op.
     * </p>
     *
     * @param services            services subject to mapping validation.
     * @param modelMapper         the application's {@link ModelMapper} instance.
     * @param customizerProvider  optional provider for {@link AbsCrudCustomizer}.
     * @return the registered {@link AbsTypeMapChecker} bean.
     */
    @Bean
    public AbsTypeMapChecker crudAbstractGenericMappingChecker(
            List<? extends AbsFlexServiceR<?, ?, ?, ?>> services,
            ModelMapper modelMapper,
            ObjectProvider<AbsCrudCustomizer> customizerProvider) {
        AbsCrudCustomizer customizer = customizerProvider.getIfAvailable(
                () -> AbsCrudCustomizer.builder().build());
        return new AbsTypeMapChecker(services, modelMapper, customizer.isTypeMapCheckerEnabled());
    }

    /**
     * Registers {@link AbsMapperEagerInitPostProcessor} so that crud-generic
     * mapper beans are eagerly initialized regardless of
     * {@code spring.main.lazy-initialization=true}.
     * <p>
     * Declared {@code static} because {@link
     * org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor}
     * beans must be created before the enclosing {@code @Configuration} class
     * is fully processed; a non-static factory method would emit a Spring
     * warning and prevent {@code @Bean} processing of this configuration.
     * </p>
     *
     * @return the registered post-processor.
     */
    @Bean
    public static AbsMapperEagerInitPostProcessor absMapperEagerInitPostProcessor() {
        return new AbsMapperEagerInitPostProcessor();
    }
}
