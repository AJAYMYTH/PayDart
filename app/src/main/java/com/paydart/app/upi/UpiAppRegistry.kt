package com.paydart.app.upi

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build

data class InstalledUpiApp(
    val packageName: String,
    val appName: String,
    val icon: Drawable? = null
)

object UpiAppRegistry {

    val KNOWN_UPI_PACKAGES = mapOf(
        "com.google.android.apps.nbu.paisa.user" to "Google Pay",
        "com.phonepe.app" to "PhonePe",
        "net.one97.paytm" to "Paytm",
        "in.org.npci.upiapp" to "BHIM",
        "com.dreamplug.androidapp" to "CRED",
        "in.amazon.mShop.android.shopping" to "Amazon Pay",
        "com.naviapp" to "Navi",
        "com.whatsapp" to "WhatsApp",
        "com.myairtelapp" to "Airtel Thanks",
        "com.mobikwik_new" to "MobiKwik",
        "com.jupiter.money" to "Jupiter"
    )

    /**
     * Checks if a specific package is installed on the device.
     */
    fun isAppInstalled(pm: PackageManager, packageName: String): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(packageName, 0)
            }
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * Queries all installed applications that can handle a UPI payment Intent.
     */
    fun getInstalledUpiApps(context: Context): List<InstalledUpiApp> {
        val pm = context.packageManager
        val upiIntent = Intent(Intent.ACTION_VIEW, Uri.parse("upi://pay"))
        val resolveFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PackageManager.MATCH_ALL
        } else {
            0
        }

        val resolveInfos = pm.queryIntentActivities(upiIntent, resolveFlags)
        val discoveredPackages = mutableSetOf<String>()
        val result = mutableListOf<InstalledUpiApp>()

        for (resolveInfo in resolveInfos) {
            val pkg = resolveInfo.activityInfo.packageName
            if (pkg != context.packageName && discoveredPackages.add(pkg)) {
                val label = resolveInfo.loadLabel(pm).toString()
                val icon = resolveInfo.loadIcon(pm)
                val friendlyName = KNOWN_UPI_PACKAGES[pkg] ?: label
                result.add(InstalledUpiApp(pkg, friendlyName, icon))
            }
        }

        // Also check known packages in case package manager query missed any on specific OEM skins
        for ((pkg, friendlyName) in KNOWN_UPI_PACKAGES) {
            if (discoveredPackages.add(pkg) && isAppInstalled(pm, pkg)) {
                val appInfo = try {
                    pm.getApplicationInfo(pkg, 0)
                } catch (e: Exception) {
                    null
                }
                val icon = appInfo?.let { pm.getApplicationIcon(it) }
                val label = appInfo?.let { pm.getApplicationLabel(it).toString() } ?: friendlyName
                result.add(InstalledUpiApp(pkg, label, icon))
            }
        }

        return result.sortedBy { it.appName.lowercase() }
    }

    /**
     * Returns a human-friendly name for a given UPI package name.
     */
    fun getAppName(context: Context, packageName: String?): String {
        if (packageName.isNullOrBlank()) return "System Chooser"
        KNOWN_UPI_PACKAGES[packageName]?.let { return it }

        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }
}
