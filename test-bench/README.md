# test-bench

The test-bench for our methods.  
The methods included are serial, across the method, across the system and mixed.  
The results are saved to a file chosen by the user.  
During the tests, a file named *report.txt* is created which contains the test parameters being tested at that moment.

[Problems](#Problems)  
[Usage](#Usage)  
[Arguments](#Arguments)  
[Example](#Example)  
[Benchmarking once](#Benchmarking_once)

## Problems <a name="Problems"></a>

For higher amounts of equations and iterations we get

    java.lang.OutOfMemoryError: GC overhead limit exceeded
    
The reason is that the garbage collector runs for 98% of the CPU time. This is due to many large objects being created and left behind
for the GC at a very fast rate. Trying several flags for the JVM which among others increase the available memory has no effect and only 
makes the program run for a few more minutes.

    -Xmx4g
    -server
    -d64
    -da
    -dsa
    -XX:+UseConcMarkSweepGC
    -XX:+UseParNewGC
    -XX:ParallelCMSThreads=4
    -XX:+ExplicitGCInvokesConcurrent
    -XX:+CMSParallelRemarkEnabled
    -XX:-CMSIncrementalPacing
    -XX:+UseCMSInitiatingOccupancyOnly
    -XX:CMSIncrementalDutyCycle=100
    -XX:CMSInitiatingOccupancyFraction=90
    -XX:CMSIncrementalSafetyFactor=10
    -XX:+CMSClassUnloadingEnabled"
    -XX:+DoEscapeAnalysis
    
An example input to trigger this error is for 10.000 iterations and 100 equations. It is believed that Criterium has something to do with it because when running the methods with the 
previous and a bit higher inputs, the error doesn't appear. Of course when the input gets too high like 10.000 iterations and 1.000 equations it appears again.

The solutions are:

* To not include in the tests such high inputs and continue using Criterium
* To include a bit higher inputs -> remove criterium -> benchmark with ```time```? (can't compare its credibility with Criterium)


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
    
## Benchmarking once for every parameter combination <a name="Benchmarking_once"></a>

The methods accept combinations of the number of cores, cores for mixed method, teams, equations, iterations and max equation size. The total number of combinations
depends on the size of each array and equals

    combinations_num = Core-array_size * Core-array-for-mixed_size * Team-array_size * Equation-array_size * Max-equation-size-array_size * Iterations-array_size
    
Also, we have 4 methods to test and that means that the total number of benchmarks will be

    benchmarks_num = 4 * combinations_num
    
In addition, every benchmark executes the given method 60 times in order to provide reliable results.

    execution_count = 60 * 4 * combinations_num
    
For a hypothetical case where

    Core-array                  [2 4 6]
    Core-array-for-mixed        [2 4]
    Team-array                  [10 20 50]
    Equation-array              [100 1000 10000]
    Max-equation-size-array     [3]
    Iterations-array            [100 1000 10000]
    
the total execution count is

    total_execution_count = 60 * 4 * (3 * 2 * 3 * 3 * 1 * 3) = 38880
    
These executions vary in length from milliseconds to minutes depending on the method tested. It is safe to say that the calculations will last days on our equipment.  

To save time and maybe have the opportunity to extend a bit more the arrays we have to benchmark each method for a given combination once.
This may seem obvious but with a bit carelessness it's easy to benchmark methods multiple times. Finding all the combinations of the parameters of interest
and putting them to the methods will result to multiple benchmarking of the methods for the same parameters. For example, suppose that we have two combinations
that differ only in the number of teams. The serial method doesn't depend on that parameter but it will be benchmarked twice. It's getting even worse when the equation count and 
iterations rise, where the aforementioned benchmark could take hours to complete.

To solve this problem we can find which parameters affect each method and don't repeat the benchmark again for some parameter combinations. For example, instead of
calculating all the possible combinations like this

```clojure
(for [equations-num Equation-array]
     [cores-for-mixed Core-array-for-mixed]
     [max-equation-size Max-equation-size-array]
     [iterations Iterations-array]
     [teams-num Team-array]
     [cores Core-array]
     
     (bench 
        (serial-method ....)))
```

we could avoid calculating for several ```team-num``` and ```cores``` by 


```clojure
(for [equations-num Equation-array]
     [cores-for-mixed Core-array-for-mixed]
     [max-equation-size Max-equation-size-array]
     [iterations Iterations-array]
     
     (do
        (bench 
            (serial-method ....))
     
     (for [teams-num Team-array]
          [cores Core-array]
          
          ;rest methods
          )))
```

The problem doing it in that way, is that the methods depend from different parameters and that creates conflicts when we try to place them in the right order.
Also, the system-generator will have to generate multiple systems depending on the parameters which means that a system must exist for the methods to calculate.
This really doesn't have a solution by reordering the methods and the generator between the parameter changes.

An alternative solution is to have a set for each method which will hold maps. These maps will have as keys the parameters and values their values. Before benchmarking a method,
a check will be made to see if this combination has been benchmarked already.

A more elegant solution is to use memoization. By memoizing a function, a map is created which has as keys the inputs of the function and as values the result. When
an input combination appears again the results is returned instead of calculating it again. We could say that it works like caching. This is the solution used in our
test-bench.

Furthermore, it may seem that not all methods need memoization. For example, the mixed one which depends from all the parameters in the combinations. Well, that is not the case
as under certain conditions it will have to repeat a benchmark. An example is when ```cores``` is the only parameter that changes **and** the ```teams-num``` is
lower than ```cores```. As a result, we have to memoize all the methods. As a proof a small test has been made giving the input 
    
```clojure
(-main "results.txt" "2,3" "2" "2" "10" "3" "10" "999" "-5" "5" "0" "0" "2" "false")
```

in the repl. The execution time of the above was measured with the ```time``` function which is not reliable but gives a rough estimate of the execution time of the
expression wrapped in it. The results were

    non memoized methods: 11.5 min
    with memoized methods: 7.16 min
    
This is 4 minutes faster even for this small test.    