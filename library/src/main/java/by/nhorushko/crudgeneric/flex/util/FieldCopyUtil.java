package by.nhorushko.crudgeneric.flex.util;

import org.apache.commons.lang3.reflect.FieldUtils;

import java.lang.reflect.Field;
import java.util.Set;

public class FieldCopyUtil {

    public static Object copy(Object source, Object target, Set<String> ignore) {
        Field[] sourceFields = source.getClass().getDeclaredFields();
        for (Field sourceField : sourceFields) {
            if (ignore.contains(sourceField.getName())) {
                continue;
            }
            Object sourceValue = getValue(sourceField, source);
            Field targetField = getFieldByName(target.getClass(), sourceField.getName());
            copyField(targetField, sourceValue, target);
        }
        return target;
    }

    private static Object getValue(Field field, Object object) {
        try {
            return FieldUtils.readField(field, object, true);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static Field getFieldByName(Class clazz, String fieldName) {
        return FieldUtils.getField(clazz, fieldName, true);
    }

    private static void copyField(Field targetField, Object value, Object target) {
        try {
            FieldUtils.writeField(targetField, target, value, true);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
