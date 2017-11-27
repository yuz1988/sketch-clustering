package edu.iastate.datasketches.clustering;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import org.apache.commons.math3.util.MathArrays;

public class Run {

	public static void main(String[] args) throws Exception{
		Scanner sc = new Scanner(new File("covtype.data"));
		List<Point> points = new ArrayList<>();
		
		while(sc.hasNextLine()) {
			String line = sc.nextLine();
			String[] strs = line.split(",");
			double[] pos = new double[strs.length];
			for (int i=0; i<strs.length; i++) {
				pos[i] = Double.parseDouble(strs[i]);
			}
			Point p = new Point(pos, 1);
			points.add(p);
		}
		sc.close();
		
		System.out.println("reading input points complete");
		KMeansSketch sketch = new KMeansSketch();
		
		// coreset tree (acceleration)
//		Long start = System.currentTimeMillis();
//		List<Point> centers = sketch.coresetTree(points, 1000, new Random());
//		Long end = System.currentTimeMillis();
//		double time = (end - start) / 1000.0;
//		System.out.println("Time cost: " + time + " seconds");
//		
//		double costSketch = computeCost(points, centers);
//		System.out.println("cost: " + costSketch);
		
		
		// kmeans++ seeding (original)
		Long start = System.currentTimeMillis();
		List<Point> centers1 = sketch.randomCenters(points, 1000, new Random());
		Long end = System.currentTimeMillis();
		double time = (end - start) / 1000.0;
		System.out.println("Time cost: " + time + " seconds");
		
		double costSeeding = computeCost(points, centers1);
		System.out.println("cost: " + costSeeding);
		
	}
	
	private static double computeCost(List<Point> points, List<Point> centers) {
		double cost = 0;
		for (Point p : points) {
			double minCost = Double.MAX_VALUE;
			// find nearest center
			for (Point c : centers) {
				double d = MathArrays.distance(p.position, c.position);
				minCost = Math.min(minCost, d*d*p.weight);
			}
			cost += minCost;
		}
		return cost;
	}

}