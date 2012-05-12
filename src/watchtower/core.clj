(ns watchtower.core
  (:require [clojure.java.io :as io]
            [clojure.set :as st]))

(def ^{:dynamic true} *last-pass* nil)
(def ^{:dynamic true} *last-file-set* nil)

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
     :filters []
     :notify-on-start? false
     :rate 100
     :on-add []
     :on-modify []
     :on-delete []}))

(defn file-filter 
  "Add a filter to a watcher. A filter is just a function that takes in a
  java.io.File and returns truthy about whether or not it should be included."
  [w filt]
  (update-in w [:filters] conj filt))

(defn rate 
  "Set the rate of polling."
  [w r]
  (assoc w :rate r))

(defn notify-on-start? 
  "Determines whether changes are broadcast at the start of the watch. The default is true."
  [w flag]
  (assoc w :notify-on-start? flag))

(defn on-add 
  "When files are create, execute a function that takes in a seq of the modified
  file objects."
  [w func]
  (update-in w [:on-add] conj func))

(defn on-change 
  "When files are modified, execute a function that takes in a seq of the modified
  file objects. This is added to preserve legacy code."
  [w func]
  (update-in w [:on-modify] conj func))

(defn on-modify 
  "When files are modified, execute a function that takes in a seq of the modified
  file objects."
  [w func]
  (update-in w [:on-modify] conj func))

(defn on-delete 
  "When files are deleted, execute a function that takes in a seq of the deleted
  file objects."
  [w func]
  (update-in w [:on-delete] conj func))

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
            files-set (apply hash-set files)
            comparison-set (st/union files-set @*last-file-set*)
            added  (seq (st/difference comparison-set @*last-file-set*))
            deleted  (seq (st/difference comparison-set files-set))
            modified (seq (doall (filter modified? files)))]
        (when (or added deleted)
          (reset! *last-file-set* files-set))
        (when (or added modified deleted) 
          (reset! *last-pass* (System/currentTimeMillis)))
        {:additions added :modifications modified :deletions deleted}))))

(defn changed-fn [funcs]
  (fn [files]
    (doseq [f funcs]
      (f files))))

(defn compile-watcher [{:keys [filters rate dirs notify-on-start? 
                               on-modify on-delete on-add]}]
  {:rate rate
   :notify-on-start? notify-on-start?
   :updated? (updated?-fn dirs filters)
   :added  (changed-fn on-add)
   :modified (changed-fn on-modify)
   :deleted  (changed-fn on-delete)})

(defn get-initial-files [{:keys [dirs filters]}]
  (let [filters (conj filters default-filter)
        final-filter #(every? (fn [func] (func %)) filters)]
      (get-files dirs final-filter)))

(defn watch 
  "Execute a watcher map"
  [w]
  (let [{:keys [updated? notify-on-start? rate modified deleted added]} (compile-watcher w)]
    (binding [*last-pass* (atom (if notify-on-start? 
                                  0 
                                  (System/currentTimeMillis)))
              *last-file-set* (atom (if notify-on-start? 
                                      #{}
                                      (apply hash-set (get-initial-files w))))]
      (while true
        (Thread/sleep rate)
        (let [changes (updated?)]
          (when-let [additions (changes :additions)] 
            (added additions))
          (when-let [modifications (changes :modifications)] 
            (modified modifications))
          (when-let [deletions (changes :deletions)] 
            (deleted deletions)))))))

(defmacro watcher 
  "Create a watcher for the given dirs (either a string or coll of strings), applying
  the given transformations.

  Transformations available: (rate) (file-filter) (on-modify)"
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
