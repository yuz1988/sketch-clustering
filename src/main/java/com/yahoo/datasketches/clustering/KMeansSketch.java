
package com.yahoo.datasketches.clustering;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.math3.util.Pair;

public class KMeansSketch {
	
	public static void main(String[] args) {
		

	}
	
	/**
	 * D^2 sampling acceleration by coreset tree
	 * (StreamKM++: A Clustering Algorithm for Data Streams, by Ackermann et al)
	 * @param points
	 * @param m
	 * @param randSeed
	 * @return
	 */
	public List<Point> coresetTree(List<Point> points, int m, Random randSeed) {
		List<Point> resultSet = new ArrayList<>();
        int numPoints = points.size();
        
		// choose one center uniformly 
        // (TODO according to the point's weight?)
		int firstCenterIndex = randSeed.nextInt(numPoints);
		Point firstCenter = new Point(points.get(firstCenterIndex));
		TreeNode root = new TreeNode(firstCenter); 
		for (int i=0; i<points.size(); i++) {
			if (i == firstCenterIndex) {
				continue;
			}
			// TODO possible to have a shallow copy?
			Point p = new Point(points.get(i));
			Pair<Point, Double> pair = new Pair<>(p, p.euclidDistTo(firstCenter));
			root.members.add(pair);
		}
		root.weight = 0.0;   // 0.0 is dummy weight for root
		resultSet.add(firstCenter);  
		
		// generate 2 to m centers
		while (resultSet.size() < m) {
			// find leaf node
			TreeNode node = root;
			while (node.left != null && node.right != null) {
				double r = randSeed.nextDouble();
				double leftNodeWeight = node.left.weight;
				double rightNodeWeight = node.right.weight;
				// sample by weights of two children nodes
				if (r < (leftNodeWeight / (leftNodeWeight + rightNodeWeight))) {
					node = node.left;
				}
				else {
					node = node.right;
				}
			}
			
			// choose one point in the leaf node P_l based on 
			// the D^2 sampling to the center of P_l
			final Point leafCenter = node.center;
			final List<Pair<Point, Double>> leafPoints = node.members;
			
			// compute weighted-squared-distance to the center
	        double[] minSqDist = new double[leafPoints.size()];
	        for (int i = 0; i < leafPoints.size(); i++) {
	        	Point p = leafPoints.get(i).getFirst();
				double d = leafCenter.euclidDistTo(p);
				minSqDist[i] = d * d * p.weight;
            }
	        double distSqSum = 0.0;
            for (double dist : minSqDist) {
                distSqSum += dist;
            }
            
            // sum through the minSquaredDist, stopping when sum >= r.
            double r = randSeed.nextDouble() * distSqSum;
            int nextCenterIndex = -1;
            double sum = 0.0;
			for (int i=0; i<minSqDist.length; i++) {
				sum += minSqDist[i];
				if (sum >= r) {
					nextCenterIndex = i;
					break;
				}
			}
			// the point wasn't found in the previous for loop, 
			// probably because distances are extremely small.  Just pick
            // the last available point.
			if (nextCenterIndex == -1) {
				nextCenterIndex = minSqDist.length - 1;
			}
			
			
			final Point nextCenter = new Point(leafPoints.get(nextCenterIndex).getFirst());
			resultSet.add(nextCenter);
			
			// generate left child TreeNode with previous leaf center
			TreeNode leftChildNode = new TreeNode(new Point(leafCenter));
			// generate right child TreeNode with new center
			TreeNode rightChildNode = new TreeNode(new Point(nextCenter));
			double leftChildWeight = 0.0;
			double rightChildWeight = 0.0;
			for (int i=0; i<leafPoints.size(); i++) {
				if (i == nextCenterIndex) {
					continue;
				}
				Point p = new Point(leafPoints.get(i).getFirst());
				double cost = leafPoints.get(i).getSecond();
				double dist2NewCenter = p.euclidDistTo(nextCenter);
				double cost2NewCenter = dist2NewCenter * dist2NewCenter;
				if (cost < cost2NewCenter) {
					// add point p to left child TreeNode
					leftChildNode.members.add(new Pair<Point, Double>(p, cost));
					leftChildWeight += cost;
				}
				else {
					// add point p to right child TreeNode
					rightChildNode.members.add(new Pair<Point, Double>(p, cost2NewCenter));
					rightChildWeight += cost2NewCenter;
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
		
		return resultSet;
	}

}
