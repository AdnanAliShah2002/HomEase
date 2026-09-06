import { createClient } from "@supabase/supabase-js";

export const SUPABASE_URL = "https://nsqrfagylbqrwlbvnsug.supabase.co";
export const SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im5zcXJmYWd5bGJxcndsYnZuc3VnIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzI5NTY4NTIsImV4cCI6MjA4ODUzMjg1Mn0.qNnOe8xR6ehutINqCGVK7tfHLBh14tBWgbhFeHvFa40";

/**
 * Supabase client initialized strictly with the anon key.
 * Never includes or uses the service role key in client-side code.
 * Direct table queries and operations use the logged-in user's authenticated session.
 */
export const supabase = createClient(SUPABASE_URL, SUPABASE_ANON_KEY, {
  auth: {
    persistSession: true,
    autoRefreshToken: true,
    detectSessionInUrl: false,
  },
});

/**
 * Public provider profiles view query helper
 * Safely fetches approved providers without revealing sensitive CNIC or private data
 */
export async function getPublicProviderProfiles() {
  const { data, error } = await supabase
    .from("public_provider_profiles")
    .select("*");
  if (error) throw error;
  return data;
}
