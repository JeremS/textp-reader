(ns fr.jeremyschoffen.textp.alpha.reader.build
  (:require
    [clojure.spec.test.alpha :as st]
    [fr.jeremyschoffen.mbt.alpha.core :as mbt-core]
    [fr.jeremyschoffen.mbt.alpha.default :as mbt-defaults]
    [fr.jeremyschoffen.mbt.alpha.utils :as u]
    [fr.jeremyschoffen.mbt.alpha.mbt-style :as mbt-build]
    [fr.jeremyschoffen.textp.alpha.reader.docs.core :as docs]
    [build :refer [token]]))

(u/pseudo-nss
  git.commit
  maven
  maven.credentials
  project
  project.license
  versioning)


(def conf (mbt-defaults/config
            {::project/name "textp-reader"
             ::project/author "Jeremy Schoffen"
             ::project/git-url "https://github.com/JeremS/textp-reader"

             ::maven/group-id 'fr.jeremyschoffen
             ::versioning/major :alpha
             ::versioning/scheme mbt-defaults/git-distance-scheme

             ::maven/server mbt-defaults/clojars
             ::maven/credentials {::maven.credentials/user-name "jeremys"
                                  ::maven.credentials/password token}

             ::project/licenses [{::project.license/name "Eclipse Public License - v 2.0"
                                  ::project.license/url "https://www.eclipse.org/legal/epl-v20.html"
                                  ::project.license/distribution :repo
                                  ::project.license/file (u/safer-path "LICENSE")}]}))


(defn generate-docs! [conf]
  (-> conf
      (u/assoc-computed ::project/maven-coords mbt-defaults/deps-make-maven-coords
                        ::project/git-coords mbt-defaults/deps-make-git-coords)
      (assoc-in [::git/commit! ::git.commit/message] "Generated the docs.")
      (mbt-defaults/generate-then-commit!
        (u/do-side-effect! docs/make-readme!))))

(u/spec-op generate-docs!
           :deps [mbt-build/merge-last-version
                  mbt-defaults/deps-make-maven-coords
                  mbt-defaults/deps-make-git-coords]
           :param {:req [::git/repo
                         ::maven/artefact-name
                         ::maven/group-id
                         ::project/git-url
                         ::project/version
                         ::versioning/scheme
                         ::versioning/tag-base-name]
                   :opt [::maven/classifier
                         ::versioning/tag-base-name
                         ::versioning/version]})


(defn bump! []
  (-> conf
      mbt-build/bump-project!
      generate-docs!))


(st/instrument `[mbt-build/bump-project!
                 generate-docs!
                 mbt-core/clean!
                 mbt-build/install!
                 mbt-build/deploy!])
(comment
  (bump!)
  (mbt-core/clean! conf)
  (mbt-build/build! conf)
  (mbt-build/install! conf)


  (mbt-build/deploy! conf))