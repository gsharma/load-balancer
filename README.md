# Load Balancing Strategies

## Select 1 of R random choices from N nodes
So, the core idea is to select R random nodes from the list of N total nodes behind the load balancer. Then query the "load" on these R nodes and pick the one with the least "load". Depending on the use-case, "load" could be modeled as the number of connections to a node. A couple of additional constraints to consider:
1. "load" in a busy environment is continuously changing. To provide some sanity during selection, pessimistic locking could be employed by the load balancer during the selection and allocation phases
2. selection could result in ties with same load. In order to break ties and avoid over-allocation to the first node, a second round of randomization should be performed to pick 1 of the T nodes with same load

Note that this algorithm can work well for an HTTP/L7 or an L4 load balancer. It is important to understand the use-case and associated tradeoffs before selecting an algorithm.

From the paper:
https://www.eecs.harvard.edu/~michaelm/postscripts/handbook2001.pdf
Suppose that n balls are thrown into n bins, with each ball choosing a bin independently and uniformly at random. Then the maximum load, or the largest number of balls in any bin, is approximately log n / log (log n) with high probability. Now suppose instead that the balls 2 bins chosen independently and uniformly at random, the maximum load is log (log n) / log d + \theta(1) with high probability. The important implication of this result is that even a small amount of choice can lead to drastically diㄦent results in load balancing. Indeed, having just two random choices (i.e., d = 2) yields a large reduction in the maximum load over having one choice, while each additional choice beyond two decreases the maximum load by just a constant factor.

## Round Robin
Basic idea is to round-robin between available nodes (next-in-loop) without giving weight to their respective loads or weights (could imply available capacity). This generally assumes homogeneity with regard to server capacities.

## Weighted Round Robin
Weighted round-robin shares the simplicity of round-robin algorithm but gives due consideration to the fact that a fleet of nodes is not always homogeneous and operators could assign relative weights to nodes to let them service a higher percentage of requests. A second consideration here would be load-shedding wherein a set of nodes need to be taken offline for maintenance - having their weights gradually reduced over time can help with draining their activity before taking them completely out of rotation.

