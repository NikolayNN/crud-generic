package by.nhorushko.crudgeneric.flex.mapper;

import by.nhorushko.crudgeneric.flex.AbsMapper;
import by.nhorushko.crudgeneric.flex.model.AbsBaseDto;
import by.nhorushko.crudgeneric.flex.model.AbstractDto;
import by.nhorushko.crudgeneric.flex.model.AbstractEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * The explicit mapping of one entity's CRUD triple: how a create DTO becomes a new entity, how an
 * update DTO is written onto the managed entity, how the entity becomes a read DTO, and how each
 * PATCH body is written onto the managed entity.
 * <p>
 * Nothing is copied implicitly — the entity holds exactly what {@link #toEntity},
 * {@link #updateEntity} and {@link #patches} write. {@code null} has no meaning of its own either:
 * {@code e.setName(dto.getName())} clears the field when the DTO carries {@code null}, and
 * {@code if (dto.getName() != null) e.setName(dto.getName())} keeps it.
 * </p>
 * <p>
 * Registered pairs: {@code CREATE_DTO -> ENTITY} and {@code ENTITY -> READ_DTO} as {@link Mapper}s,
 * {@code UPDATE_DTO -> ENTITY} plus one {@code BODY -> ENTITY} per {@link #patches} entry as
 * {@link Updater}s. The create mapper calls {@code nullifyZeroId()} on the result of
 * {@link #toEntity}: the library rule "id 0 means new".
 * </p>
 */
public abstract class AbsFlexMapConfig<CREATE_DTO extends AbsBaseDto,
                                       UPDATE_DTO extends AbstractDto<?>,
                                       READ_DTO extends AbstractDto<?>,
                                       ENTITY extends AbstractEntity<?>> implements MapperSource {

    protected final AbsMapper mapper;
    private final Class<CREATE_DTO> createDtoClass;
    private final Class<UPDATE_DTO> updateDtoClass;
    private final Class<READ_DTO> readDtoClass;
    private final Class<ENTITY> entityClass;

    public AbsFlexMapConfig(AbsMapper mapper, Class<CREATE_DTO> createDtoClass, Class<UPDATE_DTO> updateDtoClass,
                            Class<READ_DTO> readDtoClass, Class<ENTITY> entityClass) {
        this.mapper = mapper;
        this.createDtoClass = Objects.requireNonNull(createDtoClass, "createDtoClass");
        this.updateDtoClass = Objects.requireNonNull(updateDtoClass, "updateDtoClass");
        this.readDtoClass = Objects.requireNonNull(readDtoClass, "readDtoClass");
        this.entityClass = Objects.requireNonNull(entityClass, "entityClass");
    }

    /**
     * Builds a new entity from the create DTO: {@code new Entity()} plus every field it needs.
     */
    protected abstract ENTITY toEntity(CREATE_DTO dto);

    /**
     * Writes the update DTO onto the managed entity. Only what this method writes changes.
     */
    protected abstract void updateEntity(UPDATE_DTO dto, ENTITY entity);

    /**
     * Builds the read DTO from the entity. Do not hand over the entity's mutable objects — copy a
     * collection ({@code List.copyOf}, or {@code mapper.mapAll} into child DTOs), prefer
     * {@code Instant} to {@code Date}: the DTO is also the "before" snapshot of an update, and a
     * shared object would change along with the entity.
     */
    protected abstract READ_DTO toReadDto(ENTITY entity);

    /**
     * Declares one {@link Updater} per PATCH body class — a partial class without id, applied to the
     * managed entity by {@code AbsFlexServiceRUD.patch(id, body)}. None by default. Called when the
     * registry collects {@link #updaters()}, never from the constructor.
     */
    protected void patches(Patches<ENTITY> p) {
    }

    @Override
    public Collection<Mapper<?, ?>> mappers() {
        return List.<Mapper<?, ?>>of(
                Mapper.of(createDtoClass, entityClass, this::createEntity),
                Mapper.of(entityClass, readDtoClass, this::toReadDto));
    }

    @Override
    public Collection<Updater<?, ?>> updaters() {
        Patches<ENTITY> patches = new Patches<>(entityClass);
        patches(patches);
        List<Updater<?, ?>> result = new ArrayList<>();
        result.add(Updater.of(updateDtoClass, entityClass, this::updateEntity));
        result.addAll(patches.updaters());
        return result;
    }

    private ENTITY createEntity(CREATE_DTO dto) {
        ENTITY entity = toEntity(dto);
        if (entity != null) {
            entity.nullifyZeroId();
        }
        return entity;
    }
}
