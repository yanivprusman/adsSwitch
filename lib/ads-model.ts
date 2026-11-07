/** What `d adsArea --json 1` answers, and the words the screens use for it. */
export type Campaign = { id: string; label: string; status: string; budgetIls: number; todayCostIls: number; todayClicks: number };
export type AdSet = { on: boolean; enabledCount: number; campaigns: Campaign[]; dailyBudgetIls: number; todayCostIls: number; todayClicks: number };
export type Mode = "local" | "south" | "center" | "north" | "jerusalem" | "nationwide" | "off" | "mixed";
export type Version = Exclude<Mode, "off" | "mixed">;
export type Site = { id: string; label: string; mode: Mode; sets: Record<Version, AdSet> };
export type Center = { name: string; he: string; lat: number; lon: number };
export type AreaState = { ok: boolean; error?: string; mode: Mode; site: string | null; changedFrom: Mode | null; updatedAt: string; radiusKm: number; centers: Center[]; sites: Site[] };
export type GeoCity = { id: string; name: string; he: string; lat: number; lon: number; distanceKm: number; inArea: boolean; costIls: number; clicks: number };
export type Spend = { costIls: number; clicks: number };
export type GeoReport = { ok: boolean; error?: string; days: number; radiusKm: number; totals: Spend; inArea: Spend; unlocated: Spend; cities: GeoCity[] };

export const VERSIONS: Version[] = ["local", "south", "center", "north", "jerusalem", "nationwide"];
/** The switch's buttons, in reading order. */
export const SWITCH: { mode: Version | "off"; label: string }[] = [
  { mode: "local", label: "האזור שלי" },
  { mode: "south", label: "דרום" },
  { mode: "center", label: "מרכז" },
  { mode: "jerusalem", label: "ירושלים" },
  { mode: "north", label: "צפון" },
  { mode: "nationwide", label: "כל הארץ" },
  { mode: "off", label: "כבוי" },
];

export const SITE_DETAIL: Record<string, string> = {
  pigeons: "הרחקת יונים וניקוי חלונות",
  etc: "עבודות גובה",
  panelshed: "מחסני גינה: חיפוש ושופינג",
};

export function modeTitle(mode: Mode): string {
  return { local: "באזור שלך", south: "בדרום", center: "במרכז", north: "בצפון", jerusalem: "בירושלים והסביבה", nationwide: "בכל הארץ", off: "כבוי", mixed: "מעורב" }[mode];
}

export function modeShort(mode: Mode): string {
  return { local: "האזור שלי", south: "דרום", center: "מרכז", north: "צפון", jerusalem: "ירושלים", nationwide: "כל הארץ", off: "כבוי", mixed: "מעורב" }[mode];
}

export function modeDetail(mode: Mode, radiusKm: number, centers: Center[]): string {
  switch (mode) {
    case "local": return `${radiusKm} ק״מ סביב ${centers.map((c) => c.he).join(" ו")}`;
    case "south": return "מחוז דרום, כולל אילת";
    case "center": return "מחוזות תל אביב והמרכז";
    case "north": return "מחוזות חיפה והצפון";
    case "jerusalem": return "מחוז ירושלים";
    case "nationwide": return "כל ישראל";
    case "off": return "אף מודעה לא מוצגת, ולא יורד כסף.";
    case "mixed": return "קמפיינים הופעלו ידנית. בחירה למטה תסדר אותם.";
  }
}

/** Solid colour on white, and the lighter colour on the dark panel. */
export function modeColor(mode: Mode): string {
  if (mode === "local") return "#16A34A";
  if (mode === "nationwide") return "#2563EB";
  if (mode === "south" || mode === "center" || mode === "north" || mode === "jerusalem") return "#7C3AED";
  if (mode === "mixed") return "#D97706";
  return "#6B7280";
}

export function modeGlow(mode: Mode): string {
  if (mode === "local") return "#4ADE80";
  if (mode === "nationwide") return "#60A5FA";
  if (mode === "south" || mode === "center" || mode === "north" || mode === "jerusalem") return "#A78BFA";
  if (mode === "mixed") return "#D97706";
  return "#B4BAC1";
}

export function shekels(v: number, forceAgorot = false): string {
  const showAgorot = forceAgorot || (v % 1 !== 0 && v < 100);
  return "₪" + v.toLocaleString("en-US", { minimumFractionDigits: showAgorot ? 2 : 0, maximumFractionDigits: showAgorot ? 2 : 0 });
}
