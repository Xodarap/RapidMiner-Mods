
/**
 * Data transport struct. Contains statistics about an example.
 * @author ben
 *
 */
public final class StatisticsData {
	public final double average;
	public final double standardDeviation;
	public final double count;
	public final String attrName;
	
	public StatisticsData(double avg, double stdDev, double cnt, String name){
		average = avg;
		standardDeviation = stdDev;
		count = cnt;
		attrName = name;
	}
	
	@Override
	public String toString(){
			return "Name: " + attrName + " Average: " + average + 
					" Standard Deviation: " + standardDeviation + " Count: " + count;	
	}
}
