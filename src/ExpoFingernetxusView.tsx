import { requireNativeViewManager } from 'expo-modules-core';
import * as React from 'react';

import { ExpoFingernetxusViewProps } from './ExpoFingernetxus.types';

const NativeView: React.ComponentType<ExpoFingernetxusViewProps> =
  requireNativeViewManager('ExpoFingernetxus');

export default function ExpoFingernetxusView(props: ExpoFingernetxusViewProps) {
  return <NativeView {...props} />;
}
