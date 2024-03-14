package by.nhorushko.crudgeneric.flex.mapper;

import by.nhorushko.crudgeneric.flex.AbsEntityModelMapper;
import by.nhorushko.crudgeneric.v2.domain.AbstractDto;
import by.nhorushko.crudgeneric.v2.domain.AbstractEntity;
import org.modelmapper.AbstractCondition;
import org.modelmapper.spi.MappingContext;

/**
 * Abstract class for mapping entities to Data Transfer Objects (DTOs) with final fields.
 * <p>
 * This class is designed for converting instances of entities, extending {@link AbstractEntity},
 * into their corresponding DTO representations, particularly those DTOs extending {@link AbstractDto}
 * with final fields. It leverages {@link AbsEntityModelMapper} for the mapping process and necessitates
 * the implementation of the {@code create} method in subclasses. This method defines the specific
 * conversion logic from an entity to its DTO, accommodating the instantiation of DTOs with final fields
 * by directly passing the necessary values to their constructors or builder methods.
 * </p>
 * <p>
 * The use of this class is ideal for DTOs that are immutable or have constraints that require them
 * to be constructed with all necessary data upfront. It provides a structured approach to ensuring
 * that entities are mapped to DTOs in a manner that respects the immutability and data integrity of the
 * resulting DTOs.
 * </p>
 *
 * @param <ENTITY> the type of the entity extending {@link AbstractEntity}
 * @param <DTO>    the type of the DTO extending {@link AbstractDto}, intended to have final fields
 */
public abstract class AbsMapEntityToDto<ENTITY extends AbstractEntity<?>, DTO extends AbstractDto<?>> {
    private final AbsEntityModelMapper mapper;
    private final Class<ENTITY> entityClass;
    private final Class<DTO> dtoClass;

    public AbsMapEntityToDto(AbsEntityModelMapper mapper, Class<ENTITY> entityClass, Class<DTO> dtoClass) {
        this.mapper = mapper;
        this.entityClass = entityClass;
        this.dtoClass = dtoClass;
        this.configureMapper();
    }

    /**
     * Abstract method to create a DTO from an entity. Implement this method in subclasses to define
     * the conversion logic from an entity instance to its corresponding DTO, especially considering
     * DTOs with final fields that require initialization through constructors or builder patterns.
     *
     * @param from the entity from which the DTO will be created
     * @return the created DTO, respecting the immutability and finality of its fields
     */
    protected abstract DTO create(ENTITY from);

    /**
     * Configures the ModelMapper with custom conditions and converters for mapping from entities to DTOs.
     * <p>
     * This method sets up the type map in the ModelMapper and specifies a converter that uses the {@code create}
     * method to perform the actual conversion. This setup is particularly important for DTOs with final fields,
     * ensuring their proper initialization during the mapping process.
     * </p>
     */
    private void configureMapper() {
        mapper.getModelMapper()
                .createTypeMap(entityClass, dtoClass)
                .setCondition(new AbstractCondition<>() {
                    @Override
                    public boolean applies(MappingContext<Object, Object> context) {
                        return true;
                    }
                })
                .setConverter(context -> create(context.getSource()));
    }
}
