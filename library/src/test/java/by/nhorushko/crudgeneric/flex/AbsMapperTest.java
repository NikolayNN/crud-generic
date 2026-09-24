package by.nhorushko.crudgeneric.flex;

import by.nhorushko.crudgeneric.flex.exception.MappingNotFoundException;
import by.nhorushko.crudgeneric.flex.mapper.Mapper;
import by.nhorushko.crudgeneric.flex.mapper.MapperRegistry;
import by.nhorushko.crudgeneric.flex.mapper.Updater;
import by.nhorushko.crudgeneric.flex.model.AbstractDto;
import by.nhorushko.crudgeneric.flex.model.AbstractEntity;
import jakarta.persistence.EntityManager;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AbsMapperTest {

    private final AtomicInteger registryCalls = new AtomicInteger();
    private AbsMapper mapper;

    @Before
    public void setUp() {
        MapperRegistry registry = new MapperRegistry(
                List.of(Mapper.of(Source.class, Target.class, source -> new Target(source.name))),
                List.of(Updater.of(Source.class, Target.class, (source, target) -> target.name = source.name)),
                List.of());
        mapper = new AbsMapper(() -> {
            registryCalls.incrementAndGet();
            return registry;
        }, null);
    }

    @Test
    public void mapNullSourceReturnsNullWithoutTouchingTheRegistry() {
        assertNull(mapper.map(null, Target.class));
        assertEquals(0, registryCalls.get());
    }

    @Test
    public void mapUsesTheRegisteredMapper() {
        assertEquals("a", mapper.map(new Source("a"), Target.class).name);
    }

    @Test
    public void mapWithoutRegisteredPairThrowsMappingNotFound() {
        assertThrows(MappingNotFoundException.class, () -> mapper.map(new Target("a"), Source.class));
    }

    @Test
    public void updateNullSourceReturnsDestinationUnchanged() {
        Target target = new Target("kept");

        assertSame(target, mapper.update(null, target));
        assertEquals("kept", target.name);
    }

    @Test
    public void updateWithNullDestinationThrowsInsteadOfSilentlyReturningNull() {
        assertThrows(NullPointerException.class, () -> mapper.update(new Source("a"), null));
    }

    @Test
    public void updateWritesThroughTheUpdaterAndReturnsTheSameInstance() {
        Target target = new Target("old");

        Target result = mapper.update(new Source("new"), target);

        assertSame(target, result);
        assertEquals("new", target.name);
    }

    @Test
    public void updateWithoutRegisteredUpdaterThrowsMappingNotFound() {
        assertThrows(MappingNotFoundException.class, () -> mapper.update(new Target("a"), new Source("b")));
    }

    @Test
    public void mapAllNullReturnsNull() {
        assertNull(mapper.mapAll(null, Target.class));
    }

    /** The result goes straight into entity collections that Hibernate manages, so it must be mutable. */
    @Test
    public void mapAllReturnsMutableListInSourceOrder() {
        List<Target> result = mapper.mapAll(List.of(new Source("a"), new Source("b")), Target.class);

        result.add(new Target("c"));

        assertEquals(3, result.size());
        assertEquals("a", result.get(0).name);
        assertEquals("b", result.get(1).name);
    }

    @Test
    public void registryIsResolvedOnFirstUseAndOnlyOnce() {
        assertEquals(0, registryCalls.get());

        mapper.map(new Source("a"), Target.class);
        mapper.update(new Source("b"), new Target("c"));
        mapper.mapAll(List.of(new Source("d")), Target.class);

        assertEquals(1, registryCalls.get());
    }

    @Test
    public void referenceUsesTheDtoIdAndTheEntityManager() {
        EntityManager entityManager = mock(EntityManager.class);
        ItemEntity reference = new ItemEntity();
        when(entityManager.getReference(ItemEntity.class, 5L)).thenReturn(reference);
        AbsMapper withJpa = new AbsMapper(new MapperRegistry(List.of(), List.of(), List.of()), entityManager);

        assertSame(reference, withJpa.reference(new ItemDto(5L), ItemEntity.class));
        assertSame(reference, withJpa.referenceById(5L, ItemEntity.class));
        assertSame(entityManager, withJpa.getEntityManager());
    }

    static class Source {
        final String name;

        Source(String name) {
            this.name = name;
        }
    }

    static class Target {
        String name;

        Target(String name) {
            this.name = name;
        }
    }

    static class ItemEntity implements AbstractEntity<Long> {
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

    static class ItemDto implements AbstractDto<Long> {
        private final Long id;

        ItemDto(Long id) {
            this.id = id;
        }

        @Override
        public Long getId() {
            return id;
        }
    }
}
