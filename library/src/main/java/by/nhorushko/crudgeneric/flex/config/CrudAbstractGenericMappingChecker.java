package by.nhorushko.crudgeneric.flex.config;

import by.nhorushko.crudgeneric.flex.service.AbsFlexServiceCRUD;
import by.nhorushko.crudgeneric.flex.service.AbsFlexServiceExtCRUD;
import by.nhorushko.crudgeneric.flex.service.AbsFlexServiceR;
import by.nhorushko.crudgeneric.flex.service.AbsFlexServiceRUD;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.modelmapper.ModelMapper;
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.SmartLifecycle;

import java.util.Collection;

@RequiredArgsConstructor
public class CrudAbstractGenericMappingChecker implements SmartLifecycle {

    private final Collection<? extends AbsFlexServiceR<?, ?, ?, ?>> services;
    private final ModelMapper modelMapper;

    private boolean isRunning = false;

    @Override
    public void start() {
        checkMappers();
        isRunning = true;
    }

    @Override
    public void stop() {
        isRunning = false;
    }

    @Override
    public boolean isRunning() {
        return this.isRunning;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }

    public void checkMappers() {
        for (AbsFlexServiceR<?, ?, ?, ?> service : services) {
            Class<?> entityClass = getClassValue(AbsFlexServiceR.Fields.entityClass, service);
            Class<?> readDtoClass = getClassValue(AbsFlexServiceR.Fields.readDtoClass, service);
            checkTypeMap(entityClass, readDtoClass);
            checkTypeMap(readDtoClass, entityClass);
            if (service instanceof AbsFlexServiceRUD) {
                Class<?> updateDtoClass = getClassValue(AbsFlexServiceRUD.Fields.updateDtoClass, service);
                checkTypeMap(updateDtoClass, entityClass);
            }
            if (service instanceof AbsFlexServiceCRUD) {
                Class<?> createDtoClass = getClassValue(AbsFlexServiceCRUD.Fields.createDtoClass, service);
                checkTypeMap(createDtoClass, entityClass);
            }
            if (service instanceof AbsFlexServiceExtCRUD) {
                Class<?> createDtoClass = getClassValue(AbsFlexServiceExtCRUD.Fields.createDtoClass, service);
                checkTypeMap(createDtoClass, entityClass);
            }
        }
    }

    @SneakyThrows
    protected Class<?> getClassValue(String fieldName, Object service) {
        return (Class<?>) FieldUtils.readField(service, fieldName, true);
    }

    protected void checkTypeMap(Class<?> sourceType, Class<?> destinationType) {
        var typeMap = modelMapper.getTypeMap(sourceType, destinationType);
        if (typeMap == null) {
            throw new UnsupportedOperationException(String.format("TypeMap for mapping %s -> %s is not exists", sourceType.getSimpleName(), destinationType.getSimpleName()));
        }
    }
}
