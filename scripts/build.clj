(ns build
  (:require
    [clojure.spec.test.alpha :as st]
    [fr.jeremyschoffen.mbt.alpha.core :as mbt-core]
    [fr.jeremyschoffen.mbt.alpha.default :as mbt-defaults]
    [fr.jeremyschoffen.mbt.alpha.utils :as u]
    [docs.core :as docs]))


(st/instrument
  `[mbt-core/deps-make-coord])


(def specific-conf (sorted-map
                     :project/author "Jeremy Schoffen"
                     :maven/group-id 'fr.jeremyschoffen.textp
                     :versioning/major :alpha
                     :versioning/scheme mbt-defaults/simple-scheme))


(def conf (mbt-defaults/make-conf specific-conf))


(defn make-docs! [conf]
  (let [maven-coords (mbt-core/deps-make-coord conf)]
    (docs/make-readme! maven-coords)))


(defn new-milestone! [conf]
  (-> conf
      (mbt-defaults/generate-before-bump! (u/side-effect! make-docs!))
      mbt-defaults/bump-tag!))


(comment
  (-> conf
      (u/assoc-computed :project/version (comp str mbt-defaults/anticipated-next-version))
      make-docs!)
  (new-milestone! conf)

  (mbt-core/clean! conf)

  (mbt-defaults/build-jar! conf)
  (mbt-defaults/install! conf))