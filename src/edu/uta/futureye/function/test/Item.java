package edu.uta.futureye.function.test;

import java.util.List;

public interface Item {
	public void setName(String name);
	public String getName();
	
	/**
	 * �ӷ������ж�����Item�������ġ�������Ĺ�ϵ
	 * @param item
	 * @return 0��� ��-1С�ڣ� 1����
	 */
	public int symCompairTo(Item item);
	
	/**
	 * �Ƿ���Ԫ��һ������£�ÿ��item����ϵ����
	 * �������ʾϵ��ʱ������ʹ��һ��Dummy Item
	 * @return
	 */
	public boolean isDummy();
	
	/**
	 * ����
	 * @return
	 */
	public Item copy();
	
	/**
	 * ��ֵ
	 * @param items
	 * @return
	 */
	public double getValue(Item ...items);
	
	/**
	 * �����Ա���name��һ��
	 * @param name
	 * @return
	 */
	public Item _d(String name);
	
	/**
	 * 
	 * @return
	 */
	public List<Item> getSubItems();
	public void setSubItems(List<Item> items);
}
