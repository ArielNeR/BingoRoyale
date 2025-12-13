# 🎱 Bingo Royale

Aplicación móvil de Bingo clásico para Android que permite jugar en red local (WiFi/LAN) sin necesidad de internet, cuentas ni servidores externos.

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Min SDK](https://img.shields.io/badge/Min%20SDK-24-blue?style=for-the-badge)

---

## 📱 Capturas de Pantalla

<p align="center">
  <img src="screenshots/home.png" width="200" alt="Pantalla Principal"/>
  <img src="screenshots/caller.png" width="200" alt="Modo Cantador"/>
  <img src="screenshots/player.png" width="200" alt="Modo Jugador"/>
</p>

---

## 🎮 Características

### Dos Modos de Juego
- **Cantador**: Genera números aleatorios y los transmite a los jugadores conectados
- **Jugador**: Recibe los números y marca su cartón manualmente

### Conexión Local
- Comunicación directa por WiFi/LAN
- Descubrimiento automático de partidas en la red
- Sin necesidad de internet ni servidores externos
- Ingreso manual de IP como alternativa

### Modos de Bingo
- **Modo 75 (USA)**: Cartón 5x5 con espacio FREE central
- **Modo 90 (Europeo)**: Cartón 3x9 con 15 números por cartón

### Interfaz
- Diseño oscuro con acentos dorados
- Animaciones suaves
- Feedback háptico (vibración)
- Notificaciones de BINGO entre jugadores

---

## 📋 Requisitos

- Android 7.0 (API 24) o superior
- Conexión WiFi para modo multijugador
- Permisos: WiFi, Internet, Vibración

---

## 🚀 Instalación

### Opción 1: APK
1. Descarga el APK desde [Releases](https://github.com/tu-usuario/bingo-royale/releases)
2. Habilita "Instalar desde fuentes desconocidas" en tu dispositivo
3. Instala el APK

### Opción 2: Compilar desde código
```bash
# Clonar repositorio
git clone https://github.com/tu-usuario/bingo-royale.git

# Abrir en Android Studio
# File -> Open -> Seleccionar carpeta del proyecto

# Compilar y ejecutar
# Run -> Run 'app'
