package com.example.automated_testing

import NetworkUsageStats
import android.app.Activity
import android.app.ActivityManager
import android.app.AlertDialog
import android.app.AppOpsManager
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.automated_testing.ui.theme.AutomatedtestingTheme
import org.acra.BuildConfig
import org.acra.config.httpSender
import org.acra.data.StringFormat
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import java.util.concurrent.TimeUnit
import kotlin.math.pow
import org.acra.ACRA
import org.acra.config.CoreConfigurationBuilder
import org.acra.config.HttpSenderConfiguration
import org.acra.config.HttpSenderConfigurationBuilder
import org.acra.config.LimiterConfigurationBuilder
import org.acra.ktx.initAcra

class MainActivity : ComponentActivity() {
    var internetStatus = mutableStateOf(false)
    var mqttConnectionStatus = mutableStateOf(false)

    private lateinit var mqttClient: MqttAndroidClient

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase)
//        ACRA.init(application, CoreConfigurationBuilder()
//            .withBuildConfigClass(BuildConfig::class.java)
//            .withReportFormat(StringFormat.JSON)
//            .withPluginConfigurations(
//                HttpSenderConfigurationBuilder()
//                    .withUri("http://192.168.1.8:5000/report")
//                    .build(),
//                LimiterConfigurationBuilder()
//                    .withPeriod(5*1000)
//                    .withExceptionClassLimit(5)
//                    .build()
//            )
//        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val appInfo =AppInfo( applicationContext, activityManager)

        setContent {

            AutomatedtestingTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    println(innerPadding)
                    Column(horizontalAlignment =Alignment.CenterHorizontally, modifier = Modifier.padding(top = 25.dp)) {
                        Text(text = "Software Info")
                        displayInfo(infos = appInfo.getSoftwareInfo())
                        Text(text = "Hardware Info")
                        displayInfo(infos = appInfo.getHardwareInfo())
                        Text(text = "Tests")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            checkInternetConnectionButton()
                            Text(
                                modifier = Modifier.padding(10.dp),
                                text = if(internetStatus.value) "Connected" else "Not connected" )
                        }

//                        Row(verticalAlignment = Alignment.CenterVertically) {
//                            mqttConnectionButton()
//                            Text(
//                                modifier = Modifier.padding(10.dp),
//                                text = if(mqttConnectionStatus.value) "Connected" else "Not connected" )
//                        }
//                        changeOrientationButton(LocalContext.current)
//                        Button(onClick = {
//                            val display = (getSystemService(WINDOW_SERVICE) as WindowManager).defaultDisplay
//                            val rotation = display.rotation
//                            Log.d("AT","Rotation1:$rotation")
//                            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
//                            Log.d("AT","Rotation2:${display.rotation}")
//                        }) {
//                            Text("Orientation")
//                        }
                        Button(onClick = {
                            startActivity(Intent(Settings.ACTION_SETTINGS))
                        }) {
                            Text("Settings")
                        }
                        speedTestButton()
                        networkInfo()
                    }
                }
            }
        }

        if(!isNetworkUsageAccessGranted(this)){
            showUsageAccessDialog()
        }
    }

    private fun showUsageAccessDialog() {
        AlertDialog.Builder(this)
            .setTitle("Usage Access Required")
            .setMessage("This app needs Usage Access permission to function properly. Please enable it in the settings.")
            .setPositiveButton("Open Settings") { _, _ ->
                openUsageAccessSettings(this)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    @Composable
    fun networkInfo(){
        var networkDataList = remember {
            mutableStateListOf<NetworkUsageData>()
        }
        var duration = remember {
            mutableStateOf("24")
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Button(onClick = {
                val network = NetworkUsageStats(applicationContext)
                val packageNames = listOf("com.example.automated_testing","com.adonmo.tvlauncher","com.adonmo.devicemanager")
                val endTime = System.currentTimeMillis()
                val value = if(duration.value.trim().isEmpty()) 0 else duration.value.toLong()
                val startTime = endTime - TimeUnit.HOURS.toMillis(value) // Last 3 days

                val usageData = network.getNetworkUsageForApps(packageNames, startTime, endTime)

                networkDataList.clear()

                usageData.forEach { (packageName, usage) ->
                    Log.d("AT","$packageName: Received ${usage.first} bytes, Transmitted ${usage.second} bytes")
                    networkDataList.add(
                        NetworkUsageData(
                            packageName,
                            usage.first,
                            usage.second
                        )
                    )
                }
            }) {
                Text("Data usage")
            }
            TextField(
                value =duration.value.toString() ,
                onValueChange = {
                    duration.value = it
                },
                label = {
                    Text("Duration in hours")
                }
            )
            if(networkDataList.isNotEmpty())
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Package Name", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                    Text("Received Data (in MB)", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                    Text("Transmitted Data (in MB)", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                }
            LazyColumn {
                items(networkDataList){
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = it.packageName, modifier = Modifier.weight(1f))
                        Text(text = (it.receivedBytes/(1024.0.pow(2.0))).toString(), modifier = Modifier.weight(1f))
                        Text(text = (it.transmittedBytes/(1024.0.pow(2.0))).toString(), modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }

    @Composable
    fun displayInfo(infos:List<Information>){
        LazyColumn(modifier = Modifier.padding(horizontal = 10.dp, vertical = 20.dp)) {
            items(infos){info ->
                InfoCard(info = info)
                Divider()
            }
        }
    }

    @Composable
    fun InfoCard(info: Information){
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = info.name, modifier = Modifier.weight(1f))
            Text(text = info.value, modifier = Modifier.weight(1f))
        }
    }

    @Composable
    fun checkInternetConnectionButton(){
        val context = LocalContext.current
        Button(onClick = {
            internetStatus.value = checkInternetConnection(context)
            if(internetStatus.value){
                Log.d("AT","Internet Connected")
                Toast.makeText(context,"Connected",Toast.LENGTH_SHORT)
            }
            else{
                Log.d("AT","Internet Not Connected")
                Toast.makeText(context,"Not Connected",Toast.LENGTH_SHORT)
            }
        }) {
            Text("Check Internet Connection")
        }
    }
    @Composable
    fun mqttConnectionButton(){
        val context = LocalContext.current
        Button(onClick = {
            mqttConnect(context)
        }) {
            Text("MQTT Connect")
        }
    }

    @Composable
    fun speedTestButton(){
        var context = LocalContext.current
        Button(onClick = {
            Log.d("AT","Started Speed Test")
            val url = "https://speedtest.adonmo.com/"
            openWebsite(context,url)
            Log.d("AT","Finished Speed Test")
        }) {
            Text("Speed Test")
        }
    }

    @Composable
    fun changeOrientationButton(context: Context) {
        val portrait= remember {
            mutableStateOf(false)
        }
        val activity = context as Activity
        Button(onClick = {
            Log.d("AT","Change orientation start ${activity.requestedOrientation}")

            if(portrait.value){
                Log.d("AT","Portrait to Landscape")
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
            else{
                Log.d("AT","Landscape to Portrait")
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
            portrait.value = !portrait.value
        }) {
            Text("Change Orientation")
        }
    }



    fun checkInternetConnection(context: Context):Boolean{
        Log.d("AT","Function Entry:: checkInternetConnection")
        try{
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork ?: return false
            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
            if (activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)){
                return true
            }
            if(activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)){
                return true
            }
            if(activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)){
                return true
            }
            return false
        }
        catch (e:Exception){
            Log.e("AT","Internet Connection Check failed due to $e")
            return false
        }
    }

    fun openWebsite(context: Context, url:String){
        try {
            Log.d("AT","Function Entry:: openWebsite")
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
            }
            context.startActivity(intent)
        }
        catch (e:Exception){
            Log.e("AT","Error while opening website: $url due to $e")
        }
    }

    fun mqttConnect(context: Context){
        Log.d("AT","Function Entry:: mqttConnect")
        val serverURI="tcp://broker.emqx.io:1883"
        mqttClient = MqttAndroidClient(context,serverURI,"android_client")
        mqttClient.setCallback(object : MqttCallback{
            override fun messageArrived(topic: String?, message: MqttMessage?) {
                Log.d("AT","Receive message: $message from topic:$topic")
            }

            override fun connectionLost(cause: Throwable?) {
                Log.d("AT","Connection Lost: $cause")
                mqttConnectionStatus.value = false
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {

            }
        })

        val options = MqttConnectOptions()
        try {
            mqttClient.connect(options,null, object : IMqttActionListener{
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d("AT","Mqtt connection success")
                    mqttConnectionStatus.value = true
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e("AT","Mqtt connection failed due to $exception")
                    mqttConnectionStatus.value = false
                }
            })
        }
        catch (e:MqttException){
            Log.e("AT","MQTT connection error: $e")
            mqttConnectionStatus.value = false
        }
    }

    fun isNetworkUsageAccessGranted(context: Context): Boolean{
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun openUsageAccessSettings(context: Context){
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        context.startActivity(intent)
    }

}

data class NetworkUsageData(
    val packageName: String,
    val receivedBytes: Long,
    val transmittedBytes: Long
)


