package by.nhorushko.crudgeneric.flex.mapper;

import by.nhorushko.crudgeneric.flex.AbsDtoModelMapper;
import by.nhorushko.crudgeneric.v2.domain.AbstractDto;
import by.nhorushko.crudgeneric.v2.domain.AbstractEntity;

import javax.persistence.EntityManager;

public abstract class AbsMappingConfigAbstractDtoPresetEntity<DTO extends AbstractDto<?>, ENTITY extends AbstractEntity<?>> extends AbsMappingConfigAbstractBaseDto<DTO, ENTITY> {

    private final EntityManager entityManager;

    public AbsMappingConfigAbstractDtoPresetEntity(AbsDtoModelMapper mapper, Class<DTO> dtoClass, Class<ENTITY> entityClass, EntityManager entityManager) {
        super(mapper, dtoClass, entityClass);
        this.entityManager = entityManager;
    }

    @Override
    protected ENTITY handleAfterMapSpecificFields(DTO source, ENTITY destination) {
        ENTITY actualDestination = entityManager.find(entityClass, ((AbstractDto<?>)source).getId());
        mapper.getModelMapper().map(destination, actualDestination);
        return actualDestination;
    }
}
