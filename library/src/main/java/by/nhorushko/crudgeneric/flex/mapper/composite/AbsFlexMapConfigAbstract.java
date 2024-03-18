package by.nhorushko.crudgeneric.flex.mapper.composite;

import by.nhorushko.crudgeneric.flex.AbsModelMapper;
import by.nhorushko.crudgeneric.flex.mapper.AbsMapEntityToDto;
import by.nhorushko.crudgeneric.flex.mapper.core.AbsMapBaseDtoToEntity;
import by.nhorushko.crudgeneric.flex.mapper.core.AbsMapBasic;
import by.nhorushko.crudgeneric.flex.model.AbsBaseDto;
import by.nhorushko.crudgeneric.v2.domain.AbstractDto;
import by.nhorushko.crudgeneric.v2.domain.AbstractEntity;
import org.modelmapper.TypeMap;
import org.modelmapper.builder.ConfigurableConditionExpression;
import org.springframework.context.annotation.Bean;

/**
 * Base configuration class for establishing mappings between various types of Data Transfer Objects (DTOs) and entities.
 * <p>
 * This abstract class serves as a foundation for defining the mappings between create, update, read DTOs, and their corresponding
 * entities. It leverages {@link AbsModelMapper} for the actual mapping process. The class defines methods to create and configure
 * the necessary mapper instances for different types of DTO-to-entity and entity-to-DTO conversions. Implementations must provide
 * specific mapping logic by overriding the protected abstract methods.
 * </p>
 *
 * @param <CREATE_DTO> The type of the DTO used for creating entities, extending {@link AbsBaseDto}.
 * @param <UPDATE_DTO> The type of the DTO used for updating entities, extending {@link AbstractDto}.
 * @param <READ_DTO>   The type of the DTO used for reading entities, extending {@link AbstractDto}.
 * @param <ENTITY>     The type of the entity being mapped to or from.
 */
public abstract class AbsFlexMapConfigAbstract<CREATE_DTO extends AbsBaseDto, UPDATE_DTO extends AbstractDto<?>, READ_DTO extends AbstractDto<?>, ENTITY extends AbstractEntity<?>> {

    private final AbsModelMapper mapper;
    private final Class<CREATE_DTO> createDtoClass;
    private final Class<UPDATE_DTO> updateDtoClass;
    private final Class<READ_DTO> readDtoClass;
    private final Class<ENTITY> entityClass;

    public AbsFlexMapConfigAbstract(AbsModelMapper mapper, Class<CREATE_DTO> createDtoClass, Class<UPDATE_DTO> updateDtoClass, Class<READ_DTO> readDtoClass, Class<ENTITY> entityClass) {
        this.mapper = mapper;
        this.createDtoClass = createDtoClass;
        this.updateDtoClass = updateDtoClass;
        this.readDtoClass = readDtoClass;
        this.entityClass = entityClass;

        mapperCreateDtoToEntity();
        mapperUpdateDtoToEntity();
        mapperReadDtoToEntity();
        mapperEntityToReadDto();
        mapperEntityToEntity();
    }

    /**
     * Creates and configures a mapper for converting create DTOs to entities.
     * <p>
     * Implementations should override this method to define the specific mapping behavior for creating
     * new entities from create DTOs, addressing any custom mapping requirements.
     * </p>
     *
     * @return A configured instance of {@link AbsMapBaseDtoToEntity} for create DTO to entity conversion.
     */
    public AbsMapBasic<CREATE_DTO, ENTITY> mapperCreateDtoToEntity() {
        return mapperCreateDtoToEntity(this.mapper, createDtoClass, entityClass);
    }

    protected abstract AbsMapBasic<CREATE_DTO, ENTITY> mapperCreateDtoToEntity(AbsModelMapper mapper, Class<CREATE_DTO> createDtoClass, Class<ENTITY> entityClass);

    /**
     * Creates and configures a mapper for converting update DTOs to entities.
     * <p>
     * Implementations should override this method to define how update DTOs map to existing entities,
     * particularly handling partial updates and ensuring data integrity.
     * </p>
     *
     * @return A configured instance of {@link AbsMapBaseDtoToEntity} for update DTO to entity conversion.
     */
    public AbsMapBasic<UPDATE_DTO, ENTITY> mapperUpdateDtoToEntity() {
        return mapperUpdateDtoToEntity(this.mapper, updateDtoClass, entityClass);
    }

    protected abstract AbsMapBasic<UPDATE_DTO, ENTITY> mapperUpdateDtoToEntity(AbsModelMapper mapper, Class<UPDATE_DTO> updateDtoClass, Class<ENTITY> entityClass);

    /**
     * Creates and configures a mapper for converting read DTOs to entities.
     * <p>
     * Implementations should override this method to define the specific behavior for mapping
     * from read DTOs to entities, potentially including custom field mappings and post-processing logic.
     * </p>
     *
     * @return A configured instance of {@link AbsMapBaseDtoToEntity} for read DTO to entity conversion.
     */
    public AbsMapBasic<READ_DTO, ENTITY> mapperReadDtoToEntity() {
        return mapperReadDtoToEntity(this.mapper, readDtoClass, entityClass);
    }

    protected abstract AbsMapBasic<READ_DTO, ENTITY> mapperReadDtoToEntity(AbsModelMapper mapper, Class<READ_DTO> readDtoClass, Class<ENTITY> entityClass);

    /**
     * Creates and configures a mapper for converting entities to read DTOs.
     * <p>
     * This method should be overridden to specify the conversion process from entities back to read DTOs,
     * facilitating the preparation of data for presentation or further processing.
     * </p>
     *
     * @return A configured instance of {@link AbsMapEntityToDto} for entity to read DTO conversion.
     */
    public AbsMapBasic<ENTITY, READ_DTO> mapperEntityToReadDto() {
        return mapperEntityToReadDto(this.mapper, entityClass, readDtoClass);
    }

    protected abstract AbsMapBasic<ENTITY, READ_DTO> mapperEntityToReadDto(AbsModelMapper mapper, Class<ENTITY> entityClass, Class<READ_DTO> readDtoClass);

    /**
     * Provides a self-mapping configuration for entities.
     * <p>
     * This method facilitates the creation of a self-mapping instance for entities, typically used for
     * cloning or copying properties within the same entity class. It should be overridden when a custom
     * configuration is needed for the self-mapping process, allowing for modifications or specific handling
     * during the entity to entity conversion. This can be particularly useful for creating deep copies of entities
     * or applying certain transformations without altering the original entity.
     * </p>
     * <p>
     * The provided implementation leverages {@link AbsMapBasic} to create a direct mapping between the same entity
     * class, effectively serving as a utility for entity duplication or property transfer within entities of the same type.
     * </p>
     *
     * @return An instance of {@link AbsMapBasic} configured for self-mapping of the entity class.
     */
    public AbsMapBasic<ENTITY, ENTITY> mapperEntityToEntity() {
        return mapperEntityToEntity(this.mapper, entityClass);
    }

    protected AbsMapBasic<ENTITY, ENTITY> mapperEntityToEntity(AbsModelMapper mapper, Class<ENTITY> entityClass) {
        return new AbsMapBasic<>(mapper, entityClass, entityClass) {
        };
    }
}
