package by.nhorushko.crudgeneric.flex.service;

import by.nhorushko.crudgeneric.flex.AbsDtoModelMapper;
import by.nhorushko.crudgeneric.flex.model.AbsCreateDto;
import by.nhorushko.crudgeneric.flex.model.AbsUpdateDto;
import by.nhorushko.crudgeneric.v2.domain.*;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.jpa.repository.JpaRepository;

import javax.annotation.PostConstruct;
import java.util.Collection;
import java.util.List;

@FieldNameConstants
public abstract class AbsFlexServiceCRUD<
        ENTITY_ID,
        ENTITY extends AbstractEntity<ENTITY_ID>,
        READ_DTO extends AbstractDto<ENTITY_ID>,
        UPDATE_DTO extends AbsUpdateDto<ENTITY_ID>,
        CREATE_DTO extends AbsCreateDto,
        REPOSITORY extends JpaRepository<ENTITY, ENTITY_ID>>
        extends AbsFlexServiceRUD<ENTITY_ID, ENTITY, READ_DTO, UPDATE_DTO, REPOSITORY> {

    protected Class<CREATE_DTO> createDtoClass;

    public AbsFlexServiceCRUD(AbsDtoModelMapper mapper, REPOSITORY repository, Class<ENTITY> entityClass, Class<READ_DTO> readDtoClass, Class<UPDATE_DTO> updateDtoClass, Class<CREATE_DTO> createDtoClass) {
        super(mapper, repository, entityClass, readDtoClass, updateDtoClass);
        this.createDtoClass = createDtoClass;
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
