package org.odema.posnew.design.strategy;


import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;


@Getter
@AllArgsConstructor
public class ValidationResult {
    private final boolean valid;
    private final List<String> errors;

    public static ValidationResult success() {
        return new ValidationResult(true, new ArrayList<>());
    }

    public static ValidationResult failure(List<String> errors) {
        return new ValidationResult(false, errors);
    }

    public static ValidationResult failure(String error) {
        List<String> errors = new ArrayList<>();
        errors.add(error);
        return new ValidationResult(false, errors);
    }
}

