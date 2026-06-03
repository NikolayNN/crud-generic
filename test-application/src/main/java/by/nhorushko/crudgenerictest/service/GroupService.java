package by.nhorushko.crudgenerictest.service;

import by.nhorushko.crudgeneric.service.CrudGenericService;
import by.nhorushko.crudgenerictest.domain.dto.GroupDto;
import by.nhorushko.crudgenerictest.domain.entity.GroupEntity;
import by.nhorushko.crudgenerictest.mockmapper.GroupMapper;
import by.nhorushko.crudgenerictest.repository.GroupRepository;
import org.springframework.stereotype.Service;

@Service
public class GroupService extends CrudGenericService<GroupDto, GroupEntity, GroupRepository, GroupMapper> {
    public GroupService(GroupRepository repository, GroupMapper mapper) {
        super(repository, mapper, GroupDto.class, GroupEntity.class);
    }
}
