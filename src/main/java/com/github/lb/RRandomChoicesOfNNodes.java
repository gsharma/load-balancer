package com.github.lb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
// import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * So, the core idea is to select R random nodes from the list of N total nodes behind the load
 * balancer. Then query the "load" on these R nodes and pick the one with the least "load".
 * Depending on the use-case, "load" could be modeled as the number of connections to a node.
 * 
 * A couple of additional constraints to consider:<br/>
 * 
 * 1. "load" in a busy environment is continuously changing. To provide some sanity during
 * selection, pessimistic locking could be employed by the load balancer during the selection and
 * allocation phases<br/>
 * 
 * 2. selection could result in ties with same load. In order to break ties and avoid
 * over-allocation to the first node, a second round of randomization should be performed to pick 1
 * of the T nodes with same load
 * 
 * Note that this algorithm can work well for an HTTP/L7 or an L4 load balancer. It is important to
 * understand the use-case and associated tradeoffs before selecting an algorithm.
 * 
 * @author gaurav
 */
public class RRandomChoicesOfNNodes implements LoadBalancer {
  private static final Logger logger =
      LogManager.getLogger(RRandomChoicesOfNNodes.class.getSimpleName());

  private final ReentrantReadWriteLock superLock = new ReentrantReadWriteLock(true);
  private final WriteLock writeLock = superLock.writeLock();
  private final ReadLock readLock = superLock.readLock();

  // can cause contention but trading off against better spread/randomization
  private final Random randomizer = new Random();

  // both activeNodes and randomChoices could change at runtime
  private final List<Node> activeNodes = new ArrayList<>();
  private int randomChoices;

  // ensure that randomChoices << activeNodes.size()
  public RRandomChoicesOfNNodes(final int randomChoices) {
    validateRandomChoices(randomChoices);
    this.randomChoices = randomChoices;
  }

  @Override
  public Node selectNode() {
    Node node = null;
    if (writeLock.tryLock()) {
      try {
        // 1. select randomChoices number of random nodes
        final Node[] randomNodes = new Node[randomChoices];
        final List<Integer> selectedIndexes = new ArrayList<>(randomChoices);
        for (int iter = 0; iter < randomChoices; iter++) {
          // spin to get all non-unique random nodes to pick from
          int index = randomizer.nextInt(activeNodes.size());
          while (selectedIndexes.contains(index)) {
            index = randomizer.nextInt(activeNodes.size());
          }
          selectedIndexes.add(index);
          randomNodes[iter] = activeNodes.get(index);
        }
        logger.info(
            String.format("%d random nodes: %s", randomChoices, Arrays.deepToString(randomNodes)));

        // 2. sort selected random nodes by load
        Arrays.sort(randomNodes, new Comparator<Node>() {
          @Override
          public int compare(Node one, Node two) {
            return one.getLoad().compareTo(two.getLoad());
          }
        });

        // 3. select the node with least load factor
        node = randomNodes[0];
        logger.info("Selected " + node);

        // TODO handle #2 case of load ties
      } finally {
        writeLock.unlock();
      }
    }
    return node;
  }

  public boolean overrideRandomChoices(final int newRandomChoices) {
    validateRandomChoices(newRandomChoices);
    boolean changed = false;
    if (writeLock.tryLock()) {
      try {
        this.randomChoices = newRandomChoices;
        changed = true;
      } finally {
        writeLock.unlock();
      }
    }
    return changed;
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
  public boolean addNode(final Node node) {
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
  public boolean removeNode(final Node node) {
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

  private static void validateRandomChoices(final int randomChoices) {
    if (randomChoices < 1) {
      throw new IllegalArgumentException("Cannot use a randomChoices < 1");
    }
  }

  @Override
  public LBStrategy getStrategy() {
    return LBStrategy.SELECT_1_OF_R_RANDOM_CHOICES_FROM_N_NODES;
  }

}
