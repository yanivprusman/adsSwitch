import "server-only";
import net from "net";

/**
 * Minimal client for the local automateLinux daemon's UNIX socket: connect,
 * write one JSON command + "\n", read until the reply ends with "\n".
 *
 * This app is a thin skin over `d adsArea`. The daemon (utilities/adsArea.py)
 * owns the Google Ads calls, the nationwide/local pairing and the rule that
 * whole sets switch together — this process owns nothing.
 */
const UDS_PATH =
  process.env.AUTOMATE_LINUX_SOCKET_PATH ||
  "/run/automatelinux/automatelinux-daemon.sock";

function sendDaemonCommand(
  command: Record<string, unknown>,
  timeoutMs = 90000,
): Promise<string> {
  return new Promise((resolve, reject) => {
    let response = "";
    let done = false;
    const client = net.createConnection(UDS_PATH);
    const finish = (fn: (v: never) => void, v: unknown) => {
      if (done) return;
      done = true;
      clearTimeout(timer);
      client.destroy();
      (fn as (v: unknown) => void)(v);
    };
    const timer = setTimeout(
      () => finish(reject as never, new Error("daemon timeout")),
      timeoutMs,
    );
    client.on("connect", () => client.write(JSON.stringify(command) + "\n"));
    client.on("data", (d) => {
      response += d.toString();
      if (response.endsWith("\n")) finish(resolve as never, response);
    });
    client.on("error", (e) => finish(reject as never, e));
    client.on("close", () => finish(resolve as never, response));
  });
}

/** `d adsArea --json 1` always answers with one JSON object; anything else is
 *  a daemon-level refusal and is surfaced as one. */
export async function daemonJson(
  command: Record<string, unknown>,
): Promise<Record<string, unknown>> {
  const raw = await sendDaemonCommand({ ...command, json: 1 });
  try {
    return JSON.parse(raw.trim());
  } catch {
    return { ok: false, error: raw.trim() || "daemon returned nothing" };
  }
}
