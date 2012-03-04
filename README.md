# watchtower

A simple file/directory watcher library

## Usage

```clojure
(watcher ["src/" "resources/"]
  (rate 50) ;; poll every 50ms
  (file-filter ignore-dotfiles) ;; add a filter for the files we care about
  (file-filter (extensions :clj :cljs)) ;; filter by extensions
  (on-change #(println "files changed: " %)))
```

## License

Copyright (C) 2011 Chris Granger

Distributed under the Eclipse Public License, the same as Clojure.
