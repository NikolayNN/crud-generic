package by.nhorushko.crudgenerictest.controller;

import by.nhorushko.crudgeneric.flex.exception.FilterValidationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TestExceptionHandler {

    @ExceptionHandler(FilterValidationException.class)
    public ResponseEntity<String> handleFilterValidation(FilterValidationException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}
