package by.nhorushko.crudgeneric.flex.mapper;

import by.nhorushko.crudgeneric.flex.model.AbsCreateDto;
import by.nhorushko.crudgeneric.flex.model.AbsUpdateDto;
import by.nhorushko.crudgeneric.flex.model.AbstractDto;
import by.nhorushko.crudgeneric.flex.model.AbstractEntity;
import org.junit.Test;
import org.springframework.aop.framework.ProxyFactory;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * A consumer's mapper bean can end up behind a CGLIB proxy (an aspect, @Transactional). The proxy
 * instance's own fields are never initialised, so fromClass(), toClass() and map() must stay
 * overridable for the proxy to reach the target: the bases must not declare them final.
 */
public class MapperBasesCglibProxyTest {

    @Test
    public void proxiedBasesRegisterUnderTheirDeclaredPairsAndDelegate() {
        Mapper<?, ?> toDto = cglibProxy(new ItemToDto());
        Mapper<?, ?> toEntity = cglibProxy(new ItemCreateToEntity());
        Updater<?, ?> update = cglibProxy(new ItemUpdateToEntity());

        MapperRegistry registry = new MapperRegistry(List.of(toDto, toEntity), List.of(update), List.of());

        assertEquals("a", registry.getMapper(ItemEntity.class, ItemDto.class).map(new ItemEntity(1L, "a")).name);
        assertNull(registry.getMapper(ItemCreate.class, ItemEntity.class).map(new ItemCreate(0L, "b")).getId());
        ItemEntity entity = new ItemEntity(1L, "old");
        registry.getUpdater(ItemUpdate.class, ItemEntity.class).update(new ItemUpdate(1L, "new"), entity);
        assertEquals("new", entity.name);
    }

    @SuppressWarnings("unchecked")
    private static <T> T cglibProxy(T target) {
        ProxyFactory factory = new ProxyFactory(target);
        factory.setProxyTargetClass(true);
        return (T) factory.getProxy();
    }

    public static class ItemToDto extends AbsMapEntityToDto<ItemEntity, ItemDto> {
        public ItemToDto() {
            super(null, ItemEntity.class, ItemDto.class);
        }

        @Override
        protected ItemDto create(ItemEntity from) {
            return new ItemDto(from.getId(), from.name);
        }
    }

    public static class ItemCreateToEntity extends AbsMapDtoToEntity<ItemCreate, ItemEntity> {
        public ItemCreateToEntity() {
            super(null, ItemCreate.class, ItemEntity.class);
        }

        @Override
        protected ItemEntity create(ItemCreate from) {
            return new ItemEntity(from.id, from.name);
        }
    }

    public static class ItemUpdateToEntity extends AbsMapUpdateDtoToEntity<ItemUpdate, ItemEntity> {
        public ItemUpdateToEntity() {
            super(null, ItemUpdate.class, ItemEntity.class);
        }

        @Override
        public void update(ItemUpdate from, ItemEntity into) {
            into.name = from.name;
        }
    }

    public static class ItemCreate implements AbsCreateDto {
        final Long id;
        final String name;

        ItemCreate(Long id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    public static class ItemUpdate implements AbsUpdateDto<Long> {
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

    public static class ItemDto implements AbstractDto<Long> {
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

    public static class ItemEntity implements AbstractEntity<Long> {
        private Long id;
        String name;

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
