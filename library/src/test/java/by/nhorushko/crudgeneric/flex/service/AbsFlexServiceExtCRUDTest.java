package by.nhorushko.crudgeneric.flex.service;

import by.nhorushko.crudgeneric.flex.AbsModelMapper;
import by.nhorushko.crudgeneric.flex.mapper.mapper.AbsMapperExtRelation;
import by.nhorushko.crudgeneric.flex.model.AbsCreateDto;
import by.nhorushko.crudgeneric.flex.model.AbsUpdateDto;
import by.nhorushko.crudgeneric.flex.model.AbstractDto;
import by.nhorushko.crudgeneric.flex.model.AbstractEntity;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AbsFlexServiceExtCRUDTest {

    private static final Long RELATION_ID = 7L;

    @Mock
    private AbsModelMapper mapper;

    @Mock
    private JpaRepository<ItemEntity, Long> repository;

    @Mock
    private AbsMapperExtRelation<ItemCreate, ItemEntity, Long, OwnerEntity> extMapper;

    private AbsFlexServiceExtCRUD<Long, ItemEntity, ItemDto, ItemUpdate, ItemCreate, JpaRepository<ItemEntity, Long>, Long, OwnerEntity> service;

    @Before
    public void setUp() {
        service = new AbsFlexServiceExtCRUD<>(
                mapper, repository, ItemEntity.class, ItemDto.class, ItemUpdate.class, ItemCreate.class, extMapper) {
            @Override
            protected void beforeSaveHook(Long relationId, ItemCreate dto) {
                dto.setName("hooked");
            }
        };
    }

    /**
     * saveAll validates that mapped entities are new; save must apply the same
     * guard instead of silently updating the existing row the id points at.
     */
    @Test
    public void saveRejectsMappedEntityWithExistingId() {
        when(extMapper.map(eq(RELATION_ID), any(ItemCreate.class))).thenReturn(entity(5L));

        try {
            service.save(RELATION_ID, new ItemCreate("item"));
            fail("expected IllegalArgumentException for a mapped entity with an existing id");
        } catch (IllegalArgumentException expected) {
        }

        verify(repository, never()).save(any(ItemEntity.class));
    }

    /**
     * The sentinel id 0 marks a new entity ({@code isNew()} contract). It must pass the
     * create guard and be normalised to {@code null} before the repository call, otherwise
     * Spring Data sees a non-null id and routes the save through merge instead of persist.
     */
    @Test
    public void saveTreatsZeroIdAsNewAndNullifiesItBeforeSave() {
        when(extMapper.map(eq(RELATION_ID), any(ItemCreate.class))).thenReturn(entity(0L));
        List<Long> idsAtSaveTime = new ArrayList<>();
        when(repository.save(any(ItemEntity.class))).thenAnswer(invocation -> {
            ItemEntity entity = invocation.getArgument(0);
            idsAtSaveTime.add(entity.getId());
            return entity;
        });
        when(mapper.map(any(ItemEntity.class), eq(ItemDto.class))).thenReturn(new ItemDto(1L, "hooked"));

        service.save(RELATION_ID, new ItemCreate("item"));

        assertEquals(singletonList((Long) null), idsAtSaveTime);
    }

    @Test
    public void saveAllTreatsZeroIdAsNewAndNullifiesItBeforeSave() {
        when(extMapper.mapAll(eq(RELATION_ID), anyCollection()))
                .thenReturn(new ArrayList<>(singletonList(entity(0L))));
        List<Long> idsAtSaveTime = new ArrayList<>();
        when(repository.saveAll(anyList())).thenAnswer(invocation -> {
            List<ItemEntity> entities = invocation.getArgument(0);
            entities.forEach(entity -> idsAtSaveTime.add(entity.getId()));
            return entities;
        });
        when(mapper.mapAll(anyCollection(), eq(ItemDto.class))).thenReturn(singletonList(new ItemDto(1L, "hooked")));

        service.saveAll(RELATION_ID, singletonList(new ItemCreate("item")));

        assertEquals(singletonList((Long) null), idsAtSaveTime);
    }

    /**
     * beforeSaveHook may mutate the DTO (defaults, normalisation), so it must
     * run before the DTOs are mapped to entities — as save() already does.
     */
    @Test
    public void saveAllRunsBeforeSaveHookBeforeMapping() {
        List<String> namesAtMapTime = new ArrayList<>();
        when(extMapper.mapAll(eq(RELATION_ID), anyCollection())).thenAnswer(invocation -> {
            Collection<ItemCreate> dtos = invocation.getArgument(1);
            dtos.forEach(dto -> namesAtMapTime.add(dto.getName()));
            return new ArrayList<>(singletonList(entity(null)));
        });
        when(repository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.mapAll(anyCollection(), eq(ItemDto.class))).thenReturn(singletonList(new ItemDto(1L, "hooked")));

        service.saveAll(RELATION_ID, singletonList(new ItemCreate("original")));

        assertEquals(singletonList("hooked"), namesAtMapTime);
    }

    private ItemEntity entity(Long id) {
        ItemEntity entity = new ItemEntity();
        entity.setId(id);
        return entity;
    }

    public static class ItemEntity implements AbstractEntity<Long> {
        private Long id;
        private String name;

        @Override
        public Long getId() {
            return id;
        }

        @Override
        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class ItemDto implements AbstractDto<Long> {
        private Long id;
        private String name;

        public ItemDto() {
        }

        public ItemDto(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }

    public static class ItemUpdate implements AbsUpdateDto<Long> {
        private Long id;
        private String name;

        @Override
        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }

    public static class ItemCreate implements AbsCreateDto {
        private String name;

        public ItemCreate() {
        }

        public ItemCreate(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class OwnerEntity implements AbstractEntity<Long> {
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
}
