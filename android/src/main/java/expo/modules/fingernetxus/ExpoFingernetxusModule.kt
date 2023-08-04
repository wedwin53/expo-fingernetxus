package expo.modules.fingernetxus

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Base64
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.fpreader.fpdevice.AsyncBluetoothReader
import com.fpreader.fpdevice.BluetoothReader
import expo.modules.kotlin.Promise
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
    private var enrolResult: String = ""
    var mRefData = ByteArray(512)
    var mRefSize = 0
    var mMatData = ByteArray(512)
    var mMatSize = 0
    private var worktype = 0

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

        Events("onFingerpringCaptured", "onCaptureTemplate", "onEnrolTemplate")

        //Events("onCaptureTemplate")

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
                        Log.i("ExpoFingernetxusModule", "onGetStdImageSuccess Data: $data")
                        //Bitmap.createBitmap(256, 288, Bitmap.Config.ARGB_8888)
                        val base64Data = Base64.encodeToString(data, Base64.DEFAULT)
                        Log.i("ExpoFingernetxusModule", "Base64: $base64Data")
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
                        Log.i("ExpoFingernetxusModule", "setOnGetResImageListener Data: $data")
                        //Bitmap.createBitmap(256, 288, Bitmap.Config.ARGB_8888)
                        val base64Data = Base64.encodeToString(data, Base64.DEFAULT)
                        Log.i("ExpoFingernetxusModule", "Base64: $base64Data")
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

                    override fun onCaptureTemplateSuccess(model: ByteArray) {
                        System.arraycopy(model, 0, mMatData, 0, model.size)
                        mMatSize = model.size

                        Log.i("ExpoFingernetxusModule", "mMatSize: $mMatSize and mRefSize: $mRefSize")

                        if (mRefSize > 0) {
                            val score = asyncBluetoothReader?.bluetoothReader?.MatchTemplate(mRefData, mMatData)
                            Log.i("ExpoFingernetxusModule", "Score: $score")
                            sendEvent("onCaptureTemplate", mapOf(
                                "captureScore" to score
                            ))
                        }
                    }

                    override fun onCaptureTemplateFail() {
                        Log.e("ExpoFingernetxusModule", "Failed to capture template")
                    }
                })

                // Enroll template
                asyncBluetoothReader!!.setOnEnrolTemplateListener(object :
                    AsyncBluetoothReader.OnEnrolTemplateListener {

                    override fun onEnrolTemplateSuccess(model: ByteArray?) {
                        Log.i("ExpoFingernetxusModule", "onEnrolTemplateSuccess Data: $model")

                        System.arraycopy(model!!, 0, mRefData,0, model.size)
                        mRefSize = model.size;
                        Log.i("ExpoFingernetxusModule", "Enrol Template Success")
                        sendEvent("onEnrolTemplate", mapOf(
                            "enrolResult" to "Enrol Template Success"
                        ))
                    }

                    override fun onEnrolTemplateFail() {
                        Log.e("ExpoFingernetxusModule", "Failed to Enrol")
                        sendEvent("onEnrolTemplate", mapOf(
                            "enrolResult" to "Failed to Enrol"
                        ))
                    }
                })

                // upEnrol Template
                asyncBluetoothReader!!.setOnUpTemplateListener(object :
                    AsyncBluetoothReader.OnUpTemplateListener {

                    override fun onUpTemplateSuccess(model: ByteArray) {
                        // make the Capture and math of the template
                        if (worktype == 1) {
                            System.arraycopy(model, 0, mMatData, 0, model.size)
                            mMatSize = model.size

                            if (mRefSize > 0) {
                                val score = asyncBluetoothReader?.bluetoothReader?.MatchTemplate(mRefData, mMatData)
                                Log.i("ExpoFingernetxusModule", "Score: $score")
                                sendEvent("onCaptureTemplate", mapOf(
                                    "captureScore" to score
                                ))
                            }
                        }
                        else if (worktype == 2){
                            mMatData = ByteArray(model.size)
                            System.arraycopy(model, 0, mMatData, 0, model.size)
                            mMatSize = model.size

                            if (mRefSize > 0) {
                                val score = asyncBluetoothReader?.bluetoothReader?.MatchTemplate(mRefData, mMatData)
                                Log.i("ExpoFingernetxusModule", "Score: $score")
                                sendEvent("onCaptureTemplate", mapOf(
                                    "captureScore" to score
                                ))
                            }
                        }

                        else { // aca hace el enrol
                            System.arraycopy(model, 0, mRefData, 0, model.size)
                            mRefSize = model.size
                            Log.i("ExpoFingernetxusModule", "Enrol Template Success")
                            sendEvent("onEnrolTemplate", mapOf(
                                "enrolResult" to "Enrol Template Success"
                            ))

                        }
                    }

                    override fun onUpTemplateFail() {
                        if (worktype == 1) {
                            Log.e("ExpoFingernetxusModule", "Failed to capture template")
                        } else {
                            Log.e("ExpoFingernetxusModule", "Failed to Enrol")
                        }
                    }
                }) // end setOnUpTemplateListener


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
//                            asyncBluetoothReader!!.CaptureTemplateNoImage()
                            worktype = 1
                            asyncBluetoothReader!!.GetImageAndTemplate()
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

        AsyncFunction("enrolTemplate") { promise: Promise ->

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
                        Log.i("ExpoFingernetxusModule", "Bluetooth reader is connected and ready to enrol")
                        // checks if asyncBluetoothReader is null and if not, image variable is set to NO_CONNECTION
                        if (asyncBluetoothReader == null) {
                            enrolResult = "NO_CONNECTION"
                        }else {
                            // calls the function to enrol template
//                            asyncBluetoothReader!!.EnrolTempatelNoImage()
                            worktype = 0
                            asyncBluetoothReader!!.GetImageAndTemplate()
                        }

                    }
                    Log.i("ExpoFingernetxusModule", "Inside enrolTemplate")

                } // end launch
                promise.resolve(enrolResult)

            } // end if

        }// end function enrolTemplate

        // experimental math template with params
        AsyncFunction("enrolOnDemand") { template: String, promise: Promise ->

            val activity = appContext.activityProvider?.currentActivity
            val applicationContext = activity?.applicationContext
            if (applicationContext != null) {
            //                scope.launch {
            //
            //
            //                } // end launch
                Log.i("ExpoFingernetxusModule", "Inside enrolOnDemand")
                // checks if asyncBluetoothReader is null and if not, image variable is set to NO_CONNECTION

                if (template != null) {
                    // take the template and convert it to byte array
                    val templateByteArray = template.toByteArray()
                    mRefData = ByteArray(templateByteArray.size)

                    System.arraycopy(templateByteArray, 0, mRefData,0, templateByteArray.size)
                    mRefSize = templateByteArray.size;
                    enrolResult = "OK"
                }

                promise.resolve(enrolResult)

            } // end if

        }// end function enrolOnDemand

        AsyncFunction("captureOnDemandTemplate") { promise: Promise ->

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
//                            asyncBluetoothReader!!.CaptureTemplateNoImage()
                            worktype = 2
                            asyncBluetoothReader!!.GetImageAndTemplate()
                        }

                    }
                    Log.i("ExpoFingernetxusModule", "Inside captureOnDemandTemplate")

                } // end launch
                promise.resolve(template)

            } // end if

        }// end function captureOnDemandTemplate


    }// end definition
} // end class

