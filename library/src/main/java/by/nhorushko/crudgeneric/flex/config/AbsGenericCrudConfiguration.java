package by.nhorushko.crudgeneric.flex.config;

import by.nhorushko.crudgeneric.flex.AbsModelMapper;
import by.nhorushko.crudgeneric.flex.service.AbsFlexServiceR;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.persistence.EntityManager;
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
     *
     * @return A configured ModelMapper instance.
     */
    @Bean
    public ModelMapper modelMapper() {
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
     * This bean initializer method constructs an instance of {@link AbsTypeMapChecker}, which is responsible for
     * checking that all necessary type mappings between DTOs and entities are correctly configured in ModelMapper.
     * This verification process is crucial for ensuring that the application's data mapping configurations are set up
     * properly before the application fully starts, preventing runtime errors related to missing or incorrect mappings.
     * </p>
     * <p>
     * The {@link AbsTypeMapChecker} operates at the end of the application's startup phase, thanks to its implementation
     * of {@link SmartLifecycle}, which allows it to have control over its startup sequence within the Spring ApplicationContext.
     * If any mappings are missing, the application will fail to start, providing an early warning to developers about
     * configuration issues.
     * </p>
     *
     * @param services    A list of services extending {@link AbsFlexServiceR}. These services are checked by the
     *                    {@link AbsTypeMapChecker} to ensure that each has the necessary type mappings configured
     *                    for their DTO and entity classes.
     * @param modelMapper The {@link ModelMapper} instance used throughout the application for DTO to entity mappings.
     *                    This is the same ModelMapper instance that will be checked by the {@link AbsTypeMapChecker}.
     * @return An initialized {@link AbsTypeMapChecker} bean ready to verify the application's ModelMapper configurations.
     */
    @Bean
    public AbsTypeMapChecker crudAbstractGenericMappingChecker(List<? extends AbsFlexServiceR<?, ?, ?, ?>> services, ModelMapper modelMapper) {
        return new AbsTypeMapChecker(services, modelMapper);
    }
}
