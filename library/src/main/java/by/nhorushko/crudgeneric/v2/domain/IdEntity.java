package by.nhorushko.crudgeneric.v2.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Marker interface for entities with an identifiable ID of any type.
 * <p>
 * This interface is designed to be implemented by entity classes that can be uniquely identified by an ID.
 * The type of the ID can vary (e.g., {@code Long}, {@code String}, etc.), and is specified by the generic
 * type parameter {@code ID}. It includes a method to retrieve the ID of the entity and a default method
 * to check if the entity is considered "new". An entity is considered "new" if its ID is either {@code null}
 * or, in the case of numeric IDs, has a value of zero.
 * </p>
 * <p>
 * Implementing this interface allows for a uniform way of handling entities across the application,
 * facilitating operations like persistence and retrieval by providing a common method to identify entities.
 * </p>
 *
 * @param <ID> the type of the ID field (e.g., {@code Long}, {@code String}, etc.)
 */
public interface IdEntity<ID> {

    /**
     * Retrieves the ID of this entity.
     *
     * @return the ID of the entity, which could be of any type specified by the {@code ID} type parameter
     */
    ID getId();


    /**
     * Checks if this entity is considered "new".
     * <p>
     * An entity is considered "new" if its ID is {@code null} or, for numeric IDs, equals to zero. This default
     * implementation covers common ID types but can be overridden if a different logic is required for determining
     * the newness of an entity.
     * </p>
     *
     * @return {@code true} if the entity is new, {@code false} otherwise
     */
    @JsonIgnore
    default boolean isNew() {
        if (getId() == null) {
            return true;
        }
        if (getId() instanceof Number) {
            return ((Number) getId()).longValue() == 0;
        }
        return false;
    }
}
