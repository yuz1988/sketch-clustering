package edu.iastate.datasketches.clustering;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.math3.util.Pair;


public class Bucket {
	
	public int m;
	
	// Bucket B_0 can store any number between 0 and m points,
	// for i>=1, bucket B_i is either empty or contains exactly m points.
	// Coreset is a list of weighted data points.
	public List<Point> coreset;
	
	public Bucket(int m) {
		this.m = m;
		this.coreset = new ArrayList<>();
	}
	
	public Bucket(int m, List<Point> coreset) {
		this.m = m;
		// shallow copy
		this.coreset = coreset;
	}
	
	/**
	 * clone bucket b to return a new bucket
	 * @param b
	 */
	public Bucket(Bucket b) {
		this.m = b.m;
		this.coreset = new ArrayList<>();
		// deep copy
		for (Point p : coreset) {
			this.coreset.add(new Point(p));
		}
	}
	
	/**
	 * add a new point to the coreset
	 * @param p new point
	 */
	public void addPoint(Point p) {
		coreset.add(p);
	}
	
	/**
	 * get coreset size
	 * @return
	 */
	public int coresetSize() {
		return coreset.size();
	}
	
	/**
	 * Merge the coreset in this bucket (size m) with other buckets (coresets)
	 * 
	 * @return a new bucket with merged coreset (size m)
	 */
	public Bucket mergeBuckets(List<Bucket> bucketList) {
		// the coreset in this bucket
		List<Point> unionSet = coreset;
		// union all the coresets in the bucketList
		for (Bucket b : bucketList) {
			unionSet.addAll(b.coreset);
		}
		List<Point> mergedCoreset = KMeansPlusPlus.fastSeeding(unionSet, m, new Random());
		Bucket mergedBucket = new Bucket(m, mergedCoreset);
		return mergedBucket;
	}
	
	
	/**
	 * Merge the coreset in this bucket_0 (size 0 to m-1) with another bucket_0
	 * 
	 * @param anotherBucket
	 * @return a pair of two buckets: a new bucket_0 (size 0 to m-1) and the
	 *         "carry" bucket (size m)
	 */
	public Pair<Bucket, Bucket> mergeBucket0(Bucket anotherBucket) {
		// union the coresets in two buckets (deep copy)
		List<Point> unionSet = new ArrayList<>();
		for (Point p : coreset) {
			unionSet.add(new Point(p));
		}
		for (Point p : anotherBucket.coreset) {
			unionSet.add(new Point(p));
		}
		
		// generate two buckets: bucketRemain and bucketCarry, 
		// bucketCarry maybe null if sum of two coresets size is less than m
		Bucket bucketRemain = new Bucket(m);
		Bucket bucketCarry = null;
		if (unionSet.size() < m) {
			bucketRemain.coreset = unionSet;
		}
		else {
			bucketCarry = new Bucket(m);
			for (int i=0; i<unionSet.size(); i++) {
				if (i < m) {
					// add to bucketCarry
					bucketCarry.addPoint(unionSet.get(i));
				}
				else {
					bucketRemain.addPoint(unionSet.get(i));
				}
			}
		}
		
		return new Pair<Bucket, Bucket>(bucketRemain, bucketCarry);
	}
}
