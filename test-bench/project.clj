(defproject test-bench "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
				[rm-hull/infix "0.2.10"]
				[org.clojure/core.async "0.2.395"]
				[criterium "0.4.4"]
				[org.clojure/math.combinatorics "0.1.4"]]
  :main ^:skip-aot test-bench.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})