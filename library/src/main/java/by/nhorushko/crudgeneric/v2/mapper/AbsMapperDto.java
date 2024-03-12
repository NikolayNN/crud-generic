package by.nhorushko.crudgeneric.v2.mapper;

import by.nhorushko.crudgeneric.flex.model.AbstractBaseDto;
import by.nhorushko.crudgeneric.v2.domain.AbstractEntity;
import org.modelmapper.AbstractCondition;
import org.modelmapper.ModelMapper;
import org.modelmapper.spi.MappingContext;

import java.util.Collection;
import java.util.List;

public abstract class AbsMapperDto<ENTITY extends AbstractEntity<?>, DTO extends AbstractBaseDto>
        extends AbsMapperBase<ENTITY, DTO> {

    public AbsMapperDto(ModelMapper modelMapper, Class<ENTITY> entityClass, Class<DTO> dtoClass) {
        super(modelMapper, entityClass, dtoClass);
        configureMapper();
    }

    public DTO toDto(ENTITY from) {
        return map(from, dtoClass);
    }

    public List<DTO> toDtos(Collection<ENTITY> entities) {
        return mapAll(entities, dtoClass);
    }

    protected abstract DTO create(ENTITY from);

    @SuppressWarnings("unchecked")
    private void configureMapper() {
        modelMapper
                .createTypeMap(entityClass, dtoClass)
                .setCondition(new AbstractCondition<>() {
                    @Override
                    public boolean applies(MappingContext<Object, Object> context) {
                        return true;
                    }
                })
                .setConverter(context -> create(context.getSource()));
    }
}
