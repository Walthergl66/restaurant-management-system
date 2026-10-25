/**
 * Estación Burger — Input con estilo neón
 */

import React, { useState } from 'react';
import {
  View,
  TextInput,
  Text,
  StyleSheet,
  TextInputProps,
  TouchableOpacity,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { colors } from '@/theme/colors';

interface InputProps extends TextInputProps {
  label?: string;
  error?: string;
  icon?: keyof typeof Ionicons.glyphMap;
  isPassword?: boolean;
  compact?: boolean;
}

export function Input({
  label,
  error,
  icon,
  isPassword,
  compact = false,
  style,
  onFocus,
  onBlur,
  ...props
}: InputProps) {
  const [showPassword, setShowPassword] = useState(false);

  return (
    <View style={[styles.container, compact && styles.containerCompact]}>
      {label && <Text style={[styles.label, compact && styles.labelCompact]}>{label}</Text>}
      <View
        style={[
          styles.inputContainer,
          compact && styles.inputContainerCompact,
          error && styles.inputContainerError,
        ]}
      >
        {icon && (
          <Ionicons
            name={icon}
            size={20}
            color={colors.textMuted}
            style={styles.icon}
          />
        )}
        <TextInput
          style={[styles.input, compact && styles.inputCompact, style]}
          placeholderTextColor={colors.textMuted}
          onFocus={onFocus}
          onBlur={onBlur}
          secureTextEntry={isPassword && !showPassword}
          {...props}
        />
        {isPassword && (
          <TouchableOpacity
            onPress={() => setShowPassword(!showPassword)}
            style={styles.eyeButton}
          >
            <Ionicons
              name={showPassword ? 'eye-off' : 'eye'}
              size={20}
              color={colors.textMuted}
            />
          </TouchableOpacity>
        )}
      </View>
      {error && <Text style={styles.error}>{error}</Text>}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    marginBottom: 16,
  },
  containerCompact: {
    marginBottom: 8,
  },
  labelCompact: {
    fontSize: 11,
    lineHeight: 14,
    marginBottom: 4,
  },
  inputContainerCompact: {
    minHeight: 44,
    paddingHorizontal: 12,
  },
  inputCompact: {
    minWidth: 0,
    paddingVertical: 10,
    lineHeight: 20,
  },
  label: {
    color: colors.textSecondary,
    fontSize: 13,
    fontWeight: '600',
    marginBottom: 8,
    textTransform: 'uppercase',
    letterSpacing: 0.5,
  },
  inputContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.surface,
    borderRadius: 14,
    borderWidth: 1,
    borderColor: colors.surfaceBorder,
    paddingHorizontal: 16,
  },
  inputContainerError: {
    borderColor: colors.error,
  },
  icon: {
    marginRight: 12,
  },
  input: {
    flex: 1,
    color: colors.textPrimary,
    fontSize: 16,
    paddingVertical: 16,
  },
  eyeButton: {
    padding: 4,
  },
  error: {
    color: colors.error,
    fontSize: 12,
    marginTop: 6,
    marginLeft: 4,
  },
});
