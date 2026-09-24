package by.nhorushko.crudgenerictest.mapper;

import by.nhorushko.crudgeneric.flex.AbsMapper;
import by.nhorushko.crudgeneric.flex.mapper.AbsMapperExtRelation;
import by.nhorushko.crudgenerictest.domain.dto.TaskCreateDto;
import by.nhorushko.crudgenerictest.domain.entity.ProjectEntity;
import by.nhorushko.crudgenerictest.domain.entity.TaskEntity;
import org.springframework.stereotype.Component;

@Component
public class TaskExtMapper extends AbsMapperExtRelation<TaskCreateDto, TaskEntity, Long, ProjectEntity> {

    public TaskExtMapper(AbsMapper mapper) {
        super(mapper, TaskEntity.class, ProjectEntity.class);
    }

    @Override
    protected void setRelation(TaskEntity task, ProjectEntity project) {
        task.setProject(project);
    }
}
