(ns fr.jeremyschoffen.textp.reader.alpha.utils
  (:require [net.cgrand.macrovich :as macro]))


(macro/deftime
  (defmacro get-resource [path]
    (slurp path)))