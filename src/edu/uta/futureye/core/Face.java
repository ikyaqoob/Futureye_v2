package edu.uta.futureye.core;

import java.util.LinkedList;
import java.util.List;

import edu.uta.futureye.core.intf.GeoEntity;
import edu.uta.futureye.util.EdgeList;

public class Face implements GeoEntity {
	public Element owner = null;
	public List<NodeLocal> nodeLocalList = new LinkedList<NodeLocal>();
	
	/**
	 * Face�Լ���ɶ�ά��Ԫ�����ڱ߽���֣�����֣�
	 * @return
	 */
	public Element changeToElement() {
		return null;
	}
	
	public EdgeList getEdges() {
		return null;
	}
	
	public String toString() {
		return nodeLocalList.toString();
	}	
}
