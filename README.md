# esg-with-feature-expressions

## Project Purpose
Event Sequence Graphs with Feature Expressions (ESG-Fx) is a variability-aware testing approach that represents the behavior of an entire Software Product Line (SPL) in a single model. This project automatically generates executable test sequences for different product configurations, addressing the scalability challenges of model-based SPL testing.

## Prerequisites
To build and run this project, you need the following tools installed on your system:
* **Java 11:** Required to run the main ESG-Fx framework.
* **Java 8:** Strictly required for the structural baseline tests generated via the GUITAR framework.
* **Maven 3.x:** For building the project and managing dependencies.
* **Python 3.9+:** Required for statistical analysis and time measurement scripts (developed using Anaconda Python 3.9.13 with `pandas` and `matplotlib`).

## How to use esg-with-feature-expressions in Eclipse IDE

Clone esg-with-feature-expressions project. 
Open Eclipse IDE. 
Follow File -> Import -> Maven -> Existing Maven Projects and select the cloned project. 

## Publications & Reproducibility

This repository contains materials and reproducibility packages for multiple studies.

### 1. Variability-Aware Event Sequence Graphs for Scalable Software Product Line Testing 
This study presents the "Model Once, Generate Any" approach and evaluates it on 8 SPLs at an industrial scale using a distributed infrastructure.
* **[Reproducibility Guide](REPRODUCIBILITY.md):** Step-by-step instructions for setting up the 80-core DigitalOcean cluster, compiling the project, running the shard-based generation scripts, and executing the Python evaluation scripts.

### 2. Software Product Line Testing based on Event Sequence Graphs with Feature Expressions (UBMK 2023)
Our previous work introduced the foundational ESG-Fx models and evaluated them on preliminary SPLs.
* **[Experiment Results](files/SoftwareProductLineTestingbasedonEventSequenceGraphswithFeatureExpressions/ExperimentResults.md):** Detailed tables, edge coverage metrics, and feature models used in this specific paper.
* **Citation:** > D. Ö. Kaya, T. Tuglular and F. Belli, "Software Product Line Testing based on Event Sequence Graphs with Feature Expressions," *2023 8th International Conference on Computer Science and Engineering (UBMK)*, Burdur, Turkiye, 2023, pp. 175-180, doi: [10.1109/UBMK59864.2023.10286660](https://doi.org/10.1109/UBMK59864.2023.10286660).
