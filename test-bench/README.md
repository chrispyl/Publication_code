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

### Regarding the maximum iterations-equations

For higher amounts of equations and iterations we get

    java.lang.OutOfMemoryError: GC overhead limit exceeded
    
Generally, this means that the garbage collector runs for 98% of the CPU time. This can be due to head retention of some sequence which takes up a lot of heap, as well as many large objects being created and left behind
for the GC at a very fast rate. In both cases the GC is triggered in order to free heap space but it is unable to do so and the loop continues.

In our case, the reason is that during the integration we store the previously produced values in collections. In Clojure, when primitives are put into collections they are
auto-boxed. This means that they are wrapped into objects and end up to the heap. 

Trying several flags for the JVM which among others increase the available heap size has no effect and only makes the program run for a few more minutes.

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
    
To trigger this error in a machine with 4Gb ram (where the JVM automatically puts 1Gb heap) we can try to produce 1.000.000 elements (doubles), e.g 10.000 iterations and 100 equations. It is believed that Criterium has something to do with it (maybe can't trigger the GC among the runs) because when running the methods with the 
previous and a bit higher iterations, the error doesn't appear. Of course for higher numbers such 10.000.000 elements, e.g 10.000 iterations and 1.000 equations it appears again.

The solutions are:

1. To not include in the tests such high inputs and **continue** using Criterium

    This is too limiting as we will have to stay **under** the
    
        10.000 iterations - 100 equations
        or
        1.000 iterations - 1.000 equations

2. To include a bit higher inputs -> **remove** criterium -> benchmark with ```time```? (can't compare its credibility with Criterium)

    We can't achieve the same reliable results without Criterium. Also, the benefit would be just another oder of magnitude. For example, the combination of
    
        10.000 iterations - equations 
        
    would be possible but not much after that.    

3. To remove the transients (which easily lead to head retention) at the expense of execution time from the methods who use them, and **maybe continue** using Criterium

    While dealing with transients many intermediate collections are created and occupy the heap too fast. By removing them
    the heap is still filled but at a slower rate allowing us to produce elements of 2 magnitudes higher e.g 100.000.000 elements or 10.000 iterations 1.000 equations. The concern is
    that we cannot benchmark with Criterium without the error occuring. Maybe if we also quadraple the heap...
    
    The schreenshot below is taken from the JVisualVM and shows this option for 10.000 iterations and 1.000 equations, and an increased heap to 2Gb. The heap is dynamically adjusted by the JVM with maximum size 2GB. As it seems,
    for some time it can handle the executions but after a while the heap is not enough and the GC starts woriking too much and not freeing space.
    
    ![alt text](test-bench-images/heap.png "heap 10.000 iterations, 1.000 equations")

4. ~~To use primitives instead of boxed types in combination with 4.~~

    As said earlier when primitives enter collections they are boxed and enter the heap. To avoid this we could use arrays. The problem is that this is possible only in the serial method and the across the system one.
    The rest use as a synchronization mechanism ```promises``` inside the vectors with the results, which can't be put into arrays. So, this solution is not really an option.

5. To keep only the data needed for next iterations     

    For the ```serial``` and the ```across the system methods```, this means to keep only the values of the previous iterations. 
    
    For the ```across the method``` and the ```mixed one```, this is dangerous as other threads might seek values produced long before from a thread. In our case where the equations
    have specific form this is not possible and if we keep the last 10-100 results for each equation we should be ok.
    
6. Measures not related to the methods     

    Increase heap size.  
    Use the ```quick-bench``` mode of Criterium to execute less times each method. Between the executions, the GC can't clear all the data of the previous execution and the heap is filled more. By executing less times less garbage will be on heap between the executions and the benchmark will complete before the heap blows.

### Regarding the parsing of the equations

For high numbers of equations e.g 10.000 equations, the parsing takes a really long time (hours) complete.

The solutions are:

1. Cut down some steps which won't be used now, like the constant replacing, detection of non differential equations etc which are believed to be the root of the problem.

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