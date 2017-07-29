(defproject sys-gen "v0.1"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
			   [org.clojure/math.combinatorics "0.1.4"]]
  :main ^:skip-aot sys-gen.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
