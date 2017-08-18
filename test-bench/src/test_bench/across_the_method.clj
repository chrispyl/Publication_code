(ns test-bench.across-the-method
	(:require [test-bench.teamming :refer :all]
			 [test-bench.topo-sort :refer :all]
			 [test-bench.serial :refer [remove-elements]]))

(defn repeatedly*
  [coll n f]
  (if-not (instance? clojure.lang.IEditableCollection coll)
    (loop [v coll idx 0]
      (if (>= idx n)
        v
        (recur (conj v (f)) (inc idx))))
    (loop [v (transient coll) idx 0]
      (if (>= idx n)
        (persistent! v)
        (recur (conj! v (f)) (inc idx))))))
		
(defn create-subsystem-list [team-map system-map]
	(let [subsystems (for [team team-map]
					  (create-subsystem (flatten team) system-map))]
		subsystems))		  

(defn create-value-vec [initial-value iterations equation-name-as-key]
  (let [v (repeatedly* [] (+ iterations 1) promise)]
	   (deliver (v 0) initial-value)
		v))					
			
(defn create-fns-atoms-map [system-map iterations]
	(zipmap 
		(keys system-map) 
		(map 
			#(atom (create-value-vec ((system-map %) :init-val) iterations %))
			(keys system-map))))		

(defn map-to-atom-with-map-of-promises [m]
	(let [num-of-elements (-> m vals first count)
		 v (repeatedly* [] num-of-elements promise)]
		 (doall
			(map #(deliver (v %2) %) (first (vals m)) (range num-of-elements)))
		{(-> m keys first) (atom v)}))
			
(defn put-fileValues-to-fns-atoms-map [fns-atoms-map fileValues]
	(loop [m fns-atoms-map ks (keys fileValues)]
		(if (empty? ks)
			m
			(recur (merge m (map-to-atom-with-map-of-promises {(first ks) (fileValues (first ks))})) (rest ks)))))			
			
(defn where-to-pull-params [subsystem fns-atoms-map]
	(for [subsystem-key (keys subsystem)]
		(mapv #(fns-atoms-map %) ((subsystem subsystem-key) :params))))	

(defn where-to-store [subsystem fns-atoms-map]
	(doall 
		(map #(fns-atoms-map %) (keys subsystem))))				
		
;returns the executalbe functions in the order of (keys subsystem)		
(defn get-functions [subsystem]
	(doall 
		(map #((subsystem %) :func) (keys subsystem))))		

(defn get-param-names-as-keys [subsystem]
	(map #((subsystem %) :params) (keys subsystem)))
		
;this function returns the atoms holding the values of the equations in the order of (keys subsystem)		
(defn get-equation-atoms [subsystem fns-atoms-map]
	(doall
		(map #(fns-atoms-map %) (keys subsystem))))		

(defn calc-new-value [iteration function param-names pull-from]
	(function (zipmap param-names (map #(deref ((deref %) iteration)) pull-from)))) ;must make a map here of params matched to previous step parameters to values
	
;function is an executable funciton created by infix		
(defn euler-method [iteration step  function equation-atom param-names pull-from]
	(+ @(@equation-atom iteration) (* 1 (calc-new-value iteration function param-names pull-from))))		

;equation atom is the atom which holds the values for this equation	
;function is an executable funciton created by infix
;pull-from is a list of atoms containing the atoms from where the equation will pull its values
;store-at is an atom where the equation will store its values		
(defn update-atom [iteration step param-names pull-from function equation-atom store-at]
	(swap! store-at assoc (+ iteration 1) (deliver (@store-at (+ iteration 1)) (euler-method iteration 1 function equation-atom param-names pull-from))))
		
(defn calc-funcs-in-chunk [iterations step fns-atoms-map system-map]
	(let  [simple-eqs-map (remove-elements true system-map)
		   ordered-simple-eqs (topol-sort simple-eqs-map)
		   system-map (remove-elements false system-map)		;this system map has the simple equations removed
	   
		   ;keys
		   system-map-keys (keys system-map)                ;as a result of the redefined system map, these keys are only from dif equations
		   simple-eqs-map-keys ordered-simple-eqs
		   ;functions
		   dif-eqs-functions (get-functions system-map)
		   simple-eqs-functions (get-functions simple-eqs-map)
		   ;equation-atoms
		   dif-eqs-atoms (get-equation-atoms system-map fns-atoms-map)
		   simple-eqs-atoms (get-equation-atoms simple-eqs-map fns-atoms-map)
		   ;param-names ;just to put them as keys for the map destinated to go to infix function
		   dif-eqs-param-names (get-param-names-as-keys system-map)
		   simple-eqs-param-names (get-param-names-as-keys simple-eqs-map)
		   ;pull
		   diff-eqs-pull (where-to-pull-params system-map fns-atoms-map)
		   simple-eqs-pull (where-to-pull-params simple-eqs-map fns-atoms-map)
		   ;storage
		   diff-eqs-store (where-to-store system-map fns-atoms-map)
		   simple-eqs-store (where-to-store simple-eqs-map fns-atoms-map)
		   calculations (loop [iteration 0]
							(when-not (= iteration iterations)
								(dorun
									(map #(update-atom iteration 1 % %2 %3 %4 %5)
										dif-eqs-param-names
										diff-eqs-pull
										dif-eqs-functions
										dif-eqs-atoms
										diff-eqs-store))
								(dorun	
									(map #(update-atom iteration 1 % %2 %3 %4 %5)
										simple-eqs-param-names
										simple-eqs-pull
										simple-eqs-functions
										simple-eqs-atoms
										simple-eqs-store))
									(recur (inc iteration))))
		    ]
		))

(defn create-result-map-for-across-the-method [fns-atoms-map]
	(zipmap 
		(keys fns-atoms-map) 
		(map 
			#(reduce (fn [v prom] (conj v @prom)) [] %) 
			(map deref (vals fns-atoms-map)))))
		
;to system map exei ta panta, keys einia ta function names  
(defn across-the-method-integration [iterations subsystems system-map fileValues]
	(let [fns-atoms-map (create-fns-atoms-map system-map iterations)
		 fns-atoms-map (put-fileValues-to-fns-atoms-map fns-atoms-map fileValues)
		 futures (doall 
					(map #(future (calc-funcs-in-chunk iterations 1 fns-atoms-map %)) subsystems))]
		(create-result-map-for-across-the-method fns-atoms-map)))	
		  