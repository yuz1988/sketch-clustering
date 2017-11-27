
package com.yahoo.datasketches.clustering;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.math3.exception.NumberIsTooSmallException;
import org.apache.commons.math3.util.Pair;

public class KMeansSketch {
	
	public static void main(String[] args) {
		

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
	public List<Point> coresetTree(final List<Point> points, int m, Random randSeed) {
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
	private int sampleByWeight(double[] nums, double r) {
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
	private void dfs(TreeNode node, final List<Point> resultSet) {
		// leaf node
		if (node.left==null && node.right==null) {
			double weight = 0;
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
