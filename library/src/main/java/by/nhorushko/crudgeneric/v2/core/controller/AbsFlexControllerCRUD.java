package by.nhorushko.crudgeneric.v2.core.controller;

import by.nhorushko.crudgeneric.domain.SettingsVoid;
import by.nhorushko.crudgeneric.exception.AuthenticationException;
import by.nhorushko.crudgeneric.v2.core.service.AbsFlexServiceCRUD;
import by.nhorushko.crudgeneric.v2.domain.AbstractDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import javax.servlet.http.HttpServletRequest;

//todo Add @Valid annotation to methods with request body
public abstract class AbsFlexControllerCRUD<
        ID,
        READ_DTO extends AbstractDto<ID>,
        DTO_VIEW extends READ_DTO,
        UPDATE_DTO extends AbstractDto<ID>,
        CREATE_DTO extends AbstractDto<ID>,
        SETTINGS extends SettingsVoid,
        SERVICE extends AbsFlexServiceCRUD<ID, ?, READ_DTO, UPDATE_DTO, CREATE_DTO, ?>>
        extends AbsFlexControllerRUD<ID, READ_DTO, DTO_VIEW, UPDATE_DTO, SETTINGS, SERVICE> {

    public AbsFlexControllerCRUD(SERVICE service) {
        super(service);
    }

    @PostMapping
    public ResponseEntity<DTO_VIEW> save(@RequestBody CREATE_DTO obj,
                                         SETTINGS settings,
                                         HttpServletRequest request) {
        checkAccessSaveBefore(obj, request);
        obj = handleBeforeSave(obj, request, settings);
        READ_DTO saved = service.save(obj);
        return okResponse(saved, settings);
    }

    protected CREATE_DTO handleBeforeSave(CREATE_DTO obj, HttpServletRequest request, SETTINGS settings) {
        return obj;
    }

    protected void checkAccessSaveBefore(CREATE_DTO obj, HttpServletRequest request) throws AuthenticationException {

    }
}
