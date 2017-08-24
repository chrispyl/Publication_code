(ns test-bench.across-the-system
	(:require [test-bench.serial :refer [serial-integration
									 remove-elements
									 create-init-vals-map
									 create-value-map
									 create-params-vector
									 create-fn-vector
									 calc-func
									 euler-method]]
			 [test-bench.topo-sort :refer [topol-sort]]))


(defn dependent-integration [iterations system-map produced-values-map fileValues]
	(let  [outer-dependencies (filter #(nil? (system-map %)) (set (flatten (map :params (vals system-map)))))
		   outer-dependencies-produced-values-map (zipmap outer-dependencies (map #(produced-values-map %) outer-dependencies))
		   whole-system-keys (keys system-map)
		   init-vals-map (create-init-vals-map system-map)
		   value-map (create-value-map system-map init-vals-map)
		   value-map (reduce #(assoc % (first %2) (second %2)) value-map fileValues)
		   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
		   simple-eqs-map (remove-elements true system-map)
		   ordered-simple-eqs (topol-sort simple-eqs-map)
		   system-map (remove-elements false system-map)
		   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
		   params-vector (create-params-vector system-map)
		   simple-eqs-params-vector (create-params-vector simple-eqs-map ordered-simple-eqs)
		   fn-vector (create-fn-vector system-map)
		   simple-eqs-fn-vector (create-fn-vector simple-eqs-map ordered-simple-eqs)
		   system-map-keys (keys system-map)
		   simple-eqs-map-keys ordered-simple-eqs
		   final-value-map (loop [iter 0 vm value-map]
								(if (< iter iterations)
									(let [value-map-after-diffs-assoced (loop [ks system-map-keys m vm pv params-vector fv fn-vector]
																			(if (empty? ks)
																				m
																				(recur (next ks) (assoc m (first ks) (conj ((first ks) m) (euler-method iter 1 m outer-dependencies-produced-values-map (first ks) (first pv) (first fv)))) (next pv) (next fv))))
										  value-map-after-eqs-assoced (loop [ks ordered-simple-eqs m value-map-after-diffs-assoced params-vec simple-eqs-params-vector fn-vec simple-eqs-fn-vector]
																			(if (empty? ks)
																				m
																				(recur (next ks) (assoc m (first ks) (conj ((first ks) m) (calc-func (inc iter) (first ks) m outer-dependencies-produced-values-map (first params-vec) (first fn-vec)))) (next params-vec) (next fn-vec))))]
									(recur (inc iter) value-map-after-eqs-assoced))
									vm))]
		final-value-map))			
				 
	
(defn across-the-system-integration [iterations subsystems-map system-map fileValues]
	(let [futures (doall ;without doall only 1 go starts
					(map #(future (serial-integration iterations % fileValues)) (:independent subsystems-map)))
		  independent-result (apply merge (map deref futures))
		  dependent-result (dependent-integration iterations (:dependent subsystems-map) independent-result fileValues)
		  merged-results (merge independent-result dependent-result)]
		  (zipmap (keys merged-results) (vals merged-results))))				 
		  	