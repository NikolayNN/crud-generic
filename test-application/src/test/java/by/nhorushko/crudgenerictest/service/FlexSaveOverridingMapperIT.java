package by.nhorushko.crudgenerictest.service;

import by.nhorushko.crudgeneric.flex.AbsMapper;
import by.nhorushko.crudgeneric.flex.mapper.Mapper;
import by.nhorushko.crudgeneric.flex.model.AbsCreateDto;
import by.nhorushko.crudgeneric.flex.service.AbsFlexServiceCRUD;
import by.nhorushko.crudgenerictest.domain.dto.OrderDto;
import by.nhorushko.crudgenerictest.domain.dto.OrderUpdateDto;
import by.nhorushko.crudgenerictest.domain.entity.OrderEntity;
import by.nhorushko.crudgenerictest.repository.OrderRepository;
import lombok.Value;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A create mapper declared as a plain {@code Mapper.of} bean — not built on AbsMapDtoToEntity or
 * AbsFlexMapConfig — copies the sentinel id 0 verbatim into the entity, skipping their
 * nullifyZeroId. persistOrMerge normalises the sentinel itself: the save must INSERT, not
 * merge/fail. Flex twin of the deleted v2 SaveOverridingMapperIT.
 */
@SpringBootTest
class FlexSaveOverridingMapperIT {

    @Autowired
    private OverridingOrderService service;
    @Autowired
    private OrderRepository orderRepository;

    @AfterEach
    void cleanUp() {
        orderRepository.deleteAll();
    }

    @Test
    void sentinelZeroIdInsertsEvenWhenMapperBypassesBaseNormalisation() {
        OrderDto saved = service.save(new ZeroIdOrderCreate(0L, "ov-order"));

        assertThat(saved.getId()).isNotNull();
        assertThat(orderRepository.existsById(saved.getId())).isTrue();
    }

    @TestConfiguration
    static class Config {
        /** A distinct source type, so no collision with OrderMapConfig's OrderCreateDto -> OrderEntity. */
        @Bean
        Mapper<ZeroIdOrderCreate, OrderEntity> zeroIdOrderCreateMapper() {
            return Mapper.of(ZeroIdOrderCreate.class, OrderEntity.class, dto -> {
                OrderEntity entity = new OrderEntity();
                entity.setId(dto.getId()); // 0L sentinel copied verbatim — NOT normalised
                entity.setName(dto.getName());
                return entity;
            });
        }

        @Bean
        OverridingOrderService overridingOrderService(AbsMapper mapper, OrderRepository repository) {
            return new OverridingOrderService(mapper, repository);
        }
    }

    static class OverridingOrderService
            extends AbsFlexServiceCRUD<Long, OrderEntity, OrderDto, OrderUpdateDto, ZeroIdOrderCreate, OrderRepository> {
        OverridingOrderService(AbsMapper mapper, OrderRepository repository) {
            super(mapper, repository, OrderEntity.class, OrderDto.class, OrderUpdateDto.class, ZeroIdOrderCreate.class);
        }
    }

    @Value
    static class ZeroIdOrderCreate implements AbsCreateDto {
        Long id;
        String name;
    }
}
