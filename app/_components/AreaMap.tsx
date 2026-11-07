import { ISRAEL_OUTLINE } from "@/lib/israel";
import { modeGlow, type Center, type GeoCity, type Mode } from "@/lib/ads-model";

const LON_MIN = 34.2, LON_MAX = 35.98, LAT_MIN = 29.42, LAT_MAX = 33.36;
const LON_SCALE = Math.cos((31.4 * Math.PI) / 180);
const SCALE = 100; // SVG units per degree of latitude
const WIDTH = (LON_MAX - LON_MIN) * LON_SCALE * SCALE;
const HEIGHT = (LAT_MAX - LAT_MIN) * SCALE;
/** Latitudes, not district borders: a picture of "where", not a boundary. Same bands as the phone. */
const BANDS: Partial<Record<Mode, [number, number, number?, number?]>> = {
  south: [LAT_MIN, 31.85], center: [31.85, 32.4], north: [32.4, LAT_MAX],
  jerusalem: [31.62, 31.95, 34.88, LON_MAX],
};

const x = (lon: number) => (lon - LON_MIN) * LON_SCALE * SCALE;
const y = (lat: number) => (LAT_MAX - lat) * SCALE;

const OUTLINE = (() => {
  const points: string[] = [];
  for (let i = 0; i < ISRAEL_OUTLINE.length; i += 2) points.push(`${x(ISRAEL_OUTLINE[i]).toFixed(1)},${y(ISRAEL_OUTLINE[i + 1]).toFixed(1)}`);
  return points.join(" ");
})();

export default function AreaMap({ siteId, mode, pending, centers, radiusKm, cities }: {
  siteId: string; mode: Mode; pending: boolean; centers: Center[]; radiusKm: number; cities: GeoCity[];
}) {
  const band = BANDS[mode];
  const clip = `band-${siteId}`;
  const maxCost = Math.max(0, ...cities.map((c) => c.costIls));
  return (
    <svg viewBox={`0 0 ${WIDTH} ${HEIGHT}`} className={`h-56 w-auto shrink-0 ${pending ? "animate-pulse" : ""}`} style={{ opacity: mode === "off" ? 0.5 : 1 }} role="img" aria-label={`מפה: ${mode}`}>
      {band && <clipPath id={clip}><rect x={band[2] === undefined ? 0 : x(band[2])} y={y(band[1])} width={band[2] === undefined ? WIDTH : x(band[3] ?? LON_MAX) - x(band[2])} height={y(band[0]) - y(band[1])} /></clipPath>}
      <polygon points={OUTLINE} fill={mode === "nationwide" ? "rgba(37,99,235,.42)" : mode === "mixed" ? "rgba(217,119,6,.22)" : "rgba(255,255,255,.06)"} stroke={mode === "nationwide" ? modeGlow(mode) : "rgba(255,255,255,.32)"} strokeWidth="1.2" strokeLinejoin="round" />
      {band && <polygon points={OUTLINE} fill="rgba(124,58,237,.65)" clipPath={`url(#${clip})`} />}
      {centers.map((c) => (
        <g key={c.name}>
          <circle cx={x(c.lon)} cy={y(c.lat)} r={(radiusKm / 111) * SCALE} fill={mode === "local" ? "rgba(22,163,74,.38)" : "none"} stroke="#4ADE80" strokeOpacity={mode === "local" ? 0.95 : 0.55} strokeWidth="1.3" strokeDasharray={mode === "local" ? undefined : "4 3"} />
        </g>
      ))}
      {maxCost > 0 && cities.map((c) => (
        <circle key={c.id} cx={x(c.lon)} cy={y(c.lat)} r={1.5 + 4 * Math.sqrt(c.costIls / maxCost)} fill={c.inArea ? "#4ADE80" : "#DCE4EC"} fillOpacity=".9" />
      ))}
      {centers.map((c) => <circle key={c.name} cx={x(c.lon)} cy={y(c.lat)} r="2.4" fill="#fff" />)}
    </svg>
  );
}
