package by.nhorushko.crudgenerictest.service;

import by.nhorushko.crudgenerictest.domain.dto.TaskCreateDto;
import by.nhorushko.crudgenerictest.domain.dto.TaskDto;
import by.nhorushko.crudgenerictest.domain.entity.ProjectEntity;
import by.nhorushko.crudgenerictest.repository.ProjectRepository;
import by.nhorushko.crudgenerictest.repository.TaskRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Create path of {@code AbsFlexServiceExtCRUD} on the real Spring/Hibernate
 * stack: the saved entity must be linked to the relation resolved by id, the
 * sentinel id 0 must insert a new row (not merge), and a real id must be
 * rejected because the ext path is create-only.
 */
@SpringBootTest
class FlexExtSaveIT {

    @Autowired
    private TaskServiceExtCRUD service;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private ProjectRepository projectRepository;

    @AfterEach
    void cleanUp() {
        taskRepository.deleteAll();
        projectRepository.deleteAll();
    }

    @Test
    void saveLinksNewEntityToRelation() {
        ProjectEntity project = persistedProject("apollo");

        TaskDto saved = service.save(project.getId(), new TaskCreateDto(null, "write report"));

        assertThat(saved.getId()).isPositive();
        assertThat(saved.getTitle()).isEqualTo("write report");
        assertThat(taskRepository.findAllByProjectId(project.getId())).hasSize(1);
    }

    @Test
    void saveWithSentinelZeroIdInsertsNewRow() {
        ProjectEntity project = persistedProject("apollo");

        TaskDto saved = service.save(project.getId(), new TaskCreateDto(0L, "task"));

        assertThat(saved.getId()).isPositive();
        assertThat(taskRepository.count()).isEqualTo(1L);
        assertThat(taskRepository.findAllByProjectId(project.getId())).hasSize(1);
    }

    @Test
    void saveRejectsRealIdBecauseExtPathIsCreateOnly() {
        ProjectEntity project = persistedProject("apollo");
        TaskDto existing = service.save(project.getId(), new TaskCreateDto(null, "existing"));

        assertThatThrownBy(() -> service.save(project.getId(), new TaskCreateDto(existing.getId(), "dup")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(taskRepository.count()).isEqualTo(1L);
    }

    @Test
    void saveAllLinksAllEntitiesToRelation() {
        ProjectEntity project = persistedProject("apollo");

        List<TaskDto> saved = service.saveAll(project.getId(),
                List.of(new TaskCreateDto(null, "a"), new TaskCreateDto(0L, "b")));

        assertThat(saved).hasSize(2);
        assertThat(saved).allSatisfy(dto -> assertThat(dto.getId()).isPositive());
        assertThat(taskRepository.findAllByProjectId(project.getId())).hasSize(2);
    }

    private ProjectEntity persistedProject(String name) {
        ProjectEntity project = new ProjectEntity();
        project.setName(name);
        return projectRepository.save(project);
    }
}
