(ns sys-gen.linear-system-generator
	(:require [clojure.math.combinatorics :as combo]))

;Returns an obejct of type Random initialized with the given seed
;seed - long number	
(defn generator-state [seed]
	(java.util.Random. seed))

;Returns a double number in the range [lower-bound,higher-bound)
;gen-state - object of type Random
;lower-bound - double number
;higher-bound - double number	
(defn next-double [gen-state lower-bound higher-bound]
	(+ (* (.nextDouble gen-state) (- higher-bound lower-bound)) lower-bound))	

;Returns a number with specified precision
;number - double number
;precision - long number	
(defn apply-precision [number precision]
	(let [factor (Math/pow 10 precision)]
		(/ (Math/floor (* number factor)) factor)))		

;If precision is zero it casts a number to long
;number - double number
;precision - long number	
(defn cast-if-zero-precision [number precision]
	(if (zero? precision)
		(long number)
		number))		

;Creastes a random number in the range [lower-bound,higher-bound), with specified precision and if precision is 0 it casts it to long
;gen-state - object of type Random
;lower-bound - double number
;higher-bound - double number
;precision - long number	
(defn create-rnd-number [gen-state lower-bound higher-bound precision]
	(let [rnd-double (next-double gen-state lower-bound higher-bound)
		 rnd-double-with-precision (apply-precision rnd-double precision)]
		(cast-if-zero-precision rnd-double-with-precision precision)))		

;Groups the variable names into teams
;variable-names - list or vector
;number-of-equations - long number
;number-of-teams - long number		
(defn create-teams [variable-names number-of-equations number-of-teams]
    (cond 
      (<= number-of-equations number-of-teams) (partition 1 variable-names)
      (> number-of-equations number-of-teams) (if (> (mod number-of-equations number-of-teams) 0)
                               (let [initial-division (partition-all (quot number-of-equations number-of-teams) variable-names)]
                                 (concat
                                   (drop (mod number-of-equations number-of-teams) (take number-of-teams initial-division))
                                   (map conj initial-division (take-last (mod number-of-equations number-of-teams) variable-names))))
                               (partition-all (quot number-of-equations number-of-teams) variable-names))))		  			 		

;Returns the equation name from which an equation will depend from
;equation-name - string
;equation-variable-names - list or vector containing strings						   
(defn get-previous-equation-name [equation-name equation-variable-names]
	(let [equation-variable-names-as-vector (vec equation-variable-names)
		  index-of-equation-name (.indexOf equation-variable-names-as-vector equation-name)
		  index-of-previous-equation-name (if (< (dec index-of-equation-name) 0)
												(dec (count equation-variable-names))
												(dec index-of-equation-name))]
		(equation-variable-names-as-vector index-of-previous-equation-name)))

;Returns a list of the variables that will appear in an equation
;equation-name - string
;previous-equation-name - string
;equation-variable-names - list or vector containing strings
;equation-size - long number	
(defn which-variables-will-appear [equation-name previous-equation-name equation-variable-names equation-size]
	(let [grab-bag-without-previous-equation-name (disj (set equation-variable-names) previous-equation-name)
		 variables-other-than-previous-equation-name (vec (take (dec equation-size) grab-bag-without-previous-equation-name))] ;if equation size is 1 it will be empty
		(conj variables-other-than-previous-equation-name previous-equation-name)))

;Returns a list of the operators between terms as strings, the first operator is considered 'empty' as a plus in the front has no meaning
;equation-size - long number		
(defn operators-between-terms [equation-size]
	(conj 
		(take (dec equation-size) (repeat "+"))
		""))

;If a term starts with minus, it wraps it in parenthesses
;term - string		
(defn if-neg-wrap-with-parens [term]
	(if (= (first term) \-)
		(str "(" term ")")
		term))	

;Creates a term
;gen-state - object of type Random	
;variable-in-term - string
;weigh-low - double number
;weight-high - double number
;precision - long number	
(defn create-term [gen-state variable-in-term weight-low weight-high precision]
	(let [coefficient (create-rnd-number gen-state weight-low weight-high precision)
		 term (str coefficient "*" variable-in-term)]
		(if-neg-wrap-with-parens term)))	

;Generates a linear equation
;gen-state - object of type Random
;equation-name - string
;equation-variable-names - list or vector containing strings	
;equation-size - long number
;weigh-low - double number
;weight-high - double number
;precision - long number			
(defn linear-equation-generator [gen-state equation-name equation-variable-names equation-size weight-low weight-high precision]
	(let [previous-equation-name (get-previous-equation-name equation-name equation-variable-names)
		 variables-appearing (which-variables-will-appear equation-name previous-equation-name equation-variable-names equation-size)
		 operators-between-terms (operators-between-terms equation-size)
		 terms (map #(create-term gen-state % weight-low weight-high precision) variables-appearing)
		 expression (interleave operators-between-terms terms)]
		 (apply str expression)))	

;Puts an initial value to an equation in the range [initial-value-low initial-value-high) and specified precision
;equation - string 		
;gen-state - object of type Random 
;initial-value-low - double number
;initial-value-high - double number
;precision - long number
(defn put-initial-value-to-equation [equation gen-state initial-value-low initial-value-high precision]
	(str equation "#" (create-rnd-number gen-state initial-value-low initial-value-high precision)))

;Puts the name of equation in front of it
;equation-name - string
;expression - string 	
(defn put-name-to-expression [equation-name expression]
	(str equation-name "'=" expression))	

;Returns the equations. Acts as helper function to create the expressions and put initia values and names
;gen-state - object of type Random
;teams - list of lists
;equation-size - long number
;weigh-low - double number
;weight-high - double number
;initial-value-low - double number
;initial-value-high - double number
;precision - long number	
(defn create-equations [gen-state teams equation-size weight-low weight-high initial-value-low initial-value-high precision]
	(let [expressions (flatten	
						(map (fn [team]
								(map #(linear-equation-generator gen-state % team equation-size weight-low weight-high precision) team))
							teams))	
		 expressions-with-names (map #(put-name-to-expression % %2) (flatten teams) expressions)
		 expressions-with-names-and-init-vals (map #(put-initial-value-to-equation % gen-state initial-value-low initial-value-high precision) expressions-with-names)]
		expressions-with-names-and-init-vals))			

;Checks the equation size for correctness
;max-equation-size - long number
;number-of-equations - long number	
;teams - list of lists	
(defn define-equation-size [max-equation-size number-of-equations teams]
	(let [min-team-size (apply min (map count teams))]
		(cond
			(and (= max-equation-size 1) (not= (count teams) number-of-equations)) 2 ;as an edge case
			(> max-equation-size min-team-size) min-team-size
			:else max-equation-size)))

;Returns a list of equations as strings
;seed - long number
;weigh-low - double number
;weight-high - double number
;initial-value-low - double number
;initial-value-high - double number
;precision - long number	
;number-of-equations - long number
;number-of-teams - long number
;max-equation-size - long number			
(defn linear-system-generator [seed weight-low weight-high initial-value-low initial-value-high precision number-of-equations number-of-teams max-equation-size]
	(let [gen-state (generator-state seed)
		 alphabet (map #(str (char %) "_") (range 97 123)) ;in ascci 97 is 'a' and 123 'z', then underscore is put in order not to have name collisions when letter combinaitons exist e.g ln, sin ect
		 letter-combinations (rest (take (inc number-of-equations) (combo/subsets alphabet))) ;rest is used to leave out the empty list which is first, inc is used because the subsets contain the empty list which will be removed next, and so we need another one to have a count of 'number-of-equations'
		 variable-names (map #(apply str %) letter-combinations)
		 teams (create-teams variable-names number-of-equations number-of-teams)
		 equation-size (define-equation-size max-equation-size number-of-equations teams)
		 equations (create-equations gen-state teams equation-size weight-low weight-high initial-value-low initial-value-high precision)]
		 equations))


		 