import "server-only";

/**
 * The one guard on this API.
 *
 * These routes turn paid advertising on and off. The service listens on
 * 0.0.0.0 — the WireGuard overlay AND the home LAN — so "only my devices can
 * reach it" is not true enough to rely on.
 *
 * A missing token in the environment REFUSES every request: an API that
 * guards nothing because its secret was never set looks like it is working.
 */
export function authorize(request: Request): string | null {
  const expected = process.env.ADS_SWITCH_API_TOKEN ?? "";
  if (!expected) return "ADS_SWITCH_API_TOKEN is not set on the server";

  const header = request.headers.get("authorization") ?? "";
  const given = header.startsWith("Bearer ") ? header.slice(7) : "";
  if (!given) return "missing bearer token";

  if (given.length !== expected.length) return "bad token";
  let diff = 0;
  for (let i = 0; i < expected.length; i++)
    diff |= given.charCodeAt(i) ^ expected.charCodeAt(i);
  return diff === 0 ? null : "bad token";
}
