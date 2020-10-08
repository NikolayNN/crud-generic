package by.nhorushko.crudgeneric.mapper;

import by.nhorushko.crudgeneric.domain.AbstractDto;
import by.nhorushko.crudgeneric.domain.AbstractEntity;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
/** some useful https://habr.com/ru/post/438808/ */
public abstract class AbstractMapper<ENTITY extends AbstractEntity, DTO extends AbstractDto> implements Mapper<ENTITY, DTO> {

    protected Class<ENTITY> entityClass;
    protected Class<DTO> dtoClass;
    protected ModelMapper mapper;

    public AbstractMapper(Class<ENTITY> entityClass, Class<DTO> dtoClass, ModelMapper modelMapper) {
        this.entityClass = entityClass;
        this.dtoClass = dtoClass;
        this.mapper = modelMapper;
        this.setupMapper();
        DtoMappers.register(entityClass, dtoClass, this);
    }

    public void setupMapper() {
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
        return mapper.map(dto, entityClass);
    }

    @Override
    public List<ENTITY> toEntity(List<DTO> dtos) {
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
    public List<DTO> toDto(List<ENTITY> entities) {
        return Objects.isNull(entities) ? null : entities.stream().map(e -> toDto(e)).collect(Collectors.toList());
    }

    public Converter<ENTITY, DTO> toDtoConverter() {
        return context -> {
            ENTITY source = context.getSource();
            DTO destination = context.getDestination();
            mapSpecificFields(source, destination);
            return context.getDestination();
        };
    }

    public Converter<DTO, ENTITY> toEntityConverter() {
        return context -> {
            DTO source = context.getSource();
            ENTITY destination = context.getDestination();
            mapSpecificFields(source, destination);
            return context.getDestination();
        };
    }

    public void mapSpecificFields(ENTITY source, DTO destination) {
    }

    public void mapSpecificFields(DTO source, ENTITY destination) {
    }
}