package edu.uta.futureye.application;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import edu.uta.futureye.algebra.SparseVectorHashMap;
import edu.uta.futureye.algebra.intf.Matrix;
import edu.uta.futureye.algebra.intf.SparseMatrix;
import edu.uta.futureye.algebra.intf.SparseVector;
import edu.uta.futureye.algebra.intf.Vector;
import edu.uta.futureye.algebra.solver.Solver;
import edu.uta.futureye.algebra.solver.external.SolverJBLAS;
import edu.uta.futureye.core.EdgeLocal;
import edu.uta.futureye.core.Element;
import edu.uta.futureye.core.Mesh;
import edu.uta.futureye.core.Node;
import edu.uta.futureye.core.NodeType;
import edu.uta.futureye.core.intf.Assembler;
import edu.uta.futureye.function.MultiVarFunc;
import edu.uta.futureye.function.FMath;
import edu.uta.futureye.function.Variable;
import edu.uta.futureye.function.basic.DiscreteIndexFunction;
import edu.uta.futureye.function.basic.FC;
import edu.uta.futureye.function.basic.FDelta;
import edu.uta.futureye.function.basic.Vector2MathFunc;
import edu.uta.futureye.function.intf.MathFunc;
import edu.uta.futureye.io.MeshReader;
import edu.uta.futureye.lib.assembler.AssemblerScalar;
import edu.uta.futureye.lib.assembler.AssemblerScalarFast;
import edu.uta.futureye.lib.element.FELinearTriangle;
import edu.uta.futureye.lib.weakform.WeakFormL22D;
import edu.uta.futureye.lib.weakform.WeakFormLaplace2D;
import edu.uta.futureye.util.Constant;
import edu.uta.futureye.util.FutureyeException;
import edu.uta.futureye.util.PairDoubleInteger;
import edu.uta.futureye.util.Utils;
import edu.uta.futureye.util.container.ElementList;
import edu.uta.futureye.util.container.NodeList;
import edu.uta.futureye.util.container.ObjList;

public class MouseHeadOld {
	public enum TailType {top,bottom,left,right};
	
	public static String outputFolder = "MouseHead";
	
	//Light source
	public MathFunc delta = null;
	public Variable lightSource = null; //light source position
	
	//Inclusion mu_a
	public MathFunc mu_a = null;
	public double mu_a_bk = 0.1;

	//Inclusion 1/(3*mu_s') = 1.0/30.0 ?
	public MathFunc k = new FC(1.0/50.0);
	
	public double factor = 70000;
	
	ObjList<Vector> tails = new ObjList<Vector>();
	ObjList<String> tailTypes = new ObjList<String>();
	
	public void setDelta(double x,double y) {
		this.lightSource = new Variable();
		this.lightSource.set("x", x);
		this.lightSource.set("y", y);
		delta = new FDelta(this.lightSource,0.01,2e5);
	}
	
	public void setMu_a_Band(double incX, double incBand, double maxMu_a) {
		final double fcx = incX;
		final double fcr = incBand;
		final double fmu_a = maxMu_a;
		mu_a = new MultiVarFunc("x","y"){
			@Override
			public double apply(Variable v) {
				double dx = v.get("x")-fcx;
				if(Math.sqrt(dx*dx) < fcr) {
					double r = fmu_a*Math.cos((Math.PI/2)*Math.sqrt(dx*dx)/fcr); 
					return r<mu_a_bk?mu_a_bk:r;
				}
				else
					return mu_a_bk;
			}
		};
	}

	public void setMu_a(double incX, double incY, double incR, double maxMu_a) {
		final double fcx = incX;
		final double fcy = incY;
		final double fcr = incR;
		final double fmu_a = maxMu_a;
		mu_a = new MultiVarFunc("x","y"){
			@Override
			public double apply(Variable v) {
				double dx = v.get("x")-fcx;
				double dy = v.get("y")-fcy;
				if(Math.sqrt(dx*dx+dy*dy) < fcr) {
					double r = fmu_a*Math.cos((Math.PI/2)*Math.sqrt(dx*dx+dy*dy)/fcr); 
					return r<mu_a_bk?mu_a_bk:r;
				}
				else
					return mu_a_bk;
			}
		};
	}
	
	public Vector solveForwardNeumann(Mesh mesh) {
		//Mark border type
		HashMap<NodeType, MathFunc> mapNTF = new HashMap<NodeType, MathFunc>();
		mapNTF.clear();
		mapNTF.put(NodeType.Robin, null);
		mesh.clearBorderNodeMark();
		mesh.markBorderNode(mapNTF);
		
		WeakFormLaplace2D weakForm = new WeakFormLaplace2D();
		
		//Right hand side
		weakForm.setF(this.delta);
		
		// *** u + u_n = 0, on boundary ***
		weakForm.setParam(
				this.k, this.mu_a, FC.C0, this.k //d==k,q=0 (鍗筹細u_n + u =0)
			);
		
		Assembler assembler = new AssemblerScalar(mesh, weakForm);
		System.out.println("Begin Assemble...solveForwardNeumann");
		assembler.assemble();
		SparseMatrix stiff = assembler.getStiffnessMatrix();
		SparseVector load = assembler.getLoadVector();
		assembler.imposeDirichletCondition(new FC(0.0));
		System.out.println("Assemble done!");

		Solver solver = new Solver();
		Vector u = solver.solveCGS(stiff, load);
		return u;
	}	
	
	public Vector solveForwardDirichlet(Mesh mesh, MathFunc diri) {
		WeakFormLaplace2D weakForm = new WeakFormLaplace2D();
		
		//Right hand side
		weakForm.setF(this.delta);

		weakForm.setParam(
				this.k, this.mu_a, null, this.k //d==k,q=0 (鍗筹細u_n + u =0)
			);
		
		Assembler assembler = new AssemblerScalarFast(mesh, weakForm);
		System.out.println("Begin Assemble...solveForwardDirichlet");
		assembler.assemble();
		SparseMatrix stiff = assembler.getStiffnessMatrix();
		SparseVector load = assembler.getLoadVector();
		//Dirichlet condition
		assembler.imposeDirichletCondition(diri);
		
		System.out.println("Assemble done!");

		Solver solver = new Solver();
		Vector u = solver.solveCGS(stiff, load);
		return u;
	}	
	
	public Vector solveParamInverse(Mesh mesh, Vector U) {
		HashMap<NodeType, MathFunc> mapNTF2 = new HashMap<NodeType, MathFunc>();
		mapNTF2.put(NodeType.Dirichlet, null);
		mesh.clearBorderNodeMark();
		mesh.markBorderNode(mapNTF2);
		
		//Weak form
		WeakFormL22D weakFormL2 = new WeakFormL22D();
		//Right hand side
		weakFormL2.setF(this.delta);
		//Parameters
		weakFormL2.setParam(
				this.k, new Vector2MathFunc(U)
			);
		
		Assembler assembler = new AssemblerScalar(mesh, weakFormL2);
		System.out.println("Begin Assemble...solveParamInverse");
		assembler.assemble();
		Matrix stiff = assembler.getStiffnessMatrix();
		Vector load = assembler.getLoadVector();
		assembler.imposeDirichletCondition(new FC(0.1));
		System.out.println("Assemble done!");
		
		SolverJBLAS solver = new SolverJBLAS();
		Vector u = solver.solveDGESV(stiff, load);
		return u;
	}

	/**
	 * 
	 * @param mesh1
	 * @param mesh2
	 * @param u
	 * @return
	 */
	public Vector extractData(Mesh meshFrom, Mesh meshTo, Vector u) {
		NodeList nodeTo = meshTo.getNodeList();
		int dimTo = nodeTo.size();
		Vector rlt = new SparseVectorHashMap(dimTo);
		for(int i=1;i<=nodeTo.size();i++) {
			Node node = meshFrom.findNode(nodeTo.at(i));
			if(node != null) {
				rlt.set(nodeTo.at(i).globalIndex, u.get(node.globalIndex));
			}
		}
		return rlt;
	}
	
	/**
	 * 鎻愬彇meshOmega1杈圭晫鐐圭殑鍙傛暟鍧愭爣锛屾彁渚涚粰matlab绋嬪簭鐨�
	 * 鈥滄彃鍊肩粨鐐瑰弬鏁板潗鏍�(0->r1->r2->r3->r4(0))鈥�
	 * 
	 *  S1            S6
	 * (r4)0---->----r1
	 *     /          \
	 *    /            \
	 *   鈹�              鈹�
	 *  /                \
	 * r3-------<--------r2
	 * S4                S9
	 */
	public void extractBorderParamData(Mesh omega) {
		ObjList<PairDoubleInteger> rs = new ObjList<PairDoubleInteger>();
		NodeList nodes = omega.getNodeList();
		Node S1 = new Node(0,-0.9,0.8);
		Node S6 = new Node(0,0.9,0.8);
		Node S9 = new Node(0,1.5,-0.4);
		Node S4 = new Node(0,-1.5,-0.4);
		double dis1 = Utils.computeLength(S1,S6);
		double dis2 = dis1 + Utils.computeLength(S6,S9);
		double dis3 = dis2 + Utils.computeLength(S9,S4);
		//double dis4 = dis3 + Utils.computeLength(S4,S1);
		
		for(int i=1;i<=nodes.size();i++) {
			Node node = nodes.at(i);
			if(node.isInnerNode())
				continue;
			//闄ゅ幓灏忕煩褰紙澶栭棶棰樺湪Omega1涓婃眰瑙ｆ椂锛�
			if(Math.abs(node.coord(1)-(-1.6))<Constant.meshEps ||
					Math.abs(node.coord(1)-1.6)<Constant.meshEps ||
					Math.abs(node.coord(2)-(-0.5))<Constant.meshEps ||
					Math.abs(node.coord(2)-0.9)<Constant.meshEps)
				continue;
			//闄ゅ幓澶х煩褰紙澶栭棶棰樺湪Omega11涓婃眰瑙ｏ級
			if(Math.abs(node.coord(1)-(-2.1))<Constant.meshEps ||
					Math.abs(node.coord(1)-2.1)<Constant.meshEps ||
					Math.abs(node.coord(2)-(-1.0))<Constant.meshEps ||
					Math.abs(node.coord(2)-1.4)<Constant.meshEps)
				continue;
			
			PairDoubleInteger di = new PairDoubleInteger();
			di.i = node.getIndex();
			
			if(Utils.isPointOnLineSegment(S1, S6, node)) {//top
				di.d = Utils.computeLength(S1,node);
			} else if (Utils.isPointOnLineSegment(S6, S9, node)) {//right
				di.d = dis1 + Utils.computeLength(S6,node);
			} else if(Utils.isPointOnLineSegment(S9, S4, node)) {//bottom
				di.d = dis2 + Utils.computeLength(S9,node);
			} else if(Utils.isPointOnLineSegment(S4, S1, node)) {//left
				di.d = dis3 + Utils.computeLength(S4,node);
			} else {
				throw new FutureyeException("");
			}
			rs.add(di);
		}
		
		Collections.sort(rs.toList(), new Comparator<PairDoubleInteger>() {
			@Override
			public int compare(PairDoubleInteger o1, PairDoubleInteger o2) {
				if(o1.d>o2.d) return 1;
				else return -1;
			}
			
		});
		for(int i=1;i<=rs.size();i++) {
			System.out.println(rs.at(i).i+" "+rs.at(i).d);
		}
	}
	
	/**
	 * 璇诲彇matlab鎻掑€煎悗鐨勬暟鎹細(globalIndex, value)瀵�
	 * @param srcId
	 * @return
	 */
	public ObjList<PairDoubleInteger> readInterpData(int srcId, int timeId) {
		ObjList<PairDoubleInteger> vs = new ObjList<PairDoubleInteger>();
		FileInputStream in;
		try {
			in = new FileInputStream(
					String.format(".\\"+outputFolder+"\\InterpOut%d_%d.txt", 
							srcId,timeId));

			InputStreamReader reader = new InputStreamReader(in);
			BufferedReader br = new BufferedReader(reader);
	
			String str = null;
			while((str = br.readLine()) != null){
				System.out.println(str);
				if(str.startsWith("#")) continue;
				String[] line = str.split("(\\s)+");
				PairDoubleInteger pair = new PairDoubleInteger();
				pair.i = Integer.parseInt(line[0]);
				pair.d = Double .parseDouble(line[2])*factor;
				vs.add(pair);
			}
			br.close();
			in.close();
			return vs;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	
	/**
	 * 鑾峰彇鍖哄煙鐨勮竟鐣岀粨鐐瑰垪琛紙涓嶰mega2杈圭晫涓€鑷寸殑浠讳綍鍖哄煙锛�
	 * Omega2:  [-1.6,1.6]*[-0.5,0.9]
	 * 
	 * @param type
	 * @param omega2
	 * @return
	 */
	public ObjList<Node> getBorderNodes_Omega2(TailType type, Mesh omega2) {
		NodeList nodes2 = omega2.getNodeList(); 
		ObjList<Node> borderNodes = new ObjList<Node>();
		if(type == TailType.left) {
			for(int i=1;i<=nodes2.size();i++) {
				if( //y=[-0.5,0.9]
					nodes2.at(i).coord(2)-(-0.5)>-Constant.meshEps && 0.9-nodes2.at(i).coord(2)>-Constant.meshEps &&
					//x=-1.6
					Math.abs(nodes2.at(i).coord(1)-(-1.6))<Constant.meshEps
				) {
					borderNodes.add(nodes2.at(i));
					System.out.println(nodes2.at(i));
				}
			}
		} else if(type == TailType.bottom) {
			for(int i=1;i<=nodes2.size();i++) {
				if( //x:[-1.6,1.6]
					nodes2.at(i).coord(1)-(-1.6)>-Constant.meshEps && 1.6-nodes2.at(i).coord(1)>-Constant.meshEps &&
					//y:-0.5
					Math.abs(nodes2.at(i).coord(2)-(-0.5))<Constant.meshEps
				) {
					borderNodes.add(nodes2.at(i));
					System.out.println(nodes2.at(i));
				}
			}
		}  else if(type == TailType.right) {
			for(int i=1;i<=nodes2.size();i++) {
				if( //y=[-0.5,0.9]
					nodes2.at(i).coord(2)-(-0.5)>-Constant.meshEps && 0.9-nodes2.at(i).coord(2)>-Constant.meshEps &&
					//x=1.6
					Math.abs(nodes2.at(i).coord(1)-1.6)<Constant.meshEps
					) {
						borderNodes.add(nodes2.at(i));
						System.out.println(nodes2.at(i));
					}
			}
		}  else if(type == TailType.top) {
			for(int i=1;i<=nodes2.size();i++) {
				if( //x:[-1.6,1.6]
					nodes2.at(i).coord(1)-(-1.6)>-Constant.meshEps && 1.6-nodes2.at(i).coord(1)>-Constant.meshEps &&
					//y:0.9
					Math.abs(nodes2.at(i).coord(2)-(0.9))<Constant.meshEps
				) {
					borderNodes.add(nodes2.at(i));
					System.out.println(nodes2.at(i));
				}
			}
		} else {
			throw new FutureyeException(""+type);
		}
		return borderNodes;
	}
	
	/**
	 * 璁＄畻鍏夋簮鍦ㄥ彸杈圭殑tail=>宸ail
	 */	
	public Vector computeTailLeft(Mesh omega2,Vector u2_bk,
			Mesh omega1,Vector u1) {
		Vector tail = new SparseVectorHashMap(u2_bk.getDim());
		NodeList nodes2 = omega2.getNodeList(); 
		ObjList<Node> u2LeftNodes = this.getBorderNodes_Omega2(TailType.left, omega2);
		ObjList<Node> u1LeftNodes = this.getBorderNodes_Omega2(TailType.left, omega1);
		
		//on left tail=u1-u0
		for(int i=1;i<=u2LeftNodes.size();i++) {
			Node u2BottomNode = u2LeftNodes.at(i);
			for(int j=1;j<=u1LeftNodes.size();j++) {
				Node u1BottomNode = u1LeftNodes.at(j);
				if(u2BottomNode.coordEquals(u1BottomNode)) {
					tail.set(u2BottomNode.globalIndex, 
							u1.get(u1BottomNode.globalIndex)-u2_bk.get(u2BottomNode.globalIndex));
				}
			}
		}
		Tools.plotVector(omega2, outputFolder, "left_tail0.dat", tail);
		
		//extends from left to omega2
		for(int i=1;i<=nodes2.size();i++) {
			Node node  = nodes2.at(i);
			
			if(Math.abs(node.coord(1)-(-1.6))<Constant.meshEps) 
				continue;
			
			boolean find = false;
			for(int j=1;j<=u2LeftNodes.size();j++) {
				if(find) break;
				Node u2LeftNode = u2LeftNodes.at(j);
				ElementList eles = u2LeftNode.belongToElements;
				for(int k=1;k<=eles.size();k++) {
					if(find) break;
					Element e = eles.at(k);
					ObjList<EdgeLocal> edges = e.edges();
					for(int ie=1;ie<=edges.size();ie++) {
						if(find) break;
						EdgeLocal edge = edges.at(ie);
						Node newNode = new Node(0,u2LeftNode.coord(1),node.coord(2));
						if(Utils.isPointOnLineSegment(edge.beginNode(), edge.endNode(),
								newNode)) {
							double val = Utils.linearInterpolate(edge.beginNode(), edge.endNode(),
									newNode, 
									tail.get(edge.beginNode().globalIndex), 
									tail.get(edge.endNode().globalIndex));
							//鍏夋簮
							double lx = lightSource.get("x");
							double ly = lightSource.get("y");
							//鍐呴儴鐐�
							double x = node.coord(1);
							double y = node.coord(2);
							
							double MaxS = Math.sqrt((lx-(-1.6))*(lx-(-1.6)));
							double LS = Math.sqrt((x-lx)*(x-lx)+(y-ly)*(y-ly));
							if(LS>MaxS) LS=MaxS;
							double Inc = Math.exp(3.3*(MaxS-LS)/MaxS)-1.0;
							
							double SS = val*(1+Inc);
							//tail.set(node.globalIndex, SS);
							tail.set(node.globalIndex, val);
							find=true;
						}
					}
				}
			}
		}
		Tools.plotVector(omega2, outputFolder, "left_tail1.dat", tail);
		
		
		for(int i=1;i<=nodes2.size();i++) {
			Node node  = nodes2.at(i);
			tail.add(node.globalIndex,u2_bk.get(node.globalIndex));
		}
		Tools.plotVector(omega2, outputFolder, "left_tail.dat", tail);
		
		return tail;
	}
	
	public Vector computeTailRight(Mesh omega2,Vector u2_bk,
			Mesh omega1,Vector u1) {
		Vector tail = new SparseVectorHashMap(u2_bk.getDim());
		NodeList nodes2 = omega2.getNodeList(); 
		ObjList<Node> u2RightNodes = this.getBorderNodes_Omega2(TailType.right, omega2);
		ObjList<Node> u1RightNodes = this.getBorderNodes_Omega2(TailType.right, omega1);
		
		//on left tail=u1-u0
		for(int i=1;i<=u2RightNodes.size();i++) {
			Node u2BottomNode = u2RightNodes.at(i);
			for(int j=1;j<=u1RightNodes.size();j++) {
				Node u1BottomNode = u1RightNodes.at(j);
				if(u2BottomNode.coordEquals(u1BottomNode)) {
					tail.set(u2BottomNode.globalIndex, 
							u1.get(u1BottomNode.globalIndex)-u2_bk.get(u2BottomNode.globalIndex));
				}
			}
		}
		Tools.plotVector(omega2, outputFolder, "right_tail0.dat", tail);
		
		//extends from right to omega2
		for(int i=1;i<=nodes2.size();i++) {
			Node node  = nodes2.at(i);
			
			if(Math.abs(node.coord(1)-1.6)<Constant.meshEps) 
				continue;
			
			boolean find = false;
			for(int j=1;j<=u2RightNodes.size();j++) {
				if(find) break;
				Node u2RightNode = u2RightNodes.at(j);
				ElementList eles = u2RightNode.belongToElements;
				for(int k=1;k<=eles.size();k++) {
					if(find) break;
					Element e = eles.at(k);
					ObjList<EdgeLocal> edges = e.edges();
					for(int ie=1;ie<=edges.size();ie++) {
						if(find) break;
						EdgeLocal edge = edges.at(ie);
						Node newNode = new Node(0,u2RightNode.coord(1),node.coord(2));
						if(Utils.isPointOnLineSegment(edge.beginNode(), edge.endNode(),
								newNode)) {
							double val = Utils.linearInterpolate(edge.beginNode(), edge.endNode(),
									newNode, 
									tail.get(edge.beginNode().globalIndex), 
									tail.get(edge.endNode().globalIndex));
							//鍏夋簮
							double lx = lightSource.get("x");
							double ly = lightSource.get("y");
							//鍐呴儴鐐�
							double x = node.coord(1);
							double y = node.coord(2);
							
							double MaxS = Math.sqrt((lx-1.6)*(lx-1.6));
							double LS = Math.sqrt((x-lx)*(x-lx)+(y-ly)*(y-ly));
							if(LS>MaxS) LS=MaxS;
							double Inc = Math.exp(3.3*(MaxS-LS)/MaxS)-1.0;
							
							double SS = val*(1+Inc);
							//tail.set(node.globalIndex, SS);
							tail.set(node.globalIndex, val);
							find=true;
						}
					}
				}
			}
		}
		Tools.plotVector(omega2, outputFolder, "right_tail1.dat", tail);
		
		
		for(int i=1;i<=nodes2.size();i++) {
			Node node  = nodes2.at(i);
			tail.add(node.globalIndex,u2_bk.get(node.globalIndex));
		}
		Tools.plotVector(omega2, outputFolder, "right_tail.dat", tail);
		
		return tail;
	}
	
	/**
	 * 璁＄畻鍏夋簮鍦ㄤ笂杈圭殑tail=>涓媡ail
	 */
	public Vector computeTailBottom(Mesh omega2,Vector u2_bk,
			Mesh omega1,Vector u1) {
		Vector tail = new SparseVectorHashMap(u2_bk.getDim());
		NodeList nodes2 = omega2.getNodeList(); 
		ObjList<Node> u2BottomNodes = this.getBorderNodes_Omega2(TailType.bottom, omega2);
		ObjList<Node> u1BottomNodes = this.getBorderNodes_Omega2(TailType.bottom, omega1);
		
		//on bottom tail=u1-u0
		for(int i=1;i<=u2BottomNodes.size();i++) {
			Node u2BottomNode = u2BottomNodes.at(i);
			for(int j=1;j<=u1BottomNodes.size();j++) {
				Node u1BottomNode = u1BottomNodes.at(j);
				if(u2BottomNode.coordEquals(u1BottomNode)) {
					tail.set(u2BottomNode.globalIndex, 
							u1.get(u1BottomNode.globalIndex)-u2_bk.get(u2BottomNode.globalIndex));
				}
			}
		}
		Tools.plotVector(omega2, outputFolder, "bottom_tail0.dat", tail);
		
		//extends from bottom to omega2
		for(int i=1;i<=nodes2.size();i++) {
			Node node  = nodes2.at(i);
			
			if(Math.abs(node.coord(2)-(-0.5))<Constant.meshEps) 
				continue;
			
			boolean find = false;
			for(int j=1;j<=u2BottomNodes.size();j++) {
				if(find) break;
				Node u2BottomNode = u2BottomNodes.at(j);
				ElementList eles = u2BottomNode.belongToElements;
				for(int k=1;k<=eles.size();k++) {
					if(find) break;
					Element e = eles.at(k);
					ObjList<EdgeLocal> edges = e.edges();
					for(int ie=1;ie<=edges.size();ie++) {
						if(find) break;
						EdgeLocal edge = edges.at(ie);
						Node newNode = new Node(0,node.coord(1),u2BottomNode.coord(2));
						if(Utils.isPointOnLineSegment(edge.beginNode(), edge.endNode(),
								newNode)) {
							double val = Utils.linearInterpolate(edge.beginNode(), edge.endNode(),
									newNode, 
									tail.get(edge.beginNode().globalIndex), 
									tail.get(edge.endNode().globalIndex));
							//鍏夋簮
							double lx = lightSource.get("x");
							double ly = lightSource.get("y");
							//鍐呴儴鐐�
							double x = node.coord(1);
							double y = node.coord(2);
							
							double MaxS = Math.sqrt((ly-(-0.5))*(ly-(-0.5)));
							double LS = Math.sqrt((x-lx)*(x-lx)+(y-ly)*(y-ly));
							if(LS>MaxS) LS=MaxS;
							//double Inc = Math.exp(BS/(LS+BS))-1;
							double Inc = Math.exp(3.3*(MaxS-LS)/MaxS)-1.0;
							
							double SS = val*(1+Inc);
							//tail.set(node.globalIndex, SS);
							tail.set(node.globalIndex, val);
							find=true;
						}
					}
				}
			}
		}
		Tools.plotVector(omega2, outputFolder, "bottom_tail1.dat", tail);
		
		
		for(int i=1;i<=nodes2.size();i++) {
			Node node  = nodes2.at(i);
			tail.add(node.globalIndex,u2_bk.get(node.globalIndex));
		}
		Tools.plotVector(omega2, outputFolder, "bottom_tail.dat", tail);
		
		return tail;
	}
	
	/**
	 * 璁＄畻鍏夋簮鍦ㄤ笂杈圭殑tail=>涓媡ail
	 */
	public Vector computeTailTop(Mesh omega2,Vector u2_bk,
			Mesh omega1,Vector u1) {
		Vector tail = new SparseVectorHashMap(u2_bk.getDim());
		NodeList nodes2 = omega2.getNodeList(); 
		ObjList<Node> u2TopNodes = this.getBorderNodes_Omega2(TailType.top, omega2);
		ObjList<Node> u1TopNodes = this.getBorderNodes_Omega2(TailType.top, omega1);
		
		//on top tail=u1-u0
		for(int i=1;i<=u2TopNodes.size();i++) {
			Node u2BottomNode = u2TopNodes.at(i);
			for(int j=1;j<=u1TopNodes.size();j++) {
				Node u1BottomNode = u1TopNodes.at(j);
				if(u2BottomNode.coordEquals(u1BottomNode)) {
					tail.set(u2BottomNode.globalIndex, 
							u1.get(u1BottomNode.globalIndex)-u2_bk.get(u2BottomNode.globalIndex));
				}
			}
		}
		Tools.plotVector(omega2, outputFolder, "top_tail0.dat", tail);
		
		//extends from top to omega2
		for(int i=1;i<=nodes2.size();i++) {
			Node node  = nodes2.at(i);
			
			if(Math.abs(node.coord(2)-1.0)<Constant.meshEps) 
				continue;
			
			boolean find = false;
			for(int j=1;j<=u2TopNodes.size();j++) {
				if(find) break;
				Node u2TopNode = u2TopNodes.at(j);
				ElementList eles = u2TopNode.belongToElements;
				for(int k=1;k<=eles.size();k++) {
					if(find) break;
					Element e = eles.at(k);
					ObjList<EdgeLocal> edges = e.edges();
					for(int ie=1;ie<=edges.size();ie++) {
						if(find) break;
						EdgeLocal edge = edges.at(ie);
						Node newNode = new Node(0,node.coord(1),u2TopNode.coord(2));
						if(Utils.isPointOnLineSegment(edge.beginNode(), edge.endNode(),
								newNode)) {
							double val = Utils.linearInterpolate(edge.beginNode(), edge.endNode(),
									newNode, 
									tail.get(edge.beginNode().globalIndex), 
									tail.get(edge.endNode().globalIndex));
							//鍏夋簮
							double lx = lightSource.get("x");
							double ly = lightSource.get("y");
							//鍐呴儴鐐�
							double x = node.coord(1);
							double y = node.coord(2);
							
							double MaxS = Math.sqrt((ly-1.0)*(ly-1.0));
							double LS = Math.sqrt((x-lx)*(x-lx)+(y-ly)*(y-ly));
							if(LS>MaxS) LS=MaxS;
							//double Inc = Math.exp(BS/(LS+BS))-1;
							double Inc = Math.exp(3.3*(MaxS-LS)/MaxS)-1.0;
							
							double SS = val*(1+Inc);
							//tail.set(node.globalIndex, SS);
							tail.set(node.globalIndex, val);
							find=true;
						}
					}
				}
			}
		}
		Tools.plotVector(omega2, outputFolder, "top_tail1.dat", tail);
		
		for(int i=1;i<=nodes2.size();i++) {
			Node node  = nodes2.at(i);
			tail.add(node.globalIndex,u2_bk.get(node.globalIndex));
		}
		Tools.plotVector(omega2, outputFolder, "top_tail.dat", tail);
		
		return tail;
	}

	
	public void extendAlphaFromBottom(Mesh omega2,Vector alpha) {
		NodeList nodes2 = omega2.getNodeList(); 
		ObjList<Node> u2BottomNodes = this.getBorderNodes_Omega2(TailType.bottom, omega2);
		//extends from bottom to omega2
		for(int i=1;i<=nodes2.size();i++) {
			Node node  = nodes2.at(i);
			
			if(Math.abs(node.coord(2)-(-0.5))<Constant.meshEps) 
				continue;
			
			boolean find = false;
			for(int j=1;j<=u2BottomNodes.size();j++) {
				if(find) break;
				Node u2BottomNode = u2BottomNodes.at(j);
				ElementList eles = u2BottomNode.belongToElements;
				for(int k=1;k<=eles.size();k++) {
					if(find) break;
					Element e = eles.at(k);
					ObjList<EdgeLocal> edges = e.edges();
					for(int ie=1;ie<=edges.size();ie++) {
						if(find) break;
						EdgeLocal edge = edges.at(ie);
						Node newNode = new Node(0,node.coord(1),u2BottomNode.coord(2));
						if(Utils.isPointOnLineSegment(edge.beginNode(), edge.endNode(),
								newNode)) {
							double val = Utils.linearInterpolate(edge.beginNode(), edge.endNode(),
									newNode, 
									alpha.get(edge.beginNode().globalIndex), 
									alpha.get(edge.endNode().globalIndex));
							alpha.set(node.globalIndex, val);
							find=true;
						}
					}
				}
			}
		}			
	}
	
	public void extendAlphaFromLeft(Mesh omega2,Vector alpha) {
		NodeList nodes2 = omega2.getNodeList(); 
		ObjList<Node> u2LeftNodes = this.getBorderNodes_Omega2(TailType.left, omega2);
		
		//extends from left to omega2
		for(int i=1;i<=nodes2.size();i++) {
			Node node  = nodes2.at(i);
			
			if(Math.abs(node.coord(1)-(-1.6))<Constant.meshEps) 
				continue;
			
			boolean find = false;
			for(int j=1;j<=u2LeftNodes.size();j++) {
				if(find) break;
				Node u2LeftNode = u2LeftNodes.at(j);
				ElementList eles = u2LeftNode.belongToElements;
				for(int k=1;k<=eles.size();k++) {
					if(find) break;
					Element e = eles.at(k);
					ObjList<EdgeLocal> edges = e.edges();
					for(int ie=1;ie<=edges.size();ie++) {
						if(find) break;
						EdgeLocal edge = edges.at(ie);
						Node newNode = new Node(0,u2LeftNode.coord(1),node.coord(2));
						if(Utils.isPointOnLineSegment(edge.beginNode(), edge.endNode(),
								newNode)) {
							double val = Utils.linearInterpolate(edge.beginNode(), edge.endNode(),
									newNode, 
									alpha.get(edge.beginNode().globalIndex), 
									alpha.get(edge.endNode().globalIndex));
							alpha.set(node.globalIndex, val);
							find=true;
						}
					}
				}
			}
		}
			
	}
	
	public void extendAlphaFromRight(Mesh omega2,Vector alpha) {
		NodeList nodes2 = omega2.getNodeList(); 
		ObjList<Node> u2RightNodes = this.getBorderNodes_Omega2(TailType.right, omega2);
		
		//extends from right to omega2
		for(int i=1;i<=nodes2.size();i++) {
			Node node  = nodes2.at(i);
			
			if(Math.abs(node.coord(1)-1.6)<Constant.meshEps) 
				continue;
			
			boolean find = false;
			for(int j=1;j<=u2RightNodes.size();j++) {
				if(find) break;
				Node u2RightNode = u2RightNodes.at(j);
				ElementList eles = u2RightNode.belongToElements;
				for(int k=1;k<=eles.size();k++) {
					if(find) break;
					Element e = eles.at(k);
					ObjList<EdgeLocal> edges = e.edges();
					for(int ie=1;ie<=edges.size();ie++) {
						if(find) break;
						EdgeLocal edge = edges.at(ie);
						Node newNode = new Node(0,u2RightNode.coord(1),node.coord(2));
						if(Utils.isPointOnLineSegment(edge.beginNode(), edge.endNode(),
								newNode)) {
							double val = Utils.linearInterpolate(edge.beginNode(), edge.endNode(),
									newNode, 
									alpha.get(edge.beginNode().globalIndex), 
									alpha.get(edge.endNode().globalIndex));
							alpha.set(node.globalIndex, val);
							find=true;
						}
					}
				}
			}
		}
	}
	
	public void extendAlphaFromTop(Mesh omega2,Vector alpha) {
		NodeList nodes2 = omega2.getNodeList(); 
		ObjList<Node> u2TopNodes = this.getBorderNodes_Omega2(TailType.top, omega2);
		
		//extends from top to omega2
		for(int i=1;i<=nodes2.size();i++) {
			Node node  = nodes2.at(i);
			
			if(Math.abs(node.coord(2)-1.0)<Constant.meshEps) 
				continue;
			
			boolean find = false;
			for(int j=1;j<=u2TopNodes.size();j++) {
				if(find) break;
				Node u2TopNode = u2TopNodes.at(j);
				ElementList eles = u2TopNode.belongToElements;
				for(int k=1;k<=eles.size();k++) {
					if(find) break;
					Element e = eles.at(k);
					ObjList<EdgeLocal> edges = e.edges();
					for(int ie=1;ie<=edges.size();ie++) {
						if(find) break;
						EdgeLocal edge = edges.at(ie);
						Node newNode = new Node(0,node.coord(1),u2TopNode.coord(2));
						if(Utils.isPointOnLineSegment(edge.beginNode(), edge.endNode(),
								newNode)) {
							double val = Utils.linearInterpolate(edge.beginNode(), edge.endNode(),
									newNode, 
									alpha.get(edge.beginNode().globalIndex), 
									alpha.get(edge.endNode().globalIndex));
							alpha.set(node.globalIndex, val);
							find=true;
						}
					}
				}
			}
		}
	}

	
	public void run(int srcLightId, int timeId, TailType tailType) {
		//Read a triangle mesh from an input file
		//鑰侀紶澶达紝姊舰鍖哄煙
		MeshReader reader = new MeshReader("mouse_omega.grd");
		//鍖呭洿鑰侀紶澶寸殑鐭╁舰瑙勫垯鍖哄煙
		MeshReader reader2 = new MeshReader("mouse_omega2.grd");
		//澶у尯鍩熻儗鏅В
		MeshReader reader0 = new MeshReader("mouse_omega00.grd");
		//澶栭棶棰橈紝澶у尯鍩�
		MeshReader reader1 = new MeshReader("mouse_omega11.grd");
		
		Mesh meshOmega = reader.read2DMesh();
		Mesh meshOmega0 = reader0.read2DMesh();
		Mesh meshOmega1 = reader1.read2DMesh();
		Mesh meshOmega2 = reader2.read2DMesh();
		
		//Geometry relationship
		meshOmega.computeNodeBelongsToElements();
		meshOmega0.computeNodeBelongsToElements();
		meshOmega1.computeNodeBelongsToElements();
		meshOmega2.computeNodeBelongsToElements();
		meshOmega2.computeNeighborNodes();
		
		//鎻愬彇澶栭棶棰榤eshOmega1鐨勫唴杈圭晫缁撶偣鐨勨€滃弬鏁板潗鏍団€�
		//濡傛灉鍙槸鎻愬彇鍙傛暟鍧愭爣锛岃繍琛屽埌杩欓噷锛屽皢杈撳嚭澶嶅埗鍒癿atlab鏂囦欢
		//mouse_grid_border.m涓殑浜岀淮鏁扮粍rs涓紝杩愯
		//灏唌atlab杈撳嚭鏂囦欢澶嶅埗鍒板綋鍓嶇洰褰�
		extractBorderParamData(meshOmega1);
		
		//Use element library to assign degree of freedom (DOF) to element
		FELinearTriangle linearTriangle = new FELinearTriangle();
		ElementList eList0 = meshOmega0.getElementList();
		for(int i=1;i<=eList0.size();i++)
			linearTriangle.assignTo(eList0.at(i));
		ElementList eList2 = meshOmega2.getElementList();
		for(int i=1;i<=eList2.size();i++)
			linearTriangle.assignTo(eList2.at(i));
		ElementList eList1 = meshOmega1.getElementList();
		for(int i=1;i<=eList1.size();i++)
			linearTriangle.assignTo(eList1.at(i));
		
		//澶у尯鍩熻儗鏅В锛岀敤浜嶤alibration
		Vector u0_bk = solveForwardNeumann(meshOmega0);
		Tools.plotVector(meshOmega0, outputFolder, tailType+"_u0_bk.dat", u0_bk);
		
		//鎶藉彇鈥滃ぇ鍖哄煙鑳屾櫙瑙ｂ€濆埌Omega涓�
		Vector u_bk = this.extractData(meshOmega0, meshOmega, u0_bk);
		Tools.plotVector(meshOmega, outputFolder, tailType+"_u_bk.dat", u_bk);
		
		//鎶藉彇鈥滃ぇ鍖哄煙鑳屾櫙瑙ｂ€濆埌Omega2涓�
		Vector u2_bk = this.extractData(meshOmega0, meshOmega2, u0_bk);
		Tools.plotVector(meshOmega2, outputFolder, tailType+"_u2_bk.dat", u2_bk);
		
		
		//澶栭棶棰� Solve exterior problem
		final TailType fTailType = tailType;
		HashMap<NodeType, MathFunc> mapNTF = new HashMap<NodeType, MathFunc>();
		mapNTF.put(NodeType.Robin, new MultiVarFunc("x","y") {
			@Override
			public double apply(Variable v) {
				double x = v.get("x");
				double y = v.get("y");
				//Omega1
//				if(Math.abs(x-1.6)<Constant.meshEps ||
//						Math.abs(x+1.6)<Constant.meshEps ||
//						Math.abs(y-0.9)<Constant.meshEps ||
//						Math.abs(y+0.5)<Constant.meshEps)
				//Omega11
				if(Math.abs(x-2.1)<Constant.meshEps ||
						Math.abs(x+2.1)<Constant.meshEps ||
						Math.abs(y-1.4)<Constant.meshEps ||
						Math.abs(y+1.0)<Constant.meshEps)
					return 1;
				//bottom tail: 澶栭棶棰樹笂杈圭晫涓嶈Dirichlet鏉′欢锛屾敼涓篟obin
				if(fTailType==TailType.bottom && Math.abs(y-0.8)<Constant.meshEps)
					return 1;
				//left tail: 澶栭棶棰樺彸杈圭晫涓嶈Dirichlet鏉′欢锛屾敼涓篟obin
				if(fTailType==TailType.left && Math.abs(x-1.6)<Constant.meshEps)
					return 1;
				//right tail: 澶栭棶棰樺彸杈圭晫涓嶈Dirichlet鏉′欢锛屾敼涓篟obin
				if(fTailType==TailType.right && Math.abs(x-(-1.6))<Constant.meshEps)
					return 1;
				//top tail: 澶栭棶棰樺彸杈圭晫涓嶈Dirichlet鏉′欢锛屾敼涓篟obin
				if(fTailType==TailType.top && Math.abs(x-(-0.5))<Constant.meshEps)
					return 1;
				
				return 0;
			}
		});
		
		mapNTF.put(NodeType.Dirichlet, null);
		meshOmega1.clearBorderNodeMark();
		meshOmega1.markBorderNode(mapNTF);
//鎵撳嵃Robin杈圭晫鏉′欢锛屾鏌ユ槸鍚︽爣璁版纭�		
		NodeList o1_nodes = meshOmega1.getNodeList();
		for(int i=1;i<=o1_nodes.size();i++) {
			if(o1_nodes.at(i).getNodeType()==NodeType.Robin)
				System.out.println(o1_nodes.at(i));
		}
		for(int i=1;i<=o1_nodes.size();i++) {
			if(o1_nodes.at(i).getNodeType()==NodeType.Dirichlet)
				System.out.println(o1_nodes.at(i));
		}
//		ElementList o1_ele = meshOmega1.getElementList();
//		for(int i=1;i<=o1_ele.size();i++)
//			System.out.println(o1_ele.at(i));
		
		//璇诲彇娴嬮噺杈圭晫鏉′欢锛屽厜婧愮紪鍙蜂负srcLightId锛屾椂闂寸紪鍙蜂负timeId
		MathFunc diri = new DiscreteIndexFunction(readInterpData(srcLightId,timeId));
		Vector u1 = solveForwardDirichlet(meshOmega1,diri);
		Tools.plotVector(meshOmega1, outputFolder, tailType+"_u1.dat", u1);
		
		//鏋勯€爐ail left
		if(tailType==TailType.left) {
			Vector tailLeft = computeTailLeft(meshOmega2,u2_bk,meshOmega1,u1);
			//姹傝В绯绘暟鍙嶉棶棰�
			Vector alphaLeft = this.solveParamInverse(meshOmega2, tailLeft);
			Tools.plotVector(meshOmega0, outputFolder, "alphaLeft"+timeId+".dat", alphaLeft);
			alphaLeft = Utils.gaussSmooth(meshOmega2, alphaLeft, 1, 0.5);
			alphaLeft = Utils.gaussSmooth(meshOmega2, alphaLeft, 1, 0.5);
			alphaLeft = Utils.gaussSmooth(meshOmega2, alphaLeft, 1, 0.5);
			alphaLeft = Utils.gaussSmooth(meshOmega2, alphaLeft, 1, 0.5);
			Tools.plotVector(meshOmega2, outputFolder, "alphaLeft_smooth"+timeId+".dat", alphaLeft);
			extendAlphaFromLeft(meshOmega2,alphaLeft);
			for(int i=1;i<=alphaLeft.getDim();i++) {
				double v = alphaLeft.get(i);
				if(v<0) alphaLeft.set(i,0);
			}
			Tools.plotVector(meshOmega2, outputFolder, "alphaLeft_smooth_ext"+timeId+".dat", alphaLeft);
			tails.add(alphaLeft);
			tailTypes.add("L");
		}
		
		//鏋勯€爐ail right
		if(tailType==TailType.right) {
			Vector tailRight = computeTailRight(meshOmega2,u2_bk,meshOmega1,u1);
			//姹傝В绯绘暟鍙嶉棶棰�
			Vector alphaRight = this.solveParamInverse(meshOmega2, tailRight);
			Tools.plotVector(meshOmega0, outputFolder, "alphaRight"+timeId+".dat", alphaRight);
			alphaRight = Utils.gaussSmooth(meshOmega2, alphaRight, 1, 0.5);
			alphaRight = Utils.gaussSmooth(meshOmega2, alphaRight, 1, 0.5);
			alphaRight = Utils.gaussSmooth(meshOmega2, alphaRight, 1, 0.5);
			alphaRight = Utils.gaussSmooth(meshOmega2, alphaRight, 1, 0.5);
			Tools.plotVector(meshOmega2, outputFolder, "alphaRight_smooth"+timeId+".dat", alphaRight);
			extendAlphaFromLeft(meshOmega2,alphaRight);
			for(int i=1;i<=alphaRight.getDim();i++) {
				double v = alphaRight.get(i);
				if(v<0) alphaRight.set(i,0);
			}
			Tools.plotVector(meshOmega2, outputFolder, "alphaRight_smooth_ext"+timeId+".dat", alphaRight);
			tails.add(alphaRight);
			tailTypes.add("R");
		}
		
		//鏋勯€爐ail bottom
		if(tailType==TailType.bottom) {
			Vector tailBottom = computeTailBottom(meshOmega2,u2_bk,meshOmega1,u1);
			//姹傝В绯绘暟鍙嶉棶棰�
			Vector alphaBottom = this.solveParamInverse(meshOmega2, tailBottom);
			Tools.plotVector(meshOmega2, outputFolder, "alphaBottom"+timeId+".dat", alphaBottom);
			alphaBottom = Utils.gaussSmooth(meshOmega2, alphaBottom, 1, 0.5);
			alphaBottom = Utils.gaussSmooth(meshOmega2, alphaBottom, 1, 0.5);
			alphaBottom = Utils.gaussSmooth(meshOmega2, alphaBottom, 1, 0.5);
			alphaBottom = Utils.gaussSmooth(meshOmega2, alphaBottom, 1, 0.5);
			Tools.plotVector(meshOmega2, outputFolder, "alphaBottom_smooth"+timeId+".dat", alphaBottom);
			extendAlphaFromBottom(meshOmega2,alphaBottom);
			for(int i=1;i<=alphaBottom.getDim();i++) {
				double v = alphaBottom.get(i);
				if(v<0) alphaBottom.set(i,0);
			}
			Tools.plotVector(meshOmega2, outputFolder, "alphaBottom_smooth_ext"+timeId+".dat", alphaBottom);
			tails.add(alphaBottom);
			tailTypes.add("B");
		}
		
		//鏋勯€爐ail bottom
		if(tailType==TailType.top) {
			Vector tailTop = computeTailTop(meshOmega2,u2_bk,meshOmega1,u1);
			//姹傝В绯绘暟鍙嶉棶棰�
			Vector alphaTop = this.solveParamInverse(meshOmega2, tailTop);
			Tools.plotVector(meshOmega2, outputFolder, "alphaTop"+timeId+".dat", alphaTop);
			alphaTop = Utils.gaussSmooth(meshOmega2, alphaTop, 1, 0.5);
			alphaTop = Utils.gaussSmooth(meshOmega2, alphaTop, 1, 0.5);
			alphaTop = Utils.gaussSmooth(meshOmega2, alphaTop, 1, 0.5);
			alphaTop = Utils.gaussSmooth(meshOmega2, alphaTop, 1, 0.5);
			Tools.plotVector(meshOmega2, outputFolder, "alphaTop_smooth"+timeId+".dat", alphaTop);
			extendAlphaFromBottom(meshOmega2,alphaTop);
			for(int i=1;i<=alphaTop.getDim();i++) {
				double v = alphaTop.get(i);
				if(v<0) alphaTop.set(i,0);
			}
			Tools.plotVector(meshOmega2, outputFolder, "alphaTop_smooth_ext"+timeId+".dat", alphaTop);
			tails.add(alphaTop);
			tailTypes.add("T");
		}
		
		if(tails.size() == 2) {
			Vector v1 = tails.at(1);
			Vector v2 = tails.at(2);
			String sType = String.format("_%s%s%02d", 
					tailTypes.at(1),tailTypes.at(2),timeId);
			double max1 = v1.normInf();
			double max2 = v2.normInf();
			double max = Math.max(max1, max2);
			for(int i=1;i<=v1.getDim();i++) {
				v1.set(i,max*v1.get(i)/max1);
				v2.set(i,max*v2.get(i)/max2);
			}
			Tools.plotVector(meshOmega2, outputFolder, "alpha"+sType+"_v1.dat", v1);
			Tools.plotVector(meshOmega2, outputFolder, "alpha"+sType+"_v2.dat", v2);
			v1 = Utils.gaussSmooth(meshOmega2, v1, 1, 0.5);
			v1 = Utils.gaussSmooth(meshOmega2, v1, 1, 0.5);
			v1 = Utils.gaussSmooth(meshOmega2, v1, 1, 0.5);
			v2 = Utils.gaussSmooth(meshOmega2, v2, 1, 0.5);
			v2 = Utils.gaussSmooth(meshOmega2, v2, 1, 0.5);
			v2 = Utils.gaussSmooth(meshOmega2, v2, 1, 0.5);
			v1.add(1.0, v2);
			Tools.plotVector(meshOmega2, outputFolder, "alpha"+sType+"_add.dat", v1);
			NodeList nodes2 = meshOmega2.getNodeList();
			for(int i=1;i<=v1.getDim();i++) {
				double v = v1.get(i);
				if(v<0.7*max) v1.set(i,0);
				if(nodes2.at(i).coord(1)<-1.0 || nodes2.at(i).coord(1)>1.0)
					v1.set(i,0);
			}
			Tools.plotVector(meshOmega2, outputFolder, "alpha"+sType+"_ext.dat", v1);
			v1 = Utils.gaussSmooth(meshOmega2, v1, 1, 0.5);
			v1 = Utils.gaussSmooth(meshOmega2, v1, 1, 0.5);
			v1 = Utils.gaussSmooth(meshOmega2, v1, 1, 0.5);
			Tools.plotVector(meshOmega2, outputFolder, "alpha"+sType+"_ext_smooth.dat", v1);
			
			Tools.plotVector(meshOmega, outputFolder, "alpha_omega"+sType+"_ext_smooth.dat",
					extractData(meshOmega2, meshOmega, v1));
			
			tails.clear();
			tailTypes.clear();
		}
		
	}
	
	public void test() {
			//Read a triangle mesh from an input file
			//鍖呭洿鑰侀紶澶寸殑鐭╁舰瑙勫垯鍖哄煙
			MeshReader reader2 = new MeshReader("mouse_omega2.grd");
			
			Mesh meshOmega2 = reader2.read2DMesh();
			
			//Geometry relationship
			meshOmega2.computeNodeBelongsToElements();
			meshOmega2.computeNeighborNodes();
			
			//Use element library to assign degree of freedom (DOF) to element
			FELinearTriangle linearTriangle = new FELinearTriangle();
			ElementList eList2 = meshOmega2.getElementList();
			for(int i=1;i<=eList2.size();i++)
				linearTriangle.assignTo(eList2.at(i));
			
			mu_a=FC.c(0.1);
			Vector u2_bk = solveForwardNeumann(meshOmega2);
			Tools.plotVector(meshOmega2, outputFolder, "test_u2_bk.dat", u2_bk);
			
			setMu_a_Band(-0.5,0.1,1);
			Vector u2 = solveForwardNeumann(meshOmega2);
			Tools.plotVector(meshOmega2, outputFolder, "test_u2.dat", u2);
			
			//姹傝В绯绘暟鍙嶉棶棰�
			Vector alphaBottom = this.solveParamInverse(meshOmega2, u2);
			Tools.plotVector(meshOmega2, outputFolder, "test_alphaBottom.dat", alphaBottom);
			
			Tools.plotVector(meshOmega2, outputFolder, "test_diff.dat", 
					u2.add(-1.0, u2_bk));
			
			Tools.plotVector(meshOmega2, outputFolder, "test_diff_ln.dat", 
					FMath.log(FMath.abs(u2)));
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		MouseHeadOld m = new MouseHeadOld();
		
		/**
		 * Omega: See the ppt file: x:[-1.5,1.5], y:[-0.4,0.8]
		 * Omega2:  [-1.6,1.6]*[-0.5,0.9]
		 * Omega00: [-2.1,2.1]*[-1.0,1.4]
		 */
		
		//m.setDelta(0.0, 0.9);
		//m.test();
		
		m.mu_a = FC.c(0.1);
//		//m.setMu_a(-0.5,0.2,0.1,1);
		
//		for(int timeId=1;timeId<=37;timeId++) {
//			m.setDelta(0.0, 0.9);//S12 y(0.8->0.9)
//			m.run(12,timeId,TailType.bottom);
//			m.setDelta(-1.2, 0.4);//S2 x(-1.1->-1.2)
//			m.run(2,timeId,TailType.right);
//		}
		
		for(int timeId=20;timeId<=37;timeId+=10) {
			m.setDelta(0.0, 0.9);//S12 y(0.8->0.9)
			m.run(12,timeId,TailType.bottom);
			m.setDelta(1.2, 0.4);//S7 x(1.1->1.2)
			m.run(7,timeId,TailType.left);
		}
		
//		for(int timeId=1;timeId<=4;timeId++) {
//			m.setDelta(0.0, -0.5);//S15 y(-0.4->-0.5)
//			m.run(15,timeId,TailType.top);
//			m.setDelta(1.2, 0.4);//S7 x(1.1->1.2)
//			m.run(7,timeId,TailType.left);
//		}
//		
//		for(int timeId=1;timeId<=4;timeId++) {
//			m.setDelta(0.0, -0.5);//S15 y(-0.4->-0.5)
//			m.run(15,timeId,TailType.top);
//			m.setDelta(-1.2, 0.4);//S2 x(-1.1->-1.2)
//			m.run(2,timeId,TailType.right);
//		}		

//		m.setDelta(1.4, 0.0);//S8 x(1.3->1.4)
//		m.run(8,1,TailType.left);
		
//		m.setDelta(0.8, -0.5);//S16 x(1.3->1.4)
//		m.run(16,1,TailType.left);
	

	}
	

}
