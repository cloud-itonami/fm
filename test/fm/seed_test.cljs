;; seed_test.cljs — fm が「読める」だけでなく「辻褄が合っている」ことの検査。
;;
;; この repo は 2026-05 に etzhayyim/root から切り出されて以来、検査を 1 本も
;; 持っていなかった。中身は seed script 1 本と設計文書 3 本なので「テストする
;; 対象が無い」ように見えるが、実際には次の 3 つが互いを指している:
;;
;;   README.md   ── 6 つの公開ドメインと 6 つの collection を **約束している**
;;   seed.ts     ── 165 レコードを PDS に書く（その約束を果たすはずのもの）
;;   PROJECT.jsonld / actors/*.yaml ── 同じ 9 体の actor を **二重に**記述している
;;
;; どれが破れても何も throw しない。破れ方はこうなる:
;;
;;   * fundKind が 1 つ落ちる → `etzhayyim coverage world --domain X` がその
;;     ドメインだけ空を返す。seed は「165 件書いた」と言い切るので気づけない。
;;   * rkey が衝突する → putRecord は **upsert** なので後の 1 件が前を黙って
;;     上書きする。要求は 165 本出るし全部 200 が返る。**消えた分は数えられない。**
;;   * metric.fundId が実在しない fund を指す → 孤児レコードが PDS に残り、
;;     fund グラフを辿った側が nil に当たる。
;;   * 2 つの設計文書の actor id が食い違う → blueprint が実在しない actor を
;;     名指ししたまま、composition の実装まで誰も気づかない。
;;
;; ## README を読んでから検査する（写しを持たない）
;;
;; 期待値をこのファイルに焼くと、README が変わったときテストは緑のまま古い
;; 約束を守り続ける。だから **README.md を実際に読んで**、そこに書かれた
;; ドメイン名と collection 名を期待値にする。README と seed のどちらが動いても
;; 食い違いとして出る。
;;
;; ## 「測れなかった」を「問題なし」と区別する（superproject CLAUDE.md）
;;
;; README の箇条書きを読み損ねると期待値が空集合になり、**空集合との比較は
;; 常に真**になる。それは「約束が守られている」ではなく「約束を読めなかった」
;; なので、床を切ったら throw する。捕獲側の床は seed_capture.cljs にある。
(ns fm.seed-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.set :as set]
            [clojure.string :as str]
            [fm.seed-capture :as cap]
            ["fs" :as fs]
            ["path" :as path]
            ["child_process" :as cp]))

(def root (js/process.cwd))
(defn- slurp* [rel] (.toString (fs/readFileSync (path/join root rel) "utf8")))

(defn- tracked
  "repo が宣言しているファイル。git に訊く —— fs の walk は『無い』と
   『見ていない』を区別できない。git が答えられないなら答えを拒否する。"
  []
  (let [out (try (.toString (cp/execFileSync "git" #js ["ls-files"]
                                             #js {:cwd root :maxBuffer 33554432}))
                 (catch :default e
                   (throw (ex-info (str "git ls-files が実行できないので、この repo が"
                                        " 何を宣言しているか分からない。合格を報告しない。 "
                                        (.-message e)) {:root root}))))
        fs* (vec (remove str/blank? (str/split-lines out)))]
    (when (empty? fs*)
      (throw (ex-info "git ls-files が 0 件を返した。0 件の検査は合格ではない。" {})))
    fs*))

;; ── README が立てている約束を読む ──────────────────────────────────────────
(defn- bullets-under
  "README の `- <見出し>:` に続く、1 段深い `` - `x` `` を集める。
   `floor` 件に満たなければ **読めなかった**ということなので throw する。"
  [heading floor]
  (let [lines (str/split-lines (slurp* "README.md"))
        after (drop-while #(not (str/includes? % heading)) lines)
        _ (when (empty? after)
            (throw (ex-info (str "README.md に見出しが無い: " heading
                                 "。約束を読めていないので合格を報告しない。") {})))
        items (->> (rest after)
                   (take-while #(str/starts-with? % "  - "))
                   (keep #(second (re-find #"`([^`]+)`" %)))
                   vec)]
    (when (< (count items) floor)
      (throw (ex-info (str "README.md の『" heading "』から " (count items)
                           " 件しか読めなかった（床 " floor "）。"
                           "箇条書きの形が変わったか、約束が縮んだ。どちらも合格ではない。")
                      {:got items :floor floor})))
    (set items)))

(def promised-domains     (delay (bullets-under "Public domains covered by this seed:" 6)))
(def promised-collections (delay (bullets-under "Seeded collections:" 6)))

;; ── seed が実際に書くもの ──────────────────────────────────────────────────
(defn- by-collection [] (group-by :collection (cap/records)))

;; collection ごとに実際に書かれる件数の床（2026-08-19 実測）。**全体の床だけでは
;; 足りない** —— 1 つの collection が空になると、そこを歩く検査は空集合を歩いて
;; 緑になる（重複も孤児参照も「0 件」だから）。実測でこれを踏んだ: 捕獲の形を
;; 読み違えた版で rkey・id・参照の 4 検査が **何も見ずに緑**になった。
(def floor-per-collection
  {"fund" 28 "manager" 28 "investor" 27 "investee" 27 "metric" 28 "commitment" 27})

(defn- coll
  "その collection のレコード。床を下回ったら **答えを拒否する**。"
  [short]
  (let [rs (get (by-collection) (str "com.etzhayyim.apps.fund." short) [])
        floor (get floor-per-collection short)]
    (when (< (count rs) floor)
      (throw (ex-info (str "collection " short " が床を下回った: " (count rs) " < " floor
                           "。この集合を歩く検査は空振りするので合格を報告しない。")
                      {:collection short :got (count rs) :floor floor})))
    rs))

(defn- field [short k] (mapv #(get-in % [:record k]) (coll short)))

(deftest readme-promises-the-collections-the-seed-actually-writes
  (testing "README が挙げた collection と seed が書く collection が一致する"
    ;; 片側だけ動くと静かに壊れる。README にあって seed が書かない collection は
    ;; 永久に空のまま（coverage がその分だけ立ち上がらない）。seed が書いて
    ;; README に無い collection は、読む側が存在を知らないので誰も引かない。
    (let [written (set (keys (by-collection)))
          promised @promised-collections]
      (is (empty? (set/difference promised written))
          (str "README が約束したのに seed が書かない collection: "
               (pr-str (sort (set/difference promised written)))))
      (is (empty? (set/difference written promised))
          (str "seed が書くのに README が挙げていない collection: "
               (pr-str (sort (set/difference written promised))))))))

(deftest every-public-domain-the-readme-names-is-actually-seeded
  (testing "README の 6 ドメインが fundKind として実際に生成される"
    ;; この repo の存在理由そのもの（README「Why this exists」）。1 つ落ちても
    ;; seed は「165 件書いた」と報告するので、`coverage world` を叩いて
    ;; そのドメインだけ空なのを見るまで分からない。
    (let [kinds (set (field "fund" :fundKind))
          missing (set/difference @promised-domains kinds)]
      (is (empty? missing)
          (str "README が覆うと言ったのに seed が 1 件も作らないドメイン: "
               (pr-str (sort missing)) " / 実際に作られた: " (pr-str (sort kinds)))))))

(deftest rkeys-are-unique-so-no-record-silently-overwrites-another
  (testing "putRecord は rkey での upsert —— 衝突は前のレコードを黙って消す"
    ;; stableRkey は小文字化・非英数の畳み込み・512 文字での切り詰めをする。
    ;; 異なる id が同じ rkey に落ちても、要求は全部出て全部 200 が返り、
    ;; seed は満額の件数を報告する。**消えた分はどこにも数えられない。**
    (doseq [short (sort (keys floor-per-collection))]
      (let [c     (str "com.etzhayyim.apps.fund." short)
            recs  (coll short)
            rkeys (map :rkey recs)
            dupes (->> rkeys frequencies (filter #(> (val %) 1)) (map key) sort)]
        (is (empty? dupes)
            (str c " で rkey が衝突している（後の 1 件が前を上書きする）: "
                 (pr-str (vec dupes))))))))

(deftest ids-are-unique-within-each-collection
  (testing "同じ collection に同じ id が 2 度現れない"
    (doseq [[short k] [["fund" :fundId] ["manager" :managerId] ["investor" :investorId]
                       ["investee" :investeeId] ["metric" :metricId]
                       ["commitment" :commitmentId]]]
      (let [ids (field short k)
            dupes (->> ids frequencies (filter #(> (val %) 1)) (map key) sort)]
        (is (empty? dupes) (str short "." (name k) " が重複している: " (pr-str (vec dupes))))))))

(deftest every-reference-resolves-to-a-record-the-seed-also-writes
  (testing "metric / commitment が指す fund・investor が実在する"
    ;; 参照先が無くても putRecord は通る。孤児レコードは PDS に残り、
    ;; fund グラフを辿った側が nil に当たって初めて分かる。
    (let [funds     (set (field "fund" :fundId))
          investors (set (field "investor" :investorId))]
      (is (empty? (remove funds (field "metric" :fundId)))
          (str "metric.fundId が実在しない fund を指している: "
               (pr-str (sort (distinct (remove funds (field "metric" :fundId)))))))
      (is (empty? (remove funds (field "commitment" :fundId)))
          (str "commitment.fundId が実在しない fund を指している: "
               (pr-str (sort (distinct (remove funds (field "commitment" :fundId)))))))
      (is (empty? (remove investors (field "commitment" :investorId)))
          (str "commitment.investorId が実在しない investor を指している: "
               (pr-str (sort (distinct (remove investors (field "commitment" :investorId))))))))))

(deftest called-capital-never-exceeds-the-commitment
  (testing "呼び出し済み資本が約束額を超えない"
    ;; 超えた値は金額として不能で、そのまま NAV / metric の入力になる。
    ;; 型としては両方ただの number なので、突き合わせない限り通る。
    (let [bad (->> (coll "commitment")
                   (map :record)
                   (filter #(> (:calledAmount %) (:commitmentAmount %)))
                   (mapv :commitmentId))]
      (is (empty? bad) (str "calledAmount > commitmentAmount: " (pr-str bad))))))

;; ── 設計文書どうし ────────────────────────────────────────────────────────
(defn- jsonld-actors
  "PROJECT.jsonld が名指しする actor を、構造を歩いて集める（正規表現で
   なぞらない）。`actors` キーの配列だけを見る。"
  []
  (let [doc (js->clj (js/JSON.parse (slurp* "PROJECT.jsonld")) :keywordize-keys false)
        acc (atom [])
        walk (fn walk [x]
               (cond
                 (map? x) (doseq [[k v] x]
                            (if (and (= k "actors") (vector? v))
                              (swap! acc into (filter string? v))
                              (walk v)))
                 (vector? x) (doseq [v x] (walk v))
                 :else nil))]
    (walk doc)
    (set @acc)))

(defn- yaml-actors
  "actor map が名指しする actor。`source_actor:` の値だけを読む。"
  []
  (->> (str/split-lines (slurp* "actors/fund-management-actor-map.yaml"))
       (keep #(second (re-find #"^\s*source_actor:\s*(\S+)\s*$" %)))
       set))

(deftest the-blueprint-and-the-actor-map-name-the-same-actors
  (testing "PROJECT.jsonld と actors/*.yaml が同じ actor を指す"
    ;; 同じ構成を 2 箇所が別々に書いている。片方だけ更新されると、blueprint が
    ;; 実在しない actor を名指ししたまま composition の実装まで進む。
    (let [j (jsonld-actors) y (yaml-actors)]
      ;; 読めなかったことを一致として報告しない。
      (is (<= 9 (count j)) (str "PROJECT.jsonld から actor を " (count j) " 体しか読めなかった"))
      (is (<= 9 (count y)) (str "actor map から actor を " (count y) " 体しか読めなかった"))
      (is (empty? (set/difference j y))
          (str "blueprint にあって actor map に無い: " (pr-str (sort (set/difference j y)))))
      (is (empty? (set/difference y j))
          (str "actor map にあって blueprint に無い: " (pr-str (sort (set/difference y j))))))))

(deftest the-project-file-points-at-a-seed-script-that-exists
  (testing "PROJECT.jsonld の etzhayyim:seedScript が実在する tracked file を指す"
    (let [doc (js->clj (js/JSON.parse (slurp* "PROJECT.jsonld")) :keywordize-keys false)
          declared (get doc "etzhayyim:seedScript")
          files (set (tracked))]
      (is (string? declared) "PROJECT.jsonld が seedScript を宣言していない")
      (is (contains? files declared)
          (str "PROJECT.jsonld が指す seed script が repo に無い: " (pr-str declared))))))
