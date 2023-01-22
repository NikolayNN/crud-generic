package by.nhorushko.crudgeneric.v2.mapper;

import by.nhorushko.crudgeneric.v2.domain.AbstractDto;
import by.nhorushko.crudgeneric.v2.domain.AbstractEntity;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;

import javax.persistence.EntityManager;
import javax.swing.text.html.parser.Entity;
import java.util.Collection;
import java.util.List;

public abstract class AbsMapperEntityDto<ENTITY extends AbstractEntity<?>, DTO extends AbstractDto<?>>
        extends AbsMapperDto<ENTITY, DTO> {

    protected final EntityManager entityManager;

    public AbsMapperEntityDto(ModelMapper modelMapper, EntityManager entityManager, Class<ENTITY> entityClass, Class<DTO> dtoClass) {
        super(modelMapper, entityClass, dtoClass);
        this.entityManager = entityManager;
        this.configureMapper();
    }

    public ENTITY toEntity(DTO dto) {
        return map(dto, entityClass);
    }

    public List<ENTITY> toEntities(Collection<DTO> dtos) {
        return mapAll(dtos, entityClass);
    }

    protected void mapSpecificFields(DTO source, ENTITY destination) {
    }

    /**
     * calls only for exists entities
     */
    protected void mapSpecificFields(DTO source, ENTITY beforeEntity, ENTITY destination) {
    }

    private void configureMapper() {
        modelMapper.createTypeMap(dtoClass, entityClass)
                .setPostConverter(createConverterDtoToEntity());
    }

    protected Converter<DTO, ENTITY> createConverterDtoToEntity() {
        return context -> {
            DTO source = context.getSource();
            ENTITY destination = context.getDestination();
            mapSpecificFields(source, destination);
            if (!source.isNew()) {
                ENTITY beforeEntity = entityManager.getReference(entityClass, source.getId());
                mapSpecificFields(source, beforeEntity, destination);
            }
            return destination;
        };
    }
}
