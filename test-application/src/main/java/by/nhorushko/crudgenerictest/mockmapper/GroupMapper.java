package by.nhorushko.crudgenerictest.mockmapper;

import by.nhorushko.crudgeneric.mapper.AbstractMapper;
import by.nhorushko.crudgenerictest.domain.dto.GroupDto;
import by.nhorushko.crudgenerictest.domain.entity.GroupEntity;
import by.nhorushko.crudgenerictest.domain.entity.ItemEntity;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

@Service
public class GroupMapper extends AbstractMapper<GroupEntity, GroupDto> {

    public GroupMapper(ModelMapper modelMapper) {
        super(GroupEntity.class, GroupDto.class, modelMapper);
    }

    @Override
    protected void mapSpecificFields(GroupDto source, GroupEntity destination) {
        if (destination.getItems() != null) {
            for (ItemEntity item : destination.getItems()) {
                item.setGroup(destination);
            }
        }
    }
}
