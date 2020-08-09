(ns fr.jeremyschoffen.textp.alpha.reader.utils
  (:require [net.cgrand.macrovich :as macro]))


(macro/deftime
  (defmacro get-resource [path]
    (slurp path)))