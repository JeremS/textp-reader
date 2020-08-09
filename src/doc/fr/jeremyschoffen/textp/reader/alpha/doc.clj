(ns fr.jeremyschoffen.textp.reader.alpha.doc
  (:require
    [clojure.java.io :as io]
    [textp.compile.alpha.core :refer [emit!]]
    [textp.lib.alpha.input :as tp-lib-input]
    [textp.doc.alpha.markdown-compiler :as compiler]
    [textp.eval.alpha.core :as textp-eval]
    [textp.eval.alpha.env.clojure]
    [fr.jeremyschoffen.textp.reader.alpha.core :as textp-reader]))




(defn evil [doc]
  (textp-eval/eval-doc-in-temp-ns doc textp.eval.alpha.env.clojure/default))


(defn read-resource [r]
  (-> r
      io/resource
      slurp))


(defn emit-resource! [resource]
  (compiler/emit-block! "text" resource))


(defn emit-read-resource! [resource]
  (let [resource (with-out-str
                   (-> resource
                       textp-reader/read-from-string
                       clojure.pprint/pprint))]
    (compiler/emit-block! "clojure" resource)))


(defmethod compiler/emit-tag! :example-block
  [node]
  (let [{:keys [resource]} (get node :attrs)
        resource (read-resource resource)]
    (emit-resource! resource)
    (compiler/emit-newline!)

    (emit! "reads as:")
    (compiler/emit-newline!)

    (emit-read-resource! resource)
    (compiler/emit-newline!)))


(defn make-doc! [resource dest]
  (-> resource
      io/resource
      slurp
      textp-reader/read-from-string
      evil
      compiler/doc->md

      (->> (spit dest))))


(defn make-readme! [project-coords]
  (tp-lib-input/with-input {:project/coords project-coords}
    (make-doc! "fr/jeremyschoffen/textp/reader/alpha/doc/readme/README.md.tp" "README.md")))

(comment

  (make-readme! '{textp/reader {:mvn/version "1"}})

  (-> "fr/jeremyschoffen/textp/reader/alpha/doc/readme/README.md.tp"
      io/resource
      slurp
      textp-reader/read-from-string
      evil
      compiler/doc->md))


