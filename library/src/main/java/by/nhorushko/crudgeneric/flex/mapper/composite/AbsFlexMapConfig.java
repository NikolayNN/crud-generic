package by.nhorushko.crudgeneric.flex.mapper.composite;

import by.nhorushko.crudgeneric.flex.AbsDtoModelMapper;
import by.nhorushko.crudgeneric.flex.mapper.AbsMapEntityToDto;
import by.nhorushko.crudgeneric.flex.mapper.core.AbsMapBaseDtoToEntity;
import by.nhorushko.crudgeneric.flex.mapper.core.AbsMapSimple;
import by.nhorushko.crudgeneric.flex.model.AbsBaseDto;
import by.nhorushko.crudgeneric.v2.domain.AbstractDto;
import by.nhorushko.crudgeneric.v2.domain.AbstractEntity;
import org.springframework.context.annotation.Bean;

/**
 * Base configuration class for establishing mappings between various types of Data Transfer Objects (DTOs) and entities.
 * <p>
 * This abstract class serves as a foundation for defining the mappings between create, update, read DTOs, and their corresponding
 * entities. It leverages {@link AbsDtoModelMapper} for the actual mapping process. The class defines methods to create and configure
 * the necessary mapper instances for different types of DTO-to-entity and entity-to-DTO conversions. Implementations must provide
 * specific mapping logic by overriding the protected abstract methods.
 * </p>
 *
 * @param <CREATE_DTO> The type of the DTO used for creating entities, extending {@link AbsBaseDto}.
 * @param <UPDATE_DTO> The type of the DTO used for updating entities, extending {@link AbstractDto}.
 * @param <READ_DTO>   The type of the DTO used for reading entities, extending {@link AbstractDto}.
 * @param <ENTITY>     The type of the entity being mapped to or from.
 */
public abstract class AbsFlexMapConfig<CREATE_DTO extends AbsBaseDto, UPDATE_DTO extends AbstractDto<?>, READ_DTO extends AbstractDto<?>, ENTITY extends AbstractEntity<?>> {

    private final AbsDtoModelMapper mapper;
    private final Class<CREATE_DTO> createDtoClass;
    private final Class<UPDATE_DTO> updateDtoClass;
    private final Class<READ_DTO> readDtoClass;
    private final Class<ENTITY> entityClass;

    public AbsFlexMapConfig(AbsDtoModelMapper mapper, Class<CREATE_DTO> createDtoClass, Class<UPDATE_DTO> updateDtoClass, Class<READ_DTO> readDtoClass, Class<ENTITY> entityClass) {
        this.mapper = mapper;
        this.createDtoClass = createDtoClass;
        this.updateDtoClass = updateDtoClass;
        this.readDtoClass = readDtoClass;
        this.entityClass = entityClass;
    }

    /**
     * Creates and configures a mapper for converting read DTOs to entities.
     * <p>
     * Implementations should override this method to define the specific behavior for mapping
     * from read DTOs to entities, potentially including custom field mappings and post-processing logic.
     * </p>
     *
     * @return A configured instance of {@link AbsMapBaseDtoToEntity} for read DTO to entity conversion.
     */
    public AbsMapBaseDtoToEntity<READ_DTO, ENTITY> mapperReadDtoToEntity() {
        return mapperReadDtoToEntity(this.mapper, readDtoClass, entityClass);
    }

    protected abstract AbsMapBaseDtoToEntity<READ_DTO, ENTITY> mapperReadDtoToEntity(AbsDtoModelMapper mapper, Class<READ_DTO> readDtoClass, Class<ENTITY> entityClass);

    /**
     * Creates and configures a mapper for converting create DTOs to entities.
     * <p>
     * Implementations should override this method to define the specific mapping behavior for creating
     * new entities from create DTOs, addressing any custom mapping requirements.
     * </p>
     *
     * @return A configured instance of {@link AbsMapBaseDtoToEntity} for create DTO to entity conversion.
     */
    @Bean
    public AbsMapBaseDtoToEntity<CREATE_DTO, ENTITY> mapperCreateDtoToEntity() {
        return mapperCreateDtoToEntity(this.mapper, createDtoClass, entityClass);
    }

    protected abstract AbsMapBaseDtoToEntity<CREATE_DTO, ENTITY> mapperCreateDtoToEntity(AbsDtoModelMapper mapper, Class<CREATE_DTO> createDtoClass, Class<ENTITY> entityClass);

    /**
     * Creates and configures a mapper for converting update DTOs to entities.
     * <p>
     * Implementations should override this method to define how update DTOs map to existing entities,
     * particularly handling partial updates and ensuring data integrity.
     * </p>
     *
     * @return A configured instance of {@link AbsMapBaseDtoToEntity} for update DTO to entity conversion.
     */
    @Bean
    public AbsMapBaseDtoToEntity<UPDATE_DTO, ENTITY> mapperUpdateDtoToEntity() {
        return mapperUpdateDtoToEntity(this.mapper, updateDtoClass, entityClass);
    }

    protected abstract AbsMapBaseDtoToEntity<UPDATE_DTO, ENTITY> mapperUpdateDtoToEntity(AbsDtoModelMapper mapper, Class<UPDATE_DTO> createDtoClass, Class<ENTITY> entityClass);

    /**
     * Creates and configures a mapper for converting entities to read DTOs.
     * <p>
     * This method should be overridden to specify the conversion process from entities back to read DTOs,
     * facilitating the preparation of data for presentation or further processing.
     * </p>
     *
     * @return A configured instance of {@link AbsMapEntityToDto} for entity to read DTO conversion.
     */
    @Bean
    public AbsMapEntityToDto<ENTITY, READ_DTO> mapperEntityToReadDto() {
        return mapperEntityToReadDto(this.mapper, entityClass, readDtoClass);
    }

    protected abstract AbsMapEntityToDto<ENTITY, READ_DTO> mapperEntityToReadDto(AbsDtoModelMapper mapper, Class<ENTITY> entityClass, Class<READ_DTO> readDtoClass);

    /**
     * Creates and configures a mapper for converting entities to read DTOs.
     * <p>
     * This method should be overridden to specify the conversion process from entities back to read DTOs,
     * facilitating the preparation of data for presentation or further processing.
     * </p>
     *
     * @return A configured instance of {@link AbsMapEntityToDto} for entity to read DTO conversion.
     */
    @Bean
    public AbsMapSimple<ENTITY, ENTITY> mapperEntityToEntity() {
        return mapperEntityToEntity(this.mapper, entityClass);
    }

    protected AbsMapSimple<ENTITY, ENTITY> mapperEntityToEntity(AbsDtoModelMapper mapper, Class<ENTITY> entityClass) {
        return new AbsMapSimple<>(mapper, entityClass, entityClass) {
        };
    }
}
