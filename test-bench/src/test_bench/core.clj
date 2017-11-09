(ns test-bench.core
	(:require  [test-bench.bench :refer [benchmark-procedure]]
			  [test-bench.email :refer [send-mail]]
			  [test-bench.result-processing :refer [create-excel]]
			  [test-bench.helper-functions :refer [get-working-directory-path]]
			  [clojure.string :as str])
	(:gen-class))	

(defn split-at-comma [string]
	(str/split string #","))	

(defn strings-to-ints [strings]
	(mapv #(Integer/parseInt %) strings))	

(defn parse-string [string]
	(-> string
		split-at-comma
		strings-to-ints))	
	
(defn parse-arrays [core-array team-array equation-array iterations-array]
	(let [core-vector (parse-string core-array) 
		 team-vector (parse-string team-array)
		 equation-vector (parse-string equation-array)
		 iterations-vector (parse-string iterations-array)]
		[core-vector team-vector equation-vector iterations-vector]))	

(defn parse-system-generator-arguments [seed weightLow weightHigh initial-value-low initial-value-high double-precision]
	(let [seed (Integer/parseInt seed)
		 weightLow (Double/parseDouble weightLow)
		 weightHigh (Double/parseDouble weightHigh)
		 initial-value-low (Double/parseDouble initial-value-low)
		 initial-value-high (Double/parseDouble initial-value-high)
		 double-precision (Integer/parseInt double-precision)]
		[seed weightLow weightHigh initial-value-low initial-value-high double-precision]))			
		
(defn -main [system-file-name file-name core-array team-array equation-array iterations-array seed weightLow weightHigh initial-value-low initial-value-high double-precision]
	(let [[core-vector team-vector equation-vector iterations-vector] (parse-arrays core-array team-array equation-array iterations-array)
		 [seed weightLow weightHigh initial-value-low initial-value-high double-precision] (parse-system-generator-arguments seed weightLow weightHigh initial-value-low initial-value-high double-precision)]
		(benchmark-procedure system-file-name file-name core-vector team-vector equation-vector iterations-vector seed weightLow weightHigh initial-value-low initial-value-high double-precision)
		(create-excel (str (get-working-directory-path) "\\" file-name) file-name)
		(send-mail file-name "progress.txt")))
		
	