### Description
This repository contains the code used for a publication made with my professor [Ioannis Athanasiadis](http://www.athanasiadis.info/), regarding **parallelism** in dynamic system **simulation** using **Clojure**. Continuing the work done in my [thesis](https://github.com/chrispyl/Thesis_Systems_Dynamic_Modeling_with_Clojure),
we chose the fastest parallel methods from each type of parallelism and created a testbench where we benchmarked them, while  modifying the parameters of the dynamic systems, and examined the results. To benchmark the methods, we created a system generator capable of producing systems of different sizes and with specific characteristics tailored to our needs. 

In [sys-gen](https://github.com/chrispyl/Publication_code/tree/master/sys-gen) lies the system generator and in [test-bench](https://github.com/chrispyl/Publication_code/tree/master/test-bench) the testbench along with commentary.
