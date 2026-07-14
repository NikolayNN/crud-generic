package by.nhorushko.crudgeneric.flex.model;

/**
 * This interface mark pojo as DTO
 * которые должны содержать в себе ID
 */
public interface AbstractDto<ID>  extends AbsBaseDto, IdEntity<ID> {
}
