(ns jtk-dvlp.re-frame.forms
  (:refer-clojure
   :exclude [dispatch-fn])

  (:require
   [re-frame.core :as rf]))


;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Dispatch-Helpers

(defn dispatch-fn
  [event-vec]
  (fn [e]
    (js-invoke e "preventDefault")
    (rf/dispatch event-vec)))

(defn dispatch-value-fn
  [event-vec]
  (fn [js-event]
    (->> js-event
         (.-target)
         (.-value)
         (conj event-vec)
         (rf/dispatch-sync))))


;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Events

(rf/reg-event-db :forms/reset-value
  (fn [db [_ db-path value]]
    (assoc-in db db-path value)))

(rf/reg-event-db :forms/clear-value
  (fn [db [_ db-path]]
    (assoc-in db db-path nil)))


;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Views

(defn input
  ([]
   (input {}))

  ([attrs]
   (input attrs nil))

  ([_attrs _content]

   (let [random-id
         (random-uuid)]

     (fn [{:keys [class style id type label]
          :or {type :text} :as attrs}
         value]

       (let [id
             (or id random-id)

             type-name
             (name type)

             class
             (->> (str type-name "-input")
                  (conj class))]

         [:div.input
          {:class class
           :style style
           :title label}
          (when label
            [:label.input-label
             {:for id}
             label])
          [:input.input-input
           (-> attrs
               (dissoc :class :style)
               (merge {:id id
                       :type type-name
                       :value value}))]])))))
