package com.aliasi.sentences;

import java.io.*;
import java.util.*;
//TODO: import org.tartarus.snowball.ext.PorterStemmer;

/**
 * An implementation of Marti Hearst's text tiling algorithm.
 */
public class TextTilingParagraphModel extends com.aliasi.sentences.AbstractSentenceModel {

	public void boundaryIndices(String[] tokens, String[] whitespaces, int start, int length, Collection<Integer> indices) {
		StringBuffer sb = new StringBuffer();
		for ( int i = start ; i < start+length ; i++ ) {
			sb.append(tokens[i]);
			sb.append(whitespaces[i]);
		}
		String stopwords[] = { "the" , "at" , "on", "a" };
		TextTilingParagraphModel t = new TextTilingParagraphModel(sb.toString(), stopwords);
		t.w = 5;
		t.s = 5;
		t.similarityDetermination();
		t.depthScore();
		t.boundaryIdentification();
		indices.addAll(t.segmentation);
	}

	public static void main(String[] args) {
		String aux;
		try {
			int window;
			int step;
			String input;
			try {
				window = (Integer.valueOf(args[0])).intValue();
				step = (Integer.valueOf(args[1])).intValue();
				input = args[2];
			} catch (Exception e) {
				window = Integer.valueOf(10);
				step = Integer.valueOf(5);
				input = "text.txt";
			}
			String stopwordList = args[2];
			aux = "# Stopword list : " + stopwordList;
			System.out.println(aux);
			aux = "# Window        : " + window;
			System.out.println(aux);
			aux = "# Step          : " + step;
			System.out.println(aux);
			RawText c = new RawText(input);
			aux = "# Collection    : " + c.text.size();
			System.out.println(aux);
			if (c.text.size() <= (window * 2)) {
				aux = "# Fatal error : Window size (" + window + " * 2 = "
						+ (window * 2) + ") larger then collection ("
						+ c.text.size() + ")";
				System.err.println(aux);
				System.exit(1);
			}
			System.out.println();
			String stopwords[] = { "the" , "at" , "on", "a" };
			TextTilingParagraphModel t = new TextTilingParagraphModel(c, stopwords); // Initialise text tiling algorithm with collection
			t.w = window; // Set window size according to user parameter
			t.s = step; // Set step size according to user parameter
			t.similarityDetermination(); // Compute similarity scores
			t.depthScore(); // Compute depth scores using the similarity scores
			t.boundaryIdentification(); // Identify the boundaries
			genOutput(c, t.segmentation); // Generate
		} catch (Exception e) {
			aux = "# Fatal error : " + e;
			System.err.println(aux);
			e.printStackTrace();
			aux = "# Fatal error : Require parameters <window size> <step size> <stopword list> <input file> <output file>";
			System.err.println(aux);
		}
	}

	protected int w = 100; // Size of the sliding window
	protected int s = 10;  // Step size

	/* Token -> stem dictionary */
	protected Hashtable<String,String> stemOf = new Hashtable<String,String>(); // Token -> stem

	/* Similarity scores and the corresponding locations */
	protected float[] sim_score = new float[0];
	protected int[] site_loc = new int[0];

	/* Depth scores */
	protected float[] depth_score = new float[0];

	/* Segment boundaries */
	protected Vector<Integer> segmentation = new Vector<Integer>();

	Set<String> S;
	RawText C;
	
	private TextTilingParagraphModel( String text, String[] stopwords ) {
		C = new RawText(text);
		S = new HashSet<String>();
		for ( String s : stopwords ) S.add(s.toLowerCase());
		preprocess();
	}

	private TextTilingParagraphModel( RawText text, String[] stopwords ) {
		C = text;
		S = new HashSet<String>();
		for ( String s : stopwords ) S.add(s.toLowerCase());
		preprocess();
	}

	protected void blockAdd(final String term, Hashtable<String, Integer> B) {
		Integer freq = B.get(term);
		if (freq == null)
			freq = new Integer(1);
		else
			freq = new Integer(freq.intValue() + 1);
		B.put(term, freq);
	}

	/** Compute the cosine similarity measure for two blocks */
	protected float blockCosine(final Hashtable<String,Integer> B1, final Hashtable<String,Integer> B2) {
		/* 1. Declare variables */
		int W; // Weight of a term (temporary variable)
		int sq_b1 = 0; // Sum of squared weights for B1
		int sq_b2 = 0; // Sum of squared weights for B2
		int sum_b = 0; // Sum of product of weights for common terms in B1 and B2
		/* 2. Compute the squared sum of term weights for B1 */
		for (Enumeration<Integer> e = B1.elements(); e.hasMoreElements();) {
			W = e.nextElement().intValue();
			sq_b1 += (W * W);
		}
		/* 3. Compute the squared sum of term weights for B2 */
		for (Enumeration<Integer> e = B2.elements(); e.hasMoreElements();) {
			W = e.nextElement().intValue();
			sq_b2 += (W * W);
		}
		/* 4. Compute sum of term weights for common terms in B1 and B2 */
		/* 4.1. Union of terms in B1 and B2 */
		Hashtable<String,Boolean> union = new Hashtable<String,Boolean>(B1.size() + B2.size());
		for (Enumeration<String> e = B1.keys(); e.hasMoreElements();)
			union.put(e.nextElement(), new Boolean(true));
		for (Enumeration<String> e = B2.keys(); e.hasMoreElements();)
			union.put(e.nextElement(), new Boolean(true));
		/* 4.2. Compute sum */
		Integer W1; // Weight of a term in B1 (temporary variable)
		Integer W2; // Weight of a term in B2 (temporary variable)
		String term; // A term (temporary variable)
		for (Enumeration<String> e = union.keys(); e.hasMoreElements();) {
			term = e.nextElement();
			W1 = B1.get(term);
			W2 = B2.get(term);
			if (W1 != null && W2 != null)
				sum_b += (W1.intValue() * W2.intValue());
		}
		/* 5. Compute similarity */
		float sim;
		sim = (float) sum_b / (float) Math.sqrt(sq_b1 * sq_b2);
		return sim;
	}

	protected void blockRemove(final String term, Hashtable<String,Integer> B) {
		Integer freq = B.get(term);
		if (freq != null) {
			if (freq.intValue() == 1) B.remove(term);
			else B.put(term, new Integer(freq.intValue() - 1));
		}
	}

	protected void boundaryIdentification() {
		/* Declare variables */
		float mean = 0; // Mean depth score
		float sd = 0; // S.D. of depth score
		float threshold; // Threshold to use for determining boundaries
		int neighbours = 3; // The area to check before assigning boundary
		/* Compute mean and s.d. from depth scores */
		for (int i = depth_score.length; i-- > 0;) mean += depth_score[i];
		mean = mean / depth_score.length;
		for (int i = depth_score.length; i-- > 0;) sd += Math.pow(depth_score[i] - mean, 2);
		sd = sd / depth_score.length;
		/* Compute threshold */
		threshold = mean - sd / 2;
		/* Identify segments in pseudo-sentence terms */
		Vector<Integer> pseudo_boundaries = new Vector<Integer>();
		boolean largest = true; // Is the potential boundary the largest in the local area?
		for (int i = depth_score.length; i-- > 0;) {
			/* Found a potential boundary */
			if (depth_score[i] >= threshold) {
				/* Check if the nearby area has anything better */
				largest = true;
				/* Scan left */
				for (int j = neighbours; largest && j > 0 && (i - j) > 0; j--) {
					if (depth_score[i - j] > depth_score[i])
						largest = false;
				}
				/* Scan right */
				for (int j = neighbours; largest && j > 0
						&& (i + j) < depth_score.length; j--) {
					if (depth_score[i + j] > depth_score[i])
						largest = false;
				}
				/* Lets make the decision */
				if (largest)
					pseudo_boundaries.addElement(new Integer(site_loc[i]));
			}
		}
		/* Convert pseudo boundaries into real boundaries. We use the nearest true boundary. */
		/* Convert real boundaries into array for faster access */
		int[] true_boundaries = new int[C.boundaries.size()];
		for (int i = true_boundaries.length; i-- > 0;)
			true_boundaries[i] = (C.boundaries.elementAt(i)).intValue();
		int pseudo_boundary;
		int distance; // Distance between pseudo and true boundary
		int smallest_distance; // Shortest distance
		int closest_boundary; // Nearest real boundary
		for (int i = pseudo_boundaries.size(); i-- > 0;) {
			pseudo_boundary = (pseudo_boundaries.elementAt(i)).intValue();
			/* This is pretty moronic, but it works. Can definitely be improved */
			smallest_distance = Integer.MAX_VALUE;
			closest_boundary = true_boundaries[0];
			for (int j = true_boundaries.length; j-- > 0;) {
				distance = Math.abs(true_boundaries[j] - pseudo_boundary);
				if (distance <= smallest_distance) {
					smallest_distance = distance;
					closest_boundary = true_boundaries[j];
				}
			}
			segmentation.addElement(new Integer(closest_boundary));
		}
	}

	protected void depthScore() {
		/* Declare variables */
		float maxima = 0; // Local maxima
		float dleft = 0; // Difference for the left side
		float dright = 0; // Difference for the right side
		/* For each position, compute depth score */
		depth_score = new float[sim_score.length];
		for (int i = sim_score.length; i-- > 0;) {
			/* Scan left */
			maxima = sim_score[i];
			for (int j = i; j > 0 && sim_score[j] >= maxima; j--)
				maxima = sim_score[j];
			dleft = maxima - sim_score[i];
			/* Scan right */
			maxima = sim_score[i];
			for (int j = i; j < sim_score.length && sim_score[j] >= maxima; j++)
				maxima = sim_score[j];
			dright = maxima - sim_score[i];
			/* Declare depth score */
			depth_score[i] = dleft + dright;
		}
	}


	protected static void genOutput( RawText c, Vector<Integer> seg ) throws IOException {
		/* Declare variables */
		Vector<String> text = c.text; // The text
		Vector<Integer> sentence = c.boundaries; // Sentence boundaries
		int start, end; // Sentence boundaries
		String aux = "";
		aux = "==========";
		System.out.println(aux);
		for (int i = 1; i < sentence.size(); i++) {
			/* Get sentence boundaries */
			start = (sentence.elementAt(i - 1)).intValue();
			end = (sentence.elementAt(i)).intValue();
			/* If start is a topic boundary, print marker */
			if (seg.contains(new Integer(start))) {
				aux = "\n==========";
				System.out.println(aux);
			}
			/* Print a sentence */
			for (int j = start; j < end; j++) {
				aux = text.elementAt(j) + " ";
				System.out.print(aux);
			}
			System.out.println();
		}
		/* The implicit boundary at the end of the file */
		aux = "\n==========";
		System.out.println(aux);
	}

	protected boolean include(int i) {
		/*
		 * Noise reduction by filtering out everything but nouns and verbs -
		 * Best but requires POS tagging String pos = (String)
		 * C.pos.elementAt(i); return (pos.startsWith("N") ||
		 * pos.startsWith("V"));
		 */
		/* Noise reduction by stopword removal - OK */
		String token = C.text.elementAt(i);
		return !S.contains(token.toLowerCase());
	}

	protected void preprocess() {
		//TODO: PorterStemmer stemmer = new PorterStemmer();
		Vector<String> text = C.text; // Text of the collection
		String token; // A token
		/* Construct a dictionary of tokens */
		for (int i = text.size(); i-- > 0;) {
			token = text.elementAt(i);
			stemOf.put(token, token);
		}
		/* Complete mapping token -> stem */
		for (Enumeration<String> e = stemOf.keys(); e.hasMoreElements();) {
			token = e.nextElement();
			//TODO: stemmer.setCurrent(token);
			//TODO: stemmer.stem();
			//TODO: stemOf.put(token, stemmer.getCurrent());
		}
	}

	protected void similarityDetermination() {
		/* Declare variables */
		Vector<String> text = C.text; // The source text
		Hashtable<String,Integer> left = new Hashtable<String,Integer>(); // Left sliding window
		Hashtable<String,Integer> right = new Hashtable<String,Integer>(); // Right sliding window
		Vector<Float> score = new Vector<Float>(); // Scores
		Vector<Integer> site = new Vector<Integer>(); // Locations
		/* Initialise windows */
		for (int i = w; i-- > 0;)
			blockAdd(stemOf.get(text.elementAt(i)), left);
		for (int i = w * 2; i-- > w;)
			blockAdd(stemOf.get(text.elementAt(i)), right);
		/* Slide window and compute score */
		final int end = text.size() - w; // Last index to check
		String token; // A stem
		int step = 0; // Step counter
		int i; // Counter
		for (i = w; i < end; i++) {
			/* Compute score for a step */
			if (step == 0) {
				score.addElement(new Float(blockCosine(left, right)));
				site.addElement(new Integer(i));
				step = s;
			}
			/* Remove word which is at the very left of the left window */
			if (include(i - w)) {
				blockRemove(
						stemOf.get(text.elementAt(i - w)),
						left);
			}
			/*
			 * Add current word to the left window and remove it from the right
			 * window
			 */
			if (include(i)) {
				token = text.elementAt(i);
				blockAdd(stemOf.get(token), left);
				blockRemove(stemOf.get(token), right);
			}
			/* Add the first word after the very right of the right window */
			if (include(i + w)) {
				blockAdd(stemOf.get(text.elementAt(i + w)),
						right);
			}
			step--;
		}
		/* Compute score for the last step */
		if (step == 0) {
			score.addElement(new Float(blockCosine(left, right)));
			site.addElement(new Integer(i));
			step = s;
		}
		/* Smoothing with a window size of 3 */
		sim_score = new float[score.size() - 2];
		site_loc = new int[site.size() - 2];
		for (int j = 0; j < sim_score.length; j++) {
			sim_score[j] = (( score.elementAt(j)).floatValue()
					+ ( score.elementAt(j + 1)).floatValue() + ( score
					.elementAt(j + 2)).floatValue()) / 3;
			site_loc[j] = ( site.elementAt(j + 1)).intValue();
		}

	}

}

class RawText {

	public Vector<String> text = new Vector<String>(300,50);

	public Vector<Integer> boundaries = new Vector<Integer>(100,20);

	public RawText() { super(); }

	public RawText(Reader in) {
		try {
			Reader r = new BufferedReader(in);
			parse(r);
			r.close();
		} catch (Exception e) {}
	}

	public RawText(InputStream in) {
		try {
			Reader r = new BufferedReader(new InputStreamReader(in));
			parse(r);
			r.close();
		} catch (Exception e) {}
	}

	public RawText(File file) {
		try {
			Reader r = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
			parse(r);
			r.close();
		} catch (Exception e) {}
	}

	public RawText(String data) {
		try {
			Reader r = new BufferedReader(new StringReader(data));
			parse(r);
			r.close();
		} catch (Exception e) {}
	}

	protected void parse(Reader r) {
		StreamTokenizer tk = new StreamTokenizer(r);
		tk.resetSyntax();
		tk.wordChars('\u0021', '\u00FF');
		tk.whitespaceChars('\u0000', '\u0020');
		tk.eolIsSignificant(true);
		tk.lowerCaseMode(false);
		int word = 0; // Absolute word count, first word is word 0
		int prev_word = -1; // The location of the previous boundary
		boundaries.addElement(new Integer(word));
		try {
			while (tk.nextToken() != StreamTokenizer.TT_EOF) {
			if (tk.ttype == StreamTokenizer.TT_EOL && word != prev_word) {
				boundaries.addElement(new Integer(word));
				prev_word = word; 
			} else if (tk.ttype == StreamTokenizer.TT_WORD) {
				text.addElement(tk.sval);
				word++;
			}
		}
	} catch (IOException e) { }
}

}