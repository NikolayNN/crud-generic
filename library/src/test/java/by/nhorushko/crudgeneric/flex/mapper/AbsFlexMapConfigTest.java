package by.nhorushko.crudgeneric.flex.mapper;

import by.nhorushko.crudgeneric.flex.model.AbsCreateDto;
import by.nhorushko.crudgeneric.flex.model.AbsUpdateDto;
import by.nhorushko.crudgeneric.flex.model.AbstractDto;
import by.nhorushko.crudgeneric.flex.model.AbstractEntity;
import org.junit.Test;

import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class AbsFlexMapConfigTest {

    @Test
    public void exposesCreateAndReadMappersAndTheUpdateUpdaterWithDeclaredClasses() {
        ItemConfig config = new ItemConfig();

        assertEquals(List.of("ItemCreate->ItemEntity", "ItemEntity->ItemDto"), mapperPairs(config.mappers()));
        assertEquals(List.of("ItemUpdate->ItemEntity"), updaterPairs(config.updaters()));
    }

    @Test
    public void addsOneUpdaterPerPatchEntry() {
        PatchedItemConfig config = new PatchedItemConfig();

        assertEquals(List.of("ItemUpdate->ItemEntity", "NamePatch->ItemEntity", "CodePatch->ItemEntity"),
                updaterPairs(config.updaters()));
    }

    /** An overridable method called from the constructor would run before the subclass's fields exist. */
    @Test
    public void patchesIsNotCalledFromTheConstructor() {
        PatchedItemConfig config = new PatchedItemConfig();
        assertEquals(0, config.patchesCalls);

        config.updaters();

        assertEquals(1, config.patchesCalls);
    }

    @Test
    public void createMapperNormalisesZeroIdAndKeepsRealId() {
        MapperRegistry registry = new MapperRegistry(List.of(), List.of(), List.of(new ItemConfig()));
        Mapper<ItemCreate, ItemEntity> create = registry.getMapper(ItemCreate.class, ItemEntity.class);

        assertNull(create.map(new ItemCreate(0L, "new")).getId());
        assertEquals(Long.valueOf(7L), create.map(new ItemCreate(7L, "assigned")).getId());
    }

    @Test
    public void adaptersDelegateToTheAbstractMethodsAndPatches() {
        MapperRegistry registry = new MapperRegistry(List.of(), List.of(), List.of(new PatchedItemConfig()));
        ItemEntity entity = new ItemEntity(1L, "old");

        registry.getUpdater(ItemUpdate.class, ItemEntity.class).update(new ItemUpdate(1L, "updated"), entity);
        assertEquals("updated", entity.name);
        registry.getUpdater(NamePatch.class, ItemEntity.class).update(new NamePatch("patched"), entity);
        assertEquals("patched", entity.name);
        registry.getUpdater(CodePatch.class, ItemEntity.class).update(new CodePatch("c-1"), entity);
        assertEquals("c-1", entity.code);
        assertEquals("patched", registry.getMapper(ItemEntity.class, ItemDto.class).map(entity).name);
    }

    @Test
    public void rejectsNullClassInTheConstructor() {
        NullPointerException e = assertThrows(NullPointerException.class, () -> new ItemConfig(null));

        assertEquals("createDtoClass", e.getMessage());
    }

    /** A patch declared for the config's own UPDATE_DTO collides with the update pair: startup fails naming the config. */
    @Test
    public void patchForTheUpdateDtoClassFailsTheRegistryBuildNamingTheConfig() {
        ItemConfig config = new ItemConfig() {
            @Override
            protected void patches(Patches<ItemEntity> p) {
                p.add(ItemUpdate.class, (dto, entity) -> entity.name = dto.name);
            }
        };

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> new MapperRegistry(List.of(), List.of(), List.of(config)));

        assertTrue(e.getMessage(), e.getMessage().startsWith("Duplicate Updater for " + ItemUpdate.class.getName()));
        assertTrue(e.getMessage(), e.getMessage().contains(config.getClass().getName()));
    }

    @Test
    public void sameBodyClassDeclaredTwiceFailsTheRegistryBuildNamingTheConfig() {
        ItemConfig config = new ItemConfig() {
            @Override
            protected void patches(Patches<ItemEntity> p) {
                p.add(NamePatch.class, (patch, entity) -> entity.name = patch.name())
                        .add(NamePatch.class, (patch, entity) -> entity.name = patch.name());
            }
        };

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> new MapperRegistry(List.of(), List.of(), List.of(config)));

        assertTrue(e.getMessage(), e.getMessage().startsWith("Duplicate Updater for " + NamePatch.class.getName()));
        assertTrue(e.getMessage(), e.getMessage().contains(config.getClass().getName()));
    }

    private static List<String> mapperPairs(Collection<Mapper<?, ?>> mappers) {
        return mappers.stream()
                .map(m -> m.fromClass().getSimpleName() + "->" + m.toClass().getSimpleName())
                .toList();
    }

    private static List<String> updaterPairs(Collection<Updater<?, ?>> updaters) {
        return updaters.stream()
                .map(u -> u.fromClass().getSimpleName() + "->" + u.toClass().getSimpleName())
                .toList();
    }

    static class ItemConfig extends AbsFlexMapConfig<ItemCreate, ItemUpdate, ItemDto, ItemEntity> {
        ItemConfig() {
            this(ItemCreate.class);
        }

        ItemConfig(Class<ItemCreate> createDtoClass) {
            super(null, createDtoClass, ItemUpdate.class, ItemDto.class, ItemEntity.class);
        }

        @Override
        protected ItemEntity toEntity(ItemCreate dto) {
            return new ItemEntity(dto.id, dto.name);
        }

        @Override
        protected void updateEntity(ItemUpdate dto, ItemEntity entity) {
            entity.name = dto.name;
        }

        @Override
        protected ItemDto toReadDto(ItemEntity entity) {
            return new ItemDto(entity.getId(), entity.name);
        }
    }

    static class PatchedItemConfig extends ItemConfig {
        int patchesCalls;

        @Override
        protected void patches(Patches<ItemEntity> p) {
            patchesCalls++;
            p.add(NamePatch.class, (patch, entity) -> entity.name = patch.name())
                    .add(CodePatch.class, (patch, entity) -> entity.code = patch.code());
        }
    }

    record NamePatch(String name) {
    }

    record CodePatch(String code) {
    }

    static class ItemCreate implements AbsCreateDto {
        final Long id;
        final String name;

        ItemCreate(Long id, String name) {
            this.id = id;
            this.name = name;
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

    static class ItemEntity implements AbstractEntity<Long> {
        private Long id;
        String name;
        String code;

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
}
