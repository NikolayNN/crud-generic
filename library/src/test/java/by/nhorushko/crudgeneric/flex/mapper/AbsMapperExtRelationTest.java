package by.nhorushko.crudgeneric.flex.mapper;

import by.nhorushko.crudgeneric.flex.AbsMapper;
import by.nhorushko.crudgeneric.flex.model.AbsCreateDto;
import by.nhorushko.crudgeneric.flex.model.AbstractEntity;
import jakarta.persistence.EntityManager;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AbsMapperExtRelationTest {

    private final EntityManager entityManager = mock(EntityManager.class);
    private final ProjectEntity project = new ProjectEntity();
    private TaskExtMapper extMapper;

    @Before
    public void setUp() {
        when(entityManager.getReference(ProjectEntity.class, 7L)).thenReturn(project);
        MapperRegistry registry = new MapperRegistry(
                List.of(Mapper.of(TaskCreate.class, TaskEntity.class, dto -> new TaskEntity(dto.title))),
                List.of(), List.of());
        extMapper = new TaskExtMapper(new AbsMapper(registry, entityManager));
    }

    @Test
    public void mapCreatesTheEntityAndPassesTheReferenceToSetRelation() {
        TaskEntity task = extMapper.map(7L, new TaskCreate("write"));

        assertEquals("write", task.title);
        assertSame(project, task.project);
    }

    @Test
    public void mapAllLinksEveryEntityToTheSameReference() {
        List<TaskEntity> tasks = extMapper.mapAll(7L, List.of(new TaskCreate("a"), new TaskCreate("b")));

        assertEquals(2, tasks.size());
        assertSame(project, tasks.get(0).project);
        assertSame(project, tasks.get(1).project);
    }

    @Test
    public void mapAllReturnsMutableList() {
        List<TaskEntity> tasks = extMapper.mapAll(7L, List.of(new TaskCreate("a")));

        tasks.add(new TaskEntity("b"));

        assertEquals(2, tasks.size());
    }

    /** setRelation is the consumer's code: the library must never hand it a null target. */
    @Test
    public void mapRejectsNullDtoBeforeSetRelation() {
        NullPointerException e = assertThrows(NullPointerException.class, () -> extMapper.map(7L, null));

        assertEquals("dto", e.getMessage());
        assertEquals(0, extMapper.relationsSet);
    }

    @Test
    public void mapAllRejectsNullElementBeforeSetRelation() {
        NullPointerException e = assertThrows(NullPointerException.class,
                () -> extMapper.mapAll(7L, Arrays.asList(new TaskCreate("a"), null)));

        assertEquals("dto", e.getMessage());
        assertEquals(1, extMapper.relationsSet);
    }

    static class TaskExtMapper extends AbsMapperExtRelation<TaskCreate, TaskEntity, Long, ProjectEntity> {
        int relationsSet;

        TaskExtMapper(AbsMapper mapper) {
            super(mapper, TaskEntity.class, ProjectEntity.class);
        }

        @Override
        protected void setRelation(TaskEntity target, ProjectEntity relation) {
            relationsSet++;
            target.project = relation;
        }
    }

    static class TaskCreate implements AbsCreateDto {
        final String title;

        TaskCreate(String title) {
            this.title = title;
        }
    }

    static class TaskEntity {
        final String title;
        ProjectEntity project;

        TaskEntity(String title) {
            this.title = title;
        }
    }

    static class ProjectEntity implements AbstractEntity<Long> {
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
