(ns fr.jeremyschoffen.textp.alpha.reader.core-test
  (:require
    [clojure.test :as test :refer [deftest testing is are] :include-macros true]
    [fr.jeremyschoffen.textp.alpha.reader.core :as c]
    [fr.jeremyschoffen.textp.alpha.reader.utils :as u :include-macros true]))


(def ex1-str (u/get-resource "resources-test/ex1.textp"))
(def embedding (u/get-resource "resources-test/embedding.textp"))


(def simple-form '(+ 1 2 3))
(def simple-form-textp (str \◊ simple-form \◊))

(deftest round-trips
  (is (= simple-form (first (c/read-from-string simple-form-textp)))))
