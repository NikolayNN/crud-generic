package by.nhorushko.crudgenerictest.mapper;

import by.nhorushko.crudgeneric.flex.AbsModelMapper;
import by.nhorushko.crudgeneric.flex.mapper.composite.AbsFlexMapConfigDefault;
import by.nhorushko.crudgeneric.flex.model.AbsCreateDto;
import by.nhorushko.crudgeneric.flex.model.AbsUpdateDto;
import by.nhorushko.crudgeneric.v2.domain.AbstractDto;
import by.nhorushko.crudgenerictest.domain.dto.OrderDto;
import by.nhorushko.crudgenerictest.domain.entity.OrderEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spring startup with TWO {@code AbsFlexMapConfigDefault} beans for the same
 * entity ({@code OrderEntity}): the production {@code OrderMapConfig} plus a
 * second config with its own DTO triple. Registration must not collide — the
 * shared ENTITY->ENTITY self-map is registered once (guarded), and both DTO
 * sets keep working. A dedicated H2 url forks the cached test context so the
 * extra config does not leak into the other integration tests.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:twoconfigsdb;NON_KEYWORDS=USER")
class FlexTwoConfigsForSameEntityIT {

    @TestConfiguration
    static class SecondOrderConfig {
        @Bean
        AbsFlexMapConfigDefault<OrderSummaryCreateDto, OrderSummaryUpdateDto, OrderSummaryDto, OrderEntity> orderSummaryMapConfig(AbsModelMapper mapper) {
            return new AbsFlexMapConfigDefault<>(mapper,
                    OrderSummaryCreateDto.class, OrderSummaryUpdateDto.class, OrderSummaryDto.class, OrderEntity.class) {
                @Override
                protected OrderSummaryDto createReadDtoFromEntity(AbsModelMapper mapper, OrderEntity entity) {
                    return new OrderSummaryDto(entity.getId(), entity.getName());
                }
            };
        }
    }

    @Autowired
    private AbsModelMapper mapper;

    @Test
    void bothConfigsMapEntityToTheirReadDto() {
        OrderEntity entity = new OrderEntity();
        entity.setId(7L);
        entity.setName("order-7");

        assertThat(mapper.map(entity, OrderDto.class)).isEqualTo(new OrderDto(7L, "order-7"));
        assertThat(mapper.map(entity, OrderSummaryDto.class)).isEqualTo(new OrderSummaryDto(7L, "order-7"));
    }

    @Test
    void secondConfigCreateDtoMapsToEntity() {
        OrderEntity entity = mapper.map(new OrderSummaryCreateDto("summary-order"), OrderEntity.class);

        assertThat(entity.getId()).isNull();
        assertThat(entity.getName()).isEqualTo("summary-order");
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class OrderSummaryDto implements AbstractDto<Long> {
        private Long id;
        private String name;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class OrderSummaryUpdateDto implements AbsUpdateDto<Long> {
        private Long id;
        private String name;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class OrderSummaryCreateDto implements AbsCreateDto {
        private String name;
    }
}
