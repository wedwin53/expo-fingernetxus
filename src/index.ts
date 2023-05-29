import { NativeModulesProxy, EventEmitter, Subscription } from 'expo-modules-core';

// Import the native module. On web, it will be resolved to ExpoFingernetxus.web.ts
// and on native platforms to ExpoFingernetxus.ts
import ExpoFingernetxusModule from './ExpoFingernetxusModule';
import ExpoFingernetxusView from './ExpoFingernetxusView';
import { ChangeEventPayload, ExpoFingernetxusViewProps } from './ExpoFingernetxus.types';

// Get the native constant value.
export const PI = ExpoFingernetxusModule.PI;

export function hello(): string {
  return ExpoFingernetxusModule.hello();
}

export async function setValueAsync(value: string) {
  return await ExpoFingernetxusModule.setValueAsync(value);
}

const emitter = new EventEmitter(ExpoFingernetxusModule ?? NativeModulesProxy.ExpoFingernetxus);

export function addChangeListener(listener: (event: ChangeEventPayload) => void): Subscription {
  return emitter.addListener<ChangeEventPayload>('onChange', listener);
}

export { ExpoFingernetxusView, ExpoFingernetxusViewProps, ChangeEventPayload };
