(ns jtk-dvlp.re-frame.clock
  (:require
   [re-frame.core :as rf]

   [cljs-time.core :as time]
   [cljs-time.format :as time-format]))


;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Co-effects

(rf/reg-cofx
 :clock/now
 (fn [coeffects _]
   (assoc coeffects :clock/now (time/now))))


;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Events

(rf/reg-event-fx :clock/tick
  [(rf/inject-cofx :clock/now)]
  (fn [{:keys [:clock/now db]} _]
    (let [timing
          (get-in db [:clock/db :timing] 5000)]

      {:db
       (assoc-in db [:clock/db :now] now)

       :dispatch-later
       [{:ms timing
         :dispatch [:clock/tick]}]})))


;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Subscriptions

(rf/reg-sub :clock/db
  (fn [{:keys [:clock/db]}]
    db))

(rf/reg-sub :clock/now
  :<- [:clock/db]
  (fn [{:keys [now]}]
    now))

(rf/reg-sub :clock/ago
  :<- [:clock/now]
  (fn [now [_ timestamp]]
    (when (<= timestamp now)
      (let [interval
            (time/interval timestamp now)]

        (->> [[time/in-days "days"]
              [time/in-hours "hours"]
              [time/in-minutes "mins"]
              [time/in-seconds "secs"]]
             (map (fn [[in-unit unit]]
                    [(in-unit interval) unit]))
             (filter (comp #(> % 0) first))
             (first))))))

(rf/reg-sub :clock/formatter
  :<- [:clock/db]
  (fn [{:keys [formatter]}]
    (or formatter (time-format/formatter-local "d. MMM yyyy HH:mm"))))

(rf/reg-sub :clock/datetime
  :<- [:clock/formatter]
  (fn [formatter [_ timestamp]]
    (->> timestamp
         (time/to-default-time-zone)
         (time-format/unparse formatter))))


;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Views

(defn ago
  [attrs timestamp]
  (let [[period unit]
        @(rf/subscribe [:clock/ago timestamp])

        datetime
        @(rf/subscribe [:clock/datetime timestamp])

        attrs
        (assoc attrs :title datetime)]

    (if period
      [:span attrs (str period " " unit " ago")]
      [:span attrs "now"])))
