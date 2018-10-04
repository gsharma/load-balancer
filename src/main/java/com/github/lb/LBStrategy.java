package com.github.lb;

/**
 * An enumeration of available load balancing strategies.
 * 
 * @author gaurav
 */
public enum LBStrategy {
  SELECT_1_OF_R_RANDOM_CHOICES_FROM_N_NODES, ROUND_ROBIN, WEIGHTED_ROUND_ROBIN;
}
