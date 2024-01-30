package by.nhorushko.crudgeneric.v2.service;

import by.nhorushko.crudgeneric.v2.domain.AbstractDto;
import by.nhorushko.crudgeneric.v2.domain.AbstractEntity;
import by.nhorushko.crudgeneric.v2.mapper.AbsMapperEntityExtDto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

/**
 * Read Update Delete Create Service
 */
public abstract class AbsServiceExtCRUD<
        ENTITY_ID,
        ENTITY extends AbstractEntity<ENTITY_ID>,
        DTO extends AbstractDto<ENTITY_ID>,
        EXT_ID,
        EXT extends AbstractEntity<EXT_ID>,
        REPOSITORY extends JpaRepository<ENTITY, ENTITY_ID>>

        extends AbsServiceRUD<ENTITY_ID, ENTITY, DTO, AbsMapperEntityExtDto<ENTITY, DTO, EXT_ID, EXT>, REPOSITORY> {

    public AbsServiceExtCRUD(AbsMapperEntityExtDto<ENTITY, DTO, EXT_ID, EXT> mapper,
                             REPOSITORY repository) {
        super(mapper, repository);
    }

    public DTO save(EXT_ID relationId, DTO dto) {
        if (dto.isNew()) {
            ENTITY entity = repository.save(mapper.toEntity(relationId, dto));
            DTO saved = mapper.toDto(entity);
            afterSaveHook(relationId, saved);
            return saved;
        }
        throw new IllegalArgumentException(wrongIdMessage(dto.getId()));
    }

    public List<DTO> saveAll(EXT_ID relationId, Collection<DTO> dtos) {
        List<ENTITY> entities = mapper.toEntities(relationId, dtos);
        entities.forEach(e -> {
            if (!e.isNew()) throw new IllegalArgumentException(wrongIdMessage(e.getId()));
        });
        entities = repository.saveAll(mapper.toEntities(relationId, dtos));
        List<DTO> saved = mapper.toDtos(entities);
        saved.forEach(s -> afterSaveHook(relationId, s));
        return saved;
    }

    private String wrongIdMessage(ENTITY_ID id) {
        return String.format("wrong id: %s to save new entity id should be null", id);
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
    protected void afterSaveHook(EXT_ID relationId, DTO dto) {
        // Default implementation does nothing, intended for override.
    }
}
