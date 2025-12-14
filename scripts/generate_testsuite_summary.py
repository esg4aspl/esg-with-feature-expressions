import pandas as pd
import os
import glob
import sys

# ================= DYNAMIC PATH CONFIGURATION =================
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.dirname(SCRIPT_DIR)
CASES_BASE_DIR = os.path.join(PROJECT_ROOT, "files", "Cases")
OUTPUT_FILE = os.path.join(CASES_BASE_DIR, "SPLTestSuiteSummary.csv")

# ================= CASE CONFIGURATION =================
CASE_MAPPING = {
    "BankAccountv2": "BAv2",
    "Elevator": "El",
    "eMail": "eM",
    "StudentAttendanceSystem": "SAS",
    "syngovia": "Svia",
    "Tesla": "Tesla"
}

SHARD_SUBFOLDER = "shards_testsequencegeneration"

# Calculation Logic Prefixes
TOTAL_PREFIXES = ["Total"] 
AVG_PREFIXES = ["Avg", "Average"]
STATIC_COLS = ["SPL", "ESG-Fx Number of Vertices", "ESG-Fx Number of Edges"]

def process_cases():
    all_summaries = []
    skipped_files = [] 
    
    final_column_order = [] 

    print(f"🚀 Starting aggregation for {len(CASE_MAPPING)} cases...")
    print(f"📍 Script Location: {SCRIPT_DIR}")

    for case_folder, expected_spl_code in CASE_MAPPING.items():
        
        case_path = os.path.join(CASES_BASE_DIR, case_folder, SHARD_SUBFOLDER)
        file_pattern = os.path.join(case_path, "testsuite.shard*.csv")
        all_files = glob.glob(file_pattern)

        if not all_files:
            print(f"⚠️  WARNING: No files found for case: {case_folder}")
            continue

        print(f"   📂 Processing {case_folder} -> Expecting SPL: '{expected_spl_code}'")

        df_list = []
        
        for file in all_files:
            try:
                # 1. Check if file is empty
                if os.stat(file).st_size == 0:
                    print(f"      ⚠️  SKIPPING EMPTY FILE: {os.path.basename(file)}")
                    skipped_files.append(file)
                    continue

                # =========================================================
                # READING CSV (Semi-colon sep, Comma decimal)
                # =========================================================
                df = pd.read_csv(file, sep=';', decimal=',') 
                
                # TRIM WHITESPACE from headers
                df.columns = df.columns.str.strip()

                # 2. Header Check
                if 'SPL' not in df.columns:
                    print(f"      ⚠️  SKIPPING MALFORMED FILE (No SPL Header): {os.path.basename(file)}")
                    skipped_files.append(file)
                    continue 
                
                # 3. Content Check
                if df.empty:
                     print(f"      ⚠️  SKIPPING NO DATA: {os.path.basename(file)}")
                     skipped_files.append(file)
                     continue

                # 4. SPL Validation
                actual_spl_in_file = df['SPL'].iloc[0]
                if actual_spl_in_file != expected_spl_code:
                    print(f"      ⚠️  SKIPPING WRONG SPL CODE: {os.path.basename(file)}")
                    skipped_files.append(file)
                    continue
                
                # Capture Column Order
                if not final_column_order:
                    raw_cols = list(df.columns)
                    priority = ["SPL", "Number of Products"]
                    others = [c for c in raw_cols if c not in priority]
                    final_column_order = priority + others

                df_list.append(df)
                
            except Exception as e:
                print(f"      ❌ Error reading file {os.path.basename(file)}: {e}")
                skipped_files.append(file)
                continue

        if not df_list:
            print(f"      ❌ No valid files collected for {case_folder}. Skipping.")
            continue

        # --- MERGE LOGIC ---
        combined_df = pd.concat(df_list, ignore_index=True)
        result_row = {}

        # A. Static Info
        for col in STATIC_COLS:
            if col in combined_df.columns and not combined_df[col].empty:
                result_row[col] = combined_df[col].iloc[0]
            else:
                result_row[col] = 0
        
        result_row["SPL"] = expected_spl_code

        # B. Sum of Products
        total_products = combined_df["Number of Products"].sum()
        result_row["Number of Products"] = total_products

        # C. Process Other Columns
        for col in combined_df.columns:
            if col in STATIC_COLS or col == "Number of Products":
                continue 

            # Sum logic
            if any(col.startswith(prefix) for prefix in TOTAL_PREFIXES):
                result_row[col] = combined_df[col].sum()
            
            # Weighted Average logic
            elif any(col.startswith(prefix) for prefix in AVG_PREFIXES):
                weighted_sum = (combined_df[col] * combined_df["Number of Products"]).sum()
                if total_products > 0:
                    result_row[col] = weighted_sum / total_products
                else:
                    result_row[col] = 0

        all_summaries.append(result_row)

    # --- SAVE RESULT ---
    final_df = pd.DataFrame(all_summaries)
    
    if final_df.empty:
        print("\n❌ No data processed.")
        return

    # Enforce order
    valid_order = [c for c in final_column_order if c in final_df.columns]
    final_df = final_df[valid_order]

    final_df = final_df.round(2)

    os.makedirs(os.path.dirname(OUTPUT_FILE), exist_ok=True)
    
    # Save as semi-colon separated
    final_df.to_csv(OUTPUT_FILE, index=False, sep=';', decimal=',') 
    
    print(f"\n✅ SUCCESS! Summary saved to: {OUTPUT_FILE}")
    
    if skipped_files:
        print("\n⚠️  WARNING: The following files were skipped due to errors:")
        for f in skipped_files:
            print(f"   - {f}")

if __name__ == "__main__":
    process_cases()