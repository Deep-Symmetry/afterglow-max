(defproject afterglow-max "0.1.3"
  :description "A package for hosting Afterglow inside Cycling ‘74’s Max."
  :url "https://github.com/brunchboy/afterglow-max"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [afterglow "0.1.3"]
                 [com.taoensso/timbre "4.1.1"]]
  :main afterglow.core  ; So command-line arguments can be used, e.g. to convert QXF files.
  :uberjar-name "afterglow-max.jar"
  :manifest {"Name" ~#(str (clojure.string/replace (:group %) "." "/")
                            "/" (:name %) "/")
             "Package" ~#(str (:group %) "." (:name %))
             "Specification-Title" ~#(:name %)
             "Specification-Version" ~#(:version %)}

  :profiles {:provided {:dependencies [[local/max "1.0.0"]]}
             :dev {:resource-paths ["dev_resources"]
                   :repl-options {:init-ns afterglow.max.core
                                  :welcome (println "afterglow-max loaded.")}}

             :uberjar {:aot :all}}
  :repositories {"project" "file:repo"}

  :plugins [[codox "0.8.13"]
            [lein-environ "1.0.0"]
            [lein-ancient "0.6.8-SNAPSHOT"]]

  :codox {:src-dir-uri "http://github.com/brunchboy/afterglow-max/blob/master/"
          :src-linenum-anchor-prefix "L"
          :output-dir "target/doc"}
  :min-lein-version "2.0.0")
