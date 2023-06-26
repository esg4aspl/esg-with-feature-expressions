# esg-with-feature-expressions

## How to use esg-with-feature-expressions in Eclipse IDE

Clone esg-with-feature-expressions project. 
Open Eclipse IDE. 
Follow File -> Import -> Maven -> Existing Maven Projects and select the cloned project. 

## Experiment Results

# Soda Vending Machine SPL

The feature model of the Soda Vending Machine SPL is given below. There are 4 features and 12 product configurations.  The vending machine, in its simplest form, accepts a coin, provides change, serves beverages, and eventually unlocks a compartment for the customer to retrieve their soda, after which it closes the compartment once more. 

![Feature Model_SVM](SPL_Figures/SVM_FeatureModel.png)

The ESG-Fx model of Soda Vending Machine SPL is given below. There are 15 vertices and 21 edges in this model. 


![ESGFx_SVM](SPL_Figures/SVM_ESGFx.png)

The product configurations' test sequence generation times (in milliseconds) are given below. 

| SVM | Soda  | Tea   | FreeDrinks | CancelPurchase | Number of Features | Test Sequence Generation Time (ms) |
| --- | ----- | ----- | ---------- | -------------- | ------------------ | ---------------------------------- |
| P1  | TRUE  | FALSE | FALSE      | FALSE          | 1                  | 15,12                              |
| P2  | FALSE | TRUE  | FALSE      | FALSE          | 1                  | 14,29                              |
| P3  | TRUE  | TRUE  | FALSE      | FALSE          | 2                  | 14,46                              |
| P4  | TRUE  | FALSE | TRUE       | FALSE          | 2                  | 14,30                              |
| P5  | FALSE | TRUE  | TRUE       | FALSE          | 2                  | 14,63                              |
| P6  | TRUE  | FALSE | FALSE      | TRUE           | 2                  | 14,71                              |
| P7  | FALSE | TRUE  | FALSE      | TRUE           | 2                  | 14,70                              |
| P8  | TRUE  | TRUE  | FALSE      | TRUE           | 3                  | 14,53                              |
| P9  | TRUE  | FALSE | TRUE       | TRUE           | 3                  | 14,40                              |
| P10 | FALSE | TRUE  | TRUE       | TRUE           | 3                  | 14,41                              |
| P11 | TRUE  | TRUE  | TRUE       | FALSE          | 3                  | 14,58                              |
| P12 | TRUE  | TRUE  | TRUE       | TRUE           | 4                  | 14,73                              |
|     |       |       |            |                |                    | **14,57**                              |
