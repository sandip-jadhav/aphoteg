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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
	
	public static double cossineSimilarity(Object vocabulario, String doc, String query){
		int numDocs = 950;
		
		Map<String,Integer> vocabulary = (Map<String,Integer>) vocabulario;
		
		TreeMap<String, Double> vectorDoc = new TreeMap<String, Double>();
		TreeMap<String, Double> vectorQuery = new TreeMap<String, Double>();
		
		List<String> listDoc = Arrays.asList(parser(doc));
		List<String> listQuery = Arrays.asList(parser(query));
		
		// Apenas Docs sem stopwords
		for(String term : listDoc) {
			double valDoc, valQuery;
			if(containsIgnoreCase(listDoc,term)){
				double tf = getTF(doc,term);
				double idf = getIDF(vocabulary, term, numDocs);
				valDoc = tf * idf;
			} else{ valDoc = 0.00;}
			vectorDoc.put(term, valDoc);
			if(containsIgnoreCase(listQuery,term)){
				double tf = getTF(doc,term);
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

}