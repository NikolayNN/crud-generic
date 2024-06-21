package by.nhorushko.crudgenerictest.mapper;

import by.nhorushko.crudgeneric.v2.mapper.AbsMapperEntityDto;
import by.nhorushko.crudgenerictest.domain.dto.User;
import by.nhorushko.crudgenerictest.domain.entity.UserEntity;
import javax.persistence.EntityManager;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class UserMapper extends AbsMapperEntityDto<UserEntity, User> {
    public UserMapper(ModelMapper modelMapper, EntityManager entityManager) {
        super(modelMapper, entityManager,  UserEntity.class, User.class);
    }

    @Override
    protected User create(UserEntity entity) {
        return new User(entity.getId(), entity.getName());
    }
}
