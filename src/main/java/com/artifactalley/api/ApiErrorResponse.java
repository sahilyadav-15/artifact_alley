package com.artifactalley.api;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.time.OffsetDateTime;
import java.util.List;

@JacksonXmlRootElement(localName = "error")
public record ApiErrorResponse(OffsetDateTime timestamp, int status, String code, String message,
                               String path, String requestId,
                               @JacksonXmlElementWrapper(localName = "fieldErrors")
                               @JacksonXmlProperty(localName = "fieldError") List<FieldError> fieldErrors) {
    public record FieldError(String field, String message) { }
}
