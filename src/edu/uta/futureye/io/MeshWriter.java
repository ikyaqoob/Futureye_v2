package edu.uta.futureye.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.Calendar;

import edu.uta.futureye.algebra.intf.Vector;
import edu.uta.futureye.core.Element;
import edu.uta.futureye.core.Mesh;
import edu.uta.futureye.core.Node;
import edu.uta.futureye.util.container.ElementList;
import edu.uta.futureye.util.container.NodeList;

/**
 * Write .dat file for tecplot
 * 
 * @author yuemingl
 *
 */
public class MeshWriter {
	Mesh mesh = null;
	
	public MeshWriter(Mesh mesh) {
		this.mesh = mesh;
	}
	
	public void writeTechplot(String fileName, Vector u, Vector ...us) {
		FileOutputStream out;
		try {
			File file = new File(fileName);
			out = new FileOutputStream(file);
			OutputStreamWriter writer = new OutputStreamWriter(out, "UTF-8");
			PrintWriter br = new PrintWriter(writer);
			
			NodeList nodes = mesh.getNodeList();
			ElementList elements = mesh.getElementList();
			int nNode = nodes.size();
			int nElement = elements.size();
			//找到包含结点最多的单元的结点数目，因为有些网格是混合单元网格
			int nMaxNodes = 0;
			for(int i=1;i<=elements.size();i++) {
				if(elements.at(i).nodes.size() > nMaxNodes)
					nMaxNodes = elements.at(i).nodes.size();
			}
			int dim = nodes.at(1).dim();
			
			String [] VNs ={"V","W","U4","U5","U6","U7","U8","U9"};
			if(dim == 1) {
				StringBuilder sb = new StringBuilder();
					for(int i=1;i<=nNode;i++) {
						Node node = nodes.at(i);
						sb.delete(0, sb.length());
						sb.append("   ");
						for(int ui=0;ui<us.length;ui++)
							sb.append(String.format("%f    ", us[ui].get(i)));
						br.println(String.format("%f    %f %s", 
								node.coord(1),
								u.get(i),
								sb.toString()));	
				}
			} else if(dim == 2) { //二维单元
				if(nMaxNodes % 3 == 0) {
					StringBuilder sb = new StringBuilder();
					sb.append("VARIABLES=\"X\",\"Y\",\"U\"");
					for(int ui=0;ui<us.length;ui++)
						sb.append(String.format(",\"%s\"",VNs[ui]));
					br.println(sb.toString());
					
					if(nMaxNodes == 3)
						br.println(String.format("ZONE F=FEPOINT ET=TRIANGLE N=%d E=%d",nNode,nElement));
					else if(nMaxNodes == 6)
						br.println(String.format("ZONE F=FEPOINT ET=TRIANGLE N=%d E=%d",nNode,4*nElement));
						
					for(int i=1;i<=nNode;i++) {
						Node node = nodes.at(i);
						sb.delete(0, sb.length());
						sb.append("   ");
						for(int ui=0;ui<us.length;ui++)
							sb.append(String.format("%f    ", us[ui].get(i)));
						br.println(String.format("%f    %f    %f %s", 
								node.coord(1),
								node.coord(2),
								u.get(i),
								sb.toString()));	
					}
					for(int i=1;i<=nElement;i++) {
						Element e = elements.at(i);
						if(e.nodes.size() == 3) {
							br.println(String.format("%d    %d    %d", 
									e.nodes.at(1).globalIndex,
									e.nodes.at(2).globalIndex,
									e.nodes.at(3).globalIndex
									));
						} else if(e.nodes.size() == 6) {
							br.println(String.format("%d    %d    %d", 
									e.nodes.at(1).globalIndex,
									e.nodes.at(4).globalIndex,
									e.nodes.at(6).globalIndex
									));						
							br.println(String.format("%d    %d    %d", 
									e.nodes.at(2).globalIndex,
									e.nodes.at(5).globalIndex,
									e.nodes.at(4).globalIndex
									));	
							br.println(String.format("%d    %d    %d", 
									e.nodes.at(3).globalIndex,
									e.nodes.at(6).globalIndex,
									e.nodes.at(5).globalIndex
									));	
							br.println(String.format("%d    %d    %d", 
									e.nodes.at(4).globalIndex,
									e.nodes.at(5).globalIndex,
									e.nodes.at(6).globalIndex
									));	
						} else {
							System.out.println("Error: TRIANGLE nodes number="+e.nodes.size());
						}
						
					}
				} else if(nMaxNodes % 4 == 0) {
					StringBuilder sb = new StringBuilder();
					sb.append("VARIABLES=\"X\",\"Y\",\"U\"");
					for(int ui=0;ui<us.length;ui++)
						sb.append(String.format(",\"%s\"",VNs[ui]));
					br.println(sb.toString());
					
					if(nMaxNodes == 4)
						br.println(String.format("ZONE F=FEPOINT ET=QUADRILATERAL N=%d E=%d",nNode,nElement));
					else if(nMaxNodes == 8)
						br.println(String.format("ZONE F=FEPOINT ET=QUADRILATERAL N=%d E=%d",nNode,5*nElement));
	
					for(int i=1;i<=nNode;i++) {
						Node node = nodes.at(i);
						sb.delete(0, sb.length());
						sb.append("   ");
						for(int ui=0;ui<us.length;ui++)
							sb.append(String.format("%f    ", us[ui].get(i)));
						br.println(String.format("%f    %f    %f %s", 
								node.coord(1),
								node.coord(2),
								u.get(i),
								sb.toString()));
					}
					for(int i=1;i<=nElement;i++) {
						Element e = elements.at(i);
						if(e.nodes.size() == 4) {
							br.println(String.format("%d    %d    %d    %d", 
									e.nodes.at(1).globalIndex,
									e.nodes.at(2).globalIndex,
									e.nodes.at(3).globalIndex,
									e.nodes.at(4).globalIndex
									));
						} else if(e.nodes.size() == 8) {
							br.println(String.format("%d    %d    %d    %d", 
									e.nodes.at(1).globalIndex,
									e.nodes.at(5).globalIndex,
									e.nodes.at(8).globalIndex,
									e.nodes.at(1).globalIndex
									));						
							br.println(String.format("%d    %d    %d    %d", 
									e.nodes.at(2).globalIndex,
									e.nodes.at(6).globalIndex,
									e.nodes.at(5).globalIndex,
									e.nodes.at(2).globalIndex
									));						
							br.println(String.format("%d    %d    %d    %d", 
									e.nodes.at(3).globalIndex,
									e.nodes.at(7).globalIndex,
									e.nodes.at(6).globalIndex,
									e.nodes.at(3).globalIndex
									));
							br.println(String.format("%d    %d    %d    %d", 
									e.nodes.at(4).globalIndex,
									e.nodes.at(8).globalIndex,
									e.nodes.at(7).globalIndex,
									e.nodes.at(4).globalIndex
									));
							br.println(String.format("%d    %d    %d    %d", 
									e.nodes.at(5).globalIndex,
									e.nodes.at(6).globalIndex,
									e.nodes.at(7).globalIndex,
									e.nodes.at(8).globalIndex
									));
						} else if(e.nodes.size() == 3) {
							br.println(String.format("%d    %d    %d    %d", 
									e.nodes.at(1).globalIndex,
									e.nodes.at(2).globalIndex,
									e.nodes.at(3).globalIndex,
									e.nodes.at(1).globalIndex
									));
						} else {
							System.out.println("Error: QUADRILATERAL nodes number="+e.nodes.size());
						}
						
					}				
				}
			} else if(dim == 3) { //三维单元
				StringBuilder sb = new StringBuilder();
				sb.append("VARIABLES=\"X\",\"Y\",\"Z\",\"U\"");
				for(int ui=0;ui<us.length;ui++)
					sb.append(String.format(",\"%s\"",VNs[ui]));
				br.println(sb.toString());
				
				//四面体单元
				if(nMaxNodes == 4)
					br.println(String.format("ZONE F=FEPOINT ET=TETRAHEDRON N=%d E=%d",nNode,nElement));
				else if(nMaxNodes == 8)
					br.println(String.format("ZONE F=FEPOINT ET=BRICK N=%d E=%d",nNode,nElement));
				for(int i=1;i<=nNode;i++) {
					Node node = nodes.at(i);
					sb.delete(0, sb.length());
					sb.append("   ");
					for(int ui=0;ui<us.length;ui++)
						sb.append(String.format("%f    ", us[ui].get(i)));
					br.println(String.format("%f    %f    %f    %f %s", 
							node.coord(1),
							node.coord(2),
							node.coord(3),
							u.get(i),
							sb.toString()));			
				}
				for(int i=1;i<=nElement;i++) {
					Element e = elements.at(i);
					if(e.nodes.size() == 4) {
						br.println(String.format("%d    %d    %d    %d", 
								e.nodes.at(1).globalIndex,
								e.nodes.at(2).globalIndex,
								e.nodes.at(3).globalIndex,
								e.nodes.at(4).globalIndex
								));
					} else if(e.nodes.size() == 8) {
						br.println(String.format("%d    %d    %d    %d    %d    %d    %d    %d", 
								e.nodes.at(1).globalIndex,
								e.nodes.at(2).globalIndex,
								e.nodes.at(3).globalIndex,
								e.nodes.at(4).globalIndex,
								e.nodes.at(5).globalIndex,
								e.nodes.at(6).globalIndex,
								e.nodes.at(7).globalIndex,
								e.nodes.at(8).globalIndex
								));						
					}
				}
				
			}
			br.close();
			out.close();
			
		
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 输出一维函数
	 * 
	 * @param fileName
	 * @param x
	 * @param y
	 */
	public static void writeTechplotLine(String fileName, Vector x, Vector ...y) {
		FileOutputStream out;
		try {
			File file = new File(fileName);
			out = new FileOutputStream(file);
			OutputStreamWriter writer = new OutputStreamWriter(out, "UTF-8");
			PrintWriter br = new PrintWriter(writer);
			for(int i=1;i<=x.getDim();i++) {
				br.print(x.get(i));
				for(int j=0;j<y.length;j++) {
					br.print("\t"+y[j].get(i));
				}
				br.println();
			}
				
			br.close();
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 输出二维网格（用于网格加密后，将网格写为文件，可以下次读取使用）
	 * @param fileName
	 */
	public static void write2DMesh(Mesh mesh, String fileName) {
		FileOutputStream out;
		try {
			File file = new File(fileName);
			out = new FileOutputStream(file);
			OutputStreamWriter writer = new OutputStreamWriter(out, "UTF-8");
			PrintWriter br = new PrintWriter(writer);
			
			DateFormat fmt = DateFormat.getDateTimeInstance();
			br.println("#   UCD geometry file from Futureye");
			br.println("#   "+fmt.format(Calendar.getInstance().getTime()));
			br.println("#   ");
			
			NodeList nList = mesh.getNodeList();
			int nN = nList.size();
			ElementList eList = mesh.getElementList();
			int nE = eList.size();
			br.format("%d %d 0 0 0", nN,nE);
			br.println();
			
			for(int i=1;i<=nN;i++) {
				Node node = nList.at(i);
				br.format("%d %f %f", i, node.coord(1),node.coord(2));
				br.println();
			}
			
			for(int i=1;i<=nE;i++) {
				Element e = eList.at(i);
				if(e.nodes.size() == 3) {
					br.format("%d 0 tri %d %d %d", i,
							e.nodes.at(1).globalIndex,
							e.nodes.at(2).globalIndex,
							e.nodes.at(3).globalIndex);
				} else {
					br.format("%d 0 quad %d %d %d %d", i,
							e.nodes.at(1).globalIndex,
							e.nodes.at(2).globalIndex,
							e.nodes.at(3).globalIndex,				
							e.nodes.at(4).globalIndex);					
				}
				br.println();
			}
			
			br.close();
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
