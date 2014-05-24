(defproject service "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/data.json "0.2.4"]

                 [compojure "1.1.8"]
                 [ring "1.2.2"]

                 [javax.servlet/servlet-api "2.5"]
                 [http-kit "2.1.16"]
                 [org.clojure/tools.logging "0.2.3"]

                 [sonian/carica "1.1.0" :exclusions [[cheshire]]]
                 [korma "0.3.1"]
                 [com.h2database/h2 "1.3.170"]

                 [mysql/mysql-connector-java "5.1.30"]
                 [fogus/ring-edn "0.2.0"]]
  :plugins [[lein-ring "0.8.10"]]
  :ring {:handler service.core/app}

  :main service.core
  :aot [service.core]
  )
