package com.github.lb;

import java.util.function.Function;

/**
 * A skeleton id provider. Note that id's generated by this provider are expected to be immutable.
 * 
 * @author gaurav
 */
public interface IdProvider extends Function<Void, String> {
  @Override
  default String apply(Void blah) {
    return id();
  }

  String id();
}
