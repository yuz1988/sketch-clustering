
package com.yahoo.datasketches.clustering;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.util.Pair;

public class TreeNode {
	
	Point center;
	
	List<Pair<Point, Double>> members;
	
	double weight;   // cost of members to center
	
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
