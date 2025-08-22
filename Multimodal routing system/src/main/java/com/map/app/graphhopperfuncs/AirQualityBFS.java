package com.map.app.graphhopperfuncs;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import com.graphhopper.GraphHopper;
import com.graphhopper.coll.GHBitSet;
import com.graphhopper.coll.GHBitSetImpl;
import com.graphhopper.routing.ev.DecimalEncodedValue;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.storage.Graph;
import com.graphhopper.util.EdgeExplorer;
import com.graphhopper.util.EdgeIterator;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.XFirstSearch;
import com.map.app.model.AirQuality;
import com.map.app.service.TransportMode;

/**
 * @author Siftee
 */
public class AirQualityBFS extends XFirstSearch {
	// does a BFS traversal and assigns edge with air quality value as average of
	// aqi value of base and adjacent node.
	private final Graph gh;
	private final GraphHopper hopper;
	private final ArrayList<AirQuality> ap;

	public AirQualityBFS(GraphHopper hopper, Graph gh, ArrayList<AirQuality> ap) {
		this.gh = gh;
		this.hopper = hopper;
		this.ap = ap;
	}

	@Override
	protected GHBitSet createBitSet() {
		return new GHBitSetImpl();
	}
	//public double get
	@Override
	public void start(EdgeExplorer explorer, int temp) {
		// Print debug information about air quality data being used
		System.out.println("\n===== AIR QUALITY ROUTING DATA =====");
		System.out.println("Using " + ap.size() + " air quality data points for routing");
		System.out.println("Sample of air quality data being used for routing:");
		
		// Print first 5 air quality points
		int displayCount = 0;
		for (AirQuality airQuality : ap) {
			if (displayCount < 5) {
				System.out.println(airQuality);
				displayCount++;
			} else if (displayCount == 5) {
				System.out.println("... (more data points)");
				break;
			}
		}
		int defaultSmoke;
		Properties prop=new Properties();
		try (FileInputStream ip = new FileInputStream("config.properties")) {
			prop.load(ip);
			defaultSmoke = Integer.parseInt(prop.getProperty("default_smoke"));
		} catch (IOException e) {
			throw new RuntimeException("Config properties are not found. Aborting ...");
		}

		for (TransportMode mode : TransportMode.values()) {
			FlagEncoder encoder = hopper.getEncodingManager().getEncoder(mode.toString());
			DecimalEncodedValue smokeEnc = encoder.getDecimalEncodedValue("smoke");
			// SimpleIntDeque fifo = new SimpleIntDeque();
			// GHBitSet visited = createBitSet();
			// System.out.println("KYA"+gh.getNodes());
			int count = 0;
			int processedEdges = 0;
			Set<Integer> edge_uni=new HashSet<>();
			System.out.println("Processing nodes from " + temp + " to " + gh.getNodes());
			for (int startNode = temp; startNode < gh.getNodes(); startNode++) {
				EdgeIterator edgeIterator = explorer.setBaseNode(startNode);
				while (edgeIterator.next()) {
					EdgeIteratorState edge = gh.getEdgeIteratorState(edgeIterator.getEdge(), edgeIterator.getAdjNode());
					edgeIterator.getEdge();
					if(edge_uni.contains(edgeIterator.getEdge()))
					{
						continue;
					}
					int connectedId = edgeIterator.getAdjNode();
					double base_lat = gh.getNodeAccess().getLat(startNode);
					double base_lon = gh.getNodeAccess().getLon(startNode);
					double airQualityBase = IDW(base_lat, base_lon);
					double adjacent_lat = gh.getNodeAccess().getLat(connectedId);
					double adjacent_lon = gh.getNodeAccess().getLon(connectedId);
					double airQualityAdj = IDW(adjacent_lat, adjacent_lon);
					if (Double.isNaN(airQualityAdj) || Double.isNaN(airQualityBase)) {
						edge.set(smokeEnc, defaultSmoke);
						edge.setReverse(smokeEnc, defaultSmoke);
						count++;
					} else {
						edge.set(smokeEnc, Math.max(convToConcentration((airQualityBase + airQualityAdj) / 2), defaultSmoke));
						edge.setReverse(smokeEnc, Math.max(convToConcentration((airQualityBase + airQualityAdj) / 2), defaultSmoke));
						count++;
					}
					edge_uni.add(edge.getEdge());
					processedEdges++;
				}
			}
			System.out.println("Transport mode " + mode + ": Applied air quality data to " + processedEdges + " road segments");
		}
		System.out.println("=====================================");
	}

	// micro gm / m^3
	public double convToConcentration(double aqi)
	{
		if(aqi>=0 && aqi<=50)		
		{			return aqi*0.308;		
		}		
		else if(aqi>=51 && aqi<=100)		
		{			
			return ((aqi-51)*0.508) + 15.5;		
		}		
		else if(aqi>=101 && aqi<=150)		
		{			
			return ((aqi-101)*0.508)+40.5;		
		}		
		else if(aqi>=151 && aqi<=200)	
		{			
			return ((aqi-151)*1.73)+65.5;		
		}		
		else if(aqi>=201 && aqi<=300)		
		{			
			return ((aqi-201)*1.009)+150.5;		
		}		
		else if(aqi>=301 && aqi<=400) 
			return ((aqi-301)*1.009)+250.2; 
		else		
		{	
			return ((aqi-401)*1.51)+350.5;		
		}
	}
	
	private double IDW(double fromlat, double fromlon) {
		double numer = 0;
		double denom = 0;
		double exp = 2;
		for (AirQuality point : ap) {
			double v = point.getAqi();
			double d = haversine(point.getLat(), point.getLon(), fromlat, fromlon);
			if (d == 0) {
				return point.getAqi();
			}
			numer = numer + (v / Math.pow(d, exp));
			denom = denom + (1 / Math.pow(d, exp));
		}
		return numer / denom;
	}

	private double haversine(double lat1, double lon1, double lat2, double lon2) {
		// distance between latitudes and longitudes
		double dLat = Math.toRadians(lat2 - lat1);
		double dLon = Math.toRadians(lon2 - lon1);

		// convert to radians
		lat1 = Math.toRadians(lat1);
		lat2 = Math.toRadians(lat2);

		// apply formulae
		double a = Math.pow(Math.sin(dLat / 2), 2) + Math.pow(Math.sin(dLon / 2), 2) * Math.cos(lat1) * Math.cos(lat2);
		double rad = 6371;
		double c = 2 * Math.asin(Math.sqrt(a));
		return rad * c;
	}

}