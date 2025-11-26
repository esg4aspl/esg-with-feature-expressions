import pandas as pd
import glob
import os
import sys

def generate_time_measurement_summary():
    # --- CONFIGURATION ---
    script_dir = os.path.dirname(os.path.abspath(__file__))
    project_root = os.path.dirname(script_dir)
    base_dir = os.path.join(project_root, "files", "Cases")
    output_file = os.path.join(base_dir, "SPLTimeMeasurementSummary.csv")
    target_folder = "shards_timemeasurement"
    
    coverage_types = [
        "randomwalk",
        "eventcoverage", 
        "eventcouplecoverage", 
        "eventtriplecoverage", 
        "eventquadruplecoverage"
    ]

    print(f"🔍 Summarizer started. Scanning: {base_dir}")

    if not os.path.exists(base_dir):
        print("⚠️ Warning: Base directory not found. Skipping summary.")
        return

    all_dataframes = []
    
    if os.path.exists(base_dir):
        cases = [d for d in os.listdir(base_dir) if os.path.isdir(os.path.join(base_dir, d))]
        cases.sort()
    else:
        cases = []

    for case_name in cases:
        case_path = os.path.join(base_dir, case_name)
        base_shard_dir = os.path.join(case_path, target_folder)
        
        if os.path.exists(base_shard_dir):
            for cov_type in coverage_types:
                specific_shard_dir = os.path.join(base_shard_dir, cov_type)
                
                if os.path.exists(specific_shard_dir):
                    pattern = os.path.join(specific_shard_dir, "*shard*.csv")
                    shard_files = glob.glob(pattern)
                    
                    if shard_files:
                        for f in shard_files:
                            try:
                                df = pd.read_csv(f, sep=";")
                                
                                # Basic cleaning
                                if 'SPL Name' in df.columns: 
                                    df = df.dropna(subset=['SPL Name'])
                                
                                if 'Coverage Type' not in df.columns:
                                    df['Coverage Type'] = cov_type
                                
                                # Add Source File ID to distinguish shards
                                df['ShardID'] = os.path.basename(f)
                                
                                all_dataframes.append(df)
                            except Exception as e: 
                                pass

    if not all_dataframes:
        print("⚠️  No Time Measurement data found. Summary NOT created.")
        return

    full_df = pd.concat(all_dataframes, ignore_index=True)

    # --- NUMERIC CONVERSIONS ---
    # List of time columns to process (Updated names)
    time_cols = [
        'Total Time(ms)', 
        'SAT Time(ms)', 
        'ProductESGGeneration Time(ms)', 
        'Test Generation Time(ms)'
    ]
    
    # Convert all time columns and Processed Products to float
    cols_to_convert = time_cols + ['Processed Products']
    
    for col in cols_to_convert:
        if col in full_df.columns:
             if full_df[col].dtype == 'object':
                 full_df[col] = full_df[col].astype(str).str.replace(',', '.').astype(float)

    # --- STEP 1: AGGREGATE PER SHARD (Average the 10 runs) ---
    shard_group_cols = ['SPL Name', 'Coverage Type', 'ShardID']
    
    # Aggregation dictionary: Mean for time columns, Max for product count
    shard_agg_dict = {col: 'mean' for col in time_cols if col in full_df.columns}
    shard_agg_dict['Processed Products'] = 'max' 
    
    shard_df = full_df.groupby(shard_group_cols).agg(shard_agg_dict).reset_index()

    # --- STEP 2: AGGREGATE ACROSS SHARDS (Weighted Average) ---
    # Prepare weighted sums for all available time columns
    available_time_cols = [col for col in time_cols if col in shard_df.columns]
    
    for col in available_time_cols:
        # Calculate Total Duration for this shard = AvgTime * ProductCount
        shard_df[f'Weighted_{col}'] = shard_df[col] * shard_df['Processed Products']

    # Group by SPL and Coverage
    final_group_cols = ['SPL Name', 'Coverage Type']
    
    # Final Aggregation: Sum of Weighted Times and Sum of Products
    final_agg_dict = {f'Weighted_{col}': 'sum' for col in available_time_cols}
    final_agg_dict['Processed Products'] = 'sum'
    
    summary_df = shard_df.groupby(final_group_cols).agg(final_agg_dict).reset_index()

    # Calculate final weighted averages
    def calc_weighted_avg(row, col_name):
        if row['Processed Products'] > 0:
            return row[f'Weighted_{col_name}'] / row['Processed Products']
        return 0

    for col in available_time_cols:
        summary_df[col] = summary_df.apply(lambda row: calc_weighted_avg(row, col), axis=1)

    # --- RENAME AND FORMAT ---
    column_mapping = {
        'SPL Name': 'SPL',
        'Coverage Type': 'coverage type',
        'Processed Products': 'number of products',
        'Total Time(ms)': 'average total time',
        'SAT Time(ms)': 'average sat time',
        'ProductESGGeneration Time(ms)': 'average product gen time',
        'Test Generation Time(ms)': 'average test gen time'
    }
    summary_df = summary_df.rename(columns=column_mapping)
    
    # Select target columns dynamically based on what's available
    target_columns = ['SPL', 'coverage type', 'number of products', 
                      'average total time', 'average sat time', 
                      'average product gen time', 'average test gen time']
    
    target_columns = [c for c in target_columns if c in summary_df.columns]
    summary_df = summary_df[target_columns]

    # Re-format numbers to European style (comma)
    for col in summary_df.columns:
        if col != 'SPL' and col != 'coverage type':
             if col == 'number of products':
                 summary_df[col] = summary_df[col].apply(lambda x: str(int(x)) if x.is_integer() else f"{x:.2f}".replace('.', ','))
             else:
                 summary_df[col] = summary_df[col].apply(lambda x: f"{x:.2f}".replace('.', ','))

    summary_df = summary_df.sort_values(by=['SPL', 'coverage type'])
    
    summary_df.to_csv(output_file, sep=";", index=False)
    print(f"✅ Time Measurement Summary Updated (With Breakdown): {output_file}")

if __name__ == "__main__":
    generate_time_measurement_summary()