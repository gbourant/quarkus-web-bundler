package io.quarkiverse.web.bundler.deployment.items;

import static java.util.Objects.requireNonNull;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A web asset generated at build time by another extension (e.g. compiled from Java), added to an entry point
 * alongside the scanned assets. It follows the same rules as scanned assets: auto-imported when the entry point
 * has no index file, otherwise it must be imported manually (e.g. {@code import { foo } from "./<scopedPath>";}).
 */
public final class GeneratedBundleWebAssetBuildItem extends MultiBuildItem {

    private final String entryPointKey;
    private final String scopedPath;
    private final byte[] content;

    public GeneratedBundleWebAssetBuildItem(String entryPointKey, String scopedPath, byte[] content) {
        this.entryPointKey = requireNonNull(entryPointKey, "entryPointKey is required");
        this.scopedPath = requireNonNull(scopedPath, "scopedPath is required");
        this.content = requireNonNull(content, "content is required");
    }

    public String entryPointKey() {
        return entryPointKey;
    }

    /**
     * The path relative to the entry point directory
     */
    public String scopedPath() {
        return scopedPath;
    }

    public byte[] content() {
        return content;
    }
}
