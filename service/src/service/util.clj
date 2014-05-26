(ns service.util)

(defn escape-html ;;COPY-PASTE from hiccup
  "Change special characters into HTML character entities."
  [text]
  (.. #^String text
    (replace "&" "&amp;")
    (replace "<" "&lt;")
    (replace ">" "&gt;")
    (replace "\"" "&quot;")))
