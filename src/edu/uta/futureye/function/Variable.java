package edu.uta.futureye.function;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import edu.uta.futureye.core.intf.Point;
import edu.uta.futureye.function.intf.Function;

/**
 * Function arguments (Independent variables of a function)
 * @author liuyueming
 *
 */
public class Variable {
	protected Map<String,Double> values = new LinkedHashMap<String,Double>();
//	protected boolean bApplyRestirct = false;
	//Node Index
	protected int index = 0;
	
	public Variable() {
	}
	public Variable(int index) {
		this.index = index;
	}	
	public Variable(String name, double val) {
		values.put(name, val);
	}
	
	public Variable(VarPair fitst, VarPair ...pairs) {
		values.put(fitst.name, fitst.value);
		for(int i=0;i<pairs.length;i++)
			values.put(pairs[i].name, pairs[i].value);
	}
	
	public double get(String name) {
		return values.get(name);
	}
	
	public Variable(double val) {
		values.put("x", val);
	}
	/**
	 * ������һά�Ա���
	 */
	public double get() {
		return values.values().iterator().next();
	}	
	
	public void set(String name, double val) {
		values.put(name, val);
	}

	public Map<String,Double> getValues() {
		return values;
	}

	public String toString() {
		return values.toString();
	}
	
//	public void applyRestirct(boolean flag) {
//		bApplyRestirct = flag;
//	}
//	
//	/**
//	 * ����������Ա��������ƣ������ά�κ������Ƶ�һά�߽��ϣ�isRestrict()����ʾ������ֵʱ�Ƿ�Ӧ�ø�����
//	 * @return
//	 */
//	public boolean isRestrict() {
//		return bApplyRestirct;
//	}
	
	
	public void setIndex(int index) {
		this.index = index;
	}
	
	public int getIndex() {
		return index;
	}
	
	/**
	 * ����fun���Ա���������Point��ֵ���Լ�index�������Ҫ�Ļ�����
	 * ����һ��Variable�Ķ���
	 * @param fun
	 * @param point
	 * @param index
	 * @return
	 */
	public static Variable createFrom(Function fun, Point point, int index) {
		if(fun == null)
			return null;
		Variable var = new Variable(index);
		if(fun.varNames() != null) {
			int ic = 1;
			for(String vn : fun.varNames()) {
				var.set(vn, point.coord(ic));
				ic++;
			}
		} else {
			//VectorBasedFunction
		}
		return var;
	}
	
	public static void main(String[] args) {
		Variable v1 = new Variable();
		System.out.println(v1);
		Variable v2 = new Variable(new VarPair("x",1.0));
		System.out.println(v2);
		Variable v3 = new Variable(new VarPair("x",1.0),
				new VarPair("y",2.0));
		System.out.println(v3);
	}
	
}
