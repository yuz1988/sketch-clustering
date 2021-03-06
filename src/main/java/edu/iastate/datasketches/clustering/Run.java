package edu.iastate.datasketches.clustering;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import edu.iastate.liberty.FullyOnline;

public class Run {

	public static void main(String[] args) throws Exception{
		// read points
		String filePath = "E:/dataset/clean/shuttle.txt";
		Scanner sc = new Scanner(new File(filePath));
		List<Point> points = new ArrayList<>();
		while(sc.hasNextLine()) {
			String line = sc.nextLine();
			String[] strs = line.split(",");
			double[] pos = new double[strs.length];
			for (int i=0; i<strs.length; i++) {
				pos[i] = Double.parseDouble(strs[i]);
			}
			Point p = new Point(pos, 1);
			points.add(new Point(p));
		}
		sc.close();
		
		// int[] numCenters = new int[]{20, 40, 60, 80, 100};
		int[] numCenters = new int[]{17, 33, 54, 67, 92};
		for (int k : numCenters) {
			// create model
			// PracticalOnline po = new PracticalOnline(k);
			// KMeansSketch ks = new KMeansSketch(k, 50*k, 2, 15, 15);
			// stream clustering
//			for (Point p : points) {
//				// po.cluster(p);
//				ks.cluster(p);
//			}
			
			System.out.println("clustering input points complete");
			// List<Point> centers = po.getCenters();
			List<Point> centers = KMeansPlusPlus.fastSeeding(points, k);
		    // List<Point> centers = ks.getCenters();
			double cost = computeCost(points, centers);
			System.out.println("k: " + k + " cost: " + cost + " num: " + centers.size());
		}
		
		// coreset tree (acceleration)
//		Long start = System.currentTimeMillis();
//		List<Point> centers = KMeansPlusPlus.fastSeeding(points, 1000, new Random());
//		Long end = System.currentTimeMillis();
//		double time = (end - start) / 1000.0;
//
//		System.out.println("Time cost: " + time + " seconds");
//		double costSketch = computeCost(points, centers);
//		System.out.println("cost: " + costSketch);
		
		
		// kmeans++ seeding (original)
//		Long start = System.currentTimeMillis();
//		List<Point> centers1 = KMeansPlusPlus.seeding(points, 1000, new Random());
//		Long end = System.currentTimeMillis();
//		double time = (end - start) / 1000.0;
//		System.out.println("Time cost: " + time + " seconds");
//		
//		double costSeeding = computeCost(points, centers1);
//		System.out.println("cost: " + costSeeding);
		
	}
	
	private static double computeCost(List<Point> points, List<Point> centers) {
		double cost = 0;
		for (Point p : points) {
			double minCost = Double.MAX_VALUE;
			// find nearest center
			for (Point c : centers) {
				double d = p.euclidDistTo(c);
				minCost = Math.min(minCost, d*d*p.weight);
			}
			cost += minCost;
		}
		return cost;
	}

}
