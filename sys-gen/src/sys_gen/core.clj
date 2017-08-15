(ns sys-gen.core
  (:require [sys-gen.linear-system-generator :refer [linear-system-generator]])
  (:gen-class))

;Writes equations to a file
;file-name - string
;equations - list or vector of strings  
(defn write-to-file [file-name equations]	
	(spit file-name (apply str (map prn-str equations))))

;Parses the input, calls the linear system generator, writes the result to a file	
(defn -main [ & args]
  (let [[file-name seed weight-low weight-high initial-value-low initial-value-high double-precision number-of-equations number-of-teams max-equation-size] args

		seed (Long/parseLong seed)
		weight-low (Double/parseDouble weight-low)
		weight-high (Double/parseDouble weight-high)
		initial-value-low (Double/parseDouble initial-value-low)
		initial-value-high (Double/parseDouble initial-value-high)
		double-precision (Long/parseLong double-precision)
		number-of-equations (Long/parseLong number-of-equations)
		number-of-teams (Long/parseLong number-of-teams)
		max-equation-size (Long/parseLong max-equation-size)
		
		equations (linear-system-generator seed weight-low weight-high initial-value-low initial-value-high double-precision number-of-equations number-of-teams max-equation-size)]
		
		(write-to-file file-name equations)))
