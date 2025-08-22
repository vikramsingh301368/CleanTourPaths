package com.map.app.containers;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.graphhopper.util.shapes.BBox;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import com.graphhopper.GraphHopper;
import com.graphhopper.storage.Graph;
import com.map.app.graphhopperfuncs.AirQualityBFS;
import com.map.app.model.AirQuality;

/**
 * @author Siftee, Amit
 */

public class AirQualityDataExtractor {
	private final JSONParser jsonP;
	private final Lock writeLock;

	public GraphHopper getHopper() {
		return hopper;
	}

	private final GraphHopper hopper;
	private String aqiApiKey = System.getenv("waqi_api_key");
	private static final String url = "https://api.waqi.info/map/bounds/?latlng=";

	public AirQualityDataExtractor(GraphHopper ghopper, Lock lock) {
		hopper = ghopper;
		this.jsonP = new JSONParser();
		this.writeLock = lock;
		if (aqiApiKey ==null) {

			Properties prop=new Properties();

			try(FileInputStream fileInputStream = new FileInputStream("config.properties")) {
				prop.load(fileInputStream);

				aqiApiKey = prop.getProperty("waqi_api_key");
			} catch (IOException e) {
				throw new RuntimeException("Config.properties not found. Aborting ...");
			}
		}
	}

	/***
	 * Fetching the content from the api and parsing the json result
	 * @param boundingBox
	 */
// 	public void readWAQIData(BBox boundingBox) {
// 		if (aqiApiKey.equals("<WAQI_API_KEY>")){
// 			throw new RuntimeException("API Key for AQI URL is not found. Aborting...");
// 		}
// 		try {
// 			writeLock.lock();

// 			URL uri = new URL(url + boundingBox.minLat + "," + boundingBox.minLon + "," + boundingBox.maxLat + "," + boundingBox.maxLon + "&token=" + aqiApiKey);

// 			HttpURLConnection httpURLConnection = (HttpURLConnection) uri.openConnection();

// 			int responseCode = httpURLConnection.getResponseCode();

// 			if (responseCode != 200) {
// 				throw new RuntimeException("HttpResponseCode: " + responseCode);
// 			}

// 			ArrayList<AirQuality> airQualityArrayList = new ArrayList<>();
// 			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));
// 			String inputLine;
// 			StringBuilder response = new StringBuilder();

// 			while ((inputLine = bufferedReader .readLine()) != null) {
// 				response.append(inputLine);
// 			}

// 			bufferedReader.close();

// 			JSONObject obj = (JSONObject) jsonP.parse(response.toString());
// 			JSONArray data = (JSONArray) obj.get("data");

// 			for (Object datum : data) {

// 				JSONObject obj1 = (JSONObject) datum;

// 				double lat = (double) obj1.get("lat");
// 				double lon = (double) obj1.get("lon");
// 				String aqi = (String) obj1.get("aqi");

// 				// Regex to check string
// 				// contains only digits
// 				String regex = "[0-9]+";

// 				// Compile the ReGex
// 				Pattern pattern = Pattern.compile(regex);

// 				// If the string is empty
// 				// return false
// 				if (aqi == null) {
// 					continue;
// 				}

// 				// Find match between given string
// 				// and regular expression
// 				// using Pattern.matcher()
// 				Matcher matcher = pattern.matcher(aqi);

// 				if (matcher.matches()) {
// 					double aqiDouble = Double.parseDouble(aqi);
// 					JSONObject obj2 = (JSONObject) obj1.get("station");
// 					String name = (String) obj2.get("name");
// 					airQualityArrayList.add(new AirQuality(lat, lon, aqiDouble, name));
// 				}
// 			}
// //			airQualityArrayList.clear();
// //			read_historical_aqi(airQualityArrayList);
// 			//assign air quality metric to edge in graphhopper
// 			Graph gh = hopper.getGraphHopperStorage().getBaseGraph();

// 			AirQualityBFS airQualityBFS = new AirQualityBFS(hopper, gh, airQualityArrayList);

// 			airQualityBFS.start(gh.createEdgeExplorer(), 0);
// 			} 
// 		catch (Exception e) {
// 			e.printStackTrace();
// 		}
// 		finally {
// 			writeLock.unlock();
// 			System.out.println("WAQI API parsing done...");
			
// 		}
// 	}


public void readWAQIData(BBox boundingBox) {
    if (aqiApiKey.equals("<WAQI_API_KEY>")) {
        throw new RuntimeException("API Key for AQI URL is not found. Aborting...");
    }
    try {
        writeLock.lock();
        URL uri = new URL(url + boundingBox.minLat + "," + boundingBox.minLon + "," + boundingBox.maxLat + "," + boundingBox.maxLon + "&token=" + aqiApiKey);
        HttpURLConnection httpURLConnection = (HttpURLConnection) uri.openConnection();
        int responseCode = httpURLConnection.getResponseCode();

        if (responseCode != 200) {
            throw new RuntimeException("HttpResponseCode: " + responseCode);
        }

        ArrayList<AirQuality> airQualityArrayList = new ArrayList<>();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();

        while ((inputLine = bufferedReader.readLine()) != null) {
            response.append(inputLine);
        }

        bufferedReader.close();

        JSONObject obj = (JSONObject) jsonP.parse(response.toString());
        JSONArray data = (JSONArray) obj.get("data");

        for (Object datum : data) {
            JSONObject obj1 = (JSONObject) datum;
            double lat = (double) obj1.get("lat");
            double lon = (double) obj1.get("lon");
            String aqi = (String) obj1.get("aqi");

            String regex = "[0-9]+";
            Pattern pattern = Pattern.compile(regex);

            if (aqi == null) {
                continue;
            }

            Matcher matcher = pattern.matcher(aqi);

            if (matcher.matches()) {
                double aqiDouble = Double.parseDouble(aqi);
                JSONObject obj2 = (JSONObject) obj1.get("station");
                String name = (String) obj2.get("name");
                airQualityArrayList.add(new AirQuality(lat, lon, aqiDouble, name));
            }
        }

        // Print the API data
        System.out.println("\n===== WAQI API DATA =====");
        System.out.println("Found " + airQualityArrayList.size() + " air quality monitoring stations");
        for (AirQuality aq : airQualityArrayList) {
            System.out.println(aq);
        }
        System.out.println("=========================");

        // Read and print historical data
        ArrayList<AirQuality> historicalData = new ArrayList<>();
        read_historical_aqi(historicalData);
        
        System.out.println("\n===== HISTORICAL AQI DATA =====");
        System.out.println("Found " + historicalData.size() + " historical air quality records");
        // Print first 10 records only to avoid flooding the terminal
        int count = 0;
        for (AirQuality aq : historicalData) {
            if (count < 10) {
                System.out.println(aq);
                count++;
            } else if (count == 10) {
                System.out.println("... (more records omitted)");
                count++;
            }
            airQualityArrayList.add(aq);
        }
        System.out.println("=============================");

        Graph gh = hopper.getGraphHopperStorage().getBaseGraph();
        AirQualityBFS airQualityBFS = new AirQualityBFS(hopper, gh, airQualityArrayList);
        airQualityBFS.start(gh.createEdgeExplorer(), 0);
    } catch (Exception e) {
        e.printStackTrace();
    } finally {
        writeLock.unlock();
        System.out.println("WAQI API parsing done...");
    }
}



	// reading historical csv aqi data

    //ye purana code h original r niche wala mene test karne ke liye likha h

	// private void read_historical_aqi(ArrayList<AirQuality> ap) {
	// 	Properties prop=new Properties();
	// 	try(FileInputStream ip = new FileInputStream("config.properties")) {
	// 		prop.load(ip);
	// 		String aqPath=prop.getProperty("air_quality_file");
	// 		BufferedReader br = new BufferedReader(new FileReader(aqPath));
	// 		String newLine;
	// 		String[] strings;
	// 		br.readLine();
	// 		while ((newLine = br.readLine()) != null) {
	// 			strings = newLine.split(",");
	// 			if (strings.length !=0) {
	// 				if (!Objects.equals(strings[0], "") | !Objects.equals(strings[1], "") | !Objects.equals(strings[2], "") | !Objects.equals(strings[3], ""))
	// 					// reads the first column after locations data as aqi
	// 					ap.add(new AirQuality(Double.parseDouble(strings[1]), Double.parseDouble(strings[2]), Double.parseDouble(strings[3]), strings[0]));
	// 			}
	// 		}
	// 		br.close();
	// 	} catch (IOException e) {
	// 		throw new RuntimeException("Path not found");
	// 	}
	// }


    private void read_historical_aqi(ArrayList<AirQuality> ap) {
        Properties prop = new Properties();
        try (FileInputStream ip = new FileInputStream("config.properties")) {
            prop.load(ip);
            String aqPath = prop.getProperty("air_quality_file");
            
            // Ensure file exists and is accessible
            try (BufferedReader br = new BufferedReader(new FileReader(aqPath))) {
                String newLine;
                String[] strings;
                
                // Skip the header
                br.readLine();
                
                // Track unique locations to avoid duplicates
                Set<String> uniqueLocations = new HashSet<>();
                
                while ((newLine = br.readLine()) != null) {
                    strings = newLine.split(",");
                    
                    // Format: local_time,city,latitude,longitude,AT,RH,PM2_5,PM10
                    if (strings.length >= 8) {
                        // Ensure that the values are not empty before parsing
                        if (!strings[2].trim().isEmpty() && !strings[3].trim().isEmpty() && 
                            !strings[6].trim().isEmpty()) {
                            
                            try {
                                // Parse values
                                double latitude = Double.parseDouble(strings[2].trim());
                                double longitude = Double.parseDouble(strings[3].trim());
                                
                                // Use PM2.5 as AQI value (could also use PM10 from strings[7])
                                double pm25 = Double.parseDouble(strings[6].trim());
                                
                                // Create a unique key for this location
                                String locationKey = latitude + "," + longitude;
                                
                                // Only add if we haven't seen this location before
                                if (!uniqueLocations.contains(locationKey)) {
                                    uniqueLocations.add(locationKey);
                                    
                                    // Station name format: "City - Location"
                                    String stationName = strings[1].trim() + " - Historical";
                                    
                                    // Add parsed values to AirQuality object
                                    ap.add(new AirQuality(latitude, longitude, pm25, stationName));
                                }
                            } catch (NumberFormatException e) {
                                System.err.println("Invalid value in line: " + newLine);
                            }
                        }
                    }
                }
                
                System.out.println("Loaded " + uniqueLocations.size() + " unique historical locations");
            } catch (IOException e) {
                throw new RuntimeException("Failed to read air quality file: " + aqPath, e);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config.properties", e);
        }
    }
}