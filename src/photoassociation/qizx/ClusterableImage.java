package photoassociation.qizx;

import java.net.URL;
import java.util.List;
import java.util.ArrayList;
import java.awt.Image;
import java.awt.image.BufferedImage;

public class ClusterableImage extends Clusterable<BufferedImage> {

	BufferedImage img;
	
	String id;
	
	public ClusterableImage ( URL url ) {
		Image image = java.awt.Toolkit.getDefaultToolkit().getDefaultToolkit().createImage(url);
		img = NaiveSimilarityFinder.toBufferedImage(image);
		id = url.toExternalForm();
	}	
	
	public ClusterableImage ( String linkImage) {
		Image image = java.awt.Toolkit.getDefaultToolkit().getDefaultToolkit().createImage(linkImage);
		img = NaiveSimilarityFinder.toBufferedImage(image);
		id = linkImage;
	}
	
	public ClusterableImage ( BufferedImage img, String id ) {
		this.img = img;
	} 

	public double getDistance ( Clusterable<BufferedImage> element ) {
		return NaiveSimilarityFinder.imageDistance(img,((ClusterableImage)element).img);
	}
	
	public BufferedImage getImage ( ) { return img; }
	
	public String getID ( ) { return id; }

}