(ns service.core
  (:require [compojure.route :as route]
            [clojure.java.io :as io]
            [ring.middleware.reload :as reload])
  (:use [clojure.data.json :only [json-str read-json]]
        compojure.core
        compojure.handler
        ring.middleware.edn
        carica.core
        clojure.tools.logging
        org.httpkit.server
        [service quiz util])
  (:gen-class))

(defn- now [] (System/currentTimeMillis))

(def clients (atom {}))                 ; a hub, a map of client => sequence number
(def last-question-time (atom (now)))
(def last-message-time (atom (now)))
(def current-question (atom []))

(let [max-id (atom 0)]
  (defn next-id []
    (swap! max-id inc)))

 ;;;;;;;;;;;;;; MESSAGE TEMPLATES ;;;;;;;;;;;;;;

(defn send-public [msg]   ;(merge data
  (doseq [client (keys @clients)]
    (send! client
           (json-str (merge {:author (config :system-user)
                             :time (now)
                             :type "message"}
                            msg)))))

(defn send-private [msg client]
  (send! client
           (json-str (merge {:author (config :system-user)
                             :time (now)
                             :type "message"}
                            msg))))


;;;;;;;;;;;;;;; QUIZ MESSAGES ;;;;;;;;;;;;;;;;;

(defn send-prompt [step]
  (send-public {;; значения по-умолчанию
                     :msg (str "Подсказка: " (prompt (first @current-question) step))
                     :word (prompt (first @current-question) step)
                     :question (second @current-question)}))

(defn send-question []
  (send-public {:type "question"
                     :msg (str "Вопрос: " (second @current-question))
                     :word (prompt (first @current-question) 0)
                     :question (second @current-question)}))

(defn send-shame []
  (send-public {:msg (str "Никто не ответил на этот вопрос (" (first @current-question) "). Позор!")
                     :word (prompt (first @current-question) 0)
                     :question (second @current-question)}))

(defn send-timeout []
  (send-public {:msg (str "Викторина приостановлена. Для продолжения напишите что-нибудь!")
                     :question (second @current-question)}))

(defn send-welcome-when-stopped [client]
  (send-private {:msg (str "Для запуска викторины напишите что-нибудь!")}
                client))

(defn send-welcome-with-question [client]
  (send-private {:msg (str "Текущий вопрос: " (second @current-question))
                 :question (second @current-question)}
                client))

(defn send-right-answer [author answer]
  (send-public  ;;отправка правильного ответа
     {:author (config :system-user)
      :msg (str "  That's right, " author "! " answer)}))


;;;;;;;;;;;; QUIZ CONTROL ;;;;;;;;;;;;;;;;;;;;;;;

(defn stop-quiz-by-timeout []
  (reset! current-question [])
  (send-timeout))

(defn new-question-thread []
  (reset! last-question-time (now))
  (reset! current-question (next-question))

  (let [question-time @last-question-time
        interval (config :prompt-timeout)]
    (doto (Thread. #(try
                      (Thread/sleep 2000) ;перерыв для комфорта
                      (send-question)

                      (Thread/sleep interval)

                      (doseq [x [0 1 2 3]]
                        (when (= question-time @last-question-time)
                           (send-prompt x)
                           (Thread/sleep interval)))

                      (when (= question-time @last-question-time)
                        (send-shame)
                        (if (< (now) (+ @last-message-time
                                        (config :chat-timeout)))
                          (new-question-thread)
                          (stop-quiz-by-timeout)))

                      (catch InterruptedException _)))
      (.start))))

(defn is-quiz-stopped [] (= @current-question []))

(defn quiz-handler [msg]
  (reset! last-message-time (now))
  (if (is-quiz-stopped)
    (new-question-thread)
    (let [data (read-json msg)]
      (if (= (first @current-question) (:msg data))
        (do
          (send-right-answer (escape-html (:author data))
                             (first @current-question))
          (new-question-thread))
        ))))

 ;;;;;;;;;;;;;; WEBSOCKET ;;;;;;;;;;;;;;;;;;;;;;;

(defn mesg-received [msg]
  (let [data (read-json msg)]
    (info "message received: " data)
    (when (:msg data)
      (send-public (merge data {:time (now)
                                :msg (escape-html (:msg data))
                                :author (escape-html (:author data))})))
      (quiz-handler msg)))

(defn chat-handler [req]
  (with-channel req channel
    (info channel "connected")
    (if (is-quiz-stopped)
      (send-welcome-when-stopped channel)
      (send-welcome-with-question channel))
    (swap! clients assoc channel true)
    (on-receive channel #'mesg-received)
    (on-close channel (fn [status]
                        (swap! clients dissoc channel)
                        (info channel "closed, status" status)))))

;;;;;;;;;;;;;;;; COMPOJURE ;;;;;;;;;;;;;;;;;;;;

(defroutes compojure-handler
  (GET "/" [] (slurp (io/resource "public/html/index.html")))
  (GET "/req" request (str request))
  (GET "/req/123" request (str request))
  (GET "/quiz" request "<h1>quiz be here</h1><hr/>")
  (GET "/ws" []  chat-handler)
  (route/resources "/")
  (route/files "/" {:root (config :external-resources)})
  (route/not-found "Not found!"))

;;;;;;;;;;;;;;;; INIT SERVER ;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce server (atom nil))

(defn stop-server []
  (when-not (nil? @server)
    ;; graceful shutdown: wait 100ms for existing requests to be finished
    ;; :timeout is optional, when no timeout, stop immediately
    (@server :timeout 100)
    (reset! server nil)))

;lein-ring does not yet support http-kit, but ring.middleware.reload can be used as a workaround.
;lein run # start a server in :8080, hot code reload
  ;; The #' is useful, when you want to hot-reload code
  ;; You may want to take a look: https://github.com/clojure/tools.namespace
  ;; and http://http-kit.org/migration.html#reload
  ;;(reset! server (run-server #'app {:port 8080})))

(defn -main [& args] ;; entry point, lein run will pick up and start from here
  (let [handler (if (config :in-dev?);;(in-dev? args)
                  (reload/wrap-reload (site #'compojure-handler)) ;; only reload when dev
                  (site compojure-handler))]
     ;   port (Integer/parseInt (get (System/getenv) "OPENSHIFT_CLOJURE_HTTP_PORT" "8080"))
     ;   ip (get (System/getenv) "OPENSHIFT_CLOJURE_HTTP_IP" "0.0.0.0")]
    (reset! server (run-server handler {:port (config :web-port)}))))

;(stop-server)
;     (-main)
;(clear-config-cache!)  ;; сброс настроек конфига
