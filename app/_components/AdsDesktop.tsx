"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import SiteColumn from "./SiteColumn";
import { shekels, VERSIONS, type AreaState, type GeoReport, type Mode } from "@/lib/ads-model";

const PERIODS = [{ days: 7, label: "שבוע" }, { days: 30, label: "חודש" }, { days: 90, label: "3 חודשים" }];
const UNDO_SECONDS = 8;

async function getJson<T extends { ok: boolean; error?: string }>(url: string, init?: RequestInit): Promise<T> {
  const r = await fetch(url, init);
  if (r.status === 401) {
    window.location.replace("/");
    throw new Error("צריך להיכנס מחדש");
  }
  const body = (await r.json()) as T;
  if (!body.ok) throw new Error(body.error ?? "שגיאה לא ידועה");
  return body;
}

/**
 * The desktop version of the phone app: every site side by side, each with its own switch.
 *
 * Nothing is decided here. What shows as running is what Google reported back,
 * after a switch too, never what was clicked. A switch is one click and can be
 * undone for a few seconds, the same as on the phone.
 */
export default function AdsDesktop() {
  const [state, setState] = useState<AreaState | null>(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [pending, setPending] = useState<{ site: string; mode: Mode } | null>(null);
  const [undo, setUndo] = useState<{ site: string; backTo: Mode; key: number } | null>(null);
  const [days, setDays] = useState(30);
  const [geo, setGeo] = useState<Record<string, GeoReport>>({});
  const [geoError, setGeoError] = useState("");
  const undoTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  const refresh = useCallback(async () => {
    setLoading(true);
    try {
      setState(await getJson<AreaState>("/api/area"));
      setError("");
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- the first fetch of what is running
    void refresh();
    const onVisible = () => { if (document.visibilityState === "visible") void refresh(); };
    document.addEventListener("visibilitychange", onVisible);
    return () => document.removeEventListener("visibilitychange", onVisible);
  }, [refresh]);

  const siteIds = state?.sites.map((s) => s.id).join(",") ?? "";
  useEffect(() => {
    if (!siteIds) return;
    let current = true;
    for (const site of siteIds.split(",")) {
      getJson<GeoReport>(`/api/area/geo?days=${days}&site=${site}`)
        .then((g) => { if (current) setGeo((all) => ({ ...all, [site]: g })); })
        .catch((e) => { if (current) setGeoError(e instanceof Error ? e.message : String(e)); });
    }
    return () => { current = false; };
  }, [days, siteIds]);

  async function switchTo(site: string, mode: Mode, isUndo = false) {
    if (pending) return;
    if (undoTimer.current) clearTimeout(undoTimer.current);
    setUndo(null);
    setPending({ site, mode });
    try {
      const next = await getJson<AreaState>("/api/area", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ site, mode }),
      });
      setState(next);
      setError("");
      if (!isUndo && next.changedFrom && next.changedFrom !== "mixed") {
        setUndo({ site, backTo: next.changedFrom, key: Date.now() });
        undoTimer.current = setTimeout(() => setUndo(null), UNDO_SECONDS * 1000);
      }
    } catch (e) {
      setError(`לא הוחלף: ${e instanceof Error ? e.message : String(e)}`);
    } finally {
      setPending(null);
    }
  }

  const running = state?.sites.flatMap((s) => VERSIONS.map((v) => s.sets[v])) ?? [];
  const todayCost = running.reduce((sum, set) => sum + set.todayCostIls, 0);
  const todayClicks = running.reduce((sum, set) => sum + set.todayClicks, 0);

  return (
    <main dir="rtl" lang="he" className="min-h-dvh bg-[#F3F4F1] px-6 py-5 text-[#14181B]" data-id="ads-desktop">
      <div className="mx-auto max-w-[1500px]">
        <header className="flex flex-wrap items-center gap-x-6 gap-y-3">
          <div>
            <h1 className="text-2xl font-black">מודעות</h1>
            <p className="text-[13px] text-[#979DA3]">Google Ads · לכל אתר בוחרים איזו גרסה רצה</p>
          </div>
          {state && (
            <p className="text-sm text-[#5B6268]" data-id="today-total">
              היום בכל האתרים: <b className="text-[#14181B]">{shekels(todayCost)}</b> · {todayClicks === 0 ? "אין קליקים" : todayClicks === 1 ? "קליק אחד" : `${todayClicks} קליקים`}
            </p>
          )}
          <div className="ms-auto flex items-center gap-2">
            {state && <span className="text-xs text-[#979DA3]">עודכן {state.updatedAt.slice(11, 16)}</span>}
            <div className="flex rounded-lg bg-white p-1 ring-1 ring-[#E3E5E1]" role="group" aria-label="תקופה לנתוני הקליקים">
              {PERIODS.map((p) => (
                <button key={p.days} type="button" data-id={`period-${p.days}`} aria-pressed={p.days === days} onClick={() => { setDays(p.days); setGeoError(""); }}
                  className={`cursor-pointer rounded-md px-3 py-1.5 text-[13px] font-semibold transition-colors active:scale-95 ${p.days === days ? "bg-[#14181B] text-white" : "text-[#5B6268] hover:bg-[#ECEEF0]"}`}>
                  {p.label}
                </button>
              ))}
            </div>
            <button type="button" data-id="refresh" onClick={() => void refresh()} disabled={loading}
              className="cursor-pointer rounded-lg bg-white px-3 py-2 text-[13px] font-semibold text-[#5B6268] ring-1 ring-[#E3E5E1] transition-colors hover:bg-[#ECEEF0] active:scale-95 disabled:cursor-not-allowed disabled:opacity-50">
              {loading ? "מרענן…" : "רענון"}
            </button>
          </div>
        </header>

        {error && (
          <div role="alert" className="mt-4 flex items-center gap-3 rounded-xl bg-[#FDECEA] px-4 py-3 text-sm text-[#B42318]" data-id="error">
            <span className="flex-1">{error}</span>
            <button type="button" data-id="retry" onClick={() => void refresh()} className="cursor-pointer rounded-md px-2 py-1 font-semibold transition-colors hover:bg-white/60">נסה שוב</button>
          </div>
        )}

        {!state && !error && (
          <div className="mt-5 grid gap-5 xl:grid-cols-3" aria-busy="true">
            {[0, 1, 2].map((i) => <div key={i} className="h-[560px] animate-pulse rounded-3xl bg-[#E3E5E1]" />)}
          </div>
        )}

        {state && (
          <div className="mt-5 grid items-start gap-5 xl:grid-cols-3">
            {state.sites.map((site) => (
              <SiteColumn key={site.id} site={site} centers={state.centers} radiusKm={state.radiusKm}
                pending={pending} geo={geo[site.id]} geoError={geoError} days={days}
                undo={undo?.site === site.id ? undo : null} undoSeconds={UNDO_SECONDS} onSwitch={switchTo} />
            ))}
          </div>
        )}
      </div>
    </main>
  );
}
