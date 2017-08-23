(ns test-bench.bench
	(:require  [test-bench.across-the-method :refer [across-the-method-integration create-subsystem-list]]
			  [test-bench.infix-equation-handling :refer [create-system-map]]
			  [test-bench.across-the-system :refer [partition-labour-across-the-system]]
			  [test-bench.linear-system-generator :refer [linear-system-generator]]
			  [test-bench.teamming :refer [create-team-map create-subsystem-map work-sharing]]
			  [test-bench.serial :refer [serial-integration]]
			  [test-bench.custom-benchmark :refer [bench-with-result wrap-in-do-nil]]
			  [test-bench.mixed :refer [partition-labour-mixed]]
			  [test-bench.stats :refer [std-deviation]]
			  [test-bench.helper-functions :refer [get-date-time]]
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
	(dissoc bench-result-map :results :samples :input-arguments))		

(defn add-std-deviation [bench-result-map]
	(assoc bench-result-map :std-deviation (std-deviation (first (bench-result-map :variance)))))	

(defn process-result-map [result-map]
	(-> result-map
		remove-garbage
		add-std-deviation))	
	
(defn bench-serial [iterations system-map fileValues]
	(-> (bench-with-result (wrap-in-do-nil (serial-integration iterations system-map fileValues)) :samples 10)
		process-result-map))	
		
(defn bench-across-the-method [iterations subsystems system-map fileValues]
	(-> (bench-with-result (wrap-in-do-nil (across-the-method-integration iterations subsystems system-map fileValues)) :samples 10)
		process-result-map))

(defn bench-across-the-system [iterations subsystems-map system-map fileValues]
	(-> (bench-with-result (wrap-in-do-nil (partition-labour-across-the-system iterations subsystems-map system-map fileValues)) :samples 10)
		process-result-map))

(defn bench-mixed [iterations subsystems-map cores-for-mixed fileValues]
	(-> (bench-with-result (wrap-in-do-nil (partition-labour-mixed iterations subsystems-map cores-for-mixed fileValues)) :samples 10)
		process-result-map))		

;benchmark memoizations		
(def bench-serial-memo (memoize bench-serial))
(def bench-across-the-method-memo (memoize bench-across-the-method))
(def bench-across-the-system-memo (memoize bench-across-the-system))
(def bench-mixed-memo (memoize bench-mixed))

;other function memoizations
(def prepare-across-the-method-memo (memoize prepare-across-the-method))
(def prepare-across-the-system-memo (memoize prepare-across-the-system))

(defn write-calculating [string]
	(spit "progress.txt" (str "calculating " string ", " (get-date-time) (System/lineSeparator)) :append true))

(defn write-done [string benchmark-counter total-benchmarks]
	(spit "progress.txt" (str string " done, " @benchmark-counter "/" total-benchmarks " benchmarks completed, " (get-date-time) (System/lineSeparator)) :append true))

(defn update-progress-file [cores number-of-equations number-of-teams max-equation-size iterations cores-for-mixed]
	(spit "progress.txt" (str cores " cores, " number-of-teams " teams, " number-of-equations " equations, "  iterations " iterations, " max-equation-size " max-equation-size, " cores-for-mixed " cores for mixed"  (System/lineSeparator)) :append true))	
	
(defn update-results-file [file-name result-origin cores number-of-equations number-of-teams max-equation-size iterations cores-for-mixed result]
	(spit file-name (str {:cores cores 
						:equations number-of-equations 
						:teams number-of-teams 
						:iterations iterations
						:max-equation-size max-equation-size
						:cores-for-mixed cores-for-mixed
						:method result-origin 
						:mean (first (result :mean)) 
						:variance (first (result :variance)) 
						:std-deviation (result :std-deviation)} (System/lineSeparator)) :append true))
						
(defn increase-benchmark-counter [counter]
	(swap! counter inc))

(defn benchmark-procedure [file-name core-vector core-vector-for-mixed team-vector equation-vector max-equation-size-vector iterations-vector seed weightLow weightHigh initial-value-low initial-value-high double-precision]
	(let [benchmark-counter (atom 1)
		 methods-benchmarked 3
		 total-benchmarks (->> [core-vector core-vector-for-mixed team-vector equation-vector max-equation-size-vector iterations-vector]
							 (map count)
							 (reduce * methods-benchmarked))]
		 
		 (dorun
		   (for [number-of-equations equation-vector
				number-of-teams team-vector
				max-equation-size max-equation-size-vector
				cores core-vector
				cores-for-mixed core-vector-for-mixed
				iterations iterations-vector]
				(do
					(update-progress-file cores number-of-equations number-of-teams max-equation-size iterations cores-for-mixed)
					
					(let [system (linear-system-generator seed weightLow weightHigh initial-value-low initial-value-high double-precision number-of-equations number-of-teams max-equation-size)
						 system-map (create-system-map system {})
						 
						 _ (write-calculating "serial")
						 bench-result-serial (bench-serial-memo iterations system-map {})
						 _ (write-done "serial" benchmark-counter total-benchmarks)
						 _ (increase-benchmark-counter benchmark-counter)
						 
						 _ (update-results-file file-name :serial cores number-of-equations number-of-teams max-equation-size iterations cores-for-mixed bench-result-serial)
						 
						 _ (write-calculating "across-the-method")
						 subsystems (prepare-across-the-method-memo system-map cores)
						 bench-result-across-the-method (bench-across-the-method-memo iterations subsystems system-map {})
						 _ (write-done "across-the-method" benchmark-counter total-benchmarks)
						 _ (increase-benchmark-counter benchmark-counter)
						 
						 _ (update-results-file file-name :across-the-method cores number-of-equations number-of-teams max-equation-size iterations cores-for-mixed bench-result-across-the-method)
						 
						 _ (write-calculating "across-the-system")
						 subsystems-map (prepare-across-the-system-memo system-map cores {})
						 bench-result-across-the-system (bench-across-the-system-memo iterations subsystems-map system-map {})
						 _ (write-done "across-the-system" benchmark-counter total-benchmarks)
						 _ (increase-benchmark-counter benchmark-counter)
						 
						 _ (update-results-file file-name :across-the-system cores number-of-equations number-of-teams max-equation-size iterations cores-for-mixed bench-result-across-the-system)
						 
						 ;_ (write-calculating "mixed")
						 ;bench-result-for-mixed (bench-mixed-memo iterations subsystems-map cores-for-mixed {}) ;uses the subsystems-map created for across the system					
						 ;_ (write-done "mixed" benchmark-counter total-benchmarks)
						 ;_ (increase-benchmark-counter benchmark-counter)
						 
						 ;_ (update-results-file file-name :mixed cores number-of-equations number-of-teams max-equation-size iterations cores-for-mixed bench-result-for-mixed)
						 ] 
				))))
				
			(spit "progress.txt" "All completed" :append true)))	