# Textp Reader

This project provides a reader for a clojure dialect similar to the dialect 
of the [Racket](https://racket-lang.org/) language used in [pollen](https://github.com/mbutterick/pollen).


## Usage
The idea is to have a dialect of clojure that inverse the priority of code over text in a file. Text is primary, code is
secondary and introduced with special constructs.

The reader (think weird list reader) considers everything plain text unless specified otherwise. We could say that the 
reader is in text mode by default. In this mode the character `◊` is the only special character. 
It announces that a special syntactic construct is going to be used.

### Embedded clojure
The first kind of syntax is embeddded clojure: 

```text
Clojure code (list expressions) can be embedded inside the text:
◊(def x "some value")◊
A symbol can be embeded this way ◊|x|◊. It's basically sugar for ◊(do x)◊
```


### Special text

```text
We can have ◊!verbatim!◊ and ◊/commented/◊ text 
```

### Tags

Like in pollen we have a tag construct.We use tags simillar to what is found in pollen and scribble. 
We start a tag with the '◊' character followed byt the tags name and then the tags arguments. 

There are 2 types of arguments:
- clojure arguments enclosed in brackets, its understood that the enclosed text is clojure code.
 It effectively gives use vectors of clojure arguments.
- text arguments enclosed in braces. The text enclosed there is normal text that can include any of the syntax 
constructs presentd before.

For instance we could have html links this way: 
```text
 
◊html/a[:href "www.url.com"]{link}.  
```

The way the reader is implemented allows for recusion inside syntaxic constructs. When in text mode we can use 
embeded code and tags. In clojure mode we can use tags giving us acces again to text mode vi text arguments.

This is valid for the reader.
```text
◊(defn template [x]
   ◊div  [:class ◊text{class1 class2} ]
   {
     Some text and crazy recursion: ◊(str (+ 2 x) ◊text{ times.})◊
   })◊
```

It reads as such:
```clojure
[(defn
  template
  [x]
  (div
   {:tag :tag-args-clj, :content [:class (text {:tag :tag-args-txt, :content ["class1 class2"]})]}
   {:tag :tag-args-txt,
    :content ["\nSome text and crazy recursion: " (str (+ 2 x) (text {:tag :tag-args-txt, :content [" times."]})) "\n"]}))]
```
Assuming you have a `div` and a `text` function that can deal with this data you can eval that 
in a repl. I personnaly find that pretty neat...

### Special cases
Note that the previous example shows that we can have aby manner of spaces between tag arguments.

This also create the case where I might want to use a opening bracket directly after a tag.
There are several corner case like this one. We then allow for escaped characters in the syntax.
```text
In this case
◊br \[

the [ characteer won't count as the opening of clojure arguments. Also \◊ doesn't start any syntactic rule.
```
