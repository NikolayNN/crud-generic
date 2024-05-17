package by.nhorushko.crudgeneric.v2.domain;

/**
 * Interface marking a POJO as an Entity with an ID.
 * <p>
 * see also {@link IdEntity}
 */
public interface AbstractEntity<ID> extends IdEntity<ID> {
    void setId(ID id);
}
