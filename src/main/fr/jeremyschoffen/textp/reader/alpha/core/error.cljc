(ns fr.jeremyschoffen.textp.reader.alpha.core.error
  (:require
    [fr.jeremyschoffen.textp.reader.alpha.grammar :as g]
    [net.cgrand.macrovich :as macro :include-macros true]
    [meander.epsilon :as m]))

;; TODO: See how it might be possible when displaying an error to show the starting position of a grammatical rule failing instead of just the end.
(def error-msgs
  {g/plain-text
   "The parser expected plain text without any \"◊\",\"\\\"."

   g/escaping-char
   "The parser expected a backslash here."

   g/any-char
   "The parser expected anything... It shouldn't bug on that..."

   g/text-verbatim
   "The parser expected any text ended with \"!◊\"."

   g/text-comment
   "The parser expected any text ended with \"/◊\"."

   g/text-symbol
   "The parser expected a clojure symbol here, name for a tag."

   g/text-e-value
   "The parser expected to parse a clojure symbol followed by \"|◊\" here."

   g/text-e-code
   "The parse expected well formatted clojure code ended with \")◊\"."

   g/text-t-clj
   "The parser expected well formatted clojure code mixed with tags as if in a literal vector."

   g/text-t-clj-str
   "The parser expected text found inside a clojure string end with an \"\"\"."

   g/tag-plain-text
   "The parser expected plain text without any \"◊\",\"\\\",  \"}\"."

   g/text-spaces
   "The parser expect spaces only."})

(defn get-normalized-error-data [e]
  (letfn [(extract-reason [insta-reason]
            (m/search insta-reason
                      (m/scan (and ?x {:tag :regexp
                                       :expecting ?regex}))
                      (get error-msgs ?regex ?x)

                      (m/scan (and ?x {:tag (m/pred (complement =) :regexp)}))
                      ?x

                      (m/scan (and ?x (m/pred (complement map?))))
                      ?x))]

    (m/match (ex-data e)
             {:type :reader-failure
              :text ?text
              :failure ?f
              :region #:instaparse.gll{:start-index  ?start-index
                                       :end-index    ?end-index
                                       :start-line   ?start-line
                                       :start-column ?start-column
                                       :end-line     ?end-line
                                       :end-column   ?end-column}}
             {:type :reader-failure
              :start-index  ?start-index
              :end-index    ?end-index
              :start-line   ?start-line
              :start-column ?start-column
              :end-line     ?end-line
              :end-column   ?end-column
              :text ?text
              :reason (ex-message ?f)}


             {:type :parser-failure
              :failure {:index  ?index
                        :reason ?reason
                        :line   ?line
                        :column ?column
                        :text   ?text}}
             {:type :parser-failure
              :end-index  ?index
              :end-line   ?line
              :end-column ?column
              :text   ?text
              :reason (extract-reason ?reason)}

             _
             (throw e))))


(defn normalize-error [e]
  (let [data (get-normalized-error-data e)
        final-data (merge data
                          (-> e
                              ex-data
                              (select-keys #{:type :failure})))]
    (ex-info (ex-message e)
             final-data)))


(defn print-base-error-msg [e]
  (println (ex-message e))
  (let [{:keys [type reason failure]} (ex-data e)]
    (if (= type :reader-failure)
      (println reason)
      (do
        (println "Doesn't respect the following:")
        (doseq [x reason]
          (println x))
        (println)
        (println "Instaparse error:")
        (println failure)))))


(defn print-region-position [error]
  (let [{:keys [type
                start-index end-index
                start-line start-column
                end-line end-column]} (ex-data error)]
    (println "Region:")
    (if (= type :reader-failure)
      (do
        (println "From indexes" start-index "to" end-index)
        (println "line" start-line "column" start-column
                 "to line" end-line "column" end-column))
      (do
        (println "Around index" end-index
                 "line" end-line "column" end-column)))))


(defn print-failed-text [e]
  (println "Failed text:")
  (println (-> e ex-data :text)))


(macro/replace
  #?(:clj {}
     :cljs {*out* *print-fn*
            *err* *print-err-fn*})
  (defn print-error-msg [e]
    (binding [*out* *err*]
      (println "--------------------------------------------------------------------------------")
      (print-base-error-msg e)
      (println "--------------------------------------------------------------------------------")
      (print-region-position e)
      (println "--------------------------------------------------------------------------------")
      (print-failed-text e)
      (println "--------------------------------------------------------------------------------"))))



(defn handle-read-error [e]
  (let [e (normalize-error e)]
    (print-error-msg e)
    (throw e)))