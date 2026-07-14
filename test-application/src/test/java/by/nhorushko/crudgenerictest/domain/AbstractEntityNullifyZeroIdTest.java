package by.nhorushko.crudgenerictest.domain;

import by.nhorushko.crudgenerictest.domain.entity.RegionEntity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AbstractEntityNullifyZeroIdTest {

    @Test
    void zeroIdShouldBecomeNull() {
        RegionEntity entity = RegionEntity.builder().id(0L).build();

        entity.nullifyZeroId();

        assertNull(entity.getId());
    }

    @Test
    void realIdShouldRemainUnchanged() {
        RegionEntity entity = RegionEntity.builder().id(5L).build();

        entity.nullifyZeroId();

        assertEquals(5L, entity.getId());
    }

    @Test
    void nullIdShouldRemainNull() {
        RegionEntity entity = RegionEntity.builder().id(null).build();

        entity.nullifyZeroId();

        assertNull(entity.getId());
    }
}
