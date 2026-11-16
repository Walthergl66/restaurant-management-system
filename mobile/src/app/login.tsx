/**
 * Estación Burger — Login
 * Acceso móvil de Estación Burger
 */

import React, { useEffect, useRef, useState } from 'react';
import {
  AccessibilityInfo,
  Animated,
  Easing,
  View,
  Text,
  StyleSheet,
  Image,
  TextInput,
  Pressable,
  ActivityIndicator,
  TouchableOpacity,
  KeyboardAvoidingView,
  Platform,
  ScrollView,
  Dimensions,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useRouter } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { useFonts } from 'expo-font';
import { useAuth } from '@/features/auth';
import { ApiError } from '@/shared/api/apiError';
import logo from '../../assets/login-logo-web.png';
import background from '../../assets/login-brand-background.png';

export default function LoginScreen() {
  const router = useRouter();
  const [fontsLoaded] = useFonts({
    KaushanScript: require('../../assets/KaushanScript-Regular.ttf'),
    Lato: require('../../assets/Lato-Regular.ttf'),
    LatoBold: require('../../assets/Lato-Bold.ttf'),
  });
  const logoOpacity = useRef(new Animated.Value(0)).current;
  const logoScale = useRef(new Animated.Value(0.97)).current;
  const introOpacity = useRef(new Animated.Value(0)).current;
  const introOffset = useRef(new Animated.Value(10)).current;
  const formOpacity = useRef(new Animated.Value(0)).current;
  const formOffset = useRef(new Animated.Value(12)).current;
  // Dimensiones de PANTALLA fijadas una sola vez (no de ventana): en Android
  // la ventana se encoge al abrir el teclado y, con useWindowDimensions, eso
  // re-renderizaba el formulario y hacía rebotar el foco entre email y
  // contraseña. La pantalla no cambia con el teclado, y congelarla con el
  // inicializador de useState evita todo re-render durante la escritura.
  const [{ width, height }] = useState(() => Dimensions.get('screen'));
  const compact = height < 740;
  const logoHeight = Math.min(compact ? 155 : 190, (width - 80) * 1199 / 1312);
  const logoWidth = logoHeight * 1312 / 1199;
  const { login } = useAuth();

  useEffect(() => {
    let cancelled = false;
    let entrance: Animated.CompositeAnimation | undefined;

    AccessibilityInfo.isReduceMotionEnabled().then((reduceMotion) => {
      if (cancelled) return;
      if (reduceMotion) {
        logoOpacity.setValue(1);
        logoScale.setValue(1);
        introOpacity.setValue(1);
        introOffset.setValue(0);
        formOpacity.setValue(1);
        formOffset.setValue(0);
        return;
      }

      const fadeIn = (value: Animated.Value, duration: number) =>
        Animated.timing(value, {
          toValue: 1,
          duration,
          easing: Easing.out(Easing.cubic),
          useNativeDriver: true,
        });
      const riseIn = (value: Animated.Value, duration: number) =>
        Animated.timing(value, {
          toValue: 0,
          duration,
          easing: Easing.out(Easing.cubic),
          useNativeDriver: true,
        });

      entrance = Animated.sequence([
        Animated.parallel([
          fadeIn(logoOpacity, 340),
          Animated.timing(logoScale, {
            toValue: 1,
            duration: 420,
            easing: Easing.out(Easing.cubic),
            useNativeDriver: true,
          }),
        ]),
        Animated.stagger(60, [
          Animated.parallel([
            fadeIn(introOpacity, 250),
            riseIn(introOffset, 250),
          ]),
          Animated.parallel([
            fadeIn(formOpacity, 290),
            riseIn(formOffset, 290),
          ]),
        ]),
      ]);
      entrance.start();
    });

    return () => {
      cancelled = true;
      entrance?.stop();
    };
  }, [formOffset, formOpacity, introOffset, introOpacity, logoOpacity, logoScale]);

  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const handleLogin = async () => {
    if (!username.trim() || !password.trim()) {
      setError('Ingresa usuario y contraseña');
      return;
    }
    setError(null);
    setLoading(true);
    try {
      await login(username.trim(), password);
      router.replace('/(tabs)');
    } catch (err) {
      if (
        err instanceof ApiError &&
        err.type === 'urn:problem:restaurante:correo-no-verificado'
      ) {
        // La cuenta existe pero falta verificar el correo: llevar al paso 2.
        router.push({
          pathname: '/verificar',
          params: { correo: username.trim().toLowerCase() },
        });
        return;
      }
      setError(err instanceof ApiError ? err.message : 'Error al iniciar sesión');
    } finally {
      setLoading(false);
    }
  };

  return (
    <SafeAreaView style={styles.container} edges={['top', 'left', 'right']}>
      <KeyboardAvoidingView style={styles.container} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        <ScrollView contentContainerStyle={styles.scrollContent} keyboardShouldPersistTaps="handled">
          <View style={[styles.hero, compact && styles.heroCompact]}>
            <Image source={background} style={[styles.heroImage, { width, height: width * 1.5 }]} resizeMode="cover" accessible={false} />
            <Animated.View style={{ opacity: logoOpacity, transform: [{ scale: logoScale }] }}>
              <Image source={logo} style={[styles.logo, { width: logoWidth, height: logoHeight }]} resizeMode="contain" accessibilityLabel="Estación Burger" />
            </Animated.View>
            <View pointerEvents="none" style={[styles.diagonal, { width: width * 2, left: -width / 2 }]} />
          </View>
          <View style={styles.formSurface}>
            <View style={styles.content}>
              <Animated.View style={[styles.intro, { opacity: introOpacity, transform: [{ translateY: introOffset }] }]}>
                <Text style={[styles.welcome, fontsLoaded && styles.welcomeFont]}>Bienvenido</Text>
                <View style={styles.underline} />
              </Animated.View>
              <Animated.View style={{ opacity: formOpacity, transform: [{ translateY: formOffset }] }}>
                <Text style={[styles.label, fontsLoaded && styles.boldFont]}>Correo o usuario</Text>
                <View style={styles.field}>
                  <Ionicons name="person-outline" size={21} color="#302923" />
                  <TextInput accessibilityLabel="Correo o usuario" style={[styles.input, fontsLoaded && styles.bodyFont]} placeholder="Tu correo o usuario" placeholderTextColor="#817970" value={username} onChangeText={setUsername} autoCapitalize="none" autoCorrect={false} autoComplete="username" />
                </View>
                <Text style={[styles.label, fontsLoaded && styles.boldFont]}>Contraseña</Text>
                <View style={styles.field}>
                  <Ionicons name="lock-closed-outline" size={21} color="#302923" />
                  <TextInput accessibilityLabel="Contraseña" style={[styles.input, fontsLoaded && styles.bodyFont]} placeholder="Tu contraseña" placeholderTextColor="#817970" value={password} onChangeText={setPassword} secureTextEntry={!showPassword} autoComplete="current-password" />
                  <TouchableOpacity accessibilityRole="button" accessibilityLabel={showPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'} onPress={() => setShowPassword(!showPassword)} style={styles.eyeButton}>
                    <Ionicons name={showPassword ? 'eye-off-outline' : 'eye-outline'} size={21} color="#302923" />
                  </TouchableOpacity>
                </View>
                {error && <Text accessibilityRole="alert" style={[styles.error, fontsLoaded && styles.bodyFont]}>{error}</Text>}
                <TouchableOpacity accessibilityRole="button" style={styles.forgotButton} onPress={() => router.push('/recuperar')}>
                  <Text style={[styles.link, fontsLoaded && styles.boldFont]}>¿Olvidaste tu contraseña?</Text>
                </TouchableOpacity>
                <Pressable accessibilityRole="button" accessibilityState={{ disabled: loading, busy: loading }} disabled={loading} onPress={handleLogin} style={({ pressed }) => [styles.loginButton, pressed && styles.loginPressed, loading && styles.loginLoading]}>
                  {loading ? <ActivityIndicator color="#201914" /> : <Text style={[styles.loginText, fontsLoaded && styles.boldFont]}>Iniciar sesión</Text>}
                </Pressable>
                <View style={styles.accountPrompt}>
                  <Text style={[styles.accountText, fontsLoaded && styles.bodyFont]}>¿Primera vez por aquí?</Text>
                  <TouchableOpacity accessibilityRole="button" style={styles.createButton} onPress={() => router.push('/registro')}><Text style={[styles.link, fontsLoaded && styles.boldFont]}>Crear cuenta</Text></TouchableOpacity>
                </View>
              </Animated.View>
            </View>
          </View>
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#171412' },
  scrollContent: { flexGrow: 1, backgroundColor: '#f7ead9' },
  hero: { height: 310, justifyContent: 'center', alignItems: 'center', backgroundColor: '#171412', overflow: 'hidden' },
  heroCompact: { height: 245 },
  heroImage: { position: 'absolute', top: 0, left: 0 },
  diagonal: { position: 'absolute', bottom: -60, height: 110, backgroundColor: '#f7ead9', borderTopWidth: 7, borderTopColor: '#f5a000', transform: [{ rotate: '-7deg' }] },
  logo: { maxWidth: '100%' },
  formSurface: { flex: 1, backgroundColor: '#f7ead9', paddingHorizontal: 24, paddingTop: 22, paddingBottom: 24 },
  content: { width: '100%', maxWidth: 420, alignSelf: 'center' },
  intro: { alignItems: 'center', marginBottom: 20 },
  welcome: { color: '#201914', fontFamily: Platform.select({ ios: 'Snell Roundhand', android: 'cursive', default: 'cursive' }), fontSize: 43, lineHeight: 60, paddingHorizontal: 8 },
  welcomeFont: { fontFamily: 'KaushanScript' },
  bodyFont: { fontFamily: 'Lato' },
  boldFont: { fontFamily: 'LatoBold', fontWeight: 'normal' },
  underline: { width: 110, height: 4, borderRadius: 4, backgroundColor: '#f5a000', transform: [{ rotate: '-3deg' }] },
  label: { color: '#25201c', fontSize: 14, fontWeight: '700', marginBottom: 7 },
  field: { minHeight: 52, flexDirection: 'row', alignItems: 'center', borderWidth: 1, borderColor: '#b9aaa0', borderRadius: 10, backgroundColor: '#fcf8f2', paddingLeft: 14, marginBottom: 15 },
  input: { flex: 1, color: '#25201c', fontSize: 16, paddingHorizontal: 12, paddingVertical: 11, minWidth: 0 },
  eyeButton: { width: 48, height: 48, alignItems: 'center', justifyContent: 'center' },
  error: { color: '#b42336', fontSize: 13, marginBottom: 4 },
  forgotButton: { alignSelf: 'flex-end', minHeight: 44, justifyContent: 'center', marginTop: -8, marginBottom: 6 },
  link: { color: '#b90059', fontSize: 14, fontWeight: '700' },
  loginButton: { minHeight: 54, borderRadius: 12, backgroundColor: '#f5a000', alignItems: 'center', justifyContent: 'center' },
  loginPressed: { opacity: 0.8, transform: [{ scale: 0.99 }] },
  loginLoading: { opacity: 0.75 },
  loginText: { color: '#201914', fontSize: 16, fontWeight: '700' },
  accountPrompt: { flexDirection: 'row', flexWrap: 'wrap', alignItems: 'center', justifyContent: 'center', marginTop: 10 },
  accountText: { color: '#514840', fontSize: 14 },
  createButton: { minHeight: 44, justifyContent: 'center', paddingHorizontal: 8 },
});
