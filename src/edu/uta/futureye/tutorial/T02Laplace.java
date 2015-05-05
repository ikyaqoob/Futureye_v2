package edu.uta.futureye.tutorial;

import java.util.HashMap;

import edu.uta.futureye.algebra.intf.Matrix;
import edu.uta.futureye.algebra.intf.Vector;
import edu.uta.futureye.algebra.solver.external.SolverJBLAS;
import edu.uta.futureye.core.Mesh;
import edu.uta.futureye.core.NodeType;
import edu.uta.futureye.function.intf.MathFunc;
import edu.uta.futureye.io.MeshReader;
import edu.uta.futureye.io.MeshWriter;
import edu.uta.futureye.lib.assembler.AssemblerScalar;
import edu.uta.futureye.lib.element.FELinearTriangle;
import edu.uta.futureye.lib.weakform.SymWeakFormLaplace2D;
import edu.uta.futureye.lib.weakform.WeakFormLaplace2D;
import edu.uta.futureye.util.container.ElementList;
//import static edu.uta.futureye.function.operator.FMath.*;
import static symjava.symbolic.Symbol.*;


/**
 * <blockquote><pre>
 * Problem:
 *   -\Delta{u} = f
 *   u(x,y)=0, (x,y) \in \partial{\Omega}
 * where
 *   \Omega = [-3,3]*[-3,3]
 *   f = -2*(x^2+y^2)+36
 * Solution:
 *   u = (x^2-9)*(y^2-9)
 * </blockquote></pre>
 * 
 * @author liuyueming
 */
public class T02Laplace {
	public Mesh mesh;
	public Vector u;
	public void run() {
        //1.Read in a triangle mesh from an input file with
        //  format ASCII UCD generated by Gridgen
        MeshReader reader = new MeshReader("triangle.grd");
        Mesh mesh = reader.read2DMesh();
        //Compute geometry relationship between nodes and elements
        mesh.computeNodeBelongsToElements();

        //2.Mark border types
        HashMap<NodeType, MathFunc> mapNTF =
                new HashMap<NodeType, MathFunc>();
        mapNTF.put(NodeType.Dirichlet, null);
        mesh.markBorderNode(mapNTF);

        //3.Use element library to assign degrees of
        //  freedom (DOF) to element
        ElementList eList = mesh.getElementList();
        FELinearTriangle feLT = new FELinearTriangle();
        for(int i=1;i<=eList.size();i++)
            feLT.assignTo(eList.at(i));

        //4.Weak form
        SymWeakFormLaplace2D weakForm = new SymWeakFormLaplace2D();
        //Right hand side(RHS): f = -2*(x^2+y^2)+36
        weakForm.setF(-2*(x*x+y*y)+36);
        weakForm.setParam(C1, C0, C0, C0);
        weakForm.generateOrLoadBytecode();

        //5.Assembly process
        AssemblerScalar assembler =
                new AssemblerScalar(mesh, weakForm);
        assembler.assemble();
        Matrix stiff = assembler.getStiffnessMatrix();
        Vector load = assembler.getLoadVector();
        //Boundary condition
        assembler.imposeDirichletCondition(C0);

        //6.Solve linear system
        SolverJBLAS solver = new SolverJBLAS();
        Vector u = solver.solveDGESV(stiff, load);
        System.out.println("u=");
        for(int i=1;i<=u.getDim();i++)
            System.out.println(String.format("%.3f", u.get(i)));

        //7.Output results to an Techplot format file
        MeshWriter writer = new MeshWriter(mesh);
        writer.writeTechplot("./tutorial/Laplace2D.dat", u);

        this.mesh = mesh;
        this.u = u;
	}
	
    public static void main(String[] args) {
    	T02Laplace ex1 = new T02Laplace();
    	ex1.run();
    }
}
