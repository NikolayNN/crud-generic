package by.nhorushko.crudgeneric.flex.mapper;

import by.nhorushko.crudgeneric.flex.AbsDtoModelMapper;
import by.nhorushko.crudgeneric.flex.mapper.core.AbsMapDtoToPresetEntity;
import by.nhorushko.crudgeneric.flex.model.AbsUpdateDto;
import by.nhorushko.crudgeneric.v2.domain.AbstractEntity;

import javax.persistence.EntityManager;

public abstract class AbsMapUpdateDtoToPresetEntity<DTO extends AbsUpdateDto<?>, ENTITY extends AbstractEntity<?>> extends AbsMapDtoToPresetEntity<DTO, ENTITY> {
    public AbsMapUpdateDtoToPresetEntity(AbsDtoModelMapper mapper, Class<DTO> dtoClass, Class<ENTITY> entityClass, EntityManager entityManager) {
        super(mapper, dtoClass, entityClass, entityManager);
    }
}
