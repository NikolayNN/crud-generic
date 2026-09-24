package by.nhorushko.crudgeneric.flex.mapper;

import by.nhorushko.crudgeneric.flex.AbsMapper;
import by.nhorushko.crudgeneric.flex.model.AbstractDto;
import by.nhorushko.crudgeneric.flex.model.AbstractEntity;

/**
 * Base for an explicit update DTO → managed entity updater. Implement {@link #update}.
 * <p>
 * {@link #fromClass()} and {@link #toClass()} are deliberately not final: a CGLIB proxy of the bean
 * must be able to delegate them to the target.
 * </p>
 */
public abstract class AbsMapUpdateDtoToEntity<DTO extends AbstractDto<?>, ENTITY extends AbstractEntity<?>>
        implements Updater<DTO, ENTITY> {

    protected final AbsMapper mapper;
    protected final Class<DTO> dtoClass;
    protected final Class<ENTITY> entityClass;

    public AbsMapUpdateDtoToEntity(AbsMapper mapper, Class<DTO> dtoClass, Class<ENTITY> entityClass) {
        this.mapper = mapper;
        this.dtoClass = dtoClass;
        this.entityClass = entityClass;
    }

    @Override
    public Class<DTO> fromClass() {
        return dtoClass;
    }

    @Override
    public Class<ENTITY> toClass() {
        return entityClass;
    }

    /**
     * Writes the DTO onto the managed entity. Only what this method writes changes.
     */
    @Override
    public abstract void update(DTO from, ENTITY into);
}
