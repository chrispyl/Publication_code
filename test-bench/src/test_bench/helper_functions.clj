(ns test-bench.helper-functions)

;Returns a strings containing the current date in the place of the machine
(defn get-date-time []
	(.toString (java.time.LocalDateTime/now)))

;A faster implementation of 'repeatedly
;Returns	a collection containing the outputs produced by f
;coll - collection
;n - integer
;f - function
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