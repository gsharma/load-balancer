package com.github.lb;

import java.util.List;

/**
 * Skeleton for a dynamic load balancer.
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
