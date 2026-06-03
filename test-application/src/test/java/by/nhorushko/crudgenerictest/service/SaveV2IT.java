package by.nhorushko.crudgenerictest.service;

import by.nhorushko.crudgenerictest.domain.dto.RegionDto;
import by.nhorushko.crudgenerictest.domain.dto.Tracker;
import by.nhorushko.crudgenerictest.repository.RegionRepository;
import by.nhorushko.crudgenerictest.repository.TrackerRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NOTE: this repo runs Hibernate 6.4.1 (Spring Boot 3.2.1). The OptimisticLockException these
 * tests guard against only manifests on Hibernate 6.6 (the consuming app's target). On 6.4 a
 * merge of an absent-row assigned/sentinel id silently INSERTs, so these tests confirm the
 * persistOrMerge routing is correct, but do not by themselves reproduce the 6.6 failure.
 */
@SpringBootTest
class SaveV2IT {

    @Autowired
    private TrackerServiceCRUD trackerService;
    @Autowired
    private TrackerRepository trackerRepository;
    @Autowired
    private RegionServiceCRUD regionService;
    @Autowired
    private RegionRepository regionRepository;

    @AfterEach
    void cleanUp() {
        regionRepository.deleteAll();
        // tracker rows 1..5 are seeded by data.sql; remove only ids we create (>= 100)
        trackerRepository.findAll().stream()
                .filter(t -> t.getId() != null && t.getId() >= 100L)
                .forEach(trackerRepository::delete);
    }

    @Test
    void sentinelZeroIdInsertsForIdentityEntity() {
        Tracker saved = trackerService.save(new Tracker(0L, "imei-x", "phone-x"));
        assertThat(saved.getId()).isNotNull();
        assertThat(trackerRepository.existsById(saved.getId())).isTrue();
    }

    @Test
    void assignedIdAbsentInserts() {
        RegionDto saved = regionService.save(new RegionDto(42L, "north"));
        assertThat(saved.getId()).isEqualTo(42L);
        assertThat(regionRepository.existsById(42L)).isTrue();
    }

    @Test
    void assignedIdPresentUpdatesNoDuplicate() {
        regionService.save(new RegionDto(42L, "north"));
        RegionDto updated = regionService.save(new RegionDto(42L, "south"));

        assertThat(updated.getName()).isEqualTo("south");
        assertThat(regionRepository.count()).isEqualTo(1L);
        assertThat(regionRepository.findById(42L).orElseThrow().getName()).isEqualTo("south");
    }
}
