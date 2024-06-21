package by.nhorushko.crudgenerictest.mapper;

import by.nhorushko.crudgeneric.v2.mapper.AbsMapperEntityExtDto;
import by.nhorushko.crudgenerictest.domain.dto.Driver;
import by.nhorushko.crudgenerictest.domain.entity.DriverEntity;
import by.nhorushko.crudgenerictest.domain.entity.UserEntity;
import javax.persistence.EntityManager;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class DriverMapper extends AbsMapperEntityExtDto<DriverEntity, Driver, Long, UserEntity> {

    public DriverMapper(ModelMapper modelMapper, EntityManager entityManager) {
        super(modelMapper, DriverEntity.class, Driver.class, entityManager, UserEntity.class);
    }

    @Override
    protected Driver create(DriverEntity entity) {
        return new Driver(entity.getId(), entity.getName());
    }

    @Override
    protected void mapSpecificFields(Driver source, DriverEntity beforeEntity, DriverEntity destination) {
        destination.setUser(beforeEntity.getUser());
    }

    @Override
    protected void setRelation(UserEntity entity, DriverEntity destination) {
        destination.setUser(entity);
    }
}
