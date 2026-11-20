package com.focuslite.core.service.detector

import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.focuslite.core.model.ShortFormPlatform

interface ReelsDetector {
    val platform: ShortFormPlatform
    val targetPackage: String
    val isValidated: Boolean
    fun isShortFormVisible(rootNode: AccessibilityNodeInfo): Boolean
    fun performDismissAction(rootNode: AccessibilityNodeInfo): Boolean
}

/**
 * Validated detector for Instagram Reels (com.instagram.android).
 * Detects clips viewer pager, active reels tab, and reel video containers.
 */
class InstagramReelsDetector : ReelsDetector {
    override val platform: ShortFormPlatform = ShortFormPlatform.INSTAGRAM
    override val targetPackage: String = ShortFormPlatform.INSTAGRAM.defaultPackageName
    override val isValidated: Boolean = true

    companion object {
        private const val TAG = "FocusLite_A11y"
        private val REELS_VIEW_IDS = listOf(
            "com.instagram.android:id/clips_viewer_view_pager",
            "com.instagram.android:id/clips_video_container",
            "com.instagram.android:id/reel_viewer_title",
            "com.instagram.android:id/clips_media_item_container"
        )
        private val REELS_KEYWORDS = listOf("reels", "reel", "clips_viewer")
    }

    override fun isShortFormVisible(rootNode: AccessibilityNodeInfo): Boolean {
        return runCatching {
            // Fast check: look for known reels view IDs directly
            for (viewId in REELS_VIEW_IDS) {
                val nodes = rootNode.findAccessibilityNodeInfosByViewId(viewId)
                if (!nodes.isNullOrEmpty()) {
                    Log.d(TAG, "[Instagram] Detected Reels viewId: $viewId")
                    return true
                }
            }

            // Fallback: bounded tree search
            searchNode(rootNode, depth = 0, maxDepth = 6, maxBreadth = 15)
        }.getOrDefault(false)
    }

    private fun searchNode(node: AccessibilityNodeInfo?, depth: Int, maxDepth: Int, maxBreadth: Int): Boolean {
        if (node == null || depth > maxDepth) return false

        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        val text = node.text?.toString()?.lowercase() ?: ""
        val viewId = node.viewIdResourceName?.lowercase() ?: ""

        if (REELS_KEYWORDS.any { k -> desc.contains(k) || text.contains(k) || viewId.contains(k) }) {
            if (node.isSelected || viewId.contains("clips") || viewId.contains("reel")) {
                return true
            }
        }

        val childCount = minOf(node.childCount, maxBreadth)
        for (i in 0 until childCount) {
            val child = node.getChild(i)
            if (child != null) {
                val found = searchNode(child, depth + 1, maxDepth, maxBreadth)
                if (found) return true
            }
        }
        return false
    }

    override fun performDismissAction(rootNode: AccessibilityNodeInfo): Boolean {
        return runCatching {
            // Attempt to navigate back to Home Feed tab
            val homeNodes = rootNode.findAccessibilityNodeInfosByViewId("com.instagram.android:id/feed_tab")
            if (!homeNodes.isNullOrEmpty()) {
                val homeButton = homeNodes[0]
                val clicked = homeButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                Log.d(TAG, "[Instagram] Clicked Home tab: $clicked")
                if (clicked) return true
            }
            false
        }.getOrDefault(false)
    }
}

/**
 * Validated detector for YouTube Shorts (com.google.android.youtube).
 * Detects reel watch fragments, shorts button selection, and reel player overlays.
 */
class YouTubeShortsDetector : ReelsDetector {
    override val platform: ShortFormPlatform = ShortFormPlatform.YOUTUBE
    override val targetPackage: String = ShortFormPlatform.YOUTUBE.defaultPackageName
    override val isValidated: Boolean = true

    companion object {
        private const val TAG = "FocusLite_A11y"
        private val SHORTS_VIEW_IDS = listOf(
            "com.google.android.youtube:id/reel_watch_fragment",
            "com.google.android.youtube:id/reel_player_overlay",
            "com.google.android.youtube:id/shorts_container",
            "com.google.android.youtube:id/reel_recycler"
        )
    }

    override fun isShortFormVisible(rootNode: AccessibilityNodeInfo): Boolean {
        return runCatching {
            // Fast check: look for known Shorts view IDs
            for (viewId in SHORTS_VIEW_IDS) {
                val nodes = rootNode.findAccessibilityNodeInfosByViewId(viewId)
                if (!nodes.isNullOrEmpty()) {
                    Log.d(TAG, "[YouTube] Detected Shorts viewId: $viewId")
                    return true
                }
            }

            // Check if Shorts pivot tab is selected
            val pivotNodes = rootNode.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/pivot_bar")
            if (!pivotNodes.isNullOrEmpty()) {
                val pivotBar = pivotNodes[0]
                for (i in 0 until pivotBar.childCount) {
                    val tab = pivotBar.getChild(i) ?: continue
                    val desc = tab.contentDescription?.toString()?.lowercase() ?: ""
                    if (desc.contains("shorts") && tab.isSelected) {
                        Log.d(TAG, "[YouTube] Shorts tab is selected on pivot bar")
                        return true
                    }
                }
            }

            searchNode(rootNode, depth = 0, maxDepth = 6, maxBreadth = 15)
        }.getOrDefault(false)
    }

    private fun searchNode(node: AccessibilityNodeInfo?, depth: Int, maxDepth: Int, maxBreadth: Int): Boolean {
        if (node == null || depth > maxDepth) return false

        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        val viewId = node.viewIdResourceName?.lowercase() ?: ""

        if (desc.contains("shorts") && (node.isSelected || viewId.contains("shorts_button"))) {
            return true
        }
        if (viewId.contains("reel_watch") || viewId.contains("reel_player")) {
            return true
        }

        val childCount = minOf(node.childCount, maxBreadth)
        for (i in 0 until childCount) {
            val child = node.getChild(i)
            if (child != null) {
                val found = searchNode(child, depth + 1, maxDepth, maxBreadth)
                if (found) return true
            }
        }
        return false
    }

    override fun performDismissAction(rootNode: AccessibilityNodeInfo): Boolean {
        return runCatching {
            // Attempt to click Home tab (index 0 of pivot bar)
            val pivotNodes = rootNode.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/pivot_bar")
            if (!pivotNodes.isNullOrEmpty()) {
                val pivotBar = pivotNodes[0]
                if (pivotBar.childCount > 0) {
                    val homeTab = pivotBar.getChild(0)
                    val clicked = homeTab?.performAction(AccessibilityNodeInfo.ACTION_CLICK) ?: false
                    Log.d(TAG, "[YouTube] Clicked Home tab on pivot bar: $clicked")
                    if (clicked) return true
                }
            }
            false
        }.getOrDefault(false)
    }
}

/**
 * Facebook Reels Detector (Experimental / Unvalidated).
 * Gated by isValidated = false until live node hierarchy validation.
 */
class FacebookReelsDetector : ReelsDetector {
    override val platform: ShortFormPlatform = ShortFormPlatform.FACEBOOK
    override val targetPackage: String = ShortFormPlatform.FACEBOOK.defaultPackageName
    override val isValidated: Boolean = false

    override fun isShortFormVisible(rootNode: AccessibilityNodeInfo): Boolean {
        return runCatching {
            val desc = rootNode.contentDescription?.toString()?.lowercase() ?: ""
            val text = rootNode.text?.toString()?.lowercase() ?: ""
            desc.contains("reels") || text.contains("reels")
        }.getOrDefault(false)
    }

    override fun performDismissAction(rootNode: AccessibilityNodeInfo): Boolean = false
}

/**
 * Snapchat Spotlight Detector (Experimental / Unvalidated).
 * Gated by isValidated = false until live node hierarchy validation.
 */
class SnapchatSpotlightDetector : ReelsDetector {
    override val platform: ShortFormPlatform = ShortFormPlatform.SNAPCHAT
    override val targetPackage: String = ShortFormPlatform.SNAPCHAT.defaultPackageName
    override val isValidated: Boolean = false

    override fun isShortFormVisible(rootNode: AccessibilityNodeInfo): Boolean {
        return runCatching {
            val nodes = rootNode.findAccessibilityNodeInfosByViewId("com.snapchat.android:id/spotlight_tab")
            !nodes.isNullOrEmpty() && nodes[0].isSelected
        }.getOrDefault(false)
    }

    override fun performDismissAction(rootNode: AccessibilityNodeInfo): Boolean = false
}
