import * as React from 'react';

import { ExpoFingernetxusViewProps } from './ExpoFingernetxus.types';

export default function ExpoFingernetxusView(props: ExpoFingernetxusViewProps) {
  return (
    <div>
      <span>{props.name}</span>
    </div>
  );
}
