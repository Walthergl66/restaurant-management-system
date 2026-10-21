/**
 * Estación Burger — Input con estilo neón
 */

import React, { memo, useCallback, useEffect, useRef, useState } from 'react';
import {
  View,
  TextInput,
  Text,
  StyleSheet,
  TextInputProps,
  StyleProp,
  TextStyle,
  FocusEvent,
  BlurEvent,
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

/**
 * Campo de texto memoizado.
 *
 * Se separa del contenedor para que el re-render del BORDE (al ganar/perder
 * foco) NO alcance al <TextInput>. En Android, tocar el <TextInput> obliga a
 * React Native a reconciliar el EditText nativo (ATTACH_NEW_INPUT) y eso
 * hace perder pulsaciones y cerrar el teclado. Con el memo, el <TextInput>
 * solo se re-renderiza cuando cambian sus props reales (value, placeholder,
 * etc.), nunca por el estado de foco del contenedor.
 *
 * `onFocus`/`onBlur` NO forman parte de `inputProps`: se inyectan aparte y son
 * estables (useCallback), de modo que un handler inline del padre no rompe el
 * memo.
 */
interface InputFieldProps {
  compact: boolean;
  inputStyle?: StyleProp<TextStyle>;
  isPassword: boolean;
  showPassword: boolean;
  onTogglePassword: () => void;
  onFocus: (e: FocusEvent) => void;
  onBlur: (e: BlurEvent) => void;
  inputProps: TextInputProps;
}

/** Comparación superficial de las props sueltas del <TextInput>. */
function shallowEqualInputProps(a: TextInputProps, b: TextInputProps): boolean {
  const aKeys = Object.keys(a) as (keyof TextInputProps)[];
  const bKeys = Object.keys(b) as (keyof TextInputProps)[];
  if (aKeys.length !== bKeys.length) return false;
  for (const key of aKeys) {
    if (!Object.is(a[key], b[key])) return false;
  }
  return true;
}

function areInputFieldPropsEqual(
  prev: InputFieldProps,
  next: InputFieldProps
): boolean {
  return (
    prev.compact === next.compact &&
    prev.inputStyle === next.inputStyle &&
    prev.isPassword === next.isPassword &&
    prev.showPassword === next.showPassword &&
    prev.onTogglePassword === next.onTogglePassword &&
    prev.onFocus === next.onFocus &&
    prev.onBlur === next.onBlur &&
    shallowEqualInputProps(prev.inputProps, next.inputProps)
  );
}

const InputField = memo(function InputField({
  compact,
  inputStyle,
  isPassword,
  showPassword,
  onTogglePassword,
  onFocus,
  onBlur,
  inputProps,
}: InputFieldProps) {
  return (
    <>
      <TextInput
        style={[styles.input, compact && styles.inputCompact, inputStyle]}
        placeholderTextColor={colors.textMuted}
        onFocus={onFocus}
        onBlur={onBlur}
        secureTextEntry={isPassword && !showPassword}
        {...inputProps}
      />
      {isPassword && (
        <TouchableOpacity onPress={onTogglePassword} style={styles.eyeButton}>
          <Ionicons
            name={showPassword ? 'eye-off' : 'eye'}
            size={20}
            color={colors.textMuted}
          />
        </TouchableOpacity>
      )}
    </>
  );
}, areInputFieldPropsEqual);

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
  const [focused, setFocused] = useState(false);
  const [showPassword, setShowPassword] = useState(false);

  // Handlers del padre guardados en un ref: llegan por props (y su identidad
  // puede cambiar en cada render del padre) pero NO forman parte de las props
  // del <TextInput> memoizado, así que no lo invalidan.
  const externalHandlers = useRef({ onFocus, onBlur });
  useEffect(() => {
    externalHandlers.current = { onFocus, onBlur };
  }, [onFocus, onBlur]);

  const handleFocus = useCallback((e: FocusEvent) => {
    setFocused(true);
    externalHandlers.current.onFocus?.(e);
  }, []);

  const handleBlur = useCallback((e: BlurEvent) => {
    setFocused(false);
    externalHandlers.current.onBlur?.(e);
  }, []);

  const togglePassword = useCallback(() => {
    setShowPassword((prev) => !prev);
  }, []);

  return (
    <View style={[styles.container, compact && styles.containerCompact]}>
      {label && <Text style={[styles.label, compact && styles.labelCompact]}>{label}</Text>}
      <View
        style={[
          styles.inputContainer,
          compact && styles.inputContainerCompact,
          focused && styles.inputContainerFocused,
          error && styles.inputContainerError,
        ]}
      >
        {icon && (
          <Ionicons
            name={icon}
            size={20}
            color={focused ? colors.neonOrange : colors.textMuted}
            style={styles.icon}
          />
        )}
        <InputField
          compact={compact}
          inputStyle={style}
          isPassword={isPassword ?? false}
          showPassword={showPassword}
          onTogglePassword={togglePassword}
          onFocus={handleFocus}
          onBlur={handleBlur}
          inputProps={props}
        />
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
