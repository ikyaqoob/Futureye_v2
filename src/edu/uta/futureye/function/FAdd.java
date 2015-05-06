package edu.uta.futureye.function;

import java.util.Map;

import com.sun.org.apache.bcel.internal.generic.ConstantPoolGen;
import com.sun.org.apache.bcel.internal.generic.DADD;
import com.sun.org.apache.bcel.internal.generic.InstructionConstants;
import com.sun.org.apache.bcel.internal.generic.InstructionFactory;
import com.sun.org.apache.bcel.internal.generic.InstructionHandle;
import com.sun.org.apache.bcel.internal.generic.InstructionList;
import com.sun.org.apache.bcel.internal.generic.MethodGen;

import edu.uta.futureye.function.intf.MathFunc;
import edu.uta.futureye.util.Utils;

public class FAdd extends FBinaryOp {
	public FAdd(MathFunc left, MathFunc right) {
		super(left, right);
		setVarNames(Utils.mergeList(left.getVarNames(), right.getVarNames()));
	}
	
	public double apply(Variable v) {
		return arg1.apply(v) + arg2.apply(v);
	}
		
		@Override
	public double apply(Variable v, Map<Object,Object> cache) {
//基本运算不需要cache，否则计算效率会更低
//			if(cache != null) {
//				Double v1, v2;
//				v1 = cache.get(f1);
//				if(v1 == null) {
//					v1 = f1.value(v,cache);
//					cache.put(f1, v1);
//				}
//				v2 = cache.get(f2);
//				if(v2 == null) {
//					v2 = f2.value(v,cache);
//					cache.put(f2, v2);
//				}
//				return v1 + v2;
//			} else {
//				return value(v);
//			}
		return arg1.apply(v,cache) + arg2.apply(v,cache);
	}

	@Override
	public double[] applyAll(VariableArray v, Map<Object,Object> cache) {
		int len = v.length();
		double[] la = arg1.applyAll(v,cache);
		double[] ra = arg2.applyAll(v,cache);
		for(int i=0;i<len;i++) {
			la[i] += ra[i];
		}
		return la;
	}
		
	@Override
	public MathFunc _d(String varName) {
		return arg2._d(varName).A(arg2._d(varName)).setVarNames(this.getVarNames());
	}
	@Override
	public int getOpOrder() {
		return OP_ORDER3;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(arg1.toString());
		sb.append(" + ");
		sb.append(arg2.toString());
		return sb.toString();
	}

	@Override
	public InstructionHandle bytecodeGen(MethodGen mg, ConstantPoolGen cp,
			InstructionFactory factory, InstructionList il,
			Map<String, Integer> argsMap, int argsStartPos) {
		arg1.bytecodeGen(mg, cp, factory, il, argsMap, argsStartPos);
		arg2.bytecodeGen(mg, cp, factory, il, argsMap, argsStartPos);
		return il.append(InstructionConstants.DADD);
	}
}