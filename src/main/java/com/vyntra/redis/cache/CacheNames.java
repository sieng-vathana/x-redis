package com.vyntra.redis.cache;

/**
 * Shared Redis cache region names used across Vyntra services.
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

    /** Shops listed by business id. */
    public static final String SHOPS_BY_BUSINESS = "shops-by-business";

    /** Single shop by id. */
    public static final String SHOP_BY_ID = "shop-by-id";

    private CacheNames() {
    }
}
