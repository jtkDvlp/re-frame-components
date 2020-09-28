(ns jtk-dvlp.re-frame.notifications
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [reagent.ratom :as r-atom]

   [cljs-time.coerce :as time-coerce]

   [jtk-dvlp.re-frame.clock :as clock]))

;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Functions

(defn push
  [db {:keys [id] :as notification}]
  (assoc-in db [:notifications/db :notifications id] notification))

(defn push-info
  [db id timestamp message]
  (->> {:id id
        :type :info
        :title "Info"
        :message message
        :timestamp timestamp
        :duration 10000}
       (push db)))

(defn push-warn
  [db id timestamp message]
  (->> {:id id
        :type :warn
        :title "Warning"
        :message message
        :timestamp timestamp}
       (push db)))

(defn push-error
  [db id timestamp message]
  (->> {:id id
        :type :error
        :title "Error"
        :message message
        :timestamp timestamp}
       (push db)))

(defn push-success
  [db id timestamp message]
  (->> {:id id
        :type :success
        :title "Success"
        :message message
        :timestamp timestamp
        :duration 10000}
       (push db)))

(defn close
  [db id]
  (update-in db [:notifications/db :notifications] dissoc id))


;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Events

(rf/reg-event-fx :notifications/push
  [(rf/inject-cofx :utils/uuid)
   (rf/inject-cofx :clock/now)]
  (fn [{:keys [:utils/uuid :clock/now db]} [_ notification]]
    {:db
     (->> {:identifier uuid
           :timestamp now}
          (merge notification)
          (push db))}))

(rf/reg-event-fx :notifications/push-info
  [(rf/inject-cofx :utils/uuid)
   (rf/inject-cofx :clock/now)]
  (fn [{:keys [:utils/uuid :clock/now db]} [_ message]]
    {:db
     (push-info db uuid now message)}))

(rf/reg-event-fx :notifications/push-warn
  [(rf/inject-cofx :utils/uuid)
   (rf/inject-cofx :clock/now)]
  (fn [{:keys [:utils/uuid :clock/now db]} [_ message]]
    {:db
     (push-warn db uuid now message)}))

(rf/reg-event-fx :notifications/push-error
  [(rf/inject-cofx :utils/uuid)
   (rf/inject-cofx :clock/now)]
  (fn [{:keys [:utils/uuid :clock/now db]} [_ message]]
    {:db
     (push-error db uuid now message)}))

(rf/reg-event-fx :notifications/report-error
  [(rf/inject-cofx :utils/uuid)
   (rf/inject-cofx :clock/now)]
  (fn [{:keys [:utils/uuid :clock/now db]} [_ {:keys [displaytext message]}]]
    {:db
     (push-error db uuid now (or displaytext message))}))

(rf/reg-event-fx :notifications/push-success
  [(rf/inject-cofx :utils/uuid)
   (rf/inject-cofx :clock/now)]
  (fn [{:keys [:utils/uuid :clock/now db]} [_ message]]
    {:db
     (push-success db uuid now message)}))

(rf/reg-event-db :notifications/close
  (fn [db [_ identifier]]
    (close db identifier)))


;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Subscriptions

(rf/reg-sub :notifications/db
  (fn [{:keys [:notifications/db]}]
    db))

(rf/reg-sub :notifications/notifications
  :<- [:notifications/db]
  (fn [{:keys [notifications]}]
    notifications))

(rf/reg-sub :notifications/notifications--sorted
  :<- [:notifications/notifications]
  (fn [notifications]
    (->> notifications
         (vals)
         (sort-by (comp time-coerce/to-long :timestamp))
         (reverse))))


;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Views

(defn- notification
  [{:keys [id duration] :as data}]
  (let [dispatch-close
        #(rf/dispatch [:notifications/close id])

        duration-timer
        (r-atom/atom nil)

        create-duration-timer
        #(when duration
           (reset! duration-timer (js/setTimeout dispatch-close duration)))

        clear-duration-timer
        #(swap! duration-timer js/clearTimeout)]

    (r/create-class
     {:display-name
      "notification"

      :component-did-mount
      (fn [this]
        (create-duration-timer))

      :reagent-render
      (fn [{:keys [type title message timestamp] :or {type :info} :as data}]

        (let [css-classes
              (cond-> ""
                type
                (str " --" (name type))

                @duration-timer
                (str " --duration"))

              inline-style
              (cond-> {}
                @duration-timer
                (assoc :animation-duration (str duration "ms")))]

          [:li.notification
           {:class
            css-classes

            :style
            inline-style

            :on-click
            dispatch-close

            :on-mouse-leave
            create-duration-timer

            :on-mouse-over
            clear-duration-timer}

           [:div.notification-content
            (when title
              [:span.notification-title title])
            [:span.notification-message message]

            [clock/ago
             {:class
              "notification-timestamp"}
             timestamp]]]))})))

(defn view
  []
  (let [notifications
        (rf/subscribe [:notifications/notifications--sorted])]

    (fn []
      [:ul.notifications
       (for [{:keys [id] :as data}
             @notifications]

         ^{:key id}
         [notification data])])))
