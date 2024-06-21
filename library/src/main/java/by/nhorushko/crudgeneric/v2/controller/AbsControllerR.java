package by.nhorushko.crudgeneric.v2.controller;

import by.nhorushko.crudgeneric.domain.SettingsVoid;
import by.nhorushko.crudgeneric.exception.AuthenticationException;
import by.nhorushko.crudgeneric.v2.domain.AbstractDto;
import by.nhorushko.crudgeneric.v2.service.AbsServiceR;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;


public abstract class AbsControllerR<ID, DTO extends AbstractDto<ID>, DTO_VIEW, SETTINGS extends SettingsVoid,
        SERVICE extends AbsServiceR<ID, ?, DTO, ?, ?>> {

    protected final SERVICE service;

    public AbsControllerR(SERVICE service) {
        this.service = service;
    }

    @GetMapping("{id}")
    @Operation(summary = "Get by id")
    public ResponseEntity<DTO_VIEW> getById(@PathVariable("id") ID id,
                                            SETTINGS settings,
                                            HttpServletRequest request) {

        checkAccessGetByIdBefore(id, request);
        DTO dto = service.getById(id);
        checkAccessGetByIdAfter(dto, request);
        return okResponse(dto, settings);
    }

    protected ResponseEntity<DTO_VIEW> okResponse(DTO dtoIntermediate, SETTINGS settings) {
        DTO_VIEW dtoView = postHandle(dtoIntermediate, settings);
        return ResponseEntity.ok(dtoView);
    }

    protected ResponseEntity<List<DTO_VIEW>> okResponse(Collection<DTO> list, SETTINGS settings) {
        List<DTO_VIEW> result = list.stream().map(d -> postHandle(d, settings)).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    /**
     * method should convert DTO -> DTO_VIEW
     */
    protected abstract DTO_VIEW postHandle(DTO dto, SETTINGS settings);

    protected List<DTO_VIEW> postHandle(Collection<DTO> dtos, SETTINGS settings) {
        return dtos.stream().map(d -> postHandle(d, settings)).collect(Collectors.toList());
    }

    protected void checkAccessGetByIdBefore(ID id, HttpServletRequest request) throws AuthenticationException {
    }

    protected void checkAccessGetByIdAfter(DTO dto, HttpServletRequest request) throws AuthenticationException {
    }
}
