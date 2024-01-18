package by.nhorushko.crudgeneric.v2.controller;

import by.nhorushko.crudgeneric.domain.SettingsVoid;
import by.nhorushko.crudgeneric.v2.domain.AbstractDto;
import by.nhorushko.crudgeneric.v2.service.AbsServiceRUD;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;



public abstract class AbsControllerRUD<
        ID,
        DTO extends AbstractDto<ID>,
        DTO_VIEW extends DTO,
        SETTINGS extends SettingsVoid,
        SERVICE extends AbsServiceRUD<ID, ?, DTO, ?, ?>>
        extends AbsControllerRU<ID, DTO, DTO_VIEW, SETTINGS, SERVICE> {

    public AbsControllerRUD(SERVICE service) {
        super(service);
    }

    @DeleteMapping("{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") ID id, HttpServletRequest request) {
        checkAccessDeleteBefore(id, request);
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    protected void checkAccessDeleteBefore(ID id, HttpServletRequest request) {

    }
}
