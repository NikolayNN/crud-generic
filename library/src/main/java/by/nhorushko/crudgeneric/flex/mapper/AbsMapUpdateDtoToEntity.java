package by.nhorushko.crudgeneric.flex.mapper;

import by.nhorushko.crudgeneric.flex.AbsDtoModelMapper;
import by.nhorushko.crudgeneric.flex.mapper.core.AbsMapDtoToEntity;
import by.nhorushko.crudgeneric.v2.domain.AbstractDto;
import by.nhorushko.crudgeneric.v2.domain.AbstractEntity;

public abstract class AbsMapUpdateDtoToEntity<DTO extends AbstractDto<?>, ENTITY extends AbstractEntity<?>> extends AbsMapDtoToEntity<DTO, ENTITY> {
    public AbsMapUpdateDtoToEntity(AbsDtoModelMapper mapper, Class<DTO> dtoClass, Class<ENTITY> entityClass) {
        super(mapper, dtoClass, entityClass);
    }
}
