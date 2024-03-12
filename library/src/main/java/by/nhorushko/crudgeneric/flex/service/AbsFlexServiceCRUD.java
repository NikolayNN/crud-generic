package by.nhorushko.crudgeneric.flex.service;

import by.nhorushko.crudgeneric.flex.AbsModelMapper;
import by.nhorushko.crudgeneric.flex.mapper.AbstractCreateDto;
import by.nhorushko.crudgeneric.flex.model.AbstractUpdateDto;
import by.nhorushko.crudgeneric.v2.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public abstract class AbsFlexServiceCRUD<
        ENTITY_ID,
        ENTITY extends AbstractEntity<ENTITY_ID>,
        READ_DTO extends AbstractDto<ENTITY_ID>,
        UPDATE_DTO extends AbstractUpdateDto<ENTITY_ID>,
        CREATE_DTO extends AbstractCreateDto,
        REPOSITORY extends JpaRepository<ENTITY, ENTITY_ID>>
        extends AbsFlexServiceRUD<ENTITY_ID, ENTITY, READ_DTO, UPDATE_DTO, REPOSITORY> {

    public AbsFlexServiceCRUD(AbsModelMapper mapper, REPOSITORY repository, Class<ENTITY> entityClass, Class<READ_DTO> readDtoClass) {
        super(mapper, repository, entityClass, readDtoClass);
    }

    public READ_DTO save(CREATE_DTO dto) {
        beforeSaveHook(dto);
        ENTITY entity = repository.save(mapEntity(dto));
        READ_DTO actual = mapReadDto(entity);
        afterSaveHook(actual);
        return actual;
    }

    protected void beforeSaveHook(CREATE_DTO dto) {
    }

    protected void afterSaveHook(READ_DTO dto) {
    }

    public List<READ_DTO> saveAll(Collection<CREATE_DTO> dto) {
        beforeSaveAllHook(dto);
        List<ENTITY> entities = repository.saveAll(mapAllEntities(dto));
        List<READ_DTO> actual = mapAllReadDto(entities);
        afterSaveAllHook(actual);
        return actual;
    }

    protected void beforeSaveAllHook(Collection<CREATE_DTO> dtos) {
    }

    protected void afterSaveAllHook(Collection<READ_DTO> dtos) {
    }
}
