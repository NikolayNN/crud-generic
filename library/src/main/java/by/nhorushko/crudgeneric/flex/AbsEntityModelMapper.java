package by.nhorushko.crudgeneric.flex;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;

import java.util.Collection;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Getter
@RequiredArgsConstructor
public class AbsEntityModelMapper {

    private final ModelMapper modelMapper;

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
