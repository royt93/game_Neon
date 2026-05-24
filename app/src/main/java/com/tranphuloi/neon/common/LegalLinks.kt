package com.tranphuloi.neon.common

/**
 * Round 76 (R76a) — Single source of truth cho legal URLs (privacy / terms /
 * disclaimer). Trước đây hardcode "https://example.com/privacy" trong
 * DialogSettings — placeholder dev-time leak.
 *
 * Notion page tổng hợp Term + Privacy Policy + Disclaimer trong 1 trang
 * (user provided: loitp.notion.site, page id 319b1cd8...).
 */
object LegalLinks {
    /**
     * Privacy policy + terms + disclaimer (Notion page).
     * Used by Settings → "Riêng tư" link.
     */
    const val PRIVACY_POLICY_URL =
        "https://loitp.notion.site/Term-Privacy-Policy-Disclaimer-319b1cd8783942fa8923d2a3c9bce60f"

    /** Same Notion page — Term of Service section anchored. */
    const val TERMS_URL = PRIVACY_POLICY_URL

    /** Same Notion page — Disclaimer section anchored. */
    const val DISCLAIMER_URL = PRIVACY_POLICY_URL
}
