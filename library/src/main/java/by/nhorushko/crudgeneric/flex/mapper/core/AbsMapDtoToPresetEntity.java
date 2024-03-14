package by.nhorushko.crudgeneric.flex.mapper.core;

import by.nhorushko.crudgeneric.flex.AbsDtoModelMapper;
import by.nhorushko.crudgeneric.v2.domain.AbstractDto;
import by.nhorushko.crudgeneric.v2.domain.AbstractEntity;

import javax.persistence.EntityManager;

/**
 * Abstract mapper for converting DTOs into already persisted entities, specifically designed for
 * scenarios where the DTO represents an update to an existing entity.
 * <p>
 * This class extends {@link AbsMapBaseDtoToEntity} and utilizes {@link EntityManager} to fetch the
 * actual entity from the database based on the ID provided in the DTO. It is particularly useful
 * for update operations where the DTO contains modifications to be applied to an existing entity.
 * After fetching the entity, it applies the changes from the DTO to this entity.
 * </p>
 *
 * @param <DTO>    the type of the Data Transfer Object, extending {@link AbstractDto}, typically containing
 *                 an ID corresponding to the existing entity
 * @param <ENTITY> the type of the entity, extending {@link AbstractEntity}, that is presumed to exist in the
 *                 database and to which the DTO's changes will be applied
 */
public abstract class AbsMapDtoToPresetEntity<DTO extends AbstractDto<?>, ENTITY extends AbstractEntity<?>> extends AbsMapBaseDtoToEntity<DTO, ENTITY> {

    private final EntityManager entityManager;

    public AbsMapDtoToPresetEntity(AbsDtoModelMapper mapper, Class<DTO> dtoClass, Class<ENTITY> entityClass, EntityManager entityManager) {
        super(mapper, dtoClass, entityClass);
        this.entityManager = entityManager;
    }

    /**
     * Retrieves and updates the actual entity instance based on the ID provided in the source DTO.
     * <p>
     * This overridden method fetches the existing entity from the database using the ID from the DTO,
     * then applies the DTO's modifications to this entity. It ensures that the update operation
     * works on the actual database entity, reflecting changes accurately.
     * </p>
     *
     * @param source      the source DTO containing updates and the ID of the entity to update
     * @param destination a preliminary mapped entity (not fetched from the database)
     * @return the actual entity from the database with the DTO's changes applied
     */
    @Override
    protected ENTITY handleAfterMapSpecificFields(DTO source, ENTITY destination) {
        ENTITY actualDestination = entityManager.find(entityClass, ((AbstractDto<?>)source).getId());
        mapper.getModelMapper().map(destination, actualDestination);
        return actualDestination;
    }
}
