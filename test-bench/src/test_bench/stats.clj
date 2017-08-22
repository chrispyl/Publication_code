(ns test-bench.stats)

;Returns the std-deviation based of the input value
;variance - double
(defn std-deviation [variance]
	(Math/sqrt variance))