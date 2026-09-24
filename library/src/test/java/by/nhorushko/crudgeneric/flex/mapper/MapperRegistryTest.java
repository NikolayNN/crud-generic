package by.nhorushko.crudgeneric.flex.mapper;

import by.nhorushko.crudgeneric.flex.exception.MappingNotFoundException;
import jakarta.persistence.Entity;
import org.junit.Test;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class MapperRegistryTest {

    @Test
    public void findsMapperByExactPair() {
        Mapper<BaseDto, BaseEntity> mapper = Mapper.of(BaseDto.class, BaseEntity.class, dto -> new BaseEntity());

        MapperRegistry registry = new MapperRegistry(List.of(mapper), List.of(), List.of());

        assertSame(mapper, registry.getMapper(BaseDto.class, BaseEntity.class));
        assertSame(mapper, registry.findMapper(BaseDto.class, BaseEntity.class).orElseThrow());
    }

    /**
     * No walking up the hierarchy: in LocatorServer Carrier extends CarrierUpdate extends
     * CarrierCreate, and a pair for the parent must never serve the child.
     */
    @Test
    public void subclassOfRegisteredSourceIsNotFound() {
        MapperRegistry registry = new MapperRegistry(
                List.of(Mapper.of(BaseDto.class, BaseEntity.class, dto -> new BaseEntity())), List.of(), List.of());

        assertFalse(registry.findMapper(SubDto.class, BaseEntity.class).isPresent());
        MappingNotFoundException e = assertThrows(MappingNotFoundException.class,
                () -> registry.getMapper(SubDto.class, BaseEntity.class));
        assertEquals("No Mapper registered for " + SubDto.class.getName() + " -> " + BaseEntity.class.getName(),
                e.getMessage());
    }

    @Test
    public void interfacesOfTheSourceAreNotConsidered() {
        MapperRegistry registry = new MapperRegistry(
                List.of(Mapper.of(Marker.class, BaseEntity.class, marker -> new BaseEntity())), List.of(), List.of());

        assertFalse(registry.findMapper(MarkedDto.class, BaseEntity.class).isPresent());
    }

    @Test
    public void proxyLikeSubclassIsNormalisedToItsEntityForMapperSource() {
        Mapper<BaseEntity, OtherDto> mapper = Mapper.of(BaseEntity.class, OtherDto.class, entity -> new OtherDto());

        MapperRegistry registry = new MapperRegistry(List.of(mapper), List.of(), List.of());

        assertSame(mapper, registry.getMapper(BaseEntityProxy.class, OtherDto.class));
    }

    @Test
    public void proxyLikeSubclassIsNormalisedForUpdaterSourceAndDestination() {
        Updater<OtherDto, BaseEntity> intoEntity = Updater.of(OtherDto.class, BaseEntity.class, (dto, entity) -> { });
        Updater<BaseEntity, OtherEntity> fromEntity = Updater.of(BaseEntity.class, OtherEntity.class, (entity, other) -> { });

        MapperRegistry registry = new MapperRegistry(List.of(), List.of(intoEntity, fromEntity), List.of());

        assertSame(intoEntity, registry.getUpdater(OtherDto.class, BaseEntityProxy.class));
        assertSame(fromEntity, registry.getUpdater(BaseEntityProxy.class, OtherEntity.class));
    }

    @Test
    public void entitySubclassOfEntityKeepsItsOwnKey() {
        MapperRegistry registry = new MapperRegistry(
                List.of(Mapper.of(BaseEntity.class, OtherDto.class, entity -> new OtherDto())), List.of(), List.of());

        assertFalse(registry.findMapper(SubEntity.class, OtherDto.class).isPresent());
    }

    /**
     * Lookups normalise a non-@Entity subclass of an entity to the entity (the proxy rule), so a pair
     * declared for such a class could never be found: the build must fail instead of keeping a dead key.
     */
    @Test
    public void pairFromNonEntitySubclassOfEntityFailsTheBuild() {
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> new MapperRegistry(
                List.of(Mapper.of(BaseEntityProxy.class, OtherDto.class, proxy -> new OtherDto())), List.of(), List.of()));

        assertTrue(e.getMessage(), e.getMessage().contains(BaseEntityProxy.class.getName()));
        assertTrue(e.getMessage(), e.getMessage().contains("extends @Entity " + BaseEntity.class.getName()));
    }

    @Test
    public void updaterIntoNonEntitySubclassOfEntityFailsTheBuild() {
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> new MapperRegistry(
                List.of(), List.of(Updater.of(OtherDto.class, BaseEntityProxy.class, (dto, proxy) -> { })), List.of()));

        assertTrue(e.getMessage(), e.getMessage().contains(BaseEntityProxy.class.getName()));
        assertTrue(e.getMessage(), e.getMessage().contains("extends @Entity " + BaseEntity.class.getName()));
    }

    @Test
    public void duplicatePairFailsWithBothImplementations() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> new MapperRegistry(List.of(new FirstBaseMapper(), new SecondBaseMapper()), List.of(), List.of()));

        assertTrue(e.getMessage(), e.getMessage().startsWith("Duplicate Mapper for "
                + BaseDto.class.getName() + " -> " + BaseEntity.class.getName()));
        assertTrue(e.getMessage(), e.getMessage().contains(FirstBaseMapper.class.getName()));
        assertTrue(e.getMessage(), e.getMessage().contains(SecondBaseMapper.class.getName()));
    }

    @Test
    public void duplicateBetweenBeanAndSourceNamesTheSource() {
        Updater<OtherDto, BaseEntity> bean = Updater.of(OtherDto.class, BaseEntity.class, (dto, entity) -> { });
        StubSource source = new StubSource(List.of(),
                List.of(Updater.of(OtherDto.class, BaseEntity.class, (dto, entity) -> { })));

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> new MapperRegistry(List.of(), List.of(bean), List.of(source)));

        assertTrue(e.getMessage(), e.getMessage().startsWith("Duplicate Updater for "));
        assertTrue(e.getMessage(), e.getMessage().contains(StubSource.class.getName()));
    }

    /** Mapper.of beans are anonymous classes: only the bean names tell the two apart. */
    @Test
    public void duplicateBetweenNamedBeansNamesBothBeans() {
        Map<String, Updater<?, ?>> updaterBeans = Map.of("patchUpdater",
                Updater.of(OtherDto.class, BaseEntity.class, (dto, entity) -> { }));
        Map<String, MapperSource> sources = Map.of("baseConfig", new StubSource(List.of(),
                List.of(Updater.of(OtherDto.class, BaseEntity.class, (dto, entity) -> { }))));

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> new MapperRegistry(Map.of(), updaterBeans, sources));

        assertTrue(e.getMessage(), e.getMessage().startsWith("Duplicate Updater for "));
        assertTrue(e.getMessage(), e.getMessage().contains("bean 'patchUpdater' (by.nhorushko.crudgeneric.flex.mapper.Updater$"));
        assertTrue(e.getMessage(), e.getMessage().contains("bean 'baseConfig' (" + StubSource.class.getName() + ")"));
    }

    @Test
    public void mapperWithNullClassFailsWithItsClassName() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> new MapperRegistry(List.of(new NullClassMapper()), List.of(), List.of()));

        assertTrue(e.getMessage(), e.getMessage().contains(NullClassMapper.class.getName()));
    }

    @Test
    public void updaterWithNullClassFailsWithItsClassName() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> new MapperRegistry(List.of(), List.of(new NullClassUpdater()), List.of()));

        assertTrue(e.getMessage(), e.getMessage().contains(NullClassUpdater.class.getName()));
    }

    @Test
    public void missingUpdaterThrowsWithDirectionAndBothClassNames() {
        MapperRegistry registry = new MapperRegistry(List.of(), List.of(), List.of());

        MappingNotFoundException e = assertThrows(MappingNotFoundException.class,
                () -> registry.getUpdater(OtherDto.class, BaseEntity.class));

        assertEquals("No Updater registered for " + OtherDto.class.getName() + " -> " + BaseEntity.class.getName(),
                e.getMessage());
    }

    @Test
    public void mapperSourceIsUnwrapped() {
        Mapper<BaseDto, BaseEntity> mapper = Mapper.of(BaseDto.class, BaseEntity.class, dto -> new BaseEntity());
        Updater<OtherDto, BaseEntity> updater = Updater.of(OtherDto.class, BaseEntity.class, (dto, entity) -> { });

        MapperRegistry registry = new MapperRegistry(List.of(), List.of(),
                List.of(new StubSource(List.of(mapper), List.of(updater))));

        assertSame(mapper, registry.getMapper(BaseDto.class, BaseEntity.class));
        assertSame(updater, registry.getUpdater(OtherDto.class, BaseEntity.class));
    }

    /** Two configs for one entity, one per DTO set, register different pairs and coexist. */
    @Test
    public void twoSourcesForOneEntityWithDifferentDtosCoexist() {
        StubSource first = new StubSource(
                List.of(Mapper.of(BaseDto.class, BaseEntity.class, dto -> new BaseEntity()),
                        Mapper.of(BaseEntity.class, OtherDto.class, entity -> new OtherDto())),
                List.of(Updater.of(OtherDto.class, BaseEntity.class, (dto, entity) -> { })));
        StubSource second = new StubSource(
                List.of(Mapper.of(SubDto.class, BaseEntity.class, dto -> new BaseEntity()),
                        Mapper.of(BaseEntity.class, MarkedDto.class, entity -> new MarkedDto())),
                List.of(Updater.of(MarkedDto.class, BaseEntity.class, (dto, entity) -> { })));

        MapperRegistry registry = new MapperRegistry(List.of(), List.of(), List.of(first, second));

        assertTrue(registry.findMapper(BaseDto.class, BaseEntity.class).isPresent());
        assertTrue(registry.findMapper(SubDto.class, BaseEntity.class).isPresent());
        assertTrue(registry.findMapper(BaseEntity.class, OtherDto.class).isPresent());
        assertTrue(registry.findMapper(BaseEntity.class, MarkedDto.class).isPresent());
        assertTrue(registry.findUpdater(OtherDto.class, BaseEntity.class).isPresent());
        assertTrue(registry.findUpdater(MarkedDto.class, BaseEntity.class).isPresent());
    }

    static class BaseDto {
    }

    static class SubDto extends BaseDto {
    }

    interface Marker {
    }

    static class MarkedDto implements Marker {
    }

    static class OtherDto {
    }

    @Entity
    static class BaseEntity {
    }

    /** Models a Hibernate proxy: an unannotated runtime subclass of an entity. */
    static class BaseEntityProxy extends BaseEntity {
    }

    @Entity
    static class SubEntity extends BaseEntity {
    }

    @Entity
    static class OtherEntity {
    }

    static class FirstBaseMapper implements Mapper<BaseDto, BaseEntity> {
        @Override
        public Class<BaseDto> fromClass() {
            return BaseDto.class;
        }

        @Override
        public Class<BaseEntity> toClass() {
            return BaseEntity.class;
        }

        @Override
        public BaseEntity map(BaseDto from) {
            return new BaseEntity();
        }
    }

    static class SecondBaseMapper extends FirstBaseMapper {
    }

    static class NullClassMapper extends FirstBaseMapper {
        @Override
        public Class<BaseEntity> toClass() {
            return null;
        }
    }

    static class NullClassUpdater implements Updater<OtherDto, BaseEntity> {
        @Override
        public Class<OtherDto> fromClass() {
            return null;
        }

        @Override
        public Class<BaseEntity> toClass() {
            return BaseEntity.class;
        }

        @Override
        public void update(OtherDto from, BaseEntity into) {
        }
    }

    static class StubSource implements MapperSource {
        private final List<Mapper<?, ?>> mappers;
        private final List<Updater<?, ?>> updaters;

        StubSource(List<Mapper<?, ?>> mappers, List<Updater<?, ?>> updaters) {
            this.mappers = mappers;
            this.updaters = updaters;
        }

        @Override
        public Collection<Mapper<?, ?>> mappers() {
            return mappers;
        }

        @Override
        public Collection<Updater<?, ?>> updaters() {
            return updaters;
        }
    }
}
