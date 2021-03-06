package edu.uta.futureye.function.basic;

import edu.uta.futureye.core.Element;
import edu.uta.futureye.core.Node;
import edu.uta.futureye.function.MultiVarFunc;
import edu.uta.futureye.function.Variable;
import edu.uta.futureye.function.intf.MathFunc;
import edu.uta.futureye.util.Constant;

/**
 * f(x) = (y2-y1)/(x2-x1)*(x-x1)+y1;
 *
 */
public class FLinear1D  extends MultiVarFunc{
	protected double x1,x2,y1,y2;
	
	public FLinear1D(double x1, double y1, 
			double x2, double y2) {
		super(Constant.x);
		this.x1 = x1;
		this.y1 = y1;

		this.x2 = x2;
		this.y2 = y2;
	}
	
	@Override
	public double apply(Variable v) {
		double x = v.get(getVarNames().get(0));
		return (x-x1)*(y2-y1)/(x2-x1) + y1;
	}

	@Override
	public double apply(Element e, Node n, double... args) {
		return (args[0]-x1)*(y2-y1)/(x2-x1) + y1;
	}

	@Override
	public double apply(double... args) {
		return apply(null, null, args);
	}
	
	@Override
	public MathFunc diff(String varName) {
		if(this.getVarNames().contains(varName))
			return new FC((y2-y1)/(x2-x1));
		else
			return new FC(0.0);
	}
	
	public String toString() {
		return (y2-y1)/(x2-x1)+"*("+getVarNames().get(0)+"-"+x1+")+"+y1;
	}

}
