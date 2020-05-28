(ns com.eldrix.clods.service
  (:require
    [ring.adapter.jetty :as jetty]
    [clojure.data.json :as json]
    [ring.middleware.params :as params]
    [next.jdbc :as jdbc]))


(def db {:dbtype "postgresql" :dbname "ods"})
(def ds (jdbc/get-datasource db))



(defn handler [request]
  (println request)
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body  (:org (first (jdbc/execute! ds ["SELECT data::varchar as org FROM organisations WHERE id = ?"
                            "2.16.840.1.113883.2.1.3.2.4.18.48|X26"])))})




(defn start []
  (jetty/run-jetty #'handler {:port 3000, :join? false})
  (println "server running in port 3000"))

(comment
  (start)

  (into [] (map str)
   (jdbc/plan ds ["SELECT data#>>'{}' as org FROM organisations WHERE id = ?"
                                        "2.16.840.1.113883.2.1.3.2.4.18.48|X26"]))
  )