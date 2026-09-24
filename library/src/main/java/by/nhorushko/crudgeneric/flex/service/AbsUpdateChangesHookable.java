package by.nhorushko.crudgeneric.flex.service;

/**
 * Hooks that see the state of an entity before and after a write. A service that implements this
 * interface gets them on every write path of {@link AbsFlexServiceRUD}; {@code beforeUpdateHook}
 * only on the paths that carry a request body ({@code update} and {@code patch}).
 * <p>
 * The state before the write is taken from the row {@code loadForUpdate} returned, after it
 * returned: a check in {@code loadForUpdate} runs first, so these hooks never see a rejected row.
 * </p>
 *
 * @param <ENTITY_ID> the entity id type
 * @param <READ_DTO>  the read DTO the states are represented as
 */
public interface AbsUpdateChangesHookable<ENTITY_ID, READ_DTO> {

    /**
     * Called before the body is applied.
     *
     * @param previous the stored state before the write
     * @param current  the request body about to be applied: the update DTO for {@code update}, the
     *                 PATCH body for {@code patch}
     */
    void beforeUpdateHook(READ_DTO previous, Object current);

    /**
     * Called after the write is stored.
     * <p>
     * {@code previous} is mapped from the managed entity before the change, not deep-copied. If the
     * read DTO mapping hands over the entity's mutable objects (a collection, a {@code Date}, an
     * embeddable), a change made to them in place shows up in {@code previous} too, and a diff finds
     * nothing: the mapping must copy them.
     * </p>
     *
     * @param previous the state before the write
     * @param current  the stored state after the write
     */
    void afterUpdateHook(READ_DTO previous, READ_DTO current);
}
