
package edu.iastate.liberty;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import edu.iastate.datasketches.clustering.Point;

public class FullyOnline {
	
	private int k;
	
	private int n;
	
	private double q_r;
	
	private double f_r;
	
	private List<Point> centers;
	
	private Random randSeed;
	
	/**
	 * initialize parameters
	 * @param k number of centers
	 */
	public FullyOnline(int k) {
		n = 0;
		q_r = 0;
		f_r = 0;
		
		this.k = k;
		centers = new ArrayList<Point>();
		randSeed = new Random();
	}
	
	/** 
	 * upon receiving each point from the stream,
	 * note that the first (k+1) points we receive them anyway,
	 * and use them to initiate w and f_r
	 * @param p
	 */
	public void cluster(Point p) {
		n++;
		
		if (n <= k + 1) {
			centers.add(p);
			// initialize w and f_r
			if (n == k + 1) {
				// find min distance between every two centers
				double minDist = Double.MAX_VALUE;
				for (int i=0; i<centers.size(); i++) {
					Point c = centers.get(i);
					for (int j=i+1; j<centers.size(); j++) {
						minDist = Math.min(minDist, c.euclidDistTo(centers.get(j)));
					}
				}
				double w = minDist * minDist / 2;
				f_r = w / k;
			}
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
			
			// adjust the probability to assign new facility
			if (q_r >= 3 * k * (1 + Math.log(n) / Math.log(2))) {
				q_r = 0;
				f_r *= 2;
			}
		}
	}

	public List<Point> getCenters() {
		return centers;
	}

}
