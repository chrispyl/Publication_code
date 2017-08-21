(ns test-bench.benchmark
	(:require  [criterium.core]))

(defn wrap-in-do-nil [expression]
	(do
		expression
		nil))	
	
(defmacro bench-with-result [expr & options]
	(let [[report-options options] (criterium.core/extract-report-options options)
		  result `(criterium.core/quick-benchmark ~expr ~(when (seq options) (apply hash-map options)))]
		  result))