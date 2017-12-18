
package edu.iastate.datasketches.clustering;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.util.Pair;


public class KMeansSketch {
	
	// number of cluster centers
	private final int k;
	
	// bucket size
	private final int m;

	// merge threshold
	private final int r;
	
	// number of iterations for each trial
	private final int iters;
		
	// number of trials for computing cluster centers
	private final int trials;

	// coreset tree: each level of the tree is a list of buckets, 
	// each bucket in the coreset tree is with size m
	public List<List<Bucket>> coresetTree;

	// initial bucket: size can be 0 to m-1 (both inclusive)
	public Bucket bucket_0;
		
	public KMeansSketch(int k, int bucketSize, int mergeThreshold, int iters, int trials) {
		this.k = k;
		this.m = bucketSize;
		this.r = mergeThreshold;
		this.iters = iters;
		this.trials = trials;
		this.bucket_0 = new Bucket(m);
		this.coresetTree = new ArrayList<>();
	}
	
	public List<List<Bucket>> getCoresetTree() {
		return coresetTree;
	}
	
	/**
	 * update coreset tree upon every new point
	 * @param p new point
	 */
	public void cluster(Point p) {
		mergeReduce(p);
	}
	
	/**
	 * Compute k centers from the coreset tree
	 * @return list of k points as cluster centers
	 */
	public List<Point> getCenters() {
		List<Point> coresets = new ArrayList<Point>();
		for (List<Bucket> level : coresetTree) {
			for (Bucket b : level) {
				coresets.addAll(b.coreset);
			}
		}
		
		// add coreset in bucket 0
		coresets.addAll(bucket_0.coreset);
		
		// run kmeans++ multiple times to get the best k centers
		return KMeansPlusPlus.multiKMeansPlusPlus(coresets, k, iters, trials);
	}
	
	/**
	 * Runs the merge-reduce clustering algorithm. 
	 * It is like incrementing "one" to a number.
	 * @param p
	 */
	public void mergeReduce(Point p) {
		// add new point to the bucket 0
		bucket_0.addPoint(p);
		// when bucket 0 is full, update the coreset tree
		if (bucket_0.coresetSize() == m) {
			// carry digit
			Bucket bucketCarry = bucket_0;
			// empty bucket 0
			bucket_0 = new Bucket(m);
			
			for (int i=0; i<coresetTree.size(); i++) {
				List<Bucket> currentLevel = coresetTree.get(i);
				// number of buckets at level i is less than (r-1),
				// then no need to increment additionally
				if (currentLevel.size() < (r - 1)) {
					currentLevel.add(bucketCarry);
					return;
				}
				bucketCarry = bucketCarry.mergeBuckets(currentLevel);
				// empty this level (list of buckets)
				currentLevel.clear();
			}
			List<Bucket> nextLevel = new ArrayList<>();
			nextLevel.add(bucketCarry);
			coresetTree.add(nextLevel);
		}
	}
	
	/**
	 * Merge the coreset tree of two kmeans-sketches.
	 * As each coreset tree can be represented by a r-nary number, 
	 * it is like adding up two numbers, digit by digit. 
	 * @param anotherSketch
	 * @return a new kmeans-sketch with the merged coreset tree
	 */
	public KMeansSketch merge(KMeansSketch anotherSketch) {
		int i = 0;
		int j = 0;
		
		KMeansSketch mergedSketch = new KMeansSketch(k, m, r, iters, trials);
		
		// merge two bucket_0's coresets
		Pair<Bucket, Bucket> pairBuckets = bucket_0.mergeBucket0(anotherSketch.bucket_0);
		
		// add bucket_0 to the mergedSketch
		mergedSketch.bucket_0 = pairBuckets.getFirst();
		Bucket bucketCarry = pairBuckets.getSecond();
		
		List<List<Bucket>> anotherCoresetTree = anotherSketch.getCoresetTree();
		//  merge two coreset trees
		while (i < coresetTree.size() || j < anotherCoresetTree.size() || bucketCarry != null) {
			List<Bucket> unionSet = new ArrayList<Bucket>();
			if (i < coresetTree.size()) {
				unionSet.addAll(coresetTree.get(i));
				i++;
			}
			if (j < anotherCoresetTree.size()) {
				unionSet.addAll(anotherCoresetTree.get(j));
				j++;
			}
			if (bucketCarry != null) {
				unionSet.add(bucketCarry);
				bucketCarry = null;
			}
			
			// generate buckets at current level and bucketCarry
			List<Bucket> remainSet = new ArrayList<>();
			List<Bucket> carrySet = new ArrayList<>();
			if (unionSet.size() < r) {
				remainSet = unionSet;
			}
			else {
				for (int l=0; l<unionSet.size(); l++) {
					if (l < r) {
						carrySet.add(unionSet.get(l));
					}
					else {
						remainSet.add(unionSet.get(l));
					}
				}
			}
			
			// deep copy remainSet
			List<Bucket> currentLevel = new ArrayList<>();
			for (Bucket bRemain : remainSet) {
				Bucket b = new Bucket(m);
				for (Point p : bRemain.coreset) {
					b.addPoint(new Point(p));
				}
				currentLevel.add(b);
			}
			mergedSketch.coresetTree.add(currentLevel);
			
			// process carrySet
			if (!carrySet.isEmpty()) {
				bucketCarry = new Bucket(m);
				bucketCarry = bucketCarry.mergeBuckets(carrySet);
			}
		}
		
		return mergedSketch;
	}
	
}
