package by.nhorushko.crudgeneric.flex.service;

import by.nhorushko.crudgeneric.flex.AbsDtoModelMapper;
import by.nhorushko.crudgeneric.flex.mapper.AbsMapperExtRelation;
import by.nhorushko.crudgeneric.flex.model.AbstractCreateDto;
import by.nhorushko.crudgeneric.v2.domain.AbstractDto;
import by.nhorushko.crudgeneric.v2.domain.AbstractEntity;
import by.nhorushko.crudgeneric.flex.model.AbstractUpdateDto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

/**
 * Read Update Delete Create Service
 */
public abstract class AbsFlexServiceExtCRUD<
        ENTITY_ID,
        ENTITY extends AbstractEntity<ENTITY_ID>,
        READ_DTO extends AbstractDto<ENTITY_ID>,
        UPDATE_DTO extends AbstractUpdateDto<ENTITY_ID>,
        CREATE_DTO extends AbstractCreateDto,
        REPOSITORY extends JpaRepository<ENTITY, ENTITY_ID>,
        EXT_ID,
        EXT extends AbstractEntity<EXT_ID>>
        extends AbsFlexServiceRUD<ENTITY_ID, ENTITY, READ_DTO, UPDATE_DTO, REPOSITORY> {
    protected final AbsMapperExtRelation<CREATE_DTO, ENTITY, EXT_ID, EXT> extMapper;

    public AbsFlexServiceExtCRUD(AbsDtoModelMapper mapper,
                                 REPOSITORY repository,
                                 Class<ENTITY> entityClass,
                                 Class<READ_DTO> readDtoClass,
                                 AbsMapperExtRelation<CREATE_DTO, ENTITY, EXT_ID, EXT> extMapper) {
        super(mapper, repository, entityClass, readDtoClass);
        this.extMapper = extMapper;
    }

    public READ_DTO save(EXT_ID relationId, CREATE_DTO dto) {
        beforeSaveHook(relationId, dto);
        ENTITY entity = extMapper.map(relationId, dto);
        entity = repository.save(entity);
        READ_DTO actual = mapReadDto(entity);
        afterSaveHook(relationId, actual);
        return actual;
    }

    protected void beforeSaveHook(EXT_ID relationId, CREATE_DTO dto) {
    }

    private void afterSaveHook(EXT_ID relationId, READ_DTO actual) {
    }

    public List<READ_DTO> saveAll(EXT_ID relationId, Collection<CREATE_DTO> dtos) {
        List<ENTITY> entities = extMapper.mapAll(relationId, dtos);
        entities.forEach(e -> {
            if (!e.isNew()) throw new IllegalArgumentException(wrongIdMessage(e.getId()));
        });
        beforeSaveAllHook(relationId, dtos);
        entities = repository.saveAll(entities);
        List<READ_DTO> actual = mapAllReadDto(entities);
        afterSaveAllHook(relationId, dtos);
        return actual;
    }

    private void beforeSaveAllHook(EXT_ID relationId, Collection<CREATE_DTO> dtos) {
    }

    private void afterSaveAllHook(EXT_ID relationId, Collection<CREATE_DTO> dtos) {
    }

    private String wrongIdMessage(ENTITY_ID id) {
        return String.format("wrong id: %s to save new entity id should be null", id);
    }
}
