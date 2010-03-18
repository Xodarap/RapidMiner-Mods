import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.preprocessing.transformation.AggregationOperator;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeAttribute;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.math.SignificanceTestResult;

import java.util.ArrayList;
import java. util . List ;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Performs a Welch's T-Test to determine the likelihood that two groups'
 * variances are statistically significant
 * @author Ben West
 *
 */
public class WelchsTTest extends Operator{
	private OperatorDescription desc;
	private InputPort exampleInput = getInputPorts().createPort("Example Set");
	private OutputPort significanceOutput = getOutputPorts().createPort("Significance");
	private OutputPort originalOutput = getOutputPorts().createPort("Original");
	
	/**
	 * Constructor
	 * @param description - Operator description
	 */
	public WelchsTTest(OperatorDescription description ) {
		super( description );
		desc = description;
	}
	
	/**
	 * Main workhorse of the class
	 * @return IOObject[] array:
	 * 			IOObject[0] = TTestResult
	 */
	@SuppressWarnings("deprecation")
	public IOObject[] apply() throws OperatorException {
    	ExampleSet eSet = getInput(ExampleSet.class);    	
    	AggregationOperator op = new AggregationOperator(desc);
    	
    	//We use an aggregation operator to do the hard work
    	SetAggregatorParams(op);
    	String aggregateParam = getParameter("Aggregate");   
    	ExampleSet outputSet = op.apply(eSet);
    	
    	// Attributes we need to parse from the example
    	Attribute groupAttribute = outputSet.getAttributes().get(getParameter(AggregationOperator.PARAMETER_GROUP_BY_ATTRIBUTES));
    	Attribute avgAttribute = outputSet.getAttributes().get("average(" + aggregateParam + ")");
    	Attribute sdAttribute = outputSet.getAttributes().get("standard_deviation(" + aggregateParam + ")");
    	Attribute cntAttribute = outputSet.getAttributes().get("count(" + aggregateParam + ")");
    	
    	//Figure out which example is the "has nominal attribute" one and which one lacks it
    	Example lineOne = outputSet.getExample(0);
    	Example lineTwo = outputSet.getExample(1);
    	Example withGroup = (lineOne.getNominalValue(groupAttribute).equals("true")) ? lineOne : lineTwo;
    	Example withoutGroup = (lineOne.getNominalValue(groupAttribute).equals("false")) ? lineOne : lineTwo;
    	
    	//Create struct representation
    	StatisticsData withGroupStats = new StatisticsData(lineOne.getNumericalValue(avgAttribute), 
    			lineOne.getNumericalValue(sdAttribute), 
				lineOne.getNumericalValue(cntAttribute),
				lineOne.getNominalValue(groupAttribute));
    	StatisticsData withoutGroupStats = new StatisticsData(lineTwo.getNumericalValue(avgAttribute), 
    			lineTwo.getNumericalValue(sdAttribute), 
				lineTwo.getNumericalValue(cntAttribute),
				lineTwo.getNominalValue(groupAttribute));
    	
    	//Actually get the result
    	SignificanceTestResult result = new TTestResult(withGroupStats, withoutGroupStats);
    	return new IOObject[] { result };
    }
	
	/**
	 * Sets up all the parameters for our aggregator. Essentially just passes through the parameters for this
	 * operator
	 * @param op Output parameter; will be provided with the correct parameters
	 * @throws UndefinedParameterError If a parameter hasn't been set in this operator
	 */
	private void SetAggregatorParams(AggregationOperator op) throws UndefinedParameterError {
    	op.setParameter(AggregationOperator.PARAMETER_GROUP_BY_ATTRIBUTES, 
    			getParameter(AggregationOperator.PARAMETER_GROUP_BY_ATTRIBUTES));    	
    	String aggregateParam = getParameter("Aggregate");
    	
    	//We need the avg, std dev and count in order to do our t test
    	String[] avg = {aggregateParam, "average"};
    	String[] sd = {aggregateParam, "standard_deviation"};
    	String[] cnt = {aggregateParam, "count"};
    	List<String[]> params = new ArrayList<String[]>();
    	params.add(avg);
    	params.add(sd);
    	params.add(cnt);
    	op.setListParameter(AggregationOperator.PARAMETER_AGGREGATION_ATTRIBUTES, params);		
    	
    	op.setParameter(AggregationOperator.PARAMETER_IGNORE_MISSINGS, 
				getParameter(AggregationOperator.PARAMETER_IGNORE_MISSINGS));
    	op.setParameter(AggregationOperator.PARAMETER_ONLY_DISTINCT, 
    			getParameter(AggregationOperator.PARAMETER_ONLY_DISTINCT));
	}
	
	/**
	 * Parameters of the operator
	 */
    public List <ParameterType> getParameterTypes() {
    	List <ParameterType> types = super.getParameterTypes();
    	types.add(new ParameterTypeAttribute(AggregationOperator.PARAMETER_GROUP_BY_ATTRIBUTES, "Binominal attribute to use for grouping", 
    			exampleInput));
    	types.add(new ParameterTypeAttribute("Aggregate", "Attribute to calculate the T statistic for for each of the groups",
    			exampleInput));
    	types.add(new ParameterTypeBoolean(AggregationOperator.PARAMETER_ONLY_DISTINCT, 
    					"Indicates if only rows with distinct values for the aggregation attribute should be used for the calculation of the aggregation function.", 
    					false));
    	types.add(new ParameterTypeBoolean(AggregationOperator.PARAMETER_IGNORE_MISSINGS, 
    					"Indicates if missings should be ignored and aggregation should be based only on existing values or not. In the latter case the aggregated value will be missing in the presence of missing values.", 
    					true));
    	return types ;
    }
    
    /**
     * Applies the operator
     */
    @Override
    public final void doWork() throws UserError{
    	
    	//First deliver a clone of input data to the output
		try {
	    	ExampleSet input = (ExampleSet)exampleInput.getData();
			originalOutput.deliver((IOObject) input.clone());
		} catch (OperatorException e) {
			throw new UserError(this, 31415, "Cloning", e.getMessage());
		}
    	
		//Then do the t-test
    	try {
    		IOObject[] result = apply();
			significanceOutput.deliver(result[0]);
		} catch (OperatorException e) {
			throw new UserError(this, 31416, "Significance", e.getMessage());
		}		
    }
    
    public Class [] getInputClasses () {
    	return new Class[] { ExampleSet.class };
    }
    
    public Class [] getOutputClasses() {
    	return new Class[] { SignificanceTestResult.class };
    }
}

