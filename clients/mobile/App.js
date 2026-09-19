import React, { useMemo, useState } from 'react';
import {
  SafeAreaView,
  View,
  Text,
  Pressable,
  StyleSheet,
  StatusBar,
  Platform,
} from 'react-native';
import { WebView } from 'react-native-webview';
import { getUrls } from './config';

export default function App() {
  const urls = useMemo(() => getUrls(), []);
  const [entry, setEntry] = useState(null);

  if (entry) {
    const uri = entry === 'admin' ? urls.admin : urls.user;
    return (
      <SafeAreaView style={styles.safe}>
        <StatusBar barStyle="light-content" />
        <View style={styles.webHeader}>
          <Pressable onPress={() => setEntry(null)} hitSlop={12}>
            <Text style={styles.back}>返回入口选择</Text>
          </Pressable>
          <Text style={styles.webTitle}>
            {entry === 'admin' ? '管理入口' : '用户入口'}
          </Text>
        </View>
        <WebView
          source={{ uri }}
          style={styles.webview}
          startInLoadingState
          allowsBackForwardNavigationGestures
        />
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={styles.safe}>
      <StatusBar barStyle="light-content" />
      <View style={styles.shell}>
        <Text style={styles.product}>BGSSAI Short</Text>
        <Text style={styles.title}>选择入口</Text>
        <Text style={styles.hint}>
          一个 App，两个登录入口。入口只切换 Web 基址与权限面。
        </Text>
        <View style={styles.entries}>
          <Pressable
            style={({ pressed }) => [styles.entry, pressed && styles.entryPressed]}
            onPress={() => setEntry('user')}
          >
            <Text style={styles.entryTitle}>用户入口</Text>
            <Text style={styles.entryDesc}>创作者制作短剧</Text>
          </Pressable>
          <Pressable
            style={({ pressed }) => [styles.entry, pressed && styles.entryPressed]}
            onPress={() => setEntry('admin')}
          >
            <Text style={styles.entryTitle}>管理入口</Text>
            <Text style={styles.entryDesc}>运营与内容管理</Text>
          </Pressable>
        </View>
        <Text style={styles.meta}>
          env: {urls.env}
          {Platform.OS ? ` · ${Platform.OS}` : ''}
        </Text>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: {
    flex: 1,
    backgroundColor: '#0f1419',
  },
  shell: {
    flex: 1,
    paddingHorizontal: 24,
    paddingTop: 48,
    justifyContent: 'center',
    gap: 16,
  },
  product: {
    color: '#e8a54b',
    letterSpacing: 1.2,
    fontSize: 13,
    fontWeight: '600',
    textTransform: 'uppercase',
  },
  title: {
    color: '#f2f4f7',
    fontSize: 32,
    fontWeight: '700',
    letterSpacing: -0.5,
  },
  hint: {
    color: '#9aa7b5',
    fontSize: 15,
    lineHeight: 22,
    marginBottom: 12,
  },
  entries: {
    gap: 12,
  },
  entry: {
    borderWidth: 1,
    borderColor: 'rgba(242,244,247,0.12)',
    backgroundColor: 'rgba(255,255,255,0.03)',
    borderRadius: 12,
    paddingVertical: 20,
    paddingHorizontal: 18,
  },
  entryPressed: {
    backgroundColor: 'rgba(255,255,255,0.08)',
  },
  entryTitle: {
    color: '#f2f4f7',
    fontSize: 20,
    fontWeight: '700',
    marginBottom: 6,
  },
  entryDesc: {
    color: '#9aa7b5',
    fontSize: 14,
  },
  meta: {
    marginTop: 20,
    color: '#6b7785',
    fontSize: 12,
  },
  webHeader: {
    paddingHorizontal: 16,
    paddingVertical: 10,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: 'rgba(242,244,247,0.12)',
    flexDirection: 'row',
    alignItems: 'center',
    gap: 16,
  },
  back: {
    color: '#3d8bfd',
    fontSize: 14,
    fontWeight: '600',
  },
  webTitle: {
    color: '#f2f4f7',
    fontSize: 15,
    fontWeight: '600',
  },
  webview: {
    flex: 1,
    backgroundColor: '#0f1419',
  },
});
