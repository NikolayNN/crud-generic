package by.nhorushko.crudgeneric.flex.mapper;

import by.nhorushko.crudgeneric.flex.AbsModelMapper;
import by.nhorushko.crudgeneric.flex.mapper.core.AbsMapDtoToEntity;
import by.nhorushko.crudgeneric.v2.domain.AbstractDto;
import by.nhorushko.crudgeneric.v2.domain.AbstractEntity;

/**
 * Abstract class for mapping update Data Transfer Objects (DTOs) into existing entity instances.
 * <p>
 * This class extends {@link AbsMapDtoToEntity} specifically for the scenario where DTOs, extending
 * {@link AbstractDto}, contain updates for entities that already exist in the database. It inherits
 * the generic mapping capabilities from {@link AbsMapDtoToEntity} and provides a streamlined foundation
 * for implementing entity-specific update logic in subclasses.
 * </p>
 * <p>
 * Utilizing this class allows for a clean separation of the mapping logic used for creating new entities
 * from that used for updating existing ones, ensuring that update operations can be handled with
 * specificity and care to maintain data integrity and respect business logic constraints.
 * </p>
 *
 * @param <DTO>    the type of the Data Transfer Object, extending {@link AbstractDto}, used for updating entities
 * @param <ENTITY> the type of the entity, extending {@link AbstractEntity}, that is to be updated from the DTO
 */
public abstract class AbsMapUpdateDtoToEntity<DTO extends AbstractDto<?>, ENTITY extends AbstractEntity<?>> extends AbsMapDtoToEntity<DTO, ENTITY> {
    public AbsMapUpdateDtoToEntity(AbsModelMapper mapper, Class<DTO> dtoClass, Class<ENTITY> entityClass) {
        super(mapper, dtoClass, entityClass);
    }
}
