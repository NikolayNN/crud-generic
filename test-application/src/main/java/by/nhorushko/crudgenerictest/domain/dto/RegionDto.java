package by.nhorushko.crudgenerictest.domain.dto;

import by.nhorushko.crudgeneric.flex.model.AbstractDto;
import lombok.Value;

@Value
public class RegionDto implements AbstractDto<Long> {
    Long id;
    String name;
}
