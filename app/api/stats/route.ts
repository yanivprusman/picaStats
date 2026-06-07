import { NextRequest, NextResponse } from "next/server";
import net from "net";

// picaStats backend — token-gated, reshaped view over the visitStats data.
//
// Core stats come from the daemon socket (`visitStats` command) — the same
// authoritative path visitStats itself uses. The daemon is always up and reads
// the shared MySQL `site_visits` table, so this works uniformly whether the
// backend runs on the NUC (prod) or the desktop (dev, via mysql-router).
//
// Country breakdown is an optional enrichment fetched from the visitStats
// visitors endpoint (only reachable when that server is up, e.g. on the NUC);
// if unavailable it is simply omitted — it never blocks the core numbers.

export const dynamic = "force-dynamic";

const SOCKET_PATH =
  process.env.AUTOMATE_LINUX_SOCKET_PATH ||
  "/run/automatelinux/automatelinux-daemon.sock";
const VISITORS_URL = process.env.VISITSTATS_VISITORS_URL || "http://127.0.0.1:3050";
const PICAWISH_URL = process.env.PICAWISH_URL || "http://127.0.0.1:3026";
const DEFAULT_SITE = process.env.STATS_DEFAULT_SITE || "picawish.prod.ya-niv.com";

type ByDay = { date: string; visits: number; unique: number };
type Referrer = { referer: string; visits: number };
type Country = { name: string; code: string; visits: number; visitors: number };
type Site = { site: string; visits: number; unique: number };

function tokenOf(req: NextRequest): string | null {
  const auth = req.headers.get("authorization");
  if (auth?.startsWith("Bearer ")) return auth.slice(7).trim();
  return req.nextUrl.searchParams.get("token");
}

function sendToDaemon(payload: object): Promise<string> {
  return new Promise((resolve, reject) => {
    const sock = net.createConnection(SOCKET_PATH);
    let data = "";
    let settled = false;
    const done = (val: string) => {
      if (settled) return;
      settled = true;
      sock.destroy();
      resolve(val);
    };
    sock.on("connect", () => sock.write(JSON.stringify(payload) + "\n"));
    sock.on("data", (chunk) => {
      data += chunk.toString();
      // The daemon returns pretty-printed JSON that may arrive across several
      // chunks. Resolve as soon as we have a complete, parseable object.
      try {
        JSON.parse(data.trim());
        done(data.trim());
      } catch {
        /* keep buffering */
      }
    });
    sock.on("end", () => done(data.trim()));
    sock.on("error", (err) => {
      if (!settled) {
        settled = true;
        reject(err);
      }
    });
    setTimeout(() => {
      if (!settled) {
        settled = true;
        sock.destroy();
        reject(new Error("daemon timeout"));
      }
    }, 10000);
  });
}

async function getJson(url: string, timeoutMs = 5000): Promise<Record<string, unknown>> {
  const ctrl = new AbortController();
  const t = setTimeout(() => ctrl.abort(), timeoutMs);
  try {
    const res = await fetch(url, { cache: "no-store", signal: ctrl.signal });
    if (!res.ok) throw new Error(`upstream ${res.status} for ${url}`);
    return (await res.json()) as Record<string, unknown>;
  } finally {
    clearTimeout(t);
  }
}

function sumSince(byDay: ByDay[], cutoff: string): number {
  return byDay.reduce((acc, d) => (d.date >= cutoff ? acc + (d.visits || 0) : acc), 0);
}

function isoDaysAgo(n: number): string {
  const d = new Date();
  d.setUTCDate(d.getUTCDate() - n);
  return d.toISOString().slice(0, 10);
}

export async function GET(req: NextRequest) {
  const expected = process.env.STATS_TOKEN;
  if (!expected) {
    return NextResponse.json(
      { error: "STATS_TOKEN not configured on server" },
      { status: 500 },
    );
  }
  if (tokenOf(req) !== expected) {
    return NextResponse.json({ error: "unauthorized" }, { status: 401 });
  }

  const site = req.nextUrl.searchParams.get("site") || DEFAULT_SITE;
  const app = req.nextUrl.searchParams.get("app") || "picawish";

  // Core stats — authoritative, via the daemon socket. period=all gives the full
  // by_day history, from which we derive today / 7d / 30d windows.
  let statsAll: Record<string, unknown>;
  let allSites: Record<string, unknown>;
  try {
    const [rawSite, rawAll] = await Promise.all([
      sendToDaemon({ command: "visitStats", site, period: "all", top: "10" }),
      sendToDaemon({ command: "visitStats", period: "7d", top: "25" }),
    ]);
    statsAll = JSON.parse(rawSite);
    allSites = JSON.parse(rawAll);
  } catch (e) {
    return NextResponse.json(
      { error: "stats source unavailable", detail: String(e) },
      { status: 502 },
    );
  }

  const byDayAll = (statsAll.by_day as ByDay[]) || [];
  const summary = (statsAll.summary as Record<string, number>) || {};

  const today = isoDaysAgo(0);
  const yesterday = isoDaysAgo(1);
  const cut7 = isoDaysAgo(6);
  const cut30 = isoDaysAgo(29);

  const todayCount = byDayAll.find((d) => d.date === today)?.visits ?? 0;
  const yesterdayCount = byDayAll.find((d) => d.date === yesterday)?.visits ?? 0;

  // Optional enrichments — never block the core response.
  let wishes: number | null = null;
  let countries: Country[] = [];
  await Promise.all([
    (async () => {
      if (app !== "picawish") return;
      try {
        const w = await getJson(`${PICAWISH_URL}/api/stats/wishes`);
        wishes = typeof w.count === "number" ? w.count : null;
      } catch {
        wishes = null;
      }
    })(),
    (async () => {
      try {
        const v = await getJson(
          `${VISITORS_URL}/api/visitors?site=${encodeURIComponent(site)}&period=30d`,
        );
        countries = ((v.countries as Country[]) || []).slice(0, 10);
      } catch {
        countries = [];
      }
    })(),
  ]);

  return NextResponse.json({
    app,
    site,
    generatedAt: new Date().toISOString(),
    summary: {
      today: todayCount,
      yesterday: yesterdayCount,
      last7d: sumSince(byDayAll, cut7),
      last30d: sumSince(byDayAll, cut30),
      allTime: summary.total_visits ?? 0,
      uniqueAllTime: summary.unique_visitors ?? 0,
      humans: summary.human_visits ?? 0,
      bots: summary.bot_visits ?? 0,
    },
    byDay: byDayAll.slice(-30),
    topReferrers: ((statsAll.top_referrers as Referrer[]) || []).slice(0, 10),
    countries,
    wishes,
    allSites: ((allSites.by_site as Site[]) || []).slice(0, 25),
  });
}
