(ns fr.jeremyschoffen.textp.alpha.reader.docs.tags
  (:require
    [fr.jeremyschoffen.textp.alpha.lib.tag-utils :as textp-lib]
    [fr.jeremyschoffen.textp.alpha.doc.core :as textp-doc]))


(textp-lib/def-xml-tag example-block)


(def racket (textp-doc/make-link "https://racket-lang.org/" "Racket"))
(def pollen (textp-doc/make-link "https://github.com/mbutterick/pollen" "Pollen"))
(def scribble (textp-doc/make-link "https://docs.racket-lang.org/scribble/index.html" "Scribble"))

(defn prefix-doc-path [p]
  (str "fr/jeremyschoffen/textp/alpha/reader/" p))