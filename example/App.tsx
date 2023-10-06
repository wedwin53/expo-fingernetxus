import { StyleSheet, Text, View, TouchableOpacity, FlatList, Image, SafeAreaView } from 'react-native';
import { useState, useEffect } from 'react';

import * as ExpoFingernetxus from 'expo-fingernetxus';

export default function App() {
  const [result, setResult] = useState<boolean | false>(false);
  const [devicesList, setDevicesList] = useState<[]>();
  const [devicesMap, setDevicesMap] = useState<Map<string, string> | any>(new Map());
  const [base64Image, setBase64Image] = useState<string | any>("");
  const [candidateAddress, setCandidateAddress] = useState<string | any>("");
  const [template, setTemplate] = useState<string | any>("");
  const [enrolResult, setEnrolResult] = useState<string | any>("");
  
  const requestBluetoothPermissionRN = () => {
   const devicesResult = ExpoFingernetxus.requestBluetoothPermission();
   console.log({devicesList});
   console.log({devicesMap})
   setDevicesList(parseDevicesList(devicesResult));
   setResult(true);
  };

  const connectToDevice = async (deviceName: string) => {
    console.log({deviceName})
    const address = devicesMap.get(deviceName);
    console.log({address});
    const response = await ExpoFingernetxus.connectToDevice(address);
    console.log({response});
    setResult(true)
  }

  const captureFingerprint = async () => {
    const response = await ExpoFingernetxus.captureFingerprintImage();
    console.log({response});
    setBase64Image(response);
  }

  const parseDevicesList = (devicesList: any) => {
    const deciveMap = new Map();
    const parsedDevicesList = devicesList.map((device: any) => {
      const dList = device.split(",")
      const name = dList[0];
      const address = dList[1];
      deciveMap.set(name, address);
      return name;
    })
    setDevicesMap(deciveMap);
    return parsedDevicesList;
  }

  useEffect(() => {
    const sub = ExpoFingernetxus.addFingerprintCaptureListener((event: any) => {
      console.log({event});
      setBase64Image(event?.image);
    });

    const subBTState = ExpoFingernetxus.addBluetoothStateChangeListener((event: any) => {
      console.log({event});
    });


    return () => {
      sub.remove();
      subBTState.remove();
    }
  }, [])

  useEffect(() => {
    const btcOn = ExpoFingernetxus.requestTurnOnBluetooth();
    console.log({btcOn});
  }, [])

  useEffect(() => {
    const sub = ExpoFingernetxus.addFingerprintCaptureTemplateListener((event: any) => {
      console.log({captureScore: event});
      setTemplate(event?.captureScore);
    });
    return () => {
      sub.remove();
    }
  }, [])

  useEffect(() => {
    const sub = ExpoFingernetxus.addEnrolTemplateListener((event: any) => {
      console.log({enrolResult: event});
      setEnrolResult(event?.enrolResult);
    });
    return () => {
      sub.remove();
    }
  }, [])

  const captureTemplate = async () => {
    const response = await ExpoFingernetxus.captureFingerprintTemplate();
    console.log({captureTemplate:response});    
  }

  const enrolTemplate = async () => {
    const response = await ExpoFingernetxus.onEnrolTemplateAsync();
    console.log({enrolTemplate:response});
  }

  const getBTState = () => {
    const response = ExpoFingernetxus.getBluetoothState();
    console.log({getBTState: response});
  }

  const enrollTemplateOndemand = async (template: string) => {
    const response = await ExpoFingernetxus.enrolTemplateOnDemand(template);
    console.log({enrolTemplate:response});
  }

  const handleBluetoothOn = () => {
    const response = ExpoFingernetxus.requestTurnOnBluetooth();
    console.log({response});
  }

  const handleHostBTState = () => {
    const response = ExpoFingernetxus.getHostBluetoothState();
    console.log({response});
  }


  return (
    <SafeAreaView style={styles.container}>
      <Text>Permission Status: {result}</Text>
      <View style={styles.viewContainer}>

      
      <TouchableOpacity onPress={requestBluetoothPermissionRN} 
      style={styles.button}
      >
        <Text style={styles.buttonText} >Request Permission</Text>
      </TouchableOpacity>

      <TouchableOpacity onPress={getBTState} 
      style={styles.button}
      >
        <Text style={styles.buttonText} >Get Device Connection</Text>
      </TouchableOpacity>

      {result && <TouchableOpacity onPress={captureFingerprint}
      style={styles.button}
      >
        <Text style={styles.buttonText} >Capture Fingerprint</Text>
      </TouchableOpacity>}
      {/* Template Capture */}
      {result && <TouchableOpacity onPress={captureTemplate}
      style={styles.button}
      >
        <Text style={styles.buttonText} >Capture Template</Text>
      </TouchableOpacity>}
      {/* Template Enrol */}
      {result && <TouchableOpacity onPress={enrolTemplate}
      style={styles.button}
      > 
        <Text style={styles.buttonText} >Enrol Template</Text>
      </TouchableOpacity>}
      {/* Turn on the BT */}
      <TouchableOpacity onPress={handleBluetoothOn}
      style={styles.button}
      > 
        <Text style={styles.buttonText} >Turn ON BT</Text>
      </TouchableOpacity>

      <TouchableOpacity onPress={handleHostBTState}
      style={styles.button}
      > 
        <Text style={styles.buttonText} >Get BT State</Text>
      </TouchableOpacity>

      </View>
      
      <View style={styles.imagesContainer}>

      <View style={{height: 100, width: 100, backgroundColor: 'ligthgray'}}>
        <Text>Image</Text>
        {
          base64Image &&
        <Image source={{uri: base64Image}} style={{height: 100, width: 100}} />

        }
      </View>
      
      </View>
        {/* enrolment result */}
      <View style={{height: 50, width: 200, backgroundColor: 'ligthgray', marginBottom:30}}>
        <Text>Enrolment Result</Text>
        {
          enrolResult &&
        <Text>{enrolResult}</Text>
        }
        <Text>Template</Text>
        {
          template &&
        <Text>{template}</Text>
        }
      </View>
      
      
      <Text>Devices Found:</Text>
      
      <FlatList
      style={{width: '80%'}}
        data={devicesList}
        renderItem={({ item }) => <DeviceCard name={item} handleConnect={connectToDevice} />} 
      />

    </SafeAreaView>
  );
}

function DeviceCard (props: any) {

  return (
    <TouchableOpacity style={styles.deviceCard} onPress={()=> props.handleConnect(props.name)}>
      <Text>{props.name}</Text>
    </TouchableOpacity>
  )
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
    width: '40%',
    margin: 5
  },
  buttonText: {
    color: 'white',
    fontSize: 20,
  },
  deviceCard: {
    backgroundColor: 'lightblue',
    padding: 20,
    borderRadius: 10,
    margin: 5
  },
  viewContainer: {
    flexDirection: 'row',
    justifyContent: 'center',
    flexWrap: 'wrap',
    width: '100%',
    padding: 20
  },
  imagesContainer: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    width: '100%',
    padding: 20
  }

});
