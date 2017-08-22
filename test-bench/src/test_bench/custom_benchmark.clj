(ns test-bench.custom-benchmark
	(:require  [criterium.core]))

;Returns an expression wrapped in a (do nil)	block
;expression - anything
(defn wrap-in-do-nil [expression]
	(do
		expression
		nil))	

;Returns a map with the results of the benchmark
;expr - anything
;options - pairs of keywords values (e.g :samples 20)		
(defmacro bench-with-result [expr & options]
	(let [[report-options options] (criterium.core/extract-report-options options)
		  result `(criterium.core/benchmark ~expr ~(when (seq options) (apply hash-map options)))]
		  result))