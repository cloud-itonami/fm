;; seed_capture.cljs — 実物の `seed.ts` を **ネットワークなしで** 走らせて、
;; PDS に書かれるはずのレコードをそのまま捕まえる。
;;
;; なぜコピーではなく実物を走らせるか。この repo の値（FUNDS / MANAGERS /
;; INVESTORS / INVESTEES / METRICS / COMMITMENTS と、24 回まわる synthetic
;; ループ）は seed.ts の中にしか無い。テスト側に写しを置けば、写しの方だけが
;; 正しいまま実物が壊れる —— 検査したいのは実物である。
;;
;; Node 26 は TypeScript をそのまま実行できるので、`fetch` を差し替えてから
;; import すれば、seed が出す 166 本の要求が全部手元に残る。`main()` は module
;; 本体から await されずに走り出すが、中の await は全て即解決の promise なので、
;; **macrotask を 1 回挟めば** microtask 待ち行列は全部掃ける。
;;
;; ## 「測れなかった」を「問題なし」と区別する（superproject CLAUDE.md）
;;
;; この形の検査の既定の失敗は、**捕まえられなかったときに黙って緑になる**こと
;; である。stub が入らない・import が転ける・main() が走り切る前に読む —— どれも
;; 「違反 0 件」に見える。だから `records` は床を切ったら **throw する**。
;; 0 件を検査して 0 件の違反は、合格ではない。
(ns fm.seed-capture
  (:require ["path" :as path]))

(defonce ^:private calls (atom []))
(defonce ^:private done (atom false))

(def root (js/process.cwd))

;; seed が出す要求の実測本数（2026-08-19）。内訳は actor.create 1 + putRecord 165。
;; seed の中身が増えたらここを上げる。**下回ったら本物の欠落か、捕獲そのものの
;; 破損**で、どちらも黙って通してはならない。
(def floor-calls 166)

(defn- stub-fetch!
  "`fetch` を捕獲器に差し替える。**200 を返し続ける** —— seed は !res.ok で throw
   するので、ここで失敗を返すと『seed が壊れている』と区別できない赤になる。"
  []
  (set! (.-fetch js/globalThis)
        (fn [url opts]
          (swap! calls conj
                 {:url url
                  :body (js->clj (js/JSON.parse (.-body opts)) :keywordize-keys true)})
          (js/Promise.resolve
           #js {:ok true :status 200 :text (fn [] (js/Promise.resolve ""))}))))

(defn capture!
  "seed.ts を走らせて要求を捕まえる。promise を返す。"
  []
  (stub-fetch!)
  ;; seed は token が無いと import 時点で throw する。ネットワークへは出ないので
  ;; 値は何でもよいが、**本物の token を読みに行かない**ことがここでは重要。
  (set! (.-etzhayyim_TOKEN js/process.env) "seed-test-no-network")
  (-> (js/import (str "file://" (path/join root "seed.ts")))
      ;; macrotask を 1 回挟んで main() の microtask 連鎖を掃く。
      (.then (fn [_] (js/Promise. (fn [res] (js/setTimeout res 0)))))
      (.then (fn [_] (reset! done true) @calls))))

(defn requests
  "捕まえた要求。**捕獲に失敗していたら答えを拒否する。**"
  []
  (when-not @done
    (throw (ex-info "seed.ts の捕獲が完了していない。合格を報告しない。" {})))
  (let [c @calls]
    (when (< (count c) floor-calls)
      (throw (ex-info (str "seed.ts が出した要求が床を下回った: " (count c)
                           " < " floor-calls
                           "。捕獲が壊れたか、seed が黙って縮んだ。どちらも合格ではない。")
                      {:got (count c) :floor floor-calls})))
    c))

(def floor-records
  "putRecord されたレコードの床（2026-08-19 実測）。collection ごとの床は
   seed_test.cljs 側にある —— **全体が足りていても 1 つの collection が空**
   なら、その collection の検査は空集合を歩いて緑になるからである。"
  165)

(defn records
  "putRecord されたレコード本体だけ（actor.create を除く）。
   `:collection` / `:rkey` / `:record` は要求 body の中にある。"
  []
  (let [rs (into [] (comp (map :body) (filter :collection)) (requests))]
    (when (< (count rs) floor-records)
      (throw (ex-info (str "putRecord が床を下回った: " (count rs) " < " floor-records
                           "。捕獲の形が変わったか、seed が黙って縮んだ。"
                           "どちらも合格ではない。")
                      {:got (count rs) :floor floor-records})))
    rs))
