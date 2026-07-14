package by.nhorushko.crudgenerictest.mapper;

import by.nhorushko.crudgeneric.flex.AbsModelMapper;
import by.nhorushko.crudgeneric.flex.mapper.AbsMapEntityToDto;
import by.nhorushko.crudgenerictest.domain.dto.MeetingDto;
import by.nhorushko.crudgenerictest.domain.entity.MeetingEntity;
import org.springframework.stereotype.Component;

@Component
public class MeetingMapper extends AbsMapEntityToDto<MeetingEntity, MeetingDto> {

    public MeetingMapper(AbsModelMapper mapper) {
        super(mapper, MeetingEntity.class, MeetingDto.class);
    }

    @Override
    protected MeetingDto create(MeetingEntity entity) {
        return new MeetingDto(entity.getId(), entity.getTitle(), entity.getStatus(), entity.getStartTime());
    }
}
