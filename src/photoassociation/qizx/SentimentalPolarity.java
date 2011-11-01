package photoassociation.qizx;

import java.util.HashMap;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.Reader;
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;
import com.aliasi.tokenizer.Tokenizer;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

public class SentimentalPolarity {

	HashMap<String,Double> lexicon = new HashMap<String,Double>();
	
	public SentimentalPolarity ( Reader lex ) {
		HashMap<String, Vector<Double>> _temp = new HashMap<String, Vector<Double>>();
		try {
			BufferedReader csv =  new BufferedReader(lex);
			String line = "";
			while((line = csv.readLine()) != null) {
				String[] data = line.split("\t");
				Double score = Double.parseDouble(data[2])-Double.parseDouble(data[3]);
				String[] words = data[4].split(" ");
				for(String w : words) {
					String[] w_n = w.split("#");
					w_n[0] += "#"+data[0];
					int index = Integer.parseInt(w_n[1])-1;
					if(_temp.containsKey(w_n[0])) {
						Vector<Double> v = _temp.get(w_n[0]);
						if(index>v.size()) for(int i = v.size();i<index; i++) v.add(0.0);
						v.add(index, score);
						_temp.put(w_n[0], v);
					} else {
						Vector<Double> v = new Vector<Double>();
						for(int i = 0;i<index; i++) v.add(0.0);
						v.add(index, score);
						_temp.put(w_n[0], v);
					}
				}
			}
			Set<String> temp = _temp.keySet();
			for (Iterator<String> iterator = temp.iterator(); iterator.hasNext();) {
				String word = (String) iterator.next();
				Vector<Double> v = _temp.get(word);
				double score = 0.0;
				double sum = 0.0;
				for(int i = 0; i < v.size(); i++) score += ((double)1/(double)(i+1))*v.get(i);
				for(int i = 1; i<=v.size(); i++) sum += (double)1/(double)i;
				score /= sum;
				word = word.split("#")[0];
				lexicon.put(word, score);
			}
		}
		catch(Exception e){e.printStackTrace();}		
	}
	
	public double textPolarity ( String text ) {
		Tokenizer toks = IndoEuropeanTokenizerFactory.INSTANCE.tokenizer(text.toCharArray(), 0, text.length());
		double sum  = 0;
		double norm = 0;
		for ( String tok : toks.tokenize() )
			if (lexicon.containsKey(tok.toLowerCase())) {
			sum += lexicon.get(tok.toLowerCase());
			norm++;
		}
		if ( norm == 0 ) return 0;
		return sum / norm;
	}
	
	public static void main ( String args[] ) throws Exception {
		SentimentalPolarity pol = new SentimentalPolarity(new FileReader("SentiWordNet_3.0.0_20100908.txt"));
		System.out.println(pol.textPolarity("This is a terrible example"));
		System.out.println(pol.textPolarity("This is a good example"));
		System.out.println(pol.textPolarity("secure"));
	}
		
}