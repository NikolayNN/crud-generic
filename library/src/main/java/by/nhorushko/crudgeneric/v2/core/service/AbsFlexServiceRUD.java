package by.nhorushko.crudgeneric.v2.core.service;

import by.nhorushko.crudgeneric.util.FieldCopyUtil;
import by.nhorushko.crudgeneric.v2.domain.AbstractDto;
import by.nhorushko.crudgeneric.v2.domain.AbstractEntity;
import by.nhorushko.crudgeneric.v2.domain.IdEntity;
import by.nhorushko.crudgeneric.v2.mapper.AbsMapperEntityDto;
import by.nhorushko.crudgeneric.v2.service.AbsServiceR;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Set;

import static java.lang.String.format;

/**
 * Read Update Delete service
 */
public abstract class AbsFlexServiceRUD<
        ENTITY_ID,
        ENTITY extends AbstractEntity<ENTITY_ID>,
        READ_DTO extends AbstractDto<ENTITY_ID>,
        READ_DTO_MAPPER extends AbsMapperEntityDto<ENTITY, READ_DTO>,
        UPDATE_DTO extends AbstractDto<ENTITY_ID>,
        REPOSITORY extends JpaRepository<ENTITY, ENTITY_ID>>
        extends AbsServiceR<ENTITY_ID, ENTITY, READ_DTO, READ_DTO_MAPPER, REPOSITORY> {

    protected final Class<ENTITY> entityClass;

    protected Set<String> IGNORE_PARTIAL_UPDATE_PROPERTIES = Set.of("id");

    public AbsFlexServiceRUD(READ_DTO_MAPPER mapper, REPOSITORY repository, Class<ENTITY> entityClass) {
        super(mapper, repository);
        this.entityClass = entityClass;
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
        ENTITY prevValue = repository.getOne(dto.getId());
        ENTITY newValue = mapper.map(dto, entityClass);
        preUpdate(prevValue, newValue);
        ENTITY actual = repository.save(newValue);
        return mapper.toDto(actual);
    }

    /**
     * Здесь можно внести изменения в newValue, на основании предыдущего значения
     * @param prevValue
     * @param newValue
     */
    protected void preUpdate(ENTITY prevValue, ENTITY newValue) {

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
}
