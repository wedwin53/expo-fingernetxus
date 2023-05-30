// import { NativeModulesProxy, EventEmitter, Subscription } from 'expo-modules-core';

// Import the native module. On web, it will be resolved to ExpoFingernetxus.web.ts
// and on native platforms to ExpoFingernetxus.ts
import ExpoFingernetxusModule from './ExpoFingernetxusModule';
// import ExpoFingernetxusView from './ExpoFingernetxusView';
// import { ChangeEventPayload, ExpoFingernetxusViewProps } from './ExpoFingernetxus.types';

// Get the native constant value.
// export const PI = ExpoFingernetxusModule.PI;

export function requestBluetoothPermission() {
  return ExpoFingernetxusModule.requestBluetoothPermissionsAsync();
}


