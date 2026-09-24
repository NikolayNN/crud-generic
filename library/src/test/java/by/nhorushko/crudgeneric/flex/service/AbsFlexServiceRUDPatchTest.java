package by.nhorushko.crudgeneric.flex.service;

import by.nhorushko.crudgeneric.flex.AbsMapper;
import by.nhorushko.crudgeneric.flex.exception.AppNotFoundException;
import by.nhorushko.crudgeneric.flex.exception.MappingNotFoundException;
import by.nhorushko.crudgeneric.flex.mapper.Mapper;
import by.nhorushko.crudgeneric.flex.mapper.MapperRegistry;
import by.nhorushko.crudgeneric.flex.mapper.Updater;
import by.nhorushko.crudgeneric.flex.model.AbsCreateDto;
import by.nhorushko.crudgeneric.flex.model.AbsUpdateDto;
import by.nhorushko.crudgeneric.flex.model.AbstractDto;
import by.nhorushko.crudgeneric.flex.model.AbstractEntity;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The three write paths of AbsFlexServiceRUD — update, patch, changeEntity — share one pipeline:
 * loadForUpdate, an explicit change of the managed entity, saveUpdated, then the after-hooks.
 */
public class AbsFlexServiceRUDPatchTest {

    private final List<String> events = new ArrayList<>();
    private JpaRepository<ItemEntity, Long> repository;
    private ItemEntity stored;
    private RecordingService service;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() {
        repository = mock(JpaRepository.class);
        stored = new ItemEntity(1L, "old");
        when(repository.findById(1L)).thenReturn(Optional.of(stored));
        when(repository.findById(404L)).thenReturn(Optional.empty());
        ItemEntity locked = new ItemEntity(7L, "global");
        locked.locked = true;
        when(repository.findById(7L)).thenReturn(Optional.of(locked));
        when(repository.save(any(ItemEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        MapperRegistry registry = new MapperRegistry(
                List.of(Mapper.of(ItemEntity.class, ItemDto.class, entity -> new ItemDto(entity.getId(), entity.name))),
                List.of(Updater.of(ItemUpdate.class, ItemEntity.class, (dto, entity) -> entity.name = dto.name),
                        Updater.of(NamePatch.class, ItemEntity.class, (patch, entity) -> entity.name = patch.name())),
                List.of());
        service = new RecordingService(new AbsMapper(registry, null), repository, events);
    }

    /** The hookable snapshot comes from the row loadForUpdate returned: the row is read once. */
    @Test
    public void updateRunsThePipelineThroughTheUpdateDtoUpdater() {
        ItemDto result = service.update(new ItemUpdate(1L, "new"));

        assertEquals("new", result.name);
        assertEquals("new", stored.name);
        assertEquals(List.of("beforeUpdate:new", "load:1", "beforeChanges:old", "save:new",
                "afterUpdate:new", "afterChanges:old->new"), events);
        verify(repository, times(1)).findById(1L);
    }

    @Test
    public void patchRunsBeforePatchHookHookableHooksAndAfterUpdateHook() {
        ItemDto result = service.patch(1L, new NamePatch("new"));

        assertEquals("new", result.name);
        assertEquals("new", stored.name);
        assertEquals(List.of("beforePatch:1:NamePatch", "load:1", "beforeChanges:old", "save:new",
                "afterUpdate:new", "afterChanges:old->new"), events);
    }

    /**
     * A guard in loadForUpdate protects every write path: on update and patch it runs before the
     * hookable before-hook, so the hook never sees a rejected row and never runs its side effects.
     */
    @Test
    public void guardInLoadForUpdateRunsBeforeTheHookableBeforeHook() {
        assertThrows(IllegalStateException.class, () -> service.patch(7L, new NamePatch("new")));
        assertThrows(IllegalStateException.class, () -> service.update(new ItemUpdate(7L, "new")));

        assertEquals(List.of("beforePatch:7:NamePatch", "load:7", "beforeUpdate:new", "load:7"), events);
        verify(repository, never()).save(any());
    }

    /** update(null, entity) would return the entity unchanged, and patch would then store it silently. */
    @Test
    public void patchWithNullBodyOrIdThrowsWithoutSaving() {
        assertThrows(NullPointerException.class, () -> service.patch(1L, null));
        assertThrows(NullPointerException.class, () -> service.patch(null, new NamePatch("new")));

        assertTrue(events.isEmpty());
        verify(repository, never()).save(any());
        assertEquals("old", stored.name);
    }

    /** A controller passing a raw JSON map as the body gets a loud failure, not a partial write. */
    @Test
    public void patchBodyWithoutUpdaterThrowsMappingNotFoundWithoutSaving() {
        Map<String, Object> body = new LinkedHashMap<>(Map.of("name", "new"));

        MappingNotFoundException e = assertThrows(MappingNotFoundException.class, () -> service.patch(1L, body));

        assertTrue(e.getMessage(), e.getMessage().contains(LinkedHashMap.class.getName()));
        verify(repository, never()).save(any());
        assertEquals("old", stored.name);
    }

    /** The update DTO has an updater, so without this check patch would skip checkId and beforeUpdateHook. */
    @Test
    public void patchRejectsTheUpdateDtoOfTheServiceBeforeAnyHook() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.patch(1L, new ItemUpdate(1L, "new")));

        assertTrue(e.getMessage(), e.getMessage().contains(ItemUpdate.class.getName() + " is the update DTO"));
        assertTrue(events.isEmpty());
        verify(repository, never()).save(any());
        assertEquals("old", stored.name);
    }

    @Test
    public void patchRejectsTheReadDtoOfTheServiceBeforeAnyHook() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.patch(1L, new ItemDto(1L, "new")));

        assertTrue(e.getMessage(), e.getMessage().contains(ItemDto.class.getName() + " is the read DTO"));
        assertTrue(events.isEmpty());
        verify(repository, never()).save(any());
    }

    @Test
    public void crudServiceRejectsItsCreateDtoAsPatchBody() {
        AbsFlexServiceCRUD<Long, ItemEntity, ItemDto, ItemUpdate, ItemCreate, JpaRepository<ItemEntity, Long>> crud =
                new AbsFlexServiceCRUD<>(service.mapper, repository, ItemEntity.class, ItemDto.class,
                        ItemUpdate.class, ItemCreate.class) {
                };

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> crud.patch(1L, new ItemCreate("new")));

        assertTrue(e.getMessage(), e.getMessage().contains(ItemCreate.class.getName() + " is the create DTO"));
        verify(repository, never()).save(any());
    }

    /** Exact class, like every registry key: a subclass of the update DTO is its own key, not rejected here. */
    @Test
    public void patchWithSubclassOfUpdateDtoIsNotRejectedAsTheUpdateDto() {
        assertThrows(MappingNotFoundException.class, () -> service.patch(1L, new ItemUpdateSub(1L, "new")));

        verify(repository, never()).save(any());
    }

    /** Exact key at the service level: a subclass of UPDATE_DTO never borrows the parent's updater. */
    @Test
    public void updateWithSubclassOfUpdateDtoIsNotMatchedByTheParentUpdater() {
        assertThrows(MappingNotFoundException.class, () -> service.update(new ItemUpdateSub(1L, "new")));

        verify(repository, never()).save(any());
        assertEquals("old", stored.name);
    }

    @Test
    public void changeEntityAppliesChangeSavesAndRunsOnlyAfterHooks() {
        ItemDto result = service.rename(1L, "renamed");

        assertEquals("renamed", result.name);
        assertEquals("renamed", stored.name);
        assertEquals(List.of("load:1", "save:renamed", "afterUpdate:renamed", "afterChanges:old->renamed"), events);
    }

    @Test
    public void changeEntityWhoseChangeThrowsSavesNothingAndSkipsAfterHooks() {
        assertThrows(IllegalStateException.class, () -> service.changeEntity(1L, entity -> {
            throw new IllegalStateException("rejected");
        }));

        assertEquals(List.of("load:1"), events);
        verify(repository, never()).save(any());
    }

    @Test
    public void loadForUpdateWithoutEntityThrowsAppNotFound() {
        assertThrows(AppNotFoundException.class, () -> service.rename(404L, "x"));

        assertEquals(List.of("load:404"), events);
        verify(repository, never()).save(any());
    }

    static class RecordingService
            extends AbsFlexServiceRUD<Long, ItemEntity, ItemDto, ItemUpdate, JpaRepository<ItemEntity, Long>>
            implements AbsUpdateChangesHookable<Long, ItemDto> {

        private final List<String> events;

        RecordingService(AbsMapper mapper, JpaRepository<ItemEntity, Long> repository, List<String> events) {
            super(mapper, repository, ItemEntity.class, ItemDto.class, ItemUpdate.class);
            this.events = events;
        }

        ItemDto rename(Long id, String name) {
            return changeEntity(id, entity -> entity.name = name);
        }

        @Override
        protected void beforeUpdateHook(ItemUpdate dto) {
            events.add("beforeUpdate:" + dto.name);
        }

        @Override
        protected void beforePatchHook(Long id, Object body) {
            events.add("beforePatch:" + id + ":" + body.getClass().getSimpleName());
        }

        @Override
        public void beforeUpdateHook(ItemDto previous, Object current) {
            events.add("beforeChanges:" + previous.name);
        }

        @Override
        protected ItemEntity loadForUpdate(Long id) {
            events.add("load:" + id);
            ItemEntity entity = super.loadForUpdate(id);
            if (entity.locked) {
                throw new IllegalStateException("Item id: " + id + " is locked");
            }
            return entity;
        }

        @Override
        protected ItemEntity saveUpdated(ItemEntity entity) {
            events.add("save:" + entity.name);
            return super.saveUpdated(entity);
        }

        @Override
        protected void afterUpdateHook(ItemDto dto) {
            events.add("afterUpdate:" + dto.name);
        }

        @Override
        public void afterUpdateHook(ItemDto previous, ItemDto current) {
            events.add("afterChanges:" + previous.name + "->" + current.name);
        }
    }

    static class ItemEntity implements AbstractEntity<Long> {
        private Long id;
        String name;
        boolean locked;

        ItemEntity(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public Long getId() {
            return id;
        }

        @Override
        public void setId(Long id) {
            this.id = id;
        }
    }

    static class ItemDto implements AbstractDto<Long> {
        private final Long id;
        final String name;

        ItemDto(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public Long getId() {
            return id;
        }
    }

    static class ItemUpdate implements AbsUpdateDto<Long> {
        private final Long id;
        final String name;

        ItemUpdate(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public Long getId() {
            return id;
        }
    }

    static class ItemUpdateSub extends ItemUpdate {
        ItemUpdateSub(Long id, String name) {
            super(id, name);
        }
    }

    static class ItemCreate implements AbsCreateDto {
        final String name;

        ItemCreate(String name) {
            this.name = name;
        }
    }

    record NamePatch(String name) {
    }
}
