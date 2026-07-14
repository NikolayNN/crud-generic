package by.nhorushko.crudgenerictest.service;

import by.nhorushko.crudgeneric.flex.AbsModelMapper;
import by.nhorushko.crudgeneric.flex.mapper.core.AbsMapBasic;
import by.nhorushko.crudgeneric.flex.model.AbsCreateDto;
import by.nhorushko.crudgeneric.flex.service.AbsFlexServiceCRUD;
import by.nhorushko.crudgenerictest.domain.dto.OrderDto;
import by.nhorushko.crudgenerictest.domain.dto.OrderUpdateDto;
import by.nhorushko.crudgenerictest.domain.entity.OrderEntity;
import by.nhorushko.crudgenerictest.repository.OrderRepository;
import lombok.Value;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.TypeMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A mapper that registers its own converter (not via AbsMapBaseDtoToEntity) copies the
 * sentinel id 0 verbatim into the entity, bypassing the base converter's nullifyZeroId.
 * persistOrMerge normalises the sentinel itself — the save must INSERT, not merge/fail.
 * Flex twin of the deleted v2 SaveOverridingMapperIT.
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
        @Bean
        ZeroIdOrderCreateMapper zeroIdOrderCreateMapper(AbsModelMapper mapper) {
            // Registers ZeroIdOrderCreate -> OrderEntity on the SHARED mapper: a distinct
            // source type, so no collision with OrderMapConfig, and AbsTypeMapChecker
            // (which validates against the shared mapper) finds the create map at startup.
            return new ZeroIdOrderCreateMapper(mapper);
        }

        @Bean
        OverridingOrderService overridingOrderService(AbsModelMapper mapper, OrderRepository repository) {
            return new OverridingOrderService(mapper, repository);
        }
    }

    static class ZeroIdOrderCreateMapper extends AbsMapBasic<ZeroIdOrderCreate, OrderEntity> {
        ZeroIdOrderCreateMapper(AbsModelMapper mapper) {
            super(mapper, ZeroIdOrderCreate.class, OrderEntity.class);
        }

        @Override
        protected void customizeTypeMap(TypeMap<ZeroIdOrderCreate, OrderEntity> typeMap) {
            typeMap.setConverter(context -> {
                OrderEntity entity = new OrderEntity();
                entity.setId(context.getSource().getId()); // 0L sentinel copied verbatim — NOT normalised
                entity.setName(context.getSource().getName());
                return entity;
            });
        }
    }

    static class OverridingOrderService
            extends AbsFlexServiceCRUD<Long, OrderEntity, OrderDto, OrderUpdateDto, ZeroIdOrderCreate, OrderRepository> {
        OverridingOrderService(AbsModelMapper mapper, OrderRepository repository) {
            super(mapper, repository, OrderEntity.class, OrderDto.class, OrderUpdateDto.class, ZeroIdOrderCreate.class);
        }
    }

    @Value
    static class ZeroIdOrderCreate implements AbsCreateDto {
        Long id;
        String name;
    }
}
