package by.nhorushko.crudgeneric.flex.mapper;

import by.nhorushko.crudgeneric.flex.AbsDtoModelMapper;
import by.nhorushko.crudgeneric.flex.model.AbsBaseDto;
import by.nhorushko.crudgeneric.v2.domain.AbstractEntity;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;

/**
 * Абстрактный маппер для объектов DTO, не содержащих ID и, следовательно, не существующих в БД.
 * Применяется как супер класс для DTO, предназначенных для создания новых сущностей.
 */
public abstract class AbsMappingConfigAbstractBaseDto<DTO extends AbsBaseDto, ENTITY extends AbstractEntity<?>> {

    protected final Class<DTO> dtoClass;
    protected final Class<ENTITY> entityClass;
    protected final AbsDtoModelMapper mapper;

    public AbsMappingConfigAbstractBaseDto(AbsDtoModelMapper mapper, Class<DTO> dtoClass, Class<ENTITY> entityClass) {
        this.mapper = mapper;
        this.dtoClass = dtoClass;
        this.entityClass = entityClass;
        this.configureMapper();
    }

    protected void mapSpecificFields(DTO source, ENTITY destination) {
    }

    private void configureMapper() {
        mapper.getModelMapper().createTypeMap(dtoClass, entityClass)
                .setPostConverter(createConverterDtoToEntity());
        this.configureAdditionalMappings(mapper.getModelMapper());
    }

    protected Converter<DTO, ENTITY> createConverterDtoToEntity() {
        return context -> {
            DTO source = context.getSource();
            ENTITY destination = context.getDestination();
            mapSpecificFields(source, destination);
            destination = handleAfterMapSpecificFields(source, destination);
            return destination;
        };
    }

    protected ENTITY handleAfterMapSpecificFields(DTO source, ENTITY destination) {
        return destination;
    }

    protected void configureAdditionalMappings(ModelMapper modelMapper) {
        // По умолчанию не добавляет дополнительные настройки.
        // Переопределите в подклассах для дополнительной кастомизации маппинга.
    }
}
