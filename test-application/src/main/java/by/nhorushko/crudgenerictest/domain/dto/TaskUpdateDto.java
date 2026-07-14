package by.nhorushko.crudgenerictest.domain.dto;

import by.nhorushko.crudgeneric.flex.model.AbsUpdateDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskUpdateDto implements AbsUpdateDto<Long> {
    private Long id;
    private String title;
}
