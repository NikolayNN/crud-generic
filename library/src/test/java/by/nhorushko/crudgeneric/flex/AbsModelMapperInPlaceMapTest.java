package by.nhorushko.crudgeneric.flex;

import by.nhorushko.crudgeneric.flex.config.AbsGenericCrudConfiguration;
import by.nhorushko.crudgeneric.flex.mapper.core.AbsMapDtoToEntity;
import by.nhorushko.crudgeneric.v2.domain.AbstractDto;
import by.nhorushko.crudgeneric.v2.domain.AbstractEntity;
import org.junit.Before;
import org.junit.Test;
import org.modelmapper.ModelMapper;

import static org.junit.Assert.assertEquals;

/**
 * The in-place map(source, destination) overload must only write properties the
 * source carries: destination fields absent from the source stay untouched.
 */
public class AbsModelMapperInPlaceMapTest {

    private AbsModelMapper mapper;

    @Before
    public void setUp() {
        ModelMapper modelMapper = new AbsGenericCrudConfiguration().absGenericCrudModelMapper();
        mapper = new AbsModelMapper(modelMapper, null);
        new AbsMapDtoToEntity<>(mapper, OrderDto.class, OrderEntity.class) {
        };
    }

    @Test
    public void inPlaceMapLeavesFieldsAbsentFromSourceUntouched() {
        OrderEntity entity = new OrderEntity();
        entity.setId(5L);
        entity.setName("old");
        entity.setSecretCode("s3cret");

        OrderEntity result = mapper.map(new OrderDto(5L, "new"), entity);

        assertEquals("new", entity.getName());
        assertEquals("s3cret", entity.getSecretCode());
        assertEquals(Long.valueOf(5L), entity.getId());
        assertEquals(entity, result);
    }

    public static class OrderDto implements AbstractDto<Long> {
        private Long id;
        private String name;

        public OrderDto() {
        }

        public OrderDto(Long id, String name) {
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

    public static class OrderEntity implements AbstractEntity<Long> {
        private Long id;
        private String name;
        private String secretCode;

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

        public String getSecretCode() {
            return secretCode;
        }

        public void setSecretCode(String secretCode) {
            this.secretCode = secretCode;
        }
    }
}
