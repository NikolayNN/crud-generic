package by.nhorushko.crudgeneric.mapper;

import by.nhorushko.crudgeneric.domain.AbstractDto;
import by.nhorushko.crudgeneric.domain.AbstractEntity;
import org.modelmapper.Converter;
import org.modelmapper.MappingException;
import org.modelmapper.ModelMapper;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * some useful https://habr.com/ru/post/438808/
 */
@Deprecated
public abstract class AbstractMapper<ENTITY extends AbstractEntity, DTO extends AbstractDto> implements Mapper<ENTITY, DTO> {

    protected final Class<ENTITY> entityClass;
    protected final Class<DTO> dtoClass;
    protected final ModelMapper mapper;

    public AbstractMapper(Class<ENTITY> entityClass, Class<DTO> dtoClass, ModelMapper modelMapper) {
        this.entityClass = entityClass;
        this.dtoClass = dtoClass;
        this.mapper = modelMapper;
        this.setupMapper();
        DtoMappers.register(entityClass, dtoClass, this);
    }

    protected void setupMapper() {
        mapper.createTypeMap(entityClass, dtoClass)
                .setPostConverter(toDtoConverter());
        mapper.createTypeMap(dtoClass, entityClass)
                .setPostConverter(toEntityConverter());
    }

    @Override
    public ENTITY toEntity(DTO dto) {
        return Objects.isNull(dto)
                ? null
                : mapEntity(dto);
    }

    protected ENTITY mapEntity(DTO dto) {
        try {
            return mapper.map(dto, entityClass);
        } catch (MappingException e) {
            e.printStackTrace();
            throw (RuntimeException) e.getCause();
        }
    }

    @Override
    public List<ENTITY> toEntity(Collection<? extends DTO> dtos) {
        return Objects.isNull(dtos) ? null : dtos.stream().map(d -> toEntity(d)).collect(Collectors.toList());
    }

    @Override
    public DTO toDto(ENTITY entity) {
        return Objects.isNull(entity)
                ? null
                : mapDto(entity);
    }

    protected DTO mapDto(ENTITY entity) {
        return mapper.map(entity, dtoClass);
    }

    @Override
    public List<DTO> toDto(Collection<ENTITY> entities) {
        return Objects.isNull(entities) ? null : entities.stream().map(e -> toDto(e)).collect(Collectors.toList());
    }

    protected Converter<ENTITY, DTO> toDtoConverter() {
        return context -> {
            ENTITY source = context.getSource();
            DTO destination = context.getDestination();
            mapSpecificFields(source, destination);
            return context.getDestination();
        };
    }

    protected Converter<DTO, ENTITY> toEntityConverter() {
        return context -> {
            DTO source = context.getSource();
            ENTITY destination = context.getDestination();
            mapSpecificFields(source, destination);
            if (destination.getId() != null && destination.getId() == 0L) {
                destination.setId(null);
            }
            return context.getDestination();
        };
    }

    protected void mapSpecificFields(ENTITY source, DTO destination) {
    }

    protected void mapSpecificFields(DTO source, ENTITY destination) {
    }
}
