(ns test-bench.email
	(:require [postal.core :as postal]
			 [test-bench.helper-functions :refer [remove-file-type]]))

;Sends an email to the specified recipient with the results and the report of the benchmarks attached
;results-file-name - string
;progress-file-name - string	
(defn send-mail [results-file-name progress-file-name]
	(postal/send-message {:host "smtp.gmail.com"
						:user "pylianidis"
						:pass "65v*4hjjR&U^%T(*Pg(*^%*65378R&(rtfgiyr88%67r(&*5v768IOF*&"
						:ssl true}
					   {:from "pylianidis@gmail.com"
						:to "pylianidis@gmail.com"
						:subject "Automated message: benchmarks completed"
						:body [{:type :inline
								:content (java.io.File. results-file-name)}
							   {:type :inline
								:content (java.io.File. progress-file-name)}
							   {:type :inline
								:content (java.io.File. (str (remove-file-type results-file-name) ".xlsx"))}]}))	