package by.nhorushko.crudgeneric.flex.service;

import by.nhorushko.crudgeneric.flex.AbsModelMapper;
import by.nhorushko.crudgeneric.flex.model.AbsCreateDto;
import by.nhorushko.crudgeneric.flex.model.AbsUpdateDto;
import by.nhorushko.crudgeneric.v2.domain.AbstractDto;
import by.nhorushko.crudgeneric.v2.domain.AbstractEntity;
import lombok.Getter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

/**
 * Abstract service class providing create, read, update, and delete (CRUD) operations for entities.
 * <p>
 * This abstract class extends {@link AbsFlexServiceRUD} by adding support for creating entities. It is
 * designed to work with entities that can be uniquely identified by an ID and their corresponding
 * Data Transfer Objects (DTOs) for create, read, and update operations. The class leverages a
 * repository for direct entity manipulation and a custom mapper for converting between entities
 * and DTOs.
 * </p>
 * <p>
 * The class is generic and can be utilized with any entity type that extends {@link AbstractEntity},
 * along with read DTOs extending {@link AbstractDto}, update DTOs extending {@link AbsUpdateDto},
 * and create DTOs extending {@link AbsCreateDto}. This setup ensures flexible and type-safe
 * handling of entities across various CRUD operations.
 * </p>
 *
 * @param <ENTITY_ID>   the type of the entity's identifier
 * @param <ENTITY>      the entity type that extends {@link AbstractEntity}
 * @param <READ_DTO>    the DTO type used for read operations, extending {@link AbstractDto}
 * @param <UPDATE_DTO>  the DTO type used for update operations, extending {@link AbsUpdateDto}
 * @param <CREATE_DTO>  the DTO type used for create operations, extending {@link AbsCreateDto}
 * @param <REPOSITORY>  the repository type for the entity, extending {@link JpaRepository}
 */
public abstract class AbsFlexServiceCRUD<
        ENTITY_ID,
        ENTITY extends AbstractEntity<ENTITY_ID>,
        READ_DTO extends AbstractDto<ENTITY_ID>,
        UPDATE_DTO extends AbsUpdateDto<ENTITY_ID>,
        CREATE_DTO extends AbsCreateDto,
        REPOSITORY extends JpaRepository<ENTITY, ENTITY_ID>>
        extends AbsFlexServiceRUD<ENTITY_ID, ENTITY, READ_DTO, UPDATE_DTO, REPOSITORY> {

    @Getter
    protected final Class<CREATE_DTO> createDtoClass;

    public AbsFlexServiceCRUD(AbsModelMapper mapper, REPOSITORY repository, Class<ENTITY> entityClass, Class<READ_DTO> readDtoClass, Class<UPDATE_DTO> updateDtoClass, Class<CREATE_DTO> createDtoClass) {
        super(mapper, repository, entityClass, readDtoClass, updateDtoClass);
        this.createDtoClass = createDtoClass;
    }

    /**
     * Saves a new entity to the repository based on the provided DTO.
     * <p>
     * This method maps the create DTO to an entity, persists it using the repository, and then
     * returns the persisted entity mapped back to a read DTO. Hooks are provided for executing
     * logic before and after the save operation.
     * </p>
     *
     * @param dto create DTO containing the data for the new entity
     * @return the persisted entity as a READ_DTO
     */
    public READ_DTO save(CREATE_DTO dto) {
        beforeSaveHook(dto);
        ENTITY entity = repository.save(mapEntity(dto));
        READ_DTO actual = mapReadDto(entity);
        afterSaveHook(actual);
        return actual;
    }

    /**
     * Hook method called before a new entity is saved.
     * <p>
     * Override this method in subclasses to implement custom pre-save logic. This could include validation,
     * preprocessing of data, or setting default values.
     * </p>
     *
     * @param dto the DTO containing the data for the new entity
     */
    protected void beforeSaveHook(CREATE_DTO dto) {
    }

    /**
     * Hook method called after a new entity is saved.
     * <p>
     * Override this method in subclasses to implement custom post-save logic, such as logging, events firing,
     * or further processing of the saved entity.
     * </p>
     *
     * @param dto the saved entity represented as a READ_DTO
     */
    protected void afterSaveHook(READ_DTO dto) {
    }

    /**
     * Saves a collection of new entities to the repository based on the provided collection of DTOs.
     * <p>
     * This method processes each DTO in the collection, converting them into entities, which are then
     * persisted. After saving, the entities are mapped back to their corresponding READ_DTOs. Hooks are
     * provided to execute logic before and after saving the entire collection.
     * </p>
     *
     * @param dtos the collection of create DTOs for the new entities
     * @return a list of the persisted entities represented as READ_DTOs
     */
    public List<READ_DTO> saveAll(Collection<CREATE_DTO> dtos) {
        beforeSaveAllHook(dtos);
        List<ENTITY> entities = repository.saveAll(mapAllEntities(dtos));
        List<READ_DTO> actual = mapAllReadDto(entities);
        afterSaveAllHook(actual);
        return actual;
    }

    /**
     * Hook method called before a collection of new entities is saved.
     * <p>
     * Override this method in subclasses to implement custom logic to be executed before saving a collection
     * of entities. This can include bulk validation or preprocessing of data.
     * </p>
     *
     * @param dtos the collection of DTOs containing the data for the new entities
     */
    protected void beforeSaveAllHook(Collection<CREATE_DTO> dtos) {
    }

    /**
     * Hook method called after a collection of new entities is saved.
     * <p>
     * Override this method in subclasses to implement custom logic to be executed after saving a collection
     * of entities. This could involve post-processing of the saved entities or related actions such as
     * caching or notifying other parts of the application.
     * </p>
     *
     * @param dtos the collection of saved entities represented as READ_DTOs
     */
    protected void afterSaveAllHook(Collection<READ_DTO> dtos) {
    }
}
