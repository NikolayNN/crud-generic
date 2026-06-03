package by.nhorushko.crudgeneric.service;

import by.nhorushko.crudgeneric.domain.AbstractDto;
import by.nhorushko.crudgeneric.domain.AbstractEntity;
import by.nhorushko.crudgeneric.exception.AppNotFoundException;
import by.nhorushko.crudgeneric.mapper.AbstractMapper;
import by.nhorushko.crudgeneric.util.FieldCopyUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Set;

@Deprecated
public abstract class RudGenericService<
        DTO extends AbstractDto,
        ENTITY extends AbstractEntity,
        REPOSITORY extends JpaRepository<ENTITY, Long> & JpaSpecificationExecutor<ENTITY>,
        MAPPER extends AbstractMapper<ENTITY, DTO>> extends PagingAndSortingImmutableGenericService<DTO, ENTITY, REPOSITORY, MAPPER> {

    protected Set<String> ignorePartialUpdateProperties = Set.of("id");

    @PersistenceContext
    protected EntityManager entityManager;

    public RudGenericService(REPOSITORY repository, MAPPER mapper, Class<DTO> dtoClass, Class<ENTITY> entityClass) {
        super(repository, mapper, dtoClass, entityClass);
    }

    public RudGenericService(REPOSITORY repository, MAPPER mapper, Class<DTO> dtoClass, Class<ENTITY> entityClass,
                             Set<String> ignorePartialUpdateProperties) {
        super(repository, mapper, dtoClass, entityClass);
        this.ignorePartialUpdateProperties = ignorePartialUpdateProperties;
    }

    /**
     * Insert-if-absent / merge-if-present. Restores the behaviour Hibernate 6.5 merge gave
     * implicitly for detached entities whose row does not exist (broken on Hibernate 6.6).
     * The sentinel id 0 is normalised to {@code null} here — the single chokepoint every save
     * path funnels through — so application mappers that override {@code toEntity(...)} and
     * bypass the base converter's normalisation still route a new entity to persist.
     */
    protected ENTITY persistOrMerge(ENTITY entity) {
        Long id = entity.getId();
        if (id != null && id == 0L) {
            entity.setId(null);
            id = null;
        }
        if (id == null || !repository.existsById(id)) {
            entityManager.persist(entity);
            return entity;
        }
        return repository.save(entity);
    }

    public void deleteById(Long id) {
        ENTITY entity = repository.findById(id)
                .orElseThrow(() -> new AppNotFoundException(String.format("Entity id: %s was not found", id)));
        processBeforeDelete(entity);
        repository.deleteById(id);
        processAfterDelete(id);
    }

    public void processBeforeDelete(ENTITY entity) {

    }

    public void processAfterDelete(Long id) {

    }

    public void deleteAll(List<DTO> dtos) {
        repository.deleteAll(mapper.toEntity(dtos));
    }

    public DTO update(DTO obj) {
        check(obj);
        checkIdForUpdate(obj.getId());
        return saveUpdatedDto(obj);
    }

    public DTO updatePartial(Long id, Object source) {
        checkIdForUpdate(id);
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
}
