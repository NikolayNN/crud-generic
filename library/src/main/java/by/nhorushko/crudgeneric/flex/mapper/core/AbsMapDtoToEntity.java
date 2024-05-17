package by.nhorushko.crudgeneric.flex.mapper.core;

import by.nhorushko.crudgeneric.flex.AbsModelMapper;
import by.nhorushko.crudgeneric.v2.domain.AbstractDto;
import by.nhorushko.crudgeneric.v2.domain.AbstractEntity;

/**
 * Abstract mapper for converting DTOs extending {@link AbstractDto} into entities extending {@link AbstractEntity}.
 * <p>
 * This class specializes the {@link AbsMapBaseDtoToEntity} to cater specifically to DTOs that include an ID,
 * possibly representing existing entities in the database. It inherits the generic mapping capabilities and
 * provides a foundation for implementing entity-specific mappings that might involve handling of entity IDs
 * or other properties specific to DTOs used for update or complex creation scenarios.
 * </p>
 *
 * @param <DTO>    the type of the Data Transfer Object, extending {@link AbstractDto}
 * @param <ENTITY> the type of the entity, extending {@link AbstractEntity}
 */
public abstract class AbsMapDtoToEntity<DTO extends AbstractDto<?>, ENTITY extends AbstractEntity<?>> extends AbsMapBaseDtoToEntity<DTO, ENTITY> {

    public AbsMapDtoToEntity(AbsModelMapper mapper, Class<DTO> dtoClass, Class<ENTITY> entityClass) {
        super(mapper, dtoClass, entityClass);
    }
}
