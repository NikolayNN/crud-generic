package by.nhorushko.crudgeneric.flex.mapper;

import by.nhorushko.crudgeneric.flex.AbsDtoModelMapper;
import by.nhorushko.crudgeneric.flex.model.AbsBaseDto;
import by.nhorushko.crudgeneric.v2.domain.AbstractEntity;

public abstract class AbsMappingConfigCreateDto<DTO extends AbsBaseDto, ENTITY extends AbstractEntity<?>> extends AbsMappingConfigAbstractBaseDto<DTO, ENTITY> {
    public AbsMappingConfigCreateDto(AbsDtoModelMapper mapper, Class<DTO> dtoClass, Class<ENTITY> entityClass) {
        super(mapper, dtoClass, entityClass);
    }
}
