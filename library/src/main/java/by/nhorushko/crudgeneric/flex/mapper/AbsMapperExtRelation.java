package by.nhorushko.crudgeneric.flex.mapper;

import by.nhorushko.crudgeneric.flex.AbsDtoModelMapper;
import by.nhorushko.crudgeneric.flex.model.AbstractCreateDto;
import by.nhorushko.crudgeneric.v2.domain.AbstractEntity;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public abstract class AbsMapperExtRelation<DTO extends AbstractCreateDto, ENTITY, EXT_ID, EXT extends AbstractEntity<?>> {

    private final AbsDtoModelMapper mapper;
    private final Class<ENTITY> entityClass;
    private final Class<EXT> extClass;

    public AbsMapperExtRelation(AbsDtoModelMapper mapper, Class<ENTITY> entityClass, Class<EXT> extClass) {
        this.mapper = mapper;
        this.entityClass = entityClass;
        this.extClass = extClass;
    }

    public ENTITY map(EXT_ID extId, DTO dto) {
        ENTITY entity = mapper.map(dto, entityClass);
        EXT relation = mapper.referenceById(extId, extClass);
        setRelation(entity, relation);
        return entity;
    }

    public List<ENTITY> mapAll(EXT_ID extId, Collection<DTO> dtos) {
        return dtos.stream()
                .map(dto -> map(extId, dto))
                .collect(Collectors.toList());
    }

    /**
     * Находит первое поле с типом EXT и устанавливает туда значение
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
