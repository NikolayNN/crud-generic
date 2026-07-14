package by.nhorushko.crudgenerictest.service;

import by.nhorushko.crudgeneric.flex.AbsModelMapper;
import by.nhorushko.crudgeneric.flex.service.AbsFlexServiceExtCRUD;
import by.nhorushko.crudgenerictest.domain.dto.TaskCreateDto;
import by.nhorushko.crudgenerictest.domain.dto.TaskDto;
import by.nhorushko.crudgenerictest.domain.dto.TaskUpdateDto;
import by.nhorushko.crudgenerictest.domain.entity.ProjectEntity;
import by.nhorushko.crudgenerictest.domain.entity.TaskEntity;
import by.nhorushko.crudgenerictest.mapper.TaskExtMapper;
import by.nhorushko.crudgenerictest.repository.TaskRepository;
import org.springframework.stereotype.Service;

@Service
public class TaskServiceExtCRUD extends AbsFlexServiceExtCRUD<Long, TaskEntity, TaskDto, TaskUpdateDto, TaskCreateDto, TaskRepository, Long, ProjectEntity> {

    public TaskServiceExtCRUD(AbsModelMapper mapper, TaskRepository repository, TaskExtMapper extMapper) {
        super(mapper, repository, TaskEntity.class, TaskDto.class, TaskUpdateDto.class, TaskCreateDto.class, extMapper);
    }
}
