import React, { useCallback, useState } from 'react';
import { StatusBar } from 'expo-status-bar';
import { SafeAreaView, StyleSheet, View } from 'react-native';
import EntryGate from './src/EntryGate';
import EntryWebView from './src/EntryWebView';
import { getEntries } from './src/config';

export default function App() {
  const bootstrap = getEntries();
  const [active, setActive] = useState(null);

  const onSelect = useCallback((entry) => {
    setActive(entry);
  }, []);

  const onBack = useCallback(() => {
    setActive(null);
  }, []);

  return (
    <SafeAreaView style={styles.safe}>
      <StatusBar style="light" />
      <View style={styles.root}>
        {active ? (
          <EntryWebView entry={active} onBack={onBack} />
        ) : (
          <EntryGate
            profile={bootstrap.profile}
            entries={bootstrap.entries}
            onSelect={onSelect}
          />
        )}
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: {
    flex: 1,
    backgroundColor: '#0f1419',
  },
  root: {
    flex: 1,
  },
});
