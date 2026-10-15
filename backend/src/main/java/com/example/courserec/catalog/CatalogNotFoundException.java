package com.example.courserec.catalog;

import com.example.courserec.auth.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class CatalogNotFoundException extends ApiException {
    public CatalogNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
