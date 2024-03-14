package by.nhorushko.crudgeneric.flex.config;

import by.nhorushko.crudgeneric.flex.service.AbsFlexServiceCRUD;
import by.nhorushko.crudgeneric.flex.service.AbsFlexServiceExtCRUD;
import by.nhorushko.crudgeneric.flex.service.AbsFlexServiceR;
import by.nhorushko.crudgeneric.flex.service.AbsFlexServiceRUD;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.context.SmartLifecycle;

import java.util.Collection;

@RequiredArgsConstructor
public class AbsTypeMapChecker implements SmartLifecycle {

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
            Class<?> entityClass = service.getEntityClass();
            Class<?> readDtoClass = service.getReadDtoClass();
            checkTypeMap(entityClass, readDtoClass);
            checkTypeMap(readDtoClass, entityClass);
            if (service instanceof AbsFlexServiceRUD) {
                Class<?> updateDtoClass = ((AbsFlexServiceRUD<?, ?, ?, ?, ?>) service).getUpdateDtoClass();
                checkTypeMap(updateDtoClass, entityClass);
            }
            if (service instanceof AbsFlexServiceCRUD) {
                Class<?> createDtoClass = ((AbsFlexServiceCRUD<?, ?, ?, ?, ?, ?>) service).getCreateDtoClass();
                checkTypeMap(createDtoClass, entityClass);
            }
            if (service instanceof AbsFlexServiceExtCRUD) {
                Class<?> createDtoClass = ((AbsFlexServiceExtCRUD<?, ?, ?, ?, ?, ?, ?, ?>) service).getCreateDtoClass();
                checkTypeMap(createDtoClass, entityClass);
            }
        }
    }

    protected void checkTypeMap(Class<?> sourceType, Class<?> destinationType) {
        var typeMap = modelMapper.getTypeMap(sourceType, destinationType);
        if (typeMap == null) {
            throw new UnsupportedOperationException(String.format("TypeMap for mapping %s -> %s is not exists", sourceType.getSimpleName(), destinationType.getSimpleName()));
        }
    }
}
