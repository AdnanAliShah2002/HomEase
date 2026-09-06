import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2"
import { crypto } from "https://deno.land/std@0.168.0/crypto/mod.ts"

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

async function hashCode(code: string): Promise<string> {
  const data = new TextEncoder().encode(code)
  const hashBuffer = await crypto.subtle.digest("SHA-256", data)
  return Array.from(new Uint8Array(hashBuffer)).map(b => b.toString(16).padStart(2, "0")).join("")
}

// Signs a minimal HS256 JWT for Supabase RLS authentication when JWT secret is configured
async function createSupabaseJwt(userId: string, phone: string, secret: string): Promise<string> {
  const header = { alg: "HS256", typ: "JWT" }
  const now = Math.floor(Date.now() / 1000)
  const payload = {
    aud: "authenticated",
    exp: now + (30 * 24 * 3600), // 30 days expiry
    sub: userId,
    phone: phone,
    role: "authenticated",
    app_metadata: { provider: "phone", providers: ["phone"] },
    user_metadata: { phone: phone }
  }

  const base64UrlEncode = (obj: unknown) => {
    const jsonStr = JSON.stringify(obj)
    const bytes = new TextEncoder().encode(jsonStr)
    return btoa(String.fromCharCode(...bytes)).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "")
  }

  const encodedHeader = base64UrlEncode(header)
  const encodedPayload = base64UrlEncode(payload)
  const unsignedToken = `${encodedHeader}.${encodedPayload}`

  const keyData = new TextEncoder().encode(secret)
  const key = await crypto.subtle.importKey(
    "raw",
    keyData,
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["sign"]
  )

  const signatureBuffer = await crypto.subtle.sign(
    "HMAC",
    key,
    new TextEncoder().encode(unsignedToken)
  )

  const signature = btoa(String.fromCharCode(...new Uint8Array(signatureBuffer)))
    .replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "")

  return `${unsignedToken}.${signature}`
}

serve(async (req) => {
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }

  try {
    const { phone, code } = await req.json()
    const formattedPhone = phone.startsWith("+") ? phone : `+${phone}`

    // Edge function uses service role key strictly on the server to manage OTP verification and auth
    const supabaseAdmin = createClient(
      Deno.env.get('SUPABASE_URL')!,
      Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!
    )

    const { data: record, error: fetchError } = await supabaseAdmin
      .from('otp_codes')
      .select('*')
      .eq('phone', formattedPhone)
      .order('created_at', { ascending: false })
      .limit(1)
      .maybeSingle()

    if (fetchError || !record) {
      return new Response(JSON.stringify({ success: false, error: "No pending code for this number" }), { headers: corsHeaders, status: 400 })
    }

    if (new Date(record.expires_at) < new Date()) {
      return new Response(JSON.stringify({ success: false, error: "Code expired" }), { headers: corsHeaders, status: 400 })
    }

    if (record.attempts >= 5) {
      return new Response(JSON.stringify({ success: false, error: "Too many attempts" }), { headers: corsHeaders, status: 429 })
    }

    const submittedHash = await hashCode(code)

    if (submittedHash !== record.code_hash) {
      await supabaseAdmin.from('otp_codes').update({ attempts: record.attempts + 1 }).eq('id', record.id)
      return new Response(JSON.stringify({ success: false, error: "Incorrect code" }), { headers: corsHeaders, status: 400 })
    }

    // OTP verified successfully - clear pending code
    await supabaseAdmin.from('otp_codes').delete().eq('id', record.id)

    // Lookup user in public.users or profiles
    let existingUserId: string | null = null
    const { data: userInDb } = await supabaseAdmin
      .from('users')
      .select('id')
      .eq('phone', formattedPhone)
      .maybeSingle()

    if (userInDb?.id) {
      existingUserId = userInDb.id
    } else {
      const { data: profile } = await supabaseAdmin
        .from('profiles')
        .select('id')
        .eq('phone', formattedPhone)
        .maybeSingle()
      if (profile?.id) {
        existingUserId = profile.id
      }
    }

    let userId = existingUserId
    if (!userId) {
      try {
        const { data: authUser } = await supabaseAdmin.auth.admin.createUser({
          phone: formattedPhone,
          phone_confirm: true,
          user_metadata: { phone: formattedPhone }
        })
        userId = authUser?.user?.id ?? null
      } catch (_: unknown) {
        // Fallback
      }
    }

    if (!userId) {
      userId = crypto.randomUUID()
    }

    // Generate authenticated JWT access token for client RLS queries
    let accessToken: string | null = null
    const jwtSecret = Deno.env.get('JWT_SECRET') || Deno.env.get('SUPABASE_JWT_SECRET')
    if (jwtSecret) {
      try {
        accessToken = await createSupabaseJwt(userId, formattedPhone, jwtSecret)
      } catch (_: unknown) {
        // Fallback
      }
    }

    return new Response(
      JSON.stringify({
        success: true,
        isNewUser: !existingUserId,
        userId: userId,
        accessToken: accessToken,
        tokenType: "bearer",
        phone: formattedPhone
      }),
      { headers: { ...corsHeaders, 'Content-Type': 'application/json' }, status: 200 }
    )
  } catch (error) {
    return new Response(
      JSON.stringify({ error: error.message }),
      { headers: { ...corsHeaders, 'Content-Type': 'application/json' }, status: 400 }
    )
  }
})
