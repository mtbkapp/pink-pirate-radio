(defproject pink-pirate-radio-server "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]

                 ; dependencie injection
                 [com.stuartsierra/component "1.1.0"]

                 ; sqlite jdbc driver 
                 [org.xerial/sqlite-jdbc "3.39.3.0"]

                 ; datbase migrations
                 [org.liquibase/liquibase-core "3.10.3"]

                 ; clojure jdbc wrapper
                 [com.github.seancorfield/next.jdbc "1.3.834"]

                 ; http server
                 [ring/ring-jetty-adapter "1.9.6"]

                 ; http handling 
                 [ring/ring-core "1.9.6"]
                 [ring/ring-defaults "0.3.4"]
                 [ring/ring-json "0.5.1"]

                 ; http routing
                 [compojure "1.7.0"]]
  :main ^:skip-aot pink-pirate-radio-server.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}
             :dev {:dependencies [[clj-http "3.12.3"]]}})
