package com.github.lb;

/**
 * Models a comparable load metric as handled by a Node.
 * 
 * @author gaurav
 */
public class Load implements Comparable<Load> {
  private Float loadValue = 0.0f;

  public Load(final Float loadValue) {
    if (loadValue != null) {
      this.loadValue = loadValue;
    }
  }

  @Override
  public int compareTo(final Load load) {
    return this.loadValue.compareTo(load.loadValue);
  }

  public Float getLoadValue() {
    return loadValue;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("load:").append(loadValue);
    return builder.toString();
  }

}
