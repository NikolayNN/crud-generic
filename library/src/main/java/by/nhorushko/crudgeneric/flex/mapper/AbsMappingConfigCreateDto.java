package by.nhorushko.crudgeneric.flex.mapper;

import by.nhorushko.crudgeneric.flex.AbsDtoModelMapper;
import by.nhorushko.crudgeneric.flex.model.AbstractBaseDto;
import by.nhorushko.crudgeneric.v2.domain.AbstractEntity;

public abstract class AbsMappingConfigCreateDto<DTO extends AbstractBaseDto, ENTITY extends AbstractEntity<?>> extends AbsMappingConfigAbstractBaseDto<DTO, ENTITY> {
    public AbsMappingConfigCreateDto(AbsDtoModelMapper mapper, Class<DTO> dtoClass, Class<ENTITY> entityClass) {
        super(mapper, dtoClass, entityClass);
    }
}
