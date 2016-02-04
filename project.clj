(defproject afterglow-max "0.2.1-SNAPSHOT"
  :description "A package for hosting Afterglow inside Cycling ‘74’s Max."
  :url "https://github.com/brunchboy/afterglow-max"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [afterglow "0.2.1-SNAPSHOT"]
                 [com.taoensso/timbre "4.2.1"]]
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

  :plugins [[lein-codox "0.9.1"]
            [lein-environ "1.0.1"]]

  :codox {:source-uri "http://github.com/brunchboy/afterglow-max/blob/master/{filepath}#L{line}"
          :metadata {:doc/format :markdown}}
  :min-lein-version "2.0.0")
