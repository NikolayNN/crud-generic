package by.nhorushko.crudgeneric.flex.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Pins the "new entity" contract: an entity is new when its id is null
 * or a numeric zero (the historical sentinel for not-yet-persisted entities).
 */
public class IdEntityIsNewTest {

    @Test
    public void nullIdIsNew() {
        assertTrue(longEntity(null).isNew());
    }

    @Test
    public void zeroLongIdIsNew() {
        assertTrue(longEntity(0L).isNew());
    }

    @Test
    public void zeroIntegerIdIsNew() {
        assertTrue(integerEntity(0).isNew());
    }

    @Test
    public void realNumericIdIsNotNew() {
        assertFalse(longEntity(5L).isNew());
    }

    @Test
    public void nonNumericIdIsNotNew() {
        assertFalse(stringEntity("abc").isNew());
    }

    @Test
    public void nullifyZeroIdClearsNumericZero() {
        LongEntity entity = longEntity(0L);
        entity.nullifyZeroId();
        assertNull(entity.getId());
    }

    @Test
    public void nullifyZeroIdKeepsRealId() {
        LongEntity entity = longEntity(5L);
        entity.nullifyZeroId();
        assertEquals(Long.valueOf(5L), entity.getId());
    }

    private LongEntity longEntity(Long id) {
        LongEntity entity = new LongEntity();
        entity.setId(id);
        return entity;
    }

    private IntegerEntity integerEntity(Integer id) {
        IntegerEntity entity = new IntegerEntity();
        entity.setId(id);
        return entity;
    }

    private StringEntity stringEntity(String id) {
        StringEntity entity = new StringEntity();
        entity.setId(id);
        return entity;
    }

    private static class LongEntity implements AbstractEntity<Long> {
        private Long id;

        @Override
        public Long getId() {
            return id;
        }

        @Override
        public void setId(Long id) {
            this.id = id;
        }
    }

    private static class IntegerEntity implements AbstractEntity<Integer> {
        private Integer id;

        @Override
        public Integer getId() {
            return id;
        }

        @Override
        public void setId(Integer id) {
            this.id = id;
        }
    }

    private static class StringEntity implements AbstractEntity<String> {
        private String id;

        @Override
        public String getId() {
            return id;
        }

        @Override
        public void setId(String id) {
            this.id = id;
        }
    }
}
