package com.github.lb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A round-robin load balancer.
 * 
 * @author gaurav
 */
public class RoundRobinLB implements LoadBalancer {
  private static final Logger logger = LogManager.getLogger(RoundRobinLB.class.getSimpleName());

  private final ReentrantReadWriteLock superLock = new ReentrantReadWriteLock(true);
  private final WriteLock writeLock = superLock.writeLock();
  private final ReadLock readLock = superLock.readLock();

  // ensure both are protected via single-writer principle
  private final List<Node> activeNodes = new ArrayList<>();
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

        // 1. simply circle through the node list
        int nextNode = nodeIndex % activeNodes.size();
        node = activeNodes.get(nextNode);
        logger.info("Selected " + node);

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
    return LBStrategy.ROUND_ROBIN;
  }

}
