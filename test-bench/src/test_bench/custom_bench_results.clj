(ns test-bench.custom-bench-results
	(:require  [criterium.core]))

(defmacro bench-with-result [expr & options]
	(let [[report-options options] (criterium.core/extract-report-options options)
		  result `(criterium.core/benchmark ~expr ~(when (seq options) (apply hash-map options)))]
		  result))