(ns watchtower.test.core
  (:use [watchtower.core])
  (:use [midje.sweet]))

(fact "An example of how to create a file-watcher"
   (watcher* "tmp") => (:dirs ["tmp"] :filters []} )

(fact "An example of how get-files is used:"
  (get-files ["tmp"] default-filter) => seq?)