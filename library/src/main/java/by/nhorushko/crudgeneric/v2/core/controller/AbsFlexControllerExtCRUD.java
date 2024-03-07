package by.nhorushko.crudgeneric.v2.core.controller;

import by.nhorushko.crudgeneric.domain.SettingsVoid;
import by.nhorushko.crudgeneric.exception.AuthenticationException;
import by.nhorushko.crudgeneric.v2.core.service.AbsFlexServiceExtCRUD;
import by.nhorushko.crudgeneric.v2.domain.AbstractDto;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletRequest;

//todo add abstract save чтобы пользователь не забывал добавлять мапинг
public abstract class AbsFlexControllerExtCRUD<
        ID,
        READ_DTO extends AbstractDto<ID>,
        READ_DTO_VIEW extends READ_DTO,
        UPDATE_DTO extends AbstractDto<ID>,
        CREATE_DTO extends AbstractDto<ID>,
        SETTINGS extends SettingsVoid,
        SERVICE extends AbsFlexServiceExtCRUD<ID, ?, READ_DTO, UPDATE_DTO, CREATE_DTO, Ext_ID, ?, ?>,
        Ext_ID
        >
        extends AbsFlexControllerRUD<ID, READ_DTO, READ_DTO_VIEW, UPDATE_DTO, SETTINGS, SERVICE> {

    public AbsFlexControllerExtCRUD(SERVICE service) {
        super(service);
    }

    /**
     * Example mapping
     * /mechanism/unit/{id}
     */
    public ResponseEntity<READ_DTO_VIEW> save(Ext_ID relationId,
                                              CREATE_DTO body,
                                              SETTINGS settings,
                                              HttpServletRequest request) {
        checkAccessSaveBefore(relationId, body, request);
        body = handleBeforeSave(relationId, body, request);
        READ_DTO saved = service.save(relationId, body);
        return okResponse(saved, settings);
    }

    private CREATE_DTO handleBeforeSave(Ext_ID extId, CREATE_DTO body, HttpServletRequest request) {
        return body;
    }

    protected abstract void checkAccessSaveBefore(Ext_ID relationId, CREATE_DTO obj, HttpServletRequest request)
            throws AuthenticationException;
}
