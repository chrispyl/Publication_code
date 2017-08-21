(ns test-bench.core
	(:require  
			  [test-bench.across-the-method :refer [across-the-method-integration create-subsystem-list]]
			  [test-bench.infix-equation-handling :refer [create-system-map]]
			  [test-bench.across-the-system :refer [partition-labour-across-the-system]]
			  [test-bench.linear-system-generator :refer [linear-system-generator]]
			  [test-bench.teamming :refer [create-team-map create-subsystem-map work-sharing]]
			  [test-bench.serial :refer [serial-integration]]
			  [test-bench.benchmark :refer [bench-with-result wrap-in-do-nil]]
			  [test-bench.mixed :refer [partition-labour-mixed]]
			  [test-bench.stats :refer [std-deviation]]
			  [clojure.string :as str]
			  [test-bench.test-systems :refer :all]
			  [promissum.core :as promissum])
	(:gen-class))	
	
	
(defn prepare-across-the-system [system-map available-cores fileValues]
	(let [team-map (create-team-map system-map fileValues)
		 subsystems-map (create-subsystem-map team-map system-map available-cores)]
		subsystems-map))		  
		  
(defn prepare-across-the-method [system-map available-cores fileValues]
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
	(-> (bench-with-result (wrap-in-do-nil (serial-integration iterations system-map fileValues)))
		process-result-map))	
		
(defn bench-across-the-method [iterations subsystems system-map fileValues]
	(-> (bench-with-result (wrap-in-do-nil (across-the-method-integration iterations subsystems system-map fileValues)))
		process-result-map))

(defn bench-across-the-system [iterations subsystems-map system-map fileValues]
	(-> (bench-with-result (wrap-in-do-nil (partition-labour-across-the-system iterations subsystems-map system-map fileValues)))
		process-result-map))

(defn bench-mixed [iterations subsystems-map cores-for-mixed fileValues]
	(-> (bench-with-result (wrap-in-do-nil (partition-labour-mixed iterations subsystems-map cores-for-mixed fileValues)))
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
	(spit "progress.txt" (str "calculating " string (System/lineSeparator)) :append true))

(defn write-done [string]
	(spit "progress.txt" (str string " done" (System/lineSeparator)) :append true))

(defn update-progress-file [cores number-of-equations number-of-teams max-equation-size iterations]
	(spit "progress.txt" (str cores " cores, " number-of-equations " equations, " number-of-teams " teams, " max-equation-size " max-equation-size, " iterations " iterations" (System/lineSeparator)) :append true))	
	
(defn update-results-file [file-name result-origin cores number-of-equations number-of-teams max-equation-size iterations cores-for-mixed result]
	(condp = result-origin
		:serial 			 (do
							(spit file-name (str "Serial: " cores " cores, " number-of-equations " equations, " number-of-teams " teams, " max-equation-size " max-equation-size, " iterations " iterations" (System/lineSeparator)(System/lineSeparator)) :append true)
							(spit file-name (str result (System/lineSeparator)) :append true)
							(spit file-name (str "-------------------------------------------------------------------------------------------------------------------------------------------" (System/lineSeparator)) :append true))
		:across-the-method (do
							(spit file-name (str "Across the method: " cores " cores, " number-of-equations " equations, " number-of-teams " teams, " max-equation-size " max-equation-size, " iterations " iterations" (System/lineSeparator)(System/lineSeparator)) :append true)
							(spit file-name (str result (System/lineSeparator)) :append true)
							(spit file-name (str "-------------------------------------------------------------------------------------------------------------------------------------------" (System/lineSeparator)) :append true))
		:across-the-system (do
							(spit file-name (str "Across the system: " cores " cores, " number-of-equations " equations, " number-of-teams " teams, " max-equation-size " max-equation-size, " iterations " iterations" (System/lineSeparator)(System/lineSeparator)) :append true)
							(spit file-name (str result (System/lineSeparator)) :append true)
							(spit file-name (str "-------------------------------------------------------------------------------------------------------------------------------------------" (System/lineSeparator)) :append true))
		:mixed			 (do
							(spit file-name (str "Mixed: " cores " cores, " number-of-equations " equations, " number-of-teams " teams, " max-equation-size " max-equation-size, " cores-for-mixed " cores for across the method, " iterations " iterations" (System/lineSeparator)(System/lineSeparator)) :append true)						
							(spit file-name (str result (System/lineSeparator)) :append true)
							(spit file-name (str "-------------------------------------------------------------------------------------------------------------------------------------------" (System/lineSeparator)) :append true))	))

(defn parse-arrays [core-array core-array-for-mixed team-array equation-array max-equation-size-array iterations-array]
	(let [core-vector (mapv #(Integer/parseInt %) (str/split core-array #","))
		 core-vector-for-mixed (mapv #(Integer/parseInt %) (str/split core-array-for-mixed #","))
		 team-vector (mapv #(Integer/parseInt %) (str/split team-array #","))
		 equation-vector (mapv #(Integer/parseInt %) (str/split equation-array #","))
		 max-equation-size-vector (mapv #(Integer/parseInt %) (str/split max-equation-size-array #","))
		 iterations-vector (mapv #(Integer/parseInt %) (str/split iterations-array #","))]
		[core-vector core-vector-for-mixed team-vector equation-vector max-equation-size-vector iterations-vector]))	

(defn parse-system-generator-arguments [seed weightLow weightHigh initial-value-low initial-value-high double-precision linear?]
	(let [seed (Integer/parseInt seed)
		 weightLow (Double/parseDouble weightLow)
		 weightHigh (Double/parseDouble weightHigh)
		 initial-value-low (Double/parseDouble initial-value-low)
		 initial-value-high (Double/parseDouble initial-value-high)
		 double-precision (Integer/parseInt double-precision)
		 linear? (if (= linear? "true") true false)]
		[seed weightLow weightHigh initial-value-low initial-value-high double-precision linear?]))		
		
(defn -main [file-name core-array core-array-for-mixed team-array equation-array max-equation-size-array iterations-array seed weightLow weightHigh initial-value-low initial-value-high double-precision linear?]
	(let [[core-vector core-vector-for-mixed team-vector equation-vector max-equation-size-vector iterations-vector] (parse-arrays core-array core-array-for-mixed team-array equation-array max-equation-size-array iterations-array)
		 [seed weightLow weightHigh initial-value-low initial-value-high double-precision linear?] (parse-system-generator-arguments seed weightLow weightHigh initial-value-low initial-value-high double-precision linear?)]
		 
		 (dorun
		   (for [number-of-equations equation-vector
				number-of-teams team-vector
				max-equation-size max-equation-size-vector
				cores core-vector
				cores-for-mixed core-vector-for-mixed
				iterations iterations-vector]
				(do
					(update-progress-file cores number-of-equations number-of-teams max-equation-size iterations)
					
					(let [system (linear-system-generator seed weightLow weightHigh initial-value-low initial-value-high double-precision number-of-equations number-of-teams max-equation-size)
						 system-map (create-system-map system {})
						 
						 _ (write-calculating "serial")
						 bench-result-serial (bench-serial-memo iterations system-map {})
						 _ (write-done "serial")
						 
						 _ (update-results-file file-name :serial cores number-of-equations number-of-teams max-equation-size iterations cores-for-mixed bench-result-serial)
						 
						 _ (write-calculating "across-the-method")
						 subsystems (prepare-across-the-method-memo system-map cores {})
						 bench-result-across-the-method (bench-across-the-method-memo iterations subsystems system-map {})
						 _ (write-done "across-the-method")
						 
						 _ (update-results-file file-name :across-the-method cores number-of-equations number-of-teams max-equation-size iterations cores-for-mixed bench-result-across-the-method)
						 
						 _ (write-calculating "across-the-system")
						 subsystems-map (prepare-across-the-system-memo system-map cores {})
						 bench-result-across-the-system (bench-across-the-system-memo iterations subsystems-map system-map {})
						 _ (write-done "across-the-system")
						 
						 _ (update-results-file file-name :across-the-system cores number-of-equations number-of-teams max-equation-size iterations cores-for-mixed bench-result-across-the-system)
						 
						 _ (write-calculating "mixed")
						 bench-result-for-mixed (bench-mixed-memo iterations subsystems-map cores-for-mixed {}) ;uses the subsystems-map created for across the system					
						 _ (write-done "mixed")
						 
						 _ (update-results-file file-name :mixed cores number-of-equations number-of-teams max-equation-size iterations cores-for-mixed bench-result-for-mixed)] 
				))))))
		
	