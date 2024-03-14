package by.nhorushko.crudgeneric.flex.mapper;

import by.nhorushko.crudgeneric.flex.AbsDtoModelMapper;
import by.nhorushko.crudgeneric.flex.mapper.core.AbsMapBaseDtoToEntity;
import by.nhorushko.crudgeneric.flex.model.AbsBaseDto;
import by.nhorushko.crudgeneric.v2.domain.AbstractEntity;

/**
 * Abstract mapper for converting create DTOs into new entity instances.
 * <p>
 * This class extends {@link AbsMapBaseDtoToEntity} specifically for the use case of mapping DTOs,
 * which extend {@link AbsBaseDto} and do not contain an ID, to new instances of entities. It is tailored
 * for scenarios where a new entity is being created from the data provided by a DTO. The class leverages
 * the generic mapping infrastructure provided by its superclass to perform the conversion while allowing
 * for customization of the mapping process through overriding specific methods in subclasses.
 * </p>
 *
 * @param <DTO>    the type of the Data Transfer Object, extending {@link AbsBaseDto}, used for creating new entities
 * @param <ENTITY> the type of the entity, extending {@link AbstractEntity}, that is to be created from the DTO
 */
public abstract class AbsMapCreateDtoToEntity<DTO extends AbsBaseDto, ENTITY extends AbstractEntity<?>> extends AbsMapBaseDtoToEntity<DTO, ENTITY> {
    public AbsMapCreateDtoToEntity(AbsDtoModelMapper mapper, Class<DTO> dtoClass, Class<ENTITY> entityClass) {
        super(mapper, dtoClass, entityClass);
    }
}
