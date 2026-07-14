package by.nhorushko.crudgenerictest.domain.dto;

import by.nhorushko.crudgeneric.flex.model.AbsUpdateDto;
import lombok.Value;

@Value
public class RegionUpdateDto implements AbsUpdateDto<Long> {
    Long id;
    String name;
}
