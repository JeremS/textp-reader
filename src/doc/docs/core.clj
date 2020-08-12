(ns docs.core
  (:require
    [fr.jeremyschoffen.textp.alpha.lib.compilation :refer [emit!]]
    [fr.jeremyschoffen.textp.alpha.doc.core :as doc]
    [fr.jeremyschoffen.textp.alpha.doc.markdown-compiler :as compiler]
    [fr.jeremyschoffen.textp.alpha.reader.core :as textp-reader]))


(defn emit-example! [resource]
  (compiler/emit-block! "text" resource))


(defn emit-read-example! [resource]
  (let [resource (with-out-str
                   (-> resource
                       textp-reader/read-from-string
                       clojure.pprint/pprint))]
    (compiler/emit-block! "clojure" resource)))


(defmethod compiler/emit-tag! :example-block
  [node]
  (let [{:keys [resource]} (get node :attrs)
        resource (doc/slurp-resource* resource)]
    (emit-example! resource)
    (compiler/emit-newline!)

    (emit! "reads as:")
    (compiler/emit-newline!)

    (emit-read-example! resource)
    (compiler/emit-newline!)))


(def readme-src "docs/readme/README.md.tp")
(def readme-dest "README.md")


(defn make-readme! [maven-coords]
  (spit readme-dest
        (doc/make-document readme-src {:project/maven-coords maven-coords})))


(comment
  (-> readme-src
      doc/slurp-resource
      doc/read-document)

  (make-readme! '{fr.jeremyschoffen/textp-doc {:mvn/version "0"}}))

