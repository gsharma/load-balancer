package com.github.lb;

import java.util.List;

/**
 * Skeleton for load balancer's node selection.
 * 
 * @author gaurav
 */
public interface LoadBalancer {

  Node selectNode();

  List<Node> listNodes();

  boolean addNode(Node node);

  boolean removeNode(Node node);

  LBStrategy getStrategy();

}
