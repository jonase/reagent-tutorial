(ns reagent-tutorial.core
  (:require [clojure.string :as string]
            [reagent.core :as r]))

(enable-console-print!)

;; The "database" of your client side UI.
(def app-state
  (r/atom
   {:contacts
    [{:first "Ben" :last "Bitdiddle" :email "benb@mit.edu"}
     {:first "Alyssa" :middle-initial "P" :last "Hacker" :email "aphacker@mit.edu"}
     {:first "Eva" :middle "Lu" :last "Ator" :email "eval@mit.edu"}
     {:first "Louis" :last "Reasoner" :email "prolog@mit.edu"}
     {:first "Cy" :middle-initial "D" :last "Effect" :email "bugs@mit.edu"}
     {:first "Lem" :middle-initial "E" :last "Tweakit" :email "morebugs@mit.edu"}]}))

(defn update-contacts! [f & args]
  (apply swap! app-state update-in [:contacts] f args))

(defn add-contact! [c]
  (update-contacts! conj c))

(defn remove-contact! [c]
  (update-contacts! (fn [cs]
                      (vec (remove #(= % c) cs)))
                    c))

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

;; UI components
(defn contact [c]
  [:li 
   [:span (display-name c)]
   [:button {:on-click #(remove-contact! c)} 
    "Delete"]])

(defn new-contact []
  (let [val (r/atom "")]
    (fn []
      [:div
       [:input {:type "text"
                :placeholder "Contact Name"
                :value @val
                :on-change #(reset! val (-> % .-target .-value))}]
       [:button {:on-click #(when-let [c (parse-contact @val)]
                              (add-contact! c)
                              (reset! val ""))} 
        "Add"]])))

(defn contacts []
  [:div
   [:h1 "Contact list"]
   [:ul
    (for [c (:contacts @app-state)]
      [contact c])]
   [new-contact]])

;; Render the root component
(defn start []
  (r/render-component 
   [contacts]
   (.getElementById js/document "root")))
