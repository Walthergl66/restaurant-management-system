/**
 * Estación Burger — Input con estilo neón
 */

import React, { useRef, useState } from 'react';
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

  // El resaltado de foco se aplica de forma IMPERATIVA sobre el contenedor,
  // SIN setState. En Android, un setState en onFocus/onBlur re-renderizaba el
  // Input en cada foco y reestablecía el EditText nativo (ATTACH_NEW_INPUT),
  // lo que hacía perder pulsaciones y cerrar el teclado. Sin re-render, el
  // foco nativo se mantiene estable y se puede escribir con normalidad.
  const containerRef = useRef<View>(null);

  const aplicarFoco = (activo: boolean) => {
    containerRef.current?.setNativeProps({
      style: activo ? styles.inputContainerFocused : styles.inputContainerBlurred,
    });
  };

  return (
    <View style={[styles.container, compact && styles.containerCompact]}>
      {label && <Text style={[styles.label, compact && styles.labelCompact]}>{label}</Text>}
      <View
        ref={containerRef}
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
          onFocus={(e) => { aplicarFoco(true); onFocus?.(e); }}
          onBlur={(e) => { aplicarFoco(false); onBlur?.(e); }}
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
  inputContainerFocused: {
    borderColor: colors.neonOrange,
    shadowColor: colors.neonOrange,
    shadowOffset: { width: 0, height: 0 },
    shadowOpacity: 0.3,
    shadowRadius: 8,
    elevation: 4,
  },
  // Estado base del borde (sin foco): permite revertir el resaltado aplicado
  // imperativamente en onBlur sin recurrir a setState.
  inputContainerBlurred: {
    borderColor: colors.surfaceBorder,
    shadowOpacity: 0,
    elevation: 0,
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
