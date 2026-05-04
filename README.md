# Reproducibility Guide

This document accompanies the manuscript **"Variability-Aware Event Sequence Graphs for Scalable Software Product Line Testing"** and provides step-by-step instructions to reproduce every experimental result reported in the paper.

The full study uses a 10-node, 80-core distributed cluster on DigitalOcean to evaluate the **Model Once, Generate Any** approach against two baselines — the **Structural Baseline** (Event Flow Graph testing via the GUITAR framework) and the **Stochastic Baseline** (Random Walk traversal) — across eight Software Product Lines ranging from 12 to 124 billion product configurations.

---

## Table of Contents

1. [Overview](#1-overview)
2. [Hardware and Environment](#2-hardware-and-environment)
3. [Software Prerequisites](#3-software-prerequisites)
4. [Repository Layout](#4-repository-layout)
5. [Feature-Model Encoding and Product Sampling](#5-feature-model-encoding-and-product-sampling)
6. [Phase 1 — Cluster Provisioning](#6-phase-1--cluster-provisioning)
7. [Phase 2 — Orchestrated Execution](#7-phase-2--orchestrated-execution)
8. [Phase 3 — Live Monitoring](#8-phase-3--live-monitoring)
9. [Phase 4 — Result Collection](#9-phase-4--result-collection)
10. [Phase 5 — Aggregation and Statistical Analysis](#10-phase-5--aggregation-and-statistical-analysis)
11. [Wall-clock and Cost Estimate](#11-wall-clock-and-cost-estimate)
12. [Data Archive](#12-data-archive)
13. [Re-running on Different Infrastructure](#13-re-running-on-different-infrastructure)
14. [Troubleshooting](#14-troubleshooting)
15. [Acknowledgements](#15-acknowledgements)
16. [Citation](#16-citation)

---

## 1. Overview

The reproduction workflow has five phases:

| Phase | Where it runs | What it does |
|-------|---------------|--------------|
| 1. Cluster provisioning | DigitalOcean web console | Create 10 identical droplets |
| 2. Orchestrated execution | Local machine → cluster | One script provisions every node and runs all RQs |
| 3. Live monitoring | Local machine | Cluster status dashboard + log error scanning |
| 4. Result collection | Local machine ← cluster | Pull all CSV outputs into the local repository |
| 5. Aggregation + analysis | Local machine | Python scripts merge shards and run the statistical tests |

Phases 1–4 involve the cluster; Phase 5 runs entirely on the local machine.

---

## 2. Hardware and Environment

### 2.1 Cluster nodes (DigitalOcean)

| Property | Value |
|----------|-------|
| Plan | Basic |
| CPU option | Regular |
| vCPUs / node | 8 |
| RAM / node | 16 GB |
| Disk | 320 GB SSD |
| Bandwidth | 6 TB |
| Slug | `s-8vcpu-16gb` |
| Image | Ubuntu 22.04 LTS x64 |
| Datacenter region used in the original experiments | Amsterdam 3 (slug `ams3`) |
| Number of nodes | 10 (= 80 vCPUs total) |

### 2.2 Local orchestration machine

- macOS or Linux with Bash 3.2+ (the scripts are Bash-3.2 compatible so they run on stock macOS)
- `ssh`, `scp`
- Python 3.9+ for the analysis pipeline (developed on Anaconda 3.9.13)
- An SSH keypair registered in the DigitalOcean account *before* creating the droplets

---

## 3. Software Prerequisites

### 3.1 Cluster nodes — installed automatically

The provisioning script (`Setup_And_Run_DigitalOcean.sh`, see §7.2) installs the following on every node, so no manual setup is required after the droplet is created:

- OpenJDK 11 (default; runs the Model Once, Generate Any framework)
- OpenJDK 8 (required by GUITAR-based Structural Baseline)
- Maven 3.x
- `git`, `dos2unix`, `curl`

### 3.2 Local machine — install manually

Required for the analysis phase:

```bash
pip install pandas matplotlib numpy scipy statsmodels
```

---

## 4. Repository Layout

The directories that matter for reproduction:

```
esg-with-feature-expressions/
├── README.md
├── REPRODUCIBILITY.md                     ← this file
├── pom.xml                                ← Maven build
├── src/                                   ← Java sources
└── files/
    ├── Cases/                             ← per-Software-Product-Line inputs and outputs
    │   ├── SodaVendingMachine/
    │   ├── eMail/
    │   ├── Elevator/
    │   ├── BankAccountv2/
    │   ├── StudentAttendanceSystem/
    │   ├── syngovia/
    │   ├── Tesla/
    │   └── HockertyShirts/
    ├── bashscripts/                       ← experiment orchestration
    │   ├── digitalocean/                  ← cluster-level scripts
    │   │   ├── ips.txt                    ← cluster IP list (template)
    │   │   ├── Setup_And_Run_DigitalOcean.sh
    │   │   ├── Cluster_Monitor.sh
    │   │   ├── Cluster_Error_Scanner.sh
    │   │   ├── Cluster_Log_Collector.sh
    │   │   ├── Collect_Results_RQ1.sh
    │   │   ├── Collect_Results_RQ2.sh
    │   │   ├── Collect_Results_RQ3.sh
    │   │   └── Collect_DOT_EFG_CSVs.sh
    │   ├── Global_Orchestrator.sh         ← per-node phase driver
    │   ├── RQ0_ProductGeneration_Master.sh
    │   ├── RQ1_ComparativeEfficiency_Master.sh
    │   ├── RQ2_ExtremeScalability_Master.sh
    │   ├── RQ3_FaultDetection_Master.sh
    │   └── RQ3_RandomWalk_DampingSensivityAnalysis.sh
    ├── logs/                              ← per-node logs (created at runtime)
    └── scripts/                           ← Python analysis pipeline
        ├── analyse_featuremodel.py
        ├── rq1_01_aggregate_shards_perRun.py
        ├── rq1_02_aggregate_shards_perProduct.py
        ├── rq2_00_data_integrity_check.py
        ├── rq2_01_merge_shards_per_spl.py
        ├── rq2_02_median_across_runs.py
        ├── rq2_03_aggregate_across_shards.py
        ├── rq3_01_merge_shards_per_spl.py
        ├── rq3_02_aggregate_across_spls.py
        ├── rq3_equivalent_mutants/
        │   └── rq3_03_analyze_equivalent_mutants.py
        └── statistical_test_scripts/
            ├── rq1_03_per_product_scatter_spearman.py
            ├── rq1_04_scaling_regression.py
            ├── rq1_05_pairwise_wilcoxon.py
            ├── rq1_06_run_stability.py
            ├── rq1_07_mechanism_and_warmup.py
            ├── rq2_04_pairwise_wilcoxon_shards.py
            ├── rq2_06_time_breakdown.py
            ├── rq2_07_run_stability_industrial.py
            ├── rq3_04_pairwise_wilcoxon.py
            ├── rq3_05_effectsize_bh.py
            ├── rq3_06_paired_edge_vs_event.py
            ├── rq3_07_damping_sensitivity.py
            ├── rq3_08_multiseed_stability.py
            └── rq3_09_make_tables.py
```

---

## 5. Feature-Model Encoding and Product Sampling

This phase is a **one-time preparation step performed on the local machine before any cluster work**. It produces the configuration files that RQ0 reads on every cluster node to derive the per-product DOT and Event Flow Graph models.

The five small and medium Software Product Lines (Soda Vending Machine, eMail, Elevator, Bank Account, Student Attendance System) are small enough to enumerate exhaustively and therefore require no sampling. The three industrial Software Product Lines (Tesla, syngo.via, Hockerty Shirts) range from approximately 48 thousand to 124 billion valid configurations and must be subsampled. The procedure described in this section applies only to those three.

### 5.1 Sampling tool

Sampling is performed with **UniGen3**, a near-uniform sampler over the satisfying assignments of a Boolean formula in conjunctive normal form. UniGen3 is part of the [meelgroup/unigen](https://github.com/meelgroup/unigen) repository on GitHub; clone or download it and follow that project's build instructions to obtain the `unigen` binary. The original experiments used the binary produced from the official sources on macOS, but any platform with a C++ toolchain is supported.

### 5.2 Per-Software-Product-Line input files

For each of the three industrial Software Product Lines, two configuration files are committed under `files/Cases/<SPL>/configs/`:

1. **`<SPL_short>.dimacs`** — the feature model encoded in DIMACS conjunctive normal form, where every feature becomes a Boolean variable and every cross-tree constraint is rendered as a clause. This is the input to UniGen3.
2. **`<SPL_short>_dimacsmapping.txt`** — a plain-text map from DIMACS variable indices to feature names. UniGen3 outputs samples in DIMACS variable form, and this mapping file lets the downstream RQ0 pipeline reconstruct named feature configurations.

Concrete example for syngo.via:

- `files/Cases/syngovia/configs/Svia.dimacs`
- `files/Cases/syngovia/configs/Svia_dimacsmapping.txt`

### 5.3 Sample sizes and justification

The number of samples drawn for each industrial Software Product Line is fixed at study-design time so that the 95 % confidence interval has a margin of error not exceeding 5 % under simple random sampling assumptions (Cochran 1977; Israel 1992, *Determining Sample Size*, IFAS PEOD-6):

| Software Product Line | Population (valid products) | Samples drawn |
|-----------------------|-----------------------------|---------------|
| syngo.via | 47,970 | 400 |
| Tesla | 426,672 | 400 |
| Hockerty Shirts | 124,201,479,600 | 416 |

The 416-sample size for Hockerty Shirts adds a buffer above the analytical minimum (approximately 385) given the vastly larger population.

### 5.4 Running UniGen3

For each industrial Software Product Line, run UniGen3 once with the `--samples` flag set to the value in §5.3 above and the `--sampleout` flag pointing to a file under `files/Cases/<SPL>/configs/`:

```bash
# syngo.via, 400 samples
./unigen --samples 400 \
         --sampleout /path/to/repo/files/Cases/syngovia/configs/Svia_400.samples \
         /path/to/repo/files/Cases/syngovia/configs/Svia.dimacs

# Tesla, 400 samples
./unigen --samples 400 \
         --sampleout /path/to/repo/files/Cases/Tesla/configs/Te_400.samples \
         /path/to/repo/files/Cases/Tesla/configs/Te.dimacs

# Hockerty Shirts, 416 samples
./unigen --samples 416 \
         --sampleout /path/to/repo/files/Cases/HockertyShirts/configs/HS_416.samples \
         /path/to/repo/files/Cases/HockertyShirts/configs/HS.dimacs
```

UniGen3 writes one product configuration per line to `<SPL_short>_<N>.samples`. The `.samples` files are committed to this repository so that **any later reproduction uses the same products as the original study by default**.

The `.samples` and `_dimacsmapping.txt` files together drive `RQ0_ProductGeneration_Master.sh` on the cluster (see §7.4): RQ0 reads each sampled configuration, applies the mapping to recover named features, and writes one DOT file and one Event Flow Graph file per sampled product into `files/Cases/<SPL>/DOTs/` and `files/Cases/<SPL>/EFGs/`.

### 5.5 Deterministic reproduction versus independent re-sampling

Two reproduction modes are available:

- **Deterministic reproduction** (default): the existing `.samples` files in this repository are reused. The same products are evaluated as in the manuscript, so RQ1, RQ2, and RQ3 produce numerically identical results up to Java Virtual Machine noise and Random Walk seed effects. Reviewers comparing against the manuscript's tables should use this mode.
- **Independent re-sampling**: UniGen3 is invoked again, producing fresh samples from the same configuration space. Results will be statistically equivalent (UniGen3 is near-uniform) but not numerically identical. This mode is useful for arguing that the study's conclusions are not artefacts of one particular sample.

For most reviewers, deterministic reproduction is preferred. Independent re-sampling is a defensible follow-up experiment that strengthens the empirical claims at the cost of additional compute time.

---

## 6. Phase 1 — Cluster Provisioning

This phase is **manual** and was performed through the DigitalOcean web console in the original experiments. There is no Terraform / `doctl` automation in this artefact.

**Step-by-step:**

1. **Register an SSH public key** with the DigitalOcean account (Account → Settings → Security → SSH Keys). Every node will be reached via passwordless SSH as `root` from the local machine; without this key the provisioning script cannot bootstrap any node.

2. **Create 10 droplets manually** through the DigitalOcean web console with the specifications in §2.1. Select the SSH key from step 1 during creation. Use the same datacenter region for all ten nodes; the original experiments used Amsterdam 3 (`ams3`), but any region is valid as long as it is consistent across nodes.

3. **Collect the public IPv4 addresses** of the ten droplets.

4. **Populate `files/bashscripts/digitalocean/ips.txt`** with the IPs, one per line. The file shipped in this repository is a template — replace each `<YOUR_IP_N>` placeholder with the corresponding droplet's IP. The trailing comment after each IP records the shard range the node will execute and **must not be edited**:

```
<YOUR_IP_1>     #s00-07
<YOUR_IP_2>     #s08-15
<YOUR_IP_3>     #s16-23
<YOUR_IP_4>     #s24-31
<YOUR_IP_5>     #s32-39
<YOUR_IP_6>     #s40-47
<YOUR_IP_7>     #s48-55
<YOUR_IP_8>     #s56-63
<YOUR_IP_9>     #s64-71
<YOUR_IP_10>    #s72-79
```

The shard tag encodes the deterministic partitioning of work across nodes (see §7.1).

> **Note on the IPs in the template.** The original experiments used a specific set of public IPs that have since been released back to the DigitalOcean address pool. Re-using those exact addresses is neither possible nor meaningful. The tags `s00-07 … s72-79`, on the other hand, are an integral part of the partitioning scheme and are referenced by the analysis scripts.

---

## 7. Phase 2 — Orchestrated Execution

### 6.1 Sharding scheme

The 80-shard partition `(productID − 1) mod 80 = currentShard` is fixed across all RQs. Each of the ten nodes processes a contiguous block of 8 shards:

| Node | Shards | Node | Shards |
|------|--------|------|--------|
| 1 | 00–07 | 6 | 40–47 |
| 2 | 08–15 | 7 | 48–55 |
| 3 | 16–23 | 8 | 56–63 |
| 4 | 24–31 | 9 | 64–71 |
| 5 | 32–39 | 10 | 72–79 |

This partition guarantees identical workloads across approaches, which is a precondition for the paired Wilcoxon tests in the statistical pipeline (Phase 5).

### 6.2 One command provisions and runs everything

From the local machine, after editing `ips.txt`:

```bash
cd files/bashscripts/digitalocean
bash Setup_And_Run_DigitalOcean.sh
```

For each node, in parallel, the script:

1. Waits for `cloud-init` to finish (avoids `dpkg` lock contention on freshly booted droplets).
2. Installs Java 8, Java 11, Maven, Git, `dos2unix`, and `curl` non-interactively.
3. Configures Java alternatives — Java 11 is the default `java`/`javac`; Java 8 remains reachable via the `$JAVA_8_EXE` environment variable for the GUITAR-based Structural Baseline.
4. Clones this repository to `/root/esg-with-feature-expressions`.
5. Launches `Global_Orchestrator.sh START_SHARD END_SHARD` under `nohup` so the experiments survive disconnects.

Per-node provisioning logs are written to `files/logs/Setup_Logs/setup_node_<i>_<ip>.log` on the local machine.

### 6.3 What `Global_Orchestrator.sh` does on each node

The orchestrator runs the five experimental phases sequentially against the node's assigned shard range:

| Order | Master script | Output type |
|-------|---------------|-------------|
| 1 | `RQ0_ProductGeneration_Master.sh` | DOT, Event Flow Graph, per-product enumeration CSV files |
| 2 | `RQ1_ComparativeEfficiency_Master.sh` | per-product and per-shard timing CSV files |
| 3 | `RQ3_FaultDetection_Master.sh` | per-product mutation CSV files |
| 4 | `RQ3_RandomWalk_DampingSensivityAnalysis.sh` | damping × seed sensitivity CSV files |
| 5 | `RQ2_ExtremeScalability_Master.sh` | per-shard scalability CSV files |

The order is significant. RQ0 produces the DOT and Event Flow Graph models that the other Research Questions consume. RQ3 reuses the test suites generated by RQ1 without regenerating them, so RQ1 must finish before RQ3 starts. RQ2 is the most expensive phase (it operates on the full configuration space of every Software Product Line) and is therefore scheduled last.

### 6.4 What each Master script does

Every Master script, on each node, compiles the Java project once (`mvn clean package dependency:copy-dependencies -DskipTests`) and then iterates over the eight Software Product Lines.

**`RQ0_ProductGeneration_Master.sh` — Model and Product Enumeration.**
Generates the variability-aware Event Sequence Graph with Feature Expressions (ESG-Fx) and derives the per-product DOT and Event Flow Graph (EFG) representations consumed by every downstream Research Question. Runs once because the procedure is deterministic. For small and medium Software Product Lines the script enumerates every valid product; for the three industrial Software Product Lines (Tesla, syngo.via, Hockerty Shirts) it reads the UniGen3 sample files prepared in §5 (`<SPL>_<N>.samples` and the corresponding `<SPL>_dimacsmapping.txt`) and emits one DOT and one Event Flow Graph file per sampled product.

**`RQ1_ComparativeEfficiency_Master.sh` — RQ1 (Efficiency).**
Measures test generation time, test execution time, and peak generation and execution memory of the Model Once, Generate Any approach at L = 1, 2, 3, 4, the Structural Baseline (Event Flow Graph via GUITAR) at L = 2, 3, 4, and the Stochastic Baseline (Random Walk) at L = 0. Repeats every measurement **11 times** per product per approach per coverage level. The odd repetition count yields a clean median without interpolation, and the resulting power lets the design detect effect sizes ≥ 1.5 × standard deviation at α = 0.05 and β = 0.20. Random Walk seeds follow the per-product strategy `seed = 42 + productID`.

**`RQ3_FaultDetection_Master.sh` — RQ3 (Fault Detection, primary study).**
Runs in two phases on each node. **Phase 1** generates ten-seed Random Walk test suites used as the Stochastic Baseline reference. **Phase 2** runs Edge Omission and Event Omission fault detection against the test suites already produced by RQ1 (no regeneration). Edge and Event Omission runs are deterministic given a fixed test suite and therefore execute once per product.

**`RQ3_RandomWalk_DampingSensivityAnalysis.sh` — RQ3 (sensitivity sub-study).**
Three phases. **Phase 1** generates Random Walk test suites for damping factors {0.80, 0.85, 0.90} crossed with ten seeds {42 .. 51}. **Phases 2 and 3** run Edge Omission and Event Omission fault detection against those suites, producing the sensitivity CSV files in `Cases/<SPL>/faultdetection/sensitivity/`. The sub-study is justified at the design level — see the manuscript's RQ3 design rationale.

**`RQ2_ExtremeScalability_Master.sh` — RQ2 (Scalability).**
Operates on the full configuration space of every Software Product Line for which exhaustive enumeration is tractable; on the three industrial Software Product Lines it operates on the same UniGen3 samples introduced in RQ0. The repetition count is **11** for small and medium Software Product Lines and **3** for the industrial ones (Tesla, syngo.via, Hockerty Shirts), where throughput rather than variance is the measurement target. The Random Walk seed is fixed at `42` for this Research Question.

### 6.5 Local-only mode (smoke test)

Each Master script auto-detects macOS (`OSTYPE = darwin*`) and falls back to `TARGET_SHARDS=4` for local execution. To verify the pipeline end-to-end on a laptop before going to the cluster:

```bash
cd files/bashscripts
bash Global_Orchestrator.sh 0 3
```

This runs every RQ on a 4-shard partition. The same CSV files are produced; only the volume differs.

---

## 8. Phase 3 — Live Monitoring

While the cluster runs, two scripts give live status from the local machine:

```bash
# Per-node compact dashboard:
#   Java process count, RAM usage, CPU load,
#   currently running task, current run number out of total.
bash files/bashscripts/digitalocean/Cluster_Monitor.sh

# Scan every node's logs for Exception, CRITICAL, OutOfMemoryError.
bash files/bashscripts/digitalocean/Cluster_Error_Scanner.sh
```

`Cluster_Monitor.sh` is safe to run repeatedly — each invocation is a one-shot snapshot. Pairing it with `watch -n 30 ./Cluster_Monitor.sh` produces a refreshing dashboard. `Cluster_Error_Scanner.sh` is intended to be run on demand when a long-running phase looks suspicious; it greps every `.log` file in the cluster's `files/logs/` tree and reports the last five lines of any file that matches.

---

## 9. Phase 4 — Result Collection

When all nodes have finished, run the collection scripts from the local machine. Each one downloads the CSV outputs of one RQ from every node, recreating the cluster's directory structure inside the local `files/Cases/` tree:

```bash
cd files/bashscripts/digitalocean

bash Collect_Results_RQ1.sh        # comparative efficiency CSVs (and per-product EFG/DOT logs)
bash Collect_Results_RQ2.sh        # extreme scalability CSVs
bash Collect_Results_RQ3.sh        # fault detection CSVs (perProduct + sensitivity)

# Two special cases:
bash Collect_DOT_EFG_CSVs.sh       # DOT and EFG metadata CSVs are tagged with the source IP
                                   # because each node generates its own subset; tagging
                                   # prevents file-level overwrite.

bash Cluster_Log_Collector.sh      # Pulls per-node experiment logs into files/logs/cluster/
```

`Collect_Results_RQ3.sh` performs an automatic completeness check: each Software Product Line is expected to yield 56 CSV files per node (4 per-product mutation types × 8 shards + 3 sensitivity types × 8 shards), for a total of 560 CSV files across all 10 nodes per Software Product Line. Missing shard numbers are listed at the end of the script's output, which makes targeted re-runs straightforward.

After this phase the local repository contains every artefact reported in the manuscript's Tables and Figures — at the raw, per-shard granularity.

---

## 10. Phase 5 — Aggregation and Statistical Analysis

All analysis runs locally. Scripts are numbered by execution order: a script with the prefix `_01_` runs before `_02_`, `_03_`, and so on, within each RQ.

### 9.1 Aggregation pipeline (`files/scripts/`)

These scripts merge per-shard CSV files into per-Software-Product-Line tables consumed by the statistical tests. The number prefix in each filename indicates its execution order within its Research Question (`rq1_01` runs first within RQ1, `rq1_02` runs second, and so on).

| Script | Purpose |
|--------|---------|
| `rq1_01_aggregate_shards_perRun.py` | Combines 80 shards into per-run, per-product CSV files |
| `rq1_02_aggregate_shards_perProduct.py` | Reduces the 11 runs to per-product median plus interquartile range |
| `rq2_00_data_integrity_check.py` | Verifies expected file counts and shard coverage |
| `rq2_01_merge_shards_per_spl.py` | Merges 80 shards per Software Product Line |
| `rq2_02_median_across_runs.py` | Reduces 11 runs (or 3 for the industrial Software Product Lines) to a per-shard median |
| `rq2_03_aggregate_across_shards.py` | Sums per-shard cumulative time into Software-Product-Line-level totals |
| `rq3_01_merge_shards_per_spl.py` | Merges per-product mutation CSV files across shards |
| `rq3_02_aggregate_across_spls.py` | Combines all Software Product Lines into a single analysis frame |
| `rq3_equivalent_mutants/rq3_03_analyze_equivalent_mutants.py` | Detects and tags equivalent mutants for the mutation-score correction |

`analyse_featuremodel.py` is a stand-alone diagnostic tool used to sanity-check the eight feature models; it is not part of the main reproduction path.

### 9.2 Statistical tests (`files/scripts/statistical_test_scripts/`)

| Script | Manuscript artefact |
|--------|---------------------|
| `rq1_03_per_product_scatter_spearman.py` | Spearman ρ between per-product edge count and median test generation time |
| `rq1_04_scaling_regression.py` | Linear scaling regression (per product and per Software Product Line) |
| `rq1_05_pairwise_wilcoxon.py` | Paired Wilcoxon signed-rank test comparing the Model Once, Generate Any approach against the Structural Baseline and the Stochastic Baseline, with Vargha–Delaney A₁₂ effect sizes and Benjamini–Hochberg False Discovery Rate correction |
| `rq1_06_run_stability.py` | Coefficient of variation and relative interquartile range over the 11 runs |
| `rq1_07_mechanism_and_warmup.py` | Java Virtual Machine warm-up and mechanism diagnostics |
| `rq2_04_pairwise_wilcoxon_shards.py` | Paired Wilcoxon signed-rank test over the 80 shards per Software Product Line |
| `rq2_06_time_breakdown.py` | Decomposition of total elapsed time into Boolean satisfiability, product generation, transformation, test generation, and coverage components |
| `rq2_07_run_stability_industrial.py` | Run stability for the industrial Software Product Lines (defence of the 3-run choice) |
| `rq3_04_pairwise_wilcoxon.py` | Pairwise Mann–Whitney U test on Mutation Score and Detection Cost |
| `rq3_05_effectsize_bh.py` | Vargha–Delaney A₁₂ effect sizes plus Benjamini–Hochberg False Discovery Rate correction across the 9 pairings × 8 Software Product Lines grid |
| `rq3_06_paired_edge_vs_event.py` | Empirical validation of the Edge Omission ⊇ Event Omission subsumption claim |
| `rq3_07_damping_sensitivity.py` | Damping {0.80, 0.85, 0.90} × ten seeds sensitivity analysis |
| `rq3_08_multiseed_stability.py` | Inter-seed interquartile range for the Stochastic Baseline |
| `rq3_09_make_tables.py` | Generation of the final manuscript tables |

Outputs (figures, LaTeX tables, CSV summaries) land in `rq1_result/`, `rq2_result/`, and `rq3_result/` next to the scripts.

### 9.3 End-to-end analysis run

```bash
cd files/scripts

# RQ1
python rq1_01_aggregate_shards_perRun.py
python rq1_02_aggregate_shards_perProduct.py
python statistical_test_scripts/rq1_03_per_product_scatter_spearman.py
python statistical_test_scripts/rq1_04_scaling_regression.py
python statistical_test_scripts/rq1_05_pairwise_wilcoxon.py
python statistical_test_scripts/rq1_06_run_stability.py
python statistical_test_scripts/rq1_07_mechanism_and_warmup.py

# RQ2
python rq2_00_data_integrity_check.py
python rq2_01_merge_shards_per_spl.py
python rq2_02_median_across_runs.py
python rq2_03_aggregate_across_shards.py
python statistical_test_scripts/rq2_04_pairwise_wilcoxon_shards.py
python statistical_test_scripts/rq2_06_time_breakdown.py
python statistical_test_scripts/rq2_07_run_stability_industrial.py

# RQ3
python rq3_01_merge_shards_per_spl.py
python rq3_02_aggregate_across_spls.py
python rq3_equivalent_mutants/rq3_03_analyze_equivalent_mutants.py
python statistical_test_scripts/rq3_04_pairwise_wilcoxon.py
python statistical_test_scripts/rq3_05_effectsize_bh.py
python statistical_test_scripts/rq3_06_paired_edge_vs_event.py
python statistical_test_scripts/rq3_07_damping_sensitivity.py
python statistical_test_scripts/rq3_08_multiseed_stability.py
python statistical_test_scripts/rq3_09_make_tables.py
```

---

## 11. Wall-clock and Cost Estimate

The original experiments produced the manuscript results across two billing periods (March and April 2026). The DigitalOcean list price for the s-8vcpu-16gb plan in Amsterdam 3 is **96.00 USD per month, equivalently 0.143 USD per hour**, which agrees with the per-droplet rate observed in the invoices ($18.82 / 131 hours = $0.1437 per hour). Total expenditure on the 10-node cluster was:

| Period | Droplet-hours | Subtotal | Belgian Value-Added Tax (21 %) | Total |
|--------|---------------|----------|--------------------------------|-------|
| March 2026 | ~1,310 | $188.20 | $39.52 | $227.72 |
| April 2026 | ~3,840 | $551.97 | $115.91 | $667.88 |
| **Total** | **~5,150** | **$740.17** | **$155.43** | **$895.60** |

The April invoice covered approximately **three iterations** of the full pipeline because of intermediate bug fixes and partial re-runs. A **single clean reproduction**, based on the longest uninterrupted run in the original experiments (10 nodes, 244 hours each, 11 April – 22 April 2026), is estimated at:

- **Approximately 10 days of wall-clock time** with all 10 nodes occupied
- **Approximately 2,440 droplet-hours** in total (244 hours per node × 10 nodes)
- **Approximately 349 USD before tax** at the 0.143 USD/hour rate

A reasonable budget envelope for a careful single reproduction with safety margin is therefore **7 to 10 days of wall-clock time and 250 to 400 USD before any country-specific Value-Added Tax**.

---

## 12. Data Archive

This repository commits files in three categories:

1. **Code** — Java sources, bash scripts, and Python analysis scripts; all version-controlled normally.
2. **Inputs** — feature-model encodings, DIMACS files, mapping files, and the UniGen3 sample files used by the original experiments. Committed under `files/Cases/<SPL>/configs/`. These are required for any reproduction (see §5) and are deliberately kept in version control.
3. **Aggregated outputs** — manuscript-ready tables, figures, and Excel summaries produced by Phase 5. Committed under `files/scripts/statistical_test_scripts/rq{1,2,3}_result/`.

The **raw experimental data** that the cluster produces — every per-product DOT and Event Flow Graph file, and every per-shard measurement CSV file from RQ1, RQ2, and RQ3, for **all eight Software Product Lines** — is archived together on Zenodo at <https://doi.org/10.5281/zenodo.20027555> rather than committed to this repository. Per-product test sequences (the textual `.txt` and `.tst` files produced by ESG-Fx, Random Walk, and GUITAR) are intentionally **not** included in the archive: the manuscript's quantitative claims rely on the aggregated CSV measurements, and the test sequences themselves are regenerable by re-running RQ1 on the cluster. The archive comes as a single tarball that mirrors the cluster's directory layout under `files/Cases/`.

To make the raw data available locally, download the Zenodo archive and extract it on top of the repository's `files/Cases/` directory:

```bash
# Download from Zenodo
curl -L -o esg-fx-raw-data.tar.gz \
     https://zenodo.org/records/20027555/files/esg-fx-raw-data.tar.gz

# Extract under files/Cases/, preserving the original directory structure
cd /path/to/repo/files/Cases
tar -xzf /path/to/esg-fx-raw-data.tar.gz
```

After extraction, the repository's `files/Cases/<SPL>/` trees contain the same DOT files, Event Flow Graphs, test sequences, and measurement CSV files the cluster produced. The Phase 5 analysis pipeline (§10) then runs unchanged.

**If the goal is to re-execute the experiments rather than re-analyse existing data, this archive is not needed** — Phase 1 through Phase 4 regenerate every output file from scratch, using only the inputs in `files/Cases/<SPL>/configs/`.

---

## 13. Re-running on Different Infrastructure

The pipeline is not hard-coded to DigitalOcean. To run on another cloud provider or an on-premise cluster:

1. Provision *N* nodes that meet §2.1 (Ubuntu 22.04 LTS, ≥ 8 vCPUs, ≥ 16 GB RAM, passwordless SSH as root from the local machine).
2. Place their IPs in `ips.txt`.
3. Adjust the `SHARDS_PER_NODE` constant inside `Setup_And_Run_DigitalOcean.sh` so that `N × SHARDS_PER_NODE = 80`. Examples: 5 nodes × 16 shards/node, 20 nodes × 4 shards/node. The total of 80 shards is referenced in several scripts and analysis aggregations and **should not be changed**.
4. Run `Setup_And_Run_DigitalOcean.sh` exactly as in §7.2.

For a complete laptop-only reproduction at reduced scale, see §7.5.

---

## 14. Troubleshooting

**Symptom:** `Setup_And_Run_DigitalOcean.sh` reports `dpkg: locked` on a fresh node.
**Cause:** `cloud-init` is still finishing on the freshly booted droplet.
**Fix:** the script already waits via `cloud-init status --wait`; if a node still fails, simply re-run the script — already-installed packages are skipped.

**Symptom:** `Cluster_Error_Scanner.sh` reports `OutOfMemoryError` on Tesla or Hockerty Shirts during RQ2.
**Cause:** Java heap limit insufficient for the largest configuration spaces.
**Fix:** raise the `XMX` value inside the affected RQ Master script. The default for Linux nodes is `1500m`; values up to `12g` have been used without triggering swap on the 16 GB droplet.

**Symptom:** A subset of shards is missing in the completeness check at the end of `Collect_Results_RQ3.sh`.
**Cause:** a partial node failure during RQ3 (rare).
**Fix:** SSH into the affected node and re-run only the missing shard range, e.g. `bash RQ3_FaultDetection_Master.sh 24 31` from inside `/root/esg-with-feature-expressions/files/bashscripts`. Then re-run `Collect_Results_RQ3.sh` locally — already-collected CSV files are not re-downloaded.

**Symptom:** `mvn clean package` fails on a node with `Could not resolve dependencies`.
**Cause:** the node has not finished installing Maven, or Maven Central is temporarily unreachable.
**Fix:** SSH into the node, `cd /root/esg-with-feature-expressions`, and re-run `mvn clean package dependency:copy-dependencies -DskipTests` manually.

**Symptom:** `Cluster_Monitor.sh` shows a node as `UNREACHABLE`.
**Cause:** transient network issue, DigitalOcean maintenance, or the node ran out of disk.
**Fix:** SSH into the node directly. If the disk is full (Hockerty Shirts and syngo.via in particular generate large per-product output), free space under `Cases/<SPL>/testsequences/` and rerun the affected master.

---

## 15. Acknowledgements

The DigitalOcean cluster used to produce the experimental results in this artefact was funded by **Prof. Dr. Serge Demeyer** (AnSyMo/LoRe research group, Department of Computer Science, University of Antwerp), joint PhD supervisor of D. Ö. Kaya.

---

## 16. Citation

If you use this artefact, please cite the accompanying manuscript (forthcoming). For the precursor work introducing the ESG-Fx model itself, please cite:

> D. Ö. Kaya, T. Tuglular and F. Belli, "Software Product Line Testing based on Event Sequence Graphs with Feature Expressions," *2023 8th International Conference on Computer Science and Engineering (UBMK)*, Burdur, Turkiye, 2023, pp. 175–180, doi: [10.1109/UBMK59864.2023.10286660](https://doi.org/10.1109/UBMK59864.2023.10286660).
