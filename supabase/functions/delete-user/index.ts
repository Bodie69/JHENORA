// Supabase Edge Function: delete-user
//
// Lets an already-authenticated admin permanently delete another user's account.
// This can't be done from the Android app: removing the Supabase Auth login
// (auth.users) needs the service-role key (auth.admin.deleteUser), which must
// never ship to the client. Deleting the auth user cascades in the database:
//   auth.users -> public.users (on delete cascade)
//              -> announcements / events / notifications they own (on delete cascade)
//              -> organizations.leader_id (on delete set null)
// Audit log rows are kept (they have no foreign key to users).
//
// Guard rails: the caller must be an admin, cannot delete themselves, and cannot
// delete another admin (prevents one compromised admin from locking everyone out).
//
// Deploy with the Supabase CLI, linked to your project:
//   supabase functions deploy delete-user
// (or paste this file into Dashboard -> Edge Functions -> New Function).
// Deploy with default JWT verification (do NOT pass --no-verify-jwt).
// SUPABASE_URL and SUPABASE_SERVICE_ROLE_KEY are injected automatically.
//
// Request body (JSON): { "uid": string }
// Response (JSON):
//   200 { "success": true, "uid": string }
//   4xx/5xx { "error": string }

import { createClient } from "npm:@supabase/supabase-js@2";

const CORS_HEADERS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

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
  const callerId = callerData.user.id;

  const { data: callerProfile, error: profileError } = await admin
    .from("users")
    .select("role")
    .eq("uid", callerId)
    .maybeSingle();

  if (profileError || !callerProfile || callerProfile.role !== "admin") {
    return jsonResponse({ error: "Forbidden: admin role required" }, 403);
  }

  // 2. Validate input.
  let body: { uid?: string };
  try {
    body = await req.json();
  } catch {
    return jsonResponse({ error: "Invalid JSON body" }, 400);
  }

  const uid = (body.uid ?? "").trim();
  if (!UUID_RE.test(uid)) {
    return jsonResponse({ error: "A valid user id is required" }, 400);
  }
  if (uid === callerId) {
    return jsonResponse({ error: "You can't delete your own account" }, 400);
  }

  // 3. Look up the target and refuse to delete admins.
  const { data: target, error: targetError } = await admin
    .from("users")
    .select("role, email")
    .eq("uid", uid)
    .maybeSingle();

  if (targetError) {
    return jsonResponse({ error: targetError.message }, 500);
  }
  if (target?.role === "admin") {
    return jsonResponse({ error: "Admin accounts can't be deleted" }, 403);
  }

  // 4. Clear the denormalized leader name on any organization they led
  //    (leader_id itself is nulled by the foreign key on delete).
  await admin.from("organizations").update({ leader_name: null }).eq("leader_id", uid);

  // 5. Delete the auth user; the database cascades the profile and their content.
  const { error: deleteError } = await admin.auth.admin.deleteUser(uid);
  if (deleteError) {
    const notFound = /not.*found/i.test(deleteError.message);
    if (!notFound) {
      return jsonResponse({ error: deleteError.message }, 500);
    }
    // No auth user (e.g. a legacy profile row created before real auth):
    // remove the orphaned profile row directly.
    const { error: rowError } = await admin.from("users").delete().eq("uid", uid);
    if (rowError) {
      return jsonResponse({ error: rowError.message }, 500);
    }
  }

  // 6. Audit trail (service role bypasses the insert-own RLS policy).
  await admin.from("audit_logs").insert({
    user_id: callerId,
    user_email: callerData.user.email ?? null,
    action: "delete",
    target_type: "user",
    target_id: uid,
    details: `User deleted: ${target?.email ?? uid}`,
    timestamp: Date.now(),
  });

  return jsonResponse({ success: true, uid }, 200);
});
