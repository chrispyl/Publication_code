(ns test-bench.email
	(:require [postal.core :as postal]))

;Sends an email to the specified recipient with the results and the report of the benchmarks attached
;results-file-name - string
;progress-file-name - string	
(defn send-mail [results-file-name progress-file-name]
	(postal/send-message {:host ""
						:user ""
						:pass ""
						:ssl true}
					   {:from ""
						:to ""
						:subject "Automated message: benchmarks completed"
						:body [{:type :inline
								:content (java.io.File. results-file-name)}
							   {:type :inline
								:content (java.io.File. progress-file-name)}]}))	