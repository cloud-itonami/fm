/**
 * fund-management seed — writes baseline investment fund records into the PDS
 * so fund-related world coverage domains have actual collection data.
 *
 * Usage: npx tsx 60-apps/etzhayyim-project-fm/seed.ts
 */

declare const process: {
  env: Record<string, string | undefined>;
  exitCode?: number;
};

const PDS = 'https://atproto.etzhayyim.com';
const ROOT_DID = 'did:web:fund.etzhayyim.com';
const PROJECT_ID = 'fm';

const etzhayyim_TOKEN = process.env.etzhayyim_TOKEN;
if (!etzhayyim_TOKEN) {
  throw new Error('etzhayyim_TOKEN env var required — run `export etzhayyim_TOKEN=$(etzhayyim auth token)` first');
}

const INTERNAL_HEADERS = {
  'Content-Type': 'application/json',
  'Authorization': `Bearer ${etzhayyim_TOKEN}`,
  'x-etzhayyim-org-id': 'anon',
};

function stableRkey(input: string): string {
  const normalized = input.trim().toLowerCase();
  if (!normalized) return 'seed';
  const compact = normalized
    .replace(/[^a-z0-9._~-]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .slice(0, 512);
  return compact || 'seed';
}

async function putRecord(collection: string, id: string, record: Record<string, unknown>): Promise<void> {
  const res = await fetch(`${PDS}/xrpc/com.atproto.repo.putRecord`, {
    method: 'POST',
    headers: INTERNAL_HEADERS,
    body: JSON.stringify({
      repo: ROOT_DID,
      collection,
      rkey: stableRkey(`${collection}:${id}`),
      record,
    }),
  });
  if (!res.ok) {
    throw new Error(`putRecord ${collection}/${id}: ${res.status} ${await res.text()}`);
  }
}

async function actorCreate(did: string, displayName: string, description: string): Promise<void> {
  const res = await fetch(`${PDS}/xrpc/com.etzhayyim.actor.create`, {
    method: 'POST',
    headers: INTERNAL_HEADERS,
    body: JSON.stringify({ did, projectId: PROJECT_ID, displayName, description, hasWorker: false }),
  });
  if (!res.ok) {
    console.warn(`actor.create ${did}: ${res.status} ${await res.text()}`);
  }
}

function isoNow(): string {
  return new Date().toISOString();
}

type FundSeed = {
  fundId: string;
  name: string;
  fundKind: string;
  strategy: string;
  jurisdiction: string;
  domicile: string;
  vintageYear: number;
  managerName: string;
  sponsorName: string;
  currency: string;
  aumAmount?: number;
  committedCapital?: number;
};

type ManagerSeed = {
  managerId: string;
  managerName: string;
  managerType: string;
  jurisdiction: string;
  domicile: string;
  regulator: string;
  currency: string;
  aumAmount: number;
};

type InvestorSeed = {
  investorId: string;
  investorName: string;
  investorType: string;
  jurisdiction: string;
  domicile: string;
  commitmentAmount: number;
  currency: string;
};

type InvesteeSeed = {
  investeeId: string;
  investeeName: string;
  investeeType: string;
  jurisdiction: string;
  sector: string;
  valuationAmount: number;
  investedAmount: number;
  ownershipPct: number;
  currency: string;
};

type MetricSeed = {
  metricId: string;
  fundId: string;
  metricType: string;
  metricValue: number;
  metricUnit: string;
  asOfDate: string;
};

type CommitmentSeed = {
  commitmentId: string;
  fundId: string;
  investorId: string;
  commitmentAmount: number;
  calledAmount: number;
  currency: string;
};

const FUNDS: FundSeed[] = [
  {
    fundId: 'swf-norway-gpfg',
    name: 'Government Pension Fund Global',
    fundKind: 'sovereign_fund',
    strategy: 'global-diversified',
    jurisdiction: 'nor',
    domicile: 'nor',
    vintageYear: 1990,
    managerName: 'Norges Bank Investment Management',
    sponsorName: 'Kingdom of Norway',
    currency: 'NOK',
    aumAmount: 1700000000000,
  },
  {
    fundId: 'mutual-vanguard-total-world',
    name: 'Vanguard Total World Stock Fund',
    fundKind: 'mutual_fund',
    strategy: 'public-equity-index',
    jurisdiction: 'usa',
    domicile: 'usa',
    vintageYear: 2008,
    managerName: 'The Vanguard Group',
    sponsorName: 'Vanguard',
    currency: 'USD',
    aumAmount: 41000000000,
  },
  {
    fundId: 'pension-cpp-investments',
    name: 'CPP Investments Fund',
    fundKind: 'pension_fund',
    strategy: 'multi-asset-pension',
    jurisdiction: 'can',
    domicile: 'can',
    vintageYear: 1997,
    managerName: 'CPP Investments',
    sponsorName: 'Canada Pension Plan',
    currency: 'CAD',
    aumAmount: 632000000000,
  },
  {
    fundId: 'private-softbank-vision',
    name: 'SoftBank Vision Fund',
    fundKind: 'private_fund',
    strategy: 'late-stage-technology',
    jurisdiction: 'jpn',
    domicile: 'jpn',
    vintageYear: 2017,
    managerName: 'SoftBank Investment Advisers',
    sponsorName: 'SoftBank Group',
    currency: 'USD',
    committedCapital: 100000000000,
  },
];

const MANAGERS: ManagerSeed[] = [
  {
    managerId: 'mgr-nbim',
    managerName: 'Norges Bank Investment Management',
    managerType: 'sovereign-asset-manager',
    jurisdiction: 'nor',
    domicile: 'nor',
    regulator: 'Norwegian Ministry of Finance',
    currency: 'NOK',
    aumAmount: 1700000000000,
  },
  {
    managerId: 'mgr-vanguard',
    managerName: 'The Vanguard Group',
    managerType: 'mutual-fund-manager',
    jurisdiction: 'usa',
    domicile: 'usa',
    regulator: 'SEC',
    currency: 'USD',
    aumAmount: 10000000000000,
  },
  {
    managerId: 'mgr-cpp',
    managerName: 'CPP Investments',
    managerType: 'pension-fund-manager',
    jurisdiction: 'can',
    domicile: 'can',
    regulator: 'Office of the Superintendent of Financial Institutions',
    currency: 'CAD',
    aumAmount: 632000000000,
  },
  {
    managerId: 'mgr-sbia',
    managerName: 'SoftBank Investment Advisers',
    managerType: 'private-fund-manager',
    jurisdiction: 'gbr',
    domicile: 'gbr',
    regulator: 'FCA',
    currency: 'USD',
    aumAmount: 140000000000,
  },
];

const INVESTORS: InvestorSeed[] = [
  {
    investorId: 'lp-abu-dhabi',
    investorName: 'Mubadala Investment Company',
    investorType: 'sovereign-lp',
    jurisdiction: 'are',
    domicile: 'are',
    commitmentAmount: 15000000000,
    currency: 'USD',
  },
  {
    investorId: 'lp-calpers',
    investorName: 'CalPERS',
    investorType: 'pension-lp',
    jurisdiction: 'usa',
    domicile: 'usa',
    commitmentAmount: 5000000000,
    currency: 'USD',
  },
  {
    investorId: 'lp-university-endowment',
    investorName: 'Northbridge University Endowment',
    investorType: 'endowment-lp',
    jurisdiction: 'usa',
    domicile: 'usa',
    commitmentAmount: 750000000,
    currency: 'USD',
  },
];

const INVESTEES: InvesteeSeed[] = [
  {
    investeeId: 'inv-voltgrid',
    investeeName: 'VoltGrid Storage',
    investeeType: 'climate-infrastructure',
    jurisdiction: 'deu',
    sector: 'energy-storage',
    valuationAmount: 2300000000,
    investedAmount: 180000000,
    ownershipPct: 11.5,
    currency: 'EUR',
  },
  {
    investeeId: 'inv-rural-health-net',
    investeeName: 'Rural Health Net',
    investeeType: 'health-services',
    jurisdiction: 'usa',
    sector: 'primary-care',
    valuationAmount: 950000000,
    investedAmount: 90000000,
    ownershipPct: 9.2,
    currency: 'USD',
  },
  {
    investeeId: 'inv-orbit-fab',
    investeeName: 'OrbitFab Systems',
    investeeType: 'industrial-tech',
    jurisdiction: 'jpn',
    sector: 'advanced-manufacturing',
    valuationAmount: 3400000000,
    investedAmount: 250000000,
    ownershipPct: 14.1,
    currency: 'USD',
  },
];

const METRICS: MetricSeed[] = [
  { metricId: 'metric-gpfg-2026q1', fundId: 'swf-norway-gpfg', metricType: 'aum', metricValue: 1700000000000, metricUnit: 'NOK', asOfDate: '2026-03-31' },
  { metricId: 'metric-vanguard-2026q1', fundId: 'mutual-vanguard-total-world', metricType: 'aum', metricValue: 41000000000, metricUnit: 'USD', asOfDate: '2026-03-31' },
  { metricId: 'metric-cpp-2026q1', fundId: 'pension-cpp-investments', metricType: 'aum', metricValue: 632000000000, metricUnit: 'CAD', asOfDate: '2026-03-31' },
  { metricId: 'metric-vision-2026q1', fundId: 'private-softbank-vision', metricType: 'committed_capital', metricValue: 100000000000, metricUnit: 'USD', asOfDate: '2026-03-31' },
];

const COMMITMENTS: CommitmentSeed[] = [
  { commitmentId: 'commit-001', fundId: 'private-softbank-vision', investorId: 'lp-abu-dhabi', commitmentAmount: 15000000000, calledAmount: 10500000000, currency: 'USD' },
  { commitmentId: 'commit-002', fundId: 'private-softbank-vision', investorId: 'lp-calpers', commitmentAmount: 5000000000, calledAmount: 3200000000, currency: 'USD' },
  { commitmentId: 'commit-003', fundId: 'private-softbank-vision', investorId: 'lp-university-endowment', commitmentAmount: 750000000, calledAmount: 420000000, currency: 'USD' },
];

const SYNTHETIC_KINDS = [
  { slug: 'sovereign', title: 'Strategic Reserve Fund', fundKind: 'sovereign_fund', strategy: 'global-diversified', managerType: 'sovereign-asset-manager', regulator: 'National Treasury', sponsorPrefix: 'State Holding Authority', currency: 'USD', metricType: 'aum', baseAum: 18000000000, baseCommitment: 0 },
  { slug: 'mutual', title: 'Global Allocation Fund', fundKind: 'mutual_fund', strategy: 'public-equity-index', managerType: 'mutual-fund-manager', regulator: 'Securities Regulator', sponsorPrefix: 'Retail Asset Platform', currency: 'USD', metricType: 'aum', baseAum: 4500000000, baseCommitment: 0 },
  { slug: 'pension', title: 'Retirement Income Fund', fundKind: 'pension_fund', strategy: 'multi-asset-pension', managerType: 'pension-fund-manager', regulator: 'Pension Supervisor', sponsorPrefix: 'National Pension Board', currency: 'USD', metricType: 'aum', baseAum: 12000000000, baseCommitment: 0 },
  { slug: 'private', title: 'Growth Equity Fund', fundKind: 'private_fund', strategy: 'late-stage-technology', managerType: 'private-fund-manager', regulator: 'Financial Conduct Authority', sponsorPrefix: 'General Partner Group', currency: 'USD', metricType: 'committed_capital', baseAum: 0, baseCommitment: 3200000000 },
  { slug: 'government', title: 'Development Finance Fund', fundKind: 'government_fund', strategy: 'development-finance', managerType: 'development-finance-manager', regulator: 'Ministry of Finance', sponsorPrefix: 'Public Investment Agency', currency: 'USD', metricType: 'aum', baseAum: 6200000000, baseCommitment: 0 },
  { slug: 'investor', title: 'Allocator Partnership Fund', fundKind: 'investor_fund', strategy: 'limited-partner-allocation', managerType: 'allocator-platform-manager', regulator: 'Financial Services Authority', sponsorPrefix: 'Institutional Allocator Network', currency: 'USD', metricType: 'committed_capital', baseAum: 0, baseCommitment: 2400000000 },
] as const;

const JURISDICTIONS = ['usa', 'jpn', 'can', 'gbr', 'deu', 'fra', 'sgp', 'are', 'aus', 'nld', 'swe', 'ind'] as const;
const INVESTEE_KINDS = [
  { kind: 'energy-transition', sector: 'grid-modernization', currency: 'USD', baseValuation: 1800000000, baseInvested: 140000000 },
  { kind: 'digital-infrastructure', sector: 'cloud-network', currency: 'USD', baseValuation: 2400000000, baseInvested: 175000000 },
  { kind: 'health-services', sector: 'primary-care', currency: 'USD', baseValuation: 1200000000, baseInvested: 95000000 },
  { kind: 'industrial-tech', sector: 'advanced-manufacturing', currency: 'USD', baseValuation: 2900000000, baseInvested: 210000000 },
] as const;

for (let i = 0; i < 24; i += 1) {
  const spec = SYNTHETIC_KINDS[i % SYNTHETIC_KINDS.length];
  const jurisdiction = JURISDICTIONS[i % JURISDICTIONS.length];
  const investeeSpec = INVESTEE_KINDS[i % INVESTEE_KINDS.length];
  const series = i + 1;
  const fundId = `${spec.slug}-synthetic-${String(series).padStart(2, '0')}`;
  const managerId = `mgr-${spec.slug}-${String(series).padStart(2, '0')}`;
  const investorId = `lp-${spec.slug}-${String(series).padStart(2, '0')}`;
  const assetValue = Math.max(
    spec.baseAum > 0 ? spec.baseAum + series * 450000000 : 0,
    spec.baseCommitment > 0 ? spec.baseCommitment + series * 210000000 : 0,
  );

  FUNDS.push({
    fundId,
    name: `${spec.title} ${String(series).padStart(2, '0')}`,
    fundKind: spec.fundKind,
    strategy: spec.strategy,
    jurisdiction,
    domicile: jurisdiction,
    vintageYear: 2002 + (series % 20),
    managerName: `${spec.title} Capital ${String(series).padStart(2, '0')}`,
    sponsorName: `${spec.sponsorPrefix} ${String(series).padStart(2, '0')}`,
    currency: spec.currency,
    ...(spec.baseAum > 0 ? { aumAmount: spec.baseAum + series * 450000000 } : {}),
    ...(spec.baseCommitment > 0 ? { committedCapital: spec.baseCommitment + series * 210000000 } : {}),
  });

  MANAGERS.push({
    managerId,
    managerName: `${spec.title} Capital ${String(series).padStart(2, '0')}`,
    managerType: spec.managerType,
    jurisdiction,
    domicile: jurisdiction,
    regulator: spec.regulator,
    currency: spec.currency,
    aumAmount: assetValue,
  });

  INVESTORS.push({
    investorId,
    investorName: `${spec.title} Allocator ${String(series).padStart(2, '0')}`,
    investorType: `${spec.slug}-lp`,
    jurisdiction,
    domicile: jurisdiction,
    commitmentAmount: 650000000 + series * 55000000,
    currency: spec.currency,
  });

  INVESTEES.push({
    investeeId: `inv-${spec.slug}-${String(series).padStart(2, '0')}`,
    investeeName: `${spec.title} Portfolio Company ${String(series).padStart(2, '0')}`,
    investeeType: investeeSpec.kind,
    jurisdiction,
    sector: investeeSpec.sector,
    valuationAmount: investeeSpec.baseValuation + series * 120000000,
    investedAmount: investeeSpec.baseInvested + series * 12000000,
    ownershipPct: 8 + ((series % 7) + 1) * 0.7,
    currency: investeeSpec.currency,
  });

  METRICS.push({
    metricId: `metric-${fundId}-2026q1`,
    fundId,
    metricType: spec.metricType,
    metricValue: assetValue,
    metricUnit: spec.currency,
    asOfDate: '2026-03-31',
  });

  COMMITMENTS.push({
    commitmentId: `commit-${spec.slug}-${String(series).padStart(2, '0')}`,
    fundId,
    investorId,
    commitmentAmount: 650000000 + series * 55000000,
    calledAmount: Math.floor((650000000 + series * 55000000) * 0.7),
    currency: spec.currency,
  });
}

async function main(): Promise<void> {
  console.log('=== Fund Management Seed ===');
  await actorCreate(ROOT_DID, 'Fund Management', 'Baseline investment fund registry for world coverage and fund graph bootstrapping');

  for (const fund of FUNDS) {
    await putRecord('com.etzhayyim.apps.fund.fund', fund.fundId, {
      ...fund,
      ownerDid: ROOT_DID,
      status: 'active',
      sourceLicense: 'public-web',
      createdAt: isoNow(),
    });
  }

  for (const manager of MANAGERS) {
    await putRecord('com.etzhayyim.apps.fund.manager', manager.managerId, {
      ...manager,
      ownerDid: ROOT_DID,
      sourceLicense: 'public-web',
      createdAt: isoNow(),
    });
  }

  for (const investor of INVESTORS) {
    await putRecord('com.etzhayyim.apps.fund.investor', investor.investorId, {
      ...investor,
      ownerDid: ROOT_DID,
      sourceLicense: 'public-web',
      createdAt: isoNow(),
    });
  }

  for (const investee of INVESTEES) {
    await putRecord('com.etzhayyim.apps.fund.investee', investee.investeeId, {
      ...investee,
      ownerDid: ROOT_DID,
      sourceLicense: 'public-web',
      createdAt: isoNow(),
    });
  }

  for (const metric of METRICS) {
    await putRecord('com.etzhayyim.apps.fund.metric', metric.metricId, {
      ...metric,
      ownerDid: ROOT_DID,
      createdAt: isoNow(),
    });
  }

  for (const commitment of COMMITMENTS) {
    await putRecord('com.etzhayyim.apps.fund.commitment', commitment.commitmentId, {
      ...commitment,
      ownerDid: ROOT_DID,
      distributedAmount: 0,
      ownershipPct: null,
      createdAt: isoNow(),
    });
  }

  console.log(
    `Seeded ${FUNDS.length + MANAGERS.length + INVESTORS.length + INVESTEES.length + METRICS.length + COMMITMENTS.length} fund records to ${ROOT_DID}`,
  );
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
