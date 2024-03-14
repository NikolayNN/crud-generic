package by.nhorushko.crudgeneric.flex.mapper.core;

import by.nhorushko.crudgeneric.flex.AbsDtoModelMapper;
import by.nhorushko.crudgeneric.flex.model.AbsBaseDto;
import by.nhorushko.crudgeneric.v2.domain.AbstractEntity;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;

/**
 * Abstract mapper for DTO objects that do not contain an ID and therefore do not exist in the database.
 * <p>
 * This class is intended as a superclass for DTOs designed for the creation of new entities. It provides
 * the infrastructure for mapping from a DTO to an entity class, including handling of specific field mapping
 * and additional customizations. The class leverages {@link AbsDtoModelMapper} for the mapping process and
 * allows for detailed configuration of the mapping via subclassing.
 * </p>
 *
 * @param <DTO>    the type of the Data Transfer Object extending {@link AbsBaseDto}
 * @param <ENTITY> the type of the entity extending {@link AbstractEntity}
 */
public abstract class AbsMapBaseDtoToEntity<DTO extends AbsBaseDto, ENTITY extends AbstractEntity<?>> extends AbsMapSimple<DTO, ENTITY> {

    protected final Class<DTO> dtoClass;
    protected final Class<ENTITY> entityClass;
    protected final AbsDtoModelMapper mapper;

    public AbsMapBaseDtoToEntity(AbsDtoModelMapper mapper, Class<DTO> dtoClass, Class<ENTITY> entityClass) {
        super(mapper, dtoClass, entityClass);
        this.mapper = mapper;
        this.dtoClass = dtoClass;
        this.entityClass = entityClass;
        this.configureMapper();
    }

    /**
     * Template method for mapping specific fields that cannot be automatically mapped.
     * <p>
     * Implement this method in subclasses to handle specific field mappings between the DTO and
     * the entity that are not covered by the default mapping configuration.
     * </p>
     *
     * @param source      the source DTO
     * @param destination the destination entity
     */
    protected void mapSpecificFields(DTO source, ENTITY destination) {
    }

    /**
     * Configures the {@link ModelMapper} for DTO to entity mapping.
     * <p>
     * This method sets up the type mapping between DTO and entity classes and applies a post-converter
     * for handling specific field mappings. It also calls {@link #configureAdditionalMappings(ModelMapper)}
     * for further customization.
     * </p>
     */
    @Override
    protected void configureMapper() {
        mapper.getModelMapper().createTypeMap(dtoClass, entityClass)
                .setPostConverter(createConverterDtoToEntity());
        this.configureAdditionalMappings(mapper.getModelMapper());
    }

    /**
     * Creates a converter for mapping from DTO to entity.
     * <p>
     * The converter applies specific field mappings via {@link #mapSpecificFields(Object, Object)} and
     * handles additional custom processing after specific field mappings have been applied.
     * </p>
     *
     * @return a {@link Converter} that maps from DTO to entity
     */
    protected Converter<DTO, ENTITY> createConverterDtoToEntity() {
        return context -> {
            DTO source = context.getSource();
            ENTITY destination = context.getDestination();
            mapSpecificFields(source, destination);
            destination = handleAfterMapSpecificFields(source, destination);
            return destination;
        };
    }

    /**
     * Hook method for additional processing after specific fields have been mapped.
     * <p>
     * Override this method in subclasses for any post-mapping customizations or adjustments to the
     * mapped entity.
     * </p>
     *
     * @param source      the source DTO
     * @param destination the mapped entity, after specific field mappings
     * @return the entity with any additional processing applied
     */
    protected ENTITY handleAfterMapSpecificFields(DTO source, ENTITY destination) {
        return destination;
    }

    /**
     * Allows for additional custom mapping configurations.
     * <p>
     * Override this method in subclasses to add custom mappings or configuration to the {@link ModelMapper}
     * beyond the default mappings and specific field handling.
     * </p>
     *
     * @param modelMapper the {@link ModelMapper} instance to configure
     */
    protected void configureAdditionalMappings(ModelMapper modelMapper) {
        // Default implementation does not add additional mappings.
        // Override in subclasses for further customization.
    }
}
