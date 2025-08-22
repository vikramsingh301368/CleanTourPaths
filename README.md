# CleanTourPaths 🚗🌱🗺️

A comprehensive collection of projects focused on **clean transportation routing**, **pollution hotspot detection**, and **graph neural network analysis** for Varanasi, India.

## Table of Contents

- [Overview](#overview)
- [Projects](#projects)
- [Prerequisites](#prerequisites)
- [Installation & Setup](#installation--setup)
- [Usage](#usage)
- [API Documentation](#api-documentation)
- [Contributing](#contributing)
- [License](#license)

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

### 1. Clone the Repository
```bash
git clone https://github.com/vikramsingh301368/CleanTourPaths.git
cd CleanTourPaths
```

### 2. Setup Congestion Emission Routing System

#### Install Java Dependencies
```bash
cd congestion-emission-routing-system
mvn clean install
```

#### Configure Application
1. Copy `config.properties.example` to `config.properties` (if available)
2. Update configuration values:
   ```properties
   # Server Configuration
   server.port=8080
   
   # Database Configuration
   spring.datasource.url=jdbc:h2:mem:testdb
   spring.datasource.driverClassName=org.h2.Driver
   
   # Map Configuration
   map.center.lat=25.3176
   map.center.lng=82.9739
   map.zoom=12
   ```

#### Run the Application
```bash
# Using Maven
mvn spring-boot:run

# Using Java JAR
java -jar target/congestion-emission-routing-system-1.0.0.jar

# Using Maven Wrapper
./mvnw spring-boot:run
```

#### Access the Application
- **Web Interface**: http://localhost:8080
- **API Documentation**: http://localhost:8080/swagger-ui.html
- **Health Check**: http://localhost:8080/actuator/health

### 3. Setup Hotspot Detection

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

### 4. Setup GNN Varanasi

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



## 🚀 Usage

### Congestion Emission Routing System

#### API Endpoints

##### 1. Get Optimal Route
```http
POST /api/route/optimal
Content-Type: application/json

{
  "startLat": 25.3176,
  "startLng": 82.9739,
  "endLat": 25.3189,
  "endLng": 82.9739,
  "transportMode": "WALKING",
  "considerAirQuality": true,
  "considerTraffic": true
}
```

##### 2. Get Air Quality Data
```http
GET /api/air-quality?lat=25.3176&lng=82.9739
```

##### 3. Get Traffic Data
```http
GET /api/traffic?lat=25.3176&lng=82.9739
```

#### Web Interface Features
1. **Interactive Map**: Click to set start and end points
2. **Route Options**: Choose between fastest, cleanest, or balanced routes
3. **Transport Modes**: Walking, cycling, driving, or public transport
4. **Real-time Data**: Live air quality and traffic information
5. **Route Comparison**: Side-by-side analysis of different routes

### Hotspot Detection

#### Data Analysis Workflow
1. **Data Loading**: Import CSV files with air quality measurements
2. **Preprocessing**: Clean and validate data
3. **Analysis**: Apply WHO guidelines for hotspot identification
4. **Visualization**: Generate interactive maps and charts
5. **Reporting**: Export results as HTML files

#### Output Files
- `who_hotspots_visualization.html`: Overall hotspot analysis
- `who_peak_hours_hotspots_map.html`: Peak hour hotspots
- `who_off_peak_hours_hotspots_map.html`: Off-peak hour hotspots
- `who_pollution_density_map.html`: Pollution density mapping
- `who_pollution_hotspots_map.html`: Combined hotspot analysis

### GNN Varanasi

#### Notebook Sections
1. **Data Loading**: Import transportation network data
2. **Graph Construction**: Build graph representation
3. **Model Definition**: Define GNN architecture
4. **Training**: Train the neural network
5. **Evaluation**: Assess model performance
6. **Visualization**: Graph and prediction visualization

## 📚 API Documentation

### Base URL
```
http://localhost:8080/api
```

### Authentication
Currently, the API is open (no authentication required). For production use, implement proper security measures.

### Rate Limiting
- **Default**: 100 requests per minute per IP
- **Configurable**: Update in `application.properties`

### Error Handling
```json
{
  "timestamp": "2025-08-22T10:00:00.000Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid coordinates provided",
  "path": "/api/route/optimal"
}
```

## 🔧 Configuration

### Environment Variables
```bash
# Server Configuration
SERVER_PORT=8080
SERVER_CONTEXT_PATH=/api

# Database Configuration
DB_URL=jdbc:h2:mem:testdb
DB_USERNAME=sa
DB_PASSWORD=

# Map Configuration
MAP_CENTER_LAT=25.3176
MAP_CENTER_LNG=82.9739
MAP_ZOOM=12

# API Configuration
API_RATE_LIMIT=100
API_TIMEOUT=30000
```

### Logging Configuration
```properties
# Logging levels
logging.level.com.map.app=INFO
logging.level.org.springframework.web=DEBUG
logging.level.org.hibernate.SQL=DEBUG

# Log file configuration
logging.file.name=logs/application.log
logging.file.max-size=10MB
logging.file.max-history=30
```

## 🧪 Testing

### Run Tests
```bash
# Java Tests
cd congestion-emission-routing-system
mvn test

# Python Tests (if available)
cd hostspot_detection
python -m pytest tests/

cd GNN_varanasi
python -m pytest tests/
```

### Test Coverage
```bash
# Java Coverage
mvn jacoco:report

# Python Coverage
coverage run -m pytest
coverage report
coverage html
```

## 📊 Data Sources

### Air Quality Data
- **Source**: Environmental monitoring stations
- **Format**: CSV with timestamp, location, and pollutant levels
- **Pollutants**: PM2.5, PM10, NO2, SO2, CO, O3

### Traffic Data
- **Source**: Traffic monitoring systems
- **Format**: Real-time traffic flow data
- **Metrics**: Congestion level, average speed, vehicle count

### Geographic Data
- **Source**: OpenStreetMap
- **Format**: OSM PBF files
- **Coverage**: Varanasi city and surrounding areas

## 🚨 Troubleshooting

### Common Issues

#### Java Application Won't Start
```bash
# Check Java version
java -version

# Check port availability
netstat -an | grep 8080

# Check Maven installation
mvn -version
```

#### Python Dependencies Issues
```bash
# Upgrade pip
pip install --upgrade pip

# Clear pip cache
pip cache purge

# Reinstall dependencies
pip install -r requirements.txt --force-reinstall
```

#### Map Loading Issues
1. Check internet connection
2. Verify OpenStreetMap service availability
3. Check browser console for JavaScript errors
4. Verify map file paths in configuration

### Performance Optimization
1. **Database**: Use connection pooling
2. **Caching**: Implement Redis for frequently accessed data
3. **Async Processing**: Use async endpoints for heavy computations
4. **Load Balancing**: Deploy multiple instances behind a load balancer

## 🤝 Contributing

We welcome contributions! Please follow these steps:

1. **Fork** the repository
2. **Create** a feature branch (`git checkout -b feature/AmazingFeature`)
3. **Commit** your changes (`git commit -m 'Add some AmazingFeature'`)
4. **Push** to the branch (`git push origin feature/AmazingFeature`)
5. **Open** a Pull Request

### Contribution Guidelines
- Follow the existing code style
- Add tests for new functionality
- Update documentation as needed
- Ensure all tests pass before submitting

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- **OpenStreetMap** for geographic data
- **Spring Boot** team for the excellent framework
- **Python community** for data science libraries
- **Varanasi Municipal Corporation** for data support

## 📞 Support

### Getting Help
- **Issues**: [GitHub Issues](https://github.com/vikramsingh301368/CleanTourPaths/issues)
- **Discussions**: [GitHub Discussions](https://github.com/vikramsingh301368/CleanTourPaths/discussions)
- **Email**: [Your Email]

### Community
- **Slack**: [Slack Channel]
- **Discord**: [Discord Server]
- **Twitter**: [@CleanTourPaths]

---

**Made with ❤️ for cleaner, smarter transportation in Varanasi**

*Last updated: August 2025*
