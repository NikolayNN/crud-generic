package by.nhorushko.crudgeneric.v2.controller;

import by.nhorushko.crudgeneric.domain.SettingsVoid;
import by.nhorushko.crudgeneric.exception.AuthenticationException;
import by.nhorushko.crudgeneric.v2.domain.AbstractDto;
import by.nhorushko.crudgeneric.v2.service.AbsServiceExtCRUD;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletRequest;

//todo add abstract save чтобы пользователь не забывал добавлять мапинг
public abstract class AbsControllerExtCRUD<
        ID,
        DTO extends AbstractDto<ID>,
        DTO_VIEW extends DTO,
        SETTINGS extends SettingsVoid,
        SERVICE extends AbsServiceExtCRUD<ID, ?, DTO, Ext_ID, ?, ?>,
        Ext_ID
        >
        extends AbsControllerRUD<ID, DTO, DTO_VIEW, SETTINGS, SERVICE> {

    public AbsControllerExtCRUD(SERVICE service) {
        super(service);
    }

    /**
     * Example mapping
     * /mechanism/unit/{id}
     */
    public ResponseEntity<DTO_VIEW> save(Ext_ID relationId,
                                         DTO body,
                                         SETTINGS settings,
                                         HttpServletRequest request) {
        checkAccessSaveBefore(relationId, body, request);
        body = handleBeforeSave(relationId, body, request);
        DTO saved = service.save(relationId, body);
        return okResponse(saved, settings);
    }

    private DTO handleBeforeSave(Ext_ID extId, DTO body, HttpServletRequest request) {
        return body;
    }

    protected abstract void checkAccessSaveBefore(Ext_ID relationId, DTO obj, HttpServletRequest request)
            throws AuthenticationException;
}
