package by.nhorushko.crudgeneric.flex;

import by.nhorushko.crudgeneric.v2.domain.AbstractDto;
import by.nhorushko.crudgeneric.v2.domain.AbstractEntity;
import org.modelmapper.ModelMapper;

import javax.persistence.EntityManager;

/**
 * Specialized model mapper for mapping between Data Transfer Objects (DTOs) and entities.
 * <p>
 * This class extends {@link AbsEntityModelMapper} with additional functionalities tailored to working with
 * JPA entities. It utilizes {@link EntityManager} to fetch entity references based on IDs, facilitating
 * efficient operations that require entity proxies without fetching the entire entity from the database.
 * </p>
 */
public class AbsDtoModelMapper extends AbsEntityModelMapper {
    private final EntityManager entityManager;

    public AbsDtoModelMapper(ModelMapper modelMapper, EntityManager entityManager) {
        super(modelMapper);
        this.entityManager = entityManager;
    }

    /**
     * Retrieves a reference to an entity of a specified type based on the ID provided within a DTO.
     * <p>
     * This method is useful for operations where an entity reference is needed without the requirement
     * of loading the full entity from the database. It is particularly beneficial in update scenarios
     * where setting relationships based on the provided ID is sufficient.
     * </p>
     *
     * @param dto              the DTO containing the ID of the entity
     * @param destinationClass the class of the entity to which the reference is sought
     * @param <T>              the type of the entity
     * @return a reference to the entity of type {@code T}, or throws an exception if no such entity exists
     */
    public <T extends AbstractEntity<?>> T reference(AbstractDto<?> dto, Class<T> destinationClass) {
        return referenceById(dto.getId(), destinationClass);
    }

    /**
     * Retrieves a reference to an entity of a specified type based on a given ID.
     * <p>
     * Similar to {@code reference}, but this method directly accepts an entity ID instead of a DTO.
     * This allows for greater flexibility in retrieving entity references when the DTO is not available
     * or when working with raw IDs.
     * </p>
     *
     * @param id               the ID of the entity
     * @param destinationClass the class of the entity to which the reference is sought
     * @param <T>              the type of the entity
     * @return a reference to the entity of type {@code T}, or throws an exception if no such entity exists
     */
    public <T extends AbstractEntity<?>> T referenceById(Object id, Class<T> destinationClass) {
        return entityManager.getReference(destinationClass, id);
    }
}
