(ns test-bench.custom-benchmark
	(:require  [criterium.core]))

;Executes an expression and returns nil. Useful because Criterium saves the results of each run which can occupy a lot of space.
;Must find a better way of doing it because the JVM may be able to optimize it in the future.
;expression - anything
(defn return-nil [expression]
	expression
	nil)	

;Returns a map with the results of the benchmark
;expr - anything
;options - pairs of keywords values (e.g :samples 20)		
(defmacro bench-with-result [expr & options]
	(let [[report-options options] (criterium.core/extract-report-options options)
		  result `(criterium.core/benchmark ~expr ~(when (seq options) (apply hash-map options)))]
		  result))