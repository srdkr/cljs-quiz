(ns service.quiz
  (use clojure.java.io))


;(with-open [rdr (clojure.java.io/reader "resources/baza.txt")]
;         (count (line-seq rdr)))



;(with-open [rdr (reader "resources/baza.txt")]
 ; (vec (doseq [line (line-seq rdr)]
  ;  line)))



(def db (with-open [rdr (reader "resources/baza.txt")]
  (vec (filter not-empty (line-seq rdr)))))



(defn parse-line [line]
  (let [x (clojure.string/split line #"\|" )]
    [(second x) (first x)]))


 (def quiz-list (reduce conj [] (map parse-line db)))

(def quiz-db (reduce conj {} (map parse-line db)))

(defn next-question [& _] (rand-nth quiz-list))


(defn prompt
  [word step]
  (let [length (count word)
        hintsize (if (nil? step) 0
                   (if (> step 3) (* 3 (quot length 5))
                      (quot (* step length) 5)))]
    (apply str (.substring word 0 hintsize)
           (repeat (- length hintsize)  "*"))))

;(mask-word (first elem) 2)

