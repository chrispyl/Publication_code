(ns test-bench.core
	(:require  
			  [test-bench.across-the-method :refer [across-the-method-integration create-result-map-for-across-the-method create-subsystem-map-across-the-method]]
			  [test-bench.infix-equation-handling :refer [create-system-map]]
			  [test-bench.across-the-system :refer [partition-labour create-result-map]]
			  [test-bench.system-generator :refer [system-generator]]
			  [test-bench.teamming :refer [create-team-map create-subsystem-map work-sharing]]
			  [test-bench.serial :refer [serial-integration]]
			  [test-bench.custom-bench-results :refer [bench-with-result]]
			  [clojure.string :as str])
	(:gen-class))	

(defn prepare-across-the-system [start end step system-map available-cores fileValues]
	(let [team-map (create-team-map system-map fileValues)
		 subsystems-map (create-subsystem-map team-map system-map available-cores)
		 result (partition-labour start end step subsystems-map system-map fileValues)]
		(create-result-map (keys result) (vals result))))		  
		  
(defn prepare-across-the-method [start end step system-map available-cores fileValues]
	(let [team-map (work-sharing (keys system-map) available-cores)
		 subsystems (create-subsystem-map-across-the-method team-map system-map)
		 result (across-the-method-integration start end step subsystems system-map fileValues)]
		(create-result-map-for-across-the-method result)))
		  
(defn prepare-serial [start end step system-map fileValues]
	(let [result (serial-integration start end step system-map {})]
		(create-result-map (keys result) (vals result))))		  
		  
(defn -main [file-name core-array team-array equation-array max-equation-size-array start end step seed weightLow weightHigh initial-value-low initial-value-high double-precision linear?]
	(let [core-vector (mapv #(Integer/parseInt %) (str/split core-array #","))
		 team-vector (mapv #(Integer/parseInt %) (str/split team-array #","))
		 equation-vector (mapv #(Integer/parseInt %) (str/split equation-array #","))
		 max-equation-size-vector (mapv #(Integer/parseInt %) (str/split max-equation-size-array #","))
		 
		 start (Integer/parseInt seed)
		 end (Integer/parseInt seed)
		 step (Double/parseDouble seed)
		 seed (Integer/parseInt seed)
		 weightLow (Double/parseDouble weightLow)
		 weightHigh (Double/parseDouble weightHigh)
		 initial-value-low (Double/parseDouble initial-value-low)
		 initial-value-high (Double/parseDouble initial-value-high)
		 double-precision (Integer/parseInt double-precision)
		 linear? (if (= linear? "true") true false)]
		 
		 (for [number-of-teams team-vector]
			(for [number-of-equations equation-vector]
				(for [max-equation-size max-equation-size-vector]
					(let [strings (system-generator seed weightLow weightHigh initial-value-low initial-value-high double-precision number-of-equations number-of-teams linear? max-equation-size)
						 system-map (create-system-map strings {})]
						(for [cores core-vector]
							
							(do
								(spit "progress.txt" (str cores " cores, " number-of-equations " equations, " number-of-teams " teams, " max-equation-size " max-equation-size" "\n") :append true)
								
								(let [bench-result-serial (dissoc (bench-with-result (prepare-serial start end step system-map {})) :results :samples :input-arguments)
									 bench-result-across-the-method (dissoc (bench-with-result (prepare-across-the-method start end step system-map cores {})) :results :samples :input-arguments)
									 bench-result-across-the-system (dissoc (bench-with-result (prepare-across-the-system start end step system-map cores {})) :results :samples :input-arguments)]
									 
									(spit file-name (str "Serial: " cores " cores, " number-of-equations " equations, " number-of-teams " teams, " max-equation-size " max-equation-size" "\n\n") :append true)
									(spit file-name (str bench-result-serial "\n") :append true)
									(spit file-name "-----------------------------------------------------------------------------------------------\n" :append true)
									(spit file-name (str "Across the method: " cores " cores, " number-of-equations " equations, " number-of-teams " teams, " max-equation-size " max-equation-size" "\n\n") :append true)
									(spit file-name (str bench-result-across-the-method "\n") :append true)
									(spit file-name "-----------------------------------------------------------------------------------------------\n" :append true)
									(spit file-name (str "Across the system: " cores " cores, " number-of-equations " equations, " number-of-teams " teams, " max-equation-size " max-equation-size" "\n\n") :append true)
									(spit file-name (str bench-result-across-the-system "\n") :append true)
									(spit file-name "-----------------------------------------------------------------------------------------------\n" :append true)
							
							))))))) 
		))
	
	