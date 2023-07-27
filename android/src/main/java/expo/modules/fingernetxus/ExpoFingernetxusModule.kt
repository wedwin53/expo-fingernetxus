package expo.modules.fingernetxus

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.fpreader.fpdevice.AsyncBluetoothReader
import com.fpreader.fpdevice.BluetoothReader
import expo.modules.kotlin.Promise
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import kotlinx.coroutines.*
import org.json.JSONObject

class ExpoFingernetxusModule : Module() {

    // create a variable to hold if the permission has been granted or not


    var permissionGranted: Boolean = false
    var currentAdapter: BluetoothAdapter? = null
    var newDevicesList: ArrayList<String> = ArrayList()
    var pairedDevicesList: ArrayList<String> = ArrayList()
    private val scope = CoroutineScope(Dispatchers.Main)
    private var asyncBluetoothReader: AsyncBluetoothReader? = null
    private var image: String = ""
    private var template: String = ""

    private val bluetoothPermissions = arrayOf(
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )

    private val btPermissionsHighLevelSDK = arrayOf(
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_ADVERTISE
    )


    // Each module class must implement the definition function. The definition consists of components
    // that describes the module's functionality and behavior.
    // See https://docs.expo.dev/modules/module-api for more details about available components.
    override fun definition() = ModuleDefinition {
        // Sets the name of the module that JavaScript code will use to refer to the module. Takes a string as an argument.
        // Can be inferred from module's class name, but it's recommended to set it explicitly for clarity.
        // The module will be accessible from `requireNativeModule('ExpoFingernetxus')` in JavaScript.
        Name("ExpoFingernetxus")

        Events("onFingerpringCaptured")

        Events("onCaptureTemplate")

        // If bluetooth is not active on the device, this function will request to turn it on
        Function("requestBluetoothAsync") {
            val activity = appContext.activityProvider?.currentActivity
            val applicationContext = activity?.applicationContext
            if (applicationContext != null) {
                Log.i("ExpoFingernetxusModule", "Requesting bluetooth")
                val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                if (bluetoothAdapter == null) {
                    Log.i("ExpoFingernetxusModule", "No bluetooth adapter found")
                } else {
                    if (!bluetoothAdapter.isEnabled) {
                        Log.i("ExpoFingernetxusModule", "Bluetooth is not enabled")
                        // checking permissions
                        val permissionCheck = ContextCompat.checkSelfPermission(
                            applicationContext,
                            Manifest.permission.BLUETOOTH
                        )

                        val permissionCheckSDKHightLevel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            ContextCompat.checkSelfPermission(
                                applicationContext,
                                Manifest.permission.BLUETOOTH_CONNECT
                            )
                        } else {
                            ContextCompat.checkSelfPermission(
                                applicationContext,
                                Manifest.permission.BLUETOOTH
                            )
                        }

                        if (permissionCheck != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                            // Permission for Android 11 and below
                            ActivityCompat.requestPermissions(
                                activity,
                                bluetoothPermissions,
                                1
                            )
                        } else if(permissionCheckSDKHightLevel != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
                            // Permission for Android 12 and above
                            ActivityCompat.requestPermissions(
                                activity,
                                btPermissionsHighLevelSDK,
                                1
                            )
                        } else {
                            Log.i("ExpoFingernetxusModule", "Bluetooth permissions already granted")
                            permissionGranted = true
                        }

                        bluetoothAdapter.enable()
                    } else {
                        Log.i("ExpoFingernetxusModule", "Bluetooth is already enabled")
                    }
                }
            }
        }

        // Function to request bluetooth permissions
        Function("requestBluetoothPermissionsAsync") {
            val activity = appContext.activityProvider?.currentActivity


            val applicationContext = activity?.applicationContext

            if (applicationContext != null) {
                Log.i("ExpoFingernetxusModule", "Requesting bluetooth permissions")

                val permissionCheck = ContextCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.BLUETOOTH
                )

                val permissionCheckSDKHightLevel = ContextCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.BLUETOOTH_CONNECT
                )

                Log.i("ExpoFingernetxusModule", "Permission check: $permissionCheck")
                if (permissionCheck != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                    // Permission for Android 11 and below
                    ActivityCompat.requestPermissions(
                        activity,
                        bluetoothPermissions,
                        1
                    )
                } else if(permissionCheckSDKHightLevel != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
                    // Permission for Android 12 and above
                    ActivityCompat.requestPermissions(
                        activity,
                        btPermissionsHighLevelSDK,
                        1
                    )
                } else {
                    Log.i("ExpoFingernetxusModule", "Bluetooth permissions already granted")
                    permissionGranted = true

                    // BLUETOOTH CONNECTION
                    currentAdapter = BluetoothAdapter.getDefaultAdapter()
                    if (currentAdapter == null) {
                        Log.i("ExpoFingernetxusModule", "No bluetooth adapter found")
                    } else {
                        // If we're already discovering, stop it
                        if (currentAdapter!!.isDiscovering) {
                            Log.i(
                                "ExpoFingernetxusModule",
                                "Bluetooth adapter is already discovering, stopping discovery"
                            )
                            currentAdapter!!.cancelDiscovery()
                        }
                        // Request discover from BluetoothAdapter
                        Log.i("ExpoFingernetxusModule", "Starting discovery")
                        currentAdapter!!.startDiscovery()

                        // Get a set of currently paired devices
                        val pairedDevices = currentAdapter!!.bondedDevices
                        Log.i("ExpoFingernetxusModule", "Paired devices: $pairedDevices")
                        // If there are paired devices, add each one to the ArrayAdapter
                        if (pairedDevices.size > 0) {
                            for (device in pairedDevices) {
                                pairedDevicesList.add(device.name + "," + device.address)

                            }
                        } else {
                            pairedDevicesList.add("No paired devices found")
                        }


                    }
                }
            }
            return@Function pairedDevicesList
        }

        // Function to Connect to a bluetooth device received from the JS side by parameter
        AsyncFunction("connectToDeviceAsync") { deviceName: String, promise: Promise ->
            scope.launch {
                asyncBluetoothReader = AsyncBluetoothReader()
                val activity = appContext.activityProvider?.currentActivity
                val applicationContext = activity?.applicationContext
                if (applicationContext != null) {
                    Log.i("ExpoFingernetxusModule", "Connecting to device: $deviceName")
                    // Looper.prepare()
                    asyncBluetoothReader!!.stop()
                    val device = currentAdapter!!.getRemoteDevice(deviceName)
                    Log.i("ExpoFingernetxusModule", "Device: $device")
                    // Attempt to connect to the device
                    asyncBluetoothReader!!.connect(device)
                    Log.i("ExpoFingernetxusModule", "Connected to device: $device")
                }

                // Se monta la implementacion de las interfaces para la captura
                asyncBluetoothReader!!.setOnGetStdImageListener(object :
                    AsyncBluetoothReader.OnGetStdImageListener {

                    override fun onGetStdImageSuccess(data: ByteArray?) {
                        Log.i("ExpoFingernetxusModule", "Data: $data")
                        //Bitmap.createBitmap(256, 288, Bitmap.Config.ARGB_8888)
                        val base64Data = Base64.encodeToString(data, Base64.DEFAULT)
                        Log.i("ExpoFingernetxusModule", "Base64 data: $base64Data")
                        image = "data:image/png;base64,$base64Data"
                        sendEvent("onFingerpringCaptured", mapOf(
                            "image" to image
                        ))
                    }

                    override fun onGetStdImageFail() {
                        Log.e("ExpoFingernetxusModule", "Failed to get image")
                        image = "Failed to get image"
                    }
                })

                asyncBluetoothReader!!.setOnGetResImageListener(object :
                    AsyncBluetoothReader.OnGetResImageListener {

                    override fun onGetResImageSuccess(data: ByteArray?) {
                        Log.i("ExpoFingernetxusModule", "Data: $data")
                        //Bitmap.createBitmap(256, 288, Bitmap.Config.ARGB_8888)
                        val base64Data = Base64.encodeToString(data, Base64.DEFAULT)
                        Log.i("ExpoFingernetxusModule", "Base64 data: $base64Data")
                        image = "data:image/png;base64,$base64Data"
                        sendEvent("onFingerpringCaptured", mapOf(
                            "image" to image
                        ))
                    }

                    override fun onGetResImageFail() {
                        Log.e("ExpoFingernetxusModule", "Failed to get image")
                        image = "Failed to get image"
                    }
                })

                // capture template
                asyncBluetoothReader!!.setOnCaptureTemplateListener(object :
                    AsyncBluetoothReader.OnCaptureTemplateListener {

                    override fun onCaptureTemplateSuccess(data: ByteArray?) {
                        Log.i("ExpoFingernetxusModule", "Data: $data")
                        //Bitmap.createBitmap(256, 288, Bitmap.Config.ARGB_8888)
                        val base64Data = Base64.encodeToString(data, Base64.DEFAULT)
                        Log.i("ExpoFingernetxusModule", "Base64 data: $base64Data")

                        sendEvent("onCaptureTemplate", mapOf(
                            "template" to base64Data
                        ))
                    }

                    override fun onCaptureTemplateFail() {
                        Log.e("ExpoFingernetxusModule", "Failed to get Template")

                    }
                })

                sendEvent("onFingerpringCaptured", mapOf(
                    "image" to image
                ))
                //return@Function "Connected to device: $deviceName"
                promise.resolve("Connected to device: $deviceName")
            }
        }// end function connectToDeviceAsync

        AsyncFunction("captureFingerprintImageAsync") { promise: Promise ->

            val activity = appContext.activityProvider?.currentActivity
            val applicationContext = activity?.applicationContext
            if (applicationContext != null) {
                scope.launch {
                    Log.i(
                        "ExpoFingernetxusModule",
                        "Bluetooth reader state: ${asyncBluetoothReader?.bluetoothReader?.getState()}"
                    )
                    Log.i(
                        "ExpoFingernetxusModule",
                        "Bluetooth reader is connected: ${asyncBluetoothReader?.bluetoothReader?.getState() == BluetoothReader.STATE_CONNECTED}"
                    )
                    if (asyncBluetoothReader?.bluetoothReader?.getState() == BluetoothReader.STATE_CONNECTED) {
                        Log.i("ExpoFingernetxusModule", "Bluetooth reader is connected")
                        // checks if asyncBluetoothReader is null and if not, image variable is set to NO_CONNECTION
                        if (asyncBluetoothReader == null) {
                            image = "NO_CONNECTION"
                        }else {
                            // calls the function to get the image
                            asyncBluetoothReader!!.GetImageAndTemplate()
                        }

                    }
                    Log.i("ExpoFingernetxusModule", "Inside captureFingerprintImageAsync")

                } // end launch

//                sendEvent("onFingerpringCaptured", mapOf(
//                    "image" to image
//                ))
                promise.resolve(image)

            } // end if

        }// end function captureFingerprintImage

        AsyncFunction("captureTemplate") { promise: Promise ->

            val activity = appContext.activityProvider?.currentActivity
            val applicationContext = activity?.applicationContext
            if (applicationContext != null) {
                scope.launch {
                    Log.i(
                        "ExpoFingernetxusModule",
                        "Bluetooth reader state: ${asyncBluetoothReader?.bluetoothReader?.getState()}"
                    )
                    Log.i(
                        "ExpoFingernetxusModule",
                        "Bluetooth reader is connected: ${asyncBluetoothReader?.bluetoothReader?.getState() == BluetoothReader.STATE_CONNECTED}"
                    )
                    if (asyncBluetoothReader?.bluetoothReader?.getState() == BluetoothReader.STATE_CONNECTED) {
                        Log.i("ExpoFingernetxusModule", "Bluetooth reader is connected")
                        // checks if asyncBluetoothReader is null and if not, image variable is set to NO_CONNECTION
                        if (asyncBluetoothReader == null) {
                            template = "NO_CONNECTION"
                        }else {
                            // calls the function to get the image
                            asyncBluetoothReader!!.CaptureTemplateNoImage()
                        }

                    }
                    Log.i("ExpoFingernetxusModule", "Inside captureFingerprintImageAsync")

                } // end launch
                promise.resolve(template)

            } // end if

        }// end function captureTemplate

        Function("getBluetoothConnectionState") {
            val activity = appContext.activityProvider?.currentActivity
            val applicationContext = activity?.applicationContext
            var isBTEnabled = false
            if (applicationContext != null) {
                Log.i("ExpoFingernetxusModule", "getting bluetooth connection state")

                isBTEnabled = BluetoothReader.STATE_CONNECTED == asyncBluetoothReader?.bluetoothReader?.getState()
                Log.i("ExpoFingernetxusModule", "Bluetooth connection state: $isBTEnabled")
                return@Function isBTEnabled
            }else{
                Log.i("ExpoFingernetxusModule", "applicationContext is null")
                return@Function isBTEnabled
            }
        } // End getBlueThoothConnectionState


    }// end definition
} // end class

