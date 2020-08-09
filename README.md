


# Textp Reader

This project provides a reader for a clojure dialect similar to the dialect
of the [Racket](https://racket-lang.org/) language used in [Pollen](https://github.com/mbutterick/pollen)

## Instalation
Deps coords:
```clojure
#:textp{reader #:mvn{:version "0"}}
```
Lein coords:
```clojure
[textp/reader "0"]
```

## Usage
The idea is to have a dialect of clojure that inverses the priority of code over text in a file.
Text is primary, code is secondary and introduced with special syntactic constructs.

Here the reader considers everything plain text unless specified otherwise. We could say that the
reader is in text mode by default. In this mode the character `◊` is the only special character.
It announces that a special syntactic construct is going to be used.

### Embedded clojure
The first kind of syntax is embedded clojure:

```text
Clojure code (list expressions) can be embedded inside the text:
◊(def x "some value")◊
A symbol can be embeded this way ◊|x|◊. It's basically sugar for ◊(do x)◊
```
reads as:
```clojure
["Clojure code (list expressions) can be embedded inside the text:\n"
 (def x "some value")
 "\nA symbol can be embeded this way "
 x
 ". It's basically sugar for "
 (do x)]

```



### Special text
```text
We can have ◊!verbatim!◊ and ◊/commented/◊ text
```
reads as:
```clojure
["We can have " "verbatim" " and " "" " text"]

```



### Tags
Like in [Pollen](https://github.com/mbutterick/pollen) we have a tag construct. We use tags similar to what is found in [Pollen](https://github.com/mbutterick/pollen) and [Scribble](https://docs.racket-lang.org/scribble/index.html)
We start a tag with the '◊' character followed by the tag's name and then the tag's arguments.

There are 2 types of arguments:
- clojure arguments enclosed in brackets, its understood that the enclosed text is clojure code.
 It effectively gives use vectors of clojure arguments.
- text arguments enclosed in braces. The text enclosed there is normal text that can include any of the syntax
constructs presented before.

For instance we could have html links.
```text
◊html/a[:href "www.url.com"]{link}.
```
reads as:
```clojure
[(html/a
  {:tag :tag-args-clj, :content [:href "www.url.com"]}
  {:tag :tag-args-txt, :content ["link"]})
 "."]

```


The way the reader is implemented allows for recursion inside syntactic constructs. When in text mode we can use
embedded code and tags. In clojure mode we can use tags giving us access again to text mode vi text arguments.

This is valid for the reader.
```text
◊(defn template [x]
   ◊div  [:class ◊text{class1 class2} ]
   {
     Some text and crazy recursion: ◊(str (+ 2 x) ◊text{times.})◊
   })◊
```
reads as:
```clojure
[(defn
  template
  [x]
  (div
   {:tag :tag-args-clj,
    :content
    [:class (text {:tag :tag-args-txt, :content ["class1 class2"]})]}
   {:tag :tag-args-txt,
    :content
    ["\n     Some text and crazy recursion: "
     (str (+ 2 x) (text {:tag :tag-args-txt, :content ["times."]}))
     "\n   "]}))]

```


Assuming you have a `div` and a `text` function that can deal with this data, you can eval that
in a repl. I personnaly find that pretty neat...

### Special cases
Note that the previous example shows that we can have any manner of spaces between tag arguments.

This creates the case where I might want to use a opening bracket directly after a tag.
There are several corner case like this one. We then allow for escaped characters in the syntax.
```text
◊br \[

the [ character won't count as the opening of clojure arguments. Also ◊!\◊!◊ doesn't start any syntactic rule.
```
reads as:
```clojure
[(br)
 " "
 "["
 "\n\nthe [ character won't count as the opening of clojure arguments. Also "
 "\\◊"
 " doesn't start any syntactic rule."]

```


