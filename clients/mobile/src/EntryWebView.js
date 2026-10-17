import React, { useCallback, useRef, useState } from 'react';
import { ActivityIndicator, Pressable, StyleSheet, Text, View } from 'react-native';
import { WebView } from 'react-native-webview';
import AsyncStorage from '@react-native-async-storage/async-storage';

const ACCOUNT_PATH = '/account';

export default function EntryWebView({ entry, onBack }) {
  const ref = useRef(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [pathHint, setPathHint] = useState('');

  const base = entry.url.replace(/\/$/, '');

  const openAccount = useCallback(() => {
    setError(null);
    setPathHint(ACCOUNT_PATH);
    ref.current?.injectJavaScript(
      `window.location.href=${JSON.stringify(base + ACCOUNT_PATH)}; true;`,
    );
  }, [base]);

  const reload = useCallback(() => {
    setError(null);
    setLoading(true);
    ref.current?.reload();
  }, []);

  return (
    <View style={styles.wrap}>
      <View style={styles.bar}>
        <Pressable onPress={onBack} hitSlop={12} style={styles.backBtn}>
          <Text style={styles.backText}>入口</Text>
        </Pressable>
        <Text style={styles.title} numberOfLines={1}>
          {entry.label}{pathHint ? ` · ${pathHint}` : ''}
        </Text>
        <Pressable onPress={openAccount} hitSlop={12} style={styles.backBtn}>
          <Text style={styles.backText}>账号</Text>
        </Pressable>
      </View>
      {error ? (
        <View style={styles.errorBox}>
          <Text style={styles.errorTitle}>网络或会话异常</Text>
          <Text style={styles.errorBody}>{error}</Text>
          <Pressable onPress={reload} style={styles.retry}>
            <Text style={styles.retryText}>重试</Text>
          </Pressable>
        </View>
      ) : (
        <WebView
          ref={ref}
          source={{ uri: entry.url }}
          style={styles.web}
          startInLoadingState
          sharedCookiesEnabled
          thirdPartyCookiesEnabled
          allowsBackForwardNavigationGestures
          onLoadStart={() => setLoading(true)}
          onLoadEnd={() => setLoading(false)}
          onError={(e) => {
            setLoading(false);
            setError(e?.nativeEvent?.description || '加载失败');
          }}
          onHttpError={(e) => {
            if (e?.nativeEvent?.statusCode >= 500) {
              setError('服务暂时不可用（' + e.nativeEvent.statusCode + '）');
            }
          }}
          onNavigationStateChange={(nav) => {
            try {
              const u = new URL(nav.url);
              setPathHint(u.pathname || '');
              AsyncStorage.setItem('bgssai.short.lastPath', u.pathname).catch(() => {});
            } catch (_) {
              // ignore
            }
          }}
        />
      )}
      {loading && !error ? (
        <View style={styles.loading}>
          <ActivityIndicator color="#2d8cff" />
        </View>
      ) : null}
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
  loading: {
    position: 'absolute',
    left: 0,
    right: 0,
    bottom: 24,
    alignItems: 'center',
  },
  errorBox: {
    flex: 1,
    padding: 24,
    justifyContent: 'center',
  },
  errorTitle: {
    color: '#f2f5f8',
    fontSize: 18,
    fontWeight: '700',
    marginBottom: 8,
  },
  errorBody: {
    color: '#9aa3ad',
    marginBottom: 16,
  },
  retry: {
    alignSelf: 'flex-start',
    backgroundColor: '#2d8cff',
    paddingHorizontal: 14,
    paddingVertical: 8,
    borderRadius: 6,
  },
  retryText: {
    color: '#fff',
    fontWeight: '600',
  },
});
