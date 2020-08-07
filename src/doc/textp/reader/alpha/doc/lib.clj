(ns textp.reader.alpha.doc.lib
  (:require
    [textp.lib.alpha.core :as textp-lib]
    [textp.doc.alpha.core :as textp-doc]))


(textp-lib/def-xml-tag example-block)


(def racket (textp-doc/make-link "https://racket-lang.org/" "Racket"))
(def pollen (textp-doc/make-link "https://github.com/mbutterick/pollen" "Pollen"))
(def scribble (textp-doc/make-link "https://docs.racket-lang.org/scribble/index.html" "Scribble"))
