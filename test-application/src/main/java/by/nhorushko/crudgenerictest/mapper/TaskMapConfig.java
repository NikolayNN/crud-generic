package by.nhorushko.crudgenerictest.mapper;

import by.nhorushko.crudgeneric.flex.AbsMapper;
import by.nhorushko.crudgeneric.flex.mapper.AbsFlexMapConfig;
import by.nhorushko.crudgenerictest.domain.dto.TaskCreateDto;
import by.nhorushko.crudgenerictest.domain.dto.TaskDto;
import by.nhorushko.crudgenerictest.domain.dto.TaskUpdateDto;
import by.nhorushko.crudgenerictest.domain.entity.TaskEntity;
import org.springframework.stereotype.Component;

@Component
public class TaskMapConfig extends AbsFlexMapConfig<TaskCreateDto, TaskUpdateDto, TaskDto, TaskEntity> {

    public TaskMapConfig(AbsMapper mapper) {
        super(mapper, TaskCreateDto.class, TaskUpdateDto.class, TaskDto.class, TaskEntity.class);
    }

    /**
     * Copies the id: the sentinel 0 must become a new row, and a real id must reach the ext create
     * path so that it can reject it.
     */
    @Override
    protected TaskEntity toEntity(TaskCreateDto dto) {
        TaskEntity entity = new TaskEntity();
        entity.setId(dto.getId());
        entity.setTitle(dto.getTitle());
        return entity;
    }

    @Override
    protected void updateEntity(TaskUpdateDto dto, TaskEntity entity) {
        entity.setTitle(dto.getTitle());
    }

    @Override
    protected TaskDto toReadDto(TaskEntity entity) {
        return new TaskDto(entity.getId(), entity.getTitle());
    }
}
