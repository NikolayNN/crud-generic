package by.nhorushko.crudgeneric.flex.mapper;

import by.nhorushko.crudgeneric.v2.domain.AbstractEntity;
import by.nhorushko.crudgeneric.flex.model.AbstractUpdateDto;
import by.nhorushko.crudgeneric.v2.mapper.AbsMapperEntityDto;
import org.modelmapper.ModelMapper;

import javax.persistence.EntityManager;

public class AbsMapperUpdateDto<ENTITY extends AbstractEntity<?>, DTO extends AbstractUpdateDto<?>> extends AbsMapperEntityDto<ENTITY, DTO> {

    public AbsMapperUpdateDto(ModelMapper modelMapper, EntityManager entityManager, Class<ENTITY> entityClass, Class<DTO> dtoClass) {
        super(modelMapper, entityManager, entityClass, dtoClass);
    }

    @Override
    protected DTO create(ENTITY from) {
        throw new UnsupportedOperationException();
    }
}
