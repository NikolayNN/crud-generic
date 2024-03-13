package by.nhorushko.crudgeneric.flex.mapper;

import by.nhorushko.crudgeneric.flex.AbsEntityModelMapper;
import by.nhorushko.crudgeneric.v2.domain.AbstractDto;
import by.nhorushko.crudgeneric.v2.domain.AbstractEntity;
import org.modelmapper.AbstractCondition;
import org.modelmapper.spi.MappingContext;

public abstract class AbsMappingConfigEntity<ENTITY extends AbstractEntity<?>, DTO extends AbstractDto<?>> {
    private final AbsEntityModelMapper mapper;
    private final Class<ENTITY> entityClass;
    private final Class<DTO> dtoClass;

    public AbsMappingConfigEntity(AbsEntityModelMapper mapper, Class<ENTITY> entityClass, Class<DTO> dtoClass) {
        this.mapper = mapper;
        this.entityClass = entityClass;
        this.dtoClass = dtoClass;
        this.configureMapper();
    }

    protected abstract DTO create(ENTITY from);

    @SuppressWarnings("unchecked")
    private void configureMapper() {
        mapper.getModelMapper()
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
