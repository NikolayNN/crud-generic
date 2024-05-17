package by.nhorushko.crudgeneric.flex.mapper.core;

/**
 * An interface for mappers that require registration.
 * <p>
 * Implementing this interface allows mappers to be automatically registered by the framework,
 * ensuring their mapping configurations are correctly initialized before the application starts.
 * This is particularly useful for setting up custom mappings that need to be explicitly registered
 * with ModelMapper to take effect.
 * </p>
 */
public interface RegisterableMapper {

    /**
     * Registers the mapper's configuration.
     * <p>
     * This method is responsible for registering the mapper's specific configuration, such as
     * custom type maps, with ModelMapper. Implementations should define the mapping logic within
     * this method to ensure it is properly set up during application startup.
     * </p>
     */

    void register();
}
