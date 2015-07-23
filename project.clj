(defproject afterglow-max "0.1.0-SNAPSHOT"
  :description "A package for hosting Afterglow inside Cycling ‘74’s Max."
  :url "https://github.com/brunchboy/afterglow-max"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [afterglow "0.1.1-SNAPSHOT"]]
  :uberjar-name "afterglow-max.jar"

  :profiles {:provided {:dependencies [[local/max "1.0.0"]]}
             :dev {:resource-paths ["dev_resources"]
                   :repl-options {:init-ns afterglow.max.core
                                  :welcome (println "afterglow-max loaded.")}}

             :uberjar {:aot :all}}
  :repositories {"project" "file:repo"}

  :plugins [[codox "0.8.12"]
            [lein-environ "1.0.0"]
            [lein-ancient "0.6.7"]]

  :codox {:src-dir-uri "http://github.com/brunchboy/afterglow-max/blob/master/"
          :src-linenum-anchor-prefix "L"
          :output-dir "target/doc"}
  :min-lein-version "2.0.0")
