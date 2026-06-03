package by.nhorushko.crudgeneric.v2.service;

import by.nhorushko.crudgeneric.v2.domain.AbstractDto;
import by.nhorushko.crudgeneric.v2.domain.AbstractEntity;
import by.nhorushko.crudgeneric.v2.mapper.AbsMapperEntityDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

public abstract class AbsServiceCRUD<
        ENTITY_ID,
        ENTITY extends AbstractEntity<ENTITY_ID>,
        DTO extends AbstractDto<ENTITY_ID>,
        REPOSITORY extends JpaRepository<ENTITY, ENTITY_ID>>

        extends AbsServiceRUD<ENTITY_ID, ENTITY, DTO, AbsMapperEntityDto<ENTITY, DTO>, REPOSITORY> {

    @PersistenceContext
    protected EntityManager entityManager;

    public AbsServiceCRUD(AbsMapperEntityDto<ENTITY, DTO> mapper, REPOSITORY repository) {
        super(mapper, repository);
    }

    /**
     * Insert-if-absent / merge-if-present. Restores Hibernate 6.5 merge-of-absent-row semantics
     * (broken on 6.6). Sentinel id 0 is already nulled by the mapper, so a null id routes to persist.
     */
    protected ENTITY persistOrMerge(ENTITY entity) {
        ENTITY_ID id = entity.getId();
        if (id == null || !repository.existsById(id)) {
            entityManager.persist(entity);
            return entity;
        }
        return repository.save(entity);
    }

    public DTO save(DTO dto) {
        ENTITY entity = persistOrMerge(mapper.toEntity(dto));
        DTO saved = mapper.toDto(entity);
        afterSaveHook(saved);
        return saved;
    }

    public List<DTO> saveAll(Collection<DTO> dto) {
        List<ENTITY> entities = mapper.toEntities(dto).stream()
                .map(this::persistOrMerge)
                .collect(java.util.stream.Collectors.toList());
        List<DTO> saved = mapper.toDtos(entities);
        saved.forEach(this::afterSaveHook);
        return saved;
    }

    /**
     * Hook method that is called after saving each DTO.
     * <p>
     * This method provides a way to insert additional processing
     * after an entity is saved and converted back to its DTO form.
     * Subclasses can override this method to implement specific behaviors
     * such as event publishing or custom logging.
     * </p>
     * <p>
     * By default, this method does nothing and is intended to be overridden.
     * </p>
     *
     * @param dto The DTO object that has been saved.
     */
    protected void afterSaveHook(DTO dto) {
        // Default implementation does nothing, intended for override.
    }
}
