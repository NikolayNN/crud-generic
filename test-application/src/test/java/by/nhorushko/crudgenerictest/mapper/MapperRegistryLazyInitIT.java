package by.nhorushko.crudgenerictest.mapper;

import by.nhorushko.crudgeneric.flex.AbsMapper;
import by.nhorushko.crudgeneric.flex.mapper.MapperRegistry;
import by.nhorushko.crudgenerictest.domain.dto.MeetingDto;
import by.nhorushko.crudgenerictest.domain.dto.OrderCreateDto;
import by.nhorushko.crudgenerictest.domain.dto.OrderDto;
import by.nhorushko.crudgenerictest.domain.dto.OrderLineDto;
import by.nhorushko.crudgenerictest.domain.dto.OrderNamePatch;
import by.nhorushko.crudgenerictest.domain.dto.OrderUpdateDto;
import by.nhorushko.crudgenerictest.domain.dto.OrderView;
import by.nhorushko.crudgenerictest.domain.dto.RegionCreateDto;
import by.nhorushko.crudgenerictest.domain.dto.RegionDto;
import by.nhorushko.crudgenerictest.domain.dto.RegionUpdateDto;
import by.nhorushko.crudgenerictest.domain.dto.TaskCreateDto;
import by.nhorushko.crudgenerictest.domain.dto.TaskDto;
import by.nhorushko.crudgenerictest.domain.dto.TaskUpdateDto;
import by.nhorushko.crudgenerictest.domain.entity.MeetingEntity;
import by.nhorushko.crudgenerictest.domain.entity.OrderEntity;
import by.nhorushko.crudgenerictest.domain.entity.OrderLineEntity;
import by.nhorushko.crudgenerictest.domain.entity.RegionEntity;
import by.nhorushko.crudgenerictest.domain.entity.TaskEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Under global lazy init the registry is still built at startup ({@code @Lazy(false)}) and holds
 * every mapper bean of the demo: it pulls them all in through constructor injection, so a broken
 * registration fails the start, not the first request. Replaces the eager-init tests of 14.x.
 */
@SpringBootTest(properties = {
        "spring.main.lazy-initialization=true",
        "spring.datasource.url=jdbc:h2:mem:lazytestdb;NON_KEYWORDS=USER"
})
class MapperRegistryLazyInitIT {

    @Autowired
    private MapperRegistry registry;
    @Autowired
    private AbsMapper mapper;

    @Test
    void registryHoldsEveryDemoMapperUnderLazyInit() {
        assertThat(registry.findMapper(OrderCreateDto.class, OrderEntity.class)).isPresent();
        assertThat(registry.findMapper(OrderEntity.class, OrderDto.class)).isPresent();
        assertThat(registry.findUpdater(OrderUpdateDto.class, OrderEntity.class)).isPresent();
        assertThat(registry.findUpdater(OrderNamePatch.class, OrderEntity.class)).isPresent();
        assertThat(registry.findMapper(OrderLineDto.class, OrderLineEntity.class)).isPresent();
        assertThat(registry.findMapper(OrderEntity.class, OrderView.class)).isPresent();
        assertThat(registry.findMapper(RegionCreateDto.class, RegionEntity.class)).isPresent();
        assertThat(registry.findMapper(RegionEntity.class, RegionDto.class)).isPresent();
        assertThat(registry.findUpdater(RegionUpdateDto.class, RegionEntity.class)).isPresent();
        assertThat(registry.findMapper(TaskCreateDto.class, TaskEntity.class)).isPresent();
        assertThat(registry.findMapper(TaskEntity.class, TaskDto.class)).isPresent();
        assertThat(registry.findUpdater(TaskUpdateDto.class, TaskEntity.class)).isPresent();
        assertThat(registry.findMapper(MeetingEntity.class, MeetingDto.class)).isPresent();
    }

    @Test
    void mapToImmutableViewWorksUnderLazyInit() {
        OrderEntity entity = new OrderEntity();
        entity.setId(42L);
        entity.setName("alice");

        assertThat(mapper.map(entity, OrderView.class)).isEqualTo(new OrderView(42L, "alice"));
    }
}
