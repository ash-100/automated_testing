import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
import android.util.Log
import androidx.core.content.getSystemService

class NetworkUsageStats(private val context: Context) {

    private val networkStatsManager = context.getSystemService<NetworkStatsManager>()
    private val packageManager = context.packageManager

    fun getNetworkUsageForApps(packageNames: List<String>, startTime: Long, endTime: Long): Map<String, Pair<Long, Long>> {
        val result = mutableMapOf<String, Pair<Long, Long>>()

        packageNames.forEach { packageName ->
            val uid = getUidForPackage(packageName)
            if (uid != -1) {
                val usage = getNetworkUsageForUid(uid, startTime, endTime)
                result[packageName] = usage
            }
        }

        return result
    }

    private fun getUidForPackage(packageName: String): Int {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                packageManager.getPackageUid(packageName, 0)
            } else {
                @Suppress("DEPRECATION")
                packageManager.getApplicationInfo(packageName, 0).uid
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Log.d("AT","UID error: $e")
            e.printStackTrace()
            -1
        }
    }

    private fun getNetworkUsageForUid(uid: Int, startTime: Long, endTime: Long): Pair<Long, Long> {
        var rxBytes = 0L
        var txBytes = 0L

        try {
            val mobileStats = networkStatsManager?.queryDetailsForUid(
                ConnectivityManager.TYPE_MOBILE,
                null, // Subscriber ID, use null for all subscribers
                startTime,
                endTime,
                uid
            )

            val wifiStats = networkStatsManager?.queryDetailsForUid(
                ConnectivityManager.TYPE_WIFI,
                "",
                startTime,
                endTime,
                uid
            )

            val bucket = NetworkStats.Bucket()

            while (mobileStats?.hasNextBucket() == true) {
                mobileStats.getNextBucket(bucket)
                rxBytes += bucket.rxBytes
                txBytes += bucket.txBytes
            }

            while (wifiStats?.hasNextBucket() == true) {
                wifiStats.getNextBucket(bucket)
                rxBytes += bucket.rxBytes
                txBytes += bucket.txBytes
            }

            mobileStats?.close()
            wifiStats?.close()
        } catch (e: SecurityException) {
            // Handle the case where we don't have necessary permissions
            e.printStackTrace()
        }

        return Pair(rxBytes, txBytes)
    }
}

