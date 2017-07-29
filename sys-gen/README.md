# sys-gen

A generator for systems of random First Order Ordinary Differential Equations.

## Usage

Go to the project folder in the dcommand line and type 

    lein uberjar


A folder named **target** will be created. Inside the folder their is a file named **sys-gen-0.1.0-standalone.jar**. To execute it, type

    java -jar sys-gen-0.1.0-standalone.jar arg1 arg2 ...

## Arguments

An execution using the jar looks like this

    java -jar sys-gen-0.1.0-standalone.jar File-name Seed Weight-low Weight-high Initial-value-low Initial-value-high Double-precision Number-of-equations Number-of-teams Linear? Max-equation-size

The arguments in the order they are taken are explained below

**Argument** | **Description** | **Type**
--- | --- | ---
File-name | The file name where the results should be saved | String
Seed | The seed for the random generator | Integer
Weight-low | Minimum value of coefficients | Double
Weight-high | Maximum value of coefficients | Double
Initial-value-low | Minimum initial value of equations | Double
Initial-value-high | Minimum initial value of equations | Double
Double-precision | Decimal digits for coefficients | Integer
Number-of-equations | Number of equations | Integer
Number-of-teams | Number of teams | Integer
Linear? | Linear or not | Boolean
Max-equation-size | Maximum terms of equations | Integer

## Example

    java -jar sys-gen-0.1.0-standalone.jar system.txt 999 -5 5 0 10 2 100 4 false 3

## Linear equation generation

The operators used for this type of generation are ```+, -, *, /```

## Non linear equation generation 

The operators used for this type of generation are ```+, -, *, /, **```

Also there are single argument functions ```abs, sqrt, exp, ln, sin, cos, atan```

as well as double argument functions ```min, max```

## Variable names

The variable names are created using an alphabet by the combining its letters. The alphabet was initially decided to be the english one with lowercase letters.
As a result combinations like ```ln, min``` appeared which collided with the naming conventions of our parser. To avoid this, the alphabet was modified and each letter is followed by an underscore ```a_, b_, ...```.

## Generation limitations

Due to randomness, the following rules had to be applied in order to secure that the generated equations are able to be simulated.

* A variable cannot be part of the denominator of a fraction
* The exponents must be positive
* The inputs of functions that cannot take negative numbers must be positive
* Functions like ```cos, tan``` with a narrow input domain cannot used as it is hard to guarantee that the input adheres to their domain

## Generation strategy for linear equations

![alt text](sys-gen-images/linear.png "Generation strategy for linear equations")

## Generation strategy for non linear equations

![alt text](sys-gen-images/non_linear.png "Generation strategy for non linear equations")

## Something worth attention

In case of simulation, for a large number of iterations these systems will most probably lead to arithmetic underflows or overflows.
To avoid this without using arithmetic libraries set the initial values to be zero.
