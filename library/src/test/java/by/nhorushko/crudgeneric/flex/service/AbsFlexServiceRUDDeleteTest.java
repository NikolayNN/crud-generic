package by.nhorushko.crudgeneric.flex.service;

import by.nhorushko.crudgeneric.exception.AppNotFoundException;
import by.nhorushko.crudgeneric.flex.AbsModelMapper;
import by.nhorushko.crudgeneric.flex.model.AbsUpdateDto;
import by.nhorushko.crudgeneric.v2.domain.AbstractDto;
import by.nhorushko.crudgeneric.v2.domain.AbstractEntity;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AbsFlexServiceRUDDeleteTest {

    @Mock
    private AbsModelMapper mapper;

    @Mock
    private JpaRepository<ItemEntity, Long> repository;

    private final List<String> events = new ArrayList<>();

    private AbsFlexServiceRUD<Long, ItemEntity, ItemDto, ItemUpdate, JpaRepository<ItemEntity, Long>> service;

    @Before
    public void setUp() {
        service = new AbsFlexServiceRUD<>(mapper, repository, ItemEntity.class, ItemDto.class, ItemUpdate.class) {
            @Override
            protected void beforeDeleteHook(Long id) {
                events.add("before:" + id);
            }

            @Override
            protected void afterDeleteHook(Long id) {
                events.add("after:" + id);
            }
        };
    }

    @Test
    public void deleteRemovesExistingEntityAndRunsHooks() {
        when(repository.existsById(5L)).thenReturn(true);

        service.delete(5L);

        verify(repository).deleteById(5L);
        assertEquals(Arrays.asList("before:5", "after:5"), events);
    }

    @Test
    public void deleteMissingIdThrowsAndSkipsHooksAndDeletion() {
        when(repository.existsById(9L)).thenReturn(false);

        try {
            service.delete(9L);
            fail("expected AppNotFoundException for a missing id");
        } catch (AppNotFoundException expected) {
        }

        verify(repository, never()).deleteById(any());
        assertTrue(events.isEmpty());
    }

    public static class ItemEntity implements AbstractEntity<Long> {
        private Long id;

        @Override
        public Long getId() {
            return id;
        }

        @Override
        public void setId(Long id) {
            this.id = id;
        }
    }

    public static class ItemDto implements AbstractDto<Long> {
        private Long id;

        @Override
        public Long getId() {
            return id;
        }
    }

    public static class ItemUpdate implements AbsUpdateDto<Long> {
        private Long id;

        @Override
        public Long getId() {
            return id;
        }
    }
}
