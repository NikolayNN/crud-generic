package by.nhorushko.crudgeneric.flex.service;

import by.nhorushko.crudgeneric.flex.AbsModelMapper;
import by.nhorushko.crudgeneric.flex.mapper.AbstractCreateDto;
import by.nhorushko.crudgeneric.v2.domain.AbstractDto;
import by.nhorushko.crudgeneric.v2.domain.AbstractEntity;
import by.nhorushko.crudgeneric.flex.model.AbstractUpdateDto;
import by.nhorushko.crudgeneric.v2.mapper.AbsMapperEntityExtDto;
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
        EXT_ID,
        EXT extends AbstractEntity<EXT_ID>,
        REPOSITORY extends JpaRepository<ENTITY, ENTITY_ID>>
        extends AbsFlexServiceRUD<ENTITY_ID, ENTITY, READ_DTO, UPDATE_DTO, REPOSITORY> {

    private final AbsMapperEntityExtDto<ENTITY, CREATE_DTO, EXT_ID, EXT> extCreateMapper;

    public AbsFlexServiceExtCRUD(AbsModelMapper mapper,
                                 REPOSITORY repository,
                                 Class<ENTITY> entityClass,
                                 Class<READ_DTO> readDtoClass,
                                 AbsMapperEntityExtDto<ENTITY, CREATE_DTO, EXT_ID, EXT> extCreateMapper) {
        super(mapper, repository, entityClass, readDtoClass);
        this.extCreateMapper = extCreateMapper;
    }

    public READ_DTO save(EXT_ID relationId, CREATE_DTO dto) {
            beforeSaveHook(relationId, dto);
            ENTITY entity = repository.save(extCreateMapper.toEntity(relationId, dto));
            READ_DTO actual = mapReadDto(entity);
            afterSaveHook(relationId, actual);
            return actual;
    }

    protected void beforeSaveHook(EXT_ID relationId, CREATE_DTO dto) {
    }

    private void afterSaveHook(EXT_ID relationId, READ_DTO actual) {
    }

    public List<READ_DTO> saveAll(EXT_ID relationId, Collection<CREATE_DTO> dtos) {
        List<ENTITY> entities = extCreateMapper.toEntities(relationId, dtos);
        entities.forEach(e -> {
            if (!e.isNew()) throw new IllegalArgumentException(wrongIdMessage(e.getId()));
        });
        beforeSaveAllHook(relationId, dtos);
        entities = repository.saveAll(extCreateMapper.toEntities(relationId, dtos));
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
