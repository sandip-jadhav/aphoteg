package photoassociation.qizx;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import org.apache.commons.math.linear.RealMatrix;
import org.apache.commons.math.stat.correlation.Covariance;
import org.cyberneko.html.parsers.DOMParser;

import kim.bin.stats.mve.MVEWrap;
import kim.stats.mve.MVE;

import photoassociation.qizx.LDADocumentRepresentation.NonAlphaStopTokenizerFactory;
import photoassociation.qizx.LDADocumentRepresentation.StemTokenizerFactory;

import weka.core.matrix.Matrix;

import com.aliasi.cluster.LatentDirichletAllocation;
import com.aliasi.tokenizer.EnglishStopTokenizerFactory;
import com.aliasi.tokenizer.LowerCaseTokenizerFactory;
import com.aliasi.tokenizer.RegExTokenizerFactory;
import com.aliasi.tokenizer.StopTokenizerFactory;
import com.aliasi.tokenizer.Tokenizer;
import com.aliasi.tokenizer.TokenizerFactory;

public class UtilityFunctions {

	//static final TokenizerFactory BASE_TOKENIZER_FACTORY = LDADocumentRepresentation.BASE_TOKENIZER_FACTORY;
	// colocar tokenixer para apanhar dois ou mais -- seguidos 
	static final TokenizerFactory BASE_TOKENIZER_FACTORY = new RegExTokenizerFactory("\\p{L}+-\\p{L}+|[a-zA-Z0-9]+");
	static final String[] STOPWORD_LIST = LDADocumentRepresentation.STOPWORD_LIST;
	static final Set<String> STOPWORD_SET = new HashSet<String>(Arrays.asList(STOPWORD_LIST));

	static final TokenizerFactory simpleTokenizerFactory() {
		TokenizerFactory factory = BASE_TOKENIZER_FACTORY;
		factory = new NonAlphaStopTokenizerFactory(factory);
		factory = new LowerCaseTokenizerFactory(factory);
		factory = new EnglishStopTokenizerFactory(factory);
		factory = new StopTokenizerFactory(factory,STOPWORD_SET);
		return factory;
	}

	static final TokenizerFactory WORMBASE_TOKENIZER_FACTORY = simpleTokenizerFactory();

	public static String stringAsHex(byte[] bytes) {
		StringBuilder s = new StringBuilder();
		for (int i = 0; i < bytes.length; i++) {
			int parteAlta = ((bytes[i] >> 4) & 0xf) << 4;
			int parteBaixa = bytes[i] & 0xf;
			if (parteAlta == 0) s.append('0');
			s.append(Integer.toHexString(parteAlta | parteBaixa));
		}
		return s.toString();
	}

	public static String generateHash(String frase, String algoritmo) {
		try {
			MessageDigest md = MessageDigest.getInstance(algoritmo);
			md.update(frase.getBytes());
			return stringAsHex(md.digest());
		} catch (NoSuchAlgorithmException e) {
			return null;
		}
	}

	public static Document httpPost ( String location, String[] params ) {
		try {
			String data = "";
			for ( int i = 0; i < params.length; i++) {
				if ( data.length() > 0 ) data += "&";
				data += URLEncoder.encode(params[i], "UTF-8") + "=" + URLEncoder.encode(params[++i], "UTF-8");
			}		
			URL url = new URL(location);
			URLConnection conn = url.openConnection ();
			conn.setDoOutput(true);
			OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
			wr.write(data);
			wr.flush();
			BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			StringBuffer sb = new StringBuffer();
			while ((data = rd.readLine()) != null) sb.append(data);
			rd.close();
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			InputSource is = new InputSource();
			is.setCharacterStream(new StringReader(sb.toString()));
			Document doc = db.parse(is);
			return doc;
		} catch ( Exception e ) {
			return null;
		}
	}

	public static Document httpGet ( String location ) {
		try {
			URL url = new URL(location);
			URLConnection conn = url.openConnection ();
			BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			StringBuffer sb = new StringBuffer();
			String line;
			while ((line = rd.readLine()) != null) sb.append(line);
			rd.close();
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			InputSource is = new InputSource();
			is.setCharacterStream(new StringReader(sb.toString()));
			Document doc = db.parse(is);
			return doc;
		} catch ( Exception e ) {
			return null;
		}
	}

	public static String txtdoc ( String location ) {
		try {
			URL url = null;
			try {
				url = new URL(location);
			} catch ( MalformedURLException ex ) {
				url = new URL("file:/" + location);
			}
			URLConnection conn = url.openConnection ();
			BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			StringBuffer sb = new StringBuffer();
			String line;
			while ((line = rd.readLine()) != null) {
				sb.append(line);
				sb.append('\n');
			}
			rd.close();
			return sb.toString(); 
		} catch ( Exception e ) {
			return null;
		}
	}

	public static Document htmldoc ( String location ) {
		try {
			URL url = null;
			try {
				url = new URL(location);
			} catch ( MalformedURLException ex ) {
				url = new URL("file:/" + location);
			}
			URLConnection conn = url.openConnection ();
			BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			StringBuffer sb = new StringBuffer();
			String line;
			while ((line = rd.readLine()) != null) {
				sb.append(line);
				sb.append('\n');
			}
			rd.close();
			DOMParser parser = new DOMParser();
			InputSource is = new InputSource();
			is.setCharacterStream(new StringReader(sb.toString())); 
			parser.parse(is);
			Document doc = parser.getDocument();
			return doc;
		} catch ( Exception e ) {
			return null;
		}
	}

	public static double getDoubleMaxValue(){return Double.MAX_VALUE;}

	public static String[] parser(String doc){

		return doc.replaceAll("[^-\\p{Alnum}\\s]", "").split(" +");
	}

	public static String removeStopWords(String doc){
		char[] chars = doc.toCharArray();

		Tokenizer tokenizer = WORMBASE_TOKENIZER_FACTORY.tokenizer(chars,0,chars.length);

		String token;
		String result = "";

		while ((token = tokenizer.nextToken()) != null) {
			result += (" " + token); 
		}
		return result;
	}

	public static double getTF(String doc, String term){

		List<String> list = Arrays.asList(parser(doc));

		double count=0;
		double numWords=list.size();

		term = term.toLowerCase();

		for(String s : list){
			if(!(STOPWORD_SET.contains(term)) && s.compareToIgnoreCase(term)==0){
				count++;
			}
		}
		return count/numWords;
	}

	public static Object createVoculary(String[] collection){
		int i,numDoc=0;
		double numDocs = collection.length;

		Map<String,Integer> vocabulary = new HashMap<String,Integer>();

		// A StopWords List está toda em lowercase. 
		// O IDF apenas se preocupa se um termo existe num documento ou não (não importa o numero de vezes) 
		for(i=0; i<numDocs; i++){
			String doc = collection[i];
			String[] palavras = parser(doc);
			Set<String> wordSet = new HashSet<String>();

			for(String s : Arrays.asList(palavras)){
				s = s.toLowerCase();
				if(!(STOPWORD_SET.contains(s))){
					wordSet.add(s);
				}
			}

			for(String s : wordSet){
				s = s.toLowerCase();
				if(vocabulary.containsKey(s)){
					vocabulary.put(s, (vocabulary.get(s)+1));
				}
				else{vocabulary.put(s,1);}
				numDoc=i+1;
			}
		}
		return vocabulary;
	}

	public static double getIDF(Object map, String term, double numDocs){
		double numDocsTerm = 0;

		Map<String,Integer> vocabulary = (Map<String,Integer>) map;

		term = term.toLowerCase();

		if(vocabulary.containsKey(term)){
			numDocsTerm = vocabulary.get(term);
		}

		Double result = Math.log(numDocs/numDocsTerm);
		if(result.isNaN() || result.isInfinite()) return 0; 
		else return result;
	} 

	public static boolean containsIgnoreCase(List<String> list, String s){
		Iterator <String> it = list.iterator();
		while(it.hasNext()) {
			if(it.next().equalsIgnoreCase(s))
				return true;
		}
		return false;
	}

	public static double cosSim(double[] docTFIDF, double[] queryTFIDF){		
		int vectSize = docTFIDF.length;

		double dotProd = 0.00;
		double NormDoc = 0.00;
		double NormQuery = 0.00;
		int i;
		for(i = 0 ; i < vectSize ; i++){
			dotProd += (docTFIDF[i] * queryTFIDF[i]);
			NormDoc += Math.sqrt(Math.pow(docTFIDF[i], 2));
			NormQuery += Math.sqrt(Math.pow(queryTFIDF[i], 2));
		}
		Double result = dotProd / (NormDoc * NormQuery);
		if( result.isNaN()) return 0.0; else return result;
	}

	public static double cossineSimilarity(Object vocabulario, String doc, String query){
		int numDocs = 950;

		Map<String,Integer> vocabulary = (Map<String,Integer>) vocabulario;

		TreeMap<String, Double> vectorDoc = new TreeMap<String, Double>();
		TreeMap<String, Double> vectorQuery = new TreeMap<String, Double>();

		List<String> listDoc = Arrays.asList(parser(doc));
		List<String> listQuery = Arrays.asList(parser(query));

		Set<String> allWords = new HashSet<String>(listDoc);
		allWords.addAll(listQuery);

		// Apenas Docs sem stopwords
		for(String term : allWords) {
			double valDoc, valQuery;
			if(containsIgnoreCase(listDoc,term)){
				double tf = getTF(doc,term);
				double idf = getIDF(vocabulary, term, numDocs);
				valDoc = tf * idf;
			} else{ valDoc = 0.00;}
			vectorDoc.put(term, valDoc);
			if(containsIgnoreCase(listQuery,term)){
				double tf = getTF(query,term);
				double idf = getIDF(vocabulary, term, numDocs);
				valQuery = tf * idf;
			} else{ valQuery = 0.00;}
			vectorQuery.put(term,valQuery);
		}

		double dotProd = 0.00;
		double NormDoc = 0.00;
		double NormQuery = 0.00;
		int i;
		for(i = 0 ; i < vectorDoc.size() ; i++){
			dotProd += ((Double)vectorDoc.values().toArray()[i] * (Double)vectorQuery.values().toArray()[i]);
			NormDoc += Math.sqrt(Math.pow((Double)vectorDoc.values().toArray()[i], 2));
			NormQuery += Math.sqrt(Math.pow((Double)vectorQuery.values().toArray()[i], 2));
		}
		Double result = dotProd / (NormDoc * NormQuery);
		if( result.isNaN()) return 0.0; else return result;
	}

	public static double similarityFunction(double valor){
		return (1.0 /(1.0 + valor));
	}

	public static double normalizeFunction(double valor, double min, double max){
		Double result = (valor - min) / (max - min);
		if( result.isNaN()) return 0.0; else return result;
	}

	public static int votingBorda(int length, int position){

		// Para m pontos : Usando a regra de Borda -> 1º m-1 ; 2º m-2
		int pontos = length-position-1;
		return pontos;
	}

	public static int votingVeto(int length, int position){
		// (length-1) porque começa em 0 
		if(position != (length - 1)){
			return 1;
		}
		else {
			return 0;	
		}
	}

	public static int votingPlurality(int position){
		if(position == 0){
			return 1;
		}
		else {
			return 0;	
		}
	}

	public static Node voting(Node node, String types, String voteProtocol)
	{
		Element element = (Element) node;
		NodeList fotos =  element.getElementsByTagName("foto");

		int numPhotos = fotos.getLength();

		String[] vectType = types.split(" ");
		int i,k;
		String key = "";

		switch(votingProtocol.valueOf(voteProtocol)){
		case combSUM: element = votingComb(element, types, false); return element;
		case combMNZ: element = votingComb(element, types, true); return element;
		}

		//Para percorrer todas as features
		for (k=0; k < vectType.length; k++){

			String tipoOrdenacao = vectType[k];
			// Caso o atributo seja o ID da foto ou uma pontuacao, não faz nada
			if(tipoOrdenacao.equalsIgnoreCase("id") || tipoOrdenacao.contains("pontuacao") ){}
			else
			{
				//temporal; geographic(Min, mean, Max); - ascending ;
				//numComents, numFavorits; tfidf - descending
				Map<String, Node> map = null;

				map = new TreeMap<String,Node>(new AttributeComparator(false));

				//Primeiro coloco no treeMap para ficarem por ordem dos valores dos atributos
				for (i =0; i < fotos.getLength(); i++)
				{
					String attributeValue = fotos.item(i).getAttributes().getNamedItem(tipoOrdenacao).getNodeValue();

					double val;

					//System.out.println("attributeValue " + attributeValue );
					if(attributeValue.equals("INF")){
						val = Double.MAX_VALUE;
					}
					/*else if(attributeValue.equals("NaN")){
						val = 0.0;
					}*/
					else{val = Double.valueOf(attributeValue);}

					switch (features.valueOf(tipoOrdenacao)) {
					case ldaKLDiv : val = similarityFunction(val); break;
					case geograficalMin : val = similarityFunction(val); break;
					case geograficalMean : val = similarityFunction(val); break;
					case temporal : val = similarityFunction(val); break;
					case clusterGeograficalMin : val = similarityFunction(val); break;
					case clusterGeograficalMean : val = similarityFunction(val); break;
					case clusterTemporalMin : val = similarityFunction(val); break;
					case clusterTemporalMean : val = similarityFunction(val); break;
					}

					key = val+"|"+ fotos.item(i).getAttributes().getNamedItem("id").getNodeValue();
					map.put(key, fotos.item(i));
				}

				//Depois insiro a pontuação
				int j;
				for( j=0; j<map.values().size(); j++)
				{
					int length = map.values().size();
					Element noFoto = (Element)map.values().toArray()[j];

					int pontos = 0;


					switch(votingProtocol.valueOf(voteProtocol)){
					case borda: pontos = votingBorda(length, j); break;
					case plurality: pontos = votingPlurality(j); break;
					case veto: pontos = votingVeto(length, j); break;
					}

					//System.out.println("Pontos " + pontos );

					noFoto.setAttribute("pontuacao"+tipoOrdenacao, String.valueOf(pontos));

					if(noFoto.getAttributeNode("pontuacaoTotal") != null)
					{
						int totalPontos = Integer.parseInt(noFoto.getAttributeNode("pontuacaoTotal").getNodeValue());
						noFoto.setAttribute("pontuacaoTotal", String.valueOf(totalPontos+pontos));
					}
					else{noFoto.setAttribute("pontuacaoTotal", String.valueOf(pontos));}
				}
			}
		}
		return element;
	}

	public static Element votingComb(Element element, String types, boolean combMNZ){
		String[] vectTypes = types.split(" ");
		NodeList fotos =  element.getElementsByTagName("foto");
		int indiceFoto;		
		Map<String, Set<Double>> allSets = new HashMap<String, Set<Double>>();

		// Percorrer as fotos para os diferentes atributos
		// e adicionar ao TreeSet para poder saber qual o max e qual o min para o atributo "atributo" 
		for(String atributo : vectTypes){

			//Ordenado por ordem decrescente
			Set<Double> set = new TreeSet<Double>(Collections.reverseOrder());

			for(indiceFoto=0; indiceFoto<fotos.getLength(); indiceFoto++){
				Element foto = (Element) fotos.item(indiceFoto);
				String attributeVal = foto.getAttributes().getNamedItem(atributo).getNodeValue();

				Double val;

				if(attributeVal.equals("INF")){
					val = Double.MAX_VALUE;
					foto.setAttribute(atributo, val.toString());
				}
				/*else if(attributeVal.equals("NaN")){
					val = 0.0;
					foto.setAttribute(atributo, val.toString());
				}*/
				else{val = Double.valueOf(attributeVal);} 
				foto.setAttribute("valor"+atributo, val.toString());

				switch(features.valueOf(atributo)){

				case ldaKLDiv: 	
					val = similarityFunction(val);
					foto.setAttribute(atributo, val.toString());
					break;

				case temporal : 
					val = similarityFunction(val);
					foto.setAttribute(atributo, val.toString());
					break;

				case geograficalMin : 	
					val = similarityFunction(val);
					foto.setAttribute(atributo, val.toString());
					break;

				case geograficalMean : 	
					val = similarityFunction(val);
					foto.setAttribute(atributo, val.toString());
					break;


				case clusterGeograficalMin : 	
					val = similarityFunction(val);
					foto.setAttribute(atributo, val.toString());
					break;

				case clusterGeograficalMean : 	
					val = similarityFunction(val);
					foto.setAttribute(atributo, val.toString());
					break;


				case clusterTemporalMin : 	
					val = similarityFunction(val);
					foto.setAttribute(atributo, val.toString());
					break;


				case clusterTemporalMean : 	
					val = similarityFunction(val);
					foto.setAttribute(atributo, val.toString());
					break;

				}

				double value = Double.valueOf(foto.getAttributes().getNamedItem(atributo).getNodeValue());
				set.add(value);
			}
			allSets.put(atributo, set);
		}

		// Já temos todas as features com a ordenadas correctamente
		for(indiceFoto=0; indiceFoto<fotos.getLength(); indiceFoto++){

			Element foto = (Element) fotos.item(indiceFoto);

			Double pontosTotais=0.0;
			Double numNonZeros=0.0;

			for(String atributo : vectTypes) {
				double maxValue =(Double)allSets.get(atributo).toArray()[0];
				double minValue =(Double)allSets.get(atributo).toArray()[allSets.get(atributo).size()-1];

				//System.out.println("Valores Max : " + maxValue);
				//System.out.println("Valores Min : " + minValue);


				Double pontos=0.0, dVal=0.0;

				dVal = Double.valueOf(foto.getAttributes().getNamedItem(atributo).getNodeValue());
				pontos = normalizeFunction(dVal, minValue, maxValue);

				if(combMNZ && pontos!=0.0){numNonZeros++;}
				foto.setAttribute("pontuacao"+atributo, pontos.toString());
				pontosTotais += pontos;
			}

			//Adicionar o atributo à foto
			if(combMNZ){pontosTotais = pontosTotais*numNonZeros;}
			foto.setAttribute("pontuacaoTotal", pontosTotais.toString());
		}
		return element;
	}

	public enum features {
		numWordsPhotoDesc, numWordsDoc, tf, simTFIDF, ldaCoSim, ldaKLDiv, ldaEqTopic,
		geograficalMin, geograficalMean, temporal, 
		sentimentalPolarity, 
		clusterNumWords, clusterTF, clusterSimTFIDF, 
		clusterSentimentalPolarity,
		clusterTemporalMin, clusterTemporalMean,
		clusterGeograficalMin, clusterGeograficalMean,
		numComments, numFavorite, numViews
	}

	public enum votingProtocol {
		borda, veto, plurality, combSUM, combMNZ
	}

	public static String strPre(double inValue){
		DecimalFormat threeDec = new DecimalFormat("0.00000000000000000000");
		String shortString = (threeDec.format(inValue));
		return shortString.replace(',', '.');
	}

	public static LatentDirichletAllocation createLDAModel(String[] data, short numTopics){
		int minTokenCount = 1;
		LDADocumentRepresentation lda = new LDADocumentRepresentation();
		try{
			LatentDirichletAllocation model = lda.fitLDA(data,numTopics,minTokenCount);
			return model;
		}
		catch(Exception e){System.out.println("Ocorreu erro ao criar Modelo LDA " + e.getLocalizedMessage()); return null;}
	}

	public static double[] createDocumentTopics(Object modelo, String document){
		LDADocumentRepresentation lda = new LDADocumentRepresentation();
		LatentDirichletAllocation model = (LatentDirichletAllocation) modelo;

		double docTopics[] = lda.documentTopics(model,document);
		return docTopics;
	}

	public static double[] modelLDA(double[] docTopics1, double[] docTopics2){
		LDADocumentRepresentation lda = new LDADocumentRepresentation();

		Double simKLDivergence = lda.documentSimilarity(docTopics1,docTopics2,0);
		Double simCosineDistance = lda.documentSimilarity(docTopics1,docTopics2,1);
		Double simEqualTopic = lda.documentSimilarity(docTopics1,docTopics2,2);

		double[] vectResults = {0,0,0};
		vectResults[0] = simKLDivergence;
		vectResults[1] = simCosineDistance;
		vectResults[2] = simEqualTopic;
		return vectResults;
	}

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
		commonSense.math.linear.Matrix mat = new commonSense.math.linear.Matrix(matrix.getArrayCopy());

		System.out.println(mat.toString());

		MVEWrap mveW = new MVEWrap(mat);

		System.out.println(mveW);
		/*
		//System.out.println("Mariz Inicial : \n" + matrix);
		Covariance cov = new Covariance(matrix.getArrayCopy());
		//System.out.println("Cov: \n" + new Matrix(cov.getCovarianceMatrix().getData()));
		Matrix invCovMatrix = new Matrix(cov.getCovarianceMatrix().getData()).inverse();


		System.out.println("MeanLatitude : " + meanValueLatitude);
		System.out.println("MeanLongitude : " + meanValueLongitude);;
		System.out.println("Mariz Inversa Covariancia : \n" + invCovMatrix);


		//Valor para a distribuição Chi-quadrado , com probabilidade 0.5 e 2 graus de liberdade
		//double limitValue = 1.3862;
		double limitValue = 0.0506;

		// 4 graus de liberdade -> P = 0.025 -> 11.143
		// 2 graus de liberdade -> P = 0.025 -> 7.378 
		//System.out.println("Teste1 : " + Math.sqrt(7.378));
		//System.out.println("Teste2 : " + Math.sqrt(11.143));

		double[] squaredMahalanobisDistance = new double[numPlaces];
		int i;
		Matrix row = null; 
		for(i=0; i<numPlaces;i++){
			row = matrix.getMatrix(i,i,0,matrix.getColumnDimension()-1);
			row.set(0, 0, (row.get(0, 0) - meanValueLongitude));
			row.set(0, 1, (row.get(0, 1) - meanValueLatitude));
			//double val = (multiply(multiply(row.getArrayCopy(),invCovMatrix.getArrayCopy()),row.transpose().getArrayCopy())[0])[0];
			double val = Math.sqrt((multiply(multiply(row.getArrayCopy(),invCovMatrix.getArrayCopy()),row.transpose().getArrayCopy())[0])[0]);
			System.out.println("Dist " + val);
			if(val <= limitValue) squaredMahalanobisDistance[i] = 1;
			else squaredMahalanobisDistance[i]= 0;

			boolean resp;
			if(squaredMahalanobisDistance[i]==1) resp = false; else resp=true;
			System.out.println(" Ponto - mean : " + row +  " é outlier ? " + resp + "\n");

		}*/
		double[] vect = {0.01, 0.3};
		return vect;

	}

	public static double textualPolarity(String text){

		String path = "C:\\qizx\\bin\\Testes\\Common\\";
		try{
			SentimentalPolarity pol = new SentimentalPolarity(new FileReader(path + "SentiWordNet_3.0.0_20100908.txt"));
			return pol.textPolarity(text);
		}catch(Exception e){System.out.println("Erro a calcular a polaridade do texto " + e.getLocalizedMessage());}
		return Double.NaN;
	}

	public static double imagePolarity(String linkImage){
		String path = "C:\\qizx\\bin\\Testes\\Common\\Teste-10\\Imagens\\";
		//String path = "Imagens/" + linkImage;
		path +=  linkImage;
		try{
			Image image = java.awt.Toolkit.getDefaultToolkit().getDefaultToolkit().createImage(new URL("file:/"+path));
			return NaiveSimilarityFinder.imageWarmth(image);
		}catch(Exception e){System.out.println("Erro a calcular a polaridade da imagem " + e.getLocalizedMessage());}
		return Double.NaN;
	}

	public static double polaritySimilarity(String text, String linkImage){
		double textPolarity = textualPolarity(text);
		double imagePolarity = imagePolarity(linkImage);

		return Math.abs(textPolarity-imagePolarity);
	}

	public static Node getImageCluster(Node images, int numClusters){

		String path = "C:\\qizx\\bin\\Testes\\Common\\Teste-10\\Imagens\\";
		//String path = "Imagens/";
		Element element = (Element) images;
		NodeList photos =  element.getElementsByTagName("photo");
		int numPhotos = photos.getLength();

		Element mainPhoto = (Element) element.getElementsByTagName("imageClusters").item(0);
		String idMainPhoto = mainPhoto.getAttribute("id"); 

		Kmeans<ClusterableImage> kmeans = new Kmeans<ClusterableImage>();
		List<ClusterableImage> elements = new ArrayList<ClusterableImage>();

		try{
			int i; 
			for(i=0; i<photos.getLength();i++){
				Element photo = (Element)photos.item(i);
				String nameImg = path + photo.getAttribute("id") + "_"+photo.getAttribute("secret") + "_m.jpg";
				elements.add(new ClusterableImage(new URL("file:/"+nameImg)));
				//elements.add(new ClusterableImage(nameImg));
			}

			List<List<ClusterableImage>> clusters = null;

			if(numPhotos <= numClusters){clusters = kmeans.cluster(elements); }
			else{clusters = kmeans.cluster(elements,numClusters); }

			List<ClusterableImage> rigthCluster = new ArrayList<ClusterableImage>();
			for (i = 0; i < clusters.size(); i++ ) {
				for ( ClusterableImage img : clusters.get(i)) {
					if(img.id.contains(idMainPhoto)){
						rigthCluster = clusters.get(i);
						break;
					}
				}
			}

			DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
			Document document = documentBuilder.newDocument();

			Element imageCluster = document.createElement("imageCluster");
			document.appendChild(imageCluster);
			Element newImageClusters = (Element) document.getChildNodes().item(0);

			for ( ClusterableImage img : rigthCluster) {
				for(i=0; i<photos.getLength();i++){
					Element photo = (Element)photos.item(i);
					String idPhotoImg = img.id.split("/")[img.id.split("/").length-1].split("_")[0];
					if(photo.getAttribute("id").equalsIgnoreCase(idPhotoImg)){
						newImageClusters.appendChild(document.importNode(photo,true));
					}
				}
			}
			return newImageClusters;
		}catch(Exception e){System.out.println("Deu erro na criação de clusters " + e.getLocalizedMessage());}
		return null;
	}

	public static double getVincentysDistance( double lat1, double lon1, double lat2, double lon2 ){
		VincentyDistanceCalculator dist = new VincentyDistanceCalculator();
		double val = dist.getDistance(lat1, lon1, lat2, lon2);
		return val;
	}
}