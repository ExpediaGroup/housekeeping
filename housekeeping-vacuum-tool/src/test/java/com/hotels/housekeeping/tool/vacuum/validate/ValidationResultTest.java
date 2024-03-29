/**
 * Copyright (C) 2016-2021 Expedia, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
  public void validationResultInvalid() {
    ValidationResult validationResult = new ValidationResult();
    validationResult.addValidationFailure(validationFailure1);
    validationResult.addValidationFailure(validationFailure2);
    assertThat(validationResult.isValid(), is(false));
    assertThat(validationResult.getValidationFailures(), contains(validationFailure1, validationFailure2));
  }

  @Test
  public void validationResultValid() {
    ValidationResult validationResult = new ValidationResult();
    assertThat(validationResult.isValid(), is(true));
    assertThat(validationResult.getValidationFailures().isEmpty(), is(true));
  }

}
