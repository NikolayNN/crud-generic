package by.nhorushko.crudgeneric.flex.mapper;

import by.nhorushko.crudgeneric.flex.AbsDtoModelMapper;
import by.nhorushko.crudgeneric.v2.domain.AbstractDto;
import by.nhorushko.crudgeneric.v2.domain.AbstractEntity;

/**
 * Абстрактный маппер для объектов DTO, не содержащих ID и, следовательно, не существующих в БД.
 * Применяется как супер класс для DTO, предназначенных для создания новых сущностей.
 */
public abstract class AbsMappingConfigAbstractDto<DTO extends AbstractDto<?>, ENTITY extends AbstractEntity<?>> extends AbsMappingConfigAbstractBaseDto<DTO, ENTITY> {

    public AbsMappingConfigAbstractDto(AbsDtoModelMapper mapper, Class<DTO> dtoClass, Class<ENTITY> entityClass) {
        super(mapper, dtoClass, entityClass);
    }
}
