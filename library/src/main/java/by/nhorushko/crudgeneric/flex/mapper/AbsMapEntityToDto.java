package by.nhorushko.crudgeneric.flex.mapper;

import by.nhorushko.crudgeneric.flex.AbsModelMapper;
import by.nhorushko.crudgeneric.flex.mapper.core.AbsMapBasic;
import by.nhorushko.crudgeneric.flex.mapper.core.RegisterableMapper;
import by.nhorushko.crudgeneric.v2.domain.AbstractDto;
import by.nhorushko.crudgeneric.v2.domain.AbstractEntity;
import org.modelmapper.AbstractCondition;
import org.modelmapper.TypeMap;
import org.modelmapper.spi.MappingContext;

/**
 * Abstract class for mapping entities to Data Transfer Objects (DTOs) with final fields.
 * <p>
 * This class is designed for converting instances of entities, extending {@link AbstractEntity},
 * into their corresponding DTO representations, particularly those DTOs extending {@link AbstractDto}
 * with final fields. It leverages {@link AbsModelMapper} for the mapping process and necessitates
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
public abstract class AbsMapEntityToDto<ENTITY extends AbstractEntity<?>, DTO extends AbstractDto<?>> extends AbsMapBasic<ENTITY, DTO> implements RegisterableMapper {
    private final AbsModelMapper mapper;
    private final Class<ENTITY> entityClass;
    private final Class<DTO> dtoClass;

    public AbsMapEntityToDto(AbsModelMapper mapper, Class<ENTITY> entityClass, Class<DTO> dtoClass) {
        super(mapper, entityClass, dtoClass);
        this.mapper = mapper;
        this.entityClass = entityClass;
        this.dtoClass = dtoClass;
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

    @Override
    protected void customizeTypeMap(TypeMap<ENTITY, DTO> typeMap) {
        typeMap.setCondition(new AbstractCondition<>() {
                    @Override
                    public boolean applies(MappingContext<Object, Object> context) {
                        return true;
                    }
                })
                .setConverter(context -> create(context.getSource()));
    }
}
