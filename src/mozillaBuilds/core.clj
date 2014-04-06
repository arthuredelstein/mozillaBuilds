(ns mozillaBuilds.core
  "A script that produces a map of Mozilla commit hashes to
   nightly builds."
  (:require [net.cgrand.enlive-html :as enlive]
            [clojure.string :as string]))

(defn fetch-url
  "Download a URL."
  [url]
  (enlive/html-resource (java.net.URL. url)))

(defn select-links
  "Select all :a nodes on a page."
  [nodes]
  (enlive/select nodes [:a]))

(defn extract-urls
  "Read the URLs on a web page. Prefixes with current location."
  [url]
  (->> (select-links (fetch-url url))
       (map :attrs)
       (map :href)
       (map #(str url %))))
                        
(defn get-build-folders
  "Reads all URLs that contain a build folder
   like '1395246592/' ."
  [url]
  (->> (extract-urls url)
       (filter #(re-find #"\d+\/\z" %))))

(defn first-file-of-type
  "Given a list of URLs, selects the first
   instance of a URL with suffix type."
  [type urls]
  (first (filter #(.endsWith % type) urls)))

(defn map-hash-to-build
  "Produces a map containing a single Mozilla commit hash
   to a single install file."
  [folder install-type]
  (let [urls (extract-urls folder)
        dmg (first-file-of-type install-type urls)
        txt (first-file-of-type ".txt" urls)
        hash (when txt
               (-> (second (string/split-lines (slurp txt)))
                   (string/split #"\/")
                   last))]
    {hash dmg}))
    
(defn map-hashes-to-builds
  "Produces a map of Mozilla commit hashes to install files."
  [build-list-url install-type]
  (into {} (map #(map-hash-to-build % install-type) (get-folders build-list-url))))
    
(def build-list 
  "https://ftp.mozilla.org/pub/mozilla.org/firefox/tinderbox-builds/mozilla-inbound-macosx64/")
