package by.nhorushko.crudgenerictest.service;

import by.nhorushko.crudgeneric.flex.AbsModelMapper;
import by.nhorushko.crudgeneric.flex.pageable.AbsFlexPagingAndSortingService;
import by.nhorushko.crudgeneric.flex.pageable.FilterFields;
import by.nhorushko.crudgenerictest.domain.dto.MeetingDto;
import by.nhorushko.crudgenerictest.domain.entity.MeetingEntity;
import by.nhorushko.crudgenerictest.domain.entity.MeetingStatus;
import by.nhorushko.crudgenerictest.repository.MeetingRepository;
import org.springframework.stereotype.Service;

import static by.nhorushko.filterspecification.FilterOperation.*;

@Service
public class MeetingPageableService extends AbsFlexPagingAndSortingService<Long, MeetingDto, MeetingEntity> {

    public MeetingPageableService(MeetingRepository repository, AbsModelMapper mapper) {
        super(repository, mapper, MeetingDto.class);
    }

    @Override
    protected FilterFields<MeetingEntity> filterFields(FilterFields.Builder<MeetingEntity> f) {
        return f.string("title", CONTAINS)
                .ofEnum("status", MeetingStatus.class, EQUAL, IN)
                .instant("startTime", GREATER_THAN, GREATER_THAN_OR_EQUAL_TO, LESS_THAN, LESSTHAN_OR_EQUAL_TO, BETWEEN)
                .ofLocalDate("day", EQUAL, BETWEEN)
                .ofLong("regionId", "region.id", EQUAL)
                .build();
    }
}
