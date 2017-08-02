(ns test-bench.core
	(:require  
			  [test-bench.across-the-method :refer [across-the-method-integration create-subsystem-list]]
			  [test-bench.infix-equation-handling :refer [create-system-map]]
			  [test-bench.across-the-system :refer [partition-labour-across-the-system]]
			  [test-bench.system-generator :refer [system-generator]]
			  [test-bench.teamming :refer [create-team-map create-subsystem-map work-sharing]]
			  [test-bench.serial :refer [serial-integration]]
			  [test-bench.custom-bench-results :refer [bench-with-result]]
			  [test-bench.mixed :refer [partition-labour-mixed]]
			  [clojure.string :as str])
	(:gen-class))	
	
	
(defn prepare-across-the-system [iterations system-map available-cores fileValues]
	(let [team-map (create-team-map system-map fileValues)
		 subsystems-map (create-subsystem-map team-map system-map available-cores)]
		[team-map subsystems-map]))		  
		  
(defn prepare-across-the-method [iterations system-map available-cores fileValues]
	(let [team-map (work-sharing (keys system-map) available-cores)
		 subsystems (create-subsystem-list team-map system-map)]
		[team-map subsystems]))
		  
(defn -main [file-name core-array core-array-for-mixed team-array equation-array max-equation-size-array iterations-array seed weightLow weightHigh initial-value-low initial-value-high double-precision linear?]
	(let [core-vector (mapv #(Integer/parseInt %) (str/split core-array #","))
		 core-vector-for-mixed (mapv #(Integer/parseInt %) (str/split core-array-for-mixed #","))
		 team-vector (mapv #(Integer/parseInt %) (str/split team-array #","))
		 equation-vector (mapv #(Integer/parseInt %) (str/split equation-array #","))
		 max-equation-size-vector (mapv #(Integer/parseInt %) (str/split max-equation-size-array #","))
		 iterations-vector (mapv #(Integer/parseInt %) (str/split iterations-array #","))
		 
		 seed (Integer/parseInt seed)
		 weightLow (Double/parseDouble weightLow)
		 weightHigh (Double/parseDouble weightHigh)
		 initial-value-low (Double/parseDouble initial-value-low)
		 initial-value-high (Double/parseDouble initial-value-high)
		 double-precision (Integer/parseInt double-precision)
		 linear? (if (= linear? "true") true false)]
		 
		 (for [number-of-equations equation-vector]
			(for [number-of-teams team-vector]
				(for [max-equation-size max-equation-size-vector]
					(let [strings (system-generator seed weightLow weightHigh initial-value-low initial-value-high double-precision number-of-equations number-of-teams linear? max-equation-size)
						 system-map (create-system-map strings {})]
						(for [cores core-vector]
							(for [iterations iterations-vector]
								(do
									(spit "progress.txt" (str cores " cores, " number-of-equations " equations, " number-of-teams " teams, " max-equation-size " max-equation-size, " iterations " iterations" (System/lineSeparator)) :append true)
									
									(let [bench-result-serial (dissoc (bench-with-result (serial-integration iterations system-map {})) :results :samples :input-arguments)
										 
										 [team-map subsystems] (prepare-across-the-method iterations system-map cores {})
										 bench-result-across-the-method (dissoc (bench-with-result (across-the-method-integration iterations subsystems system-map {})) :results :samples :input-arguments)
										 
										 [team-map subsystems-map] (prepare-across-the-system iterations system-map cores {})
										 bench-result-across-the-system (dissoc (bench-with-result (partition-labour-across-the-system iterations subsystems-map system-map {})) :results :samples :input-arguments)
										 
										 bench-results-for-mixed (doall 
																	(map #(dissoc % :results :samples :input-arguments) 
																		(map #(bench-with-result (partition-labour-mixed iterations subsystems-map % {})) 
																			core-vector-for-mixed)))] ;this do-all is to force it to evaluate in order to be printed at the same time with the others next and not wait
										 
										(spit file-name (str "Serial: " cores " cores, " number-of-equations " equations, " number-of-teams " teams, " max-equation-size " max-equation-size, " iterations " iterations" (System/lineSeparator)(System/lineSeparator)) :append true)
										(spit file-name (str bench-result-serial (System/lineSeparator)) :append true)
										(spit file-name (str "-------------------------------------------------------------------------------------------------------------------------------------------" (System/lineSeparator)) :append true)
										(spit file-name (str "Across the method: " cores " cores, " number-of-equations " equations, " number-of-teams " teams, " max-equation-size " max-equation-size, " iterations " iterations" (System/lineSeparator)(System/lineSeparator)) :append true)
										(spit file-name (str bench-result-across-the-method (System/lineSeparator)) :append true)
										(spit file-name (str "-------------------------------------------------------------------------------------------------------------------------------------------" (System/lineSeparator)) :append true)
										(spit file-name (str "Across the system: " cores " cores, " number-of-equations " equations, " number-of-teams " teams, " max-equation-size " max-equation-size, " iterations " iterations" (System/lineSeparator)(System/lineSeparator)) :append true)
										(spit file-name (str bench-result-across-the-system (System/lineSeparator)) :append true)
										(spit file-name (str "-------------------------------------------------------------------------------------------------------------------------------------------" (System/lineSeparator)) :append true)
										(dorun
											(map #(do
													(spit file-name (str "Mixed: " cores " cores, " number-of-equations " equations, " number-of-teams " teams, " max-equation-size " max-equation-size, " % " cores for across the method, " iterations " iterations" (System/lineSeparator)(System/lineSeparator)) :append true)
													(spit file-name (str %2 (System/lineSeparator)) :append true)
													(spit file-name (str "-------------------------------------------------------------------------------------------------------------------------------------------" (System/lineSeparator)) :append true)) 
													core-vector-for-mixed 
													bench-results-for-mixed))
							)))))))) 
		))
	
	