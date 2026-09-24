package by.nhorushko.crudgeneric.flex.mapper;

import by.nhorushko.crudgeneric.flex.AbsMapper;
import by.nhorushko.crudgeneric.flex.model.AbsCreateDto;
import by.nhorushko.crudgeneric.flex.model.AbstractEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Creates entities from create DTOs and links each to an existing external entity identified by id.
 * <p>
 * The entity is mapped with the {@code Mapper<DTO, ENTITY>} from the registry, the external entity
 * is a reference obtained by id without loading it, and {@link #setRelation} links the two —
 * explicitly, in the subclass.
 * </p>
 *
 * @param <DTO>    the create DTO
 * @param <ENTITY> the entity created from the DTO
 * @param <EXT_ID> the id type of the external entity
 * @param <EXT>    the external entity the created one is linked to
 */
public abstract class AbsMapperExtRelation<DTO extends AbsCreateDto, ENTITY, EXT_ID, EXT extends AbstractEntity<?>> {

    private final AbsMapper mapper;
    private final Class<ENTITY> entityClass;
    private final Class<EXT> extClass;

    public AbsMapperExtRelation(AbsMapper mapper, Class<ENTITY> entityClass, Class<EXT> extClass) {
        this.mapper = mapper;
        this.entityClass = entityClass;
        this.extClass = extClass;
    }

    /**
     * Maps the DTO to a new entity and links it to the external entity with id {@code extId}.
     */
    public ENTITY map(EXT_ID extId, DTO dto) {
        Objects.requireNonNull(dto, "dto");
        ENTITY entity = mapper.map(dto, entityClass);
        EXT relation = mapper.referenceById(extId, extClass);
        setRelation(entity, relation);
        return entity;
    }

    /**
     * Maps every DTO and links each entity to the same external entity. The result is a new mutable
     * {@link ArrayList}, so it can be set into an entity collection that Hibernate manages.
     */
    public List<ENTITY> mapAll(EXT_ID extId, Collection<DTO> dtos) {
        return dtos.stream()
                .map(dto -> map(extId, dto))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Links the new entity to the external one, e.g. {@code target.setProject(relation)}. Neither
     * argument is ever {@code null}: {@link #map} rejects a null DTO first.
     */
    protected abstract void setRelation(ENTITY target, EXT relation);
}
