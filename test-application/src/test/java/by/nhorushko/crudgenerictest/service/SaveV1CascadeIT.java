package by.nhorushko.crudgenerictest.service;

import by.nhorushko.crudgenerictest.domain.dto.GroupDto;
import by.nhorushko.crudgenerictest.domain.dto.ItemDto;
import by.nhorushko.crudgenerictest.repository.GroupRepository;
import by.nhorushko.crudgenerictest.repository.ItemRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NOTE: this repo runs Hibernate 6.4.1 (Spring Boot 3.2.1). The OptimisticLockException these
 * tests guard against only manifests on Hibernate 6.6 (the consuming app's target). On 6.4 a
 * merge of an absent-row assigned/sentinel id silently INSERTs, so these tests confirm the
 * persistOrMerge routing is correct, but do not by themselves reproduce the 6.6 failure.
 */
@SpringBootTest
class SaveV1CascadeIT {

    @Autowired
    private GroupService service;
    @Autowired
    private GroupRepository groupRepository;
    @Autowired
    private ItemRepository itemRepository;

    @AfterEach
    void cleanUp() {
        groupRepository.deleteAll();
    }

    @Test
    void newChildWithSentinelIdIsInsertedOnExistingParent() {
        // create an existing parent (no children)
        GroupDto created = service.save(new GroupDto(0L, "group", new ArrayList<>()));
        Long groupId = created.getId();
        assertThat(itemRepository.count()).isZero();

        // save the existing parent with a NEW child carrying sentinel id = 0
        GroupDto existing = service.getById(groupId);
        existing.getItems().add(new ItemDto(0L, "child"));

        GroupDto result = service.save(existing); // must NOT throw OptimisticLockException

        assertThat(result.getId()).isEqualTo(groupId);
        assertThat(itemRepository.count()).isEqualTo(1L);
        List<ItemDto> savedItems = service.getById(groupId).getItems();
        assertThat(savedItems).hasSize(1);
        assertThat(savedItems.get(0).getId()).isNotNull();
        assertThat(savedItems.get(0).getTitle()).isEqualTo("child");
    }
}
