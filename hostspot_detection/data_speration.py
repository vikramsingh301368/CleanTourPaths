import pandas as pd

# Input CSV file path
input_file = "Air View+ Clear Skies Hourly Dataset.csv"

# Output CSV file path
output_file = "varanasi_uttar_pradesh.csv"

# Load the dataset
df = pd.read_csv(input_file)

# Filter for Varanasi, Uttar Pradesh
varanasi_df = df[
    (df['state'].str.strip().str.lower() == 'uttar pradesh') &
    (df['city'].str.strip().str.lower() == 'varanasi')
]

# Save the filtered data to CSV
varanasi_df.to_csv(output_file, index=False)

print(f"Filtered data saved to '{output_file}'")
