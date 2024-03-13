package by.nhorushko.crudgeneric.flex.controller;

import by.nhorushko.crudgeneric.domain.SettingsVoid;
import by.nhorushko.crudgeneric.exception.AuthenticationException;
import by.nhorushko.crudgeneric.flex.service.AbsFlexServiceCRUD;
import by.nhorushko.crudgeneric.flex.model.AbsCreateDto;
import by.nhorushko.crudgeneric.v2.domain.AbstractDto;
import by.nhorushko.crudgeneric.flex.model.AbsUpdateDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

public abstract class AbsFlexControllerCRUD<
        ID,
        READ_DTO extends AbstractDto<ID>,
        DTO_VIEW,
        UPDATE_DTO extends AbsUpdateDto<ID>,
        CREATE_DTO extends AbsCreateDto,
        SETTINGS extends SettingsVoid,
        SERVICE extends AbsFlexServiceCRUD<ID, ?, READ_DTO, UPDATE_DTO, CREATE_DTO, ?>>
        extends AbsFlexControllerRUD<ID, READ_DTO, DTO_VIEW, UPDATE_DTO, SETTINGS, SERVICE> {

    public AbsFlexControllerCRUD(SERVICE service) {
        super(service);
    }

    @PostMapping
    public ResponseEntity<DTO_VIEW> save(@Valid @RequestBody CREATE_DTO obj,
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
