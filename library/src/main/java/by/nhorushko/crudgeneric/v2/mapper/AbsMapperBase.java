package by.nhorushko.crudgeneric.v2.mapper;

import org.modelmapper.ModelMapper;

import java.util.Collection;
import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * Mapping for objects without Id
 * https://stackoverflow.com/questions/44534172/how-to-customize-modelmapper
 */
public abstract class AbsMapperBase<FROM, TO> {

    protected final ModelMapper modelMapper;
    protected final Class<FROM> entityClass;
    protected final Class<TO> dtoClass;

    public AbsMapperBase(ModelMapper modelMapper, Class<FROM> entityClass, Class<TO> dtoClass) {
        this.modelMapper = modelMapper;
        this.entityClass = entityClass;
        this.dtoClass = dtoClass;
    }

    public <T> T map(Object source, Class<T> destinationType) {
        if (source == null) {
            return null;
        }
        return modelMapper.map(source, destinationType);
    }

    public <T> List<T> mapAll(Collection<?> source, Class<T> destinationType) {
        if (source == null) {
            return null;
        }
        return source.stream()
                .map(o -> map(o, destinationType))
                .collect(toList());
    }
}
