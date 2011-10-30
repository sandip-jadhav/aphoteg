package photoassociation.qizx;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.apache.commons.math.stat.correlation.Covariance;
import weka.core.matrix.Matrix;

public class TestOutliersDetection {

	public static double[][] multiply(double[][] m1, double[][] m2) {
	    int m1rows = m1.length;
	    int m1cols = m1[0].length;
	    int m2rows = m2.length;
	    int m2cols = m2[0].length;
	    if (m1cols != m2rows)
	      throw new IllegalArgumentException("matrices don't match: " + m1cols + " != " + m2rows);
	    double[][] result = new double[m1rows][m2cols];

	    // multiply
	    for (int i=0; i<m1rows; i++)
	      for (int j=0; j<m2cols; j++)
	        for (int k=0; k<m1cols; k++)
	        result[i][j] += m1[i][k] * m2[k][j];

	    return result;
	  }

	public static double[] mahalanobisDistance(Node coordinates){
		
		Element element = (Element) coordinates;
		NodeList places =  element.getElementsByTagName("place");
		
		int numPlaces = places.getLength();
		
		//Apenas 2 colunas - Coordenadas (x,y)
		Matrix matrix = new Matrix(numPlaces, 2);
		
		double meanValueLatitude=0;
		double meanValueLongitude=0;
		int x,y;
		for(x=0; x < numPlaces; x++){
			Element place = (Element)places.item(x);
			Double latitude = Double.valueOf(place.getElementsByTagName("latitude").item(0).getTextContent());
			meanValueLatitude += latitude;
			Double longitude = Double.valueOf(place.getElementsByTagName("longitude").item(0).getTextContent());
			meanValueLongitude += longitude;
			for(y=0; y < 2; y++){
				if(y==0){matrix.set(x, y, longitude);}
				if(y==1){matrix.set(x, y, latitude);}
			}
		}
		
		meanValueLatitude = (meanValueLatitude/numPlaces);
		meanValueLongitude = (meanValueLongitude/numPlaces);
		
		Covariance cov = new Covariance(matrix.getArrayCopy());
		Matrix invCovMatrix = new Matrix(cov.getCovarianceMatrix().getData()).inverse();
		
		
		System.out.println("MeanLatitude : " + meanValueLatitude);
		System.out.println("MeanLongitude : " + meanValueLongitude);;
		System.out.println("Mariz Inversa Covariancia : \n" + invCovMatrix);
		 
		
		//Valor para a distribuição Chi-quadrado , com probabilidade 0.5 e 2 graus de liberdade
		double limitValue = 1.3862;
				
		double[] squaredMahalanobisDistance = new double[numPlaces];
		int i;
		Matrix row = null; 
		for(i=0; i<numPlaces;i++){
			row = matrix.getMatrix(i,i,0,matrix.getColumnDimension()-1);
			row.set(0, 0, (row.get(0, 0) - meanValueLongitude));
			row.set(0, 1, (row.get(0, 1) - meanValueLatitude));
			double val = (multiply(multiply(row.getArrayCopy(),invCovMatrix.getArrayCopy()),row.transpose().getArrayCopy())[0])[0];
			
			if(val <= limitValue) squaredMahalanobisDistance[i] = 1;
			else squaredMahalanobisDistance[i]= 0;
			
			boolean resp;
			if(squaredMahalanobisDistance[i]==1) resp = false; else resp=true;
			System.out.println(" Ponto - mean : " + row +  " é outlier ? " + resp + "\n");
			
		}
		return squaredMahalanobisDistance;
	}
	
	public static void main(String[] argv) {
		//double[] distancesVector = {10.20, 6.30, 5.3, 4.30};
		//mahalanobisDistance(distancesVector);
		
		URL urlXml;
		try {
			urlXml = new URL("file:/C:\\qizx\\bin\\Testes\\Teste-9\\Experiment_Mahalanobis-3.xml");
			URLConnection conn = urlXml.openConnection ();
			BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			StringBuffer sb = new StringBuffer();
			String line;
			while ((line = rd.readLine()) != null) {
				sb.append(line);
				sb.append('\n');
			}
			rd.close();
			Element el = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(sb.toString().getBytes())).getDocumentElement();
			mahalanobisDistance(el);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}