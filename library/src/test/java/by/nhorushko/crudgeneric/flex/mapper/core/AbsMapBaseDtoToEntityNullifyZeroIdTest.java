package by.nhorushko.crudgeneric.flex.mapper.core;

import by.nhorushko.crudgeneric.flex.AbsModelMapper;
import by.nhorushko.crudgeneric.flex.config.AbsGenericCrudConfiguration;
import by.nhorushko.crudgeneric.v2.domain.AbstractDto;
import by.nhorushko.crudgeneric.v2.domain.AbstractEntity;
import org.junit.Before;
import org.junit.Test;
import org.modelmapper.ModelMapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * The flex DTO->entity converter must normalise the sentinel id 0 to null, so
 * nested child entities mapped through it are treated as new by Hibernate
 * (mirrors the v2 fix in AbsMapperEntityDto).
 */
public class AbsMapBaseDtoToEntityNullifyZeroIdTest {

    private AbsModelMapper mapper;

    @Before
    public void setUp() {
        ModelMapper modelMapper = new AbsGenericCrudConfiguration().absGenericCrudModelMapper();
        mapper = new AbsModelMapper(modelMapper, null);
        new AbsMapDtoToEntity<>(mapper, LineDto.class, LineEntity.class) {
        };
    }

    @Test
    public void mapNormalisesSentinelZeroIdToNull() {
        LineEntity entity = mapper.map(new LineDto(0L, "child"), LineEntity.class);

        assertNull(entity.getId());
        assertEquals("child", entity.getTitle());
    }

    @Test
    public void mapKeepsRealId() {
        LineEntity entity = mapper.map(new LineDto(7L, "child"), LineEntity.class);

        assertEquals(Long.valueOf(7L), entity.getId());
    }

    public static class LineDto implements AbstractDto<Long> {
        private Long id;
        private String title;

        public LineDto() {
        }

        public LineDto(Long id, String title) {
            this.id = id;
            this.title = title;
        }

        @Override
        public Long getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }
    }

    public static class LineEntity implements AbstractEntity<Long> {
        private Long id;
        private String title;

        @Override
        public Long getId() {
            return id;
        }

        @Override
        public void setId(Long id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }
    }
}
