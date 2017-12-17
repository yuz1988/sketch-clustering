
package edu.iastate.liberty;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;

import edu.iastate.datasketches.clustering.Point;

public class PracticalOnline {
	
	private double k;
	
	private int n;
	
	private double q_r;
	
	private double f_r;
	
	private List<Point> centers;
	
	private Random randSeed;
	
	/**
	 * initialize parameters
	 * @param k number of centers
	 */
	public PracticalOnline(int k_target) {
		n = 0;
		q_r = 0;
		f_r = 0;
		
		// based on the paper, k is set "heurisitc (entirely ad-hoc)"
		this.k = (k_target-15.0) / 5.0;
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
		
		if (n <= k + 10) {
			centers.add(p);
			// initialize w and f_r
			if (n == k + 10) {
				// find 10 smallest squared distances of points 
				// in C (centers) to their closest neighbor
				PriorityQueue<Double> pq = new PriorityQueue<>();
				for (int i=0; i<centers.size(); i++) {
					Point c = centers.get(i);
					double minDist = Double.MAX_VALUE;
					// for each point, compute distance to its
					// closest neighbor
					for (int j=0; j<centers.size(); j++) {
						if (j != i) {
							minDist = Math.min(minDist, c.euclidDistTo(centers.get(j)));
						}
					}
					pq.offer(minDist);
				}
				// poll out the first 10 smallest distance
				// sum of squared distances
				double sum = 0;
				for (int i=0; i<10; i++) {
					double d = pq.poll();
					sum += d * d;
				}
				f_r = sum / 2;
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
			if (q_r >= k) {
				q_r = 0;
				f_r *= 10;
			}
		}
	}

	public List<Point> getCenters() {
		return centers;
	}

}

