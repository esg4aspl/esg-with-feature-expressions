import os
import glob
import pandas as pd
import numpy as np

LOCAL_CASES_ROOT = "/Users/dilekozturk/git/esg-with-feature-expressions/files/Cases"
SCRIPTS_FOLDER = "/Users/dilekozturk/git/esg-with-feature-expressions/files/scripts"

def load_and_standardize_rq2_data():
    search_pattern = os.path.join(LOCAL_CASES_ROOT, "*", "extremeScalabilityTestPipeline", "ESG-Fx", "*", "*_ESG-Fx_L*_shard*.csv")
    all_files = glob.glob(search_pattern)
    
    if not all_files:
        print("No CSV files found for RQ2 Extreme Scalability. Please check the paths.")
        return pd.DataFrame()

    df_list = []
    for file in all_files:
        try:
            temp_df = pd.read_csv(file, sep=';', decimal=',')
            
            if 'Transformation Time(ms)' not in temp_df.columns:
                temp_df['Transformation Time(ms)'] = 0.0
                
            cov_col = [col for col in temp_df.columns if 'Coverage(%)' in col]
            if cov_col:
                temp_df['Normalized_Coverage'] = temp_df[cov_col[0]]
            else:
                temp_df['Normalized_Coverage'] = 0.0
                
            if 'Run ID' in temp_df.columns:
                temp_df.rename(columns={'Run ID': 'RunID'}, inplace=True)
                
            df_list.append(temp_df)
        except Exception as e:
            print(f"Error reading {file}: {e}")

    if not df_list:
        return pd.DataFrame()
        
    return pd.concat(df_list, ignore_index=True)

def aggregate_shards_to_runs(df):
    df['Weighted_Coverage_Sum'] = df['Normalized_Coverage'] * df['Processed Products']

    run_level_agg = df.groupby(['SPL Name', 'Coverage Type', 'RunID']).agg({
        'Total Elapsed Time(ms)': 'sum',
        'SAT Time(ms)': 'sum',
        'Product Gen Time(ms)': 'sum',
        'Transformation Time(ms)': 'sum',
        'Test Generation Time(ms)': 'sum',
        'Test Generation Peak Memory(MB)': 'max',
        'Number of ESGFx Vertices': 'sum',
        'Number of ESGFx Edges': 'sum',
        'Number of ESGFx Test Cases': 'sum',
        'Number of ESGFx Test Events': 'sum',
        'Coverage Analysis Time(ms)': 'sum',
        'Test Execution Time(ms)': 'sum',
        'Test Execution Peak Memory(MB)': 'max',
        'Processed Products': 'sum',
        'Failed Products': 'sum',
        'Weighted_Coverage_Sum': 'sum'
    }).reset_index()

    run_level_agg['Final_Coverage(%)'] = np.where(
        run_level_agg['Processed Products'] > 0,
        run_level_agg['Weighted_Coverage_Sum'] / run_level_agg['Processed Products'],
        0.0
    )
    
    run_level_agg.drop(columns=['Weighted_Coverage_Sum'], inplace=True)
    return run_level_agg

def extract_representative_run(group_df):
    sorted_group = group_df.sort_values(by='Total Elapsed Time(ms)').reset_index(drop=True)
    median_index = len(sorted_group) // 2
    return sorted_group.iloc[median_index]

def compute_representative_runs(run_level_df):
    representative_runs = run_level_df.groupby(['SPL Name', 'Coverage Type']).apply(extract_representative_run).reset_index(drop=True)
    return representative_runs

if __name__ == "__main__":
    print("Processing RQ2 Extreme Scalability Data...")
    os.makedirs(SCRIPTS_FOLDER, exist_ok=True)
    
    raw_df = load_and_standardize_rq2_data()
    
    if not raw_df.empty:
        run_level_df = aggregate_shards_to_runs(raw_df)
        summary_df = compute_representative_runs(run_level_df)
        
        output_file = os.path.join(SCRIPTS_FOLDER, 'RQ2_ExtremeScalability_Summary.xlsx')
        
        with pd.ExcelWriter(output_file, engine='openpyxl') as writer:
            summary_df.to_excel(writer, sheet_name='SPL_Summary', index=False, float_format="%.2f")
            
            spl_names = run_level_df['SPL Name'].unique()
            for spl in spl_names:
                spl_data = run_level_df[run_level_df['SPL Name'] == spl]
                
                safe_sheet_name = str(spl)[:31]
                spl_data.to_excel(writer, sheet_name=safe_sheet_name, index=False, float_format="%.2f")
                
        print(f"Successfully generated {output_file}")
    else:
        print("Dataframe is empty, skipping Excel generation.")