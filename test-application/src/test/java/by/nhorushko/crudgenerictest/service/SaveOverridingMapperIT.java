package by.nhorushko.crudgenerictest.service;

import by.nhorushko.crudgeneric.v2.mapper.AbsMapperEntityDto;
import by.nhorushko.crudgeneric.v2.service.AbsServiceCRUD;
import by.nhorushko.crudgenerictest.domain.dto.Tracker;
import by.nhorushko.crudgenerictest.domain.entity.TrackerEntity;
import by.nhorushko.crudgenerictest.repository.TrackerRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression for the 13.3.13-jakarta bug: a mapper that OVERRIDES toEntity(...) and copies the
 * DTO id directly bypasses the {@code 0 -> null} normalisation that lives in the base converter
 * ({@link AbsMapperEntityDto#createConverterDtoToEntity} -> {@code nullifyZeroId}). The sentinel
 * id = 0 then reaches persistOrMerge, which used to {@code persist} a non-null-id IDENTITY entity
 * and fail with "detached entity passed to persist". The fix normalises the sentinel inside
 * persistOrMerge itself (the chokepoint every save path funnels through). This reproduces on
 * Hibernate 6.4.1 too, because persist() rejecting a non-null id on an IDENTITY entity is core
 * JPA behaviour, not version-specific.
 */
@SpringBootTest
class SaveOverridingMapperIT {

    @Autowired
    private OverridingTrackerService service;
    @Autowired
    private TrackerRepository trackerRepository;

    private final List<Long> createdIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        createdIds.forEach(id -> trackerRepository.findById(id).ifPresent(trackerRepository::delete));
        createdIds.clear();
    }

    @Test
    void sentinelZeroIdInsertsEvenWhenMapperOverridesToEntity() {
        Tracker saved = service.save(new Tracker(0L, "imei-ov", "phone-ov"));
        if (saved.getId() != null) {
            createdIds.add(saved.getId());
        }

        assertThat(saved.getId()).isNotNull();
        assertThat(trackerRepository.existsById(saved.getId())).isTrue();
    }

    @TestConfiguration
    static class Config {
        @Bean
        OverridingTrackerMapper overridingTrackerMapper(EntityManager entityManager) {
            // Own ModelMapper instance: the shared singleton already holds a TypeMap for
            // TrackerEntity<->Tracker registered by the production TrackerAbsMapperEntityDto.
            return new OverridingTrackerMapper(new ModelMapper(), entityManager);
        }

        @Bean
        OverridingTrackerService overridingTrackerService(OverridingTrackerMapper mapper, TrackerRepository repository) {
            return new OverridingTrackerService(mapper, repository);
        }
    }

    /**
     * Mirrors an application mapper that overrides toEntity and copies dto.getId() (the supported,
     * common pattern), thereby bypassing the base converter's {@code nullifyZeroId}.
     */
    static class OverridingTrackerMapper extends AbsMapperEntityDto<TrackerEntity, Tracker> {
        OverridingTrackerMapper(ModelMapper modelMapper, EntityManager entityManager) {
            super(modelMapper, entityManager, TrackerEntity.class, Tracker.class);
        }

        @Override
        public TrackerEntity toEntity(Tracker dto) {
            TrackerEntity e = new TrackerEntity();
            e.setId(dto.getId()); // 0L sentinel copied verbatim — NOT normalised
            e.setImei(dto.getImei());
            e.setPhoneNumber(dto.getPhoneNumber());
            return e;
        }

        @Override
        protected Tracker create(TrackerEntity from) {
            return new Tracker(from.getId(), from.getImei(), from.getPhoneNumber());
        }
    }

    static class OverridingTrackerService
            extends AbsServiceCRUD<Long, TrackerEntity, Tracker, TrackerRepository> {
        OverridingTrackerService(OverridingTrackerMapper mapper, TrackerRepository repository) {
            super(mapper, repository);
        }
    }
}
