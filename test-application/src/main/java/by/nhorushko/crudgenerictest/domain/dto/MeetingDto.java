package by.nhorushko.crudgenerictest.domain.dto;

import by.nhorushko.crudgeneric.flex.model.AbstractDto;
import by.nhorushko.crudgenerictest.domain.entity.MeetingStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MeetingDto implements AbstractDto<Long> {
    private Long id;
    private String title;
    private MeetingStatus status;
    private Instant startTime;
}
