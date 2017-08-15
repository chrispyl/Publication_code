# sys-gen

A generator for systems of random First Order Ordinary Differential Equations.

[Usage](#Usage)  
[Arguments](#Arguments)  
[Example](#Example)  
[Specification for linear systems](#spec-linear-systems)  
[Variable-names](#Variable-names)  
[The role of Max-equation-size](#The-role-of-max-equation-size)  
[Something-worth-attention](#Something-worth-attention)  

## Usage <a name="Usage"></a>

Go to the project folder in the command line and type 

    lein uberjar


A folder named **target** will be created. Inside the folder their is a file named **sys-gen-0.1.0-standalone.jar**. To execute it, type

    java -jar sys-gen-0.1.0-standalone.jar arg1 arg2 ...

## Arguments <a name="Arguments"></a>

An execution using the jar looks like this

    java -jar sys-gen-0.1.0-standalone.jar file-name seed weight-low weight-high initial-value-low initial-value-high double-precision number-of-equations number-of-teams max-equation-size

The arguments in the order they are taken are explained below

**Argument** | **Description** | **Type**
--- | --- | ---
file-name | The file name where the results should be saved | String
seed | The seed for the random generator | Long
weight-low | Minimum value of coefficients | Double
weight-high | Maximum value of coefficients | Double
initial-value-low | Minimum initial value of equations | Double
initial-value-high | Maximum initial value of equations | Double
double-precision | Decimal digits for coefficients | Long
number-of-equations | Number of equations | Long
number-of-teams | Number of teams | Long
max-equation-size | Maximum terms of equations | Long

## Example <a name="Example"></a>

    java -jar sys-gen-0.1.0-standalone.jar system.txt 999 -5 5 0 10 2 100 4 3

## Specification for linear systems <a name="spec-linear-systems"></a>

1. The range of the coefficients is defined by the user
2. Coefficient ranges can be positive or negative numbers, lower bound inclusive upper bound exclusive, i.e [-5, 5) or [1, 5) or [-5, -1)
3. The range of the initial values is defined by the user
4. Initial value ranges can be positive or negative numbers, lower bound inclusive upper bound exclusive, i.e [-5, 5) or [1, 5) or [-5, -1)
5. The operator between the terms is `+`
6. Every term has a coefficient
7. Between the coefficients and the variables the operator is `*`
8. For the variable names appearing in the equations:  
    * The last term of an equation contains the name of the previous equation in the system in order to create a chain of dependencies
    * The rest appear in the equation in the order the do in the team, and always according to max equation size
9. Coefficients are doubles
10. Initial values are doubles
11. The precision of the coefficients is defined by the user
12. The precision of the initial values is defined by the user
13. In case of zero precision the decimal part of the doubles is cut by using the `floor` function
14. In case of zero precision the results of rule 9 is being casted to long  
  
To calculate the pseudorandom values we use https://docs.oracle.com/javase/7/docs/api/java/util/Random.html#nextDouble(). To provide values in the specified range, we use the formula
    
    random-value = lower-bound + (higher-bound - lower-bound) * value-produced-by-nextDouble

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


## Something worth attention <a name="Something-worth-attention"></a>

In case of simulation, for a large number of iterations these systems will most probably lead to arithmetic underflows or overflows.
To avoid this set the initial values to zero.
