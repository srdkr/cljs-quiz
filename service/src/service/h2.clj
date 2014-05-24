(ns service.h2
(:use [korma.core]
[korma.db])
(:require [clojure.string :refer [lower-case upper-case]]))

(defdb korma-db (h2 {:db "h2test"
:naming {:keys lower-case
:fields upper-case}}))

;; --- schema creation ---

(defn h2-create-tables []
  (exec-raw ["DROP TABLE IF EXISTS users"])
  (exec-raw ["CREATE TABLE users (id INT PRIMARY KEY AUTO_INCREMENT NOT NULL, name VARCHAR, email VARCHAR, address VARCHAR)"])

  (exec-raw ["DROP TABLE IF EXISTS items"])
  (exec-raw ["CREATE TABLE items (id INT PRIMARY KEY AUTO_INCREMENT NOT NULL, name VARCHAR, price FLOAT)"])

  (exec-raw ["DROP TABLE IF EXISTS orders"])
  (exec-raw ["CREATE TABLE orders (id INT PRIMARY KEY AUTO_INCREMENT NOT NULL, items_id INT, users_id INT, date DATE, price FLOAT, FOREIGN KEY (items_id) REFERENCES items(id), FOREIGN KEY (users_id) REFERENCES users(id))"]))
;; --- korma schema definition ---

(defentity orders
(entity-fields :items_id :users_id :date :price))
(defentity users
(entity-fields :name :email :address)
(has-many orders))
(defentity items
(entity-fields :name :price)
(has-many orders))

;; --- inserts ---

(defn h2-insert
(transaction
(insert users (values {:name "kalle" :email "kalle@hotmail.com" :address "12 high street"}))
(insert users (values {:name "olle" :email "olle@gmail.com" :address "132 in-the-sticks street"}))
(insert users (values {:name "lisa" :email "lisa77@yahoo.com" :address "4 belgrave square"})))

(insert items (values {:name "Olympus OM-D" :price 523.32}))
(insert items (values {:name "Pana 20mm/1.7" :price 250}))
(insert items (values {:name "Oly 45mm/2.4" :price 261.2}))

(insert orders (values {:items_id 1 :users_id 1 :date (java.util.Date.) :price 510})))

;; --- queries ---

(select users
(fields :name :email))

(select users
(with orders))

(select items
(with orders))
