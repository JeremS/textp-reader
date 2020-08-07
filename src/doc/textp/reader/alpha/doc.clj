(ns textp.reader.alpha.doc
  (:require
    [clojure.java.io :as io]
    [textp.compile.alpha.core :refer [emit!]]
    [textp.doc.alpha.markdown-compiler :as compiler]
    [textp.eval.alpha.core :as textp-eval]
    [textp.eval.alpha.env.clojure]
    [textp.reader.alpha.core :as textp-reader]))




(defn evil [doc]
  (textp-eval/eval-doc-in-temp-ns doc textp.eval.alpha.env.clojure/default))


(defn read-resource [r]
  (-> r
      io/resource
      slurp))


(defn emit-newline! [] (emit! "\n"))


(defn emit-block! [type text]
  (emit! "```" type)
  (emit-newline!)
  (emit! text)
  (emit-newline!)
  (emit! "```"))


(defn emit-resource! [resource]
  (emit-block! "text" resource))


(defn emit-read-resource! [resource]
  (let [resource (with-out-str
                   (-> resource
                       textp-reader/read-from-string
                       clojure.pprint/pprint))]
    (emit-block! "clojure" resource)))


(defmethod compiler/emit-tag! :example-block
  [node]
  (let [{:keys [resource]} (get node :attrs)
        resource (read-resource resource)]
    (emit-resource! resource)
    (emit-newline!)

    (emit! "reads as:")
    (emit-newline!)

    (emit-read-resource! resource)
    (emit-newline!)))


(defn make-doc! [resource dest]
  (-> resource
      io/resource
      slurp
      textp-reader/read-from-string
      evil
      compiler/doc->md

      (->> (spit dest))))

(comment
  (make-doc! "textp/reader/alpha/doc/readme/README.md.tp" "README.md")

  (-> "textp/reader/alpha/doc/readme/README.md.tp"
      io/resource
      slurp
      textp-reader/read-from-string
      evil
      compiler/doc->md))


