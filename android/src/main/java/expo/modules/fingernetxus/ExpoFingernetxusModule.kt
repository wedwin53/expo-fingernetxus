package expo.modules.fingernetxus

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Build
import android.util.Base64
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.fpreader.fpdevice.AsyncBluetoothReader
import com.fpreader.fpdevice.BluetoothReader
import com.machinezoo.sourceafis.FingerprintImage
import com.machinezoo.sourceafis.FingerprintImageOptions
import com.machinezoo.sourceafis.FingerprintMatcher
import com.machinezoo.sourceafis.FingerprintTemplate


import expo.modules.kotlin.Promise
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.apache.commons.io.output.ByteArrayOutputStream


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
    var preTemplate = ByteArray(512)

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


    override fun definition() = ModuleDefinition {

        Name("ExpoFingernetxus")

        Events(
            "onFingerpringCaptured",
            "onCaptureTemplate",
            "onEnrolTemplate",
            "onCaptureVerification",
            "onBluetoothStateOn",
            "onBluetoothStateOff"
        )

        // If bluetooth is not active on the device, this function will request to turn it on
        Function("requestBluetoothAsync") {
            scope.launch(Dispatchers.IO) {

                val activity = appContext.activityProvider?.currentActivity
                val applicationContext = activity?.applicationContext
                if (applicationContext != null) {
                    Log.i("ExpoFingernetxusModule", "Requesting bluetooth")
                    currentAdapter = BluetoothAdapter.getDefaultAdapter()

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
                    } else if (permissionCheckSDKHightLevel != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        // Permission for Android 12 and above
                        ActivityCompat.requestPermissions(
                            activity,
                            btPermissionsHighLevelSDK,
                            1
                        )
                    } else {
                        Log.i("ExpoFingernetxusModule", "Bluetooth permissions already granted")
                        permissionGranted = true
                        if (currentAdapter != null && !currentAdapter!!.isEnabled) {
                            currentAdapter?.enable()
                        }

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
                } else if (permissionCheckSDKHightLevel != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
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


                        sendEvent(
                            "onFingerpringCaptured", mapOf(
                                "image" to image
                            )
                        )
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

                        if(worktype == 4){
                            // check if is valid fingerprint comparint with the preTemplate
                            val isValidFP = isValidFingerprint(preTemplate, data!!)
                            Log.i("ExpoFingernetxusModule", "isValidFP: $isValidFP")
                            sendEvent(
                                "onCaptureVerification", mapOf(
                                    "captureScore" to isValidFP
                                )
                            )
                        }

                        sendEvent(
                            "onFingerpringCaptured", mapOf(
                                "image" to image
                            )
                        )
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

                        Log.i(
                            "ExpoFingernetxusModule",
                            "mMatSize: $mMatSize and mRefSize: $mRefSize"
                        )

                        if (mRefSize > 0) {
                            val score = asyncBluetoothReader?.bluetoothReader?.MatchTemplate(
                                mRefData,
                                mMatData
                            )
                            Log.i("ExpoFingernetxusModule", "Score: $score")
                            sendEvent(
                                "onCaptureTemplate", mapOf(
                                    "captureScore" to score
                                )
                            )
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

                        System.arraycopy(model!!, 0, mRefData, 0, model.size)
                        mRefSize = model.size;
                        Log.i("ExpoFingernetxusModule", "Enrol Template Success")
                        sendEvent(
                            "onEnrolTemplate", mapOf(
                                "enrolResult" to "Enrol Template Success"
                            )
                        )
                    }

                    override fun onEnrolTemplateFail() {
                        Log.e("ExpoFingernetxusModule", "Failed to Enrol")
                        sendEvent(
                            "onEnrolTemplate", mapOf(
                                "enrolResult" to "Failed to Enrol"
                            )
                        )
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
                                val score = asyncBluetoothReader?.bluetoothReader?.MatchTemplate(
                                    mRefData,
                                    mMatData
                                )
                                Log.i("ExpoFingernetxusModule", "Score: $score")
                                val base64Template = Base64.encodeToString(model, Base64.DEFAULT)
                                sendEvent(
                                    "onCaptureTemplate", mapOf(
                                        "captureScore" to score,
                                        "template" to base64Template
                                    )
                                )
                            }
                        } else if (worktype == 2) {
                            Log.i("ExpoFingernetxusModule", "Model Object (incoming): $model")
//                            val base64Data = Base64.encodeToString(model, Base64.DEFAULT)
//                            Log.i("ExpoFingernetxusModule", "Base64 of Incomming: $base64Data")

//                            val incommingTemplate = getBytesFromBase64(base64Data)
//                            val isValidLegacy = isMatchLegacy(preTemplate, model)
//                            val isValid = isMatch(preTemplate, model)
//                            val isValidFingerprint = isValid || isValidLegacy
                            val isValidFP = isValidFingerprint(preTemplate, model)

                            Log.i("ExpoFingernetxusModule", "isValidFP: $isValidFP")
//                            Log.i("ExpoFingernetxusModule", "isValidTry2: $isValidLegacy")
                            sendEvent(
                                "onCaptureVerification", mapOf(
                                    "captureScore" to isValidFP
                                )
                            )
                        } else { // aca hace el enrol
                            System.arraycopy(model, 0, mRefData, 0, model.size)
                            mRefSize = model.size
                            Log.i("ExpoFingernetxusModule", "Enrol Template Success")
                            sendEvent(
                                "onEnrolTemplate", mapOf(
                                    "enrolResult" to "Enrol Template Success"
                                )
                            )

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


                // onBluetoothStateListener
                asyncBluetoothReader!!.setOnBluetoothStateListener(object :
                    AsyncBluetoothReader.OnBluetoothStateListener {
                    override fun onBluetoothStateChange(p0: Int) {
                        Log.d("ExpoFingernetxusModule", "onBluetoothStateChange: $p0")
                        if (p0 == BluetoothReader.STATE_CONNECTED) {
                            sendEvent(
                                "onBluetoothStateOn", mapOf(
                                    "bluetoothState" to "on"
                                )
                            )
                        }
                    }

                    override fun onBluetoothStateDevice(p0: String?) {
                        Log.d("ExpoFingernetxusModule", "onBluetoothStateDevice: $p0")
                    }

                    override fun onBluetoothStateLost(p0: Int) {
                        Log.d("ExpoFingernetxusModule", "onBluetoothStateLost: $p0")
                        sendEvent(
                            "onBluetoothStateOn", mapOf(
                                "bluetoothState" to "off"
                            )
                        )
                    }


                })

                //return@Function "Connected to device: $deviceName"
                promise.resolve("status: ${asyncBluetoothReader?.bluetoothReader?.getState()}")
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
                        } else {
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
                        } else {
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

                isBTEnabled =
                    BluetoothReader.STATE_CONNECTED == asyncBluetoothReader?.bluetoothReader?.getState()
                Log.i("ExpoFingernetxusModule", "Bluetooth connection state: $isBTEnabled")
                return@Function isBTEnabled
            } else {
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
                        Log.i(
                            "ExpoFingernetxusModule",
                            "Bluetooth reader is connected and ready to enrol"
                        )
                        // checks if asyncBluetoothReader is null and if not, image variable is set to NO_CONNECTION
                        if (asyncBluetoothReader == null) {
                            enrolResult = "NO_CONNECTION"
                        } else {
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
                Log.i("ExpoFingernetxusModule", "Inside enrolOnDemand")
                // checks if asyncBluetoothReader is null and if not, image variable is set to NO_CONNECTION

                if (template != null) {
                    // take the template and convert it to byte array
                    var bytesFromBase64 = getBytesFromBase64(template)
                    preTemplate = bytesFromBase64!!
                    enrolResult = "OK"
                }

                promise.resolve(enrolResult)

            } // end if

        }// end function enrolOnDemand

        /**
         * Once the enrollOndemand was made, this method should be called to verify the fingerprint
         */
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
                        } else {
                            // calls the function to get the image
//                            asyncBluetoothReader!!.CaptureTemplateNoImage()
                            worktype = 4
                            asyncBluetoothReader!!.GetImageAndTemplate()
                        }

                    }
                    Log.i("ExpoFingernetxusModule", "Inside captureOnDemandTemplate")

                } // end launch
                promise.resolve(template)

            } // end if

        }// end function captureOnDemandTemplate

        // get bluetooth state
        Function("getBluetoothState") {
            val activity = appContext.activityProvider?.currentActivity
            val applicationContext = activity?.applicationContext
            var isBTEnabled = false
            if (applicationContext != null) {
                Log.i("ExpoFingernetxusModule", "getting bluetooth state")

                isBTEnabled = currentAdapter?.isEnabled!!
                Log.i("ExpoFingernetxusModule", "Bluetooth state: $isBTEnabled")
                return@Function isBTEnabled
            } else {
                Log.i("ExpoFingernetxusModule", "applicationContext is null")
                return@Function isBTEnabled
            }
        } // End getBlueThoothConnectionState

    }// end definition

    // DANGER ZONE


    // END DANGER ZONE


    private fun isMatch(existingFingerprint: ByteArray, toEvaluateFingerprint: ByteArray): Boolean {

        try {
            // DECODE the incoming fingerprint to know what type if image is
            //val decodedFingerprint = decodeFingerprintImage(toEvaluateFingerprint)
            val fingerprintImageOptions = FingerprintImageOptions()
            fingerprintImageOptions.dpi(500.0)

            val incommingFP =
                FingerprintTemplate(FingerprintImage(toEvaluateFingerprint)).dpi(500.0)
            val existingFP = FingerprintTemplate(FingerprintImage(existingFingerprint).dpi(500.0))

            val matcher = FingerprintMatcher(incommingFP)
            val similarity = matcher.match(existingFP)

            val threshold = 20.0
            return similarity >= threshold

        } catch (e: IllegalArgumentException) {
            Log.e("ExpoFingernetxusModule", "isMatch::Error: ${e.message}")
            return false
        } catch (e: Exception) {
            // Maneja cualquier otra excepción no anticipada
            Log.e("ExpoFingernetxusModule", "isMatch::Error: ${e.message}")
            return false
        }
    }

    private fun isMatchLegacy(
        existingFingerprint: ByteArray,
        toEvaluateFingerprint: ByteArray
    ): Boolean {
        return try {
            FingerprintMatcher(
                FingerprintTemplate(
                    FingerprintImage().dpi(500.0).decode(existingFingerprint)
                )
            ).match(
                FingerprintTemplate(FingerprintImage().dpi(500.0).decode(toEvaluateFingerprint))
            ) >= 20.0
        } catch (e: Exception) {
            Log.e("ExpoFingernetxusModule", "isMatchLegacy::Error: ${e.message}")
            false
        }
    }

    private fun getBytesFromBase64(data: String?): ByteArray? {
        return Base64.decode(data, Base64.NO_WRAP)
    }

    private fun isMatchWithMatcher(
        existingFingerprint: ByteArray?,
        toEvaluateFingerprint: ByteArray?
    ): Boolean {
        try {
            val decodedFingerprint: ByteArray =
                decodeFingerprintImage(toEvaluateFingerprint!!)

            val decodedExistingFingerprint: ByteArray =
                decodeFingerprintImage(existingFingerprint!!)

            val incommingFP = FingerprintTemplate(FingerprintImage(decodedFingerprint))
            val existingFP = FingerprintTemplate(FingerprintImage(decodedExistingFingerprint))

            val similarity = FingerprintMatcher(incommingFP).match(existingFP)
            val threshold = 20.0
            return similarity >= threshold
        } catch (e: IllegalArgumentException) {
            Log.e("ExpoFingernetxusModule", "isMatchWithMatcher::Error: ${e.message}")
            return false
        } catch (e: Exception) {
            // Manejar cualquier otra excepción no anticipada
            Log.e("ExpoFingernetxusModule", "isMatchWithMatcher::Error: ${e.message}")
            return false
        }
    }

    /**
     * Fix Flip Fingerprint for comparations
     */

    private fun isValidFingerprint(current: ByteArray, incoming: ByteArray): Boolean {
        // flip the incomming fingerprint and compare it with the current fingerprint
        val flippedIncoming = getFlippedImage(incoming)
//        val rotatedImage = getRotatedImage(incoming)

        return isMatch(current, incoming) || isMatch(current, flippedIncoming)
    }

    private fun getFlippedImage(image: ByteArray): ByteArray {
        try {
            Log.d("ExpoFingernetxusModule", "getFlippedImage::Image: ${image.size}")
            return getBytesFromBitmap(getFlippedBitmap(getBitmapFromBytes(image)))

        } catch (e: Exception) {
            Log.e("ExpoFingernetxusModule", "getFlippedImage::Error: ${e.message}")
            return ByteArray(0)
        }
    }

    private fun getRotatedImage(image: ByteArray): ByteArray {
        try {
            Log.d("ExpoFingernetxusModule", "getRotatedImage::Image: ${image.size}")
            return getBytesFromBitmap(getRotatedBitmap(getBitmapFromBytes(image)))
        } catch (e: Exception) {
            Log.e("ExpoFingernetxusModule", "getRotatedImage::Error: ${e.message}")
            return ByteArray(0)
        }
    }

    private fun getFlippedBitmap(bmp: Bitmap): Bitmap {
        try {
            return flip(
                bmp,
                -1.0f,
                1.0f,
                (bmp.width.toFloat()) / 2.0f,
                (bmp.height.toFloat()) / 2.0f
            )
        } catch (e: Exception) {
            Log.e("ExpoFingernetxusModule", "getFlippedBitmap::Error: ${e.message}")
            return Bitmap.createBitmap(256, 288, Bitmap.Config.ARGB_8888)
        }
    }

    private fun getRotatedBitmap(bmp: Bitmap): Bitmap {
        try {
            return rotateBitmap(bmp)
        } catch (e: Exception) {
            Log.e("ExpoFingernetxusModule", "getRotatedBitmap::Error: ${e.message}")
            return Bitmap.createBitmap(256, 288, Bitmap.Config.ARGB_8888)
        }
    }

    private fun getBitmapFromBytes(bytes: ByteArray): Bitmap {

        try {
            Log.d("ExpoFingernetxusModule", "getBitmapFromBytes::Bytes: ${bytes.size}")
            val decodeByteArray = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            return decodeByteArray
        } catch (e: Exception) {
            Log.e("ExpoFingernetxusModule", "getBitmapFromBytes::Error: ${e.message}")
            return Bitmap.createBitmap(256, 288, Bitmap.Config.ARGB_8888)
        }
    }

    private fun getBytesFromBitmap(bmp: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val byteArray = stream.toByteArray()
        return byteArray
    }


    private fun flip(toFlip: Bitmap, x: Float, y: Float, cx: Float, cy: Float): Bitmap {
        val matrix = Matrix()
        matrix.postScale(x, y, cx, cy)
        val createBitmap = Bitmap.createBitmap(
            toFlip,
            0,
            0,
            toFlip.width,
            toFlip.height,
            matrix,
            true
        )
        return createBitmap
    }

    private fun rotateBitmap(toRotate: Bitmap): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(180f) // Rotar 180 grados
        return Bitmap.createBitmap(
            toRotate,
            0,
            0,
            toRotate.width,
            toRotate.height,
            matrix,
            true
        )
    }

    private fun decodeFingerprintImage(imageData: ByteArray?): ByteArray {
        // Decodificar el byte array a un Bitmap
        val bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData!!.size)
            ?: throw java.lang.IllegalArgumentException("Unable to decode image.")

        // Verificar que el Bitmap no sea nulo

        // Convertir el Bitmap a un array de bytes (grayscale)
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val grayscale = ByteArray(width * height)
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val red = (pixel shr 16) and 0xFF
            val green = (pixel shr 8) and 0xFF
            val blue = pixel and 0xFF
            val gray = (red + green + blue) / 3
            grayscale[i] = gray.toByte()
        }

        return grayscale
    }


} // end class

