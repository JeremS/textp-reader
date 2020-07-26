(ns textp.reader.alpha.grammar
  #?(:cljs (:require-macros [textp.reader.grammar :refer [def-regex make-lexer]]))
  (:require
    [clojure.set]
    [net.cgrand.macrovich :as macro :include-macros true]
    [instaparse.core :as insta]
    [instaparse.combinators :as instac]
    [medley.core :as medley]

    [lambdaisland.regal :as regal]))



;; ---------------------------------------------------------------------------------------------------------------------
;; utils
;; ---------------------------------------------------------------------------------------------------------------------

(macro/deftime
  (defmacro def-regex
    ([n xeger-expr]
     `(def-regex ~n "" ~xeger-expr))
    ([n doc xeger-expr]
     `(def ~n ~doc (regal/regex ~xeger-expr))))

  (defmacro make-lexer [& regexes]
    `(into {}
           ~(vec (for [r regexes]
                   (let [kw (-> r name keyword)]
                     `[~kw (instac/regexp ~r)]))))))


(defmethod regal/-regal->ir [:*? :common] [[_ & rs] opts]
  (regal/quantifier->ir "*?" rs opts))

;; ---------------------------------------------------------------------------------------------------------------------
;; Lexer
;; ---------------------------------------------------------------------------------------------------------------------
(def diamond \◊)
(def escaper "\\")

;; classic delimiters
(def parens "()")
(def brackets "[]")
(def braces "{}")
(def double-quote "\"")

(def all-delimitors (sorted-set parens brackets braces double-quote))

(def non-special [:not diamond])
(def anything [:class :whitespace :non-whitespace])

(def end-verbatim       [:cat \!  diamond])
(def end-comment        [:cat \/  diamond])
(def end-embedded-value [:cat \|  diamond])
(def end-embeded-code   [:alt diamond [:cat \)  diamond]]) ;; here we also had the case of a tag inside a clojure section


(def normal-text [:not diamond escaper])


;; ---------------------------------------------------------------------------------------------------------------------
;; plain text
(def-regex plain-text
  "Text to be interpreted as plain text, neither clojure code, nor special blocks of text.
  Basically any character excluding diamond and backslash which have special meaning."
  [:* normal-text])


(def-regex escaping-char
  "The backslash used to escaped characters in plain text."
  escaper)


(def-regex any-char
  "Regex that recognizes any character."
  anything)


;; ---------------------------------------------------------------------------------------------------------------------
;; verbatim & comment text blocks
(def-regex text-verbatim
  "Regex used to parse text inside a verbatim block.
  All characters allowed terminated by \"!◊\""
  [:cat [:*? anything] [:lookahead end-verbatim]])


(def-regex text-comment
  "Regex used to parse text inside a comment block.
  All characters allowed terminated by \"/◊\""
  [:cat [:*? anything] [:lookahead end-comment]])


;; ---------------------------------------------------------------------------------------------------------------------
;; embedded

(def ns-end \/)
(def ns-separator \.)
(def macro-reader-char \#)

(def symbol-regular-char-set
  (into #{:whitespace diamond ns-end} all-delimitors))

(def symbol-regular-char
  "Characters that are always forbidden in symbol names:
  - spaces
  - diamond char since it starts another grammatical rule
  - delimitors: parens, brackets, braces  and double quotes.
  - `/` since it the special meaning of separating the namespace from the symbol name.
  - `.` since it has the special meaning of separating symbol names."
  (into [:not ] symbol-regular-char-set))


(def symbol-first-char
  "In the case of the first character of a symbol name, there are more forbidden chars:
  - digits aren't allowed as first character
  - the macro reader char `#` isn't allowed either."
  (into [:not]
        (conj symbol-regular-char-set
              :digit
              macro-reader-char)))


(defn make-simple-symbol-regex
  "Regex for simple symbols without namespaces.
  The character repetition is parameterized to allow for reluctant repetition."
  [rep]
  [:cat symbol-first-char [rep symbol-regular-char]])


(def symbol-ns-part
  "Regex for the ns name of a symbol, parses dot separated names until
  a final name."
  (make-simple-symbol-regex :*))


(defn make-complex-symbol-regex
  "Regex for a full symbol name with namespace. Parse an optional ns name followed by
  the character `/` then a simple symbol. The repetition for the character of the symbol name
  is parameterized to allow fo reluctant repetition."
  [rep]
  (let [ns-part [:cat [:capture symbol-ns-part] ns-end]
        name-part [:capture (make-simple-symbol-regex rep)]]
    [:cat [:? ns-part] name-part]))


(def-regex text-symbol
  "Regex used when parsing a symbol in the case of tag names."
  (make-complex-symbol-regex :*))


(def-regex text-e-value
  "Regex used when parsing a symbol in the case of embedded values. It basically
  is the same as `text-symbol` except for the use of reluctant repetition for the
  symbol name and the use of a lookahead at the end to search for the end of an embedded
  value block."
  [:cat (make-complex-symbol-regex :*?)
        [:lookahead end-embedded-value]])


(def-regex text-e-code
  "Regex used when parsing parsing text in embedded code."
  [:cat [:*? non-special]
        [:lookahead end-embeded-code]])


;; ---------------------------------------------------------------------------------------------------------------------
;; Tags
(def text-t-clj-non-special [:not diamond brackets double-quote escaper])
(def text-escaped-char [:cat escaper anything])


(def-regex text-t-clj
  "Regex used when parsing a the text inside a clojure argument to a tag.
  Can be anything but the chars:
  - `◊`: diamond will start a new grammatical  rule
  - `[]`, brackets: theses characters will start a new grammatical rule
  - `\"`: double quote will start a new grammatical rule

  Allows for the forbidden char to appear when escaped with a backslash.
  "
  [:* [:alt text-t-clj-non-special text-escaped-char]])


(def text-t-clj-str-non-special [:not double-quote escaper])


(def-regex text-t-clj-str
  "The text inside a clojure string. Can be anything but the char:
  - `\"`: double quote will close the string

  Allows for the forbidden chars to appear when escaped with a backslash.
  "
  [:* [:alt text-t-clj-str-non-special
            text-escaped-char]])


(def text-t-txt-non-special (conj normal-text "}"))


(def-regex tag-plain-text
  "The text found inside curly braces in tags. Can be anything but the chars:
  - `◊`: diamond will start a new grammatical rule
  - `}`: right curly brace closes the text arg to the tag
  - '\\' : backslash with start an escaped char grammatical rule

  Allows for the forbidden chars to appear when escaped with a backslash."
  [:* text-t-txt-non-special])


(def-regex text-spaces
  "Spaces found inbetween tag args."
  [:* :whitespace])


(def lexer
  (make-lexer
    plain-text
    escaping-char
    any-char

    text-verbatim
    text-comment

    text-symbol
    text-e-value
    text-e-code
    text-t-clj
    text-t-clj-str
    tag-plain-text
    text-spaces))


;; ---------------------------------------------------------------------------------------------------------------------
;; Grammar
;; ---------------------------------------------------------------------------------------------------------------------
(def text-g
  (instac/ebnf
    "
    text          = plain-text | escaped-char
    escaped-char = <escaping-char> any-char
    "))

(def text-g-masked
  #{:text
    :escaped-char})

(def verbatim-g
  (instac/ebnf
    "
    verbatim = <'◊!'> text-verbatim <'!◊'>
    "))

(def comment-g
  (instac/ebnf
    "
    comment = <'◊/'> text-comment <'/◊'>
    "))


(def embedded-g
  (instac/ebnf
    "
    embedded       = embedded-code | embedded-value
    embedded-code  = <'◊'> '(' text-e-code (tag |text-e-code )* ')' <'◊'>
    embedded-value = <'◊|'> text-e-value                            <'|◊'>
    "))


(def embedded-g-masked
  #{:embedded})

(def tag-g
  (instac/ebnf
    "
    tag            = <'◊'> tag-name  tag-args* !tag-args
    tag-name       = text-symbol
    tag-args       = <text-spaces> (tag-args-clj | tag-args-txt)

    tag-args-clj   = sqbrk-enclosed
    sqbrk-enclosed =  '['  (clj-txt | sqbrk-enclosed | tag)* ']'
    <clj-txt>      =  (text-t-clj | string)*
    <string>       =  '\"' text-t-clj-str '\"'

    tag-args-txt   = brk-enclosed
    brk-enclosed   = <'{'>  (tag-text | special)*         <'}'>
    tag-text       = tag-plain-text | escaped-char
    escaped-char   = <escaping-char> any-char
    "))

(def tag-g-masked
  #{:tag-args
    :tag-text
    :embedded
    :sqbrk-enclosed
    :brk-enclosed})

(def general-g
  (instac/ebnf
    "
  doc     = (text | special)*
  special = block | clojure
  block   = verbatim | comment
  clojure = embedded | tag
  "))

(def general-g-masked
  #{:special
    :block
    :clojure})

;; ---------------------------------------------------------------------------------------------------------------------
;; Assembling the parser
;; ---------------------------------------------------------------------------------------------------------------------
(defn hide-all [g]
  (medley/map-vals instac/hide-tag g))


(defn hide-tags [g tags]
  (-> g
      (select-keys tags)
      hide-all
      (->> (merge g))))


(def grammar-masked
  (clojure.set/union text-g-masked
                     embedded-g-masked
                     tag-g-masked
                     general-g-masked))


(def grammar-rules
  (merge
    (hide-all lexer)
    text-g
    verbatim-g
    comment-g
    embedded-g
    tag-g
    general-g))


(def grammar
  (hide-tags grammar-rules grammar-masked))


(def parser
  (insta/parser grammar
                :start :doc
                :output-format :enlive))


(comment
  (def ex1
    "Hello my name is ◊em{Jeremy}{Schoffen}.
     We can embed code ◊(+ 1 2 3)◊.
     We can even embed tags in code:
     ◊(call ◊text{◊em{Me!}})◊

     Tags ins tags args:
     ◊toto[:arg1 ◊em{toto} :arg2 2 :arg3 \"arg 3\"].

     The craziest, we can embed ad nauseam:

     ◊(defn template [x]
        ◊div
        {
          the value x: ◊|x|◊
          the value x++: ◊(inc x)◊
        })◊")
  (parser ex1)

  (def ex2
    "Some text
    ◊div
    [:class \"aside\"]
    {
      some other text
    }

    ◊auto-close   \\[]

    ◊|@l|◊")

  (= (parser ex2)
     (g/parser ex2)))
