// import { NativeModulesProxy, EventEmitter, Subscription } from 'expo-modules-core';
import { EventEmitter, Subscription } from "expo-modules-core";

// Import the native module. On web, it will be resolved to ExpoFingernetxus.web.ts
// and on native platforms to ExpoFingernetxus.ts
import ExpoFingernetxusModule from "./ExpoFingernetxusModule";
// import ExpoFingernetxusView from './ExpoFingernetxusView';
// import { ChangeEventPayload, ExpoFingernetxusViewProps } from './ExpoFingernetxus.types';

// Get the native constant value.
// export const PI = ExpoFingernetxusModule.PI;

const emitter = new EventEmitter(ExpoFingernetxusModule);

export function requestBluetoothPermission() {
  return ExpoFingernetxusModule.requestBluetoothPermissionsAsync();
}

export async function connectToDevice(deviceId: string) {
  return await ExpoFingernetxusModule.connectToDeviceAsync(deviceId);
}

export async function captureFingerprintImage() {
  return await ExpoFingernetxusModule.captureFingerprintImageAsync();
}

export function addFingerprintCaptureListener(
  listener: (event: any) => void
): Subscription {
  return emitter.addListener("onFingerpringCaptured", listener);
}

export function requestTurnOnBluetooth() {
  return ExpoFingernetxusModule.requestBluetoothAsync();
}

export function addFingerprintCaptureTemplateListener(
  listener: (event: any) => void
): Subscription {
  return emitter.addListener("onCaptureTemplate", listener);
}

export function getBluetoothState() {
  return ExpoFingernetxusModule.getBluetoothConnectionState();
}

export async function captureFingerprintTemplate() {
  return await ExpoFingernetxusModule.captureTemplate();
}

export function addEnrolTemplateListener(
  listener: (event: any) => void
): Subscription {
  return emitter.addListener("onEnrolTemplate", listener);
}

export async function onEnrolTemplateAsync() {
  return await ExpoFingernetxusModule.enrolTemplate();
}

export async function enrolTemplateOnDemand(template: string) {
  return await ExpoFingernetxusModule.enrolOnDemand(template);
}

export async function captureFingerprintOnDemandTemplate() {
  return await ExpoFingernetxusModule.captureOnDemandTemplate();
}

export function addCaptureVerificationListener(
  listener: (event: any) => void
): Subscription {
  return emitter.addListener("onCaptureVerification", listener);
}

export function addBluetoothStateChangeListener(
  listener: (event: any) => void
) {
  return emitter.addListener("onBluetoothStateOn", listener);
}

export function getHostBluetoothState() {
  return ExpoFingernetxusModule.getBluetoothState();
}
