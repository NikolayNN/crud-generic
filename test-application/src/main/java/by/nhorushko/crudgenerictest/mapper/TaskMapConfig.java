package by.nhorushko.crudgenerictest.mapper;

import by.nhorushko.crudgeneric.flex.AbsModelMapper;
import by.nhorushko.crudgeneric.flex.mapper.composite.AbsFlexMapConfigDefault;
import by.nhorushko.crudgenerictest.domain.dto.TaskCreateDto;
import by.nhorushko.crudgenerictest.domain.dto.TaskDto;
import by.nhorushko.crudgenerictest.domain.dto.TaskUpdateDto;
import by.nhorushko.crudgenerictest.domain.entity.TaskEntity;
import org.springframework.stereotype.Component;

@Component
public class TaskMapConfig extends AbsFlexMapConfigDefault<TaskCreateDto, TaskUpdateDto, TaskDto, TaskEntity> {

    public TaskMapConfig(AbsModelMapper mapper) {
        super(mapper, TaskCreateDto.class, TaskUpdateDto.class, TaskDto.class, TaskEntity.class);
    }

    @Override
    protected TaskDto createReadDtoFromEntity(AbsModelMapper mapper, TaskEntity entity) {
        return new TaskDto(entity.getId(), entity.getTitle());
    }
}
