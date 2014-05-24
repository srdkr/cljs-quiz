(ns client.core
  (:require [enfocus.core :as ef])
  (:require-macros [enfocus.macros :as em]))

(em/defsnippet chat-body "/html/chat.html" "#page-wrap" [])
(em/defsnippet blog-header "/html/blog.html" ".blog-header" [])
(em/defsnippet chat-msg "/html/chat.html" ["#history > *:first-child"]
  [user msg time]
  [".author"] (ef/content (str user ":"))
  [".msg"] (ef/content msg)
  [".time"] (ef/content time))

;(def conn (js/WebSocket. (str "ws://" (.-hostname js/location)
;                              ":" (.-port js/location) "/ws")))


;;;;;;;;;;;;;;;;; TIME ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn if-add-zero [in] (if (> in 9) in (str "0" in)))

(defn get-human-time [unix-time]
;  (.log js/console "unix-time " unix-time)
  (let [js-date (js/Date. unix-time)]
;    (.log js/console js-date)
     (str (if-add-zero (.getHours js-date)) ":"
          (if-add-zero (.getMinutes js-date)) ":"
          (if-add-zero (.getSeconds js-date)))))


;;;;;;;;;;;;;;; ADD MESSAGE ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn add-msg [msg]
   (ef/at "#history" (ef/append (chat-msg (.-author msg)
                                          (.-msg msg)
                                          (get-human-time (.-time msg)))))
;; autoscroll
  (let [ hist (js/$ "#history-wrap")]
    (.scrollTop hist (.prop hist "scrollHeight"))))


;;;;;;;;;;;;;;; WEBSOCKET ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def conn (js/WebSocket. (str "ws://" (.-hostname js/location)
                              ":" (.-port js/location) "/ws")))

(set! (.-onopen conn)  (fn [e]
    (.send conn (.stringify js/JSON (js-obj "command" "getall")))))

(set! (.-onerror conn) (fn []
    (js/alert "error") (.log js/console js/arguments)))

(set! (.-onmessage conn)
  (fn [e]
;    (.log js/console "receiving" (.parse js/JSON (.-data e)))
    (let [msg (.parse js/JSON (.-data e))]
      (case (.-type msg)
        "message" (add-msg msg)
        "question" (do ;(.log js/console "QUESTION HERE!!" (.-question msg))
                     (add-msg msg))
        "prompt" (add-msg msg)
        (.log js/console "UNRECOGNIZED MESSAGE!!")))))


(defn send-to-server []
  (let [msg  (.trim (.val (js/$ "#i"))) ;; (.trim js/$ (.value i))
        author (.trim js/$ (.val (js/$ "#name")))]
    (if msg
      (do
        (.send conn (.stringify js/JSON (js-obj "msg" msg "author" author "type" "message")))
        (ef/at "#i" (ef/set-prop :value ""))
 ;       (.log js/console "sending: " (.stringify js/JSON (js-obj "msg" msg "author" author "type" "message")))
        ))))

;;;;;;;;;;;;;;;;; INIT ;;;;;;;;;;;;;;;;;;;;;;;;;

(defn init-events []
  (.click (js/$ "#send") send-to-server)

  (.keyup (.focus (js/$ "#i")) ;;keypress
          (fn [e] (if (= (.-which e) 13) (send-to-server)))))


(defn start []
  (ef/at "body" (ef/append (chat-body)))
  (ef/at "#history" (ef/content (chat-msg "*** QUIZ ***" "Добро пожаловать!" "")))
  (init-events))

;; (set! (.-onload js/window) start)
(set! (.-onload js/window) #(em/wait-for-load (start)))
