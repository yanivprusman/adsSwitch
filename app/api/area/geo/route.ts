import { NextResponse } from "next/server";
import { daemonJson } from "@/lib/daemon";
import { authorize } from "@/lib/auth";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

/** Where the ad money went, by the searcher's city: `?days=7|30|90`. */
export async function GET(request: Request) {
  const denied = authorize(request);
  if (denied)
    return NextResponse.json({ ok: false, error: denied }, { status: 401 });

  const raw = new URL(request.url).searchParams.get("days") ?? "30";
  const days = Number(raw);
  if (!Number.isInteger(days) || days < 1 || days > 90)
    return NextResponse.json(
      { ok: false, error: "days must be a whole number from 1 to 90" },
      { status: 400 },
    );

  try {
    const result = await daemonJson({ command: "adsArea", geo: 1, days });
    return NextResponse.json(result, { status: result.ok === true ? 200 : 502 });
  } catch (e) {
    return NextResponse.json(
      { ok: false, error: e instanceof Error ? e.message : String(e) },
      { status: 502 },
    );
  }
}
