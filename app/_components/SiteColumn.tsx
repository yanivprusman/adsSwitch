import AreaMap from "./AreaMap";
import { modeColor, modeDetail, modeGlow, modeShort, modeTitle, shekels, SITE_DETAIL, SWITCH, VERSIONS, type Center, type GeoReport, type Mode, type Site, type Version } from "@/lib/ads-model";

const isVersion = (m: Mode): m is Version => (VERSIONS as string[]).includes(m);

/** One site: what runs, what it cost today, the switch, and where the clicks came from. */
export default function SiteColumn({ site, centers, radiusKm, pending, geo, geoError, days, undo, undoSeconds, onSwitch }: {
  site: Site; centers: Center[]; radiusKm: number;
  pending: { site: string; mode: Mode } | null;
  geo?: GeoReport; geoError: string; days: number;
  undo: { backTo: Mode; key: number } | null; undoSeconds: number;
  onSwitch: (site: string, mode: Mode, isUndo?: boolean) => void;
}) {
  const mine = pending?.site === site.id ? pending : null;
  const running = isVersion(site.mode) ? site.sets[site.mode] : null;
  const copies = VERSIONS.map((v) => site.sets[v]);
  const todayCost = copies.reduce((sum, s) => sum + s.todayCostIls, 0);
  const todayClicks = copies.reduce((sum, s) => sum + s.todayClicks, 0);
  const budget = running?.dailyBudgetIls ?? 0;
  const glow = modeGlow(site.mode);

  return (
    <section className="flex flex-col gap-3" data-id={`site-${site.id}`} aria-labelledby={`site-${site.id}-title`}>
      <div className="px-1">
        <h2 id={`site-${site.id}-title`} className="text-xl font-extrabold">{site.label}</h2>
        <p className="text-[13px] text-[#5B6268]">{SITE_DETAIL[site.id]}</p>
      </div>

      <div className="flex gap-3 rounded-3xl bg-[#0E1714] p-5 text-white">
        <div className="min-w-0 flex-1">
          <p className="text-[13px] font-medium text-white/60">{mine ? `מעביר ל${modeShort(mine.mode)}…` : "רצות עכשיו"}</p>
          <p className="mt-0.5 text-3xl font-black" style={{ color: glow }} data-id={`mode-${site.id}`}>{modeTitle(site.mode)}</p>
          <p className="mt-1 text-sm leading-5 text-white/75">{modeDetail(site.mode, radiusKm, centers)}</p>
          <p className="mt-5 text-[13px] font-medium text-white/60">היום</p>
          <p className="text-3xl font-black">{shekels(todayCost)}{running && <span className="ms-2 text-sm font-normal text-white/55">מתוך {shekels(budget)}</span>}</p>
          {running && budget > 0 && (
            <div className="mt-2 h-1.5 overflow-hidden rounded-full bg-white/12">
              <div className="h-full rounded-full transition-[width] duration-700" style={{ width: `${Math.min(100, (todayCost / budget) * 100)}%`, background: glow }} />
            </div>
          )}
          <p className="mt-2 text-sm text-white/85">
            {todayClicks === 0 ? "עוד אין קליקים היום" : todayClicks === 1 ? `קליק אחד · ${shekels(todayCost, true)} לקליק` : `${todayClicks} קליקים · ${shekels(todayCost / todayClicks, true)} לקליק`}
          </p>
        </div>
        <AreaMap siteId={site.id} mode={site.mode} pending={!!mine} centers={centers} radiusKm={radiusKm} cities={geo?.cities ?? []} />
      </div>

      <div className="grid grid-cols-3 gap-1 rounded-2xl bg-white p-1.5 ring-1 ring-[#E3E5E1]" role="group" aria-label={`איזו גרסה רצה ב-${site.label}`}>
        {SWITCH.map((seg) => {
          const selected = seg.mode === site.mode;
          const busy = mine?.mode === seg.mode;
          return (
            <button key={seg.mode} type="button" data-id={`switch-${site.id}-${seg.mode}`} aria-pressed={selected} disabled={selected || !!pending}
              onClick={() => onSwitch(site.id, seg.mode)}
              style={selected ? { background: modeColor(seg.mode) } : undefined}
              className={`h-12 rounded-xl text-[15px] font-semibold transition-colors ${selected ? "cursor-default font-bold text-white" : pending ? "cursor-not-allowed text-[#979DA3]" : "cursor-pointer text-[#5B6268] hover:bg-[#ECEEF0] hover:text-[#14181B] active:scale-95"} ${busy ? "animate-pulse ring-2 ring-[#14181B]" : ""}`}>
              {seg.label}
            </button>
          );
        })}
      </div>

      {undo && !pending && (
        <div key={undo.key} className="overflow-hidden rounded-xl bg-[#14181B] text-white" data-id={`undo-strip-${site.id}`}>
          <div className="flex items-center gap-3 py-1.5 pe-1.5 ps-4">
            <span className="flex-1 text-sm font-semibold">{site.mode === "off" ? "כובה" : `עבר ל${modeShort(site.mode)}`}</span>
            <button type="button" data-id={`undo-${site.id}`} onClick={() => onSwitch(site.id, undo.backTo, true)} className="cursor-pointer rounded-lg px-3 py-2 text-sm font-bold text-[#4ADE80] transition-colors hover:bg-white/10 active:bg-white/20">
              ביטול · חזרה ל{modeShort(undo.backTo)}
            </button>
          </div>
          <div className="h-[3px] origin-right bg-[#4ADE80]" style={{ animation: `undo-drain ${undoSeconds}s linear forwards` }} />
          <style>{"@keyframes undo-drain{from{transform:scaleX(1)}to{transform:scaleX(0)}}"}</style>
        </div>
      )}

      {site.sets.local.campaigns.length > 1 && (
        <div className="rounded-2xl bg-white px-4 py-1 ring-1 ring-[#E3E5E1]" data-id={`campaigns-${site.id}`}>
          {site.sets.local.campaigns.map((c) => {
            const all = VERSIONS.flatMap((v) => site.sets[v].campaigns.filter((x) => x.label === c.label));
            const on = all.find((x) => x.status === "ENABLED");
            return (
              <div key={c.id} className="flex items-baseline gap-2 border-b border-[#E3E5E1] py-2.5 text-[13px] last:border-0">
                <span className="flex-1 font-semibold">{c.label}</span>
                <span className="text-[#5B6268]">היום {shekels(all.reduce((sum, x) => sum + x.todayCostIls, 0))}{on && ` מתוך ${shekels(on.budgetIls)} ליום`}</span>
              </div>
            );
          })}
        </div>
      )}

      <div className="rounded-2xl bg-white p-4 ring-1 ring-[#E3E5E1]" data-id={`geo-${site.id}`}>
        <h3 className="text-[15px] font-bold">מאיפה הגיעו הקליקים</h3>
        {!geo && !geoError && <div className="mt-3 h-16 animate-pulse rounded-lg bg-[#ECEEF0]" />}
        {!geo && geoError && <p role="alert" className="mt-2 text-sm text-[#B42318]">{geoError}</p>}
        {geo && geo.totals.clicks === 0 && <p className="mt-2 text-sm text-[#5B6268]">אין קליקים ב-{days} הימים האחרונים.</p>}
        {geo && geo.totals.clicks > 0 && (
          <>
            <p className="mt-2 text-sm text-[#5B6268]">
              <b className="text-[#14181B]">{Math.round((geo.inArea.costIls / geo.totals.costIls) * 100)}%</b> מהכסף מקליקים באזור שלך · {shekels(geo.inArea.costIls)} מתוך {shekels(geo.totals.costIls)}
            </p>
            <ul className="mt-2">
              {geo.cities.slice(0, 6).map((c) => (
                <li key={c.id} className="flex items-baseline gap-2 border-t border-[#E3E5E1] py-1.5 text-[13px]">
                  <span className="flex-1 font-medium">{c.he || c.name}</span>
                  <span className="text-[#979DA3]">{c.inArea ? "באזור" : `${c.distanceKm} ק״מ`}</span>
                  <span className="w-14 text-start font-semibold">{shekels(c.costIls)}</span>
                </li>
              ))}
            </ul>
          </>
        )}
      </div>
    </section>
  );
}
