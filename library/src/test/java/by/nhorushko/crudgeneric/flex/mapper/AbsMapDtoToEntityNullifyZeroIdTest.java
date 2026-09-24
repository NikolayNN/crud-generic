package by.nhorushko.crudgeneric.flex.mapper;

import by.nhorushko.crudgeneric.flex.AbsMapper;
import by.nhorushko.crudgeneric.flex.model.AbstractDto;
import by.nhorushko.crudgeneric.flex.model.AbstractEntity;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * AbsMapDtoToEntity normalises the sentinel id 0 to null, so nested child entities mapped through
 * it are treated as new by Hibernate.
 */
public class AbsMapDtoToEntityNullifyZeroIdTest {

    private final LineMapper lineMapper = new LineMapper();

    @Test
    public void mapNormalisesSentinelZeroIdToNull() {
        LineEntity entity = lineMapper.map(new LineDto(0L, "child"));

        assertNull(entity.getId());
        assertEquals("child", entity.getTitle());
    }

    @Test
    public void mapKeepsRealId() {
        assertEquals(Long.valueOf(7L), lineMapper.map(new LineDto(7L, "child")).getId());
    }

    @Test
    public void facadeMapGoesThroughTheNormalisingMapper() {
        AbsMapper mapper = new AbsMapper(new MapperRegistry(List.of(lineMapper), List.of(), List.of()), null);

        assertNull(mapper.map(new LineDto(0L, "child"), LineEntity.class).getId());
    }

    static class LineMapper extends AbsMapDtoToEntity<LineDto, LineEntity> {
        LineMapper() {
            super(null, LineDto.class, LineEntity.class);
        }

        @Override
        protected LineEntity create(LineDto from) {
            LineEntity entity = new LineEntity();
            entity.setId(from.getId());
            entity.setTitle(from.getTitle());
            return entity;
        }
    }

    public static class LineDto implements AbstractDto<Long> {
        private final Long id;
        private final String title;

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
