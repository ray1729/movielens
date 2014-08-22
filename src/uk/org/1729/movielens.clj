(ns uk.org.1729.movielens
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clj-time.format :as tf]
            [clj-time.coerce :refer [to-date]]
            [clojurewerkz.titanium.graph :as tg]
            [clojurewerkz.titanium.schema :as ts]
            [clojurewerkz.titanium.vertices :as tv]
            [clojurewerkz.titanium.edges :as te]))

(defn generate-config
  "Generate a configuration map to use BerkeleyDB and Lucene
   with data stored in the given directory."
  [dir]
  {"storage.backend" "berkeleyje"
   "storage.directory" (.getPath (io/file dir "bdb"))
   "index.search.backend" "lucene"
   "index.search.directory" (.getPath (io/file dir "lucene"))})

(defn init-graph
  [path]
  (let [config (generate-config (io/file path))
        graph  (tg/open config)]
    (ts/with-management-system [mgmt graph]
      (ts/make-vertex-label mgmt "Genre")
      (ts/make-property-key mgmt :genre-id Long)
      (ts/build-composite-index mgmt :genre-id-idx :vertex [:genre-id] :unique? true)
      (ts/make-property-key mgmt :genre String)
      (ts/build-composite-index mgmt :genre-idx :vertex [:genre] :unique? true)
      (ts/make-vertex-label mgmt "Movie")
      (ts/make-property-key mgmt :movie-id Long)
      (ts/build-composite-index mgmt :movie-id :vertex [:movie-id] :unique? true)
      (ts/make-property-key mgmt :title String)
      (ts/build-mixed-index mgmt :movie-title-idx :vertex [:title] "search")
      (ts/make-property-key mgmt :release-date java.util.Date)
      (ts/make-property-key mgmt :video-release-date java.util.Date)
      (ts/make-property-key mgmt :imdb-url String)
      (ts/make-vertex-label mgmt "User")
      (ts/make-property-key mgmt :user-id Long)
      (ts/build-composite-index mgmt :user-id-idx :vertex [:user-id] :unique? true)
      (ts/make-property-key mgmt :age Long)
      (ts/build-mixed-index mgmt :user-age-idx :vertex [:age] "search")
      (ts/make-property-key mgmt :gender String)
      (ts/build-composite-index mgmt :user-gender-idx :vertex [:gender])
      (ts/make-property-key mgmt :occupation String)
      (ts/build-composite-index mgmt :user-occupation-idx :vertex [:occupation])
      (ts/make-property-key mgmt :zip-code String)
      (ts/build-composite-index mgmt :user-zip-code-idx :vertex [:zip-code])
      (ts/make-edge-label mgmt "IS_GENRE" :multiplicity :many-to-many)
      (ts/make-edge-label mgmt "RATED" :multiplitity :many-to-many)
      (ts/make-property-key mgmt :rating Long)
      (ts/make-property-key mgmt :timestamp Long))
    graph))

(defn open-graph
  [path]
  (tg/open (generate-config path)))

(defn close-graph
  [graph]
  (tg/shutdown graph))

(defn- find-by-id
  [key-name graph id]
  (first (tv/find-by-kv graph key-name id)))

(def find-genre-by-id (partial find-by-id :genre-id))
(def find-movie-by-id (partial find-by-id :movie-id))
(def find-user-by-id  (partial find-by-id :user-id))

(defn create-genre
  [graph genre]
  (tv/create-with-label! graph "Genre" genre))

(defn create-user
  [graph user]
  (tv/create-with-label! graph "User" user))

(defn create-movie
  [graph movie]
  (let [m (tv/create-with-label! graph "Movie" (dissoc movie :genres))]
    (doseq [genre-id (:genres movie)]
      (let [genre (find-genre-by-id graph genre-id)]
        (te/connect! graph m "IS_GENRE" genre)))))

(defn create-rating
  [graph {:keys [user-id movie-id rating timestamp]}]
  (let [user  (find-user-by-id graph user-id)
        movie (find-movie-by-id graph movie-id)]
    (te/connect! graph user "RATED" movie {:rating rating :timestamp timestamp})))

(defn parse-long
  [s]
  (Long/parseLong s))

(let [formatter (tf/formatter "dd-MMM-yyyy")]
  (defn try-parse-date
    [s]
    (try
      (to-date (tf/parse formatter s))
      (catch Exception _))))

(defn parse-genre
  [s]
  (let [[genre id] (str/split s #"\|")]
    {:genre-id (parse-long id) :genre genre}))

(defn parse-user
  [s]
  (let [[id age gender occupation zipcode] (str/split s #"\|")]
    {:user-id    (parse-long id)
     :age        (parse-long age)
     :gender     gender
     :occupation occupation
     :zipcode    zipcode}))

(defn parse-genres
  "Return the list of indices for which (nth xs ix) is 1."
  [xs]
  (filter (fn [ix] (= (nth xs ix) "1")) (range (count xs))))

(defn parse-movie
  [s]
  (let [[id title release-date video-release-date imdb-url & genres] (str/split s #"\|")
        release-date (try-parse-date release-date)
        video-release-date (try-parse-date video-release-date)]
    (cond-> {:movie-id           (parse-long id)
             :title              title
             :imdb-url           imdb-url
             :genres             (parse-genres genres)}
            release-date (assoc :release-date release-date)
            video-release-date (assoc :video-release-date video-release-date))))

(defn parse-rating
  [s]
  (zipmap [:user-id :movie-id :rating :timestamp]
          (map parse-long (str/split s #"\s+"))))

(defn load-data
  [parse create graph file]
  (with-open [rdr (io/reader file)]
    (with-redefs [find-genre-by-id (memoize find-genre-by-id)
                  find-user-by-id  (memoize find-user-by-id)
                  find-movie-by-id (memoize find-movie-by-id)]
      (doseq [record (->> (line-seq rdr) (map str/trim) (remove empty?))]
        (create graph (parse record))))))

(def load-genres  (partial load-data parse-genre  create-genre))
(def load-users   (partial load-data parse-user   create-user))
(def load-movies  (partial load-data parse-movie  create-movie))
(def load-ratings (partial load-data parse-rating create-rating))

(defn load-movielens-data
  [graph data-dir]
  (doseq [[load filename] [[load-genres  "u.genre"]
                           [load-users   "u.user"]
                           [load-movies  "u.item"]
                           [load-ratings "u.data"]]]
    (println (str "Loading " filename))
    (tg/with-transaction [tx graph]
      (load tx (io/file data-dir filename)))))

(defn movies-rated-by-user
  [graph user]
  (map te/head-vertex (tv/outgoing-edges-of user "RATED")))

(defn users-who-rated-movie
  [graph movie]
  (map te/tail-vertex (tv/incoming-edges-of movie "RATED")))

(defn users-with-overlapping-ratings
  "Return the users who rated at least one movie in common with `user`."
  [graph user]
  (reduce (fn [accum movie]
            (into accum (map te/tail-vertex (tv/incoming-edges-of movie "RATED"))))
          #{}
          (movies-rated-by-user graph user)))

(defn user-ratings
  "Return a map of movie-id/rating for all the movies rated by `user`."
  [graph user]
  (reduce (fn [accum rating]
            (let [movie (te/head-vertex rating)]
              (assoc accum (tv/get movie :movie-id) (te/get rating :rating))))
          {}
          (tv/outgoing-edges-of user "RATED")))
