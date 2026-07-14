package by.nhorushko.crudgenerictest.domain.dto;

import by.nhorushko.crudgeneric.flex.model.AbsCreateDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Deliberately carries an {@code id} field: historical clients send new
 * entities with the sentinel id 0, and the create path must treat such a
 * DTO as new (normalising the id to null before persisting).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskCreateDto implements AbsCreateDto {
    private Long id;
    private String title;
}
