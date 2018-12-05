package com.hotels.housekeeping.tool.vacuum.validate;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ValidationResultTest {

  @Mock
  private ValidationFailure validationFailure1;
  @Mock
  private ValidationFailure validationFailure2;

  @Test
  public void validationResultInvalid() throws Exception {
    ValidationResult validationResult = new ValidationResult();
    validationResult.addValidationFailure(validationFailure1);
    validationResult.addValidationFailure(validationFailure2);
    assertThat(validationResult.isValid(), is(false));
    assertThat(validationResult.getValidationFailures(), contains(validationFailure1, validationFailure2));
  }

  @Test
  public void validationResultValid() throws Exception {
    ValidationResult validationResult = new ValidationResult();
    assertThat(validationResult.isValid(), is(true));
    assertThat(validationResult.getValidationFailures().isEmpty(), is(true));
  }

}
