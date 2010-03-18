

import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.TDistributionImpl;

import com.rapidminer.Process;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.preprocessing.transformation.AggregationOperator;
import com.rapidminer.report.Readable;
import com.rapidminer.tools.math.SignificanceTestResult;


/**
 * This class acts as a glorified struct to hold result data.
 * The T-Statistic is calculated using Welch's formula.
 * The Degrees of freedom is calculated using the Welch-Satterthwaite approximation
 * The probability of the null hypothesis is calculated using the Apache commons statistics package
 * 
 * @author Ben West
 *
 */
public class TTestResult extends SignificanceTestResult implements Readable {
	private static final long serialVersionUID = 8528790154194589881L;
	
	private final double t;
	private final double degreesOfFreedom;
	private final double probability;
	private final StatisticsData dataForGroupOne;
	private final StatisticsData dataForGroupTwo;
	
	/**
	 * Calculates the T-Statistics for two groups
	 * @param groupOne First group
	 * @param groupTwo Second group
	 */
	public TTestResult(StatisticsData groupOne, StatisticsData groupTwo){
		this.t = CalcT(groupOne.average,groupOne.standardDeviation,groupOne.count,
				groupTwo.average,groupTwo.standardDeviation,groupTwo.count);
		this.degreesOfFreedom = CalcDF(groupOne.standardDeviation,groupOne.count,
				groupTwo.standardDeviation, groupTwo.count);

		TDistributionImpl dist = new TDistributionImpl(this.degreesOfFreedom);
		try {
			probability = dist.cumulativeProbability(this.t);
		} catch (MathException e) {
			throw new Error(e.getMessage());
		}
		
		this.dataForGroupOne = groupOne;
		this.dataForGroupTwo = groupTwo;
	}	
	
	/**
	 * Calculates the T-Statistic
	 * @param x1 Average of group one
	 * @param v1 Variance of group one
	 * @param n1 Count of group one
	 * @param x2 Average of group two
	 * @param v2 Variance of group two
	 * @param n2 Count of group two
	 * @return T
	 */
	private static double CalcT(double x1, double v1, double n1,
									double x2, double v2, double n2) {
		double numerator = x1 - x2;
		double denominator = Math.sqrt(v1 / n1 + v2 / n2);
		return numerator / denominator;
	}
	
	/**
	 * Caculates the degrees of freedom. Approximated using the 
	 * Welch-Satterthwaite equation. 
	 * @param sd1 Standard deviation of group one
	 * @param n1 Count of group one
	 * @param sd2 Standard deviation of group two
	 * @param n2 Count of group two
	 * @return
	 */
	private static double CalcDF(double sd1, double n1,
									double sd2, double n2){
		double numerator = Math.pow((sd1/n1) + (sd2/n2), 2);
		double denom1 = Math.pow(sd1, 2) / (Math.pow(n1,2) * (n1 - 1));
		double denom2 = Math.pow(sd2, 2) / (Math.pow(n2,2) * (n2 - 1));
		return numerator / (denom1 + denom2);
	}
	
	/**
	 * Name of the operator
	 */
	@Override
	public String getName() {
		return "T-Test";
	}

	/**
	 * Probability of the null hypothesis
	 */
	@Override
	public double getProbability() {
		return this.probability;
	}

	/**
	 * String format of operator
	 */
	@Override
	public String toString() {
		return "T: " + this.t + 
				" DF: " + this.degreesOfFreedom + 
				" prob: " + this.probability +
				"\n\nGroup one statistics:\n\t" + this.dataForGroupOne.toString() +
				"\nGroup two statistics:\n\t" + this.dataForGroupTwo.toString();
	}

	@Override
	public boolean isInTargetEncoding() {
		return false;
	}

}
