package by.nhorushko.crudgenerictest.mapper;

import by.nhorushko.crudgenerictest.domain.dto.MockADto;
import by.nhorushko.crudgenerictest.domain.dto.Tracker;
import by.nhorushko.crudgenerictest.domain.entity.MockAEntity;
import by.nhorushko.crudgenerictest.domain.entity.TrackerEntity;
import by.nhorushko.crudgenerictest.mockmapper.MockAMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class MapperNullifyZeroIdTest {

    @Autowired
    private MockAMapper v1Mapper;

    @Autowired
    private TrackerAbsMapperEntityDto v2Mapper;

    @Test
    void v1MapperNullsZeroId() {
        MockAEntity entity = v1Mapper.toEntity(new MockADto(0L, "name"));
        assertThat(entity.getId()).isNull();
    }

    @Test
    void v1MapperKeepsRealId() {
        MockAEntity entity = v1Mapper.toEntity(new MockADto(7L, "name"));
        assertThat(entity.getId()).isEqualTo(7L);
    }

    @Test
    void v2MapperNullsZeroId() {
        TrackerEntity entity = v2Mapper.toEntity(new Tracker(0L, "imei", "phone"));
        assertThat(entity.getId()).isNull();
    }

    @Test
    void v2MapperKeepsRealId() {
        TrackerEntity entity = v2Mapper.toEntity(new Tracker(1L, "imei", "phone"));
        assertThat(entity.getId()).isEqualTo(1L);
    }
}
