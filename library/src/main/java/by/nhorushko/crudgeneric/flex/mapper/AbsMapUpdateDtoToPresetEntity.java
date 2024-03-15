package by.nhorushko.crudgeneric.flex.mapper;

import by.nhorushko.crudgeneric.flex.AbsModelMapper;
import by.nhorushko.crudgeneric.flex.mapper.core.AbsMapDtoToPresetEntity;
import by.nhorushko.crudgeneric.flex.model.AbsUpdateDto;
import by.nhorushko.crudgeneric.v2.domain.AbstractEntity;

import javax.persistence.EntityManager;

/**
 * Abstract class for mapping update DTOs into existing, persisted entities.
 * <p>
 * Extending {@link AbsMapDtoToPresetEntity}, this class specifically handles the mapping of DTOs that extend
 * {@link AbsUpdateDto}, indicating they contain updates for existing entities. It leverages the underlying
 * mapping capabilities to apply these updates to entities fetched from the database, using an {@link EntityManager}
 * for entity retrieval. This class is particularly suited for update operations where the integrity and context
 * of the existing entity need to be preserved.
 * </p>
 * <p>
 * The use of this mapper ensures that update operations are performed accurately and efficiently, with proper
 * handling of entity states and relationships. It provides a structured approach to applying updates from
 * DTOs to entities, facilitating the maintenance of data consistency and adherence to business logic.
 * </p>
 *
 * @param <DTO>    the type of the Data Transfer Object, extending {@link AbsUpdateDto}, used for conveying updates
 *                 to existing entities
 * @param <ENTITY> the type of the entity, extending {@link AbstractEntity}, which is targeted for updates
 */
public abstract class AbsMapUpdateDtoToPresetEntity<DTO extends AbsUpdateDto<?>, ENTITY extends AbstractEntity<?>> extends AbsMapDtoToPresetEntity<DTO, ENTITY> {
    public AbsMapUpdateDtoToPresetEntity(AbsModelMapper mapper, Class<DTO> dtoClass, Class<ENTITY> entityClass, EntityManager entityManager) {
        super(mapper, dtoClass, entityClass, entityManager);
    }
}
