package by.nhorushko.crudgeneric.flex.controller;

import by.nhorushko.crudgeneric.domain.SettingsVoid;
import by.nhorushko.crudgeneric.flex.service.AbsFlexServiceRUD;
import by.nhorushko.crudgeneric.v2.domain.AbstractDto;
import by.nhorushko.crudgeneric.v2.domain.AbstractUpdateDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

import javax.servlet.http.HttpServletRequest;

public abstract class AbsFlexControllerRUD<
        ID,
        READ_DTO extends AbstractDto<ID>,
        DTO_VIEW extends READ_DTO,
        UPDATE_DTO extends AbstractUpdateDto<ID>,
        SETTINGS extends SettingsVoid,
        SERVICE extends AbsFlexServiceRUD<ID, ?, READ_DTO, UPDATE_DTO, ?>>
        extends AbsFlexControllerRU<ID, READ_DTO, DTO_VIEW, UPDATE_DTO, SETTINGS, SERVICE> {

    public AbsFlexControllerRUD(SERVICE service) {
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
