(ns service.dbns
  (:require [clojure.string :as str]
            [clojure.java.jdbc :as sql])
  (:use korma.db
        korma.core)
)

(def db (h2 {:db "resources/db/korma.db"}))

(defdb korma-db db)
(exec-raw ["DROP TABLE IF EXISTS users"])
(exec-raw ["CREATE TABLE users (id INT PRIMARY KEY AUTO_INCREMENT NOT NULL, name VARCHAR, email VARCHAR, address VARCHAR)"])


(comment
(def db {:classname   "org.h2.Driver"
         :subprotocol "h2"
         :subname     "resources/db/korma.db"})
;; this creates a connection map

(def db (h2 {:db "resources/db/korma.db"}))
;; this creates the same connection map as before, but
;; using the helper (h2 ...).

(def db (h2 {:db "resources/db/korma.db"
             :user "sa"
             :password ""
             :naming {:keys str/lower-case
                      ;; set map keys to lower
                      :fields str/upper-case}}))
                      ;; but field names are upper
;; you can pass additional options

;; Helpers for common databases:
(def pg (postgres ..))
(def ms (mssql ..))
(def msac (msaccess ..))
(def orc (oracle ..))
(def mys (mysql ..))
(def sqll (sqlite3 ..))
(def h2-db (h2 ..))



;; Pass the connection map to the defdb macro:
(defdb korma-db db)

;; Or without predefining a connection map:
(defdb prod (postgres {:db "korma"
                       :user "korma"
                       :password "kormapass"
                       ;; optional keys
                       :host "myhost"
                       :port "4567"
                       :delimiters ""}))
                       ;; remove delimiters
  )
