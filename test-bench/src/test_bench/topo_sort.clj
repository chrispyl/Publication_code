(ns test-bench.topo-sort
	(:require [clojure.set :as clset]))

;returns set
(defn find-no-incoming-edges [simple-eqs-map]
	(let [keys-as-set (set (keys simple-eqs-map))]
		(loop [ks (keys simple-eqs-map) k (first ks) params (set (:params (simple-eqs-map k))) params-only-from-simple-eqs (clset/intersection params keys-as-set) nodes-no-incoming-edges #{}]
			(if (empty? ks)				
				nodes-no-incoming-edges			
				(if (empty? params-only-from-simple-eqs)			
					(if (empty? (rest ks)) ;without this if (first (rest ks)) will return nil in the last loop and we will have problem in (nil :params)
						(recur (rest ks) nil nil nil (conj nodes-no-incoming-edges k)) ;nils to show that we dont are about their values anymore
						(recur (rest ks) (first (rest ks)) (set ((simple-eqs-map (first (rest ks))) :params)) (clset/intersection (set ((simple-eqs-map (first (rest ks))) :params)) keys-as-set) (conj nodes-no-incoming-edges k)))				
					(if (empty? (rest ks))
						(recur (rest ks) nil nil nil nodes-no-incoming-edges) 
						(recur (rest ks) (first (rest ks)) (set ((simple-eqs-map (first (rest ks))) :params)) (clset/intersection (set ((simple-eqs-map (first (rest ks))) :params)) keys-as-set) nodes-no-incoming-edges)))))))

(defn remove-edges [simple-eqs-map node]
	(apply merge
		(for [vec-pair simple-eqs-map]
			(if (contains? (set (:params (second vec-pair))) node)
				{(first vec-pair) (assoc-in (second vec-pair) [:params] (vec (disj (set (:params (second vec-pair))) node)))}
				{(first vec-pair) (second vec-pair)}))))					
					
(defn topol-sort [simple-eqs-map]
	(let [S (find-no-incoming-edges simple-eqs-map)]
		(loop [nodes S node (first nodes) L [] m simple-eqs-map all-edge-free-nodes #{node}]
			(if (empty? nodes)
				L
				(let [new-map (remove-edges m node)
					  new-edge-free-nodes (clset/difference (find-no-incoming-edges new-map) all-edge-free-nodes)]
					(if (empty? new-edge-free-nodes)
						(recur (disj nodes node) (first (disj nodes node)) (conj L node) new-map all-edge-free-nodes)
						(recur (apply conj (disj nodes node) new-edge-free-nodes) (first (apply conj (disj nodes node) new-edge-free-nodes)) (conj L node) new-map (apply conj all-edge-free-nodes new-edge-free-nodes))))))))
