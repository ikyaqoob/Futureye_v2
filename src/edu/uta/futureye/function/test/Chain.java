package edu.uta.futureye.function.test;

import java.util.List;

public interface Chain extends Item{
	public int length();
	
	public void addItem(ItemPair pair);
	public void addAllItem(List<ItemPair> list);
	public void setItem(int index,ItemPair pair);
	public ItemPair getItem(int index);
	public List<ItemPair> getAllItem();
	public void clear();
	
	/**
	 * �ϲ�Chain�е�ͬ����
	 * @param bMergeFunction �����Ƿ�ϲ�Chain�е�Function����
	 */
	public void merge(boolean bMergeFunction);
	
}
