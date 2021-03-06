(ns garden-css-modules.runtime
  (:require [garden.core :refer [css]]))

;; Note: The contents of this namespace are lifted pretty directly
;; from https://github.com/matthieu-beteille/cljs-css-modules. All
;; credit for the initial implementation goes there.

(def injected-styles (atom {}))

(defn- update-style! [element style]
  (set! (.-innerHTML element) style))

(defn- create-style-element! [style id]
  (let [head (.-head js/document)
        element (.createElement js/document "style")]
    (assert (some? head)
            "An head element is required in the dom to inject the style.")
    (.appendChild head element)
    (update-style! element style)
    (swap! injected-styles assoc id element)))

(defn inject-style! [style ns name]
  (let [id (symbol (str ns "-" name))
        injected-style (id @injected-styles)]
    (if injected-style
      (update-style! injected-style style)
      (create-style-element! style id))))
