# FitVision

Entrenador personal inteligente y corrector de postura. Aplicación Android nativa en **Java** que usa
**Google ML Kit Pose Detection** para calcular los ángulos articulares del cuerpo, contar repeticiones y
corregir la postura en **pantalla** y por **voz** (TextToSpeech).

Proyecto del **Grupo 3 · Detección de Pose**.

> **Estado:** versión compilada (`assembleDebug`). Los umbrales de ángulos y de corrección son heurísticas
> y deben calibrarse con grabaciones reales antes de considerarlos fiables.

---

## Funcionalidades

| Función | Cómo funciona |
|---|---|
| **Cámara en vivo** | CameraX (`ImageAnalysis`) envía cada cuadro a ML Kit en `STREAM_MODE`. Muestra el esqueleto, el ejercicio, el ángulo principal y las repeticiones. |
| **Tomar fotografía** | Usa la cámara nativa del sistema (`TakePicture`) y analiza la foto con ML Kit en `SINGLE_IMAGE_MODE`. |
| **Elegir de la galería** | Abre el selector del sistema (`OpenDocument`) para una imagen o un video. Los videos se muestrean cada 250 ms (hasta 60 s) y cuentan repeticiones. |
| **Reconocimiento de ejercicio** | Usa la inclinación del torso (cadera → hombros) para distinguir sentadilla de flexión, con confirmación de 8 frames. |
| **Conteo de repeticiones** | Máquina de estados sobre el ángulo articular suavizado: rodilla en sentadilla, codo en flexión. |
| **Correcciones de postura** | Espalda demasiado inclinada en sentadilla, cadera fuera de línea en flexión y repetición poco profunda. |
| **Voz** | `SpeechManager` dice el conteo ("¡Uno!", "¡Dos!"…) y las correcciones en español (es-MX, con respaldo a es). |
| **Interfaz** | Material Design 3 con tarjetas elevadas y botones tonales y de contorno. |

## Ejercicios y umbrales

| Ejercicio | Ángulo principal | Fase abajo | Fase arriba | Profundidad mínima |
|---|---|---|---|---|
| Sentadilla | Rodilla (cadera-rodilla-tobillo) | < 110° | > 155° | 100° |
| Flexión | Codo (hombro-codo-muñeca) | < 100° | > 150° | 90° |

Otras reglas (`logic/PostureAnalyzer.java`, `logic/ExerciseRecognizer.java`):

- Sentadilla: se avisa si el torso se inclina más de 50° desde la vertical.
- Flexión: se avisa si el ángulo cadera (hombro-cadera-tobillo) baja de 150°.
- Ejercicio automático: torso inclinado ≥ 65° = flexión; torso ≤ 45° con piernas visibles = sentadilla.

El selector **Ejercicio** permite elegir "Automático", sentadilla o flexión.
Cambiarlo reinicia el contador. En la galería, el selector repite el análisis de la foto o del video.

Para que los ángulos sean fiables, colócate **de lado** a la cámara con el cuerpo completo visible.

## Cómo ejecutar

1. Abre la carpeta del proyecto con **Android Studio** (Koala o superior).
2. Espera la sincronización de Gradle. Se descargan AGP, CameraX y ML Kit.
3. Ejecuta la app en un dispositivo físico con Android 7.0 (API 24) o superior.
4. Entra a **Cámara en vivo** y concede el permiso de cámara.

> **Nota sobre Gradle:** el repositorio no incluye el `.jar` del wrapper. Android Studio sincroniza igualmente
> con la versión de `gradle/wrapper/gradle-wrapper.properties`. Si hace falta generarlo, ejecuta
> `gradle wrapper --gradle-version 8.13`.

## Estructura del proyecto

```
app/src/main/java/com/grupo3/signtranslator/
├── MainActivity.java                 Menú principal (tres botones)
├── LivePreviewActivity.java          Entrenamiento en tiempo real
├── StaticAnalysisActivity.java       Análisis de foto o video de la galería
├── camera/
│   └── PoseAnalyzer.java             Analizador de CameraX + ML Kit
├── ml/
│   └── PoseDetectorProvider.java     Detectores STREAM y SINGLE_IMAGE
├── ui/
│   ├── GraphicOverlay.java           Vista superpuesta y mapeo de coordenadas
│   └── PoseGraphic.java              Dibujo del esqueleto corporal
├── logic/
│   ├── Exercise.java                 Ejercicios y umbrales de ángulo
│   ├── PoseMath.java                 Ángulos y torso a partir de los puntos de ML Kit
│   ├── ExerciseRecognizer.java       Reconocimiento del ejercicio por orientación del torso
│   ├── RepCounter.java               Contador de repeticiones (máquina de estados)
│   └── PostureAnalyzer.java          Ángulo principal y correcciones de postura
├── tts/
│   ├── SpeechManager.java            Envoltorio de TextToSpeech
│   └── SpanishNumbers.java           Conteo en español para la voz
└── util/
    └── ImageUtils.java               Carga de imágenes con corrección EXIF
```

## Tecnologías

- **Lenguaje:** Java 17
- **Pose:** Google ML Kit Pose Detection `18.0.0-beta5` (33 puntos corporales)
- **Cámara:** CameraX `1.4.0` (core, camera2, lifecycle, view)
- **UI:** Material Components `1.12.0`, AppCompat `1.7.0`
- **Build:** Android Gradle Plugin `8.13.2`, Gradle `8.13`, `compileSdk` 34, `minSdk` 24

Por requisito del proyecto, **no se usa ninguna otra API de visión** (ni reconocimiento de manos ni de dedos).
Los puntos de muñeca se usan solo como parte del brazo.

## Permisos

| Permiso | Uso |
|---|---|
| `CAMERA` | Entrenamiento en vivo. Se solicita en tiempo de ejecución. |
| `READ_MEDIA_IMAGES` / `READ_EXTERNAL_STORAGE` (hasta API 32) | Acceso a imágenes. El selector de galería concede acceso temporal, por eso no se pide en tiempo de ejecución. |

## Limitaciones conocidas

- Los ángulos se miden en el plano de la imagen (2D). Con la cámara de frente, la rodilla o el codo pueden verse acortados.
- La detección automática distingue flexión y sentadilla por la inclinación del torso, pero puede confundirlas en posiciones poco habituales; el selector lo corrige.
- Los umbrales de inclinación del torso son heurísticas sin calibrar.
- La revisión de postura no detecta valgo de rodilla ni rotaciones de la columna; solo ángulos en el plano.
- Un video se analiza como una secuencia de fotos (un fotograma cada 250 ms, máximo 60 s); en la pantalla de resultado no se dibuja el esqueleto sobre el video.
- La gran angular solo se usa si CameraX expone esa lente en el dispositivo.

## Próximos pasos

- Probar en dispositivo físico y calibrar los umbrales de cada ejercicio con grabaciones reales.
- Añadir más ejercicios siguiendo el patrón de `Exercise`, `RepCounter` y `PostureAnalyzer`.

---

*Proyecto universitario · Computación Gráfica y Visual · Grupo 3*
