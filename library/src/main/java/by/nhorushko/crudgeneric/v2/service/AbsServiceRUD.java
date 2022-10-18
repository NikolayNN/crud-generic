package by.nhorushko.crudgeneric.v2.service;

import by.nhorushko.crudgeneric.exception.AppNotFoundException;
import by.nhorushko.crudgeneric.util.FieldCopyUtil;
import by.nhorushko.crudgeneric.v2.domain.AbstractDto;
import by.nhorushko.crudgeneric.v2.domain.AbstractEntity;
import by.nhorushko.crudgeneric.v2.mapper.AbsMapperDtoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Set;

import static java.lang.String.format;

/**
 * Read Update Delete service
 */
public abstract class AbsServiceRUD<
        ENTITY_ID,
        ENTITY extends AbstractEntity<ENTITY_ID>,
        DTO extends AbstractDto<ENTITY_ID>,
        MAPPER extends AbsMapperDtoEntity<ENTITY, DTO>,
        REPOSITORY extends JpaRepository<ENTITY, ENTITY_ID>>

        extends AbsServiceR<ENTITY_ID, ENTITY, DTO, MAPPER, REPOSITORY> {

    protected Set<String> IGNORE_PARTIAL_UPDATE_PROPERTIES = Set.of("id");

    public AbsServiceRUD(MAPPER mapper, REPOSITORY repository) {
        super(mapper, repository);
    }

    public DTO update(DTO dto) {
        checkId(dto.getId());
        final ENTITY entity = super.mapper.revMap(dto);
        final ENTITY updatedEntity = super.repository.save(entity);
        return this.mapper.map(updatedEntity);
    }

    private void checkId(ENTITY_ID id) {
        if (this.isIdNotDefined(id)) {
            throw new IllegalArgumentException(
                    format("Updated entity should have id: (not null OR 0), but was id: %s", id));
        }
    }

    public DTO updatePartial(ENTITY_ID id, Object source) {
        checkId(id);
        ENTITY entity = super.repository.findById(id)
                .orElseThrow(() -> new AppNotFoundException(
                        format("Partial update operation is impossible because of not existing entity with id = '%s'.",
                                id)));
        FieldCopyUtil.copy(source, entity, IGNORE_PARTIAL_UPDATE_PROPERTIES);
        return this.mapper.map(entity);
    }

    public void delete(ENTITY_ID id) {
        super.repository.deleteById(id);
    }

    private boolean isIdNotDefined(ENTITY_ID id) {
        if (id == null) {
            return true;
        }
        if (id instanceof Number) {
            return ((Number) id).longValue() == 0;
        }
        return false;
    }
}