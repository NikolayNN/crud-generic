package by.nhorushko.crudgeneric.flex.service;

import by.nhorushko.crudgeneric.flex.AbsDtoModelMapper;
import by.nhorushko.crudgeneric.flex.model.AbsUpdateDto;
import by.nhorushko.crudgeneric.util.FieldCopyUtil;
import by.nhorushko.crudgeneric.v2.domain.AbstractDto;
import by.nhorushko.crudgeneric.v2.domain.AbstractEntity;
import by.nhorushko.crudgeneric.v2.domain.IdEntity;
import lombok.Getter;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static java.lang.String.format;

/**
 * Abstract service class providing read, update, and delete operations for entities.
 * <p>
 * This abstract class extends {@link AbsFlexServiceR} by adding support for updating and deleting entities.
 * It is designed to work with entities that can be uniquely identified by an ID, along with corresponding
 * DTOs for read and update operations. The class leverages a repository for direct entity manipulation and
 * a custom mapper for converting between entities and DTOs.
 * </p>
 * <p>
 * The class is generic and can be utilized with any entity type that extends {@link AbstractEntity}, along
 * with read DTOs extending {@link AbstractDto} and update DTOs extending {@link AbsUpdateDto}. This setup
 * ensures flexible and type-safe handling of entities across various operations.
 * </p>
 *
 * @param <ENTITY_ID>   the type of the entity's identifier
 * @param <ENTITY>      the entity type that extends {@link AbstractEntity}
 * @param <READ_DTO>    the DTO type used for read operations, extending {@link AbstractDto}
 * @param <UPDATE_DTO>  the DTO type used for update operations, extending {@link AbsUpdateDto}
 * @param <REPOSITORY>  the repository type for the entity, extending {@link JpaRepository}
 */
public abstract class AbsFlexServiceRUD<
        ENTITY_ID,
        ENTITY extends AbstractEntity<ENTITY_ID>,
        READ_DTO extends AbstractDto<ENTITY_ID>,
        UPDATE_DTO extends AbsUpdateDto<ENTITY_ID>,
        REPOSITORY extends JpaRepository<ENTITY, ENTITY_ID>>
        extends AbsFlexServiceR<ENTITY_ID, ENTITY, READ_DTO, REPOSITORY> {
    protected Set<String> IGNORE_PARTIAL_UPDATE_PROPERTIES = Set.of("id");

    @Getter
    protected final Class<UPDATE_DTO> updateDtoClass;

    public AbsFlexServiceRUD(AbsDtoModelMapper mapper, REPOSITORY repository,
                             Class<ENTITY> entityClass, Class<READ_DTO> readDtoClass, Class<UPDATE_DTO> updateDtoClass) {
        super(mapper, repository, entityClass, readDtoClass);
        this.updateDtoClass = updateDtoClass;
    }

    /**
     * Updates an entity with the given DTO.
     * <p>
     * This method applies full updates to an entity based on the entirety of the provided DTO. It ensures
     * that the entity's ID is present and then maps the DTO to an entity for persistence.
     * </p>
     *
     * @param dto the DTO containing the updated data for the entity
     * @return the updated entity represented as a READ_DTO
     */
    public READ_DTO update(UPDATE_DTO dto) {
        return runUpdate(dto);
    }

    /**
     * Performs a partial update on an entity using the provided partial update object.
     * <p>
     * This method applies a subset of changes to an entity. It retrieves the current state of the entity,
     * applies the changes from the partial update object, and then persists the updated entity.
     * </p>
     *
     * @param id the ID of the entity to update
     * @param partial the object containing the partial updates
     * @return the updated entity represented as a READ_DTO
     */
    public READ_DTO updatePartial(ENTITY_ID id, Object partial) {
        READ_DTO target = copyPartial(id, partial);
        return runUpdate(target);
    }

    private READ_DTO runUpdate(AbstractDto<ENTITY_ID> dto) {
        checkId(dto);
        ENTITY newValue = mapEntity(dto);
        beforeUpdateHook(newValue);
        ENTITY actual = repository.save(newValue);
        READ_DTO actualDto = mapReadDto(actual);
        afterUpdateHook(actualDto);
        return actualDto;
    }

    /**
     * Hook method called before an entity is updated.
     * <p>
     * Override this method in subclasses to implement custom logic to be executed before updating an entity.
     * </p>
     *
     * @param newValue the entity about to be updated
     */
    protected void beforeUpdateHook(ENTITY newValue) {
    }

    /**
     * Hook method called after an entity is updated.
     * <p>
     * Override this method in subclasses to implement custom logic to be executed after updating an entity.
     * </p>
     *
     * @param dto the updated entity represented as a READ_DTO
     */
    protected void afterUpdateHook(READ_DTO dto) {
    }

    private void checkId(IdEntity<ENTITY_ID> entity) {
        if (entity.isNew()) {
            throw new IllegalArgumentException(
                    format("Updated entity: %s should have id: (not null OR 0), but was id: %s", entity.getClass(), entity.getId()));
        }
    }

    private READ_DTO copyPartial(ENTITY_ID id, Object source) {
        READ_DTO target = getById(id);
        FieldCopyUtil.copy(source, target, IGNORE_PARTIAL_UPDATE_PROPERTIES);
        return target;
    }

    /**
     * Deletes an entity by its ID.
     * <p>
     * This method removes the entity with the specified ID from the repository, effectively deleting it from the system.
     * </p>
     *
     * @param id the ID of the entity to delete
     */
    public void delete(ENTITY_ID id) {
        repository.deleteById(id);
    }

    /**
     * Maps an object to an entity of the specified class.
     * <p>
     * This utility method uses the configured mapper to convert a given object to an entity instance, facilitating
     * easy transformation of DTOs or other objects into entities.
     * </p>
     *
     * @param obj the object to map to an entity
     * @return the mapped entity
     */
    protected ENTITY mapEntity(Object obj) {
        return this.mapper.map(obj, entityClass);
    }

    /**
     * Maps a collection of objects to a list of entities.
     * <p>
     * This utility method uses the configured mapper to convert a collection of objects into a list of entities,
     * allowing for bulk transformation of DTOs or other objects into entities.
     * </p>
     *
     * @param obj the collection of objects to map to entities
     * @return the list of mapped entities
     */
    protected List<ENTITY> mapAllEntities(Collection<?> obj) {
        return this.mapper.mapAll(obj, entityClass);
    }
}
