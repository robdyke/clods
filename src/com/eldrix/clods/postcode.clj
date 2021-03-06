(ns com.eldrix.clods.postcode
  (:require
    [clojure.string :as str]
    [clojure.java.io :as io]
    [clojure.data.json :as json]
    [clojure.data.csv :as csv]
    [clojure.spec.alpha :as s])

  (:import
    (java.io InputStreamReader)))

;; The NHS Postcode directory ("NHSPD") lists all current and terminated postcodes in the UK
;; and relates them to a range of current statutory administrative, electoral, health and other
;; geographies.
;;
;; Unfortunately, it is not possible to automatically download these data from a machine-readable
;; canonical resource, as far as I know, but the download is available manually.
;;
;; The February 2020 release is available at:
;; https://geoportal.statistics.gov.uk/datasets/nhs-postcode-directory-uk-full-february-2020
;;
;;

(def field-names ["PCD2" "PCDS" "DOINTR" "DOTERM" "OSEAST100M"
                  "OSNRTH100M" "OSCTY" "ODSLAUA" "OSLAUA" "OSWARD"
                  "USERTYPE" "OSGRDIND" "CTRY" "OSHLTHAU" "RGN"
                  "OLDHA" "NHSER" "CCG" "PSED" "CENED"
                  "EDIND" "WARD98" "OA01" "NHSRLO" "HRO"
                  "LSOA01" "UR01IND" "MSOA01" "CANNET" "SCN"
                  "OSHAPREV" "OLDPCT" "OLDHRO" "PCON" "CANREG"
                  "PCT" "OSEAST1M" "OSNRTH1M" "OA11" "LSOA11"
                  "MSOA11" "CALNCV" "STP"])

(defn normalize
  "Normalizes a postcode into uppercase 8-characters with left-aligned outward code and right-aligned inward code
  returning the original if normalization not possible"
  [pc]
  (str/upper-case (let [codes (str/split pc #"\s+")] (if (= 2 (count codes)) (apply #(format "%-5s %3s" %1 %2) codes) pc))))

(defn egif
  "Normalizes a postcode into uppercase with outward code and inward codes separated by a single space"
  [pc]
  (str/upper-case (str/replace pc #"\s+" " ")))

(defn distance-between
  "Calculates the distance between two postcodes, determined by the square root of the sum of the square of
  the difference in grid coordinates (Pythagoras), result in metres.
  Parameters:
  - pc1d - first postcode NHSPD data (map)
  - pc2d - second postcode NHSPD data (map)"
  [pcd1 pcd2]
  (let [n1 (:OSNRTH1M pcd1)
        n2 (:OSNRTH1M pcd2)
        e1 (:OSEAST1M pcd1)
        e2 (:OSEAST1M pcd2)]
    (when (every? number? [n1 n2 e1 e2])
      (let [nd (- n1 n2)
            ed (- e1 e2)]
        (Math/sqrt (+ (* nd nd) (* ed ed)))))))


(defn import-postcodes
  "Import batches of postcodes to the function specified, each formatted as a vector representing
  [PCDS PCD2 json-data]."
  [in f]
  (with-open [is (io/input-stream in)]
    (->> is
         (InputStreamReader.)
         (csv/read-csv)
         (map #(zipmap field-names %))
         (map #(update % "OSNRTH1M" (fn [coord] (when-not (str/blank? coord) (Integer/parseInt coord)))))
         (map #(update % "OSEAST1M" (fn [coord] (when-not (str/blank? coord) (Integer/parseInt coord)))))
         (map #(vector (get % "PCDS") (get % "PCD2") (json/write-str %)))
         (partition-all 10000)
         (run! #(f %)))))

(comment

  ;; this is the Feb 2020 release file (928mb)
  (def filename "/Users/mark/Downloads/NHSPD_FEB_2020_UK_FULL/Data/nhg20feb.csv")

  )
