package photoassociation.qizx;

import java.net.URL;
import java.util.List;
import java.util.ArrayList;
import java.awt.Image;
import java.awt.image.BufferedImage;

public class ClusterImages {

	public static void main ( String args[] ) throws Exception {
		Kmeans<ClusterableImage> kmeans = new Kmeans<ClusterableImage>();
		List<ClusterableImage> elements = new ArrayList<ClusterableImage>();
		ClusterableImage mainImage = new ClusterableImage(new URL("http://farm2.static.flickr.com/1128/1226915900_eea86783cd_m.jpg"));
		elements.add(new ClusterableImage(new URL("http://farm2.static.flickr.com/1128/1226915900_eea86783cd_m.jpg")));
		elements.add(new ClusterableImage(new URL("http://farm2.static.flickr.com/1179/1226784048_d634ee1bc2_m.jpg")));
		elements.add(new ClusterableImage(new URL("http://farm4.static.flickr.com/3240/3099631438_e023018236_m.jpg")));
		elements.add(new ClusterableImage(new URL("http://farm6.static.flickr.com/5081/5280965669_b9f42b0519_m.jpg")));
		for ( int i = 0; i < elements.size(); i++) {
			System.out.println("Image warmth for " + elements.get(i).getID() + " : " + NaiveSimilarityFinder.imageWarmth(elements.get(i).getImage()));
		}
		List<List<ClusterableImage>> clusters = kmeans.cluster(elements);
		for ( int i = 0; i < clusters.size(); i++ ) {
			System.out.println("Results for cluster " + (i+1));
			for ( ClusterableImage img : clusters.get(i)) {
				double distance = (1.0 / (1.0 + img.getDistance(mainImage)));
				System.out.println("Distance " + img.getDistance(mainImage));
				System.out.println("Distance True " + distance);
				System.out.println("\t" + img.id);
			}
		}
	}

}