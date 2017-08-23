(ns test-bench.result-processing
	(:require [dk.ative.docjure.spreadsheet :as spreadsheet]
			 [test-bench.helper-functions :refer [remove-file-type]]))

(defn read-file [file-path]
	(-> file-path 
		clojure.java.io/reader 
		line-seq))

(defn strings-to-maps [strings]
	(mapv read-string strings))		

(defn write-to-excel [results file-name]
	(let [results-vectors (mapv #(vector (-> % :method name) (% :cores) (% :teams) (% :equations) (% :iterations) (% :mean) (% :variance) (% :std-deviation)) results)
		 collumn-names (mapv name [:method :cores :teams :equations :iterations :mean :variance :std-deviation])
		 wb (spreadsheet/create-workbook "Results"
								(apply conj (conj [] collumn-names) results-vectors))]
		 (spreadsheet/save-workbook! (str (remove-file-type file-name) ".xlsx") wb)))	
		 
(defn create-excel [file-path file-name]
	(-> file-path
		read-file
		strings-to-maps
		(write-to-excel file-name)))		 