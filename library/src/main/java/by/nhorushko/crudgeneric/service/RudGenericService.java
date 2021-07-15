package by.nhorushko.crudgeneric.service;

import by.nhorushko.crudgeneric.domain.AbstractDto;
import by.nhorushko.crudgeneric.domain.AbstractEntity;
import by.nhorushko.crudgeneric.exception.AppNotFoundException;
import by.nhorushko.crudgeneric.mapper.AbstractMapper;
import by.nhorushko.crudgeneric.util.FieldCopyUtil;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Set;

public abstract class RudGenericService<
        DTO extends AbstractDto,
        ENTITY extends AbstractEntity,
        REPOSITORY extends JpaRepository<ENTITY, Long> & JpaSpecificationExecutor<ENTITY>,
        MAPPER extends AbstractMapper<ENTITY, DTO>> extends PagingAndSortingImmutableGenericService<DTO, ENTITY, REPOSITORY, MAPPER> {

    protected Set<String> ignorePartialUpdateProperties = Set.of("id");

    public RudGenericService(REPOSITORY repository, MAPPER mapper, Class<DTO> dtoClass, Class<ENTITY> entityClass) {
        super(repository, mapper, dtoClass, entityClass);
    }

    public RudGenericService(REPOSITORY repository, MAPPER mapper, Class<DTO> dtoClass, Class<ENTITY> entityClass,
                             Set<String> ignorePartialUpdateProperties) {
        super(repository, mapper, dtoClass, entityClass);
        this.ignorePartialUpdateProperties = ignorePartialUpdateProperties;
    }

    public void deleteById(Long id) {
        try {
            repository.deleteById(id);
        } catch (EmptyResultDataAccessException ex) {
            throw new AppNotFoundException(String.format("Entity %s with id: %s was not found", entityClass.getSimpleName(), id), ex);
        }
    }

    public void deleteAll(List<DTO> dtos) {
        repository.deleteAll(mapper.toEntity(dtos));
    }

    public DTO update(DTO obj) {
        check(obj);
        checkEntityIdForUpdate(obj.getId());
        return saveUpdatedDto(obj);
    }

    public DTO updatePartial(Long id, Object source) {
        checkEntityIdForUpdate(id);
        DTO target = copyPartial(id, source);
        return saveUpdatedDto(target);
    }

    private DTO copyPartial(Long id, Object source) {
        DTO target = getById(id);
        FieldCopyUtil.copy(source, target, ignorePartialUpdateProperties);
        return target;
    }

    private DTO saveUpdatedDto(DTO obj) {
        check(obj);
        ENTITY source = findEntityById(obj.getId());
        ENTITY target = mapper.toEntity(obj);
        setupEntityBeforeUpdate(source, target);
        ENTITY savedEntity = repository.save(target);
        processEntityAfterUpdate(savedEntity);
        return mapper.toDto(savedEntity);
    }

    protected void check(DTO obj) {

    }

    protected void processEntityAfterUpdate(ENTITY entity){

    }

    protected void setupEntityBeforeUpdate(ENTITY source, ENTITY target) {
    }

    protected void checkEntityIdForUpdate(Long id) {
        if (isNew(id)) {
            throw new IllegalArgumentException(
                    String.format("Updated entity should have id: (not null OR 0), but was id: %s", id));
        }
    }

    protected void setEntityIdForSave(ENTITY e) {
        if (!isNew(e)) {
            throw new IllegalArgumentException(
                    String.format("Saved Entity should have id equals (null or 0), but id: %s", e.getId()));
        }
        e.setId(null);
    }
}
