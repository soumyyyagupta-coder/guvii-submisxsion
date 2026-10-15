package com.example.courserec.catalog;

import com.example.courserec.auth.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class CatalogValidationException extends ApiException {
    public CatalogValidationException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
