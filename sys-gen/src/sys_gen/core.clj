(ns sys-gen.core
  (:require [clojure.math.combinatorics :as combo])
  (load "system_generator")
  (:gen-class))

(defn write-to-file [file-name equations]	
	(spit file-name (apply str (map prn-str equations))))
 
(defn -main [ & args]
  (let [[file-name seed weightLow weightHigh initial-value-low initial-value-high double-precision number-of-equations number-of-teams linear? max-equation-size] args
		seed (Integer/parseInt seed)
		weightLow (Double/parseDouble weightLow)
		weightHigh (Double/parseDouble weightHigh)
		initial-value-low (Double/parseDouble initial-value-low)
		initial-value-high (Double/parseDouble initial-value-high)
		double-precision (Integer/parseInt double-precision)
		number-of-equations (Integer/parseInt number-of-equations)
		number-of-teams (Integer/parseInt number-of-teams)
		linear? (if (= linear? "true") true false)
		max-equation-size (Integer/parseInt max-equation-size)
		equations (system-generator seed weightLow weightHigh initial-value-low initial-value-high double-precision number-of-equations number-of-teams linear? max-equation-size)]
		(write-to-file file-name equations)))
