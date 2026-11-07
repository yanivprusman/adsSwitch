"use client";

import { useState } from "react";

/** What a browser that has not signed in sees. The desktop launcher signs in by itself; this is for any other browser. */
export default function SignIn({ linkExpired }: { linkExpired: boolean }) {
  const [code, setCode] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(linkExpired ? "קישור הכניסה פג תוקף. פתח שוב מהאייקון, או הזן את הקוד." : "");

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    if (!code.trim() || busy) return;
    setBusy(true);
    const r = await fetch("/api/session", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ code: code.trim() }),
    }).catch(() => null);
    if (r?.ok) {
      window.location.replace("/");
      return;
    }
    setError(r?.status === 401 ? "הקוד שגוי" : "אין חיבור לשרת");
    setBusy(false);
  }

  return (
    <main dir="rtl" lang="he" className="grid min-h-dvh place-items-center bg-[#F3F4F1] px-5 text-[#14181B]">
      <form data-id="sign-in-form" onSubmit={submit} className="w-full max-w-sm rounded-2xl border border-[#E3E5E1] bg-white p-6">
        <h1 className="text-xl font-bold">מודעות</h1>
        <p className="mt-2 text-sm text-[#5B6268]">מכאן מפעילים ומכבים פרסום בתשלום, ולכן הדף נעול.</p>
        <label htmlFor="access-code" className="mt-5 block text-[13px] font-medium text-[#5B6268]">קוד גישה</label>
        <input
          id="access-code"
          data-id="access-code"
          type="password"
          autoComplete="current-password"
          autoFocus
          dir="ltr"
          value={code}
          onChange={(e) => { setCode(e.target.value); setError(""); }}
          className="mt-1 h-11 w-full rounded-lg border border-[#C9CCC7] px-3 font-mono outline-none transition-colors hover:border-[#979DA3] focus:border-[#14181B]"
        />
        {error ? (
          <p role="alert" className="mt-1 text-xs text-[#B42318]">{error}</p>
        ) : (
          <p className="mt-1 text-xs text-[#979DA3]">הערך של <bdi className="font-mono">ADS_SWITCH_API_TOKEN</bdi> בקובץ <bdi className="font-mono">.env.local</bdi></p>
        )}
        <button type="submit" data-id="sign-in" disabled={!code.trim() || busy} className="mt-5 h-12 w-full cursor-pointer rounded-lg bg-[#14181B] font-semibold text-white transition-opacity hover:opacity-85 active:opacity-70 disabled:cursor-not-allowed disabled:opacity-50">
          {busy ? "נכנס…" : "כניסה"}
        </button>
      </form>
    </main>
  );
}
