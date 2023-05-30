package expo.modules.fingernetxus

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import org.json.JSONObject

class ExpoFingernetxusModule : Module() {

  // create a variable to hold if the permission has been granted or not


  var permissionGranted: Boolean = false
  var currentAdapter: BluetoothAdapter? = null
  var newDevicesList: ArrayList<String> = ArrayList()
  var pairedDevicesList: ArrayList<String> = ArrayList()


  // Each module class must implement the definition function. The definition consists of components
  // that describes the module's functionality and behavior.
  // See https://docs.expo.dev/modules/module-api for more details about available components.
  override fun definition() = ModuleDefinition {
    // Sets the name of the module that JavaScript code will use to refer to the module. Takes a string as an argument.
    // Can be inferred from module's class name, but it's recommended to set it explicitly for clarity.
    // The module will be accessible from `requireNativeModule('ExpoFingernetxus')` in JavaScript.
    Name("ExpoFingernetxus")


    // Function to request bluetooth permissions
    Function("requestBluetoothPermissionsAsync") {
      val activity = appContext.activityProvider?.currentActivity
        val bluetoothPermissions = arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

      val applicationContext = activity?.applicationContext

      if(applicationContext != null) {
        Log.i("ExpoFingernetxusModule", "Requesting bluetooth permissions")

        val permissionCheck = ContextCompat.checkSelfPermission(
          applicationContext,
          Manifest.permission.BLUETOOTH
        )
        Log.i("ExpoFingernetxusModule", "Permission check: $permissionCheck")
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
          ActivityCompat.requestPermissions(
            activity,
            arrayOf(bluetoothPermissions[0], bluetoothPermissions[1], bluetoothPermissions[2], bluetoothPermissions[3]),
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
              Log.i("ExpoFingernetxusModule", "Bluetooth adapter is already discovering, stopping discovery")
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

    // Function to Connect to a bluetooth device recieved from the JS side by parameter


  }



}
