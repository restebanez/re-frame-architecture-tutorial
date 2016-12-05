(ns examples.d-http
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [day8.re-frame.http-fx] ;; use on the events handler
            [ajax.core :as ajax] ;; use on the events handler
            [re-frisk.core :refer [enable-re-frisk!]]))



(re-frame/reg-event-db
 :initialize-db
 (fn  [_ _]
   {:loading? false
    :image_url ""
    :response-error ""}))


;; -- Event Handlers ----------------------------------------------------------


(re-frame/reg-event-fx                             ;; note the trailing -fx
    :request-more-cats                      ;; usage:  (dispatch [:handler-with-http])
       (fn [{:keys [db]} _]                    ;; the first param will be "world"
         {:db   (assoc db :loading? true)   ;; causes the twirly-waiting-dialog to show??
          :http-xhrio {:method          :get
                       :uri             "https://api.giphy.com/v1/gifs/random?api_key=dc6zaTOxFJmzC&tag=cats"
                       :timeout         8000                                           ;; optional see API docs
                       :response-format (ajax/json-response-format {:keywords? true})  ;; IMPORTANT!: You must provide this.
                       :on-success      [:response-more-cats-sucess]
                       :on-failure      [:response-more-cats-failure]}}))


(re-frame/reg-event-db
  :response-more-cats-sucess
  (fn
    [db [_ response]]           ;; destructure the response from the event vector
    (-> db
        (assoc :loading? false) ;; take away that "Loading ..." UI
        (assoc :response-error "") ;; clear error
        (assoc :image_url (get-in (js->clj response) [:data :image_url])))))


(re-frame/reg-event-db
  :response-more-cats-failure
  (fn
    [db [_ response]]           ;; destructure the response from the event vector
    (-> db
        (assoc :loading? false) ;; take away that "Loading ..." UI
        (assoc :response-error (js->clj response)))))  ;; fairly lame processing

;; -- Subscription Handlers ---------------------------------------------------


(re-frame/reg-sub
 :image_url
 (fn [db]
   (:image_url db)))


;; -- View Components ---------------------------------------------------------


(defn main-panel []
  (let [image-url (re-frame/subscribe [:image_url])]
    (fn []
      [:div
       [:h1 "Catss"]
       [:input {:type "button" :value "More cats!"
                :on-click #(re-frame/dispatch [:request-more-cats])}]
       [:div "This is the picture " [:img {:src @image-url}]]])))

(defn mount-root []
  (reagent/render [main-panel]
                  (.getElementById js/document "app")))

(defn ^:export init []
  (re-frame/dispatch-sync [:initialize-db])
  (enable-console-print!)
  (enable-re-frisk!)
  (mount-root))
