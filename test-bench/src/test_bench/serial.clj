(ns test-bench.serial
	(:require [test-bench.topo-sort :refer [topol-sort]]))

(defn remove-elements [differential? system-map]
	(loop [ks (keys system-map) m system-map]
		(if ks
			(recur (next ks) (if (= (:differential ((first ks) m)) differential?)
								(dissoc m (first ks))
								m))
			m)))	
	
(defn create-init-vals-map [system-map]
	(zipmap (keys system-map) (map :init-val (vals system-map))))

(defn create-value-map [system-map init-vals-map]
	(zipmap (keys system-map) (map #(vector %) (vals init-vals-map))))

;returns a vector of vectors, each one containing the parameters of a function
(defn create-params-vector 
	([system-map]
		(map :params (vals system-map)))
	([system-map ordered-simple-eqs]
		(map :params (map #(% system-map) ordered-simple-eqs))))	
	
;returns a vector of functions
(defn create-fn-vector 
	([system-map]
		(map :func (vals system-map)))
	([system-map ordered-simple-eqs]
		(map :func (map #(% system-map) ordered-simple-eqs))))				
	
(defn calc-func 
	([iter func-name value-map params-subvector func]
		(func (zipmap params-subvector (map #((% value-map) iter) params-subvector))))
	([iter func-name value-map outer-dependencies-produced-values-map params-subvector func]
		(func (zipmap params-subvector (map 
										#(if-let [v (% value-map)]
											(v iter)
											((% outer-dependencies-produced-values-map) iter))
									params-subvector))))) 	

(defn euler-method 
	([iter step value-map func-name params-subvector func]
		(+ ((func-name value-map) iter) (* 1 (calc-func iter func-name value-map params-subvector func))))
	([iter step value-map outer-dependencies-produced-values-map func-name params-subvector func]
		(+ ((func-name value-map) iter) (* 1 (calc-func iter func-name value-map outer-dependencies-produced-values-map params-subvector func)))))	
	
(defn serial-integration [iterations system-map fileValues]
	(let  [whole-system-keys (keys system-map)
		   init-vals-map (create-init-vals-map system-map)
		   value-map (transient (create-value-map system-map init-vals-map))
		   value-map (reduce #(assoc! % (first %2)  (second %2)) value-map fileValues)
		   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
		   simple-eqs-map (remove-elements true system-map)
		   ordered-simple-eqs (topol-sort simple-eqs-map)
		   system-map (remove-elements false system-map)
		   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
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
																				(recur (rest ks) (assoc! m (first ks) (conj ((first ks) m) (euler-method iter 1 m (first ks) (first pv) (first fv)))) (rest pv) (rest fv))))									
										  value-map-after-eqs-assoced (loop [ks ordered-simple-eqs m value-map-after-diffs-assoced params-vec simple-eqs-params-vector fn-vec simple-eqs-fn-vector]
																			(if (empty? ks)
																				m
																				(recur (rest ks) (assoc! m (first ks) (conj ((first ks) m) (calc-func (inc iter) (first ks) m (first params-vec) (first fn-vec)))) (rest params-vec) (rest fn-vec))))]
									(recur (inc iter) value-map-after-eqs-assoced))
									vm))]
		(persistent! final-value-map)))