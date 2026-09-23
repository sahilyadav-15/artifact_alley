package com.artifactalley.artifact;

public final class ArtifactSearchValidationException extends IllegalArgumentException {
    private final String field;

    public ArtifactSearchValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() { return field; }
}
