package com.map.app.containers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;
import java.util.concurrent.locks.Lock;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.graphhopper.routing.util.AllEdgesIterator;
import com.graphhopper.util.shapes.BBox;
import com.map.app.service.TrafficAndRoutingService;
import com.map.app.service.TransportMode;
import com.graphhopper.GraphHopper;
import com.graphhopper.routing.ev.DecimalEncodedValue;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.storage.Graph;
import com.graphhopper.storage.index.LocationIndex;
import com.graphhopper.storage.index.Snap;
import com.graphhopper.util.EdgeIteratorState;
import com.map.app.model.TrafficData;

import static com.map.app.containers.RoutePathContainer.initializeResultsCSV;
import static com.map.app.containers.RoutePathContainer.writeResults;

/**
 * @author Siftee, Amit
 */
public class TrafficDataExtractor {
    private TrafficData dt = new TrafficData();

    private final Lock writeLock;

    public GraphHopper getHopper() {
        return hopper;
    }

    private final GraphHopper hopper;

    public TrafficDataExtractor(GraphHopper hopper, Lock lock) {
        this.hopper = hopper;
        this.writeLock = lock;
    }

    public void readHEREMapData(String apiKey, BBox boundingBox) {
        // Updated to use HERE Traffic API v7 instead of v6
        // HERE Traffic API v7 requires bounding box width and height to be at most 1 degree
        
        // Calculate dimensions of the bounding box
        double width = boundingBox.maxLon - boundingBox.minLon;
        double height = boundingBox.maxLat - boundingBox.minLat;
        
        // If the bounding box is within limits, make a single API call
        if (width <= 1.0 && height <= 1.0) {
            makeApiCallForBbox(apiKey, boundingBox.minLat, boundingBox.minLon, boundingBox.maxLat, boundingBox.maxLon);
            return;
        }
        
        // Otherwise, split the bounding box into smaller sections
        // Calculate number of splits needed in each dimension
        int lonSplits = (int) Math.ceil(width);
        int latSplits = (int) Math.ceil(height);
        
        System.out.println("Splitting bounding box into " + lonSplits + "x" + latSplits + " sections");
        
        // Calculate step size for each dimension
        double lonStep = width / lonSplits;
        double latStep = height / latSplits;
        
        // Make API calls for each smaller section
        for (int i = 0; i < lonSplits; i++) {
            double minLon = boundingBox.minLon + (i * lonStep);
            double maxLon = Math.min(minLon + lonStep, boundingBox.maxLon);
            
            for (int j = 0; j < latSplits; j++) {
                double minLat = boundingBox.minLat + (j * latStep);
                double maxLat = Math.min(minLat + latStep, boundingBox.maxLat);
                
                // Make API call for this section
                makeApiCallForBbox(apiKey, minLat, minLon, maxLat, maxLon);
            }
        }
    }
    
    private void makeApiCallForBbox(String apiKey, double minLat, double minLon, double maxLat, double maxLon) {
        // HERE Traffic API v7 format: in=bbox:{westLongitude},{southLatitude},{eastLongitude},{northLatitude}
        final String URL = "https://data.traffic.hereapi.com/v7/flow"
                + "?apiKey=" + apiKey 
                + "&in=bbox:" + minLon + "," + minLat + "," 
                + maxLon + "," + maxLat
                + "&locationReferencing=shape"
                + "&return=shape,functional_class,speed"
                + "&units=metric";
        System.out.println("API Call: " + URL);
        parseHEREMapJSON(URL);
    }

    public void feed(TrafficData tempdt) {
        writeLock.lock();
        try {
            lockedFeed(tempdt);
        } finally {
            writeLock.unlock();
        }
    }

    private void lockedFeed(TrafficData tempdt) {
        this.dt = tempdt;
        Graph graph = hopper.getGraphHopperStorage().getBaseGraph();
    
        for (TransportMode mode : TransportMode.values()) {
            FlagEncoder encoder = hopper.getEncodingManager().getEncoder(mode.toString());
            DecimalEncodedValue avgSpeedEnc = encoder.getAverageSpeedEnc();
            LocationIndex locationIndex = hopper.getLocationIndex();
    
            Set<Integer> edgeIds = new HashSet<>();
            for (int i = 0; i < dt.getLat().size(); i++) {
                List<Float> entryLats = dt.getLat().get(i);
                List<Float> entryLons = dt.getLons().get(i);
                List<Float> entrySpeed = dt.getSpeed().get(i);
    
                for (int j = 0; j < entryLats.size(); j++) {
                    Float latitude = entryLats.get(j);
                    Float longitude = entryLons.get(j);
                    Snap qr = locationIndex.findClosest(latitude, longitude, EdgeFilter.ALL_EDGES);
                    if (!qr.isValid()) continue;
    
                    int edgeId = qr.getClosestEdge().getEdge();
                    if (edgeIds.contains(edgeId)) continue;
    
                    edgeIds.add(edgeId);
                    EdgeIteratorState edgeIteratorState = graph.getEdgeIteratorState(edgeId, qr.getClosestEdge().getAdjNode());
    
                    double value;
                    switch (TrafficAndRoutingService.speedChoice) {
                        case avg_actual_from_hereMaps:
                        default:
                            value = entrySpeed.get(0);
                            break;
                        case free_flow_from_hereMaps:
                            value = entrySpeed.get(1);
                            break;
                        case lower_of_two:
                            value = Math.min(entrySpeed.get(0), entrySpeed.get(1));
                            break;
                    }
    
                    // Apply scaling per transport mode
                    if (mode == TransportMode.motorcycle) {
                        value *= 0.9; 
                    } else if (mode == TransportMode.bike) {
                        value *= 0.4; 
                    } else if (mode == TransportMode.foot) {
                        value = 4.3; 
                    }
    
                    value = Math.min(value, avgSpeedEnc.getMaxDecimal());
    
                    if (value > 0) {
                        edgeIteratorState.set(avgSpeedEnc, value);
                    } else {
                        edgeIteratorState.set(avgSpeedEnc, avgSpeedEnc.getMaxDecimal());
                    }
                }
            }
    
            // Update travel time
            DecimalEncodedValue avgTimeEnc = encoder.getDecimalEncodedValue("time");
            AllEdgesIterator allEdges = graph.getAllEdges();
            while (allEdges.next()) {
                int edgeId = allEdges.getEdge();
                int adjNode = allEdges.getAdjNode();
                EdgeIteratorState edgeIteratorState = graph.getEdgeIteratorState(edgeId, adjNode);
                double speed = edgeIteratorState.get(avgSpeedEnc);
                double time = edgeIteratorState.getDistance() / (speed * 3.6);
                if (speed == 0) {
                    time = edgeIteratorState.getDistance() / (avgSpeedEnc.getMaxDecimal() * 3.6);
                }
                edgeIteratorState.set(avgTimeEnc, time);
                edgeIteratorState.setReverse(avgTimeEnc, time);
            }
        }
    }
    


    // XML parsing method removed as we've migrated to HERE Traffic API v7
    
    /**
     * Parse JSON data from HERE Maps API v7
     */
    private void parseHEREMapJSON(String Url) {
        try {
            // Create URL and open connection
            URL url = new URL(Url);
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            
            // Parse JSON response
            JSONParser parser = new JSONParser();
            JSONObject jsonResponse = (JSONObject) parser.parse(reader);
            
            // Initialize traffic data container
            TrafficData tempdt = new TrafficData();
            tempdt.setSpeed(new ArrayList<>());
            tempdt.setLat(new ArrayList<>());
            tempdt.setLons(new ArrayList<>());
            
            // Get results array from response
            JSONArray results = (JSONArray) jsonResponse.get("results");
            if (results == null) {
                System.out.println("No results found in traffic data");
                return;
            }
            
            // Process each flow segment
            for (Object resultObj : results) {
                JSONObject result = (JSONObject) resultObj;
                
                // Get the location information
                JSONObject location = (JSONObject) result.get("location");
                if (location == null) continue;
                
                // Get shape points (coordinates)
                JSONObject locationReferencing = (JSONObject) location.get("shape");
                if (locationReferencing == null) continue;
                
                JSONArray coordinates = (JSONArray) locationReferencing.get("coordinates");
                if (coordinates == null || coordinates.isEmpty()) continue;
                
                // Get functional class (road type)
                Long functionalClass = (Long) location.get("functionalClass");
                float fc = functionalClass != null ? functionalClass.floatValue() : 5;
                
                // Get flow data
                JSONObject flowData = (JSONObject) result.get("currentFlow");
                if (flowData == null) continue;
                
                // Get speed values
                JSONObject speed = (JSONObject) flowData.get("speed");
                if (speed == null) continue;
                
                // Extract actual speed (SU in v6) and free flow speed (FF in v6)
                float actualSpeed = 0;
                float freeFlowSpeed = 0;
                
                if (speed.get("actual") != null) {
                    actualSpeed = Float.parseFloat(speed.get("actual").toString());
                }
                
                if (speed.get("freeFlow") != null) {
                    freeFlowSpeed = Float.parseFloat(speed.get("freeFlow").toString());
                }
                
                // Get confidence value (CN in v6)
                float confidence = 0;
                if (flowData.get("confidence") != null) {
                    confidence = Float.parseFloat(flowData.get("confidence").toString()) / 100f; // Convert percentage to 0-1 scale
                }
                
                // Apply the same filtering as in the XML version
                if (confidence >= 0.7 && fc <= TrafficAndRoutingService.functional_road_class_here_maps) {
                    ArrayList<Float> latitudes = new ArrayList<>();
                    ArrayList<Float> longitudes = new ArrayList<>();
                    
                    // Extract coordinates
                    for (Object coordObj : coordinates) {
                        JSONObject coord = (JSONObject) coordObj;
                        if (coord.get("lat") != null && coord.get("lng") != null) {
                            float lat = Float.parseFloat(coord.get("lat").toString());
                            float lng = Float.parseFloat(coord.get("lng").toString());
                            latitudes.add(lat);
                            longitudes.add(lng);
                        }
                    }
                    
                    if (!latitudes.isEmpty() && !longitudes.isEmpty()) {
                        tempdt.getLat().add(latitudes);
                        tempdt.getLons().add(longitudes);
                        ArrayList<Float> combospeed = new ArrayList<>(2);
                        combospeed.add(actualSpeed);
                        combospeed.add(freeFlowSpeed);
                        tempdt.getSpeed().add(combospeed);
                    }
                }
            }
            
            // Feed the parsed data
            feed(tempdt);
            
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        } finally {
            System.out.println("Traffic parsing done...");
        }
    }

    public TrafficData getRoads() {
        return dt;
    }

}