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

serve(async (req) => {
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }

  try {
    const { phone } = await req.json()
    const formattedPhone = phone.startsWith("+") ? phone : `+${phone}`

    const code = Math.floor(1000 + Math.random() * 9000).toString()
    const codeHash = await hashCode(code)
    const expiresAt = new Date(Date.now() + 5 * 60 * 1000).toISOString() // 5 min expiry

    const supabaseAdmin = createClient(
      Deno.env.get('SUPABASE_URL')!,
      Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!
    )

    // clear any older codes for this number first
    await supabaseAdmin.from('otp_codes').delete().eq('phone', formattedPhone)

    const { error: dbError } = await supabaseAdmin.from('otp_codes').insert({
      phone: formattedPhone,
      code_hash: codeHash,
      expires_at: expiresAt,
    })
    if (dbError) throw new Error(`DB error: ${dbError.message}`)

    const VERIFYWAY_TOKEN = Deno.env.get('VERIFYWAY_TOKEN')
    if (!VERIFYWAY_TOKEN) throw new Error("Missing VERIFYWAY_TOKEN")

    const payload = {
      recipient: formattedPhone,
      type: "otp",
      code: code,
      channel: "whatsapp"
    }

    const response = await fetch('https://api.verifyway.com/api/v1/', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${VERIFYWAY_TOKEN}`,
        'Content-Type': 'application/json',
        'Accept': 'application/json'
      },
      body: JSON.stringify(payload)
    })

    if (!response.ok) throw new Error(`Verifyway API Error: ${response.status}`)

    return new Response(
      JSON.stringify({ success: true }),
      { headers: { ...corsHeaders, 'Content-Type': 'application/json' }, status: 200 }
    )
  } catch (error) {
    return new Response(
      JSON.stringify({ error: error.message }),
      { headers: { ...corsHeaders, 'Content-Type': 'application/json' }, status: 400 }
    )
  }
})
