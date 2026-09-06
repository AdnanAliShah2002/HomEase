import { supabase } from "@/lib/supabase";

export const sendVerificationOTP = async (phone: string): Promise<{ success: boolean; error?: string }> => {
  const { data, error } = await supabase.functions.invoke("send-otp", { body: { phone } });
  if (error || !data?.success) {
    return { success: false, error: data?.error || error?.message || "Failed to send code" };
  }
  return { success: true };
};

export const verifyOTP = async (phone: string, code: string): Promise<{ success: boolean; isNewUser?: boolean; userId?: string; error?: string }> => {
  const { data, error } = await supabase.functions.invoke("verify-otp", { body: { phone, code } });
  if (error || !data?.success) {
    return { success: false, error: data?.error || error?.message || "Verification failed" };
  }
  return { success: true, isNewUser: data.isNewUser, userId: data.userId };
};
