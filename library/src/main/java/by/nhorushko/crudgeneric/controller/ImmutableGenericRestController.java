package by.nhorushko.crudgeneric.controller;

import by.nhorushko.crudgeneric.domain.AbstractDto;
import by.nhorushko.crudgeneric.domain.SettingsVoid;
import by.nhorushko.crudgeneric.exception.AuthenticationException;
import by.nhorushko.crudgeneric.service.ImmutableGenericService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public abstract class ImmutableGenericRestController<
        DTO_INTERMEDIATE extends AbstractDto,
        DTO_VIEW extends AbstractDto,
        SETTINGS extends SettingsVoid,
        CRUD_SERVICE extends ImmutableGenericService<DTO_INTERMEDIATE, ?, ?, ?>> {

    protected final CRUD_SERVICE service;

    public ImmutableGenericRestController(CRUD_SERVICE service) {
        this.service = service;
    }

    @GetMapping("{id}")
    public ResponseEntity<DTO_VIEW> getById(@PathVariable("id") Long id,
                                            SETTINGS settings,
                                            HttpServletRequest request) {

        checkAccessGetByIdBefore(id, request);
        DTO_INTERMEDIATE dto = service.getById(id);
        checkAccessGetByIdAfter(dto, request);
        DTO_VIEW dtoView = postHandle(dto, settings);
        return ResponseEntity.ok(dtoView);
    }

    protected abstract DTO_VIEW postHandle(DTO_INTERMEDIATE dto, SETTINGS settings);

    protected List<DTO_VIEW> postHandle(Collection<DTO_INTERMEDIATE> dtos, SETTINGS settings) {
        return dtos.stream().map(d -> postHandle(d, settings)).collect(Collectors.toList());
    }

    protected void checkAccessGetByIdBefore(Long id, HttpServletRequest request) throws AuthenticationException {
    }

    protected void checkAccessGetByIdAfter(DTO_INTERMEDIATE dto, HttpServletRequest request) throws AuthenticationException {
        checkAccess(request, dto);
    }

    /**
     * ovveride если логика защиты одинакова для всех эндпоинтов
     */
    protected void checkAccess(HttpServletRequest request, DTO_INTERMEDIATE dto) throws AuthenticationException {
    }
}
