(ns textp.reader.alpha.core
  (:require
    #?(:clj [clojure.tools.reader :as r]
       :cljs [cljs.tools.reader :as r])
    [clojure.walk :as walk]
    [net.cgrand.macrovich :as macro :include-macros true]
    [instaparse.core :as insta]
    [meander.epsilon :as m]

    [textp.reader.alpha.grammar :as g]
    [textp.reader.alpha.core.error :as error]))


(defn parse
  "Wrapper around the parser from textp.reader.grammar adding error handling."
  [text]
  (let [parsed (g/parser text)]
    (when (insta/failure? parsed)
      (throw (ex-info "Parser failure."
                      {:type :parser-failure
                       :failure (insta/get-failure parsed)})))
    (insta/add-line-and-column-info-to-metadata text parsed)))


(def ^:dynamic *parse-region*
  "Stores the parse regions given by instaparse when clojurizing the parse tree."
  {})

(macro/replace
  #?(:clj {}
     :cljs {Exception :default})
  (defn read-string*
    "Wrapping of clojure(script)'s read-string function for use in our reader."
    [s]
    (try
      (r/read-string s)
      (catch Exception e
        (let [region *parse-region*]
          (throw
            (ex-info "Reader failure."
                     {:type :reader-failure
                      :text s
                      :region region
                      :failure e})))))))


(declare clojurize)


(defmulti clojurize* :tag)


(defn extract-tags [content]
  "Replaces the tags by generated unique symbols and creates a mapping from
  those symbols to the replaced tag data."
  (let [env (volatile! (transient {}))
        form (volatile! (transient []))]

    (doseq [v content]
      (if (string? v)
        (vswap! form conj! v)
        (let [sym (gensym "tag")]
          (vswap! env assoc! sym v)
          (vswap! form conj! (str " " sym " ")))))

    {:env (-> env deref persistent!)
     :form (->  form
                deref
                persistent!
                (->> (apply str)))}))


(defn inject-clojurized-tags
  "Walks the clojurized block and replaces placeholder symbols by the clojurized content."
  [form env]
  (walk/prewalk (fn [v]
                  (if-let [t (and (symbol? v)
                                  (get env v))]
                    (clojurize t)
                    v))
                form))

(defn clojurize-mixed
  "The basic content of an embeded code block is a sequence of strings and tags. These tags can't be read by
  the clojure reader.

  To turn that block into clojure data, the trick is to replace the tags by place-holder strings that will be read as
  symbols. We can then use the clojure(script) reader on the result. Next we walk the code that's now data and replace
  those symbols with the clojurized tags."
  [content]
  (let [{:keys [env form]} (extract-tags content)
        form (read-string* form)]
    (inject-clojurized-tags form env)))


(defmethod clojurize* :doc [node]
  (mapv clojurize (:content node)))


(defmethod clojurize* :verbatim [form]
  (-> form :content first))


(defmethod clojurize* :comment [_]
  "")


(defmethod clojurize* :embedded-value [form]
  (-> form :content first read-string*))


(defmethod clojurize* :embedded-code [form]
  (clojurize-mixed (:content form)))


(defmethod clojurize* :tag [form]
  (seq (mapv clojurize (:content form))))


(defmethod clojurize* :tag-name [form]
  (-> form :content first read-string*))


(defmethod clojurize* :tag-args-txt [form]
  (update form
          :content
          #(mapv clojurize %)))


(defmethod clojurize* :tag-args-clj [form]
  (update form :content clojurize-mixed))


(defn clojurize
  "Function that turns a textp parse tree to data that clojure can eval."
  [form]
  (if (string? form)
    form
    (binding [*parse-region* (meta form)]
      (clojurize* form))))


(macro/replace
  #?(:clj {}
     :cljs {Exception :default})
  (defn read-from-string [text]
    (try
      (let [parsed (parse text)]
        (clojurize parsed))
      (catch Exception e
        (error/handle-read-error e)))))


(comment
  (def ex1
    "

 ◊h1{Addition}

 The addition adds numbers together...

 ◊(+ 1 2 3)◊

 The Addition is associative:

 ◊ul{
   ◊li{◊(+ 1 (+ 2 3))◊}
   ◊li{◊(+ (+ 1 2) 3)◊}
 }

 are equivalent.

 ◊div{\\} \\1}")
  (read-from-string ex1)

  (def ex2
    "Hello my name is ◊em{Some}{Name}.
     We can embed code ◊(+ 1 2 3)◊.
     We can even embed tags in code:
     ◊(call ◊text{◊em{Me!}})◊

     Tags ins tags args:
     ◊toto[:arg1 ◊em{toto} :arg2 2 :arg3 \"arg 3\"].

     The craziest, we can embed ad nauseam:

     ◊(defn template [x]
        ◊div[:bonkers ◊div{some text}]
        {
          the value x: ◊|x|◊
          the value x++: ◊(inc x)◊
        })◊")
  (parse ex2)
  (read-from-string ex2))
