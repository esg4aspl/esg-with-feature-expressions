#!/usr/bin/env python3
"""
RQ2 Extreme Scalability Results Aggregator
===========================================

PURPOSE:
Merge shard CSV files and compute aggregated statistics for:
1. ESG-Fx (L1, L2, L3, L4)
2. EFG (L2, L3, L4) - with coverage degradation tracking
3. Random Walk (L0) - with safety limit analysis

CRITICAL METRICS:
- ESG-Fx: Throughput, scalability, 100% coverage guarantee
- EFG: Coverage degradation on large SPLs (syngo.via: 4-10%)
- Random Walk: Time complexity explosion (syngo.via: 7.4 hours)

OUTPUT:
- Merged CSV per approach/level
- Summary statistics CSV
- LaTeX table fragments for paper
"""

import pandas as pd
import numpy as np
from pathlib import Path
import sys
from typing import List, Dict, Tuple
import warnings
warnings.filterwarnings('ignore')


class RQ2ResultAggregator:
    """Aggregates RQ2 extreme scalability results across shards"""
    
    def __init__(self, base_dir: Path):
        self.base_dir = Path(base_dir)
        self.results = {}
        
    def aggregate_esgfx_l1(self, spl_name: str, shard_pattern: str = None) -> pd.DataFrame:
        """
        Aggregate ESG-Fx L1 shard results
        
        Expected columns (from TestPipelineMeasurementWriter_ExtremeScalability.writeDetailedPipelineMeasurementForESGFx_L1):
        - Run ID, SPL Name, Coverage Type
        - Total Elapsed Time(ms), SAT Time(ms), Product Gen Time(ms), Test Generation Time(ms)
        - Test Generation Peak Memory(MB)
        - Number of ESGFx Vertices, Number of ESGFx Edges
        - Number of ESGFx Test Cases, Number of ESGFx Test Events
        - L1 Coverage(%), Coverage Analysis Time(ms)
        - Test Execution Time(ms), Test Execution Peak Memory(MB)
        - Processed Products, Failed Products
        """
        if shard_pattern is None:
            shard_pattern = f"{spl_name}_ESG-Fx_L1_shard*.csv"
        
        shard_files = sorted(self.base_dir.glob(f"ESG-Fx/L1/{shard_pattern}"))
        
        if not shard_files:
            print(f"  ⚠️  No L1 shards found for {spl_name}")
            return None
        
        print(f"  Found {len(shard_files)} L1 shards")
        
        # Read and concatenate
        dfs = []
        for f in shard_files:
            df = pd.read_csv(f, sep=';', decimal=',')
            dfs.append(df)
        
        merged = pd.concat(dfs, ignore_index=True)
        
        # Compute aggregates
        stats = self._compute_statistics(merged, 'ESG-Fx_L1')
        
        return merged, stats
    
    def aggregate_esgfx_l234(self, spl_name: str, L: int, shard_pattern: str = None) -> pd.DataFrame:
        """
        Aggregate ESG-Fx L2/L3/L4 shard results
        
        Expected columns (from TestPipelineMeasurementWriter_ExtremeScalability.writeDetailedPipelineMeasurementForESGFx_L234):
        - RunID, SPL Name, Coverage Type
        - Total Elapsed Time(ms), SAT Time(ms), Product Gen Time(ms)
        - Transformation Time(ms), Test Generation Time(ms)
        - Test Generation Peak Memory(MB)
        - Number of ESGFx Vertices, Number of ESGFx Edges
        - Number of ESGFx Test Cases, Number of ESGFx Test Events
        - L{L} Coverage(%), Coverage Analysis Time(ms)
        - Test Execution Time(ms), Test Execution Peak Memory(MB)
        - Processed Products, Failed Products
        """
        if shard_pattern is None:
            shard_pattern = f"{spl_name}_ESG-Fx_L{L}_shard*.csv"
        
        shard_files = sorted(self.base_dir.glob(f"ESG-Fx/L{L}/{shard_pattern}"))
        
        if not shard_files:
            print(f"  ⚠️  No L{L} shards found for {spl_name}")
            return None
        
        print(f"  Found {len(shard_files)} L{L} shards")
        
        dfs = []
        for f in shard_files:
            df = pd.read_csv(f, sep=';', decimal=',')
            dfs.append(df)
        
        merged = pd.concat(dfs, ignore_index=True)
        stats = self._compute_statistics(merged, f'ESG-Fx_L{L}')
        
        return merged, stats
    
    def aggregate_efg(self, spl_name: str, L: int, shard_pattern: str = None) -> pd.DataFrame:
        """
        Aggregate EFG shard results
        
        Expected columns (from TestPipelineMeasurementWriter_EFG_ExtremeScalability):
        - RunID, SPL Name, Coverage Type
        - Total Elapsed Time(ms), SAT Time(ms), Product Gen Time(ms)
        - EFG Transformation Time(ms), Test Generation Time(ms)
        - Test Generation Peak Memory(MB)
        - Number of EFG Vertices, Number of EFG Edges
        - Number of EFG Test Cases, Number of EFG Test Events
        - Event Coverage(%), Event Coverage Analysis Time(ms)
        - Edge Coverage(%), Edge Coverage Analysis Time(ms)  ← CRITICAL
        - Test Execution Time(ms), Test Execution Peak Memory(MB)
        - Processed Products, Failed Products
        """
        if shard_pattern is None:
            shard_pattern = f"{spl_name}_EFG_L{L}_shard*.csv"
        
        shard_files = sorted(self.base_dir.glob(f"EFG/L{L}/{shard_pattern}"))
        
        if not shard_files:
            print(f"  ⚠️  No EFG L{L} shards found for {spl_name}")
            return None
        
        print(f"  Found {len(shard_files)} EFG L{L} shards")
        
        dfs = []
        for f in shard_files:
            df = pd.read_csv(f, sep=';', decimal=',')
            dfs.append(df)
        
        merged = pd.concat(dfs, ignore_index=True)
        stats = self._compute_statistics(merged, f'EFG_L{L}')
        
        # CRITICAL: Flag coverage degradation
        if 'Edge Coverage(%)' in merged.columns:
            avg_edge_cov = merged['Edge Coverage(%)'].mean()
            if avg_edge_cov < 50:
                print(f"  ⚠️  COVERAGE DEGRADATION: EFG L{L} achieves only {avg_edge_cov:.2f}% edge coverage")
        
        return merged, stats
    
    def aggregate_randomwalk(self, spl_name: str, shard_pattern: str = None) -> pd.DataFrame:
        """
        Aggregate Random Walk shard results
        
        Expected columns (from TestPipelineMeasurementWriter_RandomWalk_ExtremeScalability):
        - RunID, SPL Name, Coverage Type (L0)
        - Total Elapsed Time(ms), SAT Time(ms), Product Gen Time(ms), Test Generation Time(ms)
        - Test Generation Peak Memory(MB)
        - Number of Vertices, Number of Edges
        - Number of Test Cases, Number of Test Events, Aborted Sequences
        - Event Coverage(%), Event Coverage Analysis Time(ms)
        - Edge Coverage(%), Edge Coverage Analysis Time(ms)
        - Test Execution Time(ms), Test Execution Peak Memory(MB)
        - Safety Limit Hit Count ← CRITICAL
        - Avg Time on Safety Limit(ms) ← CRITICAL
        - Avg Steps on Safety Limit ← CRITICAL
        - Avg Coverage at Safety Limit(%) ← CRITICAL
        - Processed Products, Failed Products
        """
        if shard_pattern is None:
            shard_pattern = f"{spl_name}_RandomWalk_L0_shard*.csv"
        
        shard_files = sorted(self.base_dir.glob(f"RandomWalk/L0/{shard_pattern}"))
        
        if not shard_files:
            print(f"  ⚠️  No Random Walk shards found for {spl_name}")
            return None
        
        print(f"  Found {len(shard_files)} Random Walk shards")
        
        dfs = []
        for f in shard_files:
            df = pd.read_csv(f, sep=';', decimal=',')
            dfs.append(df)
        
        merged = pd.concat(dfs, ignore_index=True)
        stats = self._compute_statistics(merged, 'RandomWalk_L0')
        
        # CRITICAL: Flag time complexity explosion
        if 'Total Elapsed Time(ms)' in merged.columns:
            total_time_hours = merged['Total Elapsed Time(ms)'].mean() / (1000 * 60 * 60)
            if total_time_hours > 1.0:
                print(f"  ⚠️  TIME COMPLEXITY EXPLOSION: Random Walk took {total_time_hours:.2f} hours on average")
        
        if 'Safety Limit Hit Count' in merged.columns:
            safety_hits = merged['Safety Limit Hit Count'].sum()
            if safety_hits > 0:
                print(f"  ⚠️  SAFETY LIMIT HITS: {safety_hits} products hit 5|V|³ limit")
        
        return merged, stats
    
    def _compute_statistics(self, df: pd.DataFrame, approach: str) -> Dict:
        """
        Compute median, mean, std, min, max for all numeric columns
        
        Returns dict with structure:
        {
            'approach': str,
            'n_runs': int,
            'total_products_processed': int,
            'total_products_failed': int,
            'metrics': {
                'column_name': {
                    'median': float,
                    'mean': float,
                    'std': float,
                    'min': float,
                    'max': float
                },
                ...
            }
        }
        """
        stats = {
            'approach': approach,
            'n_runs': len(df),
            'metrics': {}
        }
        
        # Aggregate processed/failed products
        if 'Processed Products' in df.columns:
            stats['total_products_processed'] = df['Processed Products'].sum()
        if 'Failed Products' in df.columns:
            stats['total_products_failed'] = df['Failed Products'].sum()
        
        # Compute statistics for numeric columns
        numeric_cols = df.select_dtypes(include=[np.number]).columns
        
        for col in numeric_cols:
            if col in ['Run ID', 'RunID']:
                continue
            
            stats['metrics'][col] = {
                'median': df[col].median(),
                'mean': df[col].mean(),
                'std': df[col].std(),
                'min': df[col].min(),
                'max': df[col].max()
            }
        
        return stats
    
    def generate_summary_table(self, all_stats: List[Dict]) -> pd.DataFrame:
        """
        Generate summary table for paper
        
        Format:
        | Approach | SPL | Total Products | Failed | Avg Time (min) | Avg Coverage (%) | Avg Memory (MB) |
        """
        rows = []
        
        for stat in all_stats:
            approach = stat['approach']
            
            # Extract key metrics
            total_time_ms = stat['metrics'].get('Total Elapsed Time(ms)', {}).get('median', 0)
            total_time_min = total_time_ms / 60000.0
            
            # Coverage (depends on approach)
            coverage = None
            if 'L1 Coverage(%)' in stat['metrics']:
                coverage = stat['metrics']['L1 Coverage(%)']['median']
            elif 'L2 Percent(%)' in stat['metrics']:
                coverage = stat['metrics']['L2 Percent(%)']['median']
            elif 'L3 Percent(%)' in stat['metrics']:
                coverage = stat['metrics']['L3 Percent(%)']['median']
            elif 'L4 Percent(%)' in stat['metrics']:
                coverage = stat['metrics']['L4 Percent(%)']['median']
            elif 'Edge Coverage(%)' in stat['metrics']:
                coverage = stat['metrics']['Edge Coverage(%)']['median']
            
            # Memory
            gen_mem = stat['metrics'].get('Test Generation Peak Memory(MB)', {}).get('median', 0)
            exec_mem = stat['metrics'].get('Test Execution Peak Memory(MB)', {}).get('median', 0)
            peak_mem = max(gen_mem, exec_mem)
            
            rows.append({
                'Approach': approach,
                'Total Products': stat.get('total_products_processed', 0),
                'Failed Products': stat.get('total_products_failed', 0),
                'Median Time (min)': total_time_min,
                'Median Coverage (%)': coverage if coverage is not None else 'N/A',
                'Peak Memory (MB)': peak_mem
            })
        
        return pd.DataFrame(rows)
    
    def export_to_latex(self, summary_df: pd.DataFrame, output_file: Path):
        """Export summary table as LaTeX fragment"""
        with open(output_file, 'w') as f:
            f.write("% RQ2 Extreme Scalability Summary\n")
            f.write("% Generated by RQ2_aggregate_results.py\n\n")
            
            latex = summary_df.to_latex(
                index=False,
                float_format="%.2f",
                caption="RQ2 Extreme Scalability: ESG-Fx vs EFG vs Random Walk",
                label="tab:rq2_scalability"
            )
            f.write(latex)


def main():
    if len(sys.argv) < 3:
        print("USAGE: python RQ2_aggregate_results.py <base_dir> <spl_name> [output_dir]")
        print("EXAMPLE: python RQ2_aggregate_results.py Cases/Tesla/extremeScalabilityTestPipeline Tesla results/")
        sys.exit(1)
    
    base_dir = Path(sys.argv[1])
    spl_name = sys.argv[2]
    output_dir = Path(sys.argv[3]) if len(sys.argv) > 3 else base_dir / "aggregated"
    output_dir.mkdir(parents=True, exist_ok=True)
    
    print(f"\n{'='*80}")
    print(f"RQ2 EXTREME SCALABILITY AGGREGATION: {spl_name}")
    print(f"{'='*80}\n")
    
    aggregator = RQ2ResultAggregator(base_dir)
    all_stats = []
    
    # 1. ESG-Fx L1
    print("📊 Aggregating ESG-Fx L1...")
    result = aggregator.aggregate_esgfx_l1(spl_name)
    if result:
        merged, stats = result
        merged.to_csv(output_dir / f"{spl_name}_ESG-Fx_L1_merged.csv", sep=';', decimal=',', index=False)
        all_stats.append(stats)
        print(f"  ✓ Saved to {output_dir / f'{spl_name}_ESG-Fx_L1_merged.csv'}")
    
    # 2. ESG-Fx L2, L3, L4
    for L in [2, 3, 4]:
        print(f"\n📊 Aggregating ESG-Fx L{L}...")
        result = aggregator.aggregate_esgfx_l234(spl_name, L)
        if result:
            merged, stats = result
            merged.to_csv(output_dir / f"{spl_name}_ESG-Fx_L{L}_merged.csv", sep=';', decimal=',', index=False)
            all_stats.append(stats)
            print(f"  ✓ Saved to {output_dir / f'{spl_name}_ESG-Fx_L{L}_merged.csv'}")
    
    # 3. EFG L2, L3, L4
    for L in [2, 3, 4]:
        print(f"\n📊 Aggregating EFG L{L}...")
        result = aggregator.aggregate_efg(spl_name, L)
        if result:
            merged, stats = result
            merged.to_csv(output_dir / f"{spl_name}_EFG_L{L}_merged.csv", sep=';', decimal=',', index=False)
            all_stats.append(stats)
            print(f"  ✓ Saved to {output_dir / f'{spl_name}_EFG_L{L}_merged.csv'}")
    
    # 4. Random Walk L0
    print(f"\n📊 Aggregating Random Walk L0...")
    result = aggregator.aggregate_randomwalk(spl_name)
    if result:
        merged, stats = result
        merged.to_csv(output_dir / f"{spl_name}_RandomWalk_L0_merged.csv", sep=';', decimal=',', index=False)
        all_stats.append(stats)
        print(f"  ✓ Saved to {output_dir / f'{spl_name}_RandomWalk_L0_merged.csv'}")
    
    # 5. Generate summary
    if all_stats:
        print(f"\n📊 Generating summary table...")
        summary_df = aggregator.generate_summary_table(all_stats)
        summary_csv = output_dir / f"{spl_name}_RQ2_summary.csv"
        summary_df.to_csv(summary_csv, index=False)
        print(f"  ✓ Summary saved to {summary_csv}")
        
        # LaTeX export
        latex_file = output_dir / f"{spl_name}_RQ2_summary.tex"
        aggregator.export_to_latex(summary_df, latex_file)
        print(f"  ✓ LaTeX table saved to {latex_file}")
        
        # Display summary
        print(f"\n{'='*80}")
        print("SUMMARY:")
        print(f"{'='*80}")
        print(summary_df.to_string(index=False))
    
    print(f"\n{'='*80}")
    print("✅ RQ2 AGGREGATION COMPLETE")
    print(f"{'='*80}\n")


if __name__ == "__main__":
    main()