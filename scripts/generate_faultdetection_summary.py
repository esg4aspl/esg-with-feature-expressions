import pandas as pd
import glob
import os
import numpy as np

def generate_fault_detection_summary():
    # --- CONFIGURATION ---
    script_dir = os.path.dirname(os.path.abspath(__file__))
    project_root = os.path.dirname(script_dir)
    base_dir = os.path.join(project_root, "files", "Cases")
    output_file = os.path.join(base_dir, "SPLFaultDetectionSummary.csv")

    target_folders = [
        "shards_mutantgenerator_edgeinserter",
        "shards_mutantgenerator_edgeomitter",
        "shards_mutantgenerator_eventinserter",
        "shards_mutantgenerator_eventomitter"
    ]

    print(f"🔍 Summarizer started. Scanning: {base_dir}")

    if not os.path.exists(base_dir):
        print(f"⚠️ Warning: Base directory not found. Skipping summary.")
        return

    all_dataframes = []
    cases = [d for d in os.listdir(base_dir) if os.path.isdir(os.path.join(base_dir, d))]
    cases.sort()

    found_any = False

    for case_name in cases:
        case_path = os.path.join(base_dir, case_name)
        
        for subfolder in target_folders:
            shard_dir = os.path.join(case_path, subfolder)
            
            if os.path.exists(shard_dir):
                pattern = os.path.join(shard_dir, "faultdetection.shard*.csv")
                shard_files = glob.glob(pattern)
                
                if shard_files:
                    found_any = True
                    # Sadece dosya bulursa ekrana bas, yoksa sessiz geç
                    # print(f"   🔹 Found data in: {case_name} / {subfolder}")
                    
                    case_dfs = []
                    for f in shard_files:
                        try:
                            df = pd.read_csv(f, sep=";")
                            if 'SPL' in df.columns: df = df.dropna(subset=['SPL'])
                            for col in df.columns:
                                if df[col].dtype == 'object':
                                    try:
                                        if df[col].str.match(r'^-?\d+(?:,\d+)?$').all():
                                            df[col] = df[col].str.replace(',', '.').astype(float)
                                    except: pass
                            case_dfs.append(df)
                        except: pass # Hatalı dosya varsa sessizce geç
                    
                    if case_dfs: all_dataframes.extend(case_dfs)

    if not all_dataframes:
        print("⚠️  No fault detection data found across all cases. Summary NOT created.")
        return

    # ... (Matematiksel işlemler aynen devam ediyor) ...
    print("📊 Aggregating data...")
    full_df = pd.concat(all_dataframes, ignore_index=True)
    
    group_cols = ['SPL', 'Operator']
    weight_col = "Number of Mutants"
    num_cols = [c for c in full_df.columns if "Number of" in c]
    speed_cols = [c for c in full_df.columns if "Per Second" in c]

    def weighted_avg(x, weights):
        if weights.sum() == 0: return 0
        return np.average(x, weights=weights)

    grouped = full_df.groupby(group_cols)
    summary_rows = []

    for name, group in grouped:
        spl, operator = name
        row = {'SPL': spl, 'Operator': operator}
        total_mutants = group[weight_col].sum()
        row[weight_col] = total_mutants
        
        for col in num_cols:
            if col != weight_col: row[col] = group[col].sum()

        for col in speed_cols:
            if pd.api.types.is_numeric_dtype(group[col]):
                w_avg = weighted_avg(group[col], group[weight_col])
                row[col] = w_avg
            else: row[col] = 0

        for col in full_df.columns:
            if "Fault Detection Percentange" in col:
                suffix = col.split("Percentange")[-1]
                matching_count_col = next((c for c in num_cols if c.endswith(suffix) and "Detected" in c), None)
                if matching_count_col:
                    total_detected = row[matching_count_col]
                    row[col] = (total_detected / total_mutants * 100) if total_mutants > 0 else 0
                else: row[col] = 0

        summary_rows.append(row)

    summary_df = pd.DataFrame(summary_rows)
    for col in summary_df.columns:
        if pd.api.types.is_numeric_dtype(summary_df[col]):
             summary_df[col] = summary_df[col].apply(lambda x: f"{x:.2f}".replace('.', ','))

    cols = list(summary_df.columns)
    for c in ['SPL', 'Operator', 'Number of Mutants']: 
        if c in cols: cols.remove(c)
    final_cols = ['SPL', 'Operator', 'Number of Mutants'] + sorted(cols)
    final_cols = [c for c in final_cols if c in summary_df.columns]
    summary_df = summary_df[final_cols]

    summary_df.to_csv(output_file, sep=";", index=False)
    print(f"✅ Fault Detection Summary Updated: {output_file}")

if __name__ == "__main__":
    generate_fault_detection_summary()