(ns milkshakes.core
  (:require clojure.set)
  (:use [clojure.java.io :as cjio])
  (:use [clojure.string  :as cstr :only [split split-lines]])
  (:use [clojure.pprint  :as pp   :only [pprint]])
  (:gen-class))

(defn get-current-directory []
  (. (java.io.File. ".") getCanonicalPath))

(defn do-per-line [f]
  (with-open [rdr (cjio/reader "input.txt")]
    (doseq [line (line-seq rdr)]
      (f line))))

(defmacro dbg [x]
  `(let [x# ~x]
     (do (println '~x "~~>" x#)
         x#))) 

;;; and pretty-printing version

(defmacro ppdbg [x]
  `(let [x# ~x]
     (do (println "----------------")
         (clojure.pprint/pprint '~x)
         (println "~~>")
         (clojure.pprint/pprint x#)
         (println "----------------")
         x#)))

(defn parse-cases [acc ls]
  (let [flavors          (read-string (first ls))
        c                (read-string (fnext ls))
        raw-likes        (take c (drop 2 ls))
        nested-likes     (map #(cstr/split % #"\s")     raw-likes)
        nested-int-likes (map #(map read-string %)      nested-likes)
        likes            (map #(partition 2 (drop 1 %)) nested-int-likes)
        rems             (drop (+ 2 c) ls)
        ]
    (let [ans (conj acc {:flavors flavors :all-likes (vec likes)})]
      (if (not= '() rems)
        (recur ans rems)
        ans))))

(defn parse-lines [ls]
  (let [ncases (read-string (first ls))]
    (dbg ncases)
    (parse-cases [] (rest ls))
    ))

(defn case->soln [a]
  (let [N      (:flavors a)
        ;; In the batches, 1 means malted and 0 means unmalted. Keep
        ;; two arrays, one denoting whether malted is acceptable, the
        ;; other denoting whether unmalted is acceptable. Examine
        ;; customers one at a time. If a customer declares exclusive
        ;; like for unmalted or malted, mark the corresponding slot in
        ;; the other array with a zero. If we ever find that both
        ;; boxes for any flavor are zero, we're done and throw
        ;; "IMPOSSIBLE." Otherwise, at the end, accumulate batches
        ;; using malted only when unmalted is zero.
        m-acceptable  (int-array N 1)
        u-acceptable  (int-array N 1)
        ;; Index of malt-acceptable is 0, of unmalt-acceptable 1.
        acceptables   [m-acceptable u-acceptable]
        likes  (:all-likes a)
        ;; Each like is a list of a flavor index and a malted-or-not.
        ;; Make the leaves into vectors.
        mikes  (map #(map vec %) likes)
        ;; The singleton likes are important, since they imply that
        ;; the other alternative is unacceptable.
        temp0  (try (doseq [mike mikes]
                      (if (= (count mike) 1)
                        (let [[flav malt?] (first mike)
                              other (- 1 malt?)
                              flav-idx (dec flav)
                              ]
                          (if (= 0 (aget (acceptables malt?) flav-idx))
                              (throw (Exception. "IMPOSSIBLE")))
                          (aset-int (acceptables other) flav-idx 0)
                          )
                        ))
                    (catch Exception e (.getMessage e))
                    )
        ;; The doubletons are important since, if one of their likes
        ;; is unacceptable, their other like is forced, which means
        ;; that the malt-opposite of their other like is unaccetable.
        temp1 (try (doseq [mike mikes]
                     (if (= (count mike) 2)
                       (let [[f1 m1?] (first mike)
                             o1       (- 1 m1?)
                             f1i      (dec f1)
                             [f2 m2?] (second mike)
                             o2       (- 1 m2?)
                             f2i      (dec f2)
                             
                             ])
                       )
                     )
                    (catch Exception e (.getMessage e))
                   )
        nikes  (map #(map (fn [[flav malt]]
                            {:flav flav, :malt malt})
                          %)
                    mikes)
        ]
    (ppdbg (or temp0 {:m (vec m-acceptable)
                      :u (vec u-acceptable)}))
    ))

(defn -main
  "Basic husk for programming problems."
  [& args]
  ;; work around dangerous default behaviour in Clojure
  (alter-var-root #'*read-eval* (constantly false))
  (println (get-current-directory))

  (let [input (slurp
               ;"/Users/rebcabin/Downloads/C-large-practice.in"
               "input.txt"
               )
        _     (spit "input.txt" input)
        lines (cstr/split-lines input)
        answs (map-indexed
               (fn [i l]
                 (str "Case #" (inc i) ": "
                      (identity l)
                      "\n")
                 )
               (let [parsed 
                     (parse-lines lines)]
                 (map case->soln parsed))
               )
        ]
    (with-open [w (cjio/writer "output.txt")]
      (doseq [line answs] (.write w line)))
    ))

