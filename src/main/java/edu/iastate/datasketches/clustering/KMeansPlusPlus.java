
package edu.iastate.datasketches.clustering;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.math3.exception.NumberIsTooSmallException;
import org.apache.commons.math3.util.Pair;

public class KMeansPlusPlus {

	/**
	 * uniformly select points (un-weighted)
	 * @param pointList
	 * @param m
	 * @param random
	 * @return
	 */
	public static List<Point> randomCenters(final List<Point> pointList, int m, Random random) {
		int numOfPoints = pointList.size();
		int[] pointIndex = new int[numOfPoints];
		for (int i=0; i<numOfPoints; i++) {
			pointIndex[i] = i;
		}
		
		List<Integer> centerIndex = new ArrayList<>();
		for (int i=0; i<m; i++) {
			int index = random.nextInt(numOfPoints-i);
			centerIndex.add(index);
			// swap a[index] and a[numOfPoints-1-i]
			int temp = pointIndex[numOfPoints-1-i];
			pointIndex[numOfPoints-1-i] = pointIndex[index];
			pointIndex[index] = temp;
		}
		
		List<Point> resultList = new ArrayList<>();
		for (int i : centerIndex) {
			resultList.add(new Point(pointList.get(i)));
		}
		return resultList;
	}
	
	/**
     * Use K-means++ to choose the initial centers.
     * @param points the points to choose the initial centers from
     * @return the initial centers (not weighted)
     */
    public static List<Point> seeding(final List<Point> pointList, int m, Random random) {

        // The number of points in the list.
        final int numPoints = pointList.size();
        // Set the corresponding element in this array to indicate when
        // elements of pointList are no longer available.
        final boolean[] taken = new boolean[numPoints];
        // The resulting list of initial centers.
        final List<Point> resultSet = new ArrayList<>();

        // choose first center uniformly at random from points
		double sumOfWeights = 0;
		double[] pointWeights = new double[pointList.size()];
		for (int i=0; i<pointList.size(); i++) {
    		pointWeights[i] = pointList.get(i).weight;
    		sumOfWeights += pointWeights[i];
    	}
		int firstPointIndex = sampleByWeight(pointWeights, random.nextDouble() * sumOfWeights);
        final Point firstPoint = pointList.get(firstPointIndex);
        resultSet.add(new Point(firstPoint));

        // Must mark it as taken
        taken[firstPointIndex] = true;
        // To keep track of the minimum distance squared of elements of
        // pointList to elements of resultSet.
        final double[] minDistSquared = new double[numPoints];

        // Initialize the elements.  Since the only point in resultSet is firstPoint,
        // this is very easy.
        for (int i = 0; i < numPoints; i++) {
            if (i != firstPointIndex) { // That point isn't considered
                double d = firstPoint.euclidDistTo(pointList.get(i));
                minDistSquared[i] = d*d*pointList.get(i).weight;
            }
        }
        while (resultSet.size() < m) {
            // Sum up the squared distances for the points in pointList not
            // already taken.
            double distSqSum = 0.0;
            for (int i = 0; i < numPoints; i++) {
                if (!taken[i]) {
                    distSqSum += minDistSquared[i];
                }
            }

            // Add one new data point as a center. Each point x is chosen with
            // probability proportional to D(x)2
            final double r = random.nextDouble() * distSqSum;

            // The index of the next point to be added to the resultSet.
            int nextPointIndex = -1;

            // Sum through the squared min distances again, stopping when
            // sum >= r.
            double sum = 0.0;
            for (int i = 0; i < numPoints; i++) {
                if (!taken[i]) {
                    sum += minDistSquared[i];
                    if (sum >= r) {
                        nextPointIndex = i;
                        break;
                    }
                }
            }

            // If it's not set to >= 0, the point wasn't found in the previous
            // for loop, probably because distances are extremely small.  Just pick
            // the last available point.
            if (nextPointIndex == -1) {
                for (int i = numPoints - 1; i >= 0; i--) {
                    if (!taken[i]) {
                        nextPointIndex = i;
                        break;
                    }
                }
            }

            // We found one.
            if (nextPointIndex >= 0) {
                final Point p = pointList.get(nextPointIndex);
                resultSet.add(new Point(p));
                // Mark it as taken.
                taken[nextPointIndex] = true;
                if (resultSet.size() < m) {
                    // Now update elements of minDistSquared.  We only have to compute
                    // the distance to the new center to do this.
                    for (int j = 0; j < numPoints; j++) {
                        // Only have to worry about the points still not taken.
                        if (!taken[j]) {
                            double d = p.euclidDistTo(pointList.get(j));
                            double d2 = d * d * pointList.get(j).weight;
                            if (d2 < minDistSquared[j]) {
                                minDistSquared[j] = d2;
                            }
                        }
                    }
                }
            } else {
                // None found --
                // Break from the while loop to prevent
                // an infinite loop.
                break;
            }
        }
        return resultSet;
    }
	
	/**
	 * D^2 sampling acceleration by coreset tree
	 * Note our input points are weighted
	 * (StreamKM++: A Clustering Algorithm for Data Streams, by Ackermann et al)
	 * @param points
	 * @param m
	 * @param randSeed
	 * @return
	 */
	public static List<Point> fastSeeding(final List<Point> points, int m, Random randSeed) {
		if (points.size() < m) {
			throw new NumberIsTooSmallException(points.size(), m, false);
		}
		
		// choose first center uniformly at random from points
        // Note: as each input point is weighted, we sample by each point's weight,
		// so higher weight means higher probability to be selected
		double sumOfWeights = 0;
		double[] pointWeights = new double[points.size()];
		for (int i=0; i<points.size(); i++) {
    		pointWeights[i] = points.get(i).weight;
    		sumOfWeights += pointWeights[i];
    	}
		int firstCenterIndex = sampleByWeight(pointWeights, randSeed.nextDouble() * sumOfWeights);
		Point firstCenter = points.get(firstCenterIndex);
		
		// create root node
		TreeNode root = new TreeNode(firstCenter); 
		double sumOfCost = 0;
		for (int i=0; i<points.size(); i++) {
			if (i == firstCenterIndex) {
				continue;
			}
			// Note: we can have a shallow copy here
			Point p = points.get(i);
			// distance to new center
			double dist2Center = p.euclidDistTo(firstCenter);  
			// weighted-cost (D^2) to new center
			double cost = dist2Center * dist2Center * p.weight;
			sumOfCost += cost;
			Pair<Point, Double> pair = new Pair<>(p, cost);
			root.members.add(pair);
		}
		root.weight = sumOfCost;
		
		// generate 2 to m centers
		int numOfCenters = 1;
		while (numOfCenters < m) {
			// find leaf node
			// TODO add sanity check on node's weight is not 0
			TreeNode node = root;
			while (node.left != null && node.right != null) {
				double leftNodeWeight = node.left.weight;
				double rightNodeWeight = node.right.weight;
				// sample by weights of two child nodes
				if (randSeed.nextDouble() < (leftNodeWeight / (leftNodeWeight + rightNodeWeight))) {
					node = node.left;
				}
				else {
					node = node.right;
				}
			}
			
			// choose one point in the leaf node P_l based on 
			// the D^2 sampling to the center of P_l
			Point leafCenter = node.center;
			List<Pair<Point, Double>> leafPoints = node.members;
			
			// compute weighted-squared-distance to the center,
			// which is equal to the cost to the center and we
			// have already computed
			double distSqSum = node.weight;
	        double[] squaredDist = new double[leafPoints.size()];
	        for (int i = 0; i < leafPoints.size(); i++) {
	        	squaredDist[i] = leafPoints.get(i).getSecond();
            }
            
            // sample next center
            int nextCenterIndex = sampleByWeight(squaredDist, randSeed.nextDouble() * distSqSum);
            // deep copy
			Point nextCenter = leafPoints.get(nextCenterIndex).getFirst();
			
			// generate left child TreeNode with previous leaf center (see Fig 2. in the paper)
			TreeNode leftChildNode = new TreeNode(leafCenter);
			// generate right child TreeNode with new center
			TreeNode rightChildNode = new TreeNode(nextCenter);
			double leftChildWeight = 0;
			double rightChildWeight = 0;
			for (int i=0; i<leafPoints.size(); i++) {
				if (i == nextCenterIndex) {
					continue;
				}
				
				// cost to the previous center is already computed
				Point p = leafPoints.get(i).getFirst();
				double cost = leafPoints.get(i).getSecond();
				// compute weighted-cost of each point to the new center
				double dist2Center = p.euclidDistTo(nextCenter);
				double cost2Center = dist2Center * dist2Center * p.weight;
				if (cost < cost2Center) {
					// add point p to left child TreeNode
					// shallow copy
					leftChildNode.members.add(new Pair<Point, Double>(p, cost));
					leftChildWeight += cost;
				}
				else {
					// add point p to right child TreeNode
					// shallow copy
					rightChildNode.members.add(new Pair<Point, Double>(p, cost2Center));
					rightChildWeight += cost2Center;
				}
			}
			leftChildNode.weight = leftChildWeight;
			rightChildNode.weight = rightChildWeight;
			
			// link tree nodes
			node.left = leftChildNode;
			node.right = rightChildNode;
			leftChildNode.parent = node;
			rightChildNode.parent = node;
			
			// propagate update of weight attribute upwards to node root
			while (node != null) {
				// weight attribute of an inner node is defined as 
				// the sum of the weights of its child nodes
				node.weight = node.left.weight + node.right.weight;
				node = node.parent;
			}
			
			// new center found (Don't forget)
			numOfCenters++;
		}
		
		// all leaf nodes in the tree are centers
		final List<Point> resultSet = new ArrayList<>();
		dfs(root, resultSet);
		return resultSet;
	}

	/**
	 * sum through the nums, stopping when sum >= r.
	 * @param nums
	 * @param r
	 * @return
	 */
	private static int sampleByWeight(double[] nums, double r) {
		double sum = 0.0;
		for (int i=0; i<nums.length; i++) {
			sum += nums[i];
			if (sum >= r) {
				return i;
			}
		}
		// the point wasn't found in the previous for loop, 
		// probably because distances are extremely small.  
		// Just pick the last available point.
		return (nums.length-1);
	}
	
	/**
	 * retrieve all leaf nodes centers
	 * Note: we can not have a shallow copy for returned center
	 * @param node
	 * @param resultSet
	 */
	private static void dfs(TreeNode node, final List<Point> resultSet) {
		// leaf node
		if (node.left==null && node.right==null) {
			double weight = node.center.weight;
			for (Pair<Point, Double> pair : node.members) {
				weight += pair.getFirst().weight;
			}
			final Point center = new Point(node.center.position, weight);
			resultSet.add(center);
			return;
		}
		
		// inner node
		dfs(node.left, resultSet);
		dfs(node.right, resultSet);
	}
}
