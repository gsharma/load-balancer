package com.github.lb;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Models a node/server/vm/container that can be fronted by a load balancer. The load balancer will
 * need to query attributes of this object and its peers and make an informed decision how to
 * balance and route new incoming load.
 * 
 * @author gaurav
 */
public class Node {
  private final String id;

  // both scalar dimensions (load and weight) are optional
  private final AtomicReference<Load> loadReference = new AtomicReference<>(new Load(0.0f));
  private final AtomicReference<Weight> weightReference = new AtomicReference<>(new Weight(0));

  public Node(final IdProvider idProvider) {
    id = idProvider.id();
  }

  public String getId() {
    return id;
  }

  public Load getLoad() {
    return loadReference.get();
  }

  public void setLoad(final Load load) {
    loadReference.set(load);
  }

  public Weight getWeight() {
    return weightReference.get();
  }

  public void setWeight(final Weight weight) {
    weightReference.set(weight);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((id == null) ? 0 : id.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof Node)) {
      return false;
    }
    Node other = (Node) obj;
    if (id == null) {
      if (other.id != null) {
        return false;
      }
    } else if (!id.equals(other.id)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("Node[id:").append(id);
    builder.append(", ").append(loadReference.get());
    builder.append(", ").append(weightReference.get());
    builder.append("]");
    return builder.toString();
  }

}
