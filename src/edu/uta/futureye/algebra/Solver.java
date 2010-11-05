package edu.uta.futureye.algebra;

import org.netlib.lapack.DGESV;
import org.netlib.util.intW;

public class Solver {
	static double eps = 1e-10;
	
	/**
	 * 
	 * @param m
	 * @param v
	 * @return ���̵Ľ��������±��1��ʼ
	 */
	public Vector solve(Matrix m,Vector v) {
		if( !( m.getRowDim() == m.getColDim() &&
				m.getRowDim() == v.getDim()) ) {
			System.out.println("ERROR: Solver.solver() m.dim!=v.dim ");
			return null;
		}
		
	    int N = v.getDim();
	    int nrhs = 1;
	    int[]ipiv = new int[N];
	    
	    double[][]a = new double[N][N];
	    double[][]b = new double[N][1];

	    for(int i=0;i<N;i++) {
	    	for(int j=0;j<N;j++) {
	    		a[i][j] = m.get(i+1, j+1);
	    		//System.out.print(a[i][j]+" ");
	    	}
	    	//System.out.println("");
	    	b[i][0] = v.get(i+1);
	    	//System.out.println(b[i][0]);
	    }
	    intW info = new intW(0);
	    
        System.out.println("Begin Solver...");
        DGESV.DGESV(N, nrhs, a, ipiv, b, info);
        System.out.println("Solver info = " + info.val);
	    
    	Vector rv = new Vector(N);
	    for(int i=0;i<N;i++) {
	    	if(Math.abs(b[i][0]) > eps)
	    		rv.set(i+1, b[i][0]);
	    }
	    return rv;

	}
}
