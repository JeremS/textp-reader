(ns fr.jeremyschoffen.textp.alpha.reader.docs.core
  (:require
    [fr.jeremyschoffen.textp.alpha.lib.compilation :refer [emit!]]
    [fr.jeremyschoffen.textp.alpha.doc.core :as doc]
    [fr.jeremyschoffen.textp.alpha.doc.markdown-compiler :as compiler]
    [fr.jeremyschoffen.textp.alpha.reader.core :as textp-reader]
    [fr.jeremyschoffen.mbt.alpha.utils :as u]))

(u/pseudo-nss
  project)


(defn emit-example! [resource]
  (compiler/emit-block! "text" resource))


(defn emit-read-example! [resource]
  (let [resource (with-out-str
                   (-> resource
                       textp-reader/read-from-string
                       clojure.pprint/pprint))]
    (compiler/emit-block! "clojure" resource)))


(defmethod compiler/emit-tag! [::compiler/md :example-block]
  [node]
  (let [{:keys [resource]} (get node :attrs)
        resource (doc/slurp-resource* resource)]
    (emit-example! resource)
    (compiler/emit-newline!)

    (emit! "reads as:")
    (compiler/emit-newline!)

    (emit-read-example! resource)
    (compiler/emit-newline!)))


(def readme-src "fr/jeremyschoffen/textp/alpha/reader/docs/readme/README.md.tp")


(defn make-readme! [{wd ::project/working-dir
                     maven-coords ::project/maven-coords
                     git-coords ::project/git-coords}]
  (spit (u/safer-path wd "README.md")
        (doc/make-document readme-src
                           {:project/maven-coords maven-coords
                            :project/git-coords git-coords})))


(comment
  (-> readme-src
      doc/slurp-resource
      doc/read-document)
  (doc/make-document readme-src {}))

