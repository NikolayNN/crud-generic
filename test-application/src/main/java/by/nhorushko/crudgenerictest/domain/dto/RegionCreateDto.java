package by.nhorushko.crudgenerictest.domain.dto;

import by.nhorushko.crudgeneric.flex.model.AbsCreateDto;
import lombok.Value;

/**
 * Carries an id: Region uses assigned ids and the flex create path is an
 * upsert (persistOrMerge) — the id decides insert vs update.
 */
@Value
public class RegionCreateDto implements AbsCreateDto {
    Long id;
    String name;
}
