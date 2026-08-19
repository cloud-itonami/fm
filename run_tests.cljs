#!/usr/bin/env nbb
;; run_tests.cljs — fm の検査。
;;
;;   nbb --classpath test run_tests.cljs
;;
;; ネットワークには出ない。`seed.ts` は本物を走らせるが、`fetch` を捕獲器に
;; 差し替えてあるので PDS には 1 本も届かない（seed_capture.cljs を参照）。
;;
;; workspace の規則（superproject CLAUDE.md）で script host は nbb に一本化されて
;; おり、新規の .sh / .mjs は禁止。よって runner は nbb + cljs.test である。
(ns run-tests
  (:require [clojure.test :as t]
            [fm.seed-capture :as cap]
            [fm.seed-test]))

(def green-marker
  "scripts/maturity-loop/mutations.edn の `:green-marker`。全部緑のときだけ出す ——
   出力に現れるかどうかで mutation が噛んだかを判定するので、緑でないときに
   印字してはならない。"
  "fm seed + blueprint: all green")

(defmethod t/report [:cljs.test/default :end-run-tests] [m]
  (if (t/successful? m)
    (println (str "\n" green-marker))
    (do (println "\nfm seed + blueprint: FAILED")
        (js/process.exit 1))))

;; 捕獲が転けたら **テストを走らせずに落ちる**。捕獲できないまま検査を走らせると
;; 「違反 0 件」に見えてしまう（seed_capture の床はそのための二重底）。
(-> (cap/capture!)
    (.then (fn [_] (t/run-tests 'fm.seed-test)))
    (.catch (fn [e]
              (println "\nfm seed + blueprint: FAILED — seed.ts を捕獲できなかった")
              (println (.-stack e))
              (js/process.exit 1))))
