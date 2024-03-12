package by.nhorushko.crudgeneric.v2.domain;

import by.nhorushko.crudgeneric.flex.model.AbstractBaseDto;

/**
 * This interface mark pojo as DTO
 * которые должны содержать в себе ID
 */
public interface AbstractDto<ID>  extends AbstractBaseDto, IdEntity<ID> {
}
