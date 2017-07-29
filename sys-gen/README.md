# sys-gen

A generator for systems of random First Order Ordinary Differential Equations.

[Usage](#Usage)  
[Arguments](#Arguments)  
[Example](#Example)  
[Linear-equation-generation](#Linear-equation-generation)  
[Non-linear-equation-generation](#Non-linear-equation-generation)  
[Variable-names](#Variable-names)  
[The role of Max-equation-size](#The-role-of-max-equation-size)  
[Generation-limitations](#Generation-limitations)  
[Generation-strategy-for-linear-equations](#Generation-strategy-for-linear-equations)  
[Generation-strategy-for-non-linear-equations](#Generation-strategy-for-non-linear-equations)  
[Something-worth-attention](#Something-worth-attention)  

## Usage <a name="Usage"></a>

Go to the project folder in the dcommand line and type 

    lein uberjar


A folder named **target** will be created. Inside the folder their is a file named **sys-gen-0.1.0-standalone.jar**. To execute it, type

    java -jar sys-gen-0.1.0-standalone.jar arg1 arg2 ...

## Arguments <a name="Arguments"></a>

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

## Example <a name="Example"></a>

    java -jar sys-gen-0.1.0-standalone.jar system.txt 999 -5 5 0 10 2 100 4 false 3

## Linear equation generation <a name="Linear-equation-generation"></a>

The operators used for this type of generation are ```+, -, *, /```

## Non linear equation generation <a name="Non-linear-equation-generation"></a> 

The operators used for this type of generation are ```+, -, *, /, **```

Also there are single argument functions ```abs, sqrt, exp, ln, sin, cos, atan```

as well as double argument functions ```min, max```

## Variable names <a name="Variable-names"></a>

The variable names are created using an alphabet by the combining its letters. The alphabet was initially decided to be the english one with lowercase letters.
As a result combinations like ```ln, min``` appeared which collided with the naming conventions of our parser. To avoid this, the alphabet was modified and each letter is followed by an underscore ```a_, b_, ...```.

## The role of **Max-equation-size** <a name="The-role-of-max-equation-size"></a>

An aspect of the generation is to ensure that the dependencies of the equations are created in a way to form specific teams.
The initial idea was to make the equations dependent from all the other equations of their respective teams. For example, if
```
a, b, c
```
made a team, their equations could be
```
a'=a+b+c#10
b'=b+a+c#10
c'=c+a+b#10
```
As a graph the above can be depicted as

![alt text](sys-gen-images/without-maxequationsize.png "Without 'Max-equation-size'")

So, using the above method the general graph pattern are nodes connected to all the other nodes of the team.

This works but has a serious side effect, which is that the length of the equation is tied to the size of the team. If a team has 10 equations the equation size (number of terms) will be 10, if it has 1000
equations the equation size will be 1000. Now, consider that we benchmark a system of 1000 equations with two test cases, one with 10 teams and one with 50 teams.

```
Test case 1:
1000/10=100 equations size (terms per equation)
```
```
Test case 2:
1000/50=20 equations size (terms per equation)
```
Immediately we notice that in the first test the each equation has 500% more terms to calculate compared to one of the second test case.

To avoid this pitfall, the factor **Max-equation-size** was introduced, which specifies the maximum terms each equation is allowed to have. 
The creation of teams is guaranteed by the fact that the last term of each equation is a function of the previous equation, creating a chain dependency. 
For example, consider the previous team of
```
a, b, c
```
If **Max-equation-size** was 2, their equations would be
```
a'=a+c#10
b'=b+a#10
c'=c+b#10
```
The general pattern for a graph of this type is a chain as shown below with some extra edges depending on **Max-equation-size**

![alt text](sys-gen-images/with-maxequationsize.png "With 'Max-equation-size'")

## Generation limitations <a name="Generation-limitations"></a>

Due to randomness, the following rules had to be applied in order to secure that the generated equations are able to be simulated.

* A variable cannot be part of the denominator of a fraction
* The exponents must be positive
* The inputs of functions that cannot take negative numbers must be positive
* Functions like ```cos, tan``` with a narrow input domain cannot used as it is hard to guarantee that the input adheres to their domain

## Generation strategy for linear equations <a name="Generation-strategy-for-linear-equations"></a>

![alt text](sys-gen-images/linear.png "Generation strategy for linear equations")

## Generation strategy for non linear equations <a name="Generation-strategy-for-non-linear-equations"></a>

![alt text](sys-gen-images/non_linear.png "Generation strategy for non linear equations")

## Something worth attention <a name="Something-worth-attention"></a>

In case of simulation, for a large number of iterations these systems will most probably lead to arithmetic underflows or overflows.
To avoid this without using arithmetic libraries set the initial values to be zero.
