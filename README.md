# esg-with-feature-expressions

## How to use esg-with-feature-expressions in Eclipse IDE

Clone esg-with-feature-expressions project. 
Open Eclipse IDE. 
Follow File -> Import -> Maven -> Existing Maven Projects and select the cloned project. 

## Experiment Results

# Soda Vending Machine SPL

The feature model of the Soda Vending Machine SPL is given below. There are 4 features and 12 product configurations.  The vending machine, in its simplest form, accepts a coin, provides change, serves beverages, and eventually unlocks a compartment for the customer to retrieve their soda, after which it closes the compartment once more. 

![Feature Model_SVM](SPL_Figures/SVM_FeatureModel.png)

The ESG-Fx model of the Soda Vending Machine SPL is given below. There are 15 vertices and 21 edges in this model. 

![ESGFx_SVM](SPL_Figures/SVM_ESGFx.png)

Each of the  product configurations' test sequence generation times (in milliseconds) are given below. 

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

# e-Mail SPL

The feature model of the e-Mail SPL is given below. There are 5 features.  e-Mail SPL is a product line of email clients that offers the feature_ _addressbook_ for creating an addressbook of email contacts, _autoresponder_ for autoresponding incoming emails between a specified date interval, _forward_ for forwarding incoming emails, _encrypt_ for encrypting outgoing emails and, _sign_ or signing outgoing emails. A basic e-Mail client provides events for composing a new email and for reading an incoming email.

![Feature Model_eM](SPL_Figures/eM_FeatureModel.png)

The ESG-Fx model of the e-Mail SPL is given below. There are 19 vertices and 35 edges in this model. 

![ESGFx_eM](SPL_Figures/eM_ESGFx.png)

The product configurations of the e-Mail SPL are given in the table below. 

| Product ID | Features                                  |
| ---------- | ----------------------------------------- |
| 0          | -                                        |
| 1          | addressbook                               |
| 2          | autoresponder                             |
| 4          | forward                                   |
| 8          | encrypt                                   |
| 12         | sign                                      |
| 3          | addressbook, autoresponder                |
| 5          | addressbook, forward                      |
| 6          | autoresponder, forward                    |
| 9          | addressbook, encrypt                      |
| 10         | autoresponder, encrypt                    |
| 13         | addressbook, sign                         |
| 14         | autoresponder, sign                       |
| 16         | forward, sign                             |
| 20         | encrypt, sign                             |
| 7          | addressbook, autoresponder, forward       |
| 11         | addressbook, autoresponder, encrypt       |
| 15         | addressbook, autoresponder, sign          |
| 17         | addressbook, forward, sign                |
| 18         | autoresponder, forward, sign              |
| 21         | addressbook, encrypt, sign                |
| 22         | autoresponder, encrypt, sign              |
| 19         | addressbook, autoresponder, forward, sign |
| 23         | addressbook, autoresponder, encrypt, sign |

The randomly selected 10 product configurations' test sequence generation times (in milliseconds) are given below. 

| eM  | addressbook | autoresponder | forward | encrypt | sign  | Number of Features | Test Sequence Generation Time (ms) |
| --- | ----------- | ------------- | ------- | ------- | ----- | ------------------ | ---------------------------------- |
| P1  | TRUE        | FALSE         | FALSE   | FALSE   | FALSE | 1                  | 21,80                              |
| P2  | FALSE       | TRUE          | FALSE   | FALSE   | FALSE | 1                  | 20,85                              |
| P3  | FALSE       | FALSE         | TRUE    | FALSE   | FALSE | 1                  | 20,42                              |
| P4  | FALSE       | FALSE         | FALSE   | TRUE    | FALSE | 1                  | 20,06                              |
| P5  | FALSE       | FALSE         | FALSE   | FALSE   | TRUE  | 1                  | 20,50                              |
| P6  | TRUE        | FALSE         | FALSE   | TRUE    | FALSE | 2                  | 20,33                              |
| P7  | TRUE        | TRUE          | TRUE    | FALSE   | FALSE | 3                  | 20,61                              |
| P8  | FALSE       | TRUE          | FALSE   | TRUE    | TRUE  | 3                  | 20,32                              |
| P9  | TRUE        | TRUE          | TRUE    | FALSE   | TRUE  | 4                  | 20,02                              |
| P10 | TRUE        | TRUE          | FALSE   | TRUE    | TRUE  | 4                  | 20,98                              |
|     |             |               |         |         |       |                    | 20,59                              |

# Bank Account SPL

The feature model of the Bank Account SPL is given below. There are 9 features.  Bank Account SPL is a product line of banking software that offers the_ _deposit_, _withdraw_, _cancelDeposit_ and, _cancelWithdraw_ features for money deposit, withdrawal, and cancellation of the deposit and withdrawal operations, respectively; the _overdraft_ feature for negative balance permission; the _credit_ feature for assessment of a customer credit request of a certain amount; the _interest_ and _interestEstimation_ features for calculation of the expected interest; the _dailyLimit_ feature for limitation of daily withdrawal. A basic Bank Account product provides the current balance.

![Feature Model_BA](SPL_Figures/BA_FeatureModel.png)

The product configurations of the Bank Account SPL are given in the table below. 

| Product ID | Features                                                                                              |
| ---------- | ----------------------------------------------------------------------------------------------------- |
| 0          | deposit, withdraw                                                                                     |
| 1          | deposit, withdraw, interest                                                                           |
| 3          | deposit, withdraw, cancelDeposit                                                                      |
| 6          | deposit, withdraw, cancelWithdraw                                                                     |
| 24         | deposit, withdraw, credit                                                                             |
| 2          | deposit, withdraw, interest, interestEstimation                                                       |
| 4          | deposit, withdraw, cancelDeposit, interest                                                            |
| 7          | deposit, withdraw, interest, cancelWithdraw                                                           |
| 9          | deposit, withdraw, cancelWithdraw, dailyLimit                                                         |
| 12         | deposit, withdraw, cancelDeposit, cancelWithdraw                                                      |
| 25         | deposit, withdraw, credit, interest                                                                   |
| 27         | deposit, withdraw, cancelDeposit, credit                                                              |
| 30         | deposit, withdraw, credit, cancelWithdraw                                                             |
| 5          | deposit, withdraw, cancelDeposit, interest, interestEstimation                                        |
| 8          | deposit, withdraw, interest, cancelWithdraw, interestEstimation                                       |
| 10         | deposit, withdraw, interest, cancelWithdraw, dailyLimit                                               |
| 13         | deposit, withdraw, cancelDeposit, interest, cancelWithdraw                                            |
| 15         | deposit, withdraw, cancelDeposit, cancelWithdraw, dailyLimit                                          |
| 18         | deposit, withdraw, cancelWithdraw, dailyLimit, overdraft                                              |
| 26         | deposit, withdraw, credit, interest, interestEstimation                                               |
| 28         | deposit, withdraw, cancelDeposit, credit, interest                                                    |
| 31         | deposit, withdraw, credit, interest, cancelWithdraw                                                   |
| 33         | deposit, withdraw, credit, cancelWithdraw, dailyLimit                                                 |
| 36         | deposit, withdraw, cancelDeposit, credit, cancelWithdraw                                              |
| 11         | deposit, withdraw, interest, cancelWithdraw, dailyLimit, interestEstimation                           |
| 14         | deposit, withdraw, cancelDeposit, interest, cancelWithdraw, interestEstimation                        |
| 16         | deposit, withdraw, cancelDeposit, interest, cancelWithdraw, dailyLimit                                |
| 19         | deposit, withdraw, interest, cancelWithdraw, dailyLimit, overdraft                                    |
| 21         | deposit, withdraw, cancelDeposit, cancelWithdraw, dailyLimit, overdraft                               |
| 29         | deposit, withdraw, cancelDeposit, credit, interest, interestEstimation                                |
| 32         | deposit, withdraw, credit, interest, cancelWithdraw, interestEstimation                               |
| 34         | deposit, withdraw, credit, interest, cancelWithdraw, dailyLimit                                       |
| 37         | deposit, withdraw, cancelDeposit, credit, interest, cancelWithdraw                                    |
| 39         | deposit, withdraw, cancelDeposit, credit, cancelWithdraw, dailyLimit                                  |
| 17         | deposit, withdraw, cancelDeposit, interest, cancelWithdraw, dailyLimit, interestEstimation            |
| 20         | deposit, withdraw, interest, cancelWithdraw, dailyLimit, interestEstimation, overdraft                |
| 22         | deposit, withdraw, cancelDeposit, interest, cancelWithdraw, dailyLimit, overdraft                     |
| 35         | deposit, withdraw, credit, interest, cancelWithdraw, dailyLimit, interestEstimation                   |
| 38         | deposit, withdraw, cancelDeposit, credit, interest, cancelWithdraw, interestEstimation                |
| 40         | deposit, withdraw, cancelDeposit, credit, interest, cancelWithdraw, dailyLimit                        |
| 23         | deposit, withdraw, cancelDeposit, interest, cancelWithdraw, dailyLimit, interestEstimation, overdraft |
| 41         | deposit, withdraw, cancelDeposit, credit, interest, cancelWithdraw, dailyLimit, interestEstimation    |

The ESG-Fx model of the Bank Account SPL is given below. There are 23 vertices and 42 edges in this model. 

![ESGFx_BA](SPL_Figures/BA_ESGFx.png)

The randomly selected 10 product configurations' test sequence generation times (in milliseconds) are given below. 

| Products | deposit | withdraw | cancelDeposit | cancelWithdraw | overdraft | credit | interest | interestEstimation | dailyLimit | Number of Features | Test Sequence Generation Time (ms) |
| -------- | ------- | -------- | ------------- | -------------- | --------- | ------ | -------- | ------------------ | ---------- | ------------------ | ---------------------------------- |
| P1       | TRUE    | TRUE     | FALSE         | FALSE          | FALSE     | FALSE  | TRUE     | FALSE              | FALSE      | 3                  | 23,59                              |
| P2       | TRUE    | TRUE     | FALSE         | TRUE           | FALSE     | FALSE  | FALSE    | FALSE              | TRUE       | 4                  | 22,69                              |
| P3       | TRUE    | TRUE     | FALSE         | FALSE          | FALSE     | TRUE   | TRUE     | FALSE              | FALSE      | 4                  | 23,44                              |
| P4       | TRUE    | TRUE     | FALSE         | FALSE          | FALSE     | TRUE   | TRUE     | TRUE               | FALSE      | 5                  | 21,45                              |
| P5       | TRUE    | TRUE     | TRUE          | TRUE           | FALSE     | TRUE   | FALSE    | FALSE              | FALSE      | 5                  | 22,22                              |
| P6       | TRUE    | TRUE     | TRUE          | TRUE           | FALSE     | FALSE  | TRUE     | TRUE               | FALSE      | 6                  | 21,91                              |
| P7       | TRUE    | TRUE     | FALSE         | TRUE           | TRUE      | FALSE  | TRUE     | FALSE              | TRUE       | 6                  | 22,59                              |
| P8       | TRUE    | TRUE     | TRUE          | TRUE           | FALSE     | FALSE  | TRUE     | TRUE               | TRUE       | 7                  | 22,66                              |
| P9       | TRUE    | TRUE     | TRUE          | TRUE           | TRUE      | FALSE  | TRUE     | FALSE              | TRUE       | 7                  | 22,90                              |
| P10      | TRUE    | TRUE     | TRUE          | TRUE           | FALSE     | TRUE   | TRUE     | TRUE               | TRUE       | 8                  | 21,87                              |
|          |         |          |               |                |           |        |          |                    |            |                    | **22,53**                              |
