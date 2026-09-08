import { supabase } from "@/lib/supabase";

export interface CategoryDetectionResult {
  success: boolean;
  category?: string;
  serviceNote?: string;
  confidence?: "high" | "medium" | "low";
  error?: string;
}

export const detectCategory = async (description: string): Promise<CategoryDetectionResult> => {
  const { data, error } = await supabase.functions.invoke("detect-category", { body: { description } });
  if (error || !data?.success) {
    return { success: false, error: data?.error || error?.message || "Failed to detect category" };
  }
  return {
    success: true,
    category: data.category,
    serviceNote: data.service_note,
    confidence: data.confidence,
  };
};
