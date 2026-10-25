// Test suite for Cloak SOS Access Edge Function /play response contract

import { assertEquals } from "https://deno.land/std@0.177.0/testing/asserts.ts";
import { handlePlay, html } from "./index.ts";

Deno.test("html helper sets Content-Type to text/html; charset=utf-8", () => {
  const res = html("<h1>Emergency Audio</h1>");
  assertEquals(res.headers.get("content-type"), "text/html; charset=utf-8");
  assertEquals(res.status, 200);
});

Deno.test("handlePlay returns 200 Response with text/html Content-Type", async () => {
  const res = handlePlay();
  assertEquals(res.headers.get("content-type"), "text/html; charset=utf-8");
  assertEquals(res.status, 200);

  const body = await res.text();
  assertEquals(body.includes("new URLSearchParams(window.location.search)"), true);
  assertEquals(body.includes('const SID="",TK=""'), false);
});
