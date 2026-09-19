import React, { useMemo, useState } from 'react';
import {
  ActivityIndicator,
  Pressable,
  SafeAreaView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { StatusBar } from 'expo-status-bar';
import Constants from 'expo-constants';
import { WebView } from 'react-native-webview';

/**
 * One mobile app, two login entrances (Boss-style).
 * Env: EXPO_PUBLIC_USER_PRODUCT_URL + EXPO_PUBLIC_ADMIN_PRODUCT_URL
 */
function resolveEntranceUrls() {
  const extra =
    (Constants.expoConfig && Constants.expoConfig.extra) || {};
  const userUrl = String(
    process.env.EXPO_PUBLIC_USER_PRODUCT_URL ||
      process.env.USER_PRODUCT_URL ||
      extra.userProductUrl ||
      ''
  ).trim();
  const adminUrl = String(
    process.env.EXPO_PUBLIC_ADMIN_PRODUCT_URL ||
      process.env.ADMIN_PRODUCT_URL ||
      extra.adminProductUrl ||
      ''
  ).trim();
  return { userUrl, adminUrl };
}

export default function App() {
  const urls = useMemo(() => resolveEntranceUrls(), []);
  const [role, setRole] = useState(null);

  if (!urls.userUrl || !urls.adminUrl) {
    return (
      <SafeAreaView style={styles.center}>
        <StatusBar style="light" />
        <Text style={styles.title}>BGSSAI Short</Text>
        <Text style={styles.hint}>
          Set EXPO_PUBLIC_USER_PRODUCT_URL and EXPO_PUBLIC_ADMIN_PRODUCT_URL
          (see .env.example), then restart Expo.
        </Text>
      </SafeAreaView>
    );
  }

  if (!role) {
    return (
      <SafeAreaView style={styles.chooser}>
        <StatusBar style="light" />
        <Text style={styles.title}>BGSSAI Short</Text>
        <Text style={styles.lead}>一个应用 · 两个登录入口</Text>
        <Pressable
          style={styles.card}
          onPress={() => setRole('user')}
          accessibilityRole="button"
        >
          <Text style={styles.cardTitle}>创作者登录</Text>
          <Text style={styles.cardDesc}>进入用户端，制作短剧与成片。</Text>
        </Pressable>
        <Pressable
          style={styles.card}
          onPress={() => setRole('admin')}
          accessibilityRole="button"
        >
          <Text style={styles.cardTitle}>运营登录</Text>
          <Text style={styles.cardDesc}>进入管理端，审核与平台运营。</Text>
        </Pressable>
        <Text style={styles.foot}>不提供独立的管理端 / 用户端安装包。</Text>
      </SafeAreaView>
    );
  }

  const productUrl = role === 'admin' ? urls.adminUrl : urls.userUrl;

  return (
    <SafeAreaView style={styles.root}>
      <StatusBar style="dark" />
      <View style={styles.bar}>
        <Pressable onPress={() => setRole(null)} accessibilityRole="button">
          <Text style={styles.back}>返回入口</Text>
        </Pressable>
        <Text style={styles.barRole}>
          {role === 'admin' ? '运营' : '创作者'}
        </Text>
      </View>
      <WebView
        source={{ uri: productUrl }}
        startInLoadingState
        renderLoading={() => (
          <View style={styles.loading}>
            <ActivityIndicator size="large" color="#334155" />
          </View>
        )}
        allowsBackForwardNavigationGestures
        setSupportMultipleWindows={false}
      />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  root: {
    flex: 1,
    backgroundColor: '#ffffff',
  },
  chooser: {
    flex: 1,
    backgroundColor: '#0f172a',
    paddingHorizontal: 24,
    paddingTop: 72,
  },
  center: {
    flex: 1,
    backgroundColor: '#0f172a',
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 24,
  },
  title: {
    color: '#f8fafc',
    fontSize: 28,
    fontWeight: '700',
    marginBottom: 8,
  },
  lead: {
    color: '#cbd5e1',
    fontSize: 15,
    marginBottom: 36,
  },
  hint: {
    color: '#94a3b8',
    fontSize: 14,
    textAlign: 'center',
    lineHeight: 20,
  },
  card: {
    backgroundColor: '#ffffff',
    borderRadius: 14,
    paddingVertical: 22,
    paddingHorizontal: 20,
    marginBottom: 14,
  },
  cardTitle: {
    color: '#0f172a',
    fontSize: 18,
    fontWeight: '600',
    marginBottom: 6,
  },
  cardDesc: {
    color: '#64748b',
    fontSize: 14,
    lineHeight: 20,
  },
  foot: {
    marginTop: 20,
    color: '#94a3b8',
    fontSize: 12,
  },
  bar: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
    paddingVertical: 10,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: '#e2e8f0',
    backgroundColor: '#ffffff',
  },
  back: {
    color: '#334155',
    fontSize: 15,
    fontWeight: '600',
  },
  barRole: {
    color: '#64748b',
    fontSize: 13,
  },
  loading: {
    ...StyleSheet.absoluteFillObject,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#ffffff',
  },
});
