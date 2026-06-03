package by.nhorushko.crudgenerictest.domain.dto;

import by.nhorushko.crudgeneric.domain.AbstractDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemDto implements AbstractDto {
    private Long id;
    private String title;
}
