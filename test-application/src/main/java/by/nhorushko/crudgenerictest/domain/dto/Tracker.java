package by.nhorushko.crudgenerictest.domain.dto;

import by.nhorushko.crudgeneric.v2.domain.AbstractDto;
import lombok.Value;

@Value
public class Tracker implements AbstractDto<Long> {
    Long id;
    String imei;
    String phoneNumber;
}
