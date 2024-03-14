package by.nhorushko.crudgeneric.flex.config;

import by.nhorushko.crudgeneric.flex.mapper.core.RegisterableMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.SmartLifecycle;

import java.util.Collection;

/**
 * Manages the registration of all {@link RegisterableMapper} instances within the application.
 * <p>
 * This class implements {@link SmartLifecycle}, enabling it to control the registration process
 * during the application's startup phase. By doing so, it ensures that all custom mappings are
 * registered and ready for use before the application becomes fully operational.
 * </p>
 */
@RequiredArgsConstructor
public class AbsMapperRegister implements SmartLifecycle {

    private final Collection<? extends RegisterableMapper> mappers;

    private boolean isRunning = false;

    /**
     * Initiates the registration of all mappers.
     * <p>
     * On application startup, this method is called to iterate through and register each mapper
     * by invoking their {@code register} method. This bulk registration approach simplifies the
     * initialization of custom mapping configurations.
     * </p>
     */
    @Override
    public void start() {
        registerAllMappers(mappers);
        isRunning = true;
    }

    /**
     * Registers each mapper in the provided collection.
     * <p>
     * This helper method calls the {@code register} method on each {@link RegisterableMapper}
     * instance, applying their configuration to ModelMapper.
     * </p>
     *
     * @param mappers The collection of mappers to register.
     */
    private void registerAllMappers(Collection<? extends RegisterableMapper> mappers) {
        mappers.forEach(RegisterableMapper::register);
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
}
