package by.nhorushko.crudgeneric.flex.mapper.core;

import by.nhorushko.crudgeneric.flex.AbsDtoModelMapper;

/**
 * Provides a simplified mapping configuration for straightforward object transformations.
 * <p>
 * This abstract class is designed to facilitate direct mapping between two types of objects
 * without the need for additional logic or custom field mappings. It is particularly suited for
 * scenarios where the source and destination objects have fields of identical names and compatible types,
 * allowing for an automatic and seamless mapping process.
 * </p>
 * <p>
 * The {@code AbsMapSimple} class leverages {@link AbsDtoModelMapper} for the mapping process,
 * creating a type map between the specified source ({@code FROM}) and destination ({@code TO}) classes upon
 * instantiation. This preconfigured mapping behavior simplifies the setup required to convert between
 * common DTOs and entity models or between any two classes with matching field patterns.
 * </p>
 *
 * @param <FROM> the source object class from which data is mapped
 * @param <TO>   the destination object class to which data is mapped
 */
public abstract class AbsMapSimple<FROM, TO> implements RegisterableMapper {
    protected final Class<FROM> fromClass;
    protected final Class<TO> toClass;
    protected final AbsDtoModelMapper mapper;

    public AbsMapSimple(AbsDtoModelMapper mapper, Class<FROM> dtoClass, Class<TO> entityClass) {
        this.mapper = mapper;
        this.fromClass = dtoClass;
        this.toClass = entityClass;
    }

    @Override
    public void register() {
        configureMapper();
    }

    /**
     * Configures the ModelMapper to create a type map between the source and destination classes.
     * <p>
     * This method is automatically called during the instantiation of {@code AbsMapSimple} and
     * ensures that a basic mapping configuration is established in the ModelMapper. This configuration
     * is ideal for direct field-to-field mapping between {@code FROM} and {@code TO} objects with
     * matching field names and types.
     * </p>
     */
    protected void configureMapper() {
        mapper.getModelMapper().createTypeMap(fromClass, toClass);
    }
}
