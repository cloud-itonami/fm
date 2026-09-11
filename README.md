# fm — 投資ファンドのドメイン基線

`fm` は **fund management**。`fund.etzhayyim.com` が扱う投資ファンド
（ソブリン / 投資信託 / 年金 / プライベート / 政府系 / アロケータ）の
**基線レコードを 1 本の seed スクリプトとして持つだけの repo** である。

**サービスでもワーカでもライブラリでもない。** 実行できるものは 3 つで、
中身は 10 ファイルしかない:

| ファイル | 何か |
|---|---|
| `seed.ts` | 165 レコードを PDS へ書く。この repo の実質的な中身 |
| `run_tests.cljk` | seed が書くものを固定する検査（8 tests / 25 assertions） |
| `inspect_seed.cljk` | seed が書こうとしているものを**送信せずに**数える |

**まず [`docs/operator-quickstart.md`](docs/operator-quickstart.md) を読むこと。**
何が今日踏めて、何が踏めないかを実測付きで書いてある。

```bash
npx --yes kbb --backend sci --classpath test run_tests.cljk      # 検査
npx --yes kbb --backend sci --classpath test inspect_seed.cljk   # 送信せずに中身を数える
```

## 何を seed するか

> ⚠ **次の 2 つの箇条書きは検査が読む契約である。**
> `test/fm/seed_test.cljk` の `bullets-under` が、この見出し行と、続く
> `  - ` で始まる行の **最初のバッククォート**を読んで期待値にする。
> 期待値をテスト側に焼くと README だけが古くなっても緑のままになるので、
> **README が正本**にしてある。見出し文と字下げを変えると検査は緑にならず、
> 「読めなかった」として throw する（空集合との比較で緑にはならない）。
> 項目を消すことは、seed から消すことと同じ意味を持つ。

- Public domains covered by this seed:
  - `sovereign_fund` — 5 件
  - `mutual_fund` — 5 件
  - `pension_fund` — 5 件
  - `private_fund` — 5 件
  - `government_fund` — 4 件
  - `investor_fund` — 4 件
- Seeded collections:
  - `com.etzhayyim.apps.fund.fund` — 28 件
  - `com.etzhayyim.apps.fund.manager` — 28 件
  - `com.etzhayyim.apps.fund.metric` — 28 件
  - `com.etzhayyim.apps.fund.investor` — 27 件
  - `com.etzhayyim.apps.fund.investee` — 27 件
  - `com.etzhayyim.apps.fund.commitment` — 27 件

計 165 レコード。件数は `inspect_seed.cljk` の実測で、collection ごとの床は
検査側（`floor-per-collection`）にも入っている。

実在の 4 ファンド（GPFG / Vanguard Total World / CPP Investments /
SoftBank Vision）が基線で、残りは `seed.ts` の中で 24 回まわる合成ループが
6 つの `fundKind` × 12 法域から作る。**合成分は実在の組織ではない** ——
カバレッジの床を張るための骨格であって、出典のあるデータではない。

## なぜ別に在るか

`public_fund` は `public-fund.etzhayyim.com` の**別ドメイン**である
（クラウドファンディング / 予算執行）。投資ファンドのカバレッジは
`public_fund` からではなく `fund.etzhayyim.com` のレコードから起こす。

## ⚠ 今日 seed は流せない

`seed.ts` の宛先 2 つとも live ではない（2026-09-01 実測）:

- `fund.etzhayyim.com` — **DNS レコードが無い**。これは 165 本すべての
  `putRecord` の `repo` かつ全レコードの `ownerDid`（`did:web:fund.etzhayyim.com`）
  なので、解決できない間はこの DID を誰も検証できない
- `atproto.etzhayyim.com` — 名前は引けるが origin が **530**

以前この README は `etzhayyim seed --app fund` を「Authoritative path」として、
`etzhayyim coverage world --domain …` を検証手段として案内していた。**この CLI は
この workspace に実体が無い**（`command -v` / `find` / `grep` / `repo-search`
いずれも 0 件）。また seed のパスを `60-apps/etzhayyim-project-fm/seed.ts` と
書いていたが、それは抽出前の原本の位置で、この repo では `seed.ts` である。
測り方と解除条件は quickstart の §3 に置いた。

`inspect_seed.cljk` が在るのはこのためでもある —— `seed.ts` に dry-run が無い
以上、宛先が復帰した日に「何が書かれるのか」を書く前に読む手段が要る。

## 出所

`etzhayyim/root` の `60-apps/etzhayyim-project-fm`（revision
`691c245d`、5 ファイル）からの抽出物。`migration.edn` が revision ごと
記録しており、quickstart §4 に突き合わせ手順がある。
