package by.nhorushko.crudgeneric.flex.config;

import by.nhorushko.crudgeneric.flex.service.AbsFlexServiceCRUD;
import by.nhorushko.crudgeneric.flex.service.AbsFlexServiceExtCRUD;
import by.nhorushko.crudgeneric.flex.service.AbsFlexServiceR;
import by.nhorushko.crudgeneric.flex.service.AbsFlexServiceRUD;
import org.modelmapper.ModelMapper;
import org.springframework.context.SmartLifecycle;

import java.util.Collection;

/**
 * A utility class that verifies the existence and correctness of ModelMapper type mappings
 * for all services extending {@link AbsFlexServiceR} and its subclasses at application startup.
 * <p>
 * This class implements {@link SmartLifecycle}, allowing it to perform type mapping checks
 * during the startup phase of the application. It ensures that all required DTO to entity
 * mappings (and vice versa) are correctly configured in ModelMapper for the services used in
 * the application, throwing an exception if any required mapping is missing. This proactive
 * check helps in identifying configuration issues early in the development lifecycle.
 * </p>
 * <p>
 * The check can be disabled by registering an {@link AbsCrudCustomizer} bean with
 * {@code typeMapCheckerEnabled = false}. When disabled, this lifecycle bean still starts
 * but performs no validation.
 * </p>
 */
public class AbsTypeMapChecker implements SmartLifecycle {

    public static final int PHASE_NUMBER = Integer.MAX_VALUE;

    private final Collection<? extends AbsFlexServiceR<?, ?, ?, ?>> services;
    private final ModelMapper modelMapper;
    private final boolean enabled;

    private boolean isRunning = false;

    public AbsTypeMapChecker(Collection<? extends AbsFlexServiceR<?, ?, ?, ?>> services,
                             ModelMapper modelMapper) {
        this(services, modelMapper, true);
    }

    public AbsTypeMapChecker(Collection<? extends AbsFlexServiceR<?, ?, ?, ?>> services,
                             ModelMapper modelMapper,
                             boolean enabled) {
        this.services = services;
        this.modelMapper = modelMapper;
        this.enabled = enabled;
    }

    @Override
    public void start() {
        if (enabled) {
            checkMappers();
        }
        // isRunning must be set after checkMappers(): on validation failure the
        // exception aborts context startup and the bean stays not-running.
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
        return PHASE_NUMBER;
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
