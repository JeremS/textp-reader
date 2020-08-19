#!/usr/bin/env bash

clojure -A:clj:cljs:dev:nrepl:piggie:test -m nrepl.cmdline --middleware "[cider.piggieback/wrap-cljs-repl]"