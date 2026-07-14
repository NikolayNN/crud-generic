package by.nhorushko.crudgenerictest.service;

import by.nhorushko.crudgenerictest.domain.dto.RegionCreateDto;
import by.nhorushko.crudgenerictest.domain.dto.RegionDto;
import by.nhorushko.crudgenerictest.repository.RegionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Flex create path is an upsert for assigned-id entities (persistOrMerge):
 * an absent id inserts, an existing id updates without creating a duplicate.
 */
@SpringBootTest
class FlexAssignedIdSaveIT {

    @Autowired
    private RegionServiceCRUD service;
    @Autowired
    private RegionRepository repository;

    @AfterEach
    void cleanUp() {
        repository.deleteAll();
    }

    @Test
    void assignedIdAbsentInserts() {
        RegionDto saved = service.save(new RegionCreateDto(42L, "north"));

        assertThat(saved.getId()).isEqualTo(42L);
        assertThat(repository.existsById(42L)).isTrue();
    }

    @Test
    void assignedIdPresentUpdatesWithoutDuplicate() {
        service.save(new RegionCreateDto(42L, "north"));

        RegionDto updated = service.save(new RegionCreateDto(42L, "south"));

        assertThat(updated.getName()).isEqualTo("south");
        assertThat(repository.count()).isEqualTo(1L);
        assertThat(repository.findById(42L).orElseThrow().getName()).isEqualTo("south");
    }
}
