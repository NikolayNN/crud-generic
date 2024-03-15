package by.nhorushko.crudgeneric.flex.mapper.mapper;

import by.nhorushko.crudgeneric.flex.AbsModelMapper;
import by.nhorushko.crudgeneric.flex.model.AbsCreateDto;
import by.nhorushko.crudgeneric.v2.domain.AbstractEntity;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Abstract mapper for creating entities from DTOs and linking them with an external entity.
 * <p>
 * This class provides functionality to map {@link AbsCreateDto} instances to entity objects
 * while establishing a relationship with another specified entity of type {@code EXT}. It leverages
 * {@link AbsModelMapper} for the initial mapping process and then sets up a specific relationship
 * based on the provided external entity ID {@code EXT_ID}.
 * </p>
 * <p>
 * The class is designed to automate the common pattern of creating a new entity from a DTO and associating
 * it with an existing entity in the system, identified by an ID. This is particularly useful in scenarios
 * where the creation of one entity directly relates to another entity's existence, such as setting a foreign
 * key in a database.
 * </p>
 *
 * @param <DTO>    the Data Transfer Object type used for creating new entities, extending {@link AbsCreateDto}
 * @param <ENTITY> the entity type to be created and mapped from the DTO
 * @param <EXT_ID> the type of the identifier for the external entity to which the created entity will be related
 * @param <EXT>    the external entity type with which the created entity will be associated
 */
public abstract class AbsMapperExtRelation<DTO extends AbsCreateDto, ENTITY, EXT_ID, EXT extends AbstractEntity<?>> {

    private final AbsModelMapper mapper;
    private final Class<ENTITY> entityClass;
    private final Class<EXT> extClass;

    public AbsMapperExtRelation(AbsModelMapper mapper, Class<ENTITY> entityClass, Class<EXT> extClass) {
        this.mapper = mapper;
        this.entityClass = entityClass;
        this.extClass = extClass;
    }

    /**
     * Maps a DTO to an entity and associates it with an external entity identified by {@code extId}.
     *
     * @param extId the identifier of the external entity to associate with the created entity
     * @param dto   the DTO from which to create the entity
     * @return the created entity with its relationship to the external entity established
     */
    public ENTITY map(EXT_ID extId, DTO dto) {
        ENTITY entity = mapper.map(dto, entityClass);
        EXT relation = mapper.referenceById(extId, extClass);
        setRelation(entity, relation);
        return entity;
    }

    /**
     * Maps a collection of DTOs to entities and associates each with the same external entity identified by {@code extId}.
     *
     * @param extId the identifier of the external entity to associate with each created entity
     * @param dtos  the collection of DTOs from which to create the entities
     * @return a list of created entities, each with its relationship to the external entity established
     */
    public List<ENTITY> mapAll(EXT_ID extId, Collection<DTO> dtos) {
        return dtos.stream()
                .map(dto -> map(extId, dto))
                .collect(Collectors.toList());
    }

    /**
     * Finds the first field of type {@code EXT} within the target entity and sets the specified relation.
     * <p>
     * This method is designed to automatically detect and set the relationship field within the entity,
     * facilitating the association between the newly created entity and the external entity. It locates
     * the first field that matches the type {@code EXT} and assigns it the provided relation.
     * </p>
     * <p>
     * It is important to note that if the target entity contains more than one field of type {@code EXT}, an
     * exception will be thrown to prevent ambiguous relationship assignments. This behavior ensures that the
     * relationship is only set when there is a clear and unambiguous target field within the entity. If your
     * entity design includes multiple fields of the same type and you need to set a specific one, consider
     * implementing a more targeted approach in your subclass.
     * </p>
     *
     * @param target   the target entity to which the relationship is to be set
     * @param relation the external entity instance to be associated with the target entity
     * @throws IllegalArgumentException if more than one field of type {@code EXT} is found in the target entity,
     *                                  indicating an ambiguous relationship assignment.
     */
    protected void setRelation(ENTITY target, EXT relation) {
        try {
            Field field = findFieldWithType(target, extClass);
            FieldUtils.writeField(field, target, relation, true);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private Field findFieldWithType(Object targetObject, Class<EXT> extFieldType) {
        Field[] allFields = FieldUtils.getAllFields(targetObject.getClass());
        Field foundField = null;
        int count = 0;

        for (Field field : allFields) {
            if (field.getType().equals(extFieldType)) {
                foundField = field;
                count++;
            }
        }

        if (count == 1) {
            return foundField;
        } else if (count > 1) {
            throw new IllegalArgumentException(String.format("Multiple fields of type: %s found in object: %s", extFieldType.getSimpleName(), targetObject.getClass().getSimpleName()));
        } else {
            throw new IllegalArgumentException(String.format("Field with type: %s was not found in object: %s", extFieldType.getSimpleName(), targetObject.getClass().getSimpleName()));
        }
    }
}
