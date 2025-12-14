import pandas as pd
import os
import glob
import sys

# ================= DYNAMIC PATH CONFIGURATION =================
# Scriptin bulunduğu konuma göre dinamik yol belirleme
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.dirname(SCRIPT_DIR)
CASES_BASE_DIR = os.path.join(PROJECT_ROOT, "files", "Cases")
OUTPUT_FILE = os.path.join(CASES_BASE_DIR, "SPLFaultDetectionSummary.csv")

# ================= CASE & OPERATOR CONFIGURATION =================
# Case Klasör Adı -> Beklenen SPL Kısaltması (CSV içindeki)
CASE_MAPPING = {
    "BankAccountv2": "BAv2",
    "Elevator": "El",
    "eMail": "eM",
    "SodaVendingMachine": "SVM",
    "StudentAttendanceSystem": "SAS",
    "Tesla": "Te",
    "syngovia": "Svia" 
}

# Operator Adı -> Klasör Soneki
# Script hem Edge Omitter hem Event Omitter için çalışacak şekilde ayarlandı
OPERATORS = {
    "Edge Omitter": "shards_mutantgenerator_edgeomitter",
    "Event Omitter": "shards_mutantgenerator_eventomitter"
}

# Hangi sütunların toplanacağı (Sum), hangilerinin yeniden hesaplanacağı
SUM_COLS = [
    "Number of Mutants",
    "Number of Detected  Mutants RandomWalk",
    "Detected Mutants Per Second RandomWalk",
    "Number of Detected  Mutants L=1",
    "Detected Mutants Per Second L=1",
    "Number of Detected  Mutants L=2",
    "Detected Mutants Per Second L=2",
    "Number of Detected  Mutants L=3",
    "Detected Mutants Per Second L=3",
    "Number of Detected  Mutants L=4",
    "Detected Mutants Per Second L=4"
]

# Yüzde sütunları (Toplanmayacak, hesaplanacak)
PERCENT_COLS_MAPPING = {
    "Fault Detection Percentange RandomWalk": "Number of Detected  Mutants RandomWalk",
    "Fault Detection Percentange L=1": "Number of Detected  Mutants L=1",
    "Fault Detection Percentange L=2": "Number of Detected  Mutants L=2",
    "Fault Detection Percentange L=3": "Number of Detected  Mutants L=3",
    "Fault Detection Percentange L=4": "Number of Detected  Mutants L=4"
}

# Çıktıdaki sütun sırasını korumak için (Senin verdiğin örneğe göre)
FINAL_COLUMN_ORDER = [
    "SPL", "Operator", "Number of Mutants",
    "Number of Detected  Mutants RandomWalk", "Fault Detection Percentange RandomWalk", "Detected Mutants Per Second RandomWalk",
    "Number of Detected  Mutants L=1", "Fault Detection Percentange L=1", "Detected Mutants Per Second L=1",
    "Number of Detected  Mutants L=2", "Fault Detection Percentange L=2", "Detected Mutants Per Second L=2",
    "Number of Detected  Mutants L=3", "Fault Detection Percentange L=3", "Detected Mutants Per Second L=3",
    "Number of Detected  Mutants L=4", "Fault Detection Percentange L=4", "Detected Mutants Per Second L=4"
]

def process_fault_detection():
    all_summaries = []
    
    print(f"🚀 Starting Fault Detection Aggregation...")
    print(f"📍 Script Location: {SCRIPT_DIR}")

    for case_folder, expected_spl_code in CASE_MAPPING.items():
        for operator_name, folder_suffix in OPERATORS.items():
            
            # Dinamik klasör yolu: .../Cases/{CaseName}/{shards_folder}
            target_dir = os.path.join(CASES_BASE_DIR, case_folder, folder_suffix)
            
            # Klasör var mı kontrolü
            if not os.path.exists(target_dir):
                # Bazı case'lerde Event Omitter olmayabilir, sessizce geçebiliriz veya info basabiliriz
                # print(f"   ℹ️  Folder not found (skipping): {target_dir}")
                continue

            # Klasör içindeki faultdetection*.csv dosyalarını bul
            file_pattern = os.path.join(target_dir, "faultdetection*.csv")
            all_files = glob.glob(file_pattern)

            if not all_files:
                print(f"   ⚠️  WARNING: Folder exists but IS EMPTY: {case_folder} -> {operator_name}")
                print(f"       Path: {target_dir}")
                continue

            print(f"   📂 Processing {case_folder} [{operator_name}] -> Found {len(all_files)} files.")

            df_list = []
            for file in all_files:
                try:
                    if os.stat(file).st_size == 0:
                        continue
                    
                    # CSV Oku (Noktalı virgül ayracı, virgül ondalık)
                    df = pd.read_csv(file, sep=';', decimal=',')
                    df.columns = df.columns.str.strip() # Headerdaki boşlukları temizle
                    
                    # Basit Validasyon
                    if 'SPL' not in df.columns:
                        print(f"      ⚠️  Skipping malformed file: {os.path.basename(file)}")
                        continue
                        
                    # SPL Kodunun doğruluğunu kontrol et
                    if df['SPL'].iloc[0] != expected_spl_code:
                        print(f"      ⚠️  Skipping file with wrong SPL code: {os.path.basename(file)}")
                        continue

                    df_list.append(df)

                except Exception as e:
                    print(f"      ❌ Error reading {os.path.basename(file)}: {e}")
                    continue
            
            if not df_list:
                continue

            # --- AGGREGATION LOGIC ---
            combined_df = pd.concat(df_list, ignore_index=True)
            result_row = {}

            # 1. Static Columns
            result_row["SPL"] = expected_spl_code
            result_row["Operator"] = operator_name

            # 2. Sum Columns (Mutants, Detected, Per Second)
            for col in SUM_COLS:
                if col in combined_df.columns:
                    result_row[col] = combined_df[col].sum()
                else:
                    result_row[col] = 0

            # 3. Recalculate Percentages
            # Formül: (Toplam Yakalanan / Toplam Mutant) * 100
            total_mutants = result_row["Number of Mutants"]
            
            for pct_col, detected_col in PERCENT_COLS_MAPPING.items():
                detected_count = result_row.get(detected_col, 0)
                
                if total_mutants > 0:
                    percentage = (detected_count / total_mutants) * 100.0
                    # Yüzde 100'ü geçemez (veri hatası koruması)
                    result_row[pct_col] = min(percentage, 100.0)
                else:
                    result_row[pct_col] = 0.0

            all_summaries.append(result_row)

    # --- SAVE RESULTS ---
    if not all_summaries:
        print("\n❌ No valid data found to process.")
        return

    final_df = pd.DataFrame(all_summaries)

    # İstenen sütun sırasını uygula
    # Veri setinde olmayan sütunlar varsa hata vermemesi için filtreleyelim
    valid_cols = [c for c in FINAL_COLUMN_ORDER if c in final_df.columns]
    final_df = final_df[valid_cols]

    # Sayıları yuvarla (2 basamak)
    final_df = final_df.round(2)

    # Klasörü oluştur ve kaydet
    os.makedirs(os.path.dirname(OUTPUT_FILE), exist_ok=True)
    
    # Format: Noktalı virgül (;) ayracı ve virgül (,) ondalık
    final_df.to_csv(OUTPUT_FILE, index=False, sep=';', decimal=',')
    
    print(f"\n✅ SUCCESS! Summary saved to: {OUTPUT_FILE}")

if __name__ == "__main__":
    process_fault_detection()