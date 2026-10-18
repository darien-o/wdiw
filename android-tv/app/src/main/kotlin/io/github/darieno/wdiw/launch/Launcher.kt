package io.github.darieno.wdiw.launch

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import io.github.darieno.wdiw.data.Registry
import io.github.darieno.wdiw.domain.OfferPlan
import io.github.darieno.wdiw.domain.OpenStep

class Launcher(private val context: Context) {
    private val packageManager: PackageManager get() = context.packageManager

    /** The registry's packages that are installed on this device. */
    fun installedPackages(registry: Registry): Set<String> =
        registry.providers.flatMap { it.packages }.filterTo(mutableSetOf()) { isInstalled(it) }

    private fun isInstalled(packageName: String): Boolean =
        try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }

    /** Tries the plan's steps in order and returns the one that started, or null when none did. */
    fun open(plan: OfferPlan): OpenStep? = plan.steps.firstOrNull { start(it) }

    private fun start(step: OpenStep): Boolean {
        val intent = when (step) {
            is OpenStep.TitleLink -> viewIntent(step.packageName, step.uri, step.extras)
            is OpenStep.SearchLink -> viewIntent(step.packageName, step.uri, step.extras)
            is OpenStep.Launch ->
                packageManager.getLeanbackLaunchIntentForPackage(step.packageName)
                    ?: packageManager.getLaunchIntentForPackage(step.packageName)
        } ?: return false
        return try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (_: RuntimeException) {
            // ActivityNotFoundException when the app does not handle the link, SecurityException when
            // the activity is not exported. Either way the next step is tried.
            false
        }
    }

    private fun viewIntent(packageName: String, uri: String, extras: Map<String, String>): Intent =
        Intent(Intent.ACTION_VIEW, Uri.parse(uri)).setPackage(packageName).apply {
            extras.forEach { (key, value) -> putExtra(key, value) }
        }
}
