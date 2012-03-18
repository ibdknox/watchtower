(ns watchtower.core
  (:require [clojure.java.io :as io]))

(def ^{:dynamic true} *last-pass* nil)

;;*****************************************************
;; Watcher map creation 
;;*****************************************************

(defn watcher* 
  "Create a watcher map that can later be passed to (watch)"
  [dirs]
  (let [dirs (if (string? dirs)
               [dirs]
               dirs)]  
    {:dirs dirs
     :filters []}))

(defn file-filter 
  "Add a filter to a watcher. A filter is just a function that takes in a
  java.io.File and returns truthy about whether or not it should be included."
  [w filt]
  (update-in w [:filters] conj filt))

(defn rate 
  "Set the rate of polling."
  [w r]
  (assoc w :rate r))

(defn on-change 
  "When files are changed, execute a function that takes in a seq of the changed
  file objects."
  [w func]
  (update-in w [:on-change] conj func))

;;*****************************************************
;; Watcher execution  
;;*****************************************************

(defn default-filter [f] 
  (.isFile f))

(defn modified? [f]
  (> (.lastModified f) @*last-pass*))

(defn get-files [dirs filters]
  (let [dir-files (mapcat #(-> % io/file file-seq) dirs)]
    (filter filters dir-files)))

(defn updated?-fn [dirs filters]
  (let [filters (conj filters default-filter)
        final-filter #(every? (fn [func] (func %)) filters)]
    (fn []
      (let [files (get-files dirs final-filter)
            results (seq (doall (filter modified? files)))]
        (when results 
          (reset! *last-pass* (System/currentTimeMillis)))
        results))))

(defn changed-fn [funcs]
  (fn [files]
    (doseq [f funcs]
      (f files))))

(defn compile-watcher [{:keys [filters rate dirs on-change]}]
  {:rate rate
   :updated? (updated?-fn dirs filters)
   :changed (changed-fn on-change)})

(defn watch 
  "Execute a watcher map"
  [w]
  (let [{:keys [updated? rate changed]} (compile-watcher w)]
    (binding [*last-pass* (atom 0)]
      (while true
        (Thread/sleep rate)
        (when-let [changes (updated?)] 
          (changed changes))))))

(defmacro watcher 
  "Create a watcher for the given dirs (either a string or coll of strings), applying
  the given transformations.

  Transformations available: (rate) (file-filter) (on-change)"
  [dirs & body]
  `(let [w# (-> ~dirs
                (watcher*)
                ~@body)]
     (future (watch w#))))

;;*****************************************************
;; file filters
;;*****************************************************

(defn ignore-dotfiles 
  "A file-filter that removes any file that starts with a dot."
  [f]
  (not= \. (first (.getName f))))

(defn extensions 
  "Create a file-filter for the given extensions."
  [& exts]
  (let [exts-set (set (map name exts))]
    (fn [f]
      (let [fname (.getName f)
            idx (.lastIndexOf fname ".")
            cur (if-not (neg? idx) (subs fname (inc idx)))]
        (exts-set cur)))))
