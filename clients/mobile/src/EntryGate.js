import React from 'react';
import {
  Pressable,
  StyleSheet,
  Text,
  View,
} from 'react-native';

export default function EntryGate({ profile, entries, onSelect }) {
  return (
    <View style={styles.gate}>
      <Text style={styles.name}>BGSSAI Short</Text>
      <Text style={styles.sub}>选择登录入口</Text>
      <View style={styles.list}>
        {entries.map((entry) => (
          <Pressable
            key={entry.id}
            style={({ pressed }) => [
              styles.entry,
              entry.id === 'admin' ? styles.entryAdmin : styles.entryUser,
              pressed && styles.entryPressed,
            ]}
            onPress={() => onSelect(entry)}
          >
            <Text style={styles.label}>{entry.label}</Text>
            <Text style={styles.hint}>{entry.url}</Text>
          </Pressable>
        ))}
      </View>
      <Text style={styles.meta}>
        profile={profile} · installable=single-app dual-entry
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  gate: {
    flex: 1,
    justifyContent: 'center',
    paddingHorizontal: 24,
    gap: 16,
  },
  name: {
    color: '#f2f5f8',
    fontSize: 32,
    fontWeight: '700',
    textAlign: 'center',
  },
  sub: {
    color: '#9aa8b8',
    fontSize: 16,
    textAlign: 'center',
    marginBottom: 8,
  },
  list: {
    gap: 12,
  },
  entry: {
    borderRadius: 12,
    borderWidth: 1,
    borderColor: 'rgba(242,245,248,0.12)',
    backgroundColor: 'rgba(255,255,255,0.04)',
    paddingVertical: 22,
    paddingHorizontal: 18,
  },
  entryUser: {},
  entryAdmin: {},
  entryPressed: {
    backgroundColor: 'rgba(255,255,255,0.08)',
  },
  label: {
    color: '#f2f5f8',
    fontSize: 18,
    fontWeight: '600',
  },
  hint: {
    color: '#9aa8b8',
    fontSize: 12,
    marginTop: 8,
  },
  meta: {
    color: '#9aa8b8',
    fontSize: 12,
    textAlign: 'center',
    marginTop: 12,
  },
});
