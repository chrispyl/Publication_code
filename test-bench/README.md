# test-bench

The test-bench for our methods.  
The methods included are serial, across the method, across the system and mixed.  
The results are saved to a file chosen by the user.  
During the tests, a file named *report.txt* is created which contains the test parameters being tested at that moment.

[Usage](#Usage)  
[Arguments](#Arguments)  
[Example](#Example)  


## Usage <a name="Usage"></a>

Go to the project folder in the command line and type 

    lein uberjar


A folder named **target** will be created. Inside the folder their is a file named **test-bench-0.1.0-standalone.jar**. To execute it, type

    java -jar test-bench-0.1.0-standalone.jar arg1 arg2 ...

## Arguments <a name="Arguments"></a>

An execution using the jar looks like this

    java -jar test-bench-0.1.0-standalone.jar File-name Core-array Core-array-for-mixed Team-array Equation-array Max-equation-size-array Iterations-array Seed WeightLow WeightHigh Initial-value-low Initial-value-high Double-precision Linear?

The arguments in the order they are taken are explained below

**Argument** | **Description** | **Type**
--- | --- | ---
File-name | The file name where the results should be saved | String
Core-array | The cores for which to run the test-bench | Integers
Core-array-for-mixed | The second layer of cores the mixed method uses | Integers
Team-array | The teams for which to run the test-bench | Integers
Equation-array | The equations for which to run the test-bench | Integers
Max-equation-size-array | The max-equation-sizes for which to run the test-bench | Integers
Iterations-array | The iterations for which to run the test-bench | Integers
Seed | The seed for the random generator | Integer
WeightLow | Minimum value of coefficients | Double
WeightHigh | Maximum value of coefficients | Double
Initial-value-low | Minimum initial value of equations | Double
Initial-value-high | Maximum initial value of equations | Double
Double-precision | Decimal digits for coefficients | Integer
Linear? | Linear or not | Boolean

## Example <a name="Example"></a>

The arguments with *array* in their name are meant to be multiple and seperated by commas like below

    java -jar test-bench-0.1.0-standalone.jar results.txt 5,10,15 2,4 10,20 100,1000,10000 5,6,7,8 50,100,1000 999 -5 5 0 10 2 false
