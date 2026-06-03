package by.nhorushko.crudgenerictest.domain.dto;

import by.nhorushko.crudgeneric.domain.AbstractDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupDto implements AbstractDto {
    private Long id;
    private String name;
    private List<ItemDto> items = new ArrayList<>();
}
