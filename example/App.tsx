import { StyleSheet, Text, View } from 'react-native';

import * as ExpoFingernetxus from 'expo-fingernetxus';

export default function App() {
  return (
    <View style={styles.container}>
      <Text>{ExpoFingernetxus.hello()}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#fff',
    alignItems: 'center',
    justifyContent: 'center',
  },
});
