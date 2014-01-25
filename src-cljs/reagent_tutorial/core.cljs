(ns reagent-tutorial.core
  (:require [cljs.reader :refer (read-string)]
            [clojure.string :as string]
            [reagent.core :as r]))

(enable-console-print!)

(def app-state
  (r/atom {:contacts #{}}))

(defn update-contacts! [f & args]
  (apply swap! app-state update-in [:contacts] f args))

(defn reset-contacts! [cs]
  (update-contacts! (constantly cs)))

(defn add-contact! [c]
  (update-contacts! conj c))

(defn remove-contact! [c]
  (update-contacts! disj c))

;; The next three fuctions are copy/pasted verbatim from the Om tutorial
(defn middle-name [{:keys [middle middle-initial]}]
  (cond
   middle (str " " middle)
   middle-initial (str " " middle-initial ".")))

(defn display-name [{:keys [first last] :as contact}]
  (str last ", " first (middle-name contact)))

(defn parse-contact [contact-str]
  (let [[first middle last :as parts] (string/split contact-str #"\s+")
        [first last middle] (if (nil? last) [first middle] [first last middle])
        middle (when middle (string/replace middle "." ""))
        c (if middle (count middle) 0)]
    (when (>= (reduce + (map #(if % 1 0) parts)) 2)
      (cond-> {:first first :last last}
        (== c 1) (assoc :middle-initial middle)
        (>= c 2) (assoc :middle middle)))))

;; send will send a web socket message to the server
(declare send)

;; UI components
(defn contact [c]
  [:li 
   [:button {:on-click #(send {:op :remove :contact c})} 
    "Delete"]
   [:span {:style {:padding-left "5px"}} (display-name c)]])

(defn new-contact []
  (let [val (r/atom "")]
    (fn []
      [:div
       [:input {:type "text"
                :placeholder "Contact Name"
                :value @val
                :on-change #(reset! val (-> % .-target .-value))}]
       [:button {:on-click #(when-let [c (parse-contact @val)]
                              (send {:op :add :contact c})
                              (reset! val ""))} 
        "Add"]])))

(defn contacts []
  (let [sort-order (r/atom :last)]
    (fn []
      [:div
       [:h1 "Contact list:"]
       [:span "Sort By: "]
       [:select {:on-change #(reset! sort-order 
                                     (-> % .-target .-value keyword))}
        (for [v ["last" "first"]]
          [:option {:value v} (string/capitalize v)])]
       [:ul
        (for [c (sort-by @sort-order (:contacts @app-state))]
          [contact c])]
       [new-contact]])))

;; Handle web socket messages

(defmulti handle-message :op)

(defmethod handle-message :reset [{:keys [contacts]}]
  (reset-contacts! contacts))

(defmethod handle-message :remove [{:keys [contact]}]
  (remove-contact! contact))

(defmethod handle-message :add [{:keys [contact]}]
  (add-contact! contact))

(def *ws* (atom nil))

(defn send [msg]
  (when-let [ws @*ws*] 
    (.send ws (pr-str msg))))

(defn start []

  ;; Render the root component
  (r/render-component 
   [contacts]
   (.getElementById js/document "root"))

  ;; Setup the web socket connection
  (let [ws (js/WebSocket. "ws://localhost:8080/ws")]
    (set! (.-onopen ws) 
          (fn [e]
            (set! (.-onmessage ws)
                  (fn [e]
                    (-> e .-data read-string handle-message)))
            (reset! *ws* ws)
            (send {:op :get})))))
