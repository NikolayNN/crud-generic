package by.aurorasoft.crudgeneric.controller;

import by.aurorasoft.crudgeneric.domain.AbstractDto;
import by.aurorasoft.crudgeneric.exception.AuthenticationException;
import by.aurorasoft.crudgeneric.service.CrudGenericService;
import org.springframework.beans.BeanUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

public abstract class CrudGenericRestController<
        DTO extends AbstractDto,
        CRUD_SERVICE extends CrudGenericService<DTO, ?, ?, ?>> extends ImmutableGenericRestController<DTO, CRUD_SERVICE>
{

    public CrudGenericRestController(CRUD_SERVICE service) {
        super(service);
    }

    @PostMapping
    public ResponseEntity<DTO> save(@RequestBody DTO obj, HttpServletRequest request) {
        checkAccessSaveBefore(obj, request);
        DTO saved = service.save(obj);
        return ResponseEntity.ok(saved);
    }

    protected void checkAccessSaveBefore(DTO obj, HttpServletRequest request) throws AuthenticationException {
    }

    @PutMapping("{id}")
    public ResponseEntity<DTO> update(@PathVariable("id") Long id,
                                      @RequestBody DTO obj,
                                      HttpServletRequest request) {

        if (!id.equals(obj.getId())) {
            throw new IllegalArgumentException("wrong id");
        }

        checkAccessUpdateBefore(obj, request);

        DTO dbObj = service.getById(id);
        BeanUtils.copyProperties(obj, dbObj, "id");
        DTO saved = service.save(dbObj);

        return ResponseEntity.ok(saved);
    }

    protected void checkAccessUpdateBefore(DTO obj, HttpServletRequest request) {
    }


    @DeleteMapping("{id}")
    public void delete(@PathVariable("id") Long id, HttpServletRequest request) {
        checkAccessDeleteBefore(id, request);
        service.deleteById(id);
    }

    protected void checkAccessDeleteBefore(Long id, HttpServletRequest request) {
    }
}
