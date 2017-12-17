
package edu.iastate.liberty;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import edu.iastate.datasketches.clustering.Point;

public class SemiOnline {
	
	private int k;
	
	private long n;
	
	private double q_r;
	
	private double f_r;
	
	private List<Point> centers;
	
	private Random randSeed;
	
	/**
	 * initialize parameters
	 * @param n estimation of number of points
	 * @param w estimation of k-means cost
	 * @param k number of centers
	 */
	public SemiOnline(long n, double w, int k) {
		q_r = 0;
		f_r = w / k * (Math.log(n) / Math.log(2));
		
		this.k = k;
		this.n = n;
		centers = new ArrayList<Point>();
		randSeed = new Random();
	}
	
	/** 
	 * upon receiving each point from the stream
	 * @param p
	 */
	public void cluster(Point p) {
		// initially there is no center
		if (centers.isEmpty()) {
			centers.add(p);
			q_r++;
		}
		else {
			// find nearest center in centers
			double minDist = Double.MAX_VALUE;
			for (Point c : centers) {
				minDist = Math.min(minDist, p.euclidDistTo(c));
			}
			double prob = Math.min(minDist * minDist / f_r, 1);
			if (randSeed.nextDouble() < prob) {
				// add the point p to centers
				centers.add(p);
				q_r++;
			}
		}
		
		// adjust the probability to assign new facility
		if (q_r >= 3 * k * (1 + Math.log(n) / Math.log(2))) {
			q_r = 0;
			f_r *= 2;
		}
	}

	public List<Point> getCenters() {
		return centers;
	}

}
