package by.nhorushko.crudgeneric.flex;

import lombok.Value;
import org.modelmapper.ModelMapper;

import java.util.Collection;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Value
public class AbsModelMapper {

    ModelMapper modelMapper;

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
