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
import logo from '../../assets/login-neon-sign.png';
import backgroundLit from '../../assets/login-brand-background.png';
import backgroundUnlit from '../../assets/login-brand-background-unlit.png';

export default function LoginScreen() {
  const router = useRouter();
  const [fontsLoaded] = useFonts({
    KaushanScript: require('../../assets/KaushanScript-Regular.ttf'),
    Lato: require('../../assets/Lato-Regular.ttf'),
    LatoBold: require('../../assets/Lato-Bold.ttf'),
  });
  const logoOpacity = useRef(new Animated.Value(0)).current;
  const logoScale = useRef(new Animated.Value(0.97)).current;
  const heroDimOpacity = useRef(new Animated.Value(1)).current;
  const lampOnOpacity = useRef(new Animated.Value(0)).current;
  const formPaperOpacity = useRef(new Animated.Value(0)).current;
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
  const layoutWidth = Math.min(width, 420);
  const compact = height < 740;
  const logoHeight = Math.min(compact ? 155 : 190, layoutWidth - 80);
  const logoWidth = logoHeight;
  const { login } = useAuth();

  useEffect(() => {
    let cancelled = false;
    let entrance: Animated.CompositeAnimation | undefined;

    AccessibilityInfo.isReduceMotionEnabled().then((reduceMotion) => {
      if (cancelled) return;
      if (reduceMotion) {
        logoOpacity.setValue(1);
        logoScale.setValue(1);
        heroDimOpacity.setValue(0.04);
        lampOnOpacity.setValue(1);
        formPaperOpacity.setValue(1);
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
      const setOpacity = (value: Animated.Value, toValue: number, duration: number) =>
        Animated.timing(value, {
          toValue,
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
        Animated.delay(180),
        Animated.parallel([
          fadeIn(logoOpacity, 150),
          Animated.timing(logoScale, {
            toValue: 1,
            duration: 250,
            easing: Easing.out(Easing.cubic),
            useNativeDriver: true,
          }),
        ]),
        Animated.sequence([
          setOpacity(logoOpacity, 0.02, 100),
          setOpacity(logoOpacity, 1, 140),
          setOpacity(logoOpacity, 0.02, 90),
          setOpacity(logoOpacity, 1, 190),
        ]),
        Animated.parallel([
          setOpacity(heroDimOpacity, 0.04, 650),
          Animated.sequence([
            setOpacity(lampOnOpacity, 0.96, 120),
            setOpacity(lampOnOpacity, 0.02, 110),
            setOpacity(lampOnOpacity, 1, 180),
            setOpacity(lampOnOpacity, 0.03, 120),
            setOpacity(lampOnOpacity, 0.82, 340),
          ]),
        ]),
        fadeIn(formPaperOpacity, 360),
        Animated.parallel([
          Animated.stagger(55, [
            Animated.parallel([
              fadeIn(introOpacity, 220),
              riseIn(introOffset, 220),
            ]),
            Animated.parallel([
              fadeIn(formOpacity, 280),
              riseIn(formOffset, 280),
            ]),
          ]),
        ]),
      ]);
      entrance.start();
    });

    return () => {
      cancelled = true;
      entrance?.stop();
    };
  }, [formOffset, formOpacity, formPaperOpacity, heroDimOpacity, introOffset, introOpacity, lampOnOpacity, logoOpacity, logoScale]);

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
          <View style={[styles.hero, { width: layoutWidth }, compact && styles.heroCompact]}>
            <Image source={backgroundUnlit} style={[styles.heroImage, { width: layoutWidth, height: layoutWidth * 1.5 }]} resizeMode="cover" accessible={false} />
            <Animated.Image
              source={backgroundLit}
              style={[styles.heroImage, { width: layoutWidth, height: layoutWidth * 1.5, opacity: lampOnOpacity }]}
              resizeMode="cover"
              accessible={false}
            />
            <View pointerEvents="none" style={[styles.diagonal, { width: layoutWidth * 2, left: -layoutWidth / 2 }]} />
            <Animated.View
              pointerEvents="none"
              style={[styles.heroDim, { opacity: heroDimOpacity }]}
            />
            <Animated.View style={{ opacity: logoOpacity, transform: [{ scale: logoScale }] }}>
              <Image source={logo} style={[styles.logo, { width: logoWidth, height: logoHeight }]} resizeMode="contain" accessibilityLabel="Estación Burger" />
            </Animated.View>
          </View>
          <View style={styles.formSurface}>
            <Animated.View pointerEvents="none" style={[styles.formPaper, { opacity: formPaperOpacity }]} />
            <View style={styles.content}>
              <Animated.View style={[styles.intro, { opacity: introOpacity, transform: [{ translateY: introOffset }] }]}>
                <Text style={[styles.welcome, fontsLoaded && styles.welcomeFont]}>Bienvenido</Text>
                <View style={styles.underline} />
              </Animated.View>
              <Animated.View style={{ opacity: formOpacity, transform: [{ translateY: formOffset }] }}>
                <Text style={[styles.label, fontsLoaded && styles.boldFont]}>Correo o usuario</Text>
                <View style={styles.field}>
                  <Ionicons name="person-outline" size={21} color="#FF9D19" />
                  <TextInput accessibilityLabel="Correo o usuario" style={[styles.input, fontsLoaded && styles.bodyFont]} placeholder="Tu correo o usuario" placeholderTextColor="#A99A8A" value={username} onChangeText={setUsername} autoCapitalize="none" autoCorrect={false} autoComplete="username" />
                </View>
                <Text style={[styles.label, fontsLoaded && styles.boldFont]}>Contraseña</Text>
                <View style={styles.field}>
                  <Ionicons name="lock-closed-outline" size={21} color="#FF9D19" />
                  <TextInput accessibilityLabel="Contraseña" style={[styles.input, fontsLoaded && styles.bodyFont]} placeholder="Tu contraseña" placeholderTextColor="#A99A8A" value={password} onChangeText={setPassword} secureTextEntry={!showPassword} autoComplete="current-password" />
                  <TouchableOpacity accessibilityRole="button" accessibilityLabel={showPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'} onPress={() => setShowPassword(!showPassword)} style={styles.eyeButton}>
                    <Ionicons name={showPassword ? 'eye-off-outline' : 'eye-outline'} size={21} color="#FF4DB8" />
                  </TouchableOpacity>
                </View>
                {error && <Text accessibilityRole="alert" style={[styles.error, fontsLoaded && styles.bodyFont]}>{error}</Text>}
                <TouchableOpacity accessibilityRole="button" style={styles.forgotButton} onPress={() => router.push('/recuperar')}>
                  <Text style={[styles.link, fontsLoaded && styles.boldFont]}>¿Olvidaste tu contraseña?</Text>
                </TouchableOpacity>
                <Pressable accessibilityRole="button" accessibilityState={{ disabled: loading, busy: loading }} disabled={loading} onPress={handleLogin} style={({ pressed }) => [styles.loginButton, pressed && styles.loginPressed, loading && styles.loginLoading]}>
                  {loading ? <ActivityIndicator color="#20140A" /> : <Text style={[styles.loginText, fontsLoaded && styles.boldFont]}>Iniciar sesión</Text>}
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
  container: { flex: 1, backgroundColor: '#100C0A' },
  scrollContent: { flexGrow: 1, backgroundColor: '#100C0A' },
  hero: { height: 310, maxWidth: 420, alignSelf: 'center', justifyContent: 'center', alignItems: 'center', backgroundColor: '#100C0A', overflow: 'hidden' },
  heroCompact: { height: 245 },
  heroImage: { position: 'absolute', top: 0, left: 0 },
  heroDim: { position: 'absolute', left: 0, right: 0, top: 0, bottom: 0, backgroundColor: '#100C0A' },
  diagonal: { position: 'absolute', bottom: -60, height: 110, backgroundColor: '#FFF3DF', borderTopWidth: 7, borderTopColor: '#f5a000', transform: [{ rotate: '-7deg' }] },
  logo: { maxWidth: '100%' },
  formSurface: { flex: 1, backgroundColor: '#100C0A', paddingHorizontal: 24, paddingTop: 22, paddingBottom: 24 },
  formPaper: { position: 'absolute', left: 0, right: 0, top: 0, bottom: 0, backgroundColor: '#FFF3DF' },
  content: { width: '100%', maxWidth: 420, alignSelf: 'center' },
  intro: { alignItems: 'center', marginBottom: 20 },
  welcome: { color: '#442515', fontFamily: Platform.select({ ios: 'Snell Roundhand', android: 'cursive', default: 'cursive' }), fontSize: 43, lineHeight: 60, paddingHorizontal: 8 },
  welcomeFont: { fontFamily: 'KaushanScript' },
  bodyFont: { fontFamily: 'Lato' },
  boldFont: { fontFamily: 'LatoBold', fontWeight: 'normal' },
  underline: { width: 110, height: 4, borderRadius: 4, backgroundColor: '#FF8C00', transform: [{ rotate: '-3deg' }] },
  label: { color: '#442515', fontSize: 14, fontWeight: '700', marginBottom: 7 },
  field: { minHeight: 52, flexDirection: 'row', alignItems: 'center', borderWidth: 1, borderColor: '#E6A73D', borderRadius: 10, backgroundColor: '#FFF9EE', paddingLeft: 14, marginBottom: 15 },
  input: { flex: 1, color: '#442515', fontSize: 16, paddingHorizontal: 12, paddingVertical: 11, minWidth: 0 },
  eyeButton: { width: 48, height: 48, alignItems: 'center', justifyContent: 'center' },
  error: { color: '#B42336', fontSize: 13, marginBottom: 4 },
  forgotButton: { alignSelf: 'flex-end', minHeight: 44, justifyContent: 'center', marginTop: -8, marginBottom: 6 },
  link: { color: '#FF4DB8', fontSize: 14, fontWeight: '700' },
  loginButton: { minHeight: 54, borderWidth: 1, borderColor: '#FFD166', borderRadius: 12, backgroundColor: '#F5A000', alignItems: 'center', justifyContent: 'center', elevation: 7, shadowColor: '#FF8C00', shadowOffset: { width: 0, height: 0 }, shadowOpacity: 0.45, shadowRadius: 12 },
  loginPressed: { opacity: 0.8, transform: [{ scale: 0.99 }] },
  loginLoading: { opacity: 0.75 },
  loginText: { color: '#20140A', fontSize: 16, fontWeight: '700' },
  accountPrompt: { flexDirection: 'row', flexWrap: 'wrap', alignItems: 'center', justifyContent: 'center', marginTop: 10 },
  accountText: { color: '#5A3927', fontSize: 14 },
  createButton: { minHeight: 44, justifyContent: 'center', paddingHorizontal: 8 },
});
