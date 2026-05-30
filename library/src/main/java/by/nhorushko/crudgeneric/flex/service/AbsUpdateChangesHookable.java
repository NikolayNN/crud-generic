package by.nhorushko.crudgeneric.flex.service;

import by.nhorushko.crudgeneric.v2.domain.AbstractDto;

/**
 * Interface that defines hooks for capturing state changes before and after updating an entity.
 * Implementing this interface allows services to perform custom logic right before and after
 * an entity is updated, accessing both the prior and the current state of the entity.
 *
 * @param <ENTITY_ID> The type of the identifier for the entity.
 * @param <READ_DTO>  The DTO (Data Transfer Object) type used for read operations,
 *                    providing a snapshot of entity data.
 */
public interface AbsUpdateChangesHookable<ENTITY_ID, READ_DTO> {

    /**
     * Hook method called before an entity is updated.
     * This method provides an opportunity to perform actions or checks before the entity's state
     * is permanently changed. For example, validations or pre-processing can be performed on
     * the entity's previous and current states.
     *
     * @param previous The previous state of the entity represented as a READ_DTO.
     * @param current  The current state of the entity as an {@link AbstractDto}, potentially holding
     *                 updated values not yet persisted.
     */
    void beforeUpdateHook(READ_DTO previous, AbstractDto<ENTITY_ID> current);

    /**
     * Hook method called after an entity is updated.
     * This method allows for operations that need to be performed after the entity's state
     * has been updated and persisted. This could include post-processing, additional validations,
     * or triggering further dependent actions based on the old and new states.
     *
     * @param previous The previous state of the entity represented as a READ_DTO before the update.
     * @param current  The new state of the entity represented as a READ_DTO after the update.
     */
    void afterUpdateHook(READ_DTO previous, READ_DTO current);
}
