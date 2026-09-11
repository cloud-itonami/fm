# Operator quickstart — fm

`fm` は投資ファンドのドメイン基線である。中身は **1 本の seed スクリプト**
（`seed.ts`、165 レコード）と、それが何を書くかを固定する検査だけで、
サービスもワーカも持たない。

**この repo で今日できることは 2 つ、できないことが 1 つある。**

| | |
|---|---|
| ✅ §1 | 検査を回す |
| ✅ §2 | seed が書こうとしているものを、**送信せずに**数える |
| ⛔ §3 | seed を本番 PDS へ流す — **宛先が生きていないので今日は踏めない** |
| ✅ §4 | この tree の出所を原本と突き合わせる |

✅ の手順は **2026-09-01 に実際に実行した**。出力はその実測値をそのまま貼って
ある。⛔ は「まだ書いていない」ではなく、**踏もうとして測った結果**で、
測り方も §3 に置いた。

前提: Node 26 系（TypeScript を直接実行できること）。実測は `v26.7.0` /
`nbb v1.5.212`。ネットワークは §3 以外では一切使わない。

---

## 1. ✅ 検査を回す

```bash
npx --yes nbb --classpath test run_tests.cljk
```

```
=== Fund Management Seed ===
Seeded 165 fund records to did:web:fund.etzhayyim.com

Testing fm.seed-test

Ran 8 tests containing 25 assertions.
0 failures, 0 errors.

fm seed + blueprint: all green
```

冒頭 2 行は `seed.ts` 自身の `console.log` である。**PDS へは 1 本も出ていない**
—— `test/fm/seed_capture.cljk` が `fetch` を捕獲器に差し替えてから import する。

`fm seed + blueprint: all green` は全部緑のときだけ出る。緑でないときは
`FAILED` を出して exit 1 で終わる。**この行が無い成功は無い**ので、CI から
使うときは exit code か この行のどちらを見てもよい。

## 2. ✅ seed が書こうとしているものを、送信せずに数える

```bash
npx --yes nbb --classpath test inspect_seed.cljk
```

```
=== seed.ts が書こうとしているもの（送信なし）===
要求 合計          : 166
  putRecord        : 165
  actor.create     : 1

collection ごとの件数:
  com.etzhayyim.apps.fund.commitment  27
  com.etzhayyim.apps.fund.fund        28
  com.etzhayyim.apps.fund.investee    27
  com.etzhayyim.apps.fund.investor    27
  com.etzhayyim.apps.fund.manager     28
  com.etzhayyim.apps.fund.metric      28

fundKind ごとの件数（README が名乗る公開ドメイン）:
  government_fund  4
  investor_fund    4
  mutual_fund      5
  pension_fund     5
  private_fund     5
  sovereign_fund   5

rkey 相異なり      : 165 / 165 (衝突なし)

送信先ホスト:
  https://atproto.etzhayyim.com/xrpc/com.atproto.repo.putRecord
  https://atproto.etzhayyim.com/xrpc/com.etzhayyim.actor.create
```

**なぜこれが §3 より前にあるか。** `seed.ts` に dry-run スイッチは無い。唯一の
実行方法が本番 PDS への 166 本の書き込みなので、これが無いと operator が
「何が書かれるのか」を知る手段は *書いてから読む* しか無い。live な PDS に
対しては取り返しのつかない順序である。

`rkey 相異なり` を見ること。`putRecord` は rkey での upsert なので、**衝突は
失敗として観測されない** —— 全部 200 が返り、先に書いたレコードだけが黙って
消える。衝突があればこのコマンドは exit 1 で終わる。

数えられなかった場合（捕獲が壊れた・`seed.ts` が縮んだ）は理由を出して
**exit 1**。`0 件` とは印字しない —— 「測れなかった」と「測って 0 だった」を
出力で区別する。

## 3. ⛔ seed を本番 PDS へ流す — 今日は踏めない

`seed.ts` の宛先は 2 つとも live ではない。**2026-09-01 に測った:**

```bash
dig +short fund.etzhayyim.com
#   (何も返らない = A/AAAA レコードが無い)

curl -sS -o /dev/null -w '%{http_code}\n' \
     https://fund.etzhayyim.com/.well-known/did.json
#   000   ← 名前が引けないので接続にも至らない

curl -sS -o /dev/null -w '%{http_code}\n' \
     https://atproto.etzhayyim.com/xrpc/com.atproto.server.describeServer
#   530   ← 名前は Cloudflare に引けるが origin が応答しない
```

`fund.etzhayyim.com` が引けないことは、この repo にとって表面的な障害では
ない。**`did:web:fund.etzhayyim.com` は 165 本すべての `putRecord` の `repo`
であり、全レコードの `ownerDid` でもある**（`seed.ts` の `ROOT_DID`）。
did:web は解決に `https://<host>/.well-known/did.json` を要求するので、
DNS が無い間はこの DID を誰も検証できない。

もう一方の経路も無い。README が名乗っていた `etzhayyim` CLI は、この
workspace のどこにも実体が無い:

```bash
command -v etzhayyim                              # 何も出ない
find orgs -maxdepth 4 -name etzhayyim -type f     # 0 件
grep -rl '"etzhayyim"[[:space:]]*:' --include=package.json orgs   # 0 件
```

（`find` / `grep` が見るのは checkout 済みの repo だけである。west は 4,300
超を管理しており、未 checkout の repo は `find` にも `grep` にも映らない。
`nbb scripts/repo-search.cljs etzhayyim` も引いたが CLI を出す repo は無かった。）

**したがって §3 の解除条件は 3 つで、どれもこの repo の外にある:**

1. `fund.etzhayyim.com` に A/AAAA が付き、`/.well-known/did.json` が 200 を返す
2. `atproto.etzhayyim.com` の origin が復帰し、`describeServer` が 200 を返す
3. `etzhayyim_TOKEN` を発行できる経路がある（CLI が復活するか、別手段が決まる）

3 つとも満たされたら、実行は次の 1 行である。**先に §2 を回して、書かれる
165 本を読んでから流すこと。**

```bash
export etzhayyim_TOKEN="<PDS への書き込み token>"
npx --yes tsx seed.ts
```

`seed.ts` は token が無ければ import の時点で throw する（ネットワークへ出る
前に落ちる）。パスは repo ルート直下の `seed.ts` である —— README が長く
案内していた `60-apps/etzhayyim-project-fm/seed.ts` は抽出前の原本の位置で、
この repo には存在しない。

## 4. ✅ この tree の出所を原本と突き合わせる

`migration.edn` が抽出元を revision ごと名指ししているので、主張ではなく
コマンドで確かめられる。

```bash
cat migration.edn
#   :source {:repo "etzhayyim/root"
#            :path "60-apps/etzhayyim-project-fm"
#            :revision "691c245da48f3acb11dd757218f189ff2482b1c8"
#            :tracked-files 5 :bytes 23593}

git -C <superproject>/orgs/etzhayyim/root \
    ls-tree -r --name-only 691c245da48f3acb11dd757218f189ff2482b1c8 \
    -- 60-apps/etzhayyim-project-fm
#   60-apps/etzhayyim-project-fm/NOTICE
#   60-apps/etzhayyim-project-fm/PROJECT.jsonld
#   60-apps/etzhayyim-project-fm/README.md
#   60-apps/etzhayyim-project-fm/actors/fund-management-actor-map.yaml
#   60-apps/etzhayyim-project-fm/seed.ts
```

記録の `:tracked-files 5` と一致する。現在の tree は 10 ファイルで、増えた 5 つ
（`README.edn` `migration.edn` `run_tests.cljk` `test/fm/seed_capture.cljk`
`test/fm/seed_test.cljk`）はすべて抽出後にここで書かれたものである。

⚠ `git rev-parse --is-shallow-repository` が `true` を返す checkout では
この照合を信用しないこと。shallow な clone は ancestry の問いに **誤った答えを、
正しい答えと同じ顔で** 返す（superproject の ADR-2608124400）。実測した
`orgs/etzhayyim/root` は `false` だった。

---

## この repo に無いもの

`inspect_seed.cljk` が数えるのは **`seed.ts` が出す要求**であって、PDS の中身
ではない。seed 後に何が入ったかを確かめる読み取り側は、この repo に無い
（§3 が踏めるようになるまで書きようがない）。README が案内していた
`etzhayyim coverage world --domain …` がその役だったが、§3 のとおり CLI の
実体が無いので、**これを検証手段として引用しない**。
