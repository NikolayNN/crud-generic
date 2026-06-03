package by.nhorushko.crudgenerictest.service;

import by.nhorushko.crudgenerictest.MockRepository;
import by.nhorushko.crudgenerictest.MockService;
import by.nhorushko.crudgenerictest.domain.dto.MockADto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SaveV1IT {

    @Autowired
    private MockService service;
    @Autowired
    private MockRepository repository;

    @AfterEach
    void cleanUp() {
        repository.deleteAll();
    }

    @Test
    void sentinelZeroIdInserts() {
        MockADto saved = service.save(new MockADto(0L, "sentinel"));
        assertThat(saved.getId()).isNotNull();
        assertThat(repository.existsById(saved.getId())).isTrue();
    }

    @Test
    void saveWithExistingIdUpdatesNoDuplicate() {
        MockADto created = service.save(new MockADto(0L, "first"));
        Long id = created.getId();

        MockADto updated = service.save(new MockADto(id, "second"));

        assertThat(updated.getId()).isEqualTo(id);
        assertThat(repository.count()).isEqualTo(1L);
        assertThat(repository.findById(id).orElseThrow().getName()).isEqualTo("second");
    }
}
