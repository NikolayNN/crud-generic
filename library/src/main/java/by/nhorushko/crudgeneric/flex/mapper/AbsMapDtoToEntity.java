package by.nhorushko.crudgeneric.flex.mapper;

import by.nhorushko.crudgeneric.flex.AbsMapper;
import by.nhorushko.crudgeneric.flex.model.AbsBaseDto;
import by.nhorushko.crudgeneric.flex.model.AbstractEntity;

/**
 * Base for an explicit DTO → new entity mapper, typically for nested children mapped from the
 * parent's {@code toEntity} with {@code mapper.mapAll(...)}. Implement {@link #create}; {@link #map}
 * then normalises the sentinel id {@code 0} to {@code null}, so a new child is inserted.
 * <p>
 * {@link #fromClass()}, {@link #toClass()} and {@link #map} are deliberately not final: a CGLIB
 * proxy of the bean must be able to delegate them to the target.
 * </p>
 */
public abstract class AbsMapDtoToEntity<DTO extends AbsBaseDto, ENTITY extends AbstractEntity<?>>
        implements Mapper<DTO, ENTITY> {

    protected final AbsMapper mapper;
    protected final Class<DTO> dtoClass;
    protected final Class<ENTITY> entityClass;

    public AbsMapDtoToEntity(AbsMapper mapper, Class<DTO> dtoClass, Class<ENTITY> entityClass) {
        this.mapper = mapper;
        this.dtoClass = dtoClass;
        this.entityClass = entityClass;
    }

    /**
     * Builds a new entity from the DTO.
     */
    protected abstract ENTITY create(DTO from);

    @Override
    public Class<DTO> fromClass() {
        return dtoClass;
    }

    @Override
    public Class<ENTITY> toClass() {
        return entityClass;
    }

    @Override
    public ENTITY map(DTO from) {
        ENTITY entity = create(from);
        if (entity != null) {
            entity.nullifyZeroId();
        }
        return entity;
    }
}
