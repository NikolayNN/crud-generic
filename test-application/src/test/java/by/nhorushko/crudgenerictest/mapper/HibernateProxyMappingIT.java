package by.nhorushko.crudgenerictest.mapper;

import by.nhorushko.crudgeneric.flex.AbsMapper;
import by.nhorushko.crudgenerictest.domain.dto.RegionDto;
import by.nhorushko.crudgenerictest.domain.dto.RegionUpdateDto;
import by.nhorushko.crudgenerictest.domain.entity.RegionEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A Hibernate proxy's class is an unannotated runtime subclass of the entity. The registry
 * normalises it to the nearest @Entity ancestor, so a lazy reference maps with the entity's pair
 * both as a Mapper source and as an Updater destination. Each test rolls back.
 */
@SpringBootTest
@Transactional
class HibernateProxyMappingIT {

    @Autowired
    private AbsMapper mapper;
    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void proxyAsMapperSourceUsesTheEntityPair() {
        RegionEntity proxy = storedRegionProxy(7L, "north");

        assertThat(mapper.map(proxy, RegionDto.class)).isEqualTo(new RegionDto(7L, "north"));
    }

    @Test
    void proxyAsUpdaterDestinationUsesTheEntityPair() {
        RegionEntity proxy = storedRegionProxy(8L, "south");

        mapper.update(new RegionUpdateDto(8L, "renamed"), proxy);

        assertThat(proxy.getName()).isEqualTo("renamed");
    }

    private RegionEntity storedRegionProxy(Long id, String name) {
        entityManager.persist(new RegionEntity(id, name));
        entityManager.flush();
        entityManager.clear();
        RegionEntity proxy = entityManager.getReference(RegionEntity.class, id);
        assertThat(proxy.getClass()).isNotEqualTo(RegionEntity.class);
        return proxy;
    }
}
