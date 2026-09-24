package by.nhorushko.crudgeneric.flex.service;

import by.nhorushko.crudgeneric.flex.AbsMapper;
import by.nhorushko.crudgeneric.flex.exception.AppNotFoundException;
import by.nhorushko.crudgeneric.flex.exception.MappingNotFoundException;
import by.nhorushko.crudgeneric.flex.model.AbsUpdateDto;
import by.nhorushko.crudgeneric.flex.model.AbstractDto;
import by.nhorushko.crudgeneric.flex.model.AbstractEntity;
import by.nhorushko.crudgeneric.flex.model.IdEntity;
import lombok.Getter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import static java.lang.String.format;

/**
 * Abstract service class providing read, update, and delete operations for entities.
 * <p>
 * Extends {@link AbsFlexServiceR} with three write paths — {@link #update}, {@link #patch} and
 * {@link #changeEntity} — and {@link #delete}. Every write path loads the managed entity through
 * {@link #loadForUpdate}, changes it in place explicitly, and stores it through {@link #saveUpdated};
 * override those two seams rather than the write methods. A check that must guard every write path
 * belongs in {@link #loadForUpdate}: {@link #beforeUpdateHook(AbsUpdateDto)} runs for {@code update}
 * only.
 * </p>
 *
 * @param <ENTITY_ID>  the type of the entity's identifier
 * @param <ENTITY>     the entity type that extends {@link AbstractEntity}
 * @param <READ_DTO>   the DTO type used for read operations, extending {@link AbstractDto}
 * @param <UPDATE_DTO> the DTO type used for update operations, extending {@link AbsUpdateDto}
 * @param <REPOSITORY> the repository type for the entity, extending {@link JpaRepository}
 */
public abstract class AbsFlexServiceRUD<
        ENTITY_ID,
        ENTITY extends AbstractEntity<ENTITY_ID>,
        READ_DTO extends AbstractDto<ENTITY_ID>,
        UPDATE_DTO extends AbsUpdateDto<ENTITY_ID>,
        REPOSITORY extends JpaRepository<ENTITY, ENTITY_ID>>
        extends AbsFlexServiceR<ENTITY_ID, ENTITY, READ_DTO, REPOSITORY> {

    @Getter
    protected final Class<UPDATE_DTO> updateDtoClass;

    /** The service's own DTO classes by kind; {@link #patch} rejects them as bodies. */
    private final Map<Class<?>, String> ownDtoKinds = new HashMap<>();

    public AbsFlexServiceRUD(AbsMapper mapper, REPOSITORY repository,
                             Class<ENTITY> entityClass, Class<READ_DTO> readDtoClass, Class<UPDATE_DTO> updateDtoClass) {
        this(mapper, repository, entityClass, readDtoClass, updateDtoClass, null);
    }

    /**
     * For services that also create entities: {@code createDtoClass} is rejected as a PATCH body,
     * like the read and update DTOs.
     */
    protected AbsFlexServiceRUD(AbsMapper mapper, REPOSITORY repository, Class<ENTITY> entityClass,
                                Class<READ_DTO> readDtoClass, Class<UPDATE_DTO> updateDtoClass,
                                Class<?> createDtoClass) {
        super(mapper, repository, entityClass, readDtoClass);
        this.updateDtoClass = updateDtoClass;
        ownDtoKinds.put(readDtoClass, "read");
        ownDtoKinds.put(updateDtoClass, "update");
        if (createDtoClass != null) {
            ownDtoKinds.put(createDtoClass, "create");
        }
    }

    /**
     * Updates the entity identified by the DTO's id.
     * <p>
     * The managed entity is loaded with {@link #loadForUpdate}, the {@code Updater} registered for
     * {@code UPDATE_DTO -> ENTITY} writes the DTO onto it, and {@link #saveUpdated} stores it. Only
     * what the updater writes changes; a {@code null} in the DTO clears a field only if the updater
     * writes that field as is.
     * </p>
     *
     * @param dto the DTO with the entity's id and the new values
     * @return the stored entity as a READ_DTO
     * @throws IllegalArgumentException if the DTO has no id ({@code null} or {@code 0})
     * @throws AppNotFoundException     if no entity with that id exists
     * @throws MappingNotFoundException if no updater is registered for the DTO's exact class
     */
    public READ_DTO update(UPDATE_DTO dto) {
        checkId(dto);
        beforeUpdateHook(dto);
        return runUpdate(dto.getId(), dto);
    }

    /**
     * Applies a PATCH body to the entity with the given id.
     * <p>
     * The body is a partial class without id — the id comes from the path. The {@code Updater}
     * registered for {@code body.getClass() -> ENTITY}, declared in the entity mapping config's
     * {@code patches()}, writes it onto the managed entity. Runs {@link #beforePatchHook}, the
     * {@link AbsUpdateChangesHookable} hooks and {@link #afterUpdateHook}, but not
     * {@link #beforeUpdateHook(AbsUpdateDto)}.
     * </p>
     * <p>
     * The service's own read, update and create DTOs are not PATCH bodies (exact class, like every
     * registry key): the update DTO would otherwise pass its updater without {@code checkId} and
     * {@code beforeUpdateHook}. Use {@link #update} for it.
     * </p>
     *
     * @param id   the id of the entity to change
     * @param body the PATCH body
     * @return the stored entity as a READ_DTO
     * @throws NullPointerException     if {@code id} or {@code body} is {@code null}
     * @throws IllegalArgumentException if {@code body} is the service's own read, update or create DTO
     * @throws AppNotFoundException     if no entity with that id exists
     * @throws MappingNotFoundException if no updater is registered for the body's exact class
     */
    public READ_DTO patch(ENTITY_ID id, Object body) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(body, "body");
        rejectOwnDto(body);
        beforePatchHook(id, body);
        return runUpdate(id, body);
    }

    private void rejectOwnDto(Object body) {
        String kind = ownDtoKinds.get(body.getClass());
        if (kind != null) {
            throw new IllegalArgumentException(format(
                    "PATCH body %s is the %s DTO of this service; patch takes a partial class declared in patches()",
                    body.getClass().getName(), kind));
        }
    }

    /**
     * Applies a change written in code to the entity with the given id: loads it with
     * {@link #loadForUpdate}, runs {@code change} on it, stores it with {@link #saveUpdated}.
     * <p>
     * There is no request body, so the hooks that take one — {@link #beforeUpdateHook(AbsUpdateDto)},
     * {@link #beforePatchHook} and {@link AbsUpdateChangesHookable#beforeUpdateHook} — do not run.
     * {@link #afterUpdateHook} and {@link AbsUpdateChangesHookable#afterUpdateHook} do; the previous
     * state for the latter is taken before {@code change} runs.
     * </p>
     *
     * @param id     the id of the entity to change
     * @param change the change, applied to the managed entity
     * @return the stored entity as a READ_DTO
     * @throws AppNotFoundException if no entity with that id exists
     */
    protected READ_DTO changeEntity(ENTITY_ID id, Consumer<ENTITY> change) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(change, "change");
        ENTITY entity = loadForUpdate(id);
        Optional<READ_DTO> previous = snapshotIfHookable(entity);
        change.accept(entity);
        return saveAndRunAfterHooks(entity, previous);
    }

    /**
     * Loads the managed entity that every write path changes. Override for a fetch join, or for a
     * check that must guard {@code update}, {@code patch} and {@code changeEntity} alike.
     *
     * @throws AppNotFoundException if no entity with that id exists
     */
    protected ENTITY loadForUpdate(ENTITY_ID id) {
        return repository.findById(id)
                .orElseThrow(() -> new AppNotFoundException(format("Entity id: %s was not found", id)));
    }

    /**
     * Stores the changed entity on every write path. Override with {@code saveAndFlush} when the
     * response must carry values the database computes.
     */
    protected ENTITY saveUpdated(ENTITY entity) {
        return repository.save(entity);
    }

    @SuppressWarnings("unchecked")
    private READ_DTO runUpdate(ENTITY_ID id, Object body) {
        ENTITY entity = loadForUpdate(id);
        Optional<READ_DTO> previous = snapshotIfHookable(entity);
        previous.ifPresent(p -> ((AbsUpdateChangesHookable<ENTITY_ID, READ_DTO>) this).beforeUpdateHook(p, body));
        mapper.update(body, entity);
        return saveAndRunAfterHooks(entity, previous);
    }

    /**
     * The state before the write, taken from the row {@link #loadForUpdate} returned — the same on
     * every write path, and only for services that implement {@link AbsUpdateChangesHookable}.
     */
    private Optional<READ_DTO> snapshotIfHookable(ENTITY entity) {
        return this instanceof AbsUpdateChangesHookable
                ? Optional.of(mapReadDto(entity))
                : Optional.empty();
    }

    @SuppressWarnings("unchecked")
    private READ_DTO saveAndRunAfterHooks(ENTITY entity, Optional<READ_DTO> previous) {
        READ_DTO current = mapReadDto(saveUpdated(entity));
        afterUpdateHook(current);
        previous.ifPresent(p -> ((AbsUpdateChangesHookable<ENTITY_ID, READ_DTO>) this).afterUpdateHook(p, current));
        return current;
    }

    /**
     * Hook called by {@link #update} before anything is loaded. Not called by {@code patch} or
     * {@code changeEntity}: a check for every write path belongs in {@link #loadForUpdate}.
     *
     * @param dto the update DTO about to be applied
     */
    protected void beforeUpdateHook(UPDATE_DTO dto) {
    }

    /**
     * Hook called by {@link #patch} before anything is loaded.
     *
     * @param id   the id of the entity about to be changed
     * @param body the PATCH body about to be applied
     */
    protected void beforePatchHook(ENTITY_ID id, Object body) {
    }

    /**
     * Hook called after every write path, with the stored state.
     *
     * @param dto the stored entity as a READ_DTO
     */
    protected void afterUpdateHook(READ_DTO dto) {
    }

    private void checkId(IdEntity<ENTITY_ID> entity) {
        if (entity.isNew()) {
            throw new IllegalArgumentException(
                    format("Updated entity: %s should have id: (not null OR 0), but was id: %s", entity.getClass(), entity.getId()));
        }
    }

    /**
     * Deletes an entity by its ID.
     * <p>
     * This method removes the entity with the specified ID from the repository, effectively deleting it from the system.
     * The operation is idempotent: when no entity with the given id exists, the call is a silent no-op.
     * Hooks are provided for executing logic before and after the deletion; they run only when the entity exists.
     * </p>
     *
     * @param id the ID of the entity to delete
     */
    public void delete(ENTITY_ID id) {
        if (!repository.existsById(id)) {
            return;
        }
        beforeDeleteHook(id);
        repository.deleteById(id);
        afterDeleteHook(id);
    }

    /**
     * Hook method called before an existing entity is deleted.
     * <p>
     * Override this method in subclasses to implement custom logic to be executed before deleting an entity.
     * </p>
     *
     * @param id the ID of the entity about to be deleted
     */
    protected void beforeDeleteHook(ENTITY_ID id) {
    }

    /**
     * Hook method called after an entity is deleted.
     * <p>
     * Override this method in subclasses to implement custom logic to be executed after deleting an entity.
     * </p>
     *
     * @param id the ID of the deleted entity
     */
    protected void afterDeleteHook(ENTITY_ID id) {
    }
}
