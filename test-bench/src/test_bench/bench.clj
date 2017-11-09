(ns test-bench.bench
	(:require  [test-bench.across-the-method :refer [across-the-method-integration create-subsystem-list]]
			  [test-bench.infix-equation-handling :refer [create-system-map]]
			  [test-bench.across-the-system :refer [across-the-system-integration]]
			  [test-bench.linear-system-generator :refer [linear-system-generator]]
			  [test-bench.teamming :refer [create-team-map create-subsystem-map work-sharing]]
			  [test-bench.serial :refer [serial-integration]]
			  [test-bench.custom-benchmark :refer [bench-with-result return-nil]]
			  [test-bench.stats :refer [std-deviation]]
			  [test-bench.helper-functions :refer [get-date-time get-lines]]
			  [clojure.string :as str])
	(:gen-class))

(defn prepare-across-the-system [system-map available-cores fileValues]
	(let [team-map (create-team-map system-map fileValues)
		 subsystems-map (create-subsystem-map team-map system-map available-cores)]
		subsystems-map))		  
		  
(defn prepare-across-the-method [system-map available-cores]
	(let [team-map (work-sharing (keys system-map) available-cores)
		 subsystems (create-subsystem-list team-map system-map)]
		subsystems))
		
(defn remove-garbage [bench-result-map]
	(select-keys bench-result-map [:mean :variance]))		

(defn add-std-deviation [bench-result-map]
	(assoc bench-result-map :std-deviation (std-deviation (first (bench-result-map :variance)))))		
	
(defn process-result-map [result-map]
	(-> result-map
		remove-garbage
		add-std-deviation))	

(defn bench-parsing [system fileValues]
	(-> (bench-with-result (return-nil (create-system-map system fileValues)) :samples 10)
		(process-result-map)))		
		
(defn bench-preprocessing-across-the-system [system-map available-cores fileValues]
	(-> (bench-with-result (return-nil (prepare-across-the-system system-map available-cores fileValues)) :samples 10)
		process-result-map))		

(defn bench-preprocessing-across-the-method [system-map available-cores]
	(-> (bench-with-result (return-nil (prepare-across-the-method system-map available-cores)) :samples 10)
		process-result-map))			
		
(defn bench-serial [iterations system-map fileValues]
	(-> (bench-with-result (return-nil (serial-integration iterations system-map fileValues)) :samples 10)
		process-result-map))	
		
(defn bench-across-the-method [iterations subsystems system-map fileValues]
	(-> (bench-with-result (return-nil (across-the-method-integration iterations subsystems system-map fileValues)) :samples 10)
		process-result-map))

(defn bench-across-the-system [iterations subsystems-map system-map fileValues]
	(-> (bench-with-result (return-nil (across-the-system-integration iterations subsystems-map system-map fileValues)) :samples 10)
		process-result-map))	
		
;preprocessing benchmark memoizations
(def bench-preprocessing-across-the-system-memo (memoize bench-preprocessing-across-the-system))
(def bench-preprocessing-across-the-method-memo (memoize bench-preprocessing-across-the-method))		
		
;integration benchmark memoizations		
(def bench-serial-memo (memoize bench-serial))
(def bench-across-the-method-memo (memoize bench-across-the-method))
(def bench-across-the-system-memo (memoize bench-across-the-system))

;other function memoizations
(def bench-parsing-memo (memoize bench-parsing))
(def prepare-across-the-method-memo (memoize prepare-across-the-method))
(def prepare-across-the-system-memo (memoize prepare-across-the-system))

(defn write-calculating [string]
	(spit "progress.txt" (str "calculating " string ", " (get-date-time) (System/lineSeparator)) :append true))

(defn write-done 
	([string benchmark-counter total-method-benchmarks]
		(spit "progress.txt" (str string " done, " @benchmark-counter "/" total-method-benchmarks " benchmarks completed, " (get-date-time) (System/lineSeparator)) :append true))
	([string]
		(spit "progress.txt" (str string " done, " (get-date-time) (System/lineSeparator)) :append true)))

(defn update-progress-file 
	([cores number-of-equations number-of-teams iterations]
		(spit "progress.txt" (str cores " cores, " number-of-teams " teams, " number-of-equations " equations, "  iterations " iterations"  (System/lineSeparator)) :append true))
	([cores iterations]
		(spit "progress.txt" (str cores " cores, " iterations " iterations"  (System/lineSeparator)) :append true)))	
	
(defn update-results-file 
	([file-name result-origin cores number-of-equations number-of-teams iterations result preprocessing-result system-parsing-result]
		(spit file-name (str {:cores cores 
							:equations number-of-equations 
							:teams number-of-teams 
							:iterations iterations
							:method result-origin 
							:mean (first (result :mean)) 
							:variance (first (result :variance)) 
							:std-deviation (result :std-deviation)
							:preprocessing-mean (first (preprocessing-result :mean))
							:preprocessing-variance (first (preprocessing-result :variance))
							:preprocessing-std-deviation (preprocessing-result :std-deviation)
							:system-parsing-mean (first (system-parsing-result :mean))
							:system-parsing-variance (first (system-parsing-result :variance))
							:system-parsing-std-deviation (system-parsing-result :std-deviation)}
							
							(System/lineSeparator)) :append true))
	([file-name result-origin cores iterations result preprocessing-result system-parsing-result]
		(spit file-name (str {:cores cores 
							:iterations iterations
							:method result-origin 
							:mean (first (result :mean)) 
							:variance (first (result :variance)) 
							:std-deviation (result :std-deviation)
							:preprocessing-mean (first (preprocessing-result :mean))
							:preprocessing-variance (first (preprocessing-result :variance))
							:preprocessing-std-deviation (preprocessing-result :std-deviation)
							:system-parsing-mean (first (system-parsing-result :mean))
							:system-parsing-variance (first (system-parsing-result :variance))
							:system-parsing-std-deviation (system-parsing-result :std-deviation)}
							
							(System/lineSeparator)) :append true)))
						
(defn increase-benchmark-counter [counter]
	(swap! counter inc))
	
(defn benchmark-procedure-random [file-name core-vector team-vector equation-vector iterations-vector seed weightLow weightHigh initial-value-low initial-value-high double-precision]
	(let [benchmark-counter (atom 1)
		 max-equation-size 5
		 methods-benchmarked 3
		 total-method-benchmarks (->> [core-vector team-vector equation-vector iterations-vector]
								   (map count)
								   (reduce * methods-benchmarked))]
		 
		 (dorun
		   (for [number-of-equations equation-vector
				number-of-teams team-vector
				cores core-vector
				iterations iterations-vector]
				(do
					(update-progress-file cores number-of-equations number-of-teams iterations)
					
					(let [system (linear-system-generator seed weightLow weightHigh initial-value-low initial-value-high double-precision number-of-equations number-of-teams max-equation-size)
						 system-map (create-system-map system {})
						 
						 _ (write-calculating "system-parsing")
						 system-parsing-result (bench-parsing-memo system {})
						 _ (write-done "system-parsing")
						 
						 
						 _ (write-calculating "serial")
						 bench-result-serial (bench-serial-memo iterations system-map {})
						 _ (write-done "serial" benchmark-counter total-method-benchmarks)
						 _ (increase-benchmark-counter benchmark-counter)
						 
						 _ (update-results-file file-name :serial cores number-of-equations number-of-teams iterations bench-result-serial {:mean nil :variance nil :std-deviation nil} system-parsing-result)
						 
						 _ (write-calculating "across-the-method preprocessing")
						 preprocessing-result-across-the-method (bench-preprocessing-across-the-method-memo system-map cores)
						 _ (write-done "across-the-method preprocessing")
						 _ (write-calculating "across-the-method")
						 subsystems (prepare-across-the-method-memo system-map cores)
						 bench-result-across-the-method (bench-across-the-method-memo iterations subsystems system-map {})
						 _ (write-done "across-the-method" benchmark-counter total-method-benchmarks)
						 _ (increase-benchmark-counter benchmark-counter)
						 
						 _ (update-results-file file-name :across-the-method cores number-of-equations number-of-teams iterations bench-result-across-the-method preprocessing-result-across-the-method system-parsing-result)
						 
						 _ (write-calculating "across-the-system preprocessing")
						 preprocessing-result-across-the-system (bench-preprocessing-across-the-system-memo system-map cores {})
						 _ (write-done "across-the-system preprocessing")
						 _ (write-calculating "across-the-system")
						 across-the-system-subsystems-map (prepare-across-the-system-memo system-map cores {})
						 bench-result-across-the-system (bench-across-the-system-memo iterations across-the-system-subsystems-map system-map {})
						 _ (write-done "across-the-system" benchmark-counter total-method-benchmarks)
						 _ (increase-benchmark-counter benchmark-counter)
						 
						 _ (update-results-file file-name :across-the-system cores number-of-equations number-of-teams iterations bench-result-across-the-system preprocessing-result-across-the-system system-parsing-result)
						
						 ] 
				))))
				
			(spit "progress.txt" "All completed" :append true)))	

(defn benchmark-procedure-user-defined [system-file-name file-name core-vector team-vector equation-vector iterations-vector seed weightLow weightHigh initial-value-low initial-value-high double-precision]
	(let [benchmark-counter (atom 1)
		 methods-benchmarked 3
		 total-method-benchmarks (->> [core-vector iterations-vector]
								   (map count)
								   (reduce * methods-benchmarked))
		 system (-> system-file-name get-lines)
		 system-map (create-system-map system {})
		 system-parsing-result (bench-parsing-memo system {})]
		 
		 (dorun
		   (for [cores core-vector
				iterations iterations-vector]
				(do
					(update-progress-file cores iterations)
					
					(let [_ (write-calculating "serial")
						 bench-result-serial (bench-serial-memo iterations system-map {})
						 _ (write-done "serial" benchmark-counter total-method-benchmarks)
						 _ (increase-benchmark-counter benchmark-counter)
						 
						 _ (update-results-file file-name :serial cores iterations bench-result-serial {:mean nil :variance nil :std-deviation nil} system-parsing-result)
						 
						 _ (write-calculating "across-the-method preprocessing")
						 preprocessing-result-across-the-method (bench-preprocessing-across-the-method-memo system-map cores)
						 _ (write-done "across-the-method preprocessing")
						 _ (write-calculating "across-the-method")
						 subsystems (prepare-across-the-method-memo system-map cores)
						 bench-result-across-the-method (bench-across-the-method-memo iterations subsystems system-map {})
						 _ (write-done "across-the-method" benchmark-counter total-method-benchmarks)
						 _ (increase-benchmark-counter benchmark-counter)
						 
						 _ (update-results-file file-name :across-the-method cores iterations bench-result-across-the-method preprocessing-result-across-the-method system-parsing-result)
						 
						 _ (write-calculating "across-the-system preprocessing")
						 preprocessing-result-across-the-system (bench-preprocessing-across-the-system-memo system-map cores {})
						 _ (write-done "across-the-system preprocessing")
						 _ (write-calculating "across-the-system")
						 across-the-system-subsystems-map (prepare-across-the-system-memo system-map cores {})
						 bench-result-across-the-system (bench-across-the-system-memo iterations across-the-system-subsystems-map system-map {})
						 _ (write-done "across-the-system" benchmark-counter total-method-benchmarks)
						 _ (increase-benchmark-counter benchmark-counter)
						 
						 _ (update-results-file file-name :across-the-system cores iterations bench-result-across-the-system preprocessing-result-across-the-system system-parsing-result)
						
						 ] 
				))))
				
			(spit "progress.txt" "All completed" :append true)))	
			
(defn benchmark-procedure [system-file-name file-name core-vector team-vector equation-vector iterations-vector seed weightLow weightHigh initial-value-low initial-value-high double-precision]
	(if (= "_" system-file-name)
		(benchmark-procedure-random file-name core-vector team-vector equation-vector iterations-vector seed weightLow weightHigh initial-value-low initial-value-high double-precision)
		(benchmark-procedure-user-defined system-file-name file-name core-vector team-vector equation-vector iterations-vector seed weightLow weightHigh initial-value-low initial-value-high double-precision)))				