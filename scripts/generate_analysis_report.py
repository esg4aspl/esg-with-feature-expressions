import pandas as pd
import os
import sys
import numpy as np

def parse_european_float(val):
    """Helper to convert '12,34' string to 12.34 float"""
    try:
        if pd.isna(val): return 0.0
        if isinstance(val, (int, float)): return float(val)
        return float(str(val).replace(',', '.'))
    except:
        return 0.0

def format_european_float(val):
    """Helper to convert float back to '12,34' string"""
    try:
        return f"{val:.4f}".replace('.', ',')
    except:
        return "0,0000"

def generate_analysis_report():
    # --- CONFIGURATION ---
    script_dir = os.path.dirname(os.path.abspath(__file__))
    project_root = os.path.dirname(script_dir)
    base_dir = os.path.join(project_root, "files", "Cases")
    
    # Input Files
    file_time = os.path.join(base_dir, "SPLTimeMeasurementSummary.csv")
    file_fault = os.path.join(base_dir, "SPLFaultDetectionSummary.csv")
    file_suite = os.path.join(base_dir, "SPLTestSuiteSummary.csv")
    
    # Output File
    output_file = os.path.join(base_dir, "SPLFinalAnalysisReport.csv")

    print(f"🔍 Analysis Generator started.")
    print(f"📂 Scanning: {base_dir}")

    # Check inputs
    if not (os.path.exists(file_time) and os.path.exists(file_fault) and os.path.exists(file_suite)):
        print("❌ ERROR: One or more summary files are missing. Please run the other generators first.")
        return

    try:
        # Read CSVs
        df_time = pd.read_csv(file_time, sep=";")
        df_fault = pd.read_csv(file_fault, sep=";")
        df_suite = pd.read_csv(file_suite, sep=";")
        
        print("✅ Input files loaded successfully.")

        # --- PRE-PROCESSING ---
        # 1. Clean Columns (strip spaces)
        for df in [df_time, df_fault, df_suite]:
            df.columns = df.columns.str.strip()

        # 2. Define Mappings (Time/Suite names <-> Fault Detection names)
        # Key: Coverage type in Time/Suite files
        # Value: Suffix in Fault Detection file (e.g., "Percentange L=1")
        coverage_map = {
            "randomwalk": "RandomWalk",
            "eventcoverage": "L=1",
            "eventcouplecoverage": "L=2",
            "eventtriplecoverage": "L=3",
            "eventquadruplecoverage": "L=4"
        }

        analysis_rows = []
        
        # Get unique SPLs
        spl_list = df_time['SPL'].unique()
        
        for spl in spl_list:
            for cov_type, cov_suffix in coverage_map.items():
                row_data = {
                    'SPL': spl,
                    'Method': cov_type,
                    'Method_Label': cov_suffix # Short label for graphs
                }

                # --- 1. GET TIME DATA ---
                # Filter Time DF for this SPL and Coverage Type
                time_row = df_time[
                    (df_time['SPL'] == spl) & 
                    (df_time['coverage type'] == cov_type)
                ]
                
                avg_time = 0.0
                # Assuming there is 'average test gen time' (Total time might include overhead, usually test gen time is preferred for efficiency)
                # Let's use 'average test gen time' if available, otherwise total.
                target_time_col = 'average test gen time' # Calculated in previous script
                
                if not time_row.empty:
                    avg_time = parse_european_float(time_row.iloc[0].get(target_time_col, 0))
                
                row_data['Time_Sec'] = avg_time / 1000.0 # Convert ms to Seconds for better readability
                row_data['Time_Ms'] = avg_time

                # --- 2. GET FAULT DETECTION DATA ---
                # Fault detection file has multiple rows per SPL (one for each Operator).
                # We need to AVERAGE the scores across all operators for this SPL to get a "General Score".
                fault_rows = df_fault[df_fault['SPL'] == spl]
                
                # Column name e.g., "Fault Detection Percentange RandomWalk"
                # Note: Handling typo 'Percentange' from original Java code
                fault_col = f"Fault Detection Percentange {cov_suffix}"
                # Check if column exists (sometimes with leading space)
                real_fault_col = next((c for c in df_fault.columns if c.strip() == fault_col.strip()), None)
                
                avg_fault_score = 0.0
                if not fault_rows.empty and real_fault_col:
                    # Convert column to float and calculate mean
                    scores = fault_rows[real_fault_col].apply(parse_european_float)
                    avg_fault_score = scores.mean()
                
                row_data['Avg_Fault_Detection_Rate'] = avg_fault_score

                # --- 3. GET TEST SUITE DATA (For Density) ---
                suite_row = df_suite[df_suite['SPL'] == spl]
                
                model_size = 0.0
                test_length = 0.0
                
                if not suite_row.empty:
                    # Model Size (Vertices)
                    # Column: "ESG-Fx Number of Vertices"
                    v_col = next((c for c in df_suite.columns if "Vertices" in c and "ESG-Fx" in c), None)
                    if v_col:
                        model_size = parse_european_float(suite_row.iloc[0][v_col])
                    
                    # Test Length (Total Events)
                    # Column: "Avg Number of Events: randomwalk"
                    l_col = next((c for c in df_suite.columns if "Avg Number of Events" in c and cov_type in c), None)
                    if l_col:
                        test_length = parse_european_float(suite_row.iloc[0][l_col])

                row_data['Model_Size_Vertices'] = model_size
                row_data['Avg_Test_Length'] = test_length

                # --- 4. CALCULATE SCIENTIFIC METRICS ---
                
                # Metric A: Mutation Score Efficiency (MSE)
                # Formula: Detection Rate (%) / Time (sec)
                # Interpretation: How much % do I gain per second invested?
                if row_data['Time_Sec'] > 0:
                    mse = avg_fault_score / row_data['Time_Sec']
                else:
                    mse = 0.0
                row_data['Efficiency_Score_MSE'] = mse

                # Metric B: Test Density
                # Formula: Test Length / Model Size
                # Interpretation: How many steps do we walk per node in the graph?
                # High density might mean redundancy.
                if model_size > 0:
                    density = test_length / model_size
                else:
                    density = 0.0
                row_data['Test_Suite_Density'] = density

                analysis_rows.append(row_data)

        # --- SAVE RESULT ---
        final_df = pd.DataFrame(analysis_rows)
        
        # Reorder columns logically
        cols_order = [
            'SPL', 'Method', 'Method_Label', 
            'Avg_Fault_Detection_Rate', 'Time_Ms', 'Time_Sec',
            'Efficiency_Score_MSE', 
            'Model_Size_Vertices', 'Avg_Test_Length', 'Test_Suite_Density'
        ]
        final_df = final_df[cols_order]

        # Format for CSV output (European)
        for col in final_df.columns:
            if col not in ['SPL', 'Method', 'Method_Label']:
                final_df[col] = final_df[col].apply(format_european_float)

        final_df.to_csv(output_file, sep=";", index=False)
        
        print("-" * 50)
        print(f"✅ FINAL REPORT GENERATED SUCCESSFULLY!")
        print(f"📄 File: {output_file}")
        print("-" * 50)
        print("Tip: Use 'Efficiency_Score_MSE' to prove which method is best.")
        
    except Exception as e:
        print(f"❌ An error occurred: {e}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    generate_analysis_report()