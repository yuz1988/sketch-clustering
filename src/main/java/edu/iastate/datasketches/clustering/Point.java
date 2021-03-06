
package edu.iastate.datasketches.clustering;

import java.util.Arrays;

import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.ml.clustering.Clusterable;
import org.apache.commons.math3.util.MathArrays;

public class Point implements Clusterable {
	
	public double weight;
	
	public double[] position;
	
	public Point(double[] position, double weight) {
		this.weight = weight;
		this.position = Arrays.copyOf(position, position.length);
	}
	
	public Point(Point p) {
		this.weight = p.weight;
		this.position = Arrays.copyOf(p.position, p.position.length);
	}

	public double euclidDistTo(Point q) throws DimensionMismatchException {
		return MathArrays.distance(position, q.position);
	}

	@Override
	public double[] getPoint() {
		return position;
	}
}
