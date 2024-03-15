package by.nhorushko.crudgeneric.flex.mapper.core;

import by.nhorushko.crudgeneric.flex.AbsModelMapper;
import org.modelmapper.ModelMapper;
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
    public final void register() {
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
    protected final TypeMap<FROM, TO> configureMapper() {
        TypeMap<FROM, TO> typeMap = mapper.getModelMapper().createTypeMap(fromClass, toClass);
        customizeTypeMap(typeMap);
        return typeMap;
    }

    /**
     * Allows further customization of the established type map.
     * <p>
     * Once the basic type map is configured through {@link #configureMapper()}, this method
     * provides a hook for additional customizations. It can be overridden in subclasses to adjust
     * the mapping configuration, for example, by adding custom converters, specifying conditional
     * mappings, or other advanced mapping configurations that {@link ModelMapper} supports.
     * </p>
     * <p>
     * This method is particularly useful for complex mapping scenarios where the default
     * field-to-field mapping strategy needs to be supplemented with more sophisticated
     * customization to accurately reflect the domain logic.
     * </p>
     *
     * @param typeMap The {@link TypeMap} object representing the mapping configuration between
     *                the source and destination types. This object is fully initialized and can
     *                be modified to achieve the desired mapping behavior.
     */
    protected void customizeTypeMap(TypeMap<FROM, TO> typeMap) {
    }
}
