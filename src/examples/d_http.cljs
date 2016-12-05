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
   {:topic "waiting.gif"
    :gif-url ""
    :loading? false
    :response-error ""}))


;; -- Event Handlers ----------------------------------------------------------


(re-frame/reg-event-fx                             ;; note the trailing -fx
  :more-please                      ;; usage:  (dispatch [:handler-with-http])
  (fn [{:keys [db]} [_ topic]]                ;; the first param will be "world"
      {:db   (-> db
                (assoc :topic topic)
                (assoc :loading? true))   ;; causes the twirly-waiting-dialog to show??
       :http-xhrio {:method          :get
                    :uri             (str "https://api.giphy.com/v1/gifs/random?api_key=dc6zaTOxFJmzC&tag=" topic)
                    :timeout         8000                                           ;; optional see API docs
                    :response-format (ajax/json-response-format {:keywords? true})  ;; IMPORTANT!: You must provide this.
                    :on-success      [:new-gif-ok]
                    :on-failure      [:new-gif-err]}}))


(re-frame/reg-event-db
 :new-gif-ok
  (fn
    [db [_ response]]           ;; destructure the response from the event vector
    (-> db
        (assoc :loading? false) ;; take away that "Loading ..." UI
        (assoc :response-error "") ;; clear error
        (assoc :gif-url (get-in (js->clj response) [:data :image_url])))))


(re-frame/reg-event-db
 :new-gif-err
  (fn
    [db [_ response]]           ;; destructure the response from the event vector
    (-> db
        (assoc :loading? false) ;; take away that "Loading ..." UI
        (assoc :response-error (js->clj response)))))  ;; fairly lame processing

;; -- Subscription Handlers ---------------------------------------------------

(re-frame/reg-sub
 :topic
 (fn [db]
   (:topic db)))

(re-frame/reg-sub
 :gif-url
 (fn [db]
   (:gif-url db)))


;; -- View Components ---------------------------------------------------------


(defn main-panel []
  (let [image-url (re-frame/subscribe [:gif-url])
        topic     (re-frame/subscribe [:topic])]

    (fn []
      [:div
       [:h2 @topic]
       [:input {:type "button" :value "More Please!!"
                :on-click #(re-frame/dispatch [:more-please "cats"])}]
       [:br]
       [:img {:src @image-url}]])))

(defn mount-root []
  (reagent/render [main-panel]
                  (.getElementById js/document "app")))

(defn ^:export init []
  (re-frame/dispatch-sync [:initialize-db])
  (enable-console-print!)
  (enable-re-frisk!)
  (re-frame/dispatch [:more-please "cats"])
  (mount-root))
