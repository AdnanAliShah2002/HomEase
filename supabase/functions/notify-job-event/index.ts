import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2"

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

interface WebhookPayload {
  type?: 'INSERT' | 'UPDATE' | 'DELETE'
  table?: string
  record?: Record<string, any>
  old_record?: Record<string, any>
  event?: string
  // Direct API call payload alternative
  event_type?: 'job_created' | 'offer_created' | 'offer_accepted' | 'status_updated'
  job_id?: string
  data?: Record<string, any>
}

// Convert PEM string to ArrayBuffer for Web Crypto importKey
function pemToArrayBuffer(pem: string): ArrayBuffer {
  const b64 = pem
    .replace(/-----BEGIN[ A-Z0-9_-]+-----/g, '')
    .replace(/-----END[ A-Z0-9_-]+-----/g, '')
    .replace(/\s+/g, '')
  const binary = atob(b64)
  const bytes = new Uint8Array(binary.length)
  for (let i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i)
  }
  return bytes.buffer
}

function base64UrlEncode(data: Uint8Array | string): string {
  let str: string
  if (typeof data === 'string') {
    str = btoa(unescape(encodeURIComponent(data)))
  } else {
    let binary = ''
    for (let i = 0; i < data.byteLength; i++) {
      binary += String.fromCharCode(data[i])
    }
    str = btoa(binary)
  }
  return str.replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
}

// Mint Google OAuth2 Bearer token for Firebase HTTP v1 API
async function getFirebaseAccessToken(serviceAccountJson: any): Promise<string> {
  const privateKeyPem = serviceAccountJson.private_key
  const clientEmail = serviceAccountJson.client_email
  const keyBuffer = pemToArrayBuffer(privateKeyPem)

  const cryptoKey = await crypto.subtle.importKey(
    'pkcs8',
    keyBuffer,
    { name: 'RSASSA-PKCS1-v1_5', hash: 'SHA-256' },
    false,
    ['sign']
  )

  const now = Math.floor(Date.now() / 1000)
  const header = { alg: 'RS256', typ: 'JWT' }
  const claimSet = {
    iss: clientEmail,
    scope: 'https://www.googleapis.com/auth/firebase.messaging',
    aud: 'https://oauth2.googleapis.com/token',
    exp: now + 3600,
    iat: now
  }

  const encodedHeader = base64UrlEncode(JSON.stringify(header))
  const encodedClaim = base64UrlEncode(JSON.stringify(claimSet))
  const signingInput = `${encodedHeader}.${encodedClaim}`

  const signature = await crypto.subtle.sign(
    'RSASSA-PKCS1-v1_5',
    cryptoKey,
    new TextEncoder().encode(signingInput)
  )

  const jwt = `${signingInput}.${base64UrlEncode(new Uint8Array(signature))}`

  const res = await fetch('https://oauth2.googleapis.com/token', {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: `grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer&assertion=${jwt}`
  })

  if (!res.ok) {
    const text = await res.text()
    throw new Error(`Failed to obtain Google access token: ${text}`)
  }

  const tokenData = await res.json()
  return tokenData.access_token
}

// Dispatch push notification to a single FCM device token
async function sendFcmMessage(
  token: string,
  title: string,
  body: string,
  dataPayload: Record<string, string>,
  channelId: string,
  serviceAccount: any | null,
  fcmServerKey: string | null
): Promise<{ success: boolean; error?: string }> {
  try {
    // 1. Prefer HTTP v1 API if service account is configured
    if (serviceAccount && serviceAccount.project_id) {
      const accessToken = await getFirebaseAccessToken(serviceAccount)
      const url = `https://fcm.googleapis.com/v1/projects/${serviceAccount.project_id}/messages:send`

      const messageBody = {
        message: {
          token: token,
          notification: {
            title: title,
            body: body
          },
          data: dataPayload,
          android: {
            priority: 'high',
            notification: {
              channel_id: channelId,
              sound: 'default'
            }
          }
        }
      }

      const res = await fetch(url, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${accessToken}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(messageBody)
      })

      if (!res.ok) {
        const errText = await res.text()
        return { success: false, error: `HTTP v1 Error ${res.status}: ${errText}` }
      }
      return { success: true }
    }

    // 2. Fallback to legacy server key if service account is not yet uploaded
    if (fcmServerKey) {
      const legacyUrl = 'https://fcm.googleapis.com/fcm/send'
      const legacyBody = {
        to: token,
        priority: 'high',
        notification: {
          title: title,
          body: body,
          channel_id: channelId,
          sound: 'default'
        },
        data: dataPayload
      }

      const res = await fetch(legacyUrl, {
        method: 'POST',
        headers: {
          'Authorization': `key=${fcmServerKey}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(legacyBody)
      })

      if (!res.ok) {
        const errText = await res.text()
        return { success: false, error: `Legacy FCM Error ${res.status}: ${errText}` }
      }
      return { success: true }
    }

    return { success: false, error: 'Neither FIREBASE_SERVICE_ACCOUNT nor FCM_SERVER_KEY configured.' }
  } catch (err: any) {
    return { success: false, error: err.message || String(err) }
  }
}

serve(async (req) => {
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }

  try {
    const supabaseAdmin = createClient(
      Deno.env.get('SUPABASE_URL')!,
      Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!
    )

    // Load Firebase configuration
    let serviceAccount: any = null
    const saEnv = Deno.env.get('FIREBASE_SERVICE_ACCOUNT')
    if (saEnv) {
      try {
        serviceAccount = JSON.parse(saEnv)
      } catch (_e) {
        console.warn('Could not parse FIREBASE_SERVICE_ACCOUNT as JSON')
      }
    }
    const fcmServerKey = Deno.env.get('FCM_SERVER_KEY') || null

    const payload: WebhookPayload = await req.json()
    const table = payload.table || (payload.record?.category ? 'jobs' : 'job_offers')
    const type = payload.type || (payload.record?.status === 'accepted' ? 'UPDATE' : 'INSERT')
    const record = payload.record || {}
    const oldRecord = payload.old_record || {}

    const results: Array<{ recipient: string; token: string; status: string; error?: string }> = []

    // ========================================================================
    // SCENARIO 1: New Job Inserted in public.jobs (Status: searching)
    // Notify: All online providers whose registered categories match the job
    // ========================================================================
    if (table === 'jobs' && type === 'INSERT') {
      const jobId = record.id
      const categoryId = record.category_id || record.category || ''
      const serviceTitle = record.service_title || 'Service Request'
      const budgetRs = record.budget_rs || 0
      const cityArea = record.city_area || ''

      // Fetch tokens of online providers matching this category
      const { data: tokens, error: tokensError } = await supabaseAdmin
        .from('device_tokens')
        .select('phone, fcm_token, categories')
        .eq('role', 'provider')
        .eq('is_online', true)

      if (tokensError) {
        console.error('Error fetching provider tokens:', tokensError)
      }

      const targetTokens = (tokens || []).filter((t: any) => {
        if (!t.fcm_token) return false
        if (!t.categories || t.categories.length === 0) return true
        const catNorm = categoryId.toLowerCase().trim()
        return t.categories.some((c: string) => c.toLowerCase().trim() === catNorm)
      })

      for (const t of targetTokens) {
        const res = await sendFcmMessage(
          t.fcm_token,
          `New ${serviceTitle} Request Nearby!`,
          `Budget: Rs. ${budgetRs} in ${cityArea}. Tap to accept or counter.`,
          {
            type: 'job_ping',
            job_id: String(jobId),
            service_title: serviceTitle,
            budget_rs: String(budgetRs),
            category: categoryId,
            city_area: cityArea
          },
          'homease_job_pings',
          serviceAccount,
          fcmServerKey
        )
        results.push({ recipient: t.phone, token: t.fcm_token, status: res.success ? 'sent' : 'failed', error: res.error })
      }
    }

    // ========================================================================
    // SCENARIO 2: New Offer Inserted in public.job_offers (Status: pending)
    // Notify: Customer who posted the job
    // ========================================================================
    else if (table === 'job_offers' && type === 'INSERT') {
      const jobId = record.job_id
      const providerName = record.provider_name || 'A technician'
      const offerPrice = record.offer_price_rs || record.counter_price_rs || 0

      // Look up customer phone from the parent job
      const { data: jobData } = await supabaseAdmin
        .from('jobs')
        .select('customer_phone, service_title')
        .eq('id', jobId)
        .single()

      if (jobData && jobData.customer_phone) {
        const { data: customerTokens } = await supabaseAdmin
          .from('device_tokens')
          .select('phone, fcm_token')
          .eq('phone', jobData.customer_phone)

        for (const t of (customerTokens || [])) {
          if (!t.fcm_token) continue
          const res = await sendFcmMessage(
            t.fcm_token,
            `New Offer: ${providerName}`,
            `Offered Rs. ${offerPrice} for ${jobData.service_title}. Tap to review.`,
            {
              type: 'offer_received',
              job_id: String(jobId),
              offer_id: String(record.id),
              price_rs: String(offerPrice),
              provider_name: providerName
            },
            'homease_offers',
            serviceAccount,
            fcmServerKey
          )
          results.push({ recipient: t.phone, token: t.fcm_token, status: res.success ? 'sent' : 'failed', error: res.error })
        }
      }
    }

    // ========================================================================
    // SCENARIO 3: Job Accepted (Status changed to 'accepted')
    // Notify:
    //  a) Selected provider: "Offer accepted! Start trip."
    //  b) Other bidding providers: "Job filled (rejected)"
    // ========================================================================
    else if (
      (table === 'jobs' && record.status?.toLowerCase() === 'accepted' && oldRecord.status?.toLowerCase() !== 'accepted') ||
      (table === 'job_offers' && record.status?.toLowerCase() === 'accepted')
    ) {
      const jobId = record.id || record.job_id
      const selectedPhone = record.provider_phone || record.selected_provider_phone
      const agreedPrice = record.agreed_price_rs || record.offer_price_rs || 0
      const serviceTitle = record.service_title || 'Service Request'

      // 1. Notify winning provider
      if (selectedPhone) {
        const { data: winningTokens } = await supabaseAdmin
          .from('device_tokens')
          .select('phone, fcm_token')
          .eq('phone', selectedPhone)

        for (const t of (winningTokens || [])) {
          if (!t.fcm_token) continue
          const res = await sendFcmMessage(
            t.fcm_token,
            'Offer Accepted!',
            `Your offer of Rs. ${agreedPrice} was accepted! Tap to start your trip.`,
            {
              type: 'offer_accepted',
              job_id: String(jobId),
              agreed_price_rs: String(agreedPrice)
            },
            'homease_status',
            serviceAccount,
            fcmServerKey
          )
          results.push({ recipient: t.phone, token: t.fcm_token, status: res.success ? 'sent' : 'failed', error: res.error })
        }
      }

      // 2. Reject other offers and notify competing providers
      const { data: otherOffers } = await supabaseAdmin
        .from('job_offers')
        .select('id, provider_phone')
        .eq('job_id', jobId)
        .neq('provider_phone', selectedPhone)

      for (const off of (otherOffers || [])) {
        await supabaseAdmin
          .from('job_offers')
          .update({ status: 'rejected' })
          .eq('id', off.id)

        const { data: compTokens } = await supabaseAdmin
          .from('device_tokens')
          .select('phone, fcm_token')
          .eq('phone', off.provider_phone)

        for (const t of (compTokens || [])) {
          if (!t.fcm_token) continue
          const res = await sendFcmMessage(
            t.fcm_token,
            'Job Filled',
            `The customer selected another quote for ${serviceTitle}.`,
            {
              type: 'offer_rejected',
              job_id: String(jobId)
            },
            'homease_status',
            serviceAccount,
            fcmServerKey
          )
          results.push({ recipient: t.phone, token: t.fcm_token, status: res.success ? 'sent' : 'failed', error: res.error })
        }
      }
    }

    // ========================================================================
    // SCENARIO 4: Job Status Progression (on_the_way, arrived, in_progress, awaiting_customer_confirmation, completed)
    // Notify: Customer
    // ========================================================================
    else if (table === 'jobs' && type === 'UPDATE') {
      const newStatus = (record.status || '').toLowerCase()
      const oldStatus = (oldRecord.status || '').toLowerCase()

      if (newStatus !== oldStatus && ['on_the_way', 'arrived', 'in_progress', 'awaiting_customer_confirmation', 'completed'].includes(newStatus)) {
        const customerPhone = record.customer_phone
        const providerName = record.provider_name || 'Technician'

        const statusMessages: Record<string, { title: string; body: string }> = {
          'on_the_way': {
            title: 'Technician On The Way',
            body: `${providerName} has started traveling to your address.`
          },
          'arrived': {
            title: 'Technician Arrived',
            body: `${providerName} has arrived at your location.`
          },
          'in_progress': {
            title: 'Work Started',
            body: `${providerName} has begun work on your service request.`
          },
          'awaiting_customer_confirmation': {
            title: 'Service Completed',
            body: `${providerName} has completed work. Please inspect and confirm completion.`
          },
          'completed': {
            title: 'Job Closed',
            body: `Job has been marked completed. Thank you for using HomEase!`
          }
        }

        const msgConfig = statusMessages[newStatus]
        if (customerPhone && msgConfig) {
          const { data: customerTokens } = await supabaseAdmin
            .from('device_tokens')
            .select('phone, fcm_token')
            .eq('phone', customerPhone)

          for (const t of (customerTokens || [])) {
            if (!t.fcm_token) continue
            const res = await sendFcmMessage(
              t.fcm_token,
              msgConfig.title,
              msgConfig.body,
              {
                type: 'status_update',
                job_id: String(record.id),
                status: newStatus,
                status_text: msgConfig.body
              },
              'homease_status',
              serviceAccount,
              fcmServerKey
            )
            results.push({ recipient: t.phone, token: t.fcm_token, status: res.success ? 'sent' : 'failed', error: res.error })
          }
        }
      }
    }

    return new Response(
      JSON.stringify({
        success: true,
        dispatched_count: results.filter(r => r.status === 'sent').length,
        total_targets: results.length,
        details: results
      }),
      {
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
        status: 200
      }
    )
  } catch (err: any) {
    console.error('Unhandled error in notify-job-event:', err)
    return new Response(
      JSON.stringify({ success: false, error: err.message || String(err) }),
      {
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
        status: 500
      }
    )
  }
})
