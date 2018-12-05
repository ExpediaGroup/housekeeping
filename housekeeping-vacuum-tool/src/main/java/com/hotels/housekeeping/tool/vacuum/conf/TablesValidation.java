package com.hotels.housekeeping.tool.vacuum.conf;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties
public class TablesValidation {

  private final static List<String> DEFAULT_PROPERTY_EXISTS = new ArrayList<>();
  /**
   * Circus Train legacy since Vacuum Tool came out of Circus Train we check for this property by default. Just to be
   * extra careful.
   */
  static {
    DEFAULT_PROPERTY_EXISTS.add("com.hotels.bdp.circustrain.replication.event");
  }

  private List<String> checkPropertyExists = DEFAULT_PROPERTY_EXISTS;

  public List<String> getCheckPropertyExists() {
    return checkPropertyExists;
  }

  public void setCheckPropertyExists(List<String> checkPropertyExists) {
    this.checkPropertyExists = checkPropertyExists;
  }

}
