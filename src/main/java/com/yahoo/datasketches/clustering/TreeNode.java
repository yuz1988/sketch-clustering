
package com.yahoo.datasketches.clustering;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.util.Pair;

public class TreeNode {
	
	// cluster center
	Point center;
	
	// <member point, weighted-cost to center>
	List<Pair<Point, Double>> members;
	
	// weighted-cost of all members to center
	double weight;   
	
	TreeNode left;
	
	TreeNode right;
	
	TreeNode parent;
	
	// TODO
	public TreeNode(Point center, List<Point> members, double weight) {
		
	}
	
	public TreeNode(Point center) {
		this.center = new Point(center);
		this.members = new ArrayList<>();
	}
}
