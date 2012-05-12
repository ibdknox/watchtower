# watchtower

A simple file/directory watcher library

## Usage

```clojure
(use 'watchtower.core)
(watcher ["src/"]
  (rate 50) ;; poll every 50ms
  (file-filter ignore-dotfiles) ;; add a filter for the files we care about
  (file-filter (extensions :clj :cljs)) ;; filter by extensions
  (notify-on-start? true)   ;; Optional, determines whether notifications are made  
                            ;; on the existing files folder are there when the watcher loads.
                            ;; The default is true to preserve legacy code, but can be turned
                            ;; off if we only care about changes that happen when watchtower 
                            ;; is running.
  (on-modify    #(println "files modified: " %)); Optional
  (on-delete    #(println "files deleted: " %)); Optional
  (on-add       #(println "files added: " %))); Optional
```

## License

Copyright (C) 2011 Chris Granger

Distributed under the Eclipse Public License, the same as Clojure.
