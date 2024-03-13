package by.nhorushko.crudgeneric.flex.mapper;

import by.nhorushko.crudgeneric.flex.AbsDtoModelMapper;
import by.nhorushko.crudgeneric.flex.model.AbstractUpdateDto;
import by.nhorushko.crudgeneric.v2.domain.AbstractEntity;

import javax.persistence.EntityManager;

public abstract class AbsMappingConfigUpdateDtoPresetEntity<DTO extends AbstractUpdateDto<?>, ENTITY extends AbstractEntity<?>> extends AbsMappingConfigAbstractDtoPresetEntity<DTO, ENTITY> {
    public AbsMappingConfigUpdateDtoPresetEntity(AbsDtoModelMapper mapper, Class<DTO> dtoClass, Class<ENTITY> entityClass, EntityManager entityManager) {
        super(mapper, dtoClass, entityClass, entityManager);
    }
}
