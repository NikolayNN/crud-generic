//package by.nhorushko.crudgeneric.v2.mapper;
//
//import by.nhorushko.crudgeneric.domain.dto.Car;
//import by.nhorushko.crudgeneric.domain.dto.User;
//import by.nhorushko.crudgeneric.domain.entity.CarEntity;
//import by.nhorushko.crudgeneric.domain.entity.UserEntity;
//import org.junit.Before;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.mockito.Mock;
//import org.mockito.runners.MockitoJUnitRunner;
//import org.modelmapper.ModelMapper;
//
//import jakarta.persistence.EntityManager;
//
//import java.util.List;
//
//import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.assertNull;
//import static org.mockito.Matchers.any;
//import static org.mockito.Matchers.anyLong;
//import static org.mockito.Mockito.when;
//
//@RunWith(MockitoJUnitRunner.class)
//public final class ExtDtoEntityMapperTest {
//    private final ModelMapper modelMapper;
//
//    @Mock
//    private EntityManager mockedEntityManager;
//
//    private UserAbsMapperEntityExtDto userMapper;
//
//    public ExtDtoEntityMapperTest() {
//        this.modelMapper = new ModelMapper();
//    }
//
//    @Before
//    public void initializeUserMapper() {
//        this.userMapper = new UserAbsMapperEntityExtDto(this.modelMapper, this.mockedEntityManager);
//    }
//
//    @SuppressWarnings("unchecked")
//    @Test
//    public void userDtoShouldBeMappedToUserEntity() {
//        final Car givenCar = new Car(256L, "number");
//        final User givenUser = new User(255L, "email@mail.ru", "name", "surname",
//                "patronymic", givenCar);
//
//        final CarEntity givenCarEntity = new CarEntity(256L, null);
//        when(this.mockedEntityManager.getReference(any(Class.class), anyLong())).thenReturn(givenCarEntity);
//
//        final UserEntity actual = this.userMapper.toEntity(givenCar.getId(), givenUser);
//        final UserEntity expected = new UserEntity(255L, "email@mail.ru", "name", "surname",
//                "patronymic", givenCarEntity);
//        assertEquals(expected, actual);
//    }
//
//    @Test
//    public void nullUserDtoShouldBeMappedToNullUserEntity() {
//        final UserEntity givenUserEntity = this.userMapper.toEntity(255L, null);
//        final User resultUserDto = this.userMapper.toDto(givenUserEntity);
//        assertNull(resultUserDto);
//    }
//
//    @SuppressWarnings("all")
//    private static final class UserAbsMapperEntityExtDto extends AbsMapperEntityExtDto<UserEntity, User, Long, CarEntity> {
//
//        public UserAbsMapperEntityExtDto(ModelMapper modelMapper, EntityManager entityManager) {
//            super(modelMapper, UserEntity.class, User.class, entityManager, CarEntity.class);
//        }
//
//        @Override
//        protected User create(UserEntity from) {
//            throw new UnsupportedOperationException();
//        }
//
//        @Override
//        protected void setRelation(CarEntity ext, UserEntity destination) {
//            destination.setCar(ext);
//        }
//    }
//}
