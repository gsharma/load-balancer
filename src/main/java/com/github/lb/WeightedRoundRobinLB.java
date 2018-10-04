package com.github.lb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A weighted round-robin load balancer implementation.
 * 
 * Note that this works in a round-robin fashion decrementing any available weight for the nodes
 * along the way. If in a round, all runtime weights have zeroed out already, that indicates that we
 * have used up all available slots and we reset runtime available capacities equal to the weights
 * of respective nodes.
 * 
 * Important to understand here is the fact that despite having rehydrated or observing allocated
 * capacities of zero, it says little about the serving latency distribution of requests already
 * allocated to available nodes. In a real system, we need to also be cognizant of the active load
 * and drain time for in-flight requests before allocating more load to a node.
 * 
 * @author gaurav
 */
public class WeightedRoundRobinLB implements LoadBalancer {
  private static final Logger logger =
      LogManager.getLogger(WeightedRoundRobinLB.class.getSimpleName());

  private final ReentrantReadWriteLock superLock = new ReentrantReadWriteLock(true);
  private final WriteLock writeLock = superLock.writeLock();
  private final ReadLock readLock = superLock.readLock();

  // ensure both are protected via single-writer principle
  private final List<Node> activeNodes = new ArrayList<>();
  private final Map<Node, Integer> nodeRemainingCapacityMap = new HashMap<>();
  private int nodeIndex;

  @Override
  public Node selectNode() {
    Node node = null;
    if (writeLock.tryLock()) {
      try {
        // 0. short-circuit if just 1 node
        if (activeNodes.size() == 1) {
          return activeNodes.get(0);
        }

        // 1. if all runtimeCapacities are exhausted (0), reset all to allocatedCap
        boolean allDrained = true;
        for (final Map.Entry<Node, Integer> entry : nodeRemainingCapacityMap.entrySet()) {
          if (entry.getValue() > 0) {
            allDrained = false;
            break;
          }
        }
        if (allDrained) {
          // it's possible that a node got removed from the active set, don't rely on the last
          // hydration validity of runtime map
          nodeRemainingCapacityMap.clear();
          for (final Node nodeToRefill : activeNodes) {
            nodeRemainingCapacityMap.put(nodeToRefill, nodeToRefill.getWeight().getWeightValue());
          }
        }

        int remainingCapacity = 0, nextNode = 0;
        while (remainingCapacity == 0) {
          // 2. circle through the node list
          nextNode = nodeIndex % activeNodes.size();
          node = activeNodes.get(nextNode);

          // 3. we already checked that for at least one node, remainingCapacity has not drained to
          // zero; if all nodes had drained, we would have rehydrated them all
          remainingCapacity = nodeRemainingCapacityMap.get(node);

          if (remainingCapacity == 0) {
            nodeIndex = nextNode + 1;
          }
        }

        // 4. now allocate and decrement remainingCapacity
        nodeRemainingCapacityMap.put(node, remainingCapacity - 1);

        logger
            .info(String.format("Selected %s, remainingCapacity:%d", node, remainingCapacity - 1));

        nodeIndex = nextNode + 1;
      } finally {
        writeLock.unlock();
      }
    }
    return node;
  }

  @Override
  public List<Node> listNodes() {
    List<Node> nodes = Collections.emptyList();
    if (readLock.tryLock()) {
      try {
        nodes = Collections.unmodifiableList(activeNodes);
      } finally {
        readLock.unlock();
      }
    }
    return nodes;
  }

  @Override
  public boolean addNode(Node node) {
    boolean added = false;
    if (node == null) {
      throw new IllegalArgumentException("Cannot add a null node");
    }
    if (writeLock.tryLock()) {
      try {
        activeNodes.add(node);
        // start with allocating runtime capacity = node's allocated weight
        nodeRemainingCapacityMap.put(node, node.getWeight().getWeightValue());
        added = true;
        logger.info("Added " + node);
      } finally {
        writeLock.unlock();
      }
    }
    return added;
  }

  @Override
  public boolean removeNode(Node node) {
    boolean removed = false;
    if (node == null) {
      throw new IllegalArgumentException("Cannot remove a null node");
    }
    if (writeLock.tryLock()) {
      try {
        removed = activeNodes.remove(node);
        if (removed) {
          nodeRemainingCapacityMap.remove(node);
          logger.info("Removed " + node);
        }
      } finally {
        writeLock.unlock();
      }
    }
    return removed;
  }

  @Override
  public LBStrategy getStrategy() {
    return LBStrategy.WEIGHTED_ROUND_ROBIN;
  }

}
