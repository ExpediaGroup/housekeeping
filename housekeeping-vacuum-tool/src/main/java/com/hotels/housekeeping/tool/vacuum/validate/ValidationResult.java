package com.hotels.housekeeping.tool.vacuum.validate;

import java.util.ArrayList;
import java.util.List;

public class ValidationResult {

  private final List<ValidationFailure> validationFailures = new ArrayList<>();

  public void addValidationFailure(ValidationFailure validationFailure) {
    validationFailures.add(validationFailure);
  }

  public boolean isValid() {
    return validationFailures.isEmpty();
  }

  public List<ValidationFailure> getValidationFailures() {
    return validationFailures;
  }

}
