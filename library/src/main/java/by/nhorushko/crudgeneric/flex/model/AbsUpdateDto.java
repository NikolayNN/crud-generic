package by.nhorushko.crudgeneric.flex.model;

import by.nhorushko.crudgeneric.v2.domain.AbstractDto;

/**
 * Interface for marking DTOs (Data Transfer Objects) that are specifically intended for updating entities.
 * <p>
 * This interface extends {@link AbstractDto}, carrying forward the principle of having an identifiable ID.
 * By implementing this interface, a DTO is designated as being particularly suited for operations that
 * update existing entities in a system. Such DTOs typically contain a subset of entity fields that are
 * allowable or intended for update operations.
 * </p>
 * @param <ID> the type of the ID field (e.g., Long, String, etc.), specifying the type of the unique
 *             identifier that associates the DTO with its corresponding entity.
 */
public interface AbsUpdateDto<ID> extends AbstractDto<ID> {
}
