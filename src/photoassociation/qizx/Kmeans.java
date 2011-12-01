package photoassociation.qizx;

import java.util.ArrayList;
import java.util.List;

public class Kmeans<T extends Clusterable> {

	private List<Cluster<T>> clusters = new ArrayList<Cluster<T>>();

	public List<List<T>> cluster(List<T> elements) {
		Double validity = Double.MIN_VALUE;
		List<List<T>> results = null;
		List<List<T>> prevResults = null;
		int max = elements.size();
		for ( int k = 2; k < max; k++ ) {
			prevResults = results;
			results = cluster(elements,k);
			double val2 = clusterValidity();
			if ( val2 > validity ) validity = val2; else { 
				if ( prevResults == null ) return results;
				return prevResults; 
			}
		}
		return results;
	}        
	
	public List<List<T>> cluster(List<T> elements, int k) {
		clusters = new ArrayList<Cluster<T>>();
		if (k >= elements.size()) {
			System.out.println("K should not be greater then N");
			return null;
		}
		if (elements == null || elements.size() == 0) {
			System.out.println("The elements list has no objects");
		}
		List<Integer> indices = new ArrayList<Integer>();
		for (int i = 0; i < k; i++) {
			int x = ((Number) (Math.random() * elements.size())).intValue();
			while (indices.contains(x)) x = ((Number) (Math.random() * elements.size())).intValue();
			indices.add(x);
			Centroide<T> centroid = new Centroide<T>(elements.get(x));
			Cluster<T> cluster = new Cluster<T>(centroid);
			clusters.add(cluster);
		}
		int index = 0;
		for (T element : elements) {
			clusters.get(index % k).addElementToCluster(element);
			index++;
		}
		boolean change = true;
		int i = 0;
		while (change && i < k * elements.size()) {
			change = false;
			for (T element : elements) {
				Cluster<T> current = belongsTo(element);
				Cluster<T> newest = current;
				for (Cluster<T> cluster : clusters) {
					if (cluster != current) {
						double newDistance = newest.getCentroid().distanceToCentroid(element);						
						double clusterDistance = cluster.getCentroid().distanceToCentroid(element);
						if (clusterDistance < newDistance) {
							newest = cluster;
							change = true;
						}
					}
				}
				if (change == true && current != newest) {
					current.removeElementFromCluster(element);
					newest.addElementToCluster(element);
				}
			}
			i++;
			for (Cluster<T> cluster : clusters) {
				Centroide<T> centroid = new Centroide((T) (elements.get(0).centroidOfCluster(cluster.getData())));
				cluster.setCentroid(centroid);
			}
		}
		List<List<T>> response = new ArrayList<List<T>>();
		for (Cluster<T> c : clusters) {
			List<T> data = new ArrayList<T>();
			for (T element : c.getData()) data.add(element);
			response.add(data);
		}
		return response;
	}

	private Cluster<T> belongsTo(T element) {
		for (Cluster<T> c : clusters) if (c.contains(element)) return c;
		return null;
	}

	public double clusterValidity () {
		double intra = 0;
		double inter = Double.MAX_VALUE;
		double numPoints = 0;
		for (Cluster<T> c : clusters) {
			numPoints = 0;
			List<T> data = new ArrayList<T>();
			for (T element : c.getData()) {
				numPoints++;
				intra += element.getDistance(element.centroidOfCluster(c));
			}
			for (Cluster<T> c2 : clusters) {
				if ( c.equals(c2) ) continue;
				Clusterable<T> centroid = c.getData().get(0).centroidOfCluster(c);
				inter = Math.min(inter,centroid.getDistance(c2.getData().get(0).centroidOfCluster(c2)));
			}
		}
		return (intra / numPoints ) / inter;
	}

}

class Cluster<T extends Clusterable> {

	private Centroide<T> centroid;
	private List<T> data;

	public Cluster(Centroide<T> centroid) {
		this.centroid = centroid;
		this.data = new ArrayList<T>();
	}

	public Centroide<T> getCentroid() {
		return centroid;
	}

	public void setCentroid(Centroide<T> centroid) {
		this.centroid = centroid;
	}

	public List<T> getData() {
		return data;
	}

	public void setData(List<T> data) {
		this.data = data;
	}

	public void addElementToCluster(T element) {
		data.add(element);
	}

	public boolean removeElementFromCluster(T element) {
		boolean response = data.remove(element);
		return response;
	}

	public boolean contains(T element) {
		return data.contains(element);
	}

}

class Centroide<T extends Clusterable> {

	private T center;
	private Cluster<T> cluster;

	public Centroide(T center) {
		this.center = center;
	}

	public T getCenter() {
		return center;
	}

	public void setCenter(T centro) {
		this.center = centro;
	}

	public double distanceToCentroid(T element) {
		return element.getDistance(center);
	}

	public Cluster<T> getCluster() {
		return cluster;
	}

	public void setCluster(Cluster<T> cluster) {
		this.cluster = cluster;
	}

}

abstract class Clusterable<T> {

	public abstract double getDistance(Clusterable<T> element);

	public Clusterable<T> centroidOfCluster(List<Clusterable<T>> list) {
		double min = Double.MAX_VALUE;
		Clusterable<T> result = null;
		for (int p1 = 0; p1 < list.size(); p1++) {
			double dist = 0;
			for (int p2 = 0; p2 < list.size(); p2++) {
				if (p1 != p2)
					dist += list.get(p1).getDistance(list.get(p2));
			}
			if (dist < min) {
				min = dist;
				result = list.get(p1);
			}
		}
		return result;
	}

	public Clusterable<T> centroidOfCluster(Cluster<Clusterable<T>> cluster) {
		return centroidOfCluster(cluster.getData());
	}

}
