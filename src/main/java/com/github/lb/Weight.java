package com.github.lb;

/**
 * Models a comparable weight metric as handled by a Node.
 * 
 * @author gaurav
 */
public class Weight implements Comparable<Weight> {
  private Integer weightValue = 0;

  public Weight(final Integer weightValue) {
    if (weightValue == null || weightValue < 0) {
      throw new IllegalArgumentException("Weight value cannot be null or negative");
    }
    this.weightValue = weightValue;
  }

  @Override
  public int compareTo(final Weight weight) {
    return this.weightValue.compareTo(weight.weightValue);
  }

  public Integer getWeightValue() {
    return weightValue;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("weight:").append(weightValue);
    return builder.toString();
  }

}
