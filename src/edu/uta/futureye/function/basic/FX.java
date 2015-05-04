package edu.uta.futureye.function.basic;

import static com.sun.org.apache.bcel.internal.generic.InstructionConstants.DALOAD;

import java.util.Map;

import com.sun.org.apache.bcel.internal.generic.ALOAD;
import com.sun.org.apache.bcel.internal.generic.ConstantPoolGen;
import com.sun.org.apache.bcel.internal.generic.InstructionFactory;
import com.sun.org.apache.bcel.internal.generic.InstructionHandle;
import com.sun.org.apache.bcel.internal.generic.InstructionList;
import com.sun.org.apache.bcel.internal.generic.MethodGen;
import com.sun.org.apache.bcel.internal.generic.PUSH;

import edu.uta.futureye.function.AbstractMathFun;
import edu.uta.futureye.function.Variable;
import edu.uta.futureye.function.VariableArray;
import edu.uta.futureye.function.intf.MathFun;
import edu.uta.futureye.util.Constant;

/**
 * f(x)=x
 * 
 * @author liuyueming
 *
 */
public class FX extends AbstractMathFun{
	/**
	 * Used to form f(x)=x, instead of construct a new FX object, 
	 * it will be faster and memory saving :)
	 */
	public final static FX fx = new FX(Constant.x); 
	
	/**
	 * Different variable names
	 */
	public final static FX fy = new FX(Constant.y); 
	public final static FX fz = new FX(Constant.z); 
	
	public final static FX fr = new FX(Constant.r); 
	public final static FX fs = new FX(Constant.s); 
	public final static FX ft = new FX(Constant.t); 
	
	protected String varName = null;
	
	/**
	 * Use this to construct f(varName)
	 */
	public FX(String varName) {
		super(varName);
		this.varName = varName;
	}

	@Override
	public MathFun _d(String varName) {
		if(this.varName.equals(varName))
			return FC.C1;
		else
			return FC.C0;
	}

	@Override
	public double apply(Variable v) {
		return v.get(varName);
	}
	
	@Override
	public double apply(Variable v, Map<Object,Object> cache) {
		return v.get(varName);
	}
	
	@Override
	public double[] applyAll(VariableArray v, Map<Object,Object> cache) {
		return v.get(varName);
	}
	
	@Override
	public int getOpOrder() {
		return OP_ORDER0;
	}
	
	@Override
	public MathFun copy() {
		return new FX(this.varName);
	}
	
	@Override
	public String toString() {
		return varName;
	}

	@Override
	public InstructionHandle bytecodeGen(MethodGen mg, ConstantPoolGen cp,
			InstructionFactory factory, InstructionList il,
			Map<String, Integer> argsMap, int argsStartPos) {
		il.append(new ALOAD(argsStartPos));
		il.append(new PUSH(cp, argsMap.get(varName)));
		return il.append(DALOAD);
	}
}
