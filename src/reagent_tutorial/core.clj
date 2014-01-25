(ns reagent-tutorial.core
  (:require [clojure.edn :as edn]
            [org.httpkit.server :as http]
            [compojure.core :refer (defroutes GET)]
            [compojure.route :refer (resources)]
            [hiccup.page :as hiccup]))

(def contacts 
  (atom #{{:first "Ben" :last "Bitdiddle" :email "benb@mit.edu"}
          {:first "Alyssa" :middle-initial "P" :last "Hacker" :email "aphacker@mit.edu"}
          {:first "Eva" :middle "Lu" :last "Ator" :email "eval@mit.edu"}
          {:first "Louis" :last "Reasoner" :email "prolog@mit.edu"}
          {:first "Cy" :middle-initial "D" :last "Effect" :email "bugs@mit.edu"}
          {:first "Lem" :middle-initial "E" :last "Tweakit" :email "morebugs@mit.edu"}}))

(defmulti handle-message :op)

(defmethod handle-message :get [_]
  {:op :reset
   :contacts @contacts
   :broadcast? false})

(defmethod handle-message :remove [{:keys [contact] :as msg}]
  (swap! contacts disj contact)
  (assoc msg :broadcast? true))

(defmethod handle-message :add [{:keys [contact] :as msg}]
  (swap! contacts conj contact)
  (assoc msg :broadcast? true))

(def channels (atom #{}))

(defn ws-handler [req]
  (http/with-channel req channel
    (swap! channels conj channel)
    (http/on-close 
     channel 
     (fn [status] 
       (swap! channels disj channel)))
    (http/on-receive 
     channel 
     (fn [s]
       (try 
         (let [data (edn/read-string s)
               res (handle-message data)]
           (if (:broadcast? res)
             (doseq [chan @channels]
               (http/send! chan (pr-str res)))
             (http/send! channel (pr-str res))))
         (catch Exception e
           (.printStackTrace e)))))))

(defn main-view []
  (list 
   [:head [:title "Reagent Tutorial"]]
   [:body [:div#root]
    [:script {:src "app.js"}]
    [:script "reagent_tutorial.core.start()"]]))

(defroutes routes
  (GET "/" [] (hiccup/html5 (main-view)))
  (GET "/ws" req (ws-handler req))
  (resources "/"))

(defonce server (atom nil))

(defn stop-server []
  (when-not (nil? @server)
    (@server :timeout 100)
    (reset! server nil)))

(defn start-server []
  (reset! server (http/run-server #'routes {:port 8080})))

;; (start-server)
;; (stop-server)

