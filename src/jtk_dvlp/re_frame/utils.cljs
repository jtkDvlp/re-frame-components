(ns jtk-dvlp.re-frame.utils
  (:require
   [re-frame.core :as rf]))


;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Helpers

(defn dispatch
  [event-vec & args]
  (-> event-vec
      (concat args)
      (vec)
      (rf/dispatch)))

(defn dispatch-n
  [events & args]
  (doseq [event-vec events]
    (-> event-vec
        (concat args)
        (vec)
        (rf/dispatch))))

(defn debounce
  [f ms]
  (let [timer
        (atom nil)]

    (fn [& args]
      (when @timer
        (.clearTimeout js/window @timer))
      (reset! timer (.setTimeout js/window #(apply f args) ms))
      nil)))


;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Coeffects

(rf/reg-cofx
 :utils/uuid
 (fn [coeffects _]
   (assoc coeffects :utils/uuid (random-uuid))))
