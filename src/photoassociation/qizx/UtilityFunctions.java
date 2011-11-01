package photoassociation.qizx;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

public class UtilityFunctions {

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
			conn.setConnectTimeout(30000);
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
		} catch ( SocketTimeoutException e ) {
			return httpPost(location, params);
		} catch ( Exception e ) {
			return null;
		}
	}
	
	public static Document httpGet ( String location ) {
		try {
			URL url = new URL(location);
			URLConnection conn = url.openConnection ();
			conn.setConnectTimeout(30000);
			BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			StringBuffer sb = new StringBuffer();
			String line;
			while ((line = rd.readLine()) != null) sb.append(line.replace("<?", "<!--").replace("?>", "-->"));
			rd.close();
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setIgnoringComments(true);
			DocumentBuilder db = dbf.newDocumentBuilder();
	        InputSource is = new InputSource();
	        is.setCharacterStream(new StringReader(sb.toString()));
	        Document doc = db.parse(is);
			return doc;
		} catch ( SocketTimeoutException e ) {
			return httpGet(location);	
		} catch ( Exception e ) {
			return null;
		}
	}
	
	public static Document htmldoc ( String location ) {
		try {
			URL url = new URL(location);
			URLConnection conn = url.openConnection ();
			conn.setConnectTimeout(30000);
			BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			StringBuffer sb = new StringBuffer();
			String line;
			while ((line = rd.readLine()) != null) sb.append(line);
			rd.close();
			org.cyberneko.html.parsers.DOMParser db = new org.cyberneko.html.parsers.DOMParser();
	        InputSource is = new InputSource();
	        is.setCharacterStream(new StringReader(sb.toString()));
	        db.parse(is);
	        return db.getDocument();
		} catch ( SocketTimeoutException e ) {
			return htmldoc(location);	
		} catch ( Exception e ) {
			return null;
		}
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
   
   
   public static Node voting(Node node, String types, String voteProtocol){
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

  // Alterar para o ficheiro SentiWordNet_3.0.0_20100908.txt 
   public static double textualPolarity(String text){
		//String path = "C:\\qizx\\bin\\Testes\\Common\\";
		try{
		//SentimentalPolarity pol = new SentimentalPolarity(new FileReader(path + "SentiWordNet_3.0.0_20100908.txt"));
    SentimentalPolarity pol = new SentimentalPolarity(new FileReader("SentiWordNet_3.0.0_20100908.txt"));
		return pol.textPolarity(text);
		}catch(Exception e){System.out.println("Erro a calcular a polaridade do texto " + e.getLocalizedMessage());}
		return Double.NaN;
	}

  public static double imagePolarity(String linkImage){
	  try{
			Image image = java.awt.Toolkit.getDefaultToolkit().getDefaultToolkit().createImage(new URL(linkImage));
		return NaiveSimilarityFinder.imageWarmth(image);
		}catch(Exception e){System.out.println("Erro a calcular a polaridade da imagem " + e.getLocalizedMessage());}
		return Double.NaN;
	}

  public static double getVincentysDistance( double lat1, double lon1, double lat2, double lon2 ){
		VincentyDistanceCalculator dist = new VincentyDistanceCalculator();
		double val = dist.getDistance(lat1, lon1, lat2, lon2);
		return val;
	}
}