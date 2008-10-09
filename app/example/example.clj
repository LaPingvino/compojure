;; example
;;
;; This is the basic example application that comes with Compojure for
;; demonstration purposes.

(ns example
  (:use (compojure jetty
		   html
                   http)))

(defn template
  "A function to generate the standard outline of a HTML page."
  [title & body]
  (html
    (doctype :html4)
    [:html
      [:head
        [:title title]]
      [:body
        body]]))

(def validating-html false)
(def validator-functions [])
(def validate-params {})
(def validation-errors [])

(defn validator [#^java.lang.Boolean valid? message html-body]
  (if (and validating-html (not valid?))
    (do
      (set! validation-errors (conj validation-errors message))
      (html [:div {:class "error"} message html-body]))
    html-body))

(defn valid-html? [html-body params]
  "returns a boolean whether the html is valid using the given parameters"
  (binding [validating-html true
	    validate-params params
	    validation-errors []]
    (let [returned-html (html-body)]
      (println "valid-html?: validation-errors = " validation-errors)
      (zero? (count validation-errors)))))

(defn html-with-validation-errors [html-body params]
  "returns the marked up html, containing validation errors"
  (println "validate-html")
  (binding [validating-html true
	    validate-params params
	    validation-errors []]
    (html-body)
    (html-body)))

(defmacro validate-predicate [pred message html-body]
  "pred is a an expression that evaluates to a boolean"
  `(if validating-html
     (do
       (validator ~pred ~message ~html-body))
      ~html-body))

(defn seq-contains? [val seq]
  (if (> (count seq) 0)
    (if (= val (first seq))
      true
      (recur val (rest seq)))
    false))
    
(defmacro validate-acceptance [param-name message html-body]
  "true if the parameter is not nil. Intended for checkboxes"
  `(validate-predicate (not (nil? (validate-params ~param-name)))
		       ~message ~html-body))

(defmacro validate-in [param-name lst message html-body]
  "true if the paramter's value is in lst"
  `(validate-predicate (seq-contains? (validate-params ~param-name) ~lst) 
		       ~message ~html-body))

(defmacro validate-not-blank [param-name message html-body]
  "true if the field is not blank"
  `(validate-predicate (and (not (nil? (validate-params ~param-name))) 
			    (> (count (validate-params ~param-name)) 0))
		       ~message ~html-body))

(defn validation-error-summary []
  "displays a div with the summary of errors on the page"
  (when (and validating-html (> (count validation-errors) 0))
     (html [:div {:class "error_summary"}
	    [:p "the page had the following errors:"
	     [:ul
	      (map (fn [err] [:li err]) validation-errors)]]])))

(defn example-form
  "A form with all inbuilt controls."
  []
  (html 
   (doctype :xhtml-strict)
   [:html
    [:head
     (include-css "/public/test.css")
     [:title "Form"]]
     [:body 
       [:form {:method "post" :action "/form"}
	  (validation-error-summary)
	       [:p (label :name "Username:") " "
		(text-field :name "Anonymous")]
	       
	       (validate-not-blank :password "password must not be blank"
                 [:p (label :password "Password:") " "
		   (password-field :password)])

	       (validate-in :sex ["Male"] "no girls allowed!"
	          [:p (label :sex "Sex:") " "
           	     (drop-down :sex ["Male" "Female"])])

	       [:p (label :profile "Profile:") [:br]
		(text-area {:cols 40 :rows 10} :profile)]

	        (validate-acceptance :agree "must accept the eula"
		  [:p (label :agree "Have read usage agreement:") " "
		   (check-box :agree)])
	       [:p (submit-button "New User")
		(reset-button "Reset Form")]]]]))

(defn welcome-page
  "A basic welcome page."
  []
  (template "Greeting"
    [:h1#title "Welcome to Compojure"]
    [:p.info
      "Compojure is an open source web framework for "
      (link-to "http://clojure.org" "Clojure") "."]
    [:p
      "Here is an " (link-to "/form" "example of a form")
      " generated by Compojure."]))

(defservlet example-servlet
  "A Compojure example servlet."
  (GET "/"
    (welcome-page))
  (GET "/form"
       (example-form))
  (POST "/form"
	(if (valid-html? example-form (param-map request))
	  (template "Form Validation" (html [:p "You are a genius!"]))
	  (html-with-validation-errors example-form (param-map request))))

  (GET "/public/*"
       (serve-file "public" (route :*)))
  (ANY "*"
       (page-not-found)))
