package by.nhorushko.crudgeneric.flex.service;

import by.nhorushko.crudgeneric.flex.AbsDtoModelMapper;
import by.nhorushko.crudgeneric.flex.mapper.mapper.AbsMapperExtRelation;
import by.nhorushko.crudgeneric.flex.model.AbsCreateDto;
import by.nhorushko.crudgeneric.flex.model.AbsUpdateDto;
import by.nhorushko.crudgeneric.v2.domain.AbstractDto;
import by.nhorushko.crudgeneric.v2.domain.AbstractEntity;
import lombok.Getter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

/**
 * Abstract service class providing extended CRUD (Create, Read, Update, Delete) operations for entities,
 * including handling of relationships to another entity.
 * <p>
 * This class extends {@link AbsFlexServiceRUD} by incorporating additional functionality to manage
 * entities in relation to another specified entity type {@code EXT}. It leverages a custom mapper
 * {@link AbsMapperExtRelation} for handling the mapping of DTOs to entities considering the relationship.
 * </p>
 * <p>
 * It is designed for use cases where entities are closely related and operations on an entity might require
 * consideration of its relation to another entity. For example, creating or updating entities that are
 * dependent on or linked to another entity.
 * </p>
 *
 * @param <ENTITY_ID>   the type of the identifier for the primary entity
 * @param <ENTITY>      the primary entity type, extending {@link AbstractEntity}
 * @param <READ_DTO>    the DTO type for read operations, extending {@link AbstractDto}
 * @param <UPDATE_DTO>  the DTO type for update operations, extending {@link AbsUpdateDto}
 * @param <CREATE_DTO>  the DTO type for create operations, extending {@link AbsCreateDto}
 * @param <REPOSITORY>  the repository type for the primary entity, extending {@link JpaRepository}
 * @param <EXT_ID>      the type of the identifier for the related entity
 * @param <EXT>         the related entity type, extending {@link AbstractEntity}
 */
public abstract class AbsFlexServiceExtCRUD<
        ENTITY_ID,
        ENTITY extends AbstractEntity<ENTITY_ID>,
        READ_DTO extends AbstractDto<ENTITY_ID>,
        UPDATE_DTO extends AbsUpdateDto<ENTITY_ID>,
        CREATE_DTO extends AbsCreateDto,
        REPOSITORY extends JpaRepository<ENTITY, ENTITY_ID>,
        EXT_ID,
        EXT extends AbstractEntity<EXT_ID>>
        extends AbsFlexServiceRUD<ENTITY_ID, ENTITY, READ_DTO, UPDATE_DTO, REPOSITORY> {
    protected final AbsMapperExtRelation<CREATE_DTO, ENTITY, EXT_ID, EXT> extMapper;

    @Getter
    protected final Class<CREATE_DTO> createDtoClass;


    public AbsFlexServiceExtCRUD(AbsDtoModelMapper mapper,
                                 REPOSITORY repository,
                                 Class<ENTITY> entityClass,
                                 Class<READ_DTO> readDtoClass,
                                 Class<UPDATE_DTO> updateDtoClass,
                                 Class<CREATE_DTO> createDtoClass,
                                 AbsMapperExtRelation<CREATE_DTO, ENTITY, EXT_ID, EXT> extMapper) {
        super(mapper, repository, entityClass, readDtoClass, updateDtoClass);
        this.extMapper = extMapper;
        this.createDtoClass = createDtoClass;
    }

    /**
     * Saves a new entity related to a specified entity, based on the provided DTO.
     * <p>
     * This method maps the create DTO to an entity in the context of a relationship with another entity
     * identified by {@code relationId}, then saves the new entity. Hooks are provided for executing logic
     * before and after the save operation.
     * </p>
     *
     * @param relationId the identifier of the related entity
     * @param dto        the create DTO containing data for the new entity
     * @return the saved entity represented as a READ_DTO
     */
    public READ_DTO save(EXT_ID relationId, CREATE_DTO dto) {
        beforeSaveHook(relationId, dto);
        ENTITY entity = extMapper.map(relationId, dto);
        entity = repository.save(entity);
        READ_DTO actual = mapReadDto(entity);
        afterSaveHook(relationId, actual);
        return actual;
    }

    /**
     * Hook method called before a new entity, related to another specified entity, is saved.
     * <p>
     * This method provides a hook for implementing custom logic before the save operation of a new entity
     * that has a relationship with another entity identified by {@code relationId}. It can be used to
     * perform validations, set default values, or modify the DTO before it is mapped to the entity.
     * </p>
     *
     * @param relationId the identifier of the related entity to which the new entity is associated
     * @param dto        the DTO containing the data for the new entity
     */
    protected void beforeSaveHook(EXT_ID relationId, CREATE_DTO dto) {
    }

    /**
     * Hook method called after a new entity, related to another specified entity, is saved and mapped to a DTO.
     * <p>
     * This method serves as a hook for executing custom logic after an entity has been saved and mapped to a
     * READ_DTO. This could involve additional processing of the saved entity, such as logging, triggering
     * events, or further data manipulation based on the entity's relationship to another entity identified
     * by {@code relationId}.
     * </p>
     *
     * @param relationId the identifier of the related entity to which the saved entity is associated
     * @param actual     the READ_DTO representation of the saved entity
     */
    protected void afterSaveHook(EXT_ID relationId, READ_DTO actual) {
    }

    /**
     * Saves a collection of new entities related to a specified entity, based on the provided collection of DTOs.
     * <p>
     * This method processes each DTO in the collection, mapping them to entities in the context of a relationship
     * with another entity identified by {@code relationId}, then saves the new entities. Hooks are provided to
     * execute logic before and after saving the entire collection.
     * </p>
     *
     * @param relationId the identifier of the related entity
     * @param dtos       the collection of create DTOs for the new entities
     * @return a list of the saved entities represented as READ_DTOs
     */
    public List<READ_DTO> saveAll(EXT_ID relationId, Collection<CREATE_DTO> dtos) {
        List<ENTITY> entities = extMapper.mapAll(relationId, dtos);
        entities.forEach(e -> {
            if (!e.isNew()) throw new IllegalArgumentException(wrongIdMessage(e.getId()));
        });
        beforeSaveAllHook(relationId, dtos);
        entities = repository.saveAll(entities);
        List<READ_DTO> actual = mapAllReadDto(entities);
        afterSaveAllHook(relationId, dtos);
        return actual;
    }

    /**
     * Hook method called before a collection of new entities, each related to another specified entity, are saved.
     * <p>
     * This method provides a mechanism for implementing custom logic before the save operation of a collection
     * of new entities associated with another entity identified by {@code relationId}. It can be particularly
     * useful for performing bulk validations, preprocessing of the collection of DTOs, or modifying the DTOs
     * before they are mapped to entities. This pre-save processing can take into account the specific relationship
     * to the other entity, enabling more complex data integrity checks or default value settings.
     * </p>
     *
     * @param relationId the identifier of the related entity to which each new entity in the collection is associated
     * @param dtos       the collection of DTOs containing the data for the new entities
     */
    protected void beforeSaveAllHook(EXT_ID relationId, Collection<CREATE_DTO> dtos) {
    }

    /**
     * Hook method called after a collection of new entities, each related to another specified entity, are saved and mapped to DTOs.
     * <p>
     * This method serves as a hook for executing custom logic after a collection of entities has been saved and
     * mapped back to their respective READ_DTOs. This could involve additional processing of the saved entities,
     * such as logging, triggering events, or further data manipulation based on their relationship to another
     * entity identified by {@code relationId}. This post-save processing is especially relevant for operations
     * that might affect or be affected by the entity relationship, such as updating related entities or caching.
     * </p>
     *
     * @param relationId the identifier of the related entity to which each saved entity in the collection is associated
     * @param dtos       the collection of DTOs that were used to create the new entities, potentially useful for
     *                   post-save operations that require access to the original data
     */
    protected void afterSaveAllHook(EXT_ID relationId, Collection<CREATE_DTO> dtos) {
    }

    private String wrongIdMessage(ENTITY_ID id) {
        return String.format("wrong id: %s to save new entity id should be null", id);
    }
}
