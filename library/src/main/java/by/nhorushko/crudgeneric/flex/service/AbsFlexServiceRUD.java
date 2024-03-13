package by.nhorushko.crudgeneric.flex.service;

import by.nhorushko.crudgeneric.util.FieldCopyUtil;
import by.nhorushko.crudgeneric.flex.AbsDtoModelMapper;
import by.nhorushko.crudgeneric.v2.domain.AbstractDto;
import by.nhorushko.crudgeneric.v2.domain.AbstractEntity;
import by.nhorushko.crudgeneric.flex.model.AbstractUpdateDto;
import by.nhorushko.crudgeneric.v2.domain.IdEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import javax.annotation.PostConstruct;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static java.lang.String.format;

/**
 * Read Update Delete service
 */
public abstract class AbsFlexServiceRUD<
        ENTITY_ID,
        ENTITY extends AbstractEntity<ENTITY_ID>,
        READ_DTO extends AbstractDto<ENTITY_ID>,
        UPDATE_DTO extends AbstractUpdateDto<ENTITY_ID>,
        REPOSITORY extends JpaRepository<ENTITY, ENTITY_ID>>
        extends AbsFlexServiceR<ENTITY_ID, ENTITY, READ_DTO, REPOSITORY> {
    protected Set<String> IGNORE_PARTIAL_UPDATE_PROPERTIES = Set.of("id");

    protected Class<UPDATE_DTO> updateDtoClass;

    @PostConstruct
    private void checkTypeMap() {
        checkTypeMap(updateDtoClass, entityClass);
    }

    public AbsFlexServiceRUD(AbsDtoModelMapper mapper, REPOSITORY repository,
                             Class<ENTITY> entityClass, Class<READ_DTO> readDtoClass, Class<UPDATE_DTO> updateDtoClass) {
        super(mapper, repository, entityClass, readDtoClass);
        this.updateDtoClass = updateDtoClass;
    }

    public READ_DTO update(UPDATE_DTO dto) {
        return runUpdate(dto);
    }

    public READ_DTO updatePartial(ENTITY_ID id, Object partial) {
        READ_DTO target = copyPartial(id, partial);
        return runUpdate(target);
    }

    private READ_DTO runUpdate(AbstractDto<ENTITY_ID> dto) {
        checkId(dto);
        ENTITY newValue = mapEntity(dto);
        ENTITY actual = repository.save(newValue);
        READ_DTO actualDto = mapReadDto(actual);
        afterUpdateHook(actualDto);
        return actualDto;
    }

    protected void afterUpdateHook(READ_DTO dto) {

    }

    private void checkId(IdEntity<ENTITY_ID> entity) {
        if (entity.isNew()) {
            throw new IllegalArgumentException(
                    format("Updated entity: %s should have id: (not null OR 0), but was id: %s", entity.getClass(), entity.getId()));
        }
    }

    private READ_DTO copyPartial(ENTITY_ID id, Object source) {
        READ_DTO target = getById(id);
        FieldCopyUtil.copy(source, target, IGNORE_PARTIAL_UPDATE_PROPERTIES);
        return target;
    }

    public void delete(ENTITY_ID id) {
        repository.deleteById(id);
    }

    protected ENTITY mapEntity(Object obj) {
        return this.mapper.map(obj, entityClass);
    }

    protected List<ENTITY> mapAllEntities(Collection<?> obj) {
        return this.mapper.mapAll(obj, entityClass);
    }
}
