package by.nhorushko.crudgeneric.flex.model;

/**
 * Interface marking a POJO as an Entity with an ID.
 * <p>
 * see also {@link IdEntity}
 */
public interface AbstractEntity<ID> extends IdEntity<ID> {
    void setId(ID id);

    /**
     * Normalizes a numeric zero id to {@code null}.
     * <p>
     * Spring Data JPA / Hibernate treat a new entity as one whose id is {@code null}. When an entity arrives
     * with a numeric id of {@code 0}, persistence routes the save through {@code merge} (with a redundant
     * SELECT, and incorrect behaviour for {@code @Version} or non-IDENTITY strategies) instead of {@code persist}.
     * This method aligns the entity with the {@link IdEntity#isNew()} semantics by setting a numeric zero id to
     * {@code null}, so a clean insert is performed. Non-numeric ids and real ids are left untouched.
     * </p>
     */
    default void nullifyZeroId() {
        ID id = getId();
        if (id instanceof Number && ((Number) id).longValue() == 0L) {
            setId(null);
        }
    }
}
