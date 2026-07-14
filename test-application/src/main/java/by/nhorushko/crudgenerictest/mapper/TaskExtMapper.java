package by.nhorushko.crudgenerictest.mapper;

import by.nhorushko.crudgeneric.flex.AbsModelMapper;
import by.nhorushko.crudgeneric.flex.mapper.mapper.AbsMapperExtRelation;
import by.nhorushko.crudgenerictest.domain.dto.TaskCreateDto;
import by.nhorushko.crudgenerictest.domain.entity.ProjectEntity;
import by.nhorushko.crudgenerictest.domain.entity.TaskEntity;
import org.springframework.stereotype.Component;

@Component
public class TaskExtMapper extends AbsMapperExtRelation<TaskCreateDto, TaskEntity, Long, ProjectEntity> {

    public TaskExtMapper(AbsModelMapper mapper) {
        super(mapper, TaskEntity.class, ProjectEntity.class);
    }
}
