# HomEase: Multi-Agent Mass Simulation Prompts (Mirofish Compatible)

Use these two natural language simulation prompts alongside the attached specification file:
[`docs/HOMEASE_PRODUCT_VISION_AND_SIMULATION_SPEC.mdx`](./HOMEASE_PRODUCT_VISION_AND_SIMULATION_SPEC.mdx).

---

## 🎯 SIMULATION PROMPT 1: Mass Market Viability, Willingness-to-Pay (WTP) & Platform Leakage Stress-Test

```markdown
You are an advanced multi-agent behavioral simulation engine (Mirofish). 

### CONTEXT & INPUT:
Ingest and analyze the attached specification document: "HomEase: Product Vision, Technical Architecture, Critical Viability Analysis & Mass-Simulation Specification" (HOMEASE_PRODUCT_VISION_AND_SIMULATION_SPEC.mdx).

### SIMULATION OBJECTIVE:
Simulate a population of 1,000,000 synthetic agents in urban emerging markets (Karachi, Lahore, Rawalpindi/Islamabad) over a 12-month operational timeline to stress-test market viability, adoption friction, and platform leakage.

### AGENT POPULATION BREAKDOWN:
- 550,000 Urban Consumers (60% Middle-Class Renters/Apartment Dwellers [SEC B/C], 25% Upper-Middle/High-Income Homeowners [SEC A/B], 15% Low-Income Tenants [SEC C/D]).
- 400,000 Skilled Tradesmen (45% Electricians, 30% Plumbers, 15% AC/HVAC Mechanics, 10% Appliance/Carpenters). Vary their smartphone literacy from basic/illiterate to tech-savvy.
- 50,000 Neighborhood Contractors and Local Hardware Shop Owners.

### WHAT TO PREDICT & SIMULATE:
1. Two-Way Bidding Dynamics (The InDrive Model):
   - Do consumers prefer setting their own budget vs. fixed menu prices?
   - How many counter-offers does a customer typically receive within 3 minutes? What is the bid-acceptance rate?
   - Do technicians feel empowered or exploited by the counter-bidding mechanism compared to fixed-price apps (like Urban Company)?

2. Willingness-to-Pay (WTP) & Commission Elasticity:
   - Test provider reaction to 4 commission levels: 0%, 5%, 10%, and 20%.
   - At what exact commission percentage do providers begin colluding with customers to take work off-platform?
   - Will consumers pay an explicit platform booking fee, or does it trigger cart abandonment?

3. The Platform Disintermediation (Leakage) Dilemma:
   - Out of 100 first-time successful jobs, how many pairs exchange phone/WhatsApp numbers to bypass HomEase on job #2?
   - How effective are the built-in anti-leakage moats:
     * HomEase Shield (7-day re-inspection warranty)
     * Real-time 20-minute on-demand availability vs. waiting for one specific technician
     * Digital credit history for technician tool financing

4. Cash-on-Delivery (COD) Reconciliation:
   - What percentage of providers maintain a positive prepaid wallet balance (Rs 300–Rs 1,000) via Easypaisa/JazzCash to continue receiving job pings?
   - What is the default/drop-off rate when a wallet top-up is required?

### REQUIRED OUTPUT FORMAT:
1. Executive Verdict: Go / Pivot / No-Go summary with confidence score (0–100%).
2. Quantitative Metric Dashboard:
   - Day 30, Day 90, Day 360 Retention Rates (Consumer & Provider).
   - Average Customer Acquisition Cost (CAC) vs. Customer Lifetime Value (LTV).
   - Projected Disintermediation / Leakage Rate (%) across cohorts.
   - Critical Commission Tipping Point (Exact % where churn accelerates).
3. Synthetic Focus Group Verbatim Quotes:
   - 3 quotes from diverse consumers (e.g., upper-class female homemaker, budget renter, young professional).
   - 3 quotes from diverse tradesmen (e.g., veteran illiterate plumber, young tech-native AC tech, roadside daily-wager).
4. Top 3 Fatal Failure Modes & Prescribed Preventative Mitigations.
```

---

## 💰 SIMULATION PROMPT 2: Head-to-Head Revenue Model Battle & Monetization Optimization

```markdown
You are an advanced multi-agent market simulation engine (Mirofish).

### CONTEXT & INPUT:
Review the attached product draft "HOMEASE_PRODUCT_VISION_AND_SIMULATION_SPEC.mdx", specifically Section 4 detailing the "Five Comprehensive Revenue Models for HomEase":
1. Model 1: Micro-Take Rate (5%–7% deducted via Prepaid Wallet Float)
2. Model 2: Pay-Per-Bid / Lead Generation Token Model ("Connects", Rs 15–35 per bid or Rs 40–80 per won lead)
3. Model 3: Dual-Tiered Subscriptions (HomEase Pro for Tradesmen @ Rs 999/mo; HomEase Club for Homes @ Rs 499/mo)
4. Model 4: B2B Hardware & Materials Supply Chain Markup (8%–14% rebate on genuine parts)
5. Model 5: Embedded Worker Fintech (Tool financing BNPL, daily wage advance fees, micro-insurance)

### SIMULATION OBJECTIVE:
Run a head-to-head simulation across 1,000,000 agents in Pakistan/South Asian urban hubs over 24 simulated months. Determine which monetization model—or hybrid combination—maximizes Net Revenue while minimizing User Churn and Cash Leakage.

### CORE SIMULATION QUERIES:
1. Model Resistance & Adoption Friction:
   - Rank all 5 models from Lowest Provider Friction to Highest Provider Friction.
   - For Model 2 (Pay-Per-Bid), how do low-income plumbers react to spending Rs 20 upfront if they don't win the bid? Does this cause rage-quits?
   - For Model 3 (Subscriptions), what percentage of full-time technicians will pay Rs 999/month? What monthly earnings threshold must they hit before this subscription becomes a no-brainer?

2. B2B Materials Fulfillment Feasibility (Model 4):
   - In what percentage of home service jobs are replacement materials required?
   - Will customers trust in-app wholesale parts pricing, or do they still insist on sending the technician to the local market with cash?
   - What is the gross margin contribution of parts sales compared to pure labor fees?

3. Fintech Monetization Viability (Model 5):
   - What is the default rate on Tool Financing (e.g., Rs 15,000 hammer drill paid over 12 weeks via daily wage deductions)?
   - How many workers opt into the Rs 20/job micro-injury insurance?

4. The Optimal Hybrid Architecture:
   - If HomEase combines 2 or 3 models (e.g., Free Bids + 5% Micro-Take Rate + B2B Parts Markup), what is the optimal pricing configuration?

### REQUIRED OUTPUT FORMAT:
1. Revenue Model Comparison Matrix:
   - Table comparing all 5 models across: Projected 12-Mo Gross Revenue, Platform Margin (%), Provider Churn (%), Consumer Conversion (%), and Cash Leakage Vulnerability.
2. The Winning Monetization Strategy:
   - The recommended multi-phase revenue roadmap (Phase 1 Launch -> Phase 2 Scale -> Phase 3 Maturity).
   - Projected Monthly Recurring Revenue (MRR) and Average Revenue Per User (ARPU) at 100,000 Monthly Active Users (MAU).
3. Sensitivity & Elasticity Curves:
   - Price elasticity curve for lead fees (Rs 10 vs Rs 25 vs Rs 50 vs Rs 100).
   - Breakeven timeline in months based on realistic emerging-market marketing and verification costs.
4. Final Strategic Recommendation for Founders & Investors.
```
