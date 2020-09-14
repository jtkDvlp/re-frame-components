(ns jtk-dvlp.re-frame.forms
  (:require
   [re-frame.core :as rf]))


;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Dispatch-Helpers

(defn dispatch
  [event-vec]
  (fn [e]
    (js-invoke e "preventDefault")
    (rf/dispatch event-vec)))

(defn dispatch-value
  [event-vec]
  (fn [js-event]
    (->> js-event
         (.-target)
         (.-value)
         (conj event-vec)
         (rf/dispatch-sync))))


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
