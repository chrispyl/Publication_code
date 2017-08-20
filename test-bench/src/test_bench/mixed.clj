(ns test-bench.mixed
	(:require [test-bench.teamming :refer :all]
			 [test-bench.topo-sort :refer :all]
			 [test-bench.serial :refer [remove-elements]]
			 [test-bench.across-the-method :refer [calc-funcs-in-chunk
											  create-subsystem-list
											  create-value-vec
											  create-fns-atoms-map
											  put-fileValues-to-fns-atoms-map
											  create-result-map-for-across-the-method]]))		
 
(defn across-the-method-integration [iterations subsystems system-map fileValues]
	(let [fns-atoms-map (create-fns-atoms-map system-map iterations)
		 fns-atoms-map (put-fileValues-to-fns-atoms-map fns-atoms-map fileValues)
		 futures (doall 
					(map #(future (calc-funcs-in-chunk iterations 1 fns-atoms-map %)) subsystems))]
		fns-atoms-map))	

(defn partition-labour-mixed [iterations subsystems-map available-cores fileValues]
	(let [independent-team-maps (map #(work-sharing (keys %) available-cores) (subsystems-map :independent))
		 dependent-team-map (work-sharing (keys (subsystems-map :dependent)) available-cores)
	     independent-subsystems-of-subsystems (map #(create-subsystem-list % %2) independent-team-maps (subsystems-map :independent))
		 dependent-subsystems (create-subsystem-list dependent-team-map (subsystems-map :dependent))
		 futures (doall
					(map #(future (across-the-method-integration iterations % %2 fileValues)) independent-subsystems-of-subsystems (subsystems-map :independent)))
		 fns-atoms-maps (doall 
						 (map deref futures))
		 independent-results (apply merge (map #(create-result-map-for-across-the-method %) fns-atoms-maps))
		 fns-atoms-map (across-the-method-integration iterations dependent-subsystems (subsystems-map :dependent) (merge independent-results fileValues))] ;Nothing to do with filevalues, just merge here in order to avoid extra handling of the results inside the dependent part. 
		(create-result-map-for-across-the-method fns-atoms-map)))		
	
	
	