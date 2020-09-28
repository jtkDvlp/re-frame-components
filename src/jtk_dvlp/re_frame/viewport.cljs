(ns jtk-dvlp.re-frame.viewport
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [reagent.dom :as rdom]

   [jtk-dvlp.re-frame.utils :as u]))


;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Events

(rf/reg-event-db :viewport/resize
  (fn [db [_ width height]]
    (assoc-in db [:viewport/db :size] [width height])))

(rf/reg-event-db :viewport/scroll
  (fn [db [_ scroll]]
    (assoc-in db [:viewport/db :scroll] scroll)))


;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Subscriptions

(rf/reg-sub :viewport/db
  (fn [{:keys [:viewport/db]}]
    db))

(rf/reg-sub :viewport/size
  :<- [:viewport/db]
  (fn [{:keys [size]}]
    size))

(rf/reg-sub :viewport/scroll
  :<- [:viewport/db]
  (fn [{:keys [scroll]}]
    scroll))


;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Dispatches

(defn- dispatch-resize
  [_]
  (->> [:viewport/resize
        (.-innerWidth js/window)
        (.-innerHeight js/window)]
       (rf/dispatch)))

(defn- dispatch-scroll
  [js-event]
  (rf/dispatch [:viewport/scroll (.-timeStamp js-event)]))


;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Views

(defn viewport
  []
  (let [dispatch-resize
        (u/debounce dispatch-resize 200)

        dispatch-scroll
        (u/debounce dispatch-scroll 200)]

    (r/create-class
     {:display-name
      "viewport"

      :component-did-mount
      (fn [this]
        (-> js/window
            (.addEventListener "resize" dispatch-resize))

        (-> this
            (rdom/dom-node)
            (.addEventListener "scroll" dispatch-scroll true)))

      :reagent-render
      (fn [attrs & contents]
        (into
         [:div.viewport attrs]
         contents))})))

(defn- intersects?
  [{:keys [x y w h]} [vw vh]]
  (and (> (+ x w) 0)
       (< x vw)
       (> (+ y h) 0)
       (< y vh)))

(defn- boundaries
  [dom-node]
  (let [rect
        (.getBoundingClientRect dom-node)]

    {:x (.-x rect)
     :y (.-y rect)
     :w (.-width rect)
     :h (.-height rect)}))

(defn content
  [{:keys [on-in-view on-out-view]} & _]

  (let [viewport-size
        (rf/subscribe [:viewport/size])

        viewport-scroll
        (rf/subscribe [:viewport/scroll])

        in-view?
        (atom false)]

    (r/create-class
     {:display-name
      "in-view"

      :component-did-update
      (fn [this]
        (let [dom-node
              (rdom/dom-node this)

              dom-boundaries
              (boundaries dom-node)

              prev-in-view?
              @in-view?

              cur-in-view?
              (intersects? dom-boundaries @viewport-size)]

          (reset! in-view? cur-in-view?)
          (when (not= cur-in-view? prev-in-view?)
            (if cur-in-view?
              (when on-in-view (on-in-view))
              (when on-out-view (on-out-view))))))

      :reagent-render
      (fn [attrs & contents]
        (let [attrs
              (dissoc attrs :on-in-view :on-out-view)]

          @viewport-size
          @viewport-scroll

          (into
           [:div.viewport-content attrs]
           contents)))})))
