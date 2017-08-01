(ns test-bench.system-generator
	(:require [clojure.math.combinatorics :as combo]))			  

(defn generator-state [seed]
	(java.util.Random. seed))

;nextLong outputs random numbers from weightLow to weighhigh inclusive. Non uniform distribution!!!!!!!!!
(defn nextLong [gen-state weightLow weightHigh]
	(let [weightHigh-inclusive (inc weightHigh)] ;for random longs we have to do this, otherwise the weighhigh will never be reached, for doubles if we do the same the high will be surpassed so its not done
		(long (+ (* (.nextDouble gen-state) (- weightHigh-inclusive weightLow)) weightLow))))	

;nextLong outputs random numbers from weightLow to weighhigh exclusive		
(defn nextDouble [gen-state weightLow weightHigh double-precision]
	(let [number (+ (* (.nextDouble gen-state) (- weightHigh weightLow)) weightLow)
		 factor (Math/pow 10 double-precision)] ;with Math/round it can get to 1 which is sometinh gwe don't like
		(/ (Math/floor (* number factor)) factor)))	

(defn create-teams [variable-names number-of-equations number-of-teams]
    (cond 
      (<= number-of-equations number-of-teams) (partition 1 variable-names)
      (> number-of-equations number-of-teams) (if (> (mod number-of-equations number-of-teams) 0)
                               (let [initial-division (partition-all (quot number-of-equations number-of-teams) variable-names)]
                                 (concat
                                   (drop (mod number-of-equations number-of-teams) (take number-of-teams initial-division))
                                   (map conj initial-division (take-last (mod number-of-equations number-of-teams) variable-names))))
                               (partition-all (quot number-of-equations number-of-teams) variable-names))))		  		
		
(defn choose-random-operator [gen-state operators]
	(if (== 0 (count operators)) (println "operator count inside choose-random-operator is 0"))
	(let [operator-count (count operators)
		 operators-from-list-to-vector (vec operators)
		 chance-of-operator (/ 1 operator-count)
		 rnd-val (nextDouble gen-state 0 1 2)
		 operator-position-in-vector (long (quot rnd-val chance-of-operator))] ;convert to long because when used as key , key can't be double
		 ;(println chance-of-operator rnd-val operator-position-in-vector operators-from-list-to-vector)
		 (operators-from-list-to-vector operator-position-in-vector)))		

(defn if-negative-coefficient-wrap-with-parenthesses [token]
	(if (= (first token) \-) 		   
		(str "(" token ")")
		(let [[variable coefficient] (clojure.string/split token #"/")]
			(if (= (first coefficient) \-) ;the second case is ot guard when division goes on the back of the variable and - isn't detected by the above
				(str variable "/" "(" coefficient ")")
				token))))		 		

(defn guard-for-zero [type-of-number gen-state weightLow weightHigh double-precision]
	(if (= type-of-number "long")
		(loop [n (nextLong gen-state weightLow weightHigh)] ;case random number generated is long
			(if (not (zero? n))
				n
				(recur (nextLong gen-state weightLow weightHigh))))
		(loop [n (nextDouble gen-state weightLow weightHigh double-precision)] ;case of double
			(if (not (zero? n))
				n
				(recur (nextDouble gen-state weightLow weightHigh double-precision))))))		
		
;this function may put a coeffient to a variable, the operator between the variable and the coeffient is * or /
;notice that for the generated coefficients we use the input precision, for the others (which determine chance) we don't		 
(defn add-coefficient-to-linear-equation-variable [gen-state weightLow weightHigh double-precision variable-name]
	(let [chance-to-put-coeffient 0.7
		 chance-for-long-coeffient 0.5
		 chance-for-multiplication 0.6
		 chance-for-division-on-right-side-of-variable 0.5
		 put-or-not? (if (< (nextDouble gen-state 0 1 2) chance-to-put-coeffient) true false)
		 put-long? (if (< (nextDouble gen-state 0 1 2) chance-for-long-coeffient) true false)
		 mult-or-not? (if (< (nextDouble gen-state 0 1 2) chance-for-multiplication) true false)]
			(if put-or-not? ;true means put
				(if put-long? ;true means put long
					(if mult-or-not? ;true means mult
						(if-negative-coefficient-wrap-with-parenthesses (str (guard-for-zero "long" gen-state weightLow weightHigh double-precision) "*" variable-name))
						(if-negative-coefficient-wrap-with-parenthesses (str variable-name "/" (guard-for-zero "long" gen-state weightLow weightHigh double-precision))))
					(if mult-or-not?
						(if-negative-coefficient-wrap-with-parenthesses (str (guard-for-zero "double" gen-state weightLow weightHigh double-precision) "*" variable-name))
						(if-negative-coefficient-wrap-with-parenthesses (str variable-name "/" (guard-for-zero "double" gen-state weightLow weightHigh 2)))))
				variable-name)))		 

(defn which-variables-will-appear-in-equation [gen-state equation-name previous-equation-name equation-variable-names equation-size]
	(let [equation-variable-names-as-set (set equation-variable-names)
		 equation-variable-names-without-equation-name-and-previous-equation-name-as-set (disj equation-variable-names-as-set equation-name previous-equation-name)
		 equation-variable-names-without-equation-name-and-previous-equation-name (into [] equation-variable-names-without-equation-name-and-previous-equation-name-as-set)] ;convert back to list as since now everything is handled as list
		  (cond
			(= equation-size 1) (conj [] equation-name)
			(= equation-size 2) (conj [] equation-name previous-equation-name)
			:else (let [starting-vector [] ;in this let the [equation-name other-variables previous-equation-name] is built, it's ugly but -> ->> didn't fit and concat was considered a bad choice
					   with-equation-name (conj starting-vector equation-name)
					   with-other-variables (apply conj with-equation-name (take (- equation-size 2) (repeatedly #(choose-random-operator gen-state equation-variable-names-without-equation-name-and-previous-equation-name))))
					   with-previous-equation-name (conj with-other-variables previous-equation-name)]
					   with-previous-equation-name))))	

(defn get-previous-equation-name [equation-name equation-variable-names]
	(let [equation-variable-names-as-vector (vec equation-variable-names)
		  index-of-equation-name (.indexOf equation-variable-names-as-vector equation-name)
		  index-of-previous-equation-name (if (< (dec index-of-equation-name) 0)
												(dec (count equation-variable-names))
												(dec index-of-equation-name))]
		(equation-variable-names-as-vector index-of-previous-equation-name)))
				
(defn linear-equation-generator [gen-state equation-name equation-variable-names equation-size operators weightLow weightHigh double-precision]
	(let [first-operator (choose-random-operator gen-state ["" "-"]) ;"" because "+" is redudant when in front
		 rest-operators (take (dec (count equation-variable-names)) (repeatedly #(choose-random-operator gen-state operators))) ;dec because we took the first one right above
		 previous-equation-name (get-previous-equation-name equation-name equation-variable-names)
		 variables-in-equation (which-variables-will-appear-in-equation gen-state equation-name previous-equation-name equation-variable-names equation-size)
		 coefficients-to-equation-variable-names (map #(add-coefficient-to-linear-equation-variable gen-state weightLow weightHigh double-precision %) variables-in-equation)
		 expression (interleave (conj rest-operators first-operator) coefficients-to-equation-variable-names)] ;this conj will put the first operator on the front as 'rest operators' is a list
		 (apply str expression)))		

(defn unwrap-from-parens [equation]
	(if (and (clojure.string/starts-with? equation "(") (clojure.string/ends-with? equation ")"))
		(subs equation 1 (dec (count equation)))
		equation))		 
		 
(defn wrap-into-abs [term]
	(str "abs(" term ")"))	

(defn linear-equation-generator-for-non-linear [gen-state equation-variable-names operators weightLow weightHigh double-precision]
	(let [first-operator (choose-random-operator gen-state ["" "-"]) ;"" because "+" is redudant when in front
		 rest-operators (take (dec (count equation-variable-names)) (repeatedly #(choose-random-operator gen-state operators))) ;dec because we took the first one right above
		 coefficients-to-equation-variable-names (map #(add-coefficient-to-linear-equation-variable gen-state weightLow weightHigh double-precision %) equation-variable-names)
		 expression (interleave (conj rest-operators first-operator) coefficients-to-equation-variable-names)] ;this conj will put the first operator on the front as 'rest operators' is a list
		 (apply str expression)))	
	
(defn populate-field [gen-state variable-name equation-variable-names weightLow weightHigh double-precision]
	(let [max-variables-in-field 3
		 field-in-last-place-of-equation? (if (= (last equation-variable-names) variable-name) true false) ;if true it means that this variable name MUST exist in the filed in order to create the cyclic teamming strategy		 
		 chosen-variable-names (if field-in-last-place-of-equation?
								(distinct (conj (take (dec max-variables-in-field) (repeatedly #(choose-random-operator gen-state equation-variable-names))) variable-name)) ;distinct as we dont want to have several times the same variable in the field
								(distinct (take max-variables-in-field (repeatedly #(choose-random-operator gen-state equation-variable-names)))))
		 equation (linear-equation-generator-for-non-linear gen-state chosen-variable-names ["-" "+"] weightLow weightHigh double-precision)]
		 equation))
	
(defn add-coefficient-to-non-linear-equation-variable [gen-state weightLow weightHigh double-precision variable-name previous-operator single-argument-functions double-argument-functions equation-variable-names]
	(let [chance-to-put-coeffient 0.7
		 chance-for-number 0.8
		 chance-for-long-coeffient 0.5
		 chance-for-single-argument-function 0.5
		 put-or-not? (if (< (nextDouble gen-state 0 1 2) chance-to-put-coeffient) true false)
		 put-number? (if (< (nextDouble gen-state 0 1 2) chance-for-number) true false)
		 put-long? (if (< (nextDouble gen-state 0 1 2) chance-for-long-coeffient) true false)
		 put-single-arg? (if (< (nextDouble gen-state 0 1 2) chance-for-single-argument-function) true false)
		 operator-in-case-of-number (choose-random-operator gen-state ["*" "**" "/"])
		 function-in-case-of-single (choose-random-operator gen-state single-argument-functions)
		 function-in-case-of-multiple (choose-random-operator gen-state double-argument-functions)
		 new-term (if put-or-not?
					(if put-number?
						(if put-long?
							(cond
								(= operator-in-case-of-number "*") (if-negative-coefficient-wrap-with-parenthesses (str (guard-for-zero "long" gen-state weightLow weightHigh double-precision) "*" variable-name))
								(= operator-in-case-of-number "/") (if-negative-coefficient-wrap-with-parenthesses (str variable-name "/" (guard-for-zero "long" gen-state weightLow weightHigh double-precision)))
								(= operator-in-case-of-number "**") (if-negative-coefficient-wrap-with-parenthesses (str variable-name "**" (guard-for-zero "long" gen-state 0 (Math/abs (- weightHigh weightLow)) double-precision))) ;in case of power we want the base to be the variable name and the exponent a positive number between 0-(weightHigh-weightLow)
								:else (println "something went wrong")) 
							(cond
								(= operator-in-case-of-number "*") (if-negative-coefficient-wrap-with-parenthesses (str (guard-for-zero "double" gen-state weightLow weightHigh double-precision) "*" variable-name))
								(= operator-in-case-of-number "/") (if-negative-coefficient-wrap-with-parenthesses (str variable-name "/" (guard-for-zero "double" gen-state weightLow weightHigh double-precision)))
								(= operator-in-case-of-number "**") (if-negative-coefficient-wrap-with-parenthesses (str variable-name "**" (guard-for-zero "double" gen-state 0 (Math/abs (- weightHigh weightLow)) double-precision)))
								:else (println "something went wrong"))) ;then choose operation
						(if put-single-arg?
							(cond
								(= function-in-case-of-single "abs") (str "abs(" (populate-field gen-state variable-name equation-variable-names weightLow weightHigh double-precision) ")")
								(= function-in-case-of-single "exp") (str "exp(" (populate-field gen-state variable-name equation-variable-names weightLow weightHigh double-precision) ")")
								(= function-in-case-of-single "sin") (str "sin(" (populate-field gen-state variable-name equation-variable-names weightLow weightHigh double-precision) ")")
								(= function-in-case-of-single "cos") (str "cos(" (populate-field gen-state variable-name equation-variable-names weightLow weightHigh double-precision) ")")
								(= function-in-case-of-single "atan") (str "atan(" (populate-field gen-state variable-name equation-variable-names weightLow weightHigh double-precision) ")")
								(= function-in-case-of-single "sqrt") (str "sqrt(abs(" (populate-field gen-state variable-name equation-variable-names weightLow weightHigh double-precision) "))")
								(= function-in-case-of-single "ln") (str "ln(abs(" (populate-field gen-state variable-name equation-variable-names weightLow weightHigh double-precision) ") +" (nextLong gen-state 1 (+ (Math/abs weightLow) (Math/abs weightHigh))) ")")
								:else (println "something went wrong"))
							(cond
								(= function-in-case-of-multiple "min") (str "min(" (populate-field gen-state variable-name equation-variable-names weightLow weightHigh double-precision) ", " (populate-field gen-state variable-name equation-variable-names weightLow weightHigh double-precision) ")")
								(= function-in-case-of-multiple "max") (str "max(" (populate-field gen-state variable-name equation-variable-names weightLow weightHigh double-precision) ", " (populate-field gen-state variable-name equation-variable-names weightLow weightHigh double-precision) ")")
								:else (println "something went wrong"))))
					variable-name)] ;dont put coefficient
		(if (and (= previous-operator "**") (not= (apply str (take 3 new-term)) "abs"))  ;In case the term evaluates to negative and the term before the ** is zero it will create an exception, which is avoided in that way
			(wrap-into-abs new-term)												;The second part of the predicate checks if the term is already inside an abs, if it is it doesn't wrap it
			new-term)))		 
						
(defn non-linear-equation-generator [gen-state equation-name equation-variable-names equation-size operators single-argument-functions double-argument-functions weightLow weightHigh double-precision]
	(let [first-operator (choose-random-operator gen-state ["" "-"])
		 rest-operators (take (dec (count equation-variable-names)) (repeatedly #(choose-random-operator gen-state operators)))
		 operators-between-terms (conj rest-operators first-operator) ;their count is the same as the terms, as there is also an operator in the front of the first term after the '='
		 previous-equation-name (get-previous-equation-name equation-name equation-variable-names)
		 variables-in-equation (which-variables-will-appear-in-equation gen-state equation-name previous-equation-name equation-variable-names equation-size)
		 coefficients-to-equation-variable-names (map #(add-coefficient-to-non-linear-equation-variable gen-state weightLow weightHigh double-precision % %2 single-argument-functions double-argument-functions variables-in-equation) variables-in-equation operators-between-terms)
		 expression (interleave (conj rest-operators first-operator) coefficients-to-equation-variable-names)]
		(apply str expression)))
		 
;according to our simulator specification with #		 
(defn put-initial-value-to-equation [equation gen-state initial-value-low initial-value-high]
	(str equation "#" (nextLong gen-state initial-value-low initial-value-high)))

(defn put-name-to-expression [equation-name expression]
	(str equation-name "'=" expression))	

(defn create-equations [linear? gen-state teams equation-size operators-for-linear weightLow weightHigh initial-value-low initial-value-high double-precision operators-for-non-linear non-linear-single-argument-functions non-linear-double-argument-functions]
	(let [expressions (flatten
						(if linear?
							(map (fn [team]
								(map #(linear-equation-generator gen-state % team equation-size operators-for-linear weightLow weightHigh double-precision) team))
							teams)
							(map (fn [team]
								(map #(non-linear-equation-generator gen-state % team equation-size operators-for-non-linear non-linear-single-argument-functions non-linear-double-argument-functions weightLow weightHigh double-precision) team))
							teams)))
		 expressions-with-names (map #(put-name-to-expression % %2) (flatten teams) expressions)
		 expressions-with-names-and-init-vals (map #(put-initial-value-to-equation % gen-state initial-value-low initial-value-high) expressions-with-names)]
		expressions-with-names-and-init-vals))			

(defn define-equation-size [max-equation-size number-of-equations teams]
	(let [min-team-size (apply min (map count teams))]
		(cond
			(and (= max-equation-size 1) (not= (count teams) number-of-equations)) 2 ;as an edge case
			(> max-equation-size min-team-size) min-team-size
			:else max-equation-size)))
	
(defn system-generator [seed weightLow weightHigh initial-value-low initial-value-high double-precision number-of-equations number-of-teams linear? max-equation-size]
	(let [gen-state (generator-state seed)
		 alphabet (map #(str (char %) "_") (range 97 123)) ;in ascci 97 is 'a' and 123 'z', then underscore is put in order not to have name collisions when letter combinaitons exist e.g ln, sin ect
		 letter-combinations (take (inc number-of-equations) (combo/subsets alphabet)) ;inc is used because the subsets contain the empty list which will be removed next, and so we need another one to have a count of 'number-of-equations'
		 variable-names (map #(apply str %) (rest letter-combinations)) ;rest to remove the empty list which is first
		 operators-for-linear ["+" "-"]
		 operators-for-non-linear ["+" "-" "**" "*"]
		 non-linear-single-argument-functions ["abs" "sqrt" "exp" "ln" "sin" "cos" "atan"]
		 non-linear-double-argument-functions ["min" "max"]		 
		 teams (create-teams variable-names number-of-equations number-of-teams)
		 equation-size (define-equation-size max-equation-size number-of-equations teams)
		 equations (create-equations linear? gen-state teams equation-size operators-for-linear weightLow weightHigh initial-value-low initial-value-high double-precision operators-for-non-linear non-linear-single-argument-functions non-linear-double-argument-functions)]
		 (vec equations))) ;put to vector in order to have random access and fully realize it


		 