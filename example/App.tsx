import { StyleSheet, Text, View, TouchableOpacity, FlatList } from 'react-native';
import { useState } from 'react';

import * as ExpoFingernetxus from 'expo-fingernetxus';

export default function App() {
  const [result, setResult] = useState<boolean | false>();
  const [devicesList, setDevicesList] = useState<[]>();
  
  const requestBluetoothPermissionRN = () => {
   const devicesResult = ExpoFingernetxus.requestBluetoothPermission();
   console.log(devicesList);
   setDevicesList(devicesResult);
    setResult(true);
  };


  return (
    <View style={styles.container}>
      <Text>Permission Status: {result}</Text>
      <TouchableOpacity onPress={requestBluetoothPermissionRN} 
      style={styles.button}
      >
        <Text style={styles.buttonText} >Request Permission</Text>
      </TouchableOpacity>
      <Text>Devices Found:</Text>
      
      <FlatList
        data={devicesList}
        renderItem={({ item }) => <Text>{item}</Text>}
      />



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
  button: {
    backgroundColor: 'blue',
    padding: 20,
    borderRadius: 10,
  },
  buttonText: {
    color: 'white',
    fontSize: 20,
  }
});
