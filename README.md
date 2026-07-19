# etzhayyim-project-fm

Investment fund domain baseline for `fund.etzhayyim.com`.

- Public domains covered by this seed:
  - `sovereign_fund`
  - `mutual_fund`
  - `pension_fund`
  - `private_fund`
  - `government_fund`
  - `investor_fund`
- Seeded collections:
  - `com.etzhayyim.apps.fund.fund`
  - `com.etzhayyim.apps.fund.manager`
  - `com.etzhayyim.apps.fund.investor`
  - `com.etzhayyim.apps.fund.investee`
  - `com.etzhayyim.apps.fund.metric`
  - `com.etzhayyim.apps.fund.commitment`

## Why this exists

`public_fund` is a separate crowdfunding / budget-disbursement domain on
`public-fund.etzhayyim.com`. Investment-fund coverage should be bootstrapped from
`fund.etzhayyim.com` records, not only from `public_fund`.

## Seed

Authoritative path:

```bash
etzhayyim seed --app fund
```

Direct PDS seed script:

```bash
export etzhayyim_TOKEN="$(etzhayyim authn token)"
npx tsx 60-apps/etzhayyim-project-fm/seed.ts
```

After seeding, re-check:

```bash
etzhayyim coverage world --domain sovereign_fund,mutual_fund,pension_fund,private_fund,government_fund,investor_fund
```
