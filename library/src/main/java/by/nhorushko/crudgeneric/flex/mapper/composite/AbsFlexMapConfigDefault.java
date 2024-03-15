package by.nhorushko.crudgeneric.flex.mapper.composite;

import by.nhorushko.crudgeneric.flex.AbsModelMapper;
import by.nhorushko.crudgeneric.flex.mapper.AbsMapCreateDtoToEntity;
import by.nhorushko.crudgeneric.flex.mapper.AbsMapEntityToDto;
import by.nhorushko.crudgeneric.flex.mapper.AbsMapUpdateDtoToEntity;
import by.nhorushko.crudgeneric.flex.mapper.core.AbsMapBasic;
import by.nhorushko.crudgeneric.flex.mapper.core.AbsMapDtoToEntity;
import by.nhorushko.crudgeneric.flex.model.AbsBaseDto;
import by.nhorushko.crudgeneric.v2.domain.AbstractDto;
import by.nhorushko.crudgeneric.v2.domain.AbstractEntity;
import org.modelmapper.TypeMap;


/**
 * Provides a default implementation of the mapping configuration for CRUD operations.
 * <p>
 * This class extends {@link AbsFlexMapConfigAbstract} to offer default mappings between
 * CREATE, UPDATE, and READ DTOs and their corresponding ENTITY classes. It simplifies the
 * setup process by establishing basic mappings and allowing for customization of specific
 * field mappings through abstract methods.
 * </p>
 *
 * @param <CREATE_DTO> the DTO class used for create operations
 * @param <UPDATE_DTO> the DTO class used for update operations
 * @param <READ_DTO>   the DTO class used for read operations
 * @param <ENTITY>     the entity class that DTOs map to and from
 */
public abstract class AbsFlexMapConfigDefault<CREATE_DTO extends AbsBaseDto, UPDATE_DTO extends AbstractDto<?>, READ_DTO extends AbstractDto<?>, ENTITY extends AbstractEntity<?>> extends AbsFlexMapConfigAbstract<CREATE_DTO, UPDATE_DTO, READ_DTO, ENTITY> {
    public AbsFlexMapConfigDefault(AbsModelMapper mapper, Class<CREATE_DTO> createDtoClass, Class<UPDATE_DTO> updateDtoClass, Class<READ_DTO> readDtoClass, Class<ENTITY> entityClass) {
        super(mapper, createDtoClass, updateDtoClass, readDtoClass, entityClass);
    }

    @Override
    protected AbsMapBasic<CREATE_DTO, ENTITY> mapperCreateDtoToEntity(AbsModelMapper mapper, Class<CREATE_DTO> createDtoClass, Class<ENTITY> entityClass) {
        return new AbsMapCreateDtoToEntity<>(mapper, createDtoClass, entityClass) {
            @Override
            protected void mapSpecificFields(CREATE_DTO source, ENTITY destination) {
                mapSpecificFieldsCreateDtoToEntity(mapper, source, destination);
            }

            @Override
            protected void setupTypeMap(TypeMap<CREATE_DTO, ENTITY> typeMap) {
                setupMappingRulesCreateDtoToEntity(typeMap);
            }
        };
    }

    /**
     * Customizes mapping from the CREATE DTO to the corresponding entity.
     * <p>
     * This method is intended to be overridden to define custom mappings that are not automatically
     * handled. It should specify how fields in the CREATE DTO that require special handling are
     * mapped to the corresponding fields in the entity, ensuring that the entity is correctly
     * populated with all necessary information before persisting.
     * </p>
     *
     * @param mapper      The {@link AbsModelMapper} instance facilitating the mapping process.
     * @param source      The CREATE DTO containing the source data.
     * @param destination The target entity to which the data should be mapped.
     */
    protected abstract void mapSpecificFieldsCreateDtoToEntity(AbsModelMapper mapper, CREATE_DTO source, ENTITY destination);

    protected void setupMappingRulesCreateDtoToEntity(TypeMap<CREATE_DTO, ENTITY> typeMap) {
    }

    @Override
    protected AbsMapBasic<UPDATE_DTO, ENTITY> mapperUpdateDtoToEntity(AbsModelMapper mapper, Class<UPDATE_DTO> updateDtoClass, Class<ENTITY> entityClass) {
        return new AbsMapUpdateDtoToEntity<>(mapper, updateDtoClass, entityClass) {
            @Override
            protected void mapSpecificFields(UPDATE_DTO source, ENTITY destination) {
                mapSpecificFieldsUpdateDtoToEntity(mapper, source, destination);
            }

            @Override
            protected void setupTypeMap(TypeMap<UPDATE_DTO, ENTITY> typeMap) {
                setupMappingRulesUpdateDtoToEntity(typeMap);
            }
        };
    }

    /**
     * Defines custom mapping rules from the UPDATE DTO to the entity.
     * <p>
     * Override this method to implement specific field mappings required during the update process.
     * It provides a mechanism to apply complex transformations or business logic to fields that
     * cannot be directly mapped, ensuring that the entity is accurately updated with the data
     * from the UPDATE DTO.
     * </p>
     *
     * @param mapper      The {@link AbsModelMapper} used for the mapping operations.
     * @param source      The UPDATE DTO containing the data for the update.
     * @param destination The entity that needs to be updated with the DTO's data.
     */
    protected abstract void mapSpecificFieldsUpdateDtoToEntity(AbsModelMapper mapper, UPDATE_DTO source, ENTITY destination);

    protected void setupMappingRulesUpdateDtoToEntity(TypeMap<UPDATE_DTO, ENTITY> typeMap) {

    }

    @Override
    protected AbsMapBasic<READ_DTO, ENTITY> mapperReadDtoToEntity(AbsModelMapper mapper, Class<READ_DTO> readDtoClass, Class<ENTITY> entityClass) {
        return new AbsMapDtoToEntity<>(mapper, readDtoClass, entityClass) {
            @Override
            protected void mapSpecificFields(READ_DTO source, ENTITY destination) {
                mapSpecificFieldsReadDtoToEntity(mapper, source, destination);
            }

            @Override
            protected void setupTypeMap(TypeMap<READ_DTO, ENTITY> typeMap) {
                setupMappingRulesReadDtoToEntity(typeMap);
            }
        };
    }

    /**
     * Defines custom mapping rules from the READ DTO to the entity.
     * <p>
     * This abstract method is designed to be overridden in specific mapping configurations to specify how certain
     * fields or properties in the READ DTO should be transformed and assigned to corresponding fields in the entity.
     * It allows developers to implement custom field mapping that cannot be automatically mapped between the READ DTO
     * and the entity, ensuring precise adherence to business logic during data transfer.
     * </p>
     *
     * @param mapper      The instance of {@link AbsModelMapper} used for mapping.
     * @param source      The source READ DTO from which data is being mapped.
     * @param destination The entity to which data from the DTO is mapped.
     */
    protected abstract void mapSpecificFieldsReadDtoToEntity(AbsModelMapper mapper, READ_DTO source, ENTITY destination);

    protected void setupMappingRulesReadDtoToEntity(TypeMap<READ_DTO, ENTITY> typeMap) {

    }

    @Override
    protected AbsMapBasic<ENTITY, READ_DTO> mapperEntityToReadDto(AbsModelMapper mapper, Class<ENTITY> entityClass, Class<READ_DTO> readDtoClass) {
        return new AbsMapEntityToDto<>(mapper, entityClass, readDtoClass) {
            @Override
            protected READ_DTO create(ENTITY entity) {
                return createReadDtoFromEntity(mapper, entity);
            }

        };
    }

    /**
     * Constructs a READ DTO instance from an entity.
     * <p>
     * This method is designed to be overridden to provide a custom implementation for converting
     * an entity back into its corresponding READ DTO. It should encapsulate the logic necessary
     * for translating the entity's data into the form expected by the DTO, including handling
     * of complex data structures or relationships.
     * </p>
     *
     * @param mapper The {@link AbsModelMapper} instance used for any required sub-mappings.
     * @param entity The entity from which to create the READ DTO.
     * @return A fully constructed READ DTO corresponding to the given entity.
     */
    protected abstract READ_DTO createReadDtoFromEntity(AbsModelMapper mapper, ENTITY entity);
}
