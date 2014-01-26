# reagent-tutorial

This is a port of the [Om](https://github.com/swannodette/om)
[tutorial](https://github.com/swannodette/om/wiki/Tutorial) to
[Reagent](https://github.com/holmsand/reagent). The two libraries
are both ClojureScript UI libraries built on top of
[React](http://facebook.github.io/react/). It interesting to
compare/contrast the two approaches.

## Usage

    git clone git@github.com:jonase/reagent-tutorial.git
    cd reagent-tutorial
    lein cljsbuild auto

Open [`app.html`](https://github.com/jonase/reagent-tutorial/blob/master/app.html) and read/edit [`src-cljs/reagent_tutorial/core.cljs`](https://github.com/jonase/reagent-tutorial/blob/master/src-cljs/reagent_tutorial/core.cljs).

Feedback welcome!

## Code Walkthrough

First, we import the necessary namespaces:

```clojure
(ns reagent-tutorial.core
  (:require [clojure.string :as string]
            [reagent.core :as r]))
```

`clojure.string` will be used for some simple parsing and
`reagent.core` is the main entry point to all the good stuff in
Reagent. We will only use two functions from the Reagent namespace:
`r/atom` and `r/render-component`.

`r/atom` is very similar to the ordinary Clojure `atom` but with
`r/atom` watchers are notified when someone dereferences the
atom. `r/render-component` will be used to render the root UI
component.

Next, we define a global `r/atom` which will hold our application
state as well as a few helper functions to manipulate the contents of
that state.

```clojure
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
```

Think of `add-contact!` and `remove-contact!` as the interface to our
"database" of contacts. These two functions could be part of a
protocol to allow different "backend" implementations.

`parse-contact` below is used to parse a string (hopefully containing
a persons name) and either return `nil` if the string could not be
parsed or a map with keys `:first` `:last` and optionally either
`:middle` or `:middle-initial`. This function is the same as in the Om
tutorial.

```clojure
(defn parse-contact [contact-str]
  (let [[first middle last :as parts] (string/split contact-str #"\s+")
        [first last middle] (if (nil? last) [first middle] [first last middle])
        middle (when middle (string/replace middle "." ""))
        c (if middle (count middle) 0)]
    (when (>= (reduce + (map #(if % 1 0) parts)) 2)
      (cond-> {:first first :last last}
        (== c 1) (assoc :middle-initial middle)
        (>= c 2) (assoc :middle middle)))))
```

```clojure
user=> (parse-contact "John")
nil
user=> (parse-contact "John Doe")
{:first "John" :last "Doe"}
user=> (parse-contact "John E Doe")
{:first "John" :middle-initial "E" :last "Doe"}
user=> (parse-contact "John Edwin Doe")
{:first "John" :middle "Edwin" :last "Doe"}
```
 
The next two functions are used to create formatted strings from the
maps created by `parse-contact` (these two functions are also copied
from the Om tutorial):

```clojure
(defn middle-name [{:keys [middle middle-initial]}]
  (cond
   middle (str " " middle)
   middle-initial (str " " middle-initial ".")))

(defn display-name [{:keys [first last] :as contact}]
  (str last ", " first (middle-name contact)))
```

```clojure
user=> (display-name {:first "John" :last "Doe"})
"Doe, John"
user=> (display-name {:first "John" :middle-initial "E" :last "Doe"})
"Doe, John E."
user=> (display-name {:first "John" :middle "Edwin" :last "Doe"})
"Doe, John Edwin"
```

With all this out of the way we can finally start figuring out how to
put things on the screen. With Reagent you create UI components out of
[`hiccup`](https://github.com/weavejester/hiccup) data structures. The
component which displays a single contact from our "database" is
defined as follows:

```clojure
(defn contact [c]
  [:li
   [:span (display-name c)]
   [:button {:on-click #(remove-contact! c)} 
    "Delete"]])
```

The above data structure is roughly equivalent to the following
HTML/JS pseudo-code:

```html
<li>
  <span>{{displayName(c)}}</span>
  <button onClick='{{removeContact(c)}}'>Delete</button>
</li>
```

Hopefully you have no trouble reading `hiccup` data structures: a
vector like `[:li ..]` is translated to the tag
`<li>..</li>`. Arbitrary Clojure code can be used to generate these
vectors and Clojure functions can be used when registering event
handlers.

Note also that `contact` is simply an ordinary Clojure function which
takes a contact `c` and returns a `hiccup` data structure.

A function which is going to be used as a component can take up to
three arguments: The first argument (`c` in our case) must be a
map. It is used to pass data from the parent component. You can think
of this argument as equivalent to the set of html attributes for some
tag but they are usually called "props" in the terminology used by
Reagent. The second argument is a vector of child components and the last
argument is a reference to the underlying React component. The last
two arguments are not used at all in this tutorial.

The complete interface for a Reagent component is therefor

```clojure
(defn some-component [props children this] 
  ...)
```

which is used in client code as

```clojure
[some-component {:some :props}
  [first-child-component]
  [second-child-component]
  ...]
```

We can now use the `contact` component when defining `contact-list`:

```clojure
(defn contact-list []
  [:div
   [:h1 "Contact list"]
   [:ul
    (for [c (:contacts @app-state)]
      [contact c])]
   [new-contact]])
```

`contact-list` is also a function which returns hiccup data and can be
used as a component in yet a larger context. Note how `contact` is
used in the body of `contact-list`: It is not called as a function,
instead it's wrapped in a vector similar to how the rest of hiccup
works: `[contact c]`.

You can also see that we have another custom component as the last
item in the `div`. `new-contact` is a component that lets users add
new contacts to the `app-state`:

```clojure
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
```

The `new-contact` holds the current value of the input text box as
*local state* in the `val` atom. Every time we edit the text box the
`val` atom is reset to the latest text content. When the "Add" button
is clicked the string in `val` is parsed and a new contact is added to
the `app-state` database (on a successful parse).

When `app-state` changes (either by adding or deleting a contact) the
underlying React system will figure out the minimal required changes
to the DOM and perform the updates on our behalf.

The last piece of the puzzle is to attach the root node to some existing dom node. In our case the root node will be `contact-list` and we will attach it to an empty `div` element with id `root`:

```clojure
(defn start []
  (r/render-component 
   [contact-list]
   (.getElementById js/document "root")))
```

## License

Copyright © 2014 Jonas Enlund

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
