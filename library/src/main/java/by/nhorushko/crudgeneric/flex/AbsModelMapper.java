package by.nhorushko.crudgeneric.flex;

import by.nhorushko.crudgeneric.v2.domain.AbstractDto;
import by.nhorushko.crudgeneric.v2.domain.AbstractEntity;
import jakarta.persistence.EntityManager;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;

import java.util.Collection;
import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * Utility class for mapping between different object models.
 * <p>
 * This class wraps {@link ModelMapper} to provide convenient methods for mapping objects from one type
 * to another. It is designed to simplify the transformation of data between layers (e.g., from entities
 * to DTOs and vice versa) across the application. The class ensures that if the source object is null,
 * the mapping methods will return null instead of throwing an exception.
 * </p>
 */
@Getter
@RequiredArgsConstructor
public class AbsModelMapper {

    private final ModelMapper modelMapper;
    private final EntityManager entityManager;

    /**
     * Maps an object of any type to a specified type.
     * <p>
     * This method utilizes {@link ModelMapper} to convert an object from its current type to the
     * specified destination type. It is a generic method capable of handling any type of conversion
     * as long as the source and destination types are compatible with {@link ModelMapper}'s configuration.
     * </p>
     *
     * @param source          the source object to be mapped
     * @param destinationType the class type of the destination object
     * @param <T>             the type parameter of the destination class
     * @return an object of type {@code T}, which is the mapped object of the specified destination type,
     *         or null if the source object is null
     */
    public <T> T map(Object source, Class<T> destinationType) {
        if (source == null) {
            return null;
        }
        return modelMapper.map(source, destinationType);
    }

    /**
     * Maps a collection of objects to a list of objects of a specified type.
     * <p>
     * This method processes each object in the provided collection, mapping them to the specified
     * destination type. It leverages the {@code map} method for individual object mappings and
     * collects the results into a list.
     * </p>
     *
     * @param source          the collection of source objects to be mapped
     * @param destinationType the class type of the destination objects
     * @param <T>             the type parameter of the destination class
     * @return a list of objects of type {@code T}, each being a mapped object of the specified
     *         destination type, or null if the source collection is null
     */
    public <T> List<T> mapAll(Collection<?> source, Class<T> destinationType) {
        if (source == null) {
            return null;
        }
        return source.stream()
                .map(o -> map(o, destinationType))
                .collect(toList());
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
