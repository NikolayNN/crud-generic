package by.nhorushko.crudgeneric.flex.mapper;

import by.nhorushko.crudgeneric.flex.AbsDtoModelMapper;
import by.nhorushko.crudgeneric.v2.domain.AbstractDto;
import by.nhorushko.crudgeneric.v2.domain.AbstractEntity;

public abstract class AbsMappingConfigUpdateDto<DTO extends AbstractDto<?>, ENTITY extends AbstractEntity<?>> extends AbsMappingConfigAbstractDto<DTO, ENTITY> {
    public AbsMappingConfigUpdateDto(AbsDtoModelMapper mapper, Class<DTO> dtoClass, Class<ENTITY> entityClass) {
        super(mapper, dtoClass, entityClass);
    }
}
