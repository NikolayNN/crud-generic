package by.nhorushko.crudgenerictest.domain.dto;

import by.nhorushko.crudgeneric.flex.model.AbsUpdateDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Deliberately carries only a subset of {@code OrderEntity} fields: neither
 * {@code secretCode} nor {@code lines} are present here. Updating through this
 * DTO must leave those entity fields untouched.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderUpdateDto implements AbsUpdateDto<Long> {
    private Long id;
    private String name;
}
