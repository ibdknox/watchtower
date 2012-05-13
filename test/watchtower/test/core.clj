(ns watchtower.test.core
  (:use [watchtower.core])
  (:use [midje.sweet]))

(fact "An example of how to create a file-watcher"
   (watcher* "tmp") => {:dirs ["tmp"] :filters []} )

(fact "An example of how get-files is used:"
  (get-files ["tmp"] default-filter) => seq?)


(use 'watchtower.core)
(watcher ["/Users/Chris/dev/play/courtvision/resources/public/videos"]
  (rate 100) ;; poll every 50ms
  (file-filter ignore-dotfiles) ;; add a filter for the files we care about
  (file-filter (extensions :mp4)) ;; filter by extensions
  (notify-on-start? true)   ;; Optional, determines whether notifications are made  
                            ;; on the existing files folder are there when the watcher loads.
                            ;; The default is true to preserve legacy code, but can be turned
                            ;; off if we only care about changes that happen when watchtower 
                            ;; is running.
  (on-modify    #(println "files modified: " %)); Optional
  (on-delete    #(println "files deleted: " %)); Optional
  (on-add       #(println "files added: " %))); Optional