package by.nhorushko.crudgeneric.flex.mapper;

import by.nhorushko.crudgeneric.flex.AbsMapper;
import by.nhorushko.crudgeneric.flex.model.AbstractDto;
import by.nhorushko.crudgeneric.flex.model.AbstractEntity;

/**
 * Base for an explicit entity → DTO mapper: a read DTO of a read-only service, a view DTO, a DTO
 * with final fields built through its constructor. Implement {@link #create}.
 * <p>
 * {@link #fromClass()}, {@link #toClass()} and {@link #map} are deliberately not final: a CGLIB
 * proxy of the bean must be able to delegate them to the target.
 * </p>
 */
public abstract class AbsMapEntityToDto<ENTITY extends AbstractEntity<?>, DTO extends AbstractDto<?>>
        implements Mapper<ENTITY, DTO> {

    protected final AbsMapper mapper;
    protected final Class<ENTITY> entityClass;
    protected final Class<DTO> dtoClass;

    public AbsMapEntityToDto(AbsMapper mapper, Class<ENTITY> entityClass, Class<DTO> dtoClass) {
        this.mapper = mapper;
        this.entityClass = entityClass;
        this.dtoClass = dtoClass;
    }

    /**
     * Builds the DTO from the entity. Do not hand over the entity's mutable objects — copy a
     * collection ({@code List.copyOf}, or {@code mapper.mapAll} into child DTOs), prefer
     * {@code Instant} to {@code Date}: a read DTO is also the "before" snapshot of an update, and a
     * shared object would change along with the entity.
     */
    protected abstract DTO create(ENTITY from);

    @Override
    public Class<ENTITY> fromClass() {
        return entityClass;
    }

    @Override
    public Class<DTO> toClass() {
        return dtoClass;
    }

    @Override
    public DTO map(ENTITY from) {
        return create(from);
    }
}
