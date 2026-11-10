# FitVision

Entrenador personal con visión por computadora. Es una app Android que usa la cámara para ver el cuerpo,
medir los ángulos de las articulaciones, contar repeticiones y avisar cuando la postura no es correcta.

**Para qué sirve:** entrenar sentadillas y flexiones con buena técnica sin necesidad de un entrenador presente.

**Finalidad:** proyecto universitario del Grupo 3 de Computación Gráfica y Visual (detección de pose).

## Funciones

- **Cámara en vivo:** cuenta repeticiones y muestra correcciones mientras la persona entrena.
- **Foto o galería:** analiza una foto, o un video de hasta 60 s, y resume la postura y las repeticiones.
- **Reconocimiento automático** de sentadilla o flexión, o selección manual.
- **Voz en español:** dice el conteo y las correcciones.
- **Rondas:** agenda recordatorios de rutinas por hora y días de la semana.
- **Consejos y créditos:** recomendaciones de técnica e información del proyecto.

## Herramientas

- **Lenguaje:** Java 17
- **Plataforma:** Android nativo (minSdk 24)
- **Detección de pose:** Google ML Kit Pose Detection (33 puntos del cuerpo; no usa manos ni dedos)
- **Cámara:** CameraX
- **Interfaz:** Material Design 3
- **Voz:** TextToSpeech
- **Notificaciones y alarmas:** AlarmManager y NotificationManager

