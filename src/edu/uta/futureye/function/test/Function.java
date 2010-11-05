package edu.uta.futureye.function.test;

public interface Function extends Item {

	/**
	 * �����������ʽ
	 */
	public void createChain();
	
	/**
	 *  ��ȡ���ʽ
	 * @return
	 */
	public Chain getChain();
	
	/**
	 * ���ñ��ʽ
	 */
	public void setChain(Chain chain);
	
	/**
	 * ���Ϻ���
	 * @param f
	 * @return
	 */
	public Function compose(ComposePair ...pairs);
	
	/**
	 * չ������ʽ
	 * @return
	 */
	public Function expand();	
	
	/**
	 * ������ֵ�����Ա�����
	 * @param v
	 * @return
	 */
	public double getValue(Variable ...v);
}
