package by.nhorushko.crudgeneric.flex;

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
public class AbsEntityModelMapper {

    private final ModelMapper modelMapper;

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
}
