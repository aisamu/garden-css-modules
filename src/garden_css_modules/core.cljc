(ns garden-css-modules.core
  #?(:clj
       (:require
        [cljs.env]
        [clojure.string :as string]
        [garden.core :refer [css]]
        [garden.stylesheet :refer [at-media at-keyframes]]))
  #?(:cljs
     (:require
      [clojure.string :as string]
      [garden.stylesheet :refer [at-media at-keyframes]] ; TODO: Handle keyframes and @media
      [garden-css-modules.runtime :refer [inject-style!]])))

(defn- get-namespace [] `~(str *ns*))

(defmacro prod? []
  (if cljs.env/*compiler*
    (= :advanced (get-in @cljs.env/*compiler* [:options :optimizations]))
    (string/starts-with? "prod" (System/getenv "ENV"))))

(defn- hash-part [part]
  (if (string/starts-with? part ".")
    (let [name (str part
                    "__"
                    (string/replace (get-namespace) #"\." "_"))
          hashed (hash name)]
      (if (prod?)
        (str ".s" hashed)
        (str name "-" hashed)))
    part))

(defn- hash-selector [selector]
  (let [parts (string/split (name selector) #"(?=\.)|(?=#)|(?=\[)|(?=:)|(?=\s)|(?=&)")
        hashed (reduce
                #(merge %1 {%2 (hash-part %2)})
                {}
                parts)]
   {:selector (keyword (apply str (map hashed parts)))
    :names (reduce-kv
            (fn [acc part hash]
              (merge acc
                 (if (string/starts-with? part ".")
                   {(keyword (string/replace (name part) #"^\." ""))
                    (string/replace hash #"^\." "")}
                   {})))
            {}
            hashed)}))

(defn modularize
  ([& garden]
   (let [garden (if (= 1 (count garden))
                    (first garden)
                    (into [] garden))
         selector (first garden)
         style (when (map? (second garden)) (second garden))
         children (into [] (if style (rest (rest garden)) (rest garden)))]
    (if (vector? selector)
      (let [modules (map modularize garden)]
        {:styles (into [] (map #(% :styles) modules))
         :names (reduce #(merge %1 (%2 :names)) {} modules)})
      (let [{:keys [styles names]}
            (when (first children) (modularize (into [] children)))
            hash (hash-selector selector)]
        (when (first children))
        {:styles (into [] (remove nil?
                            (concat
                             [(hash :selector) style]
                             styles)))
         :names (merge (hash :names) names)})))))

(defmacro defstyle
  [style-id & styles]
  (let [module (apply modularize styles)]
    (if (boolean (:ns &env))
      `(do
        (~(symbol "garden-css-modules.runtime" "inject-style!")
          (css {:pretty-print? (not (prod?))} (~module :styles))
          (get-namespace)
          ~(name style-id))
        (def ~style-id ~(module :names)))
      `(do
        (def ~style-id {:names ~(module :names)
                          :styles ~(module :styles)})))))
