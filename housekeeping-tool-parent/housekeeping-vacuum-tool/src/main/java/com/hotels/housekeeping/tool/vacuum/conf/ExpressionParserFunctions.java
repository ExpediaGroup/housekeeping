/**
 * Copyright (C) 2017-2018 Expedia Inc.
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
package com.hotels.housekeeping.tool.vacuum.conf;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

/**
 * Contains functions that can be used when parsing expressions (e.g. in configuration files).
 */
public final class ExpressionParserFunctions {

  private ExpressionParserFunctions() {}

  private static DateTime nowInZone(DateTimeZone zone) {
    return DateTime.now(zone);
  }

  public static DateTime nowInZone(String zone) {
    return nowInZone(DateTimeZone.forID(zone));
  }

  public static DateTime nowUtc() {
    return nowInZone(DateTimeZone.UTC);
  }

  public static DateTime nowEuropeLondon() {
    return nowInZone("Europe/London");
  }

  public static DateTime nowAmericaLosAngeles() {
    return nowInZone("America/Los_Angeles");
  }

  public static String zeroPadLeft(int value, int width) {
    return zeroPadLeft(Integer.toString(value), width);
  }

  public static String zeroPadLeft(String value, int width) {
    return StringUtils.leftPad(value, width, '0');
  }

}
