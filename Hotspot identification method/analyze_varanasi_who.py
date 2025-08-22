import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import seaborn as sns
import folium
from folium.plugins import HeatMap, MarkerCluster
import plotly.express as px
import plotly.graph_objects as go
from plotly.subplots import make_subplots
import os
from sklearn.cluster import DBSCAN
from datetime import datetime
import calendar
import geopandas as gpd
import pyproj

# Create output directory
output_dir = 'new_analysis_results_who'
os.makedirs(output_dir, exist_ok=True)

# Load Varanasi boundary
print("\nLoading Varanasi boundary...")
try:
    varanasi_boundary = gpd.read_file('hotspot 2/Varanasi - Saroj Kanta Behera.gpkg')
    print(f"Varanasi boundary loaded successfully with CRS: {varanasi_boundary.crs}")
    
    # Transform from EPSG:7755 to EPSG:4326 (WGS84) for compatibility with plotly maps
    varanasi_boundary = varanasi_boundary.to_crs(epsg=4326)
    print("Boundary transformed to WGS84 (EPSG:4326)")
    
    has_boundary = True
except Exception as e:
    print(f"Error loading Varanasi boundary: {e}")
    has_boundary = False

print("Reading data...")
# Read the full dataset with proper date parsing
print("\nReading full dataset...")
df = pd.read_csv('hotspot 2/varanasi_uttar_pradesh.csv')

# Convert local_time to datetime
df['date'] = pd.to_datetime(df['local_time'], format='%d-%m-%Y %H:%M')

print("Full data shape:", df.shape)
print(f"Date range: {df['date'].min()} to {df['date'].max()}")

# Basic data cleaning and preparation
print("\nCleaning and preparing data...")

# Extract date components for analysis
df['day'] = df['date'].dt.date
df['month'] = df['date'].dt.month
df['month_name'] = df['date'].dt.month_name()
df['hour'] = df['date'].dt.hour
df['day_of_week'] = df['date'].dt.day_name()

# After extracting date components, add peak/off-peak classification
df['time_category'] = 'off_peak'
# Set peak hours (8:00-11:00 AM and 5:00-8:00 PM)
morning_peak_mask = (df['hour'] >= 8) & (df['hour'] < 11)
evening_peak_mask = (df['hour'] >= 17) & (df['hour'] < 20)
df.loc[morning_peak_mask | evening_peak_mask, 'time_category'] = 'peak'

# Define pollution metrics and location columns
pollution_metrics = ['PM2_5', 'PM10']
location_cols = ['latitude', 'longitude']

# Check if all required columns exist
required_cols = location_cols + pollution_metrics
missing_cols = [col for col in required_cols if col not in df.columns]
if missing_cols:
    print(f"Warning: Missing columns: {missing_cols}")
    print("Available columns:", df.columns.tolist())
    exit(1)

# Remove any rows with missing values in key columns
df = df.dropna(subset=location_cols + pollution_metrics)
print(f"Data shape after cleaning: {df.shape}")

# Create spatial bins for analysis (grid cells)
precision = 3  # Decimal places to round coordinates
df['lat_bin'] = np.round(df['latitude'], precision)
df['lon_bin'] = np.round(df['longitude'], precision)

# Define WHO thresholds
print("\nCalculating exceedance based on WHO thresholds...")
who_thresholds = {
    'PM2_5': 15,  # µg/m³
    'PM10': 45   # µg/m³
}

# Calculate exceedance ratio for each pollutant
for metric in pollution_metrics:
    threshold = who_thresholds[metric]
    df[f'{metric}_exceedance'] = df[metric] / threshold
    # Calculate how much it exceeds the threshold (0 if below threshold)
    df[f'{metric}_exceedance'] = df[f'{metric}_exceedance'].apply(lambda x: max(0, x - 1))
    
    # Print exceedance statistics
    print(f"\n{metric} Exceedance Statistics:")
    print(df[f'{metric}_exceedance'].describe())
    
    # Calculate percentage of readings exceeding threshold
    exceed_pct = (df[metric] > threshold).mean() * 100
    print(f"Percentage of {metric} readings exceeding WHO threshold: {exceed_pct:.2f}%")

# Calculate pollution score based on WHO exceedances
print("\nCalculating pollution score...")
df['pollution_score'] = (0.6 * df['PM2_5_exceedance'] + 0.4 * df['PM10_exceedance'])

print("\nPollution Score Statistics:")
print(df['pollution_score'].describe())

# Function to calculate Air Quality Index based on WHO guidelines
def calculate_aqi(row):
    pm25 = row['PM2_5']
    
    if pm25 <= 15:  # WHO threshold
        return 'Good'
    elif pm25 <= 30:  # 2x WHO threshold
        return 'Moderate'
    elif pm25 <= 45:  # 3x WHO threshold
        return 'Unhealthy for Sensitive Groups'
    elif pm25 <= 60:  # 4x WHO threshold
        return 'Unhealthy'
    elif pm25 <= 75:  # 5x WHO threshold
        return 'Very Unhealthy'
    else:
        return 'Hazardous'

# Calculate AQI for each measurement
df['aqi_category'] = df.apply(calculate_aqi, axis=1)

# Print AQI distribution
print("\nAQI Category Distribution:")
print(df['aqi_category'].value_counts(normalize=True) * 100)

# Function to add Varanasi boundary to any mapbox figure
def add_varanasi_boundary(fig):
    """Add Varanasi boundary to a plotly mapbox figure"""
    if not has_boundary:
        return fig
        
    try:
        # Extract boundary coordinates
        for idx, row in varanasi_boundary.iterrows():
            geom = row.geometry
            
            if geom.geom_type == 'MultiPolygon':
                # Process each polygon in the multipolygon
                for poly in geom.geoms:
                    x, y = poly.exterior.xy
                    fig.add_trace(
                        go.Scattermapbox(
                            lat=list(y),
                            lon=list(x),
                            mode='lines',
                            line=dict(width=3, color='black'),
                            name='Varanasi Boundary',
                            hoverinfo='skip',
                            showlegend=False  # Hide from legend
                        )
                    )
            else:  # Single polygon
                x, y = geom.exterior.xy
                fig.add_trace(
                    go.Scattermapbox(
                        lat=list(y),
                        lon=list(x),
                        mode='lines',
                        line=dict(width=3, color='black'),
                        name='Varanasi Boundary',
                        hoverinfo='skip',
                        showlegend=False  # Hide from legend
                    )
                )
        
        print("Varanasi boundary added to map")
    except Exception as e:
        print(f"Error adding Varanasi boundary to map: {e}")
    
    return fig

# Function to identify hotspots with 88th percentile threshold
def identify_hotspots(dataframe, metric='pollution_score', 
                     threshold_percentile=88, min_days_total=7, min_consistency_pct=0.5,
                     time_filter=None):
    """
    Identify hotspots based on consistent high pollution across multiple days.
    Now using 88th percentile (median) as threshold for high pollution.
    """
    if time_filter:
        dataframe = dataframe[dataframe['time_category'] == time_filter].copy()
        if len(dataframe) == 0:
            print(f"No data available for time category: {time_filter}")
            return pd.DataFrame()
    
    threshold = np.percentile(dataframe[metric], threshold_percentile)
    print(f"Threshold for {metric} (percentile {threshold_percentile}): {threshold:.2f}")
    
    location_groups = dataframe.groupby(['lat_bin', 'lon_bin'])
    
    hotspots = []
    for (lat, lon), group in location_groups:
        if len(group) >= 10:
            high_pollution_days = group[group[metric] > threshold]['day'].nunique()
            total_days = group['day'].nunique()
            
            consistency = high_pollution_days / total_days if total_days > 0 else 0
            
            if total_days >= min_days_total and consistency >= min_consistency_pct:
                avg_value = group[metric].mean()
                max_value = group[metric].max()
                readings_count = len(group)
                
                hour_counts = group['hour'].value_counts().to_dict()
                peak_hour = max(hour_counts.items(), key=lambda x: x[1])[0]
                
                month_data = {}
                for month in group['month_name'].unique():
                    month_group = group[group['month_name'] == month]
                    month_data[month] = {
                        'avg': month_group[metric].mean(),
                        'max': month_group[metric].max(),
                        'readings': len(month_group)
                    }
                
                hotspots.append({
                    'lat': lat,
                    'lon': lon,
                    'avg_value': avg_value,
                    'max_value': max_value,
                    'days_total': total_days,
                    'days_high_pollution': high_pollution_days,
                    'consistency': consistency,
                    'readings_count': readings_count,
                    'peak_hour': peak_hour,
                    'month_data': month_data
                })
    
    if hotspots:
        hotspots_df = pd.DataFrame(hotspots)
        hotspots_df = hotspots_df.sort_values(
            by=['consistency', 'avg_value'], ascending=False
        )
        return hotspots_df
    else:
        return pd.DataFrame()

# Identify hotspots
print("\nIdentifying hotspots using 88th percentile threshold...")
hotspots_df = identify_hotspots(df, metric='pollution_score', 
                               threshold_percentile=88, 
                               min_days_total=7, 
                               min_consistency_pct=0.5)

if not hotspots_df.empty:
    print(f"Found {len(hotspots_df)} hotspots for pollution score")
    
    # Save hotspot data
    export_df = hotspots_df.copy()
    export_df.drop('month_data', axis=1, inplace=True)
    export_df.to_csv(os.path.join(output_dir, 'pollution_score_hotspots_who.csv'), index=False)
    
    # Create interactive map
    simple_color_scale = [
        [0, 'rgb(0, 0, 255)'],      # Blue for low values
        [0.5, 'rgb(128, 0, 128)'],  # Purple for mid values
        [1.0, 'rgb(255, 0, 0)']     # Red for high values
    ]
    
    # Create a scatter mapbox for hotspots
    fig = px.scatter_mapbox(
        hotspots_df,
        lat='lat',
        lon='lon',
        color='avg_value',
        size='consistency',
        size_max=15,
        zoom=10,
        color_continuous_scale=simple_color_scale,
        title='Pollution Score Hotspots (WHO Standards)',
        hover_name='lat',
        hover_data={
            'lat': True,
            'lon': True,
            'avg_value': ':.2f',
            'max_value': ':.2f',
            'consistency': ':.2f',
            'days_high_pollution': True,
            'days_total': True,
            'readings_count': True,
            'peak_hour': True
        },
        labels={
            'avg_value': 'WHO-based Air Quality Index',
            'lat': 'Latitude',
            'lon': 'Longitude'
        }
    )
    
    fig.update_layout(
        mapbox_style='open-street-map',
        margin={"r":0,"t":50,"l":0,"b":0},
        height=800
    )
    
    # Add Varanasi boundary
    fig = add_varanasi_boundary(fig)
    
    fig.write_html(os.path.join(output_dir, 'who_pollution_hotspots_map.html'))
    
    # Create density heatmap
    fig = px.density_mapbox(
        df, 
        lat='latitude', 
        lon='longitude', 
        z='pollution_score',
        radius=10,
        zoom=10,
        title='Pollution Score Density Map (WHO Standards)',
        color_continuous_scale=simple_color_scale,
        labels={
            'pollution_score': 'WHO-based Air Quality Index',
            'latitude': 'Latitude',
            'longitude': 'Longitude'
        }
    )
    
    fig.update_layout(
        mapbox_style='open-street-map',
        margin={"r":0,"t":50,"l":0,"b":0},
        height=800
    )
    
    # Add Varanasi boundary
    fig = add_varanasi_boundary(fig)
    
    fig.write_html(os.path.join(output_dir, 'who_pollution_density_map.html'))

    # Analyze peak vs off-peak patterns
    print("\nAnalyzing peak hours hotspots...")
    peak_hotspots_df = identify_hotspots(df, metric='pollution_score', 
                                       threshold_percentile=88,
                                       time_filter='peak')
    
    if not peak_hotspots_df.empty:
        print(f"Found {len(peak_hotspots_df)} hotspots during peak hours")
        
        # Save peak hours hotspot data
        export_peak_df = peak_hotspots_df.copy()
        export_peak_df.drop('month_data', axis=1, inplace=True)
        export_peak_df.to_csv(os.path.join(output_dir, 'peak_hours_hotspots_who.csv'), index=False)
        
        # Create peak hours map
        fig = px.scatter_mapbox(
            peak_hotspots_df,
            lat='lat',
            lon='lon',
            color='avg_value',
            size='consistency',
            size_max=15,
            zoom=10,
            color_continuous_scale=simple_color_scale,
            title='Peak Hours Pollution Hotspots (WHO Standards)',
            hover_name='lat',
            hover_data={
                'lat': True,
                'lon': True,
                'avg_value': ':.2f',
                'max_value': ':.2f',
                'consistency': ':.2f',
                'days_high_pollution': True,
                'days_total': True,
                'readings_count': True,
                'peak_hour': True
            },
            labels={
                'avg_value': 'WHO-based Air Quality Index',
                'lat': 'Latitude',
                'lon': 'Longitude'
            }
        )
        
        fig.update_layout(
            mapbox_style='open-street-map',
            margin={"r":0,"t":50,"l":0,"b":0},
            height=800
        )
        
        # Add Varanasi boundary
        fig = add_varanasi_boundary(fig)
        
        fig.write_html(os.path.join(output_dir, 'who_peak_hours_hotspots_map.html'))
    else:
        print("No hotspots found during peak hours")
    
    print("\nAnalyzing off-peak hours hotspots...")
    off_peak_hotspots_df = identify_hotspots(df, metric='pollution_score', 
                                           threshold_percentile=88,
                                           time_filter='off_peak')
    
    if not off_peak_hotspots_df.empty:
        print(f"Found {len(off_peak_hotspots_df)} hotspots during off-peak hours")
        
        # Save off-peak hours hotspot data
        export_offpeak_df = off_peak_hotspots_df.copy()
        export_offpeak_df.drop('month_data', axis=1, inplace=True)
        export_offpeak_df.to_csv(os.path.join(output_dir, 'off_peak_hours_hotspots_who.csv'), index=False)
        
        # Create off-peak hours map
        fig = px.scatter_mapbox(
            off_peak_hotspots_df,
            lat='lat',
            lon='lon',
            color='avg_value',
            size='consistency',
            size_max=15,
            zoom=10,
            color_continuous_scale=simple_color_scale,
            title='Off-Peak Hours Pollution Hotspots (WHO Standards)',
            hover_name='lat',
            hover_data={
                'lat': True,
                'lon': True,
                'avg_value': ':.2f',
                'max_value': ':.2f',
                'consistency': ':.2f',
                'days_high_pollution': True,
                'days_total': True,
                'readings_count': True,
                'peak_hour': True
            },
            labels={
                'avg_value': 'WHO-based Air Quality Index',
                'lat': 'Latitude',
                'lon': 'Longitude'
            }
        )
        
        fig.update_layout(
            mapbox_style='open-street-map',
            margin={"r":0,"t":50,"l":0,"b":0},
            height=800
        )
        
        # Add Varanasi boundary
        fig = add_varanasi_boundary(fig)
        
        fig.write_html(os.path.join(output_dir, 'who_off_peak_hours_hotspots_map.html'))
    else:
        print("No hotspots found during off-peak hours")

    # Create combined visualization HTML
    combined_html = """
    <!DOCTYPE html>
    <html>
    <head>
        <title>Varanasi Pollution Hotspots (WHO Standards)</title>
        <style>
            body { margin: 0; padding: 0; font-family: Arial, sans-serif; }
            .container { width: 100%; height: 100vh; position: relative; }
            iframe { width: 100%; height: 100%; border: none; }
            .controls {
                position: absolute;
                top: 10px;
                left: 10px;
                background: white;
                padding: 15px;
                border-radius: 5px;
                box-shadow: 0 0 10px rgba(0,0,0,0.2);
                z-index: 1000;
            }
            .button-group {
                display: flex;
                gap: 10px;
                margin-top: 10px;
            }
            button {
                padding: 8px 15px;
                cursor: pointer;
                border: none;
                border-radius: 4px;
                background-color: #f0f0f0;
                transition: background-color 0.3s;
            }
            button.active {
                background-color: #4285f4;
                color: white;
            }
            .current-view {
                font-weight: bold;
                margin-bottom: 10px;
            }
        </style>
    </head>
    <body>
        <div class="container">
            <div class="controls">
                <div class="current-view">Current View: <span id="view-mode">Overall</span></div>
                <div class="button-group">
                    <button onclick="setView('overall')" class="active">Overall</button>
                    <button onclick="setView('peak')">Peak Hours</button>
                    <button onclick="setView('off-peak')">Off-Peak Hours</button>
                </div>
            </div>
            <iframe id="map-frame" src="who_pollution_hotspots_map.html"></iframe>
        </div>
        
        <script>
            function setView(view) {
                document.querySelectorAll('button').forEach(btn => btn.classList.remove('active'));
                document.querySelector(`button[onclick="setView('${view}')"]`).classList.add('active');
                
                document.getElementById('view-mode').textContent = view.charAt(0).toUpperCase() + view.slice(1);
                
                let src;
                switch(view) {
                    case 'overall':
                        src = 'who_pollution_hotspots_map.html';
                        break;
                    case 'peak':
                        src = 'who_peak_hours_hotspots_map.html';
                        break;
                    case 'off-peak':
                        src = 'who_off_peak_hours_hotspots_map.html';
                        break;
                }
                document.getElementById('map-frame').src = src;
            }
        </script>
    </body>
    </html>
    """
    
    with open(os.path.join(output_dir, 'who_hotspots_visualization.html'), 'w') as f:
        f.write(combined_html)

print("\nAnalysis complete. Results saved to new_analysis_results_who directory.")
print("Key files created:")
print("1. WHO-based pollution hotspots map: who_pollution_hotspots_map.html")
print("2. WHO-based pollution density map: who_pollution_density_map.html")
print("3. Combined visualization interface: who_hotspots_visualization.html")
print("4. Hotspots data: pollution_score_hotspots_who.csv")
