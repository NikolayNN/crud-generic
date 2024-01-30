package by.nhorushko.crudgeneric.v2.service;

import by.nhorushko.crudgeneric.v2.domain.AbstractDto;
import by.nhorushko.crudgeneric.v2.domain.AbstractEntity;
import by.nhorushko.crudgeneric.v2.mapper.AbsMapperEntityDto;
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

    public AbsServiceCRUD(AbsMapperEntityDto<ENTITY, DTO> mapper, REPOSITORY repository) {
        super(mapper, repository);
    }

    public DTO save(DTO dto) {
        ENTITY entity = repository.save(mapper.toEntity(dto));
        DTO saved = mapper.toDto(entity);
        afterSaveHook(saved);
        return saved;
    }

    public List<DTO> saveAll(Collection<DTO> dto) {
        List<ENTITY> entities = repository.saveAll(mapper.toEntities(dto));
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
