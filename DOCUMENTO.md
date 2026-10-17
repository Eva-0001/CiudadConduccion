# Ciudad y Conducción — Documento de entrega

## 1. Cómo ejecutar

**Requisitos:** JDK 17 o superior (probado con JDK 21) y Apache Maven 3.x.

```bash
# Compilar y ejecutar la versión final (JuegoEntregas, configurada en pom.xml como mainClass)
mvn compile exec:exec

# Ejecutar los tests unitarios
mvn test
```

En macOS, Maven activa automáticamente el perfil `macos` (definido en `pom.xml`) que añade el parámetro JVM `-XstartOnFirstThread`.

---

## 2. Diseño de la ciudad

| Constante | Valor | Significado |
|---|---|---|
| `CELDA` | 10 | Ancho y profundidad de cada celda (unidades OpenGL) |
| `LIMITE` | 55 | Distancia del origen a cada borde; el mapa mide **110 × 110 unidades** |

**Mapa:** matriz `MAPA[11][11]` declarada en `CiudadBase.java`. Códigos: `0` = calle, `1` = edificio, `2` = parque.

Conteo real de celdas (verificado fila a fila):

| Tipo | Cantidad |
|---|---|
| Edificios (tipo 1) | **19** |
| Parques (tipo 2) | **6** |
| Total manzanas | **25** |

**Red de calles:** las filas y columnas con índice par (0, 2, 4, 6, 8, 10) son siempre calle, formando una cuadrícula ortogonal continua de **6 avenidas horizontales × 6 avenidas verticales**. Las celdas de manzana ocupan únicamente índices impares en ambos ejes. Esta disposición garantiza que cualquier punto de calle esté conectado a todos los demás sin cortes, hecho verificado en `ConduccionTest.testDestinosAlcanzables()` mediante una búsqueda en anchura (BFS) sobre el mapa.

---

## 3. Qué se adaptó al ampliar el mapa

| Aspecto | Archivo | Método |
|---|---|---|
| **Suelo** — ancho `LIMITE*2` | `CiudadBase.java` | `escena()` |
| **Límite de colisión** — `LIMITE - RADIO_AUTO` | `CiudadConAuto.java` | `puedeCircular()` |
| **Colisiones con manzanas** — semiancho `±5` u. | `CiudadConAuto.java` | `puedeCircular()` |
| **Cámara orbital** — radio `LIMITE*1.9`, altura `LIMITE*1.6` | `CiudadBase.java` | `configurarCamara()` |
| **Minimapa** — divisor `LIMITE+2`; indicador `4×6` u. | `CiudadBase.java` / `JuegoEntregas.java` | `dibujarFrame()`, `dibujarIndicadorAuto()` |
| **Farolas** — 25 piezas generadas desde `MAPA` | `CiudadIluminada.java` | `crearLuces()` |
| **Destinos** — cinco coordenadas en rango ±50 | `JuegoEntregas.java` | constante `DESTINOS[][]` |
| **Semáforos** — posición desde `posicionFarola()` | `JuegoEntregas.java` | `decorarCiudad()` → `dibujarSemaforo()` |
| **Cámara perspectiva** — plano lejano 400, cercano 0.2 | `CiudadBase.java` | `vertexShader()` |
| **Tests** — umbrales ≥11 filas/cols, ≥12 edif., ≥4 parques, ≥9 farolas | `ConduccionTest.java` | `testMapaYConstantes()`, `testLuces()` |

---

## 4. Controles

| Tecla | Acción |
|---|---|
| `W` / Flecha arriba | Acelerar hacia adelante |
| `S` / Flecha abajo | Frenar / reversa |
| `A` / Flecha izquierda | Girar a la izquierda |
| `D` / Flecha derecha | Girar a la derecha |
| `Espacio` | Freno de mano |
| `C` | Alternar cámara (seguimiento / aérea orbital) |
| `N` | Alternar modo día / noche |
| `F` | Encender / apagar faros del auto |
| `M` | Mostrar / ocultar minimapa |
| `R` | Reiniciar posición del auto y contador de entregas |
| `ESC` | Salir de la aplicación |

---

## 5. Funciones implementadas (Requisitos obligatorios)

- **Iluminación día / noche** (`CiudadIluminada.java`, `fragmentShader()`): de día, ambiente `0.48` + difusa solar `0.65`; de noche, ambiente azulado `(0.12, 0.16, 0.24)` + difusa solar tenue `0.10`.
- **Farolas procedimentales** (`CiudadIluminada.java`, `crearLuces()`): **25 farolas**, una por manzana, a `Y = 4.5`. De noche emiten luz cálida `(1.0, 0.73, 0.34)` con cálculo de atenuación cuadrática sobre las superficies.
- **Faros del auto** (`CiudadIluminada.java`, `fragmentShader()`): dos conos de luz frontales que proyectan en la dirección del auto con `smoothstep(0.85, 0.97)`, atenuación cuadrática y color `(1.0, 0.94, 0.72)`.
- **Luz difusa con normales** (`CiudadBase.java`, `vertexShader()`): cada vértice del cubo maestro incluye su normal; el shader evalúa iluminación Lambertiana por fragmento.
- **Decoración urbana obligatoria** (`JuegoEntregas.java`, `decorarCiudad()`):
  - Árboles (tronco + copa) y bancos (asiento, soporte, respaldo) en los parques.
  - Ventanas por pisos en las cuatro fachadas de edificios con efecto emisivo nocturno.
  - Pasos peatonales cebrados sobre la calzada contigua.
- **Semáforos animados** (`JuegoEntregas.java`, `dibujarSemaforo()`): ciclo de **12 s** — rojo 5 s (fases 0–4), verde 5 s (fases 5–9), amarillo 2 s (fases 10–11).
- **Minimapa** (`JuegoEntregas.java`, `dibujarFrame()`): segundo pase de renderizado ortográfico en un recuadro de hasta 260 px usando `glScissor`; indicador cian con punta blanca para orientación del auto y marca dorada para el destino activo, manteniendo el norte hacia arriba.
- **Reinicio** (`CiudadConAuto.java` + `JuegoEntregas.java`, `reiniciar()`): restaura posición inicial `(−30, 30)`, ángulo `0`, velocidad `0`, entregas `0` y cronómetro en `0`.
- **Entregas** (`JuegoEntregas.java`, `actualizar()`): 5 paradas secuenciales; se valida cercanía (distancia < 3 u.) y detención (velocidad < 1 u/s). La barra de título muestra progreso y tiempo.

---

## 6. Limitaciones y Mejoras Opcionales No Implementadas

- **Mejoras opcionales no implementadas:**
  - Sin **tráfico autónomo** ni otros vehículos rodando en la ciudad.
  - Sin **panel dentro de la ventana (HUD)** con velocidad y luces: la información se visualiza en la barra de título de la ventana.
  - Sin **menú o pantalla de ayuda** interna dentro de la ventana.
  - Los semáforos son un elemento visual animado y no aplican freno forzado al auto.
  - Sin señalización vertical adicional.
- **Limitaciones gráficas y generales:**
  - Sin **sombras arrojadas**: el cálculo es iluminación local por fragmento (sin shadow mapping ni ray tracing).
  - Sin pistas de audio ni efectos de sonido.

---

## 7. Recursos externos

- **Base del proyecto:** ejemplos de cátedra (`AppCamara.java` y `AppLaberinto.java`: inicialización de GLFW, gestión de VAO/VBO, shaders y ciclo de eventos).
- **LWJGL 3.3.3:** bindings Java para GLFW y OpenGL 3.3 Core (declarados en `pom.xml`).
- **JUnit 3.8.1:** framework para la ejecución automatizada de pruebas unitarias en Maven.
- **Asistencia de IA:** se empleó asistencia de inteligencia artificial para apoyar la adaptación y consistencia de nombres, la generación procedimental de farolas y la verificación de pruebas unitarias.
