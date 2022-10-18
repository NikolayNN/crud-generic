package by.nhorushko.crudgeneric.v2.controller;

import by.nhorushko.crudgeneric.domain.SettingsVoid;
import by.nhorushko.crudgeneric.exception.AuthenticationException;
import by.nhorushko.crudgeneric.v2.domain.AbstractDto;
import by.nhorushko.crudgeneric.v2.service.AbsServiceCRUD;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import javax.servlet.http.HttpServletRequest;

public abstract class AbsControllerCRUD<
        ID,
        DTO extends AbstractDto<ID>,
        DTO_VIEW extends DTO,
        SETTINGS extends SettingsVoid,
        SERVICE extends AbsServiceCRUD<ID, ?, DTO, ?>>
        extends AbsControllerRUD<ID, DTO, DTO_VIEW, SETTINGS, SERVICE> {

    public AbsControllerCRUD(SERVICE service) {
        super(service);
    }

    @PostMapping
    public ResponseEntity<DTO_VIEW> save(@RequestBody DTO obj,
                                         SETTINGS settings,
                                         HttpServletRequest request) {
        checkAccessSaveBefore(obj, request);
        obj = handleBeforeSave(obj, request, settings);
        DTO saved = service.save(obj);
        return okResponse(saved, settings);
    }

    protected DTO handleBeforeSave(DTO obj, HttpServletRequest request, SETTINGS settings) {
        return obj;
    }

    protected void checkAccessSaveBefore(DTO obj, HttpServletRequest request) throws AuthenticationException {

    }
}