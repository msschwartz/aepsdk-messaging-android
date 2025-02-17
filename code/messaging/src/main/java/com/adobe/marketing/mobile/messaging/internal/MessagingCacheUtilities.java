/*
  Copyright 2021 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.messaging.internal;

import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.IMAGES_CACHE_SUBDIRECTORY;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.LOG_TAG;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.PROPOSITIONS_CACHE_SUBDIRECTORY;

import androidx.annotation.VisibleForTesting;

import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.services.caching.CacheEntry;
import com.adobe.marketing.mobile.services.caching.CacheExpiry;
import com.adobe.marketing.mobile.services.caching.CacheResult;
import com.adobe.marketing.mobile.services.caching.CacheService;
import com.adobe.marketing.mobile.util.StringUtils;
import com.adobe.marketing.mobile.util.UrlUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class contains functionality to cache the json message payload and any image asset URL's present in an
 * AJO in-app message definition.
 */
final class MessagingCacheUtilities {
    private final static String SELF_TAG = "MessagingCacheUtilities";
    private final CacheService cacheService;
    private final String assetCacheLocation;
    private final String METADATA_KEY_PATH_TO_FILE = "pathToFile";
    private final Map<String, String> assetMap = new HashMap<>();

    public MessagingCacheUtilities() {
        this.cacheService = ServiceProvider.getInstance().getCacheService();
        this.assetCacheLocation = MessagingUtils.getAssetCacheLocation();
    }
    // ========================================================================================================
    // Message payload caching
    // ========================================================================================================

    /**
     * Determines if propositions have been previously cached.
     *
     * @return {@code boolean} containing true if cached propositions are found, false otherwise.
     */
    boolean arePropositionsCached() {
        return cacheService.get(MessagingConstants.CACHE_BASE_DIR, PROPOSITIONS_CACHE_SUBDIRECTORY) != null;
    }

    /**
     * Delete all contents in the Messaging extension cache subdirectory.
     */
    @VisibleForTesting
    void clearCachedData() {
        cacheService.remove(MessagingConstants.CACHE_BASE_DIR, PROPOSITIONS_CACHE_SUBDIRECTORY);
        cacheService.remove(MessagingConstants.CACHE_BASE_DIR, IMAGES_CACHE_SUBDIRECTORY);
        Log.trace(LOG_TAG, SELF_TAG, "In-app messaging %s and %s caches have been deleted.", PROPOSITIONS_CACHE_SUBDIRECTORY, IMAGES_CACHE_SUBDIRECTORY);
    }

    /**
     * Retrieves cached {@code String} proposition payloads and returns them in a {@link List<PropositionPayload>}.
     *
     * @return a {@code List<PropositionPayload>} containing the cached proposition payloads.
     */
    List<PropositionPayload> getCachedPropositions() {
        final CacheResult cacheResult = cacheService.get(MessagingConstants.CACHE_BASE_DIR, PROPOSITIONS_CACHE_SUBDIRECTORY);
        if (cacheResult == null) {
            Log.trace(LOG_TAG, SELF_TAG, "Unable to find a cached proposition.");
            return null;
        }

        final Map<String, String> fileMetadata = cacheResult.getMetadata();
        if (fileMetadata != null && !fileMetadata.isEmpty()) {
            Log.trace(LOG_TAG, SELF_TAG, "Loading cached proposition from (%s)", fileMetadata.get(METADATA_KEY_PATH_TO_FILE));
        }
        ObjectInputStream objectInputStream = null;
        List<PropositionPayload> cachedPropositions;
        try {
            objectInputStream = new ObjectInputStream(cacheResult.getData());
            cachedPropositions = (List<PropositionPayload>) objectInputStream.readObject();
        } catch (final NullPointerException nullPointerException) {
            Log.warning(LOG_TAG, SELF_TAG, "Exception occurred when retrieving the cached proposition file: %s", nullPointerException.getMessage());
            return null;
        } catch (final IOException ioException) {
            Log.warning(LOG_TAG, SELF_TAG, "Exception occurred when reading from the cached file: %s", ioException.getMessage());
            return null;
        } catch (final ClassNotFoundException classNotFoundException) {
            Log.warning(LOG_TAG, SELF_TAG, "Class not found: %s", classNotFoundException.getMessage());
            return null;
        } finally {
            try {
                if (objectInputStream != null) {
                    objectInputStream.close();
                }
            } catch (final IOException ioException) {
                Log.warning(LOG_TAG, SELF_TAG, "Exception occurred when closing the FileInputStream: %s", ioException.getMessage());
            }
        }
        return cachedPropositions;
    }

    /**
     * Caches the {@code List<PropositionPayload>} payload.
     *
     * @param propositionPayload the {@link List<PropositionPayload>} containing the message payload to be cached.
     */
    void cachePropositions(final List<PropositionPayload> propositionPayload) {
        // clean any existing cached propositions first if propositionPayload is null or empty
        if (propositionPayload == null || propositionPayload.isEmpty()) {
            cacheService.remove(MessagingConstants.CACHE_BASE_DIR, PROPOSITIONS_CACHE_SUBDIRECTORY);
            Log.trace(MessagingConstants.LOG_TAG, SELF_TAG, "In-app messaging cache has been deleted.");
            return;
        }

        Log.debug(LOG_TAG, SELF_TAG, "Creating new cached propositions");
        ByteArrayOutputStream byteArrayOutputStream = null;
        ObjectOutputStream objectOutputStream = null;
        InputStream inputStream = null;

        try {
            byteArrayOutputStream = new ByteArrayOutputStream();
            objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(propositionPayload);
            objectOutputStream.flush();
            inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
            final CacheEntry cacheEntry = new CacheEntry(inputStream, CacheExpiry.never(), null);
            cacheService.set(MessagingConstants.CACHE_BASE_DIR, PROPOSITIONS_CACHE_SUBDIRECTORY, cacheEntry);
        } catch (final IOException e) {
            Log.warning(LOG_TAG, SELF_TAG, "IOException while attempting to write remote file (%s)", e);
        } finally {
            try {
                if (objectOutputStream != null) {
                    objectOutputStream.close();
                }
                if (byteArrayOutputStream != null) {
                    byteArrayOutputStream.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (final IOException e) {
                Log.warning(LOG_TAG, SELF_TAG, "Unable to close the ObjectOutputStream (%s) ", e);
            }
        }
    }

    // ========================================================================================================
    // Image asset caching
    // ========================================================================================================

    /**
     * Caches the assets provided in the {@link java.util.List}.
     *
     * @param assetsUrls a {@link List<String>} containing asset URL's to be cached.
     */
    void cacheImageAssets(final List<String> assetsUrls) {
        if (StringUtils.isNullOrEmpty(assetCacheLocation)) {
            Log.debug(LOG_TAG, SELF_TAG, "Failed to cache asset, the asset cache location is not available.");
            return;
        }

        if (cacheService == null) {
            Log.trace(LOG_TAG, SELF_TAG, "Failed to cache asset, the cache manager is not available.");
            return;
        }

        final List<String> assetsToRetain = new ArrayList<>();

        // validate asset URLs and remove duplicates
        if (assetsUrls != null && !assetsUrls.isEmpty()) {
            for (final String imageAssetUrl : assetsUrls) {
                if (assetIsDownloadable(imageAssetUrl) && !assetsToRetain.contains(imageAssetUrl)) {
                    assetsToRetain.add(imageAssetUrl);
                    // update the asset to cached location map
                    assetMap.put(imageAssetUrl, assetCacheLocation);
                }
            }
        }

        // download the assets
        MessageAssetDownloader messageAssetDownloader = new MessageAssetDownloader(assetsToRetain);
        messageAssetDownloader.downloadAssetCollection();
    }

    /**
     * Determine whether the provided {@code String} asset is downloadable.
     * <p>
     * Checks that the provided asset is both a valid URL, and has a scheme of "http" or "https".
     *
     * @param asset {@link String} containing the asset path to check
     * @return {@code boolean} indicating whether the provided asset is downloadable
     */
    private boolean assetIsDownloadable(final String asset) {
        return UrlUtils.isValidUrl(asset) && (asset.startsWith("http") || asset.startsWith("https"));
    }

    /**
     * Returns a {@link Map<String, String>} containing the remote asset mapped to it's cached location.
     *
     * @return {@code Map<String, String} containing a mapping of a remote image asset URL and it's cached location
     */
    Map<String, String> getAssetsMap() {
        return assetMap;
    }
}