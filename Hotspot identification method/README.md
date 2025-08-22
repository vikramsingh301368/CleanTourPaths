# pollution_hotspots_detection

# Varanasi Air Quality Analysis (WHO Standards)

This analysis examines air quality data from Varanasi using WHO (World Health Organization) thresholds and identifies pollution hotspots during different times of the day.

## Data Source

- File: `varanasi_uttar_pradesh.csv`
- Time Period: March 28, 2025 to July 30, 2025
- Total Records: 11,542 (after cleaning)
- Parameters:
  - PM2.5 (Fine particulate matter)
  - PM10 (Coarse particulate matter)
  - AT (Air Temperature)
  - RH (Relative Humidity)
  - Location (latitude, longitude)
  - Timestamp (local_time)

## WHO Thresholds Used

- PM2.5: 15 µg/m³
- PM10: 45 µg/m³

## Analysis Methodology

### 1. Pollution Score Calculation
- Exceedance ratio calculation for each pollutant:
  ```python
  exceedance = (measured_value / who_threshold) - 1
  ```
  (negative values set to 0)

- Combined pollution score:
  ```python
  pollution_score = 0.6 * PM2.5_exceedance + 0.4 * PM10_exceedance
  ```

### 2. Time Categories
- Peak Hours: 8:00-11:00 AM and 5:00-8:00 PM
- Off-Peak Hours: All other times

### 3. Hotspot Identification Criteria
- Uses 50th percentile (median) threshold
- Minimum 7 days of data required
- At least 50% of days must show high pollution
- Minimum 10 readings per location
- Different thresholds for different time periods:
  - Overall: 0.59 (50th percentile)
  - Peak Hours: 0.61 (50th percentile)
  - Off-Peak Hours: 0.58 (50th percentile)

## Key Findings

### 1. Overall Pollution Levels
- 88.28% of PM2.5 readings exceed WHO threshold
- 75.51% of PM10 readings exceed WHO threshold

### 2. AQI Distribution
- Moderate: 57.53%
- Unhealthy for Sensitive Groups: 23.66%
- Good: 11.72%
- Unhealthy: 4.64%
- Hazardous: 2.03%
- Very Unhealthy: 0.42%

### 3. Hotspots Identified
- Overall: 5 hotspots
- Peak Hours: 4 hotspots
- Off-Peak Hours: 5 hotspots

## Output Files

### 1. Data Files
- `pollution_score_hotspots_who.csv`: Overall hotspot data
- `peak_hours_hotspots_who.csv`: Peak hours hotspot data
- `off_peak_hours_hotspots_who.csv`: Off-peak hours hotspot data

### 2. Visualizations
- `who_pollution_hotspots_map.html`: Overall hotspots map
- `who_peak_hours_hotspots_map.html`: Peak hours hotspots map
- `who_off_peak_hours_hotspots_map.html`: Off-peak hours hotspots map
- `who_pollution_density_map.html`: Pollution density heatmap
- `who_hotspots_visualization.html`: Combined interactive visualization with toggle between views

## How to Use

1. Run the Analysis:
   ```bash
   python analyze_varanasi_who.py
   ```

2. View Results:
   - Open `who_hotspots_visualization.html` in a web browser
   - Use the toggle buttons to switch between:
     - Overall view
     - Peak hours view
     - Off-peak hours view

3. Interpret Maps:
   - Color scale: Blue (low) → Purple (medium) → Red (high)
   - Point size indicates consistency of high pollution readings
   - Hover over points to see detailed statistics

## Code Structure

`analyze_varanasi_who.py` contains:
1. Data loading and cleaning
2. WHO threshold-based exceedance calculations
3. Time period classification
4. Hotspot identification algorithm
5. Visualization generation

## Dependencies

- pandas
- numpy
- plotly
- folium
- matplotlib
- seaborn
- scikit-learn
- geopandas

## Notes

- All thresholds are based on WHO 2021 Air Quality Guidelines
- The analysis uses a weighted approach giving more importance to PM2.5 (60%) over PM10 (40%)
- Hotspot identification uses median (50th percentile) as threshold to ensure focus on consistently problematic areas
- Peak/off-peak analysis helps understand temporal patterns in pollution levels
