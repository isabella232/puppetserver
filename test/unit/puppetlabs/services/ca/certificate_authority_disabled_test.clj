(ns puppetlabs.services.ca.certificate-authority-disabled-test
  (:require [clojure.test :refer :all]
            [me.raynes.fs :as fs]
            [puppetlabs.services.ca.certificate-authority-disabled-service :as disabled]
            [puppetlabs.services.jruby.jruby-puppet-testutils :as jruby-testutils]
            [puppetlabs.services.jruby.jruby-puppet-service :as jruby-puppet]
            [puppetlabs.services.puppet-profiler.puppet-profiler-service :as profiler]
            [puppetlabs.trapperkeeper.app :as tk-app]
            [puppetlabs.trapperkeeper.testutils.logging :as logutils]
            [puppetlabs.trapperkeeper.testutils.bootstrap :as tk-testutils]
            [puppetlabs.trapperkeeper.services.authorization.authorization-service :as tk-auth]
            [puppetlabs.services.jruby.jruby-puppet-service :as jruby]
            [puppetlabs.kitchensink.core :as ks]))

(deftest ca-disabled-files-test
  (testing "Ensure no certificates are generated when CA disabled service is enabled."
    (logutils/with-test-logging
     (let [puppet-conf-dir (str (ks/temp-dir))
           config (-> (jruby-testutils/jruby-puppet-tk-config
                       (jruby-testutils/jruby-puppet-config
                        {:max-active-instances 1}))
                      (assoc-in [:jruby-puppet :master-conf-dir]
                                puppet-conf-dir)
                      (assoc :puppet {"vardir" (str puppet-conf-dir "/var")}))]
       (tk-testutils/with-app-with-config
        app
        [profiler/puppet-profiler-service
         jruby-puppet/jruby-puppet-pooled-service
         (jruby-testutils/mock-jruby-pool-manager-service config)
         disabled/certificate-authority-disabled-service
         tk-auth/authorization-service]
        config
        (let [jruby-service (tk-app/get-service app :JRubyPuppetService)]
          (jruby/with-jruby-puppet
           jruby-puppet jruby-service :ca-disabled-files-test
           (is (not (fs/exists? (fs/file puppet-conf-dir "ssl")))))))))))
