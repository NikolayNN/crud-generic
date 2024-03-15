package by.nhorushko.crudgeneric.flex.mapper.core;

import by.nhorushko.crudgeneric.flex.AbsModelMapper;
import org.modelmapper.TypeMap;

/**
 * Provides a simplified mapping configuration for straightforward object transformations.
 * <p>
 * This abstract class is designed to facilitate direct mapping between two types of objects
 * without the need for additional logic or custom field mappings. It is particularly suited for
 * scenarios where the source and destination objects have fields of identical names and compatible types,
 * allowing for an automatic and seamless mapping process.
 * </p>
 * <p>
 * The {@code AbsMapSimple} class leverages {@link AbsModelMapper} for the mapping process,
 * creating a type map between the specified source ({@code FROM}) and destination ({@code TO}) classes upon
 * instantiation. This preconfigured mapping behavior simplifies the setup required to convert between
 * common DTOs and entity models or between any two classes with matching field patterns.
 * </p>
 *
 * @param <FROM> the source object class from which data is mapped
 * @param <TO>   the destination object class to which data is mapped
 */
public abstract class AbsMapBasic<FROM, TO> implements RegisterableMapper {
    protected final Class<FROM> fromClass;
    protected final Class<TO> toClass;
    protected final AbsModelMapper mapper;

    public AbsMapBasic(AbsModelMapper mapper, Class<FROM> dtoClass, Class<TO> entityClass) {
        this.mapper = mapper;
        this.fromClass = dtoClass;
        this.toClass = entityClass;
    }

    @Override
    public void register() {
        initMapper();
    }

    private void initMapper() {
        TypeMap<FROM, TO> typeMap = configureMapper();
        customizeTypeMap(typeMap);
    }

    /**
     * Configures the ModelMapper to create a type map between the source and destination classes.
     * <p>
     * This method is automatically called during the instantiation of {@code AbsMapSimple} and
     * ensures that a basic mapping configuration is established in the ModelMapper. This configuration
     * is ideal for direct field-to-field mapping between {@code FROM} and {@code TO} objects with
     * matching field names and types.
     * </p>
     *
     * @return The {@link TypeMap} object representing the mapping configuration established between
     * the source and destination types. This object can be further customized if needed.
     */
    protected TypeMap<FROM, TO> configureMapper() {
        return mapper.getModelMapper().createTypeMap(fromClass, toClass);
    }

    /**
     * Offers a hook for additional customizations of the type map between source and destination classes.
     * <p>
     * This method is called after the initial type map configuration established by {@link #configureMapper()}.
     * It provides an opportunity to apply further customizations to the {@link TypeMap} object, such as specifying
     * custom converters, conditionals, or property mappings that are not covered by the basic field-to-field mapping.
     * </p>
     * <p>
     * Override this method in subclasses to implement specific mapping customizations required by your application's
     * data transformation logic. This flexibility allows for precise control over how data is mapped from the source
     * to the destination, accommodating complex scenarios beyond straightforward field name matches.
     * </p>
     * <p>
     * For example, you might use this method to add custom converters that handle complex data types, transformations
     * that involve logic beyond mere copying, or conditional mappings that only apply under certain circumstances.
     * </p>
     *
     * @param typeMap The {@link TypeMap} object that was created by {@link #configureMapper()} and represents the
     *                mapping configuration between the source ({@code FROM}) and destination ({@code TO}) classes.
     *                This type map can be further customized within this method.
     */
    protected void customizeTypeMap(TypeMap<FROM, TO> typeMap) {
    }
}
