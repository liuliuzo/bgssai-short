import React from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { WebView } from 'react-native-webview';

export default function EntryWebView({ entry, onBack }) {
  return (
    <View style={styles.wrap}>
      <View style={styles.bar}>
        <Pressable onPress={onBack} hitSlop={12} style={styles.backBtn}>
          <Text style={styles.backText}>入口</Text>
        </Pressable>
        <Text style={styles.title} numberOfLines={1}>
          {entry.label}
        </Text>
      </View>
      <WebView
        source={{ uri: entry.url }}
        style={styles.web}
        startInLoadingState
        allowsBackForwardNavigationGestures
      />
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: {
    flex: 1,
    backgroundColor: '#0f1419',
  },
  bar: {
    height: 48,
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 12,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: 'rgba(242,245,248,0.12)',
    gap: 12,
  },
  backBtn: {
    paddingVertical: 6,
    paddingHorizontal: 8,
  },
  backText: {
    color: '#2d8cff',
    fontSize: 16,
    fontWeight: '600',
  },
  title: {
    flex: 1,
    color: '#f2f5f8',
    fontSize: 16,
    fontWeight: '600',
  },
  web: {
    flex: 1,
    backgroundColor: '#ffffff',
  },
});
