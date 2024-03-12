package by.nhorushko.crudgeneric.flex.mapper;

import by.nhorushko.crudgeneric.v2.domain.AbstractEntity;
import by.nhorushko.crudgeneric.v2.mapper.AbsMapperEntityDto;
import org.modelmapper.ModelMapper;

import javax.persistence.EntityManager;

public class AbsMapperCreateDto<ENTITY extends AbstractEntity<?>, DTO extends AbstractCreateDto> extends AbsMapperEntityDto<ENTITY, DTO> {

    public AbsMapperCreateDto(ModelMapper modelMapper, EntityManager entityManager, Class<ENTITY> entityClass, Class<DTO> dtoClass) {
        super(modelMapper, entityManager, entityClass, dtoClass);
    }

    @Override
    protected DTO create(ENTITY from) {
        throw new UnsupportedOperationException();
    }
}
