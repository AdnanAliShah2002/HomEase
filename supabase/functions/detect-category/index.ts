import { serve } from "https://deno.land/std@0.168.0/http/server.ts"

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

const CATEGORIES = [
  "Laundry & Ironing", "Home Cleaning", "AC Servicing",
  "Car Care/Wash", "Plumbing", "Electrical", "Appliance Repair"
];

serve(async (req) => {
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }

  try {
    const { description } = await req.json()
    const GEMINI_API_KEY = Deno.env.get('GEMINI_API_KEY')
    if (!GEMINI_API_KEY) throw new Error("Missing GEMINI_API_KEY")

    const prompt = `You are a classifier for a home services app. Given a customer's problem description, respond with ONLY a JSON object, no other text, in this exact shape:
{"category": "<one of: ${CATEGORIES.join(", ")}>", "service_note": "<a short 5-10 word summary of the likely specific issue>", "confidence": "<high|medium|low>"}

Customer description: "${description}"`;

    const response = await fetch(
      `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${GEMINI_API_KEY}`,
      {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          contents: [{ parts: [{ text: prompt }] }]
        })
      }
    )

    const data = await response.json()
    const rawText = data.candidates?.[0]?.content?.parts?.[0]?.text ?? "{}"
    const cleaned = rawText.replace(/```json|```/g, "").trim()
    const parsed = JSON.parse(cleaned)

    // safety check: make sure the returned category is actually one of ours
    if (!CATEGORIES.includes(parsed.category)) {
      return new Response(
        JSON.stringify({ success: false, error: "Could not confidently classify" }),
        { headers: { ...corsHeaders, 'Content-Type': 'application/json' }, status: 200 }
      )
    }

    return new Response(
      JSON.stringify({ success: true, ...parsed }),
      { headers: { ...corsHeaders, 'Content-Type': 'application/json' }, status: 200 }
    )
  } catch (error) {
    return new Response(
      JSON.stringify({ success: false, error: error.message }),
      { headers: { ...corsHeaders, 'Content-Type': 'application/json' }, status: 400 }
    )
  }
})
