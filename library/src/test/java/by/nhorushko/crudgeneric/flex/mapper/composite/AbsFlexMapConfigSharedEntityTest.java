package by.nhorushko.crudgeneric.flex.mapper.composite;

import by.nhorushko.crudgeneric.flex.AbsModelMapper;
import by.nhorushko.crudgeneric.flex.config.AbsGenericCrudConfiguration;
import by.nhorushko.crudgeneric.flex.model.AbsCreateDto;
import by.nhorushko.crudgeneric.flex.model.AbsUpdateDto;
import by.nhorushko.crudgeneric.flex.model.AbstractDto;
import by.nhorushko.crudgeneric.flex.model.AbstractEntity;
import org.junit.Test;
import org.modelmapper.ModelMapper;

import static org.junit.Assert.assertNotNull;

/**
 * Two map configs over the same entity (each with its own DTO set) must both
 * register: the shared ENTITY->ENTITY self-map may only be created once, not
 * crash the second config with "TypeMap already exists".
 */
public class AbsFlexMapConfigSharedEntityTest {

    @Test
    public void twoConfigsForSameEntityRegisterWithoutCollision() {
        ModelMapper modelMapper = new AbsGenericCrudConfiguration().absGenericCrudModelMapper();
        AbsModelMapper mapper = new AbsModelMapper(modelMapper, null);

        new AbsFlexMapConfigDefault<CreateA, UpdateA, ReadA, SharedEntity>(
                mapper, CreateA.class, UpdateA.class, ReadA.class, SharedEntity.class) {
            @Override
            protected ReadA createReadDtoFromEntity(AbsModelMapper mapper, SharedEntity entity) {
                return new ReadA(entity.getId(), entity.getName());
            }
        };

        new AbsFlexMapConfigDefault<CreateB, UpdateB, ReadB, SharedEntity>(
                mapper, CreateB.class, UpdateB.class, ReadB.class, SharedEntity.class) {
            @Override
            protected ReadB createReadDtoFromEntity(AbsModelMapper mapper, SharedEntity entity) {
                return new ReadB(entity.getId(), entity.getName());
            }
        };

        assertNotNull(modelMapper.getTypeMap(SharedEntity.class, SharedEntity.class));
        assertNotNull(modelMapper.getTypeMap(CreateA.class, SharedEntity.class));
        assertNotNull(modelMapper.getTypeMap(CreateB.class, SharedEntity.class));
    }

    public static class SharedEntity implements AbstractEntity<Long> {
        private Long id;
        private String name;

        @Override
        public Long getId() {
            return id;
        }

        @Override
        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class CreateA implements AbsCreateDto {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class UpdateA implements AbsUpdateDto<Long> {
        private Long id;
        private String name;

        @Override
        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }

    public static class ReadA implements AbstractDto<Long> {
        private Long id;
        private String name;

        public ReadA() {
        }

        public ReadA(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }

    public static class CreateB implements AbsCreateDto {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class UpdateB implements AbsUpdateDto<Long> {
        private Long id;
        private String name;

        @Override
        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }

    public static class ReadB implements AbstractDto<Long> {
        private Long id;
        private String name;

        public ReadB() {
        }

        public ReadB(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }
}
