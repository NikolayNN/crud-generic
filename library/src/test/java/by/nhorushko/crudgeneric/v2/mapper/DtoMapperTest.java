package by.nhorushko.crudgeneric.v2.mapper;

import by.nhorushko.crudgeneric.domain.dto.Car;
import by.nhorushko.crudgeneric.domain.dto.User;
import by.nhorushko.crudgeneric.domain.entity.CarEntity;
import by.nhorushko.crudgeneric.domain.entity.UserEntity;
import org.junit.Test;
import org.modelmapper.ModelMapper;

import static org.junit.Assert.assertEquals;

public final class DtoMapperTest {
    private final AbsMapperDto<UserEntity, User> userMapper;

    public DtoMapperTest() {
        final ModelMapper modelMapper = new ModelMapper();
        final AbsMapperDto<CarEntity, Car> carMapper = new CarAbsMapperDto(modelMapper);
        this.userMapper = new UserAbsMapperDto(modelMapper, carMapper);
    }

    @Test
    public void userEntityShouldBeMappedToDto() {
        final UserEntity givenEntity = new UserEntity(255L, "email@mail.ru", "name", "surname",
                "patronymic", new CarEntity(256L, "number"));

        final User actual = this.userMapper.create(givenEntity);
        final User expected = new User(255L, "email@mail.ru", "name", "surname",
                "patronymic", new Car(256L, "number"));

        assertEquals(expected, actual);
    }

    private static final class CarAbsMapperDto extends AbsMapperDto<CarEntity, Car> {

        public CarAbsMapperDto(ModelMapper modelMapper) {
            super(modelMapper, CarEntity.class, Car.class);
        }

        @Override
        protected Car create(CarEntity entity) {
            return new Car(entity.getId(), entity.getNumber());
        }
    }

    private static final class UserAbsMapperDto extends AbsMapperDto<UserEntity, User> {
        private final AbsMapperDto<CarEntity, Car> carMapper;

        public UserAbsMapperDto(ModelMapper modelMapper, AbsMapperDto<CarEntity, Car> carDtoMapper) {
            super(modelMapper, UserEntity.class, User.class);
            this.carMapper = carDtoMapper;
        }

        @Override
        protected User create(UserEntity entity) {
            return new User(entity.getId(), entity.getEmail(), entity.getName(), entity.getSurname(),
                    entity.getPatronymic(), this.carMapper.map(entity.getCar()));
        }
    }
}