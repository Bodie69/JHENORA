// Supabase Edge Function: create-display-account
//
// Lets an already-authenticated admin create a new "live_display" account
// (dedicated login for a campus signage kiosk device) without disturbing the
// admin's own session. This can't be done safely from the Android app: the
// normal client-side sign-up call authenticates the *caller* as the new
// user, which would log the admin out and turn them into the display
// account. This function instead runs server-side with the Supabase
// service-role key (auth.admin.createUser), which creates the auth user
// without touching the caller's session, and never ships to the client.
//
// Deploy with the Supabase CLI, linked to your project:
//   supabase functions deploy create-display-account
// (or paste this file into Dashboard -> Edge Functions -> New Function).
// Deploy with default JWT verification (do NOT pass --no-verify-jwt) so
// Supabase's gateway rejects requests with no/invalid Authorization header
// before this code even runs. SUPABASE_URL and SUPABASE_SERVICE_ROLE_KEY are
// injected automatically into every Edge Function's environment; no manual
// secret configuration is needed.
//
// Request body (JSON): { "email": string, "password": string, "full_name": string }
// Response (JSON):
//   200 { "success": true, "uid": string }
//   4xx/5xx { "error": string }

import { createClient } from "npm:@supabase/supabase-js@2";

const CORS_HEADERS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

function jsonResponse(body: Record<string, unknown>, status: number): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...CORS_HEADERS, "Content-Type": "application/json" },
  });
}

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: CORS_HEADERS });
  }
  if (req.method !== "POST") {
    return jsonResponse({ error: "Method not allowed" }, 405);
  }

  const authHeader = req.headers.get("Authorization");
  if (!authHeader) {
    return jsonResponse({ error: "Missing Authorization header" }, 401);
  }
  const callerJwt = authHeader.replace(/^Bearer\s+/i, "");

  const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
  const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
  const admin = createClient(supabaseUrl, serviceRoleKey);

  // 1. Resolve the caller from their JWT and confirm they're an admin.
  const { data: callerData, error: callerError } = await admin.auth.getUser(callerJwt);
  if (callerError || !callerData?.user) {
    return jsonResponse({ error: "Invalid session" }, 401);
  }

  const { data: callerProfile, error: profileError } = await admin
    .from("users")
    .select("role")
    .eq("uid", callerData.user.id)
    .maybeSingle();

  if (profileError || !callerProfile || callerProfile.role !== "admin") {
    return jsonResponse({ error: "Forbidden: admin role required" }, 403);
  }

  // 2. Validate input.
  let body: { email?: string; password?: string; full_name?: string };
  try {
    body = await req.json();
  } catch {
    return jsonResponse({ error: "Invalid JSON body" }, 400);
  }

  const email = (body.email ?? "").trim();
  const password = body.password ?? "";
  const fullName = (body.full_name ?? "").trim();

  if (!email.includes("@")) {
    return jsonResponse({ error: "A valid email is required" }, 400);
  }
  if (password.length < 8) {
    return jsonResponse({ error: "Password must be at least 8 characters" }, 400);
  }
  if (!fullName) {
    return jsonResponse({ error: "A label/name is required" }, 400);
  }

  // 3. Create the auth user server-side (does not affect the caller's session).
  const { data: created, error: createError } = await admin.auth.admin.createUser({
    email,
    password,
    email_confirm: true,
  });

  if (createError || !created?.user) {
    const message = createError?.message ?? "Failed to create account";
    const status = /already.*registered|already.*exists/i.test(message) ? 409 : 500;
    return jsonResponse({ error: message }, status);
  }

  const uid = created.user.id;

  // 4. Insert the profile row (service-role bypasses RLS).
  const { error: insertError } = await admin.from("users").insert({
    uid,
    email,
    role: "live_display",
    first_name: fullName,
    status: "approved",
    email_verified: true,
    created_at: Date.now(),
  });

  if (insertError) {
    // Roll back the orphaned auth user so a failed creation never leaves a
    // half-created account behind.
    await admin.auth.admin.deleteUser(uid);
    return jsonResponse({ error: insertError.message }, 500);
  }

  return jsonResponse({ success: true, uid }, 200);
});
