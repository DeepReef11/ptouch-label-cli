#!/usr/bin/env bb
(ns ptouch-label
  (:require [clojure.string :as str]
            ))
(require '[babashka.process :refer [shell]]
         '[hiccup2.core :as h]
         '[babashka.fs :as fs]
         '[babashka.cli :as cli]
         '[clojure.string :as str]
         )
(import 'java.time.format.DateTimeFormatter
        'java.time.LocalDateTime)


(def cli-opts
  {
   :t1 {
        :desc "Text content that should appear on every label. You can also use t2, t3 and so on for more text attribute."
        }
   :l1 {
        :desc "List of text separated by separator argument. Each string of text will appear on a different label. You can also use l2, l3 and so one for more list attribute."
        }
   :separator {
               :alias :s
               :desc "Characters to use to divide the text into texts."
               :default ","
               }
   :number {
          :alias :n
          :desc "Number of label to print. If there's a list of text and there's more texts than number, the list will be cut short to number. Required for text only label."
          }
   :font-size {
               :alias :fs
               :desc "Enforce a different font size in pixel to use for each line."
               }
   :tape-height {
                 :desc "Should not be used. Specify the height of ptouch tape in pixel. Mostly used for trying template without having a powered ptouch connected. For 12mm, tape is 76px."
                 }
   :image-right {
                 :alias :ir
                 :desc "Full path to image to the right"
                 }
   :image-left {
                :alias :il
                :desc "Full path to image to the left"
                }
   :template  {
               :desc "
Template to use. Default food-label.
Template works with different arguments.
- t# is for a repeated text through all label. Ex: -t1 \"text 1\" -t2 \"txt 2\" --template \"t1 t2\".
    Result would be a 2 line label with t1 on first line and t2 on second line.
- l# is a list of texts seperated by separator. Ex: -l1 \"val 1, v2, v3\" -l2 \"l2v1, l2v2\" --template \"l1 l2\"
    Result would be 3 labels (because -l1 has 3 values, last label's second line would be empty).
- i is the label's number (first label is 1, second label is 2 ...)
- n is the label's number over total of labels (ex: 1/3, 2/3, 3/3).
- d is the date in numerical format
- Dot (.) is for adding the string on the same line of.
- Space ( ) is for adding the string on a new line.
Example of template: -t1 \"Title\" -l1 \"item 1, item 2, item 3\" --template \"t1.i l1 n d\"
Result:
Title 1     Title 2     Title 3
item 1      item 2      item 3
1/3         2/3         3/3
01-01-1999  01-01-1999  01-01-1999

Premade choice: food-label, chain

"
               :default "food-label"
               }
   :no-print {
              :desc "Create files at /tmp/ptouch-label but don't print"
              :default false
              }
   :preview {
             :desc "Open the image before printing"
             :default false
             }
   :help {
          :desc "Print help."
          }
   :print-command {
                    :desc "String of the command to launch for printing"
                    :default "sudo ptouch-print --image"
                    }
   }
  )
(def tmp-folder "/tmp/ptouch-label")
(def html-path (str tmp-folder "/ptouch-label.html"))
(def img-path (str tmp-folder "/ptouch-label.png"))
(def date (LocalDateTime/now))
(def formatter (DateTimeFormatter/ofPattern "yyyy-MM-dd"))
(def now
  (.format date formatter))
(shell {:continue true :out :string :err :string} "mkdir" tmp-folder)

(def html-size-unit "px")

;; TODO add font para (font-family and font-size)

(defn line [height font-size & content]
     [:div.line {:style {:height height :font-size font-size}} content]
  )
(defn section [height & content]
  [:div.section {:style {:height height}} content]
  )
(defn label [height & content]
  [:div.label {:style :height} content]
  )

(defn meta []
  [:head [:meta {:http-equiv "Content-Type" :content "text/html" :charset "utf-8"}]]
  )
(defn body-wrap [height & content]
  [:html {:style {:min-width 0 :min-height 0}
          } (meta) [:body {:style {:min-width 0 :min-height 0}}
                    [:div.tape { :style {:display "flex" :height (str height html-size-unit)}} content]]])
(defn style [& {:keys [font-size] :or {font-size "76px"}}]
  [:style
   ;; @font-face {font-family: ForcedSquare; src: url(./forced-square.ttf);}
   (str ".label {"
        "   align-items: center;"
        "   word-break: keep-all;"
        "   width: max-content;"
        "   font-family: monospace;"
        "   white-space: nowrap;"
        "   display: flex;"
        "   background-color: white;"
        ;; "   flex-direction: column;"
        "}"
        ".section {"
        "   display: flex;"
        "   flex-direction: column;"
        "   align-items: center;"
        "}"
        ".line {"
        "   overflow-y: hidden;"
        ;; "   height: " line-height ";"
        "   font-size: " font-size ";"
        "   background-color: white;"
        "}"
        )])

(defn cutmark [height & {:keys [space]}]
  (let [tape-height (str height html-size-unit) h10 (quot height 10) h (str h10 html-size-unit)]
    [:div {:style {:height tape-height :width h :display "flex" :flex-direction "column" :background-color "white" }}
     [:div {:style {:height h :width h :background-color "black"}} ]

     [:div {:style {:height (str (- height (* h10 2)) html-size-unit) :width h}} ]
     [:div {:style {:height h :width h :background-color "black"}} ]
     ]
    ))

(defn html-from-template
  [
   & {:keys [
             texts-map
             lists-map
             separator
             number
             height
             template
             image-l
             image-r
             font-size
             ]
   }
   ]

  (let [s-lines (str/split template #" ")
        lines (count s-lines)
        line-height (quot height lines)
        line-font-size (if (some? font-size) font-size (quot (quot height 1.15) lines))
        n (if (some? number) number
              (loop [m lists-map
                     bn 0]
                (println (str m " bn: " bn))
                (if (empty? m)
                  bn
                  (recur (rest m) (let [mn (count (str/split (val(first m)) (re-pattern separator)))]
                        (if (> mn bn) mn bn))
                  )
                )))
        cm (cutmark height)
        img-l (when (some? image-l) [:img {:src image-l :height height :style {:background-color "white"}}])
        img-r (when (some? image-r) [:img {:src image-r :height height :style {:background-color "white"}}])
        spacer " "
        ]
    (apply concat (for [i (range 1 (+ n 1)) :let [i-n (str i "/" n)]]
                    [
                     cm
                     (when img-l img-l)

                     (label height (section height (for [sl (seq s-lines)]
                                                     (line line-height line-font-size
                                                           (str/join (for [s (str/split sl #"\.")]
                                                            (case (first s)
                                                              \t (str (get texts-map (keyword s)) spacer)
                                                              \l (let [m (str/split
                                                                          (get lists-map (keyword s))
                                                                          (re-pattern separator))
                                                                       ]
                                                                   (if (>= (count m) i)
                                                                     (str (str/trim (get
                                                                                     m                                                                                   (- i 1))) spacer)
                                                                     ""))
                                                              \d (str now spacer)
                                                              \n (str i-n spacer)
                                                              \i (str i spacer)
                                                              (println (str "Error in the template format: " s))
                                                              )))
                                                            ))))
                     (when img-r img-r)
                     cm
                     ]
                    )
           )))

(defn generate-html-label [& {:keys [text texts-map lists-map number template height separator font-size image-l image-r] :or {template "food-label"}}]

  (spit html-path
        (let [cm (cutmark height)
              img-l (when (some? image-l) [:img {:src image-l :height height}])
              img-r (when (some? image-r) [:img {:src image-r :height height}])
              s-template (case template
                           "food-label" "t1 d n"
                           "chain" "l1"
                           template )
              ]
        (str (h/html (body-wrap height
                                  (html-from-template
                                                 :texts-map texts-map
                                                 :lists-map lists-map
                                                 :height height
                                                 :number number
                                                 :font-size font-size
                                                 :image-l image-l
                                                 :image-r image-r
                                                 :template s-template
                                                 :separator separator
                                                 )

                                  )
                                (style :font-size font-size)
                                )
                     )
             )))

(defn load-fonts []
  ;; NOTE: Command will execute from current shell directory, not from script's directory TODO
  ;; (shell "cp" "fonts/forced-square.ttf" tmp-folder)
  )
(defn convert-html-to-png []
  (shell (str "cutycapt --min-height=0 --min-width=0 --smooth  --url=file://" html-path " --out=" img-path))
  (shell "convert " img-path "-trim" img-path)
  (shell "convert " img-path "-density" "150" "-threshold" "70%" img-path)
  )

(defn help []
  (println (cli/format-opts {:spec cli-opts})))


(defn pixel-tape-height []
  (try
    (let [
          info (:out (shell {:out :string} "sudo" "ptouch-print" "--info"))
          s "maximum printing width for this tape is "
          slenght (count s)
          px-start (+ (str/index-of info s) slenght)
          px-end (str/index-of info "px" slenght)
          ]
      (read-string (subs info px-start px-end))
      ) (catch Exception e (do

                             (println (str (.getMessage e) " Try turning PTouch On. Using 76 pixel of tape height (12mm)."))
                             76
                             )
               )))

(defn -main []
  (try (let [args (cli/parse-opts *command-line-args* {:spec cli-opts})]
         (if (get args :help) (help)
             (do
               (let [text (get args :text)
                     height (if (get args :tape-height) (get args :tape-height) (pixel-tape-height))
                     number (get args :number)
                     template (get args :template)
                     font-size (get args :font-size)
                     img-r (get args :image-right)
                     img-l (get args :image-left)
                     separator (get args :separator)
                     no-print (get args :no-print)
                     preview (get args :preview)
                     texts-map (select-keys args (for
                                 [[key v] args :when (let [k (name key)]
                                                       (and
                                                        (= (first (name k)) \t)
                                                        (not= k "text")
                                                        (not= k "template")))] key))


                     lists-map (select-keys args (for
                                [[key v] args :when (let [k (name key)]
                                                      (and
                                                       (= (first (name k)) \l)

                                                       (not= k "list")))] key))
                     print-command (get args :print-command)
                     ]
                 (do
                   (println *file*)
                     (load-fonts)
                     (generate-html-label :text text
                                 :height height
                                 :number number
                                 :font-size font-size
                                 :image-l img-l
                                 :image-r img-r
                                 :template template
                                 :separator separator
                                 :texts-map texts-map
                                 :lists-map lists-map
                                 )
                     (convert-html-to-png)
                     (let [print (if (some? preview)
                                   (do
                                     (shell "xdg-open" img-path)
                                     (and (not no-print)

                                           (do
                                                (print  "Do you want to print? (y/n) ")
                                                  (flush)
                                                  (= 0 (str/index-of (str/lower-case (str (read-line))) "y"))
                                                  )
                                          )
                                     )
                                   (not no-print))]
                     (when print (do
                                   (try
                                     (println "Printing...")
                                     (shell (str/split print-command #" ") img-path)

                                     ;; (shell "sudo" "ptouch-print" "--image" img-path)

                                     (catch Exception e (println (str (.getMessage e) " Try turning PTouch On.")))))))))))
         (println "end")
         )

       (catch Exception e
         (println (.getMessage e))
         (help)
         )))

(-main )
