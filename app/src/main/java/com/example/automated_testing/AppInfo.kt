package com.example.automated_testing

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.util.Log
import androidx.webkit.WebViewCompat
import java.io.File
import kotlin.math.pow

class AppInfo(private val context: Context,private val activityManager: ActivityManager) {

    private fun getAndroidVersion(): Information{
        return Information("Android Version",Build.VERSION.RELEASE)
    }

    private fun getSDKVersion(): Information{
        return Information("SDK Version",Build.VERSION.SDK_INT.toString())
    }

    private fun getWebviewVersion(): Information{
        try {
            val webviewPackageInfo = WebViewCompat.getCurrentWebViewPackage(context)
            if(webviewPackageInfo!=null)
                return Information("Webview Version",webviewPackageInfo.versionCode.toString())
            return Information("Webview Version","Error")
        }
        catch (e:Exception){
            Log.e("AT",e.toString())
            return Information("Webview Version","")
        }
    }

    private fun getBrand(): Information{
        return Information("Brand",Build.BRAND)
    }

    private fun getModel(): Information{
        return Information("Model",Build.MODEL)
    }

    private fun getManufacturer(): Information{
        return Information("Manufacturer",Build.MANUFACTURER)
    }

    private fun getRootAccess(): Information{
        val isRooted = findBinary("su")
        return Information("Root Access",if (isRooted) "YES" else "NO")
    }

    private fun findBinary(binaryName: String): Boolean{
        val paths = System.getenv("PATH")?.split(":")?: emptyList()
        return  paths.any{File(it,binaryName).exists()}
    }

    fun getSoftwareInfo():List<Information>{
        return listOf(
            getAndroidVersion(),
            getSDKVersion(),
            getWebviewVersion(),
            getBrand(),
            getModel(),
            getManufacturer(),
            getRootAccess()
        )
    }

    private fun getTotalInternalStorage(): Information{
        val internalStorage = context.filesDir.absolutePath
        val internalStats = StatFs(internalStorage)
        val internalBlockSize = internalStats.blockSizeLong
        val internalTotalBlocks = internalStats.blockCountLong

        val totalInternalStorage= internalBlockSize* internalTotalBlocks /(1024.0.pow(3.0))
        return Information("Total Internal Storage",totalInternalStorage.toString())
    }

    private fun getAvailableInternalStorage(): Information{
        val internalStorage = context.filesDir.absolutePath
        val internalStats = StatFs(internalStorage)
        val internalBlockSize = internalStats.blockSizeLong
        val internalAvailableBlocks =internalStats.availableBlocksLong

        val availableInternalStorage = internalBlockSize*internalAvailableBlocks/(1024.0.pow(3.0))
        return Information("Available Internal Storage",availableInternalStorage.toString())
    }

    private fun getTotalExternalStorage(): Information{
        val externalStorage = Environment.getExternalStorageDirectory().absolutePath
        val externalStat = StatFs(externalStorage)
        val externalBlockSize = externalStat.blockSizeLong
        val externalTotalBlocks = externalStat.blockCountLong

        val totalExternalStorage = externalBlockSize*externalTotalBlocks/(1024.0.pow(3.0))
        return Information("Total External Storage",totalExternalStorage.toString())
    }

    private fun getAvailableExternalStorage(): Information{
        val externalStorage = Environment.getExternalStorageDirectory().absolutePath
        val externalStat = StatFs(externalStorage)
        val externalBlockSize = externalStat.blockSizeLong
        val externalAvailableBlocks = externalStat.availableBlocksLong

        val availableExternalStorage = externalBlockSize*externalAvailableBlocks/(1024.0.pow(3.0))
        return Information("Available External Storage",availableExternalStorage.toString())
    }

    private fun getTotalRAM():Information{
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return Information("Total RAM",convertBytesToGB(memoryInfo.totalMem))
    }

    private fun getAvailableRAM():Information{
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return Information("Available RAM",convertBytesToGB(memoryInfo.availMem))
    }
    fun getHardwareInfo(): List<Information>{
        return listOf(
            getTotalInternalStorage(),
            getAvailableInternalStorage(),
            getTotalExternalStorage(),
            getAvailableExternalStorage(),
            getTotalRAM(),
            getAvailableRAM()
        )
    }

    private  fun convertBytesToGB(value: Long): String{
        return (value/(1024.0.pow(3.0))).toString()
    }
}

data class Information(val name: String, val value:String)