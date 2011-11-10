(ns watchtower.core
  (:require [clojure.java.io :as io]))

(def ^{:dynamic true} *last-pass* nil)

;;*****************************************************
;; Watcher map creation 
;;*****************************************************

(defn watcher* [dirs]
  (let [dirs (if (string? dirs)
               [dirs]
               dirs)]  
    {:dirs dirs
     :filters []}))

(defn file-filter [w filt]
  (update-in w [:filters] conj filt))

(defn rate [w r]
  (assoc w :rate r))

(defn on-change [w func]
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

(defn watch [w]
  (let [{:keys [updated? rate changed]} (compile-watcher w)]
    (binding [*last-pass* (atom 0)]
      (while true
        (Thread/sleep rate)
        (when-let [changes (updated?)] 
          (changed changes))))))

(defmacro watcher [dirs & body]
  `(let [w# (-> ~dirs
                (watcher*)
                ~@body)]
     (future (watch w#))))

;;*****************************************************
;; file filters
;;*****************************************************

(defn ignore-dotfiles [f]
  (not= \. (first (.getName f))))

(defn extensions [& exts]
  (let [exts-set (set (map name exts))]
    (fn [f]
      (let [fname (.getName f)
            cur (subs fname (inc (.lastIndexOf fname ".")))]
        (exts-set cur)))))
