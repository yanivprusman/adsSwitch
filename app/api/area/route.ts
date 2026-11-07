import { NextResponse } from "next/server";
import { daemonJson } from "@/lib/daemon";
import { authorize } from "@/lib/auth";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

const MODES = new Set(["local", "nationwide", "south", "center", "north", "jerusalem", "off"]);
const SITES = new Set(["pigeons", "etc", "panelshed"]);

async function relay(command: Record<string, unknown>) {
  try {
    const result = await daemonJson(command);
    return NextResponse.json(result, { status: result.ok === true ? 200 : 502 });
  } catch (e) {
    return NextResponse.json(
      { ok: false, error: e instanceof Error ? e.message : String(e) },
      { status: 502 },
    );
  }
}

/** Which set of ads is running, with today's spend. */
export async function GET(request: Request) {
  const denied = authorize(request);
  if (denied)
    return NextResponse.json({ ok: false, error: denied }, { status: 401 });
  return relay({ command: "adsArea" });
}

/** Switch one site: { "site": "pigeons" | "etc" | "panelshed", "mode": "local" | "nationwide" | "south" | "center" | "north" | "jerusalem" | "off" }. The reply is the state
 *  re-read from Google afterwards, not an echo of the request. */
export async function POST(request: Request) {
  const denied = authorize(request);
  if (denied)
    return NextResponse.json({ ok: false, error: denied }, { status: 401 });

  let body: Record<string, unknown>;
  try {
    body = await request.json();
  } catch {
    return NextResponse.json({ ok: false, error: "invalid_json" }, { status: 400 });
  }
  const mode = String(body.mode ?? "");
  if (!MODES.has(mode))
    return NextResponse.json(
      { ok: false, error: `mode must be one of: ${[...MODES].join(", ")}` },
      { status: 400 },
    );
  const site = String(body.site ?? "");
  if (!SITES.has(site))
    return NextResponse.json(
      { ok: false, error: `site must be one of: ${[...SITES].join(", ")}` },
      { status: 400 },
    );
  return relay({ command: "adsArea", site, mode });
}
