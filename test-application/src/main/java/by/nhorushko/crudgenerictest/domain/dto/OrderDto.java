package by.nhorushko.crudgenerictest.domain.dto;

import by.nhorushko.crudgeneric.v2.domain.AbstractDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDto implements AbstractDto<Long> {
    private Long id;
    private String name;
}
