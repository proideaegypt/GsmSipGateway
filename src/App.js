import React, {memo, useState} from 'react';
import {
  View, Text, TextInput, TouchableOpacity,
  StyleSheet, Alert, NativeModules, ScrollView
} from 'react-native';

const {SipBridge} = NativeModules;

const Field = memo(function Field({
  label,
  value,
  onChangeText,
  secure,
  keyboardType,
}) {
  return (
    <View style={styles.field}>
      <Text style={styles.label}>{label}</Text>
      <TextInput
        style={styles.input}
        value={value}
        onChangeText={onChangeText}
        secureTextEntry={secure}
        autoCapitalize="none"
        autoCorrect={false}
        blurOnSubmit={false}
        keyboardType={keyboardType || 'default'}
        placeholderTextColor="#555"
        placeholder={label}
      />
    </View>
  );
});

export default function App() {
  const [config, setConfig] = useState({
    host: '192.168.1.100',
    port: '5060',
    username: 'android_gsm1',
    password: '',
    bridgeExtension: '1000',
    answerRings: '1',
  });
  const [status, setStatus] = useState('Not configured');

  const saveAndStart = async () => {
    try {
      const result = await SipBridge.saveConfig({
        ...config,
        port: parseInt(config.port),
        answerRings: parseInt(config.answerRings, 10) || 1,
      });
      setStatus('Running');
      Alert.alert('Success', result);
    } catch (e) {
      setStatus('Error: ' + e.message);
      Alert.alert('Error', e.message);
    }
  };

  return (
    <ScrollView style={styles.container} keyboardShouldPersistTaps="handled">
      <Text style={styles.title}>GSM SIP Gateway</Text>
      <View style={styles.statusBar}>
        <Text style={styles.statusText}>Status: {status}</Text>
      </View>
      <Text style={styles.section}>FreePBX Settings</Text>
      <Field
        label="FreePBX IP"
        value={config.host}
        onChangeText={v => setConfig(p => ({...p, host: v}))}
        keyboardType="numbers-and-punctuation"
      />
      <Field
        label="SIP Port"
        value={config.port}
        onChangeText={v => setConfig(p => ({...p, port: v}))}
        keyboardType="numeric"
      />
      <Field
        label="SIP Username"
        value={config.username}
        onChangeText={v => setConfig(p => ({...p, username: v}))}
      />
      <Field
        label="SIP Password"
        value={config.password}
        onChangeText={v => setConfig(p => ({...p, password: v}))}
        secure
      />
      <Field
        label="Bridge Target (extension or SIP URI)"
        value={config.bridgeExtension}
        onChangeText={v => setConfig(p => ({...p, bridgeExtension: v}))}
      />
      <Field
        label="Answer After Rings"
        value={config.answerRings}
        onChangeText={v => setConfig(p => ({...p, answerRings: v}))}
        keyboardType="numeric"
      />
      <TouchableOpacity style={styles.btn} onPress={saveAndStart}>
        <Text style={styles.btnText}>Save & Start Gateway</Text>
      </TouchableOpacity>
      <View style={styles.howto}>
        <Text style={styles.howtoTitle}>How it works</Text>
        <Text style={styles.howtoItem}>1. Incoming call arrives on SIM</Text>
        <Text style={styles.howtoItem}>2. App auto-answers the GSM call</Text>
        <Text style={styles.howtoItem}>3. Bridges audio to FreePBX via SIP</Text>
        <Text style={styles.howtoItem}>4. FreePBX routes the call normally</Text>
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {flex: 1, backgroundColor: '#0a0a0a', padding: 20},
  title: {color: '#fff', fontSize: 22, fontWeight: 'bold', marginTop: 50, marginBottom: 6},
  statusBar: {backgroundColor: '#1a1a2e', padding: 10, borderRadius: 8, marginBottom: 24},
  statusText: {color: '#88aaff', fontSize: 13},
  section: {color: '#888', fontSize: 12, textTransform: 'uppercase', letterSpacing: 1, marginBottom: 12},
  field: {marginBottom: 14},
  label: {color: '#aaa', marginBottom: 5, fontSize: 12},
  input: {backgroundColor: '#1e1e1e', color: '#fff', padding: 12, borderRadius: 8, borderWidth: 1, borderColor: '#2a2a2a'},
  btn: {backgroundColor: '#2563eb', padding: 16, borderRadius: 10, alignItems: 'center', marginTop: 8, marginBottom: 24},
  btnText: {color: '#fff', fontSize: 15, fontWeight: '700'},
  howto: {backgroundColor: '#111827', padding: 16, borderRadius: 10, marginBottom: 40},
  howtoTitle: {color: '#fff', fontWeight: '600', marginBottom: 10},
  howtoItem: {color: '#6b7280', marginBottom: 6, fontSize: 13},
});
