package com.github.lb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

/**
 * Tests to check sanity of RRandomChoicesOfNNodes lb strategy.
 * 
 * @author gaurav
 */
public class RRandomChoicesOfNNodesTest {
  private static final Logger logger =
      LogManager.getLogger(RRandomChoicesOfNNodes.class.getSimpleName());

  @Test
  public void testLbSelection() {
    final int randomChoices = 2;
    final LoadBalancer lb = new RRandomChoicesOfNNodes(randomChoices);
    assertEquals(LBStrategy.SELECT_1_OF_R_RANDOM_CHOICES_FROM_N_NODES, lb.getStrategy());

    final IdProvider idProvider = new RandomIdProvider();
    final Random random = new Random();
    final int maxLoad = 1000;

    final int nodes = 20;
    for (int iter = 0; iter < nodes; iter++) {
      Node node = new Node(idProvider);
      node.setLoad(new Load((float) random.nextInt(maxLoad)));
      lb.addNode(node);
    }

    final Map<Node, Integer> nodeSelectionFrequency = new HashMap<>();
    final int runs = 50;
    for (int iter = 0; iter < runs; iter++) {
      Node node = lb.selectNode();
      assertNotNull(node);
      if (nodeSelectionFrequency.containsKey(node)) {
        Integer frequency = nodeSelectionFrequency.get(node);
        nodeSelectionFrequency.put(node, frequency + 1);
      } else {
        nodeSelectionFrequency.put(node, 1);
      }
    }
    StringBuilder builder = new StringBuilder("Node selection distribution::");
    for (Map.Entry<Node, Integer> entry : nodeSelectionFrequency.entrySet()) {
      builder.append("\n\t").append(entry.getKey()).append("::").append(entry.getValue());
    }
    logger.info(builder.toString());
  }

}
