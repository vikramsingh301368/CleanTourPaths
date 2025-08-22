# CleanTourPaths 🚗🌱🗺️

A comprehensive collection of projects focused on **clean transportation routing**, **pollution hotspot detection**, and **graph neural network analysis** for Varanasi, India.

## Overview

CleanTourPaths is a multi-faceted project that addresses urban transportation challenges through:
- **Intelligent routing** that considers air quality and traffic conditions
- **Pollution hotspot detection** using WHO guidelines
- **Graph neural network analysis** for transportation networks
- **Real-time data processing** and visualization

## Projects

### 1. Congestion Emission Routing System

A Java Spring Boot application that provides intelligent routing solutions considering:
- **Air Quality Index (AQI)** data
- **Traffic congestion** patterns
- **Multi-modal transportation** options

#### Features
- Real-time multimodal routing information
- Routing algorithms based on air pollution exposure, congestion, distance, and time
- Interactive map visualization

### 2. Hotspot Detection

Python-based analysis tool for identifying recurring and non-recurring air pollution hotspots in Varanasi using WHO guidelines.

#### Features
- **Peak vs. Off-peak** hour analysis
- **Pollution density mapping**
- **Hotspot visualization** with interactive maps
- **Development of Hotspot Index** 

### 3. Spatiotemporal Graph Neural Network (STGNN) for Varanasi

Spatiotemporal Graph Neural Network (STGNN) is used for the prediction of PM2.5 for Varanasi.

#### Features
- **Graph formation** for fixed monitors
- **Long Short Term Memory (LSTM)** for temporal prediction
- **Graph Neural Network (GNN)** for spatial prediction


## 🛠️ Installation & Setup

### 1. Multimodal routing system
In order to use the above project,
* Clone the repository using the following command on your console/command prompt in the location of your choice: <br>git clone https://github.com/vikramsingh301368/CleanTourPaths.git 
* Get your two API keys from:
  * HERE Maps REST API (freemium): https://developer.here.com
  * WAQI API: https://aqicn.org/api/
* Paste your API keys to config.properties in the relevant location
* Go to **Run** from the menu bar, then goto **Edit Configurations...**, then add a new Maven configuration (it will automatically select your project), then in Run command location paste: **spring-boot:run** and apply
* Run your **gh_configured_router** 
* Go to the location of your project and execute the following command to run project and also set the api keys:<br> mvn spring-boot:run -Dspring-boot.run.arguments=--here_api_key=<YOUR_HERE_API_KEY>,--waqi_api_key=<YOUR_WAQI_API_KEY>,--datareader.file=<LOCATION_OSM.PBF_FILE>
* Open http://localhost:9098/ where the website will be displayed
* After doing the routing, to get json response of the routing, add "&mediaType=json" as another parameter. For example: http://localhost:9098/routing?StartLoc=83.0106679%2C25.31082185&EndLoc=82.9582801%2C25.2685343&RouteType=greenest&Vehicle=car&mediaType=json
* In order to run the project without any additional commandline arguments, simply type  mvn spring-boot:run  <br>
Used the following geocoding library: https://github.com/location-iq/leaflet-geocoder<br>
Please note that specifying points outside the bounds of the datareader file currently raises PointOutOfBoundsException error:<br>
[com.graphhopper.util.exceptions.PointOutOfBoundsException: Point 0 is out of bounds: your_latitude,your_longitude the bounds are: bbox of datareader file] <br>
We are currently working on giving a relevant message to the client instead.

### 2. Setup Hotspot Detection

#### Install Python Dependencies
```bash
cd hostspot_detection

# Create virtual environment
python -m venv venv

# Activate virtual environment
# Windows
venv\Scripts\activate
# macOS/Linux
source venv/bin/activate

# Install dependencies
pip install -r requirements.txt
```

#### Required Python Packages
```bash
pip install pandas numpy matplotlib plotly folium scikit-learn
```

#### Run Analysis
```bash
# Run main analysis
python analyze_varanasi_who.py

# Run data separation
python data_speration.py

# View results
# Open analyze_results_who/*.html files in your browser
```

### 3. Setup GNN Varanasi

#### Install Python Dependencies
```bash
cd GNN_varanasi

# Create virtual environment
python -m venv venv

# Activate virtual environment
# Windows
venv\Scripts\activate
# macOS/Linux
source venv/bin/activate

# Install dependencies
pip install -r requirements.txt
```


## 📊 Data Sources

### Air Quality Data
- **Source**: GoogleAirView+ monitoring stations and WAQI API
- **Format**: CSV with timestamp, location, and pollutant levels
- **Pollutants**: PM1, PM2.5, PM10, 

### Traffic Data
- **Source**: HEREmaps API
- **Format**: Real-time traffic flow data
- **Metrics**: Congestion level in terms of Index, average speed 

### Geographic Data
- **Source**: OpenStreetMap, LeafLet
- **Coverage**: Varanasi city and surrounding areas


### Query support
- **Email**: vikramsingh301368@gmail.com


**Made with ❤️ for cleaner, smarter transportation in Varanasi**

*Last updated: August 2025*
