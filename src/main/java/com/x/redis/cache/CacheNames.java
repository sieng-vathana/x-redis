package com.x.redis.cache;

/**
 * Shared Redis cache region names used across X services.
 * Keep keys stable — renaming invalidates all existing entries.
 */
public final class CacheNames {

    /** Full product list (short TTL). */
    public static final String PRODUCTS = "products";

    /** Single product by id. */
    public static final String PRODUCT_BY_ID = "product-by-id";

    /** Auth/user lookup by username (for BFF / user-service). */
    public static final String USER_BY_USERNAME = "user-by-username";

    /** Business by id. */
    public static final String BUSINESS_BY_ID = "business-by-id";

    /** Businesses listed by owner user id. */
    public static final String BUSINESSES_BY_OWNER = "businesses-by-owner";

    /** Stores listed by business id. */
    public static final String STORES_BY_BUSINESS = "stores-by-business";

    /** Single store by id. */
    public static final String STORE_BY_ID = "store-by-id";

    /** Storage metadata (including the resolved URL) by relative object path. */
    public static final String STORAGE_FILE_BY_RELATIVE_PATH = "storage-file-by-relative-path";

    /** Authenticated marketplace favorites and cart summary by user id. */
    public static final String MARKETPLACE_USER_SUMMARY = "marketplace-user-summary";

    private CacheNames() {
    }
}
