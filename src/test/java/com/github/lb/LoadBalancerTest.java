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
 * Tests to check sanity and correctness of various lb strategies.
 * 
 * @author gaurav
 */
public class LoadBalancerTest {
  private static final Logger logger = LogManager.getLogger(LoadBalancerTest.class.getSimpleName());

  @Test
  public void testRRandomChoicesOfNNodesLB() {
    final int randomChoices = 2;
    final LoadBalancer lb = new RRandomChoicesOfNNodesLB(randomChoices);
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
    final int rounds = 50;
    for (int iter = 0; iter < rounds; iter++) {
      Node node = lb.selectNode();
      assertNotNull(node);
      if (nodeSelectionFrequency.containsKey(node)) {
        Integer frequency = nodeSelectionFrequency.get(node);
        nodeSelectionFrequency.put(node, frequency + 1);
      } else {
        nodeSelectionFrequency.put(node, 1);
      }
    }
    StringBuilder builder = new StringBuilder("Random-LB node selection distribution::");
    for (Map.Entry<Node, Integer> entry : nodeSelectionFrequency.entrySet()) {
      builder.append("\n\t").append(entry.getKey()).append("::").append(entry.getValue());
    }
    logger.info(builder.toString());
  }

  @Test
  public void testRoundRobinLB() {
    final LoadBalancer lb = new RoundRobinLB();
    assertEquals(LBStrategy.ROUND_ROBIN, lb.getStrategy());

    final IdProvider idProvider = new RandomIdProvider();

    final int nodeCount = 6;
    Node[] nodes = new Node[nodeCount];
    for (int iter = 0; iter < nodeCount; iter++) {
      Node node = new Node(idProvider);
      lb.addNode(node);
      nodes[iter] = node;
    }

    StringBuilder builder = new StringBuilder("Round-Robin-LB node selection distribution::");
    int rounds = nodeCount * 2;
    for (int iter = 0; iter < rounds; iter++) {
      Node selectedNode = lb.selectNode();
      assertEquals(nodes[iter % nodeCount], selectedNode);
      builder.append("\n\t").append(selectedNode);
    }
    logger.info(builder.toString());
  }

  @Test
  public void testWeightedRoundRobinLB() {
    final LoadBalancer lb = new WeightedRoundRobinLB();
    assertEquals(LBStrategy.WEIGHTED_ROUND_ROBIN, lb.getStrategy());

    final IdProvider idProvider = new RandomIdProvider();

    Node node1 = new Node(idProvider);
    node1.setWeight(new Weight(3));
    lb.addNode(node1);

    Node node2 = new Node(idProvider);
    node2.setWeight(new Weight(5));
    lb.addNode(node2);

    Node node3 = new Node(idProvider);
    node3.setWeight(new Weight(7));
    lb.addNode(node3);

    StringBuilder builder =
        new StringBuilder("Weighted-Round-Robin-LB node selection distribution::");
    int rounds = 20;
    for (int iter = 0; iter < rounds; iter++) {
      Node node = lb.selectNode();
      builder.append("\n\t").append(node);

      // node1 is already drained by this time
      if (iter == 9) {
        assertEquals(node2, node);
      }
      // node1 and node2 have both drained
      if (iter == 12 || iter == 13 || iter == 14) {
        assertEquals(node3, node);
      }
      // all nodes drained, we refilled them and started over
      if (iter == 15) {
        assertEquals(node1, node);
      }
      if (iter == 16) {
        assertEquals(node2, node);
      }
      if (iter == 17) {
        assertEquals(node3, node);
      }
    }
    logger.info(builder.toString());
  }

}
